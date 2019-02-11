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
import org.w3c.dom.Document;

import com.helger.asic.AsicUtils;
import com.helger.asic.SignatureHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.IError;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.error.level.IErrorLevel;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.ByteArrayWrapper;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.text.MultilingualText;
import com.helger.httpclient.HttpClientManager;
import com.helger.jaxb.validation.WrappedCollectingValidationEventHandler;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.schematron.svrl.AbstractSVRLMessage;

import eu.toop.commons.dataexchange.v140.TDEDataProviderType;
import eu.toop.commons.dataexchange.v140.TDEErrorType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.error.EToopErrorCategory;
import eu.toop.commons.error.EToopErrorCode;
import eu.toop.commons.error.EToopErrorOrigin;
import eu.toop.commons.error.EToopErrorSeverity;
import eu.toop.commons.error.IToopErrorCode;
import eu.toop.commons.error.ToopErrorException;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.commons.jaxb.ToopWriter;
import eu.toop.commons.schematron.TOOPSchematronValidator;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.api.as4.MEPayload;
import eu.toop.connector.api.as4.MERoutingInformation;
import eu.toop.connector.api.as4.MessageExchangeManager;
import eu.toop.connector.api.http.TCHttpClientFactory;
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
  private static TDEErrorType _createError (@Nonnull final IErrorLevel aErrorLevel,
                                            @Nonnull final String sLogPrefix,
                                            @Nonnull final EToopErrorCategory eCategory,
                                            @Nonnull final IToopErrorCode aErrorCode,
                                            @Nonnull final String sErrorText,
                                            @Nullable final Throwable t)
  {
    ToopKafkaClient.send (aErrorLevel, () -> sLogPrefix + "[" + aErrorCode.getID () + "] " + sErrorText);
    return ToopMessageBuilder.createError (null,
                                           EToopErrorOrigin.RESPONSE_SUBMISSION,
                                           eCategory,
                                           aErrorCode,
                                           aErrorLevel.isError () ? EToopErrorSeverity.FAILURE
                                                                  : EToopErrorSeverity.WARNING,
                                           new MultilingualText (Locale.US, sErrorText),
                                           t == null ? null : StackTraceHelper.getStackAsString (t));
  }

  @Nonnull
  private static TDEErrorType _createError (@Nonnull final String sLogPrefix,
                                            @Nonnull final EToopErrorCategory eCategory,
                                            @Nonnull final IToopErrorCode aErrorCode,
                                            @Nonnull final String sErrorText,
                                            @Nullable final Throwable t)
  {
    return _createError (EErrorLevel.ERROR, sLogPrefix, eCategory, aErrorCode, sErrorText, t);
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

        ToopKafkaClient.send (EErrorLevel.INFO,
                              () -> "Start posting signed ASiC response to '" + sDestinationUrl + "'");

        final HttpPost aHttpPost = new HttpPost (sDestinationUrl);
        aHttpPost.setEntity (new InputStreamEntity (aBAOS.getAsInputStream ()));
        try (final CloseableHttpResponse aHttpResponse = aMgr.execute (aHttpPost))
        {
          EntityUtils.consume (aHttpResponse.getEntity ());
        }

        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Done posting signed ASiC response to '" + sDestinationUrl + "'");
      }
    }
  }

  public void runAsync (@Nonnull final TDETOOPResponseType aResponse) throws Exception
  {
    final String sRequestID = aResponse != null &&
                              aResponse.getDataRequestIdentifier () != null ? aResponse.getDataRequestIdentifier ().getValue () : "temp-tc3-id-" + GlobalIDFactory.getNewIntID ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DP outgoing response (3/4)");
    final ICommonsList <TDEErrorType> aErrors = new CommonsArrayList <> ();

    // Schematron validation
    if (TCConfig.isMPSchematronValidationEnabled ())
    {
      final ErrorList aErrorList = new ErrorList ();
      // XML creation
      final Document aDoc = ToopWriter.response ()
                                      .setValidationEventHandler (new WrappedCollectingValidationEventHandler (aErrorList))
                                      .getAsDocument (aResponse);
      if (aDoc == null)
      {
        for (final IError aError : aErrorList)
          aErrors.add (_createError (aError.getErrorLevel (),
                                     sLogPrefix,
                                     EToopErrorCategory.PARSING,
                                     EToopErrorCode.IF_001,
                                     aError.getErrorText (Locale.US),
                                     aError.getLinkedException ()));
      }
      else
      {
        // Schematron validation
        final TOOPSchematronValidator aValidator = new TOOPSchematronValidator ();
        final ICommonsList <AbstractSVRLMessage> aMsgs = aValidator.validateTOOPMessage (aDoc);
        for (final AbstractSVRLMessage aMsg : aMsgs)
        {
          aErrors.add (_createError (aMsg.getFlag (),
                                     sLogPrefix,
                                     EToopErrorCategory.PARSING,
                                     EToopErrorCode.IF_001,
                                     "[" + aMsg.getLocation () + "] [Test: " + aMsg.getTest () + "] " + aMsg.getText (),
                                     null));
        }
      }
    }
    else
    {
      ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Schematron validation was explicitly disabled.");
    }

    if (aErrors.isEmpty ())
    {
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
          ByteArrayWrapper aPayloadBytes = null;
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

            aPayloadBytes = ByteArrayWrapper.create (aBAOS, false);
          }

          if (aErrors.isEmpty ())
          {
            // build MEM once
            final MEPayload aPayload = new MEPayload (AsicUtils.MIMETYPE_ASICE, sRequestID, aPayloadBytes);
            final MEMessage aMEMessage = MEMessage.create (aPayload);

            for (final IR2D2Endpoint aEP : aEndpoints)
            {
              ToopKafkaClient.send (EErrorLevel.INFO,
                                    sLogPrefix +
                                                      "Sending MEM message to '" +
                                                      aEP.getEndpointURL () +
                                                      "' using transport protocol '" +
                                                      aEP.getTransportProtocol () +
                                                      "'");

              // Main message exchange
              final MERoutingInformation aMERoutingInfo = new MERoutingInformation (aDPParticipantID,
                                                                                    aEP.getParticipantID (),
                                                                                    aDocTypeID,
                                                                                    aProcessID,
                                                                                    aEP.getTransportProtocol (),
                                                                                    aEP.getEndpointURL (),
                                                                                    aEP.getCertificate ());
              try
              {
                MessageExchangeManager.getConfiguredImplementation ().sendDPOutgoing (aMERoutingInfo, aMEMessage);
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
    }

    if (aErrors.isNotEmpty ())
    {
      // send back to DP including errors
      aResponse.getError ().addAll (aErrors);
      sendTo_to_dp (aResponse);
    }
  }
}
