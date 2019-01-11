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
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;

import com.helger.asic.SignatureHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.text.MultilingualText;
import com.helger.httpclient.HttpClientManager;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.concept.EConceptType;
import eu.toop.commons.dataexchange.v120.TDEConceptRequestType;
import eu.toop.commons.dataexchange.v120.TDEDataElementRequestType;
import eu.toop.commons.dataexchange.v120.TDEErrorType;
import eu.toop.commons.dataexchange.v120.TDETOOPRequestType;
import eu.toop.commons.dataexchange.v120.TDETOOPResponseType;
import eu.toop.commons.error.EToopErrorCategory;
import eu.toop.commons.error.EToopErrorCode;
import eu.toop.commons.error.EToopErrorOrigin;
import eu.toop.commons.error.EToopErrorSeverity;
import eu.toop.commons.error.ToopErrorException;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.commons.jaxb.ToopXSDHelper;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.http.TCHttpClientFactory;
import eu.toop.connector.smmclient.IMappedValueList;
import eu.toop.connector.smmclient.MappedValue;
import eu.toop.connector.smmclient.SMMClient;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The nested performer class that does the hard work in step 2/4.
 *
 * @author Philip Helger
 */
final class MessageProcessorDPIncomingPerformer implements IConcurrentPerformer <TDETOOPRequestType>
{
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
                                           EToopErrorOrigin.REQUEST_RECEPTION,
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

  public static void sendTo_to_dp (@Nonnull final TDETOOPRequestType aRequest) throws ToopErrorException, IOException
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
        ToopMessageBuilder.createRequestMessageAsic (aRequest, aBAOS, aSH);

        // Send to DP (see ToDPServlet in toop-interface)
        final String sDestinationUrl = TCConfig.getMPToopInterfaceDPUrl ();

        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Posting signed ASiC request to " + sDestinationUrl);

        final HttpPost aHttpPost = new HttpPost (sDestinationUrl);
        aHttpPost.setEntity (new InputStreamEntity (aBAOS.getAsInputStream ()));
        try (final CloseableHttpResponse aHttpResponse = aMgr.execute (aHttpPost))
        {
          EntityUtils.consume (aHttpResponse.getEntity ());
        }
      }
    }
  }

  public void runAsync (@Nonnull final TDETOOPRequestType aRequest) throws Exception
  {
    final String sRequestID = aRequest.getDataRequestIdentifier ().getValue ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    final ICommonsList <TDEErrorType> aErrors = new CommonsArrayList <> ();

    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DP Incoming Request (2/4)");

    // Map to DP concepts
    // TODO make it dependent on requested document type
    final String sDestinationMappingURI = TCConfig.getSMMMappingNamespaceURIForDP ();
    if (StringHelper.hasText (sDestinationMappingURI))
    {
      final SMMClient aClient = new SMMClient ();
      for (final TDEDataElementRequestType aDER : aRequest.getDataElementRequest ())
      {
        final TDEConceptRequestType aSrcConcept = aDER.getConceptRequest ();
        // ignore all DC source concepts
        if (aSrcConcept.getSemanticMappingExecutionIndicator ().isValue ())
        {
          for (final TDEConceptRequestType aToopConcept : aSrcConcept.getConceptRequest ())
            // Only if not yet mapped
            if (!aToopConcept.getSemanticMappingExecutionIndicator ().isValue ())
            {
              aClient.addConceptToBeMapped (ConceptValue.create (aToopConcept));
            }
        }
      }

      // Main mapping
      final IMappedValueList aMappedValues = aClient.performMapping (sLogPrefix,
                                                                     sDestinationMappingURI,
                                                                     MPWebAppConfig.getSMMConceptProvider (),
                                                                     (sLogPrefix1,
                                                                      sSourceNamespace,
                                                                      sSourceValue,
                                                                      sDestNamespace) -> {
                                                                       final String sErrorMsg = "Found no mapping for '" +
                                                                                                sSourceNamespace +
                                                                                                '#' +
                                                                                                sSourceValue +
                                                                                                "' to destination namespace '" +
                                                                                                sDestNamespace +
                                                                                                "'";
                                                                       aErrors.add (_createError (sLogPrefix1,
                                                                                                  EToopErrorCategory.SEMANTIC_MAPPING,
                                                                                                  EToopErrorCode.SM_002,
                                                                                                  sErrorMsg,
                                                                                                  null));
                                                                     });

      // add all the mapped values in the request
      for (final TDEDataElementRequestType aDER : aRequest.getDataElementRequest ())
      {
        final TDEConceptRequestType aSrcConcept = aDER.getConceptRequest ();
        if (aSrcConcept.getSemanticMappingExecutionIndicator ().isValue ())
        {
          for (final TDEConceptRequestType aToopConcept : aSrcConcept.getConceptRequest ())
            // Only if not yet mapped
            if (!aToopConcept.getSemanticMappingExecutionIndicator ().isValue ())
            {
              // Now the toop concept was mapped
              aToopConcept.getSemanticMappingExecutionIndicator ().setValue (true);

              final ConceptValue aToopCV = ConceptValue.create (aToopConcept);
              for (final MappedValue aMV : aMappedValues.getAllBySource (x -> x.equals (aToopCV)))
              {
                final TDEConceptRequestType aDstConcept = new TDEConceptRequestType ();
                aDstConcept.setConceptTypeCode (ToopXSDHelper.createCode (EConceptType.DP.getID ()));
                aDstConcept.setSemanticMappingExecutionIndicator (ToopXSDHelper.createIndicator (false));
                aDstConcept.setConceptNamespace (ToopXSDHelper.createIdentifier (aMV.getDestination ()
                                                                                    .getNamespace ()));
                aDstConcept.setConceptName (ToopXSDHelper.createText (aMV.getDestination ().getValue ()));
                aToopConcept.addConceptRequest (aDstConcept);
              }
            }
        }
      }
      ToopKafkaClient.send (EErrorLevel.INFO,
                            () -> sLogPrefix +
                                  "Finished mapping concepts to namespace '" +
                                  sDestinationMappingURI +
                                  "'.");
    }
    else
    {
      ToopKafkaClient.send (EErrorLevel.INFO,
                            () -> sLogPrefix + "No destination mapping URI provided, so no mapping executed.");
    }

    if (aErrors.isEmpty ())
    {
      sendTo_to_dp (aRequest);
    }

    if (aErrors.isNotEmpty ())
    {
      // We have errors
      final TDETOOPResponseType aResponseMsg = ToopMessageBuilder.createResponse (aRequest);
      aResponseMsg.getError ().addAll (aErrors);
      // Put the error in queue 3/4
      MessageProcessorDPOutgoing.getInstance ().enqueue (aResponseMsg);
    }
  }
}
