/**
 * Copyright (C) 2018-2019 toop.eu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.toop.connector.mp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.asic.AsicUtils;
import com.helger.asic.SignatureHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.text.MultilingualText;
import com.helger.httpclient.HttpClientManager;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;

import eu.toop.commons.dataexchange.v140.TDEDataProviderType;
import eu.toop.commons.dataexchange.v140.TDEErrorType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.error.EToopErrorCategory;
import eu.toop.commons.error.EToopErrorCode;
import eu.toop.commons.error.EToopErrorOrigin;
import eu.toop.commons.error.EToopErrorSeverity;
import eu.toop.commons.error.ToopErrorException;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.api.http.TCHttpClientFactory;
import eu.toop.connector.me.EActingSide;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.MEException;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.MEPayload;
import eu.toop.connector.r2d2client.IR2D2Endpoint;
import eu.toop.connector.r2d2client.R2D2Client;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The nested performer class that does the hard work in step 3/4.
 *
 * @author Philip Helger
 */
final class MessageProcessorDPOutgoingPerformer implements IConcurrentPerformer <TDETOOPResponseType>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MessageProcessorDPOutgoingPerformer.class);

  @Nonnull
  private static TDEErrorType _createError (@Nonnull final String sLogPrefix,
                                            @Nonnull final EToopErrorCategory eCategory,
                                            @Nonnull final EToopErrorCode eErrorCode,
                                            @Nonnull final String sErrorText,
                                            @Nullable final Throwable t)
  {
    // Surely no DP here
    ToopKafkaClient.send (EErrorLevel.ERROR, () -> sLogPrefix + "[" + eErrorCode.getID () + "] " + sErrorText);
    return ToopMessageBuilder.createError (null,
                                           EToopErrorOrigin.RESPONSE_SUBMISSION,
                                           eCategory,
                                           eErrorCode,
                                           EToopErrorSeverity.FAILURE,
                                           new MultilingualText (Locale.US, sErrorText),
                                           t == null ? null : StackTraceHelper.getStackAsString (t));
  }

  @Nonnull
  private static TDEErrorType _createGenericError (@Nonnull final String sLogPrefix, @Nonnull final Throwable t)
  {
    return _createError (sLogPrefix, EToopErrorCategory.TECHNICAL_ERROR, EToopErrorCode.GEN, t.getMessage (), t);
  }

  public static void sendTo_to_dp (@Nonnull final TDETOOPResponseType aResponse) throws ToopErrorException, IOException
  {
    // Forward to the DP at /to-dp interface
    final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();

    try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory))
    {
      final SignatureHelper aSH = new SignatureHelper (TCConfig.getKeystoreType (),
                                                       TCConfig.getKeystorePath (),
                                                       TCConfig.getKeystorePassword (),
                                                       TCConfig.getKeystoreKeyAlias (),
                                                       TCConfig.getKeystoreKeyPassword ());

      try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        ToopMessageBuilder.createResponseMessageAsic (aResponse, aBAOS, aSH);

        // Send to DP (see ToDPServlet in toop-interface)
        final String sDestinationUrl = TCConfig.getMPToopInterfaceDPUrl ();

        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Posting signed ASiC response to " + sDestinationUrl);

        final HttpPost aHttpPost = new HttpPost (sDestinationUrl);
        aHttpPost.setEntity (new InputStreamEntity (aBAOS.getAsInputStream ()));
        try (final CloseableHttpResponse aHttpResponse = aMgr.execute (aHttpPost))
        {
          EntityUtils.consume (aHttpResponse.getEntity ());
        }
      }
    }
  }

  public void runAsync (@Nonnull final TDETOOPResponseType aResponse) throws Exception
  {
    final String sRequestID = aResponse.getDataRequestIdentifier ().getValue ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DP outgoing response (3/4)");
    final ICommonsList <TDEErrorType> aErrors = new CommonsArrayList <> ();

    // TODO Schematron validation

    // Ensure data provider element is present (required in all cases)
    final TDEDataProviderType aDataProvider = aResponse.getDataProvider ()
                                                       .isEmpty () ? null : aResponse.getDataProviderAtIndex (0);
    if (aDataProvider == null)
    {
      final String sErrorMsg = "The DataProvider element is missing in the response";
      aErrors.add (_createError (sLogPrefix, EToopErrorCategory.PARSING, EToopErrorCode.IF_001, sErrorMsg, null));
    }
    else
    {
      // No need to invoke SMM - source concepts are still available
      final IIdentifierFactory aIDFactory = TCSettings.getIdentifierFactory ();

      // invoke R2D2 client with a single endpoint
      // The destination EP is the sender of the original document!
      final IParticipantIdentifier aDCParticipantID = aIDFactory.createParticipantIdentifier (aResponse.getDataConsumer ()
                                                                                                       .getDCElectronicAddressIdentifier ()
                                                                                                       .getSchemeID (),
                                                                                              aResponse.getDataConsumer ()
                                                                                                       .getDCElectronicAddressIdentifier ()
                                                                                                       .getValue ());
      final IDocumentTypeIdentifier aDocTypeID = aIDFactory.createDocumentTypeIdentifier (aResponse.getRoutingInformation ()
                                                                                                   .getDocumentTypeIdentifier ()
                                                                                                   .getSchemeID (),
                                                                                          aResponse.getRoutingInformation ()
                                                                                                   .getDocumentTypeIdentifier ()
                                                                                                   .getValue ());
      final IProcessIdentifier aProcessID = aIDFactory.createProcessIdentifier (aResponse.getRoutingInformation ()
                                                                                         .getProcessIdentifier ()
                                                                                         .getSchemeID (),
                                                                                aResponse.getRoutingInformation ()
                                                                                         .getProcessIdentifier ()
                                                                                         .getValue ());

      ICommonsList <IR2D2Endpoint> aEndpoints;
      {
        final ICommonsList <IR2D2Endpoint> aTotalEndpoints = new R2D2Client ().getEndpoints (sLogPrefix,
                                                                                             aDCParticipantID,
                                                                                             aDocTypeID,
                                                                                             aProcessID);

        // Filter all endpoints with the corresponding transport profile
        final String sTransportProfileID = TCConfig.getMEMProtocol ().getTransportProfileID ();
        aEndpoints = aTotalEndpoints.getAll (x -> x.getTransportProtocol ().equals (sTransportProfileID));

        // Expecting exactly one endpoint!
        ToopKafkaClient.send (aEndpoints.size () == 1 ? EErrorLevel.INFO : EErrorLevel.ERROR,
                              () -> sLogPrefix +
                                    "R2D2 found [" +
                                    aEndpoints.size () +
                                    "/" +
                                    aTotalEndpoints.size () +
                                    "] endpoint(s)");
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug (sLogPrefix + "Endpoint details: " + aEndpoints);
      }

      // 3. start message exchange to DC
      // The sender of the response is the DP
      final IParticipantIdentifier aDPParticipantID = aIDFactory.createParticipantIdentifier (aDataProvider.getDPElectronicAddressIdentifier ()
                                                                                                           .getSchemeID (),
                                                                                              aDataProvider.getDPElectronicAddressIdentifier ()
                                                                                                           .getValue ());

      if (aEndpoints.isEmpty ())
      {
        // No endpoint - ooops
        aErrors.add (_createError (sLogPrefix,
                                   EToopErrorCategory.DYNAMIC_DISCOVERY,
                                   EToopErrorCode.DD_004,
                                   "Found no matching DC endpoint - not transmitting response from DP '" +
                                                          aDPParticipantID.getURIEncoded () +
                                                          "' to DC '" +
                                                          aDCParticipantID.getURIEncoded () +
                                                          "'!",
                                   null));
      }

      if (aErrors.isEmpty ())
      {
        // Combine MS data and TOOP data into a single ASiC message
        // Do this only once and not for every endpoint
        byte [] aPayloadBytes = null;
        try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
        {
          // Ensure flush/close of DumpOS!
          try (final OutputStream aDumpOS = TCDumpHelper.getDumpOutputStream (aBAOS,
                                                                              TCConfig.getDebugToDCDumpPathIfEnabled (),
                                                                              "to-dc.asic"))
          {
            ToopMessageBuilder.createResponseMessageAsic (aResponse, aDumpOS, MPWebAppConfig.getSignatureHelper ());
          }
          catch (final ToopErrorException ex)
          {
            aErrors.add (_createError (sLogPrefix,
                                       EToopErrorCategory.E_DELIVERY,
                                       ex.getErrorCode (),
                                       ex.getMessage (),
                                       ex.getCause ()));
          }
          catch (final IOException ex)
          {
            aErrors.add (_createGenericError (sLogPrefix, ex));
          }

          aPayloadBytes = aBAOS.toByteArray ();
        }

        if (aErrors.isEmpty ())
        {
          // build MEM once
          final MEPayload aPayload = new MEPayload (AsicUtils.MIMETYPE_ASICE, sRequestID, aPayloadBytes);
          final MEMessage aMEMessage = new MEMessage (aPayload);

          for (final IR2D2Endpoint aEP : aEndpoints)
          {
            ToopKafkaClient.send (EErrorLevel.INFO,
                                  sLogPrefix +
                                                    "Sending MEM message to '" +
                                                    aEP.getEndpointURL () +
                                                    "' using transport protocol '" +
                                                    aEP.getTransportProtocol () +
                                                    "'");

            // routing metadata - sender ID!
            final GatewayRoutingMetadata aGRM = new GatewayRoutingMetadata (aDPParticipantID.getURIEncoded (),
                                                                            aDocTypeID.getURIEncoded (),
                                                                            aProcessID.getURIEncoded (),
                                                                            aEP,
                                                                            EActingSide.DP);

            try
            {
              // Reuse the same MEMessage for each endpoint
              if (!MEMDelegate.getInstance ().sendMessage (aGRM, aMEMessage))
              {
                aErrors.add (_createError (sLogPrefix,
                                           EToopErrorCategory.E_DELIVERY,
                                           EToopErrorCode.ME_001,
                                           "Error sending message",
                                           null));
              }
            }
            catch (final MEException ex)
            {
              aErrors.add (_createError (sLogPrefix,
                                         EToopErrorCategory.E_DELIVERY,
                                         EToopErrorCode.ME_001,
                                         "Error sending message",
                                         ex));
            }
          }
        }
      }
    }

    if (aErrors.isNotEmpty ())
    {
      // send back to DP including errors
      aResponse.getError ().addAll (aErrors);
      sendTo_to_dp (aResponse);
    }
  }
}
