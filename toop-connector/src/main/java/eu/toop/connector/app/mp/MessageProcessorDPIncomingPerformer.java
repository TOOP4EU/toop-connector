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
package eu.toop.connector.app.mp;

import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.error.level.IErrorLevel;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.text.MultilingualText;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.concept.EConceptType;
import eu.toop.commons.dataexchange.v140.TDEConceptRequestType;
import eu.toop.commons.dataexchange.v140.TDEDataElementRequestType;
import eu.toop.commons.dataexchange.v140.TDEErrorType;
import eu.toop.commons.dataexchange.v140.TDETOOPRequestType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.error.EToopErrorCategory;
import eu.toop.commons.error.EToopErrorCode;
import eu.toop.commons.error.EToopErrorOrigin;
import eu.toop.commons.error.EToopErrorSeverity;
import eu.toop.commons.error.IToopErrorCode;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.commons.exchange.ToopRequestWithAttachments140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.commons.jaxb.ToopXSDHelper140;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.smm.IMappedValueList;
import eu.toop.connector.api.smm.ISMMClient;
import eu.toop.connector.api.smm.ISMMMultiMappingCallback;
import eu.toop.connector.api.smm.ISMMUnmappableCallback;
import eu.toop.connector.api.smm.MappedValue;
import eu.toop.connector.app.smm.SMMClient;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The nested performer class that does the hard work in step 2/4.
 *
 * @author Philip Helger
 */
final class MessageProcessorDPIncomingPerformer implements IConcurrentPerformer <ToopRequestWithAttachments140>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MessageProcessorDPIncomingPerformer.class);

  @Nonnull
  private static TDEErrorType _createError (@Nonnull final IErrorLevel aErrorLevel,
                                            @Nonnull final String sLogPrefix,
                                            @Nonnull final EToopErrorCategory eCategory,
                                            @Nonnull final IToopErrorCode aErrorCode,
                                            @Nonnull final String sErrorText,
                                            @Nullable final Throwable t)
  {
    ToopKafkaClient.send (aErrorLevel, () -> sLogPrefix + "[" + aErrorCode.getID () + "] " + sErrorText, t);
    return ToopMessageBuilder140.createError (null,
                                              EToopErrorOrigin.REQUEST_RECEPTION,
                                              eCategory,
                                              aErrorCode,
                                              EToopErrorSeverity.FAILURE,
                                              new MultilingualText (Locale.US, sErrorText),
                                              t == null ? null : StackTraceHelper.getStackAsString (t));
  }

  @Nonnull
  private static TDEErrorType _createGenericError (@Nonnull final String sLogPrefix, @Nonnull final Throwable t)
  {
    return _createError (EErrorLevel.ERROR,
                         sLogPrefix,
                         EToopErrorCategory.TECHNICAL_ERROR,
                         EToopErrorCode.GEN,
                         t.getMessage (),
                         t);
  }

  private static void _iterateTCConcepts (@Nonnull final TDETOOPRequestType aRequest,
                                          @Nonnull final Consumer <TDEConceptRequestType> aConsumer)
  {
    for (final TDEDataElementRequestType aDER1 : aRequest.getDataElementRequest ())
    {
      final TDEConceptRequestType aConcept1 = aDER1.getConceptRequest ();

      // Only handle TC type codes
      if (!aConcept1.getSemanticMappingExecutionIndicator ().isValue () &&
          EConceptType.TC.getID ().equals (aConcept1.getConceptTypeCode ().getValue ()))
      {
        aConsumer.accept (aConcept1);
      }
      else
      {
        // Descend one level
        for (final TDEConceptRequestType aConcept2 : aConcept1.getConceptRequest ())
          // Only if not yet mapped
          if (!aConcept2.getSemanticMappingExecutionIndicator ().isValue () &&
              EConceptType.TC.getID ().equals (aConcept2.getConceptTypeCode ().getValue ()))
          {
            aConsumer.accept (aConcept2);
          }
      }
    }
  }

  public void runAsync (@Nonnull final ToopRequestWithAttachments140 aRequestWA) throws Exception
  {
    // Create a clone for modification
    final TDETOOPRequestType aRequest = aRequestWA.getRequest ().clone ();

    final String sRequestID = aRequest.getDocumentUniversalUniqueIdentifier () != null ? aRequest.getDocumentUniversalUniqueIdentifier ()
                                                                                                 .getValue ()
                                                                                       : "temp-tc2-id-" +
                                                                                         GlobalIDFactory.getNewIntID ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    final ICommonsList <TDEErrorType> aErrors = new CommonsArrayList <> ();

    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DP Incoming Request (2/4)");

    // Map to DP concepts
    // TODO make it dependent on requested document type
    final String sDestinationMappingURI = TCConfig.getSMMMappingNamespaceURIForDP ();
    if (StringHelper.hasText (sDestinationMappingURI))
    {
      final ISMMClient aSMMClient = new SMMClient ();
      _iterateTCConcepts (aRequest, c -> aSMMClient.addConceptToBeMapped (ConceptValue.create (c)));
      final int nConceptsToBeMapped = aSMMClient.getTotalCountConceptsToBeMapped ();

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "A total of " + nConceptsToBeMapped + " concepts need to be mapped");

      // 0.10.3 - invoke SMM only when concepts are present
      if (nConceptsToBeMapped > 0)
      {
        // Main mapping
        ToopKafkaClient.send (EErrorLevel.INFO,
                              () -> sLogPrefix +
                                    "SMM client is mapping " +
                                    aSMMClient.getTotalCountConceptsToBeMapped () +
                                    " concept(s) to namespace '" +
                                    sDestinationMappingURI +
                                    "'");

        final boolean bUnmappableConceptLeadsToError = TCConfig.isSMMDPMappingErrorFatal ();
        final ISMMUnmappableCallback aUnmappableCallback = (sLogPrefix1,
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
          if (bUnmappableConceptLeadsToError)
          {
            aErrors.add (_createError (EErrorLevel.ERROR,
                                       sLogPrefix1,
                                       EToopErrorCategory.SEMANTIC_MAPPING,
                                       EToopErrorCode.SM_002,
                                       sErrorMsg,
                                       null));
          }
          else
          {
            if (LOGGER.isWarnEnabled ())
              LOGGER.warn (sLogPrefix1 + sErrorMsg + " (continuing anyway)");

            ToopKafkaClient.send (EErrorLevel.WARN, () -> sLogPrefix1 + sErrorMsg);
          }
        };
        final ISMMMultiMappingCallback aMultiMappingCallback = (sLogPrefix1,
                                                                sSourceNamespace,
                                                                sSourceValue,
                                                                sDestNamespace,
                                                                aMatching) -> ToopKafkaClient.send (EErrorLevel.WARN,
                                                                                                    () -> sLogPrefix1 +
                                                                                                          "Found " +
                                                                                                          aMatching.size () +
                                                                                                          " mappings for '" +
                                                                                                          sSourceNamespace +
                                                                                                          '#' +
                                                                                                          sSourceValue +
                                                                                                          "' to destination namespace '" +
                                                                                                          sDestNamespace +
                                                                                                          "'");

        final IMappedValueList aMappedValues = aSMMClient.performMapping (sLogPrefix,
                                                                          sDestinationMappingURI,
                                                                          MPConfig.getSMMConceptProvider (),
                                                                          aUnmappableCallback,
                                                                          aMultiMappingCallback);

        ToopKafkaClient.send (EErrorLevel.INFO,
                              () -> sLogPrefix + "SMM client mapping found " + aMappedValues.size () + " mapping(s)");

        // add all the mapped values in the request
        _iterateTCConcepts (aRequest, c -> {
          // Now the toop concept was mapped
          c.getSemanticMappingExecutionIndicator ().setValue (true);

          // Add all mapped values as child concepts
          final ConceptValue aToopCV = ConceptValue.create (c);
          for (final MappedValue aMV : aMappedValues.getAllBySource (x -> x.equals (aToopCV)))
          {
            final TDEConceptRequestType aDstConcept = new TDEConceptRequestType ();
            aDstConcept.setConceptTypeCode (ToopXSDHelper140.createCode (EConceptType.DP.getID ()));
            aDstConcept.setSemanticMappingExecutionIndicator (ToopXSDHelper140.createIndicator (false));
            aDstConcept.setConceptNamespace (ToopXSDHelper140.createIdentifier (aMV.getDestination ().getNamespace ()));
            aDstConcept.setConceptName (ToopXSDHelper140.createText (aMV.getDestination ().getValue ()));
            c.addConceptRequest (aDstConcept);
          }
        });
        ToopKafkaClient.send (EErrorLevel.INFO,
                              () -> sLogPrefix +
                                    "Finished mapping concepts to namespace '" +
                                    sDestinationMappingURI +
                                    "'.");
      }
    }
    else
    {
      ToopKafkaClient.send (EErrorLevel.INFO,
                            () -> sLogPrefix + "No destination mapping URI provided, so no mapping executed.");
    }

    if (aErrors.isEmpty ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "No errors found. Now forwarding to the DP");

      if (MPConfig.getToDP ()
                  .passRequestOnToDP (new ToopRequestWithAttachments140 (aRequest, aRequestWA.attachments ()))
                  .isFailure ())
      {
        aErrors.add (_createError (EErrorLevel.ERROR,
                                   sLogPrefix,
                                   EToopErrorCategory.E_DELIVERY,
                                   EToopErrorCode.GEN,
                                   "Error sending request to DP",
                                   null));
      }
    }

    final int nErrorCount = aErrors.size ();
    if (nErrorCount > 0)
    {
      // We have errors

      ToopKafkaClient.send (EErrorLevel.INFO,
                            () -> sLogPrefix + nErrorCount + " error(s) were found - directly pushing to queue 3/4.");

      final TDETOOPResponseType aResponseMsg = ToopMessageBuilder140.createResponse (aRequest);
      MPHelper.fillDefaultResponseFields (sLogPrefix, aResponseMsg);
      // add all errors
      aResponseMsg.getError ().addAll (aErrors);

      // Wrap with source attachments
      final ToopResponseWithAttachments140 aResponse = new ToopResponseWithAttachments140 (aResponseMsg,
                                                                                           aRequestWA.attachments ());
      // Put the error in queue 3/4
      MessageProcessorDPOutgoing.getInstance ().enqueue (aResponse);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "End of processing");
  }
}
