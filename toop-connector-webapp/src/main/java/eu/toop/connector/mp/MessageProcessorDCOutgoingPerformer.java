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
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.asic.AsicUtils;
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
import com.helger.commons.string.StringHelper;
import com.helger.commons.text.MultilingualText;
import com.helger.jaxb.validation.WrappedCollectingValidationEventHandler;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.schematron.svrl.AbstractSVRLMessage;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.concept.EConceptType;
import eu.toop.commons.dataexchange.v140.TDEConceptRequestType;
import eu.toop.commons.dataexchange.v140.TDEDataElementRequestType;
import eu.toop.commons.dataexchange.v140.TDEErrorType;
import eu.toop.commons.dataexchange.v140.TDERoutingInformationType;
import eu.toop.commons.dataexchange.v140.TDETOOPRequestType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.error.EToopErrorCategory;
import eu.toop.commons.error.EToopErrorCode;
import eu.toop.commons.error.EToopErrorOrigin;
import eu.toop.commons.error.EToopErrorSeverity;
import eu.toop.commons.error.IToopErrorCode;
import eu.toop.commons.error.ToopErrorException;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.commons.exchange.ToopRequestWithAttachments140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.commons.jaxb.ToopWriter;
import eu.toop.commons.jaxb.ToopXSDHelper140;
import eu.toop.commons.schematron.TOOPSchematron140Validator;
import eu.toop.commons.usecase.SMMDocumentTypeMapping;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.api.as4.MEPayload;
import eu.toop.connector.api.as4.MERoutingInformation;
import eu.toop.connector.api.as4.MessageExchangeManager;
import eu.toop.connector.r2d2client.IR2D2Endpoint;
import eu.toop.connector.r2d2client.R2D2Client;
import eu.toop.connector.smmclient.IMappedValueList;
import eu.toop.connector.smmclient.IUnmappableCallback;
import eu.toop.connector.smmclient.MappedValue;
import eu.toop.connector.smmclient.MappedValueList;
import eu.toop.connector.smmclient.SMMClient;
import eu.toop.kafkaclient.ToopKafkaClient;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.IdentifierType;

/**
 * The nested performer class that does the hard work in step 1/4.
 *
 * @author Philip Helger
 */
final class MessageProcessorDCOutgoingPerformer implements IConcurrentPerformer <ToopRequestWithAttachments140>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MessageProcessorDCOutgoingPerformer.class);

  @Nonnull
  private static TDEErrorType _createError (@Nonnull final IErrorLevel aErrorLevel,
                                            @Nonnull final String sLogPrefix,
                                            @Nonnull final EToopErrorCategory eCategory,
                                            @Nonnull final IToopErrorCode aErrorCode,
                                            @Nonnull final String sErrorText,
                                            @Nullable final Throwable t)
  {
    // Surely no DP here
    ToopKafkaClient.send (aErrorLevel, () -> sLogPrefix + "[" + aErrorCode.getID () + "] " + sErrorText, t);
    return ToopMessageBuilder140.createError (null,
                                              EToopErrorOrigin.REQUEST_SUBMISSION,
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

  private static void _iterateNonTCConcepts (@Nonnull final TDETOOPRequestType aRequest,
                                             @Nonnull final Consumer <TDEConceptRequestType> aConsumer)
  {
    for (final TDEDataElementRequestType aDER1 : aRequest.getDataElementRequest ())
    {
      final TDEConceptRequestType aConcept1 = aDER1.getConceptRequest ();

      // Only handle TC type codes
      if (!aConcept1.getSemanticMappingExecutionIndicator ().isValue () &&
          !EConceptType.TC.getID ().equals (aConcept1.getConceptTypeCode ().getValue ()))
      {
        aConsumer.accept (aConcept1);
      }
    }
  }

  public void runAsync (@Nonnull final ToopRequestWithAttachments140 aRequestWA)
  {
    final TDETOOPRequestType aRequest = aRequestWA.getRequest ();

    /*
     * This is the unique ID of this request message and must be used throughout
     * the whole process for identification
     */
    final String sRequestID = aRequest.getDocumentUniversalUniqueIdentifier () != null ? aRequest.getDocumentUniversalUniqueIdentifier ()
                                                                                                 .getValue ()
                                                                                       : "temp-tc1-id-" +
                                                                                         GlobalIDFactory.getNewIntID ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    final ICommonsList <TDEErrorType> aErrors = new CommonsArrayList <> ();

    ToopKafkaClient.send (EErrorLevel.INFO, () -> "Created new unique request ID [" + sRequestID + "]");
    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DC Request (1/4)");

    // Schematron validation
    if (TCConfig.isMPSchematronValidationEnabled ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Performing Schematron validation on incoming TOOP request");

      final ErrorList aErrorList = new ErrorList ();
      // XML creation
      final Document aDoc = ToopWriter.request140 ()
                                      .setValidationEventHandler (new WrappedCollectingValidationEventHandler (aErrorList))
                                      .getAsDocument (aRequest);
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
        final TOOPSchematron140Validator aValidator = new TOOPSchematron140Validator ();
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
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Finished Schematron validation with the following results: " + aErrorList);
    }
    else
    {
      ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Schematron validation was explicitly disabled.");
    }

    if (aErrors.isEmpty ())
    {
      final IIdentifierFactory aIF = TCSettings.getIdentifierFactory ();
      final TDERoutingInformationType aRoutingInfo = aRequest.getRoutingInformation ();
      final IParticipantIdentifier aSenderID = aRoutingInfo == null ? null
                                                                    : aIF.createParticipantIdentifier (aRoutingInfo.getDataConsumerElectronicAddressIdentifier ()
                                                                                                                   .getSchemeID (),
                                                                                                       aRoutingInfo.getDataConsumerElectronicAddressIdentifier ()
                                                                                                                   .getValue ());
      final IDocumentTypeIdentifier aDocTypeID = aRoutingInfo == null ? null
                                                                      : aIF.createDocumentTypeIdentifier (aRoutingInfo.getDocumentTypeIdentifier ()
                                                                                                                      .getSchemeID (),
                                                                                                          aRoutingInfo.getDocumentTypeIdentifier ()
                                                                                                                      .getValue ());
      final IProcessIdentifier aProcessID = aRoutingInfo == null ? null
                                                                 : aIF.createProcessIdentifier (aRoutingInfo.getProcessIdentifier ()
                                                                                                            .getSchemeID (),
                                                                                                aRoutingInfo.getProcessIdentifier ()
                                                                                                            .getValue ());

      // Select document type
      final EPredefinedDocumentTypeIdentifier eDocType = aRoutingInfo == null ? null
                                                                              : EPredefinedDocumentTypeIdentifier.getFromDocumentTypeIdentifierOrNull (aRoutingInfo.getDocumentTypeIdentifier ()
                                                                                                                                                                   .getSchemeID (),
                                                                                                                                                       aRoutingInfo.getDocumentTypeIdentifier ()
                                                                                                                                                                   .getValue ());
      if (eDocType == null)
      {
        final String sErrorMsg = "Failed to resolve document type " +
                                 (aRoutingInfo == null ? null
                                                       : aRoutingInfo.getDocumentTypeIdentifier ().getSchemeID () +
                                                         "::" +
                                                         aRoutingInfo.getDocumentTypeIdentifier ().getValue ());
        aErrors.add (_createError (sLogPrefix, EToopErrorCategory.PARSING, EToopErrorCode.IF_001, sErrorMsg, null));
      }
      else
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug (sLogPrefix + "Selected document type: " + eDocType);

        // Don't do this:
        // DataRequestIdentifier: "A reference to the universally unique
        // identifier of the corresponding Toop data request."
        // -> so set only in step 3/4
        if (false)
        {
          // Remember request ID
          aRequest.setDataRequestIdentifier (ToopXSDHelper140.createIdentifier (sRequestID));
        }

        // 1. invoke SMM
        // Map to TOOP concepts
        final SMMClient aSMMClient = new SMMClient ();
        _iterateNonTCConcepts (aRequest, c -> aSMMClient.addConceptToBeMapped (ConceptValue.create (c)));
        final int nConceptsToBeMapped = aSMMClient.getTotalCountConceptsToBeMapped ();

        if (LOGGER.isDebugEnabled ())
          LOGGER.debug (sLogPrefix + "A total of " + nConceptsToBeMapped + " concepts need to be mapped");

        // 0.10.3 - invoke SMM only when concepts are present
        if (nConceptsToBeMapped > 0)
        {
          // Main mapping
          IMappedValueList aMappedValues = null;
          try
          {
            // send back error if some value could not be mapped
            final String sSMMDomain = SMMDocumentTypeMapping.getToopSMDomainOrNull (eDocType);
            if (sSMMDomain == null)
            {
              // No SMM mapping for this document type
              aMappedValues = new MappedValueList ();
              ToopKafkaClient.send (EErrorLevel.INFO,
                                    () -> sLogPrefix +
                                          "Found no SMM document type mapping for document type " +
                                          eDocType);
            }
            else
            {
              final IUnmappableCallback aUnmappableCallback = (sLogPrefix1,
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
              };

              // Logging happens internally
              aMappedValues = aSMMClient.performMapping (sLogPrefix,
                                                         sSMMDomain,
                                                         MPWebAppConfig.getSMMConceptProvider (),
                                                         aUnmappableCallback);
            }
          }
          catch (final IllegalArgumentException | IOException ex)
          {
            // send back async error
            final String sErrorMsg = "Failed to invoke semantic mapping";
            aErrors.add (_createError (sLogPrefix,
                                       EToopErrorCategory.SEMANTIC_MAPPING,
                                       EToopErrorCode.SM_001,
                                       sErrorMsg,
                                       ex));
          }

          if (aErrors.isEmpty ())
          {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug (sLogPrefix + "Starting to add mapped SMM concepts to the TOOP request");

            final IMappedValueList aFinalMappedValues = aMappedValues;

            // add all the mapped values in the request
            _iterateNonTCConcepts (aRequest, c -> {
              // Now the source was mapped
              c.getSemanticMappingExecutionIndicator ().setValue (true);

              final ConceptValue aSrcCV = ConceptValue.create (c);
              for (final MappedValue aMV : aFinalMappedValues.getAllBySource (x -> x.equals (aSrcCV)))
              {
                final TDEConceptRequestType aToopConcept = new TDEConceptRequestType ();
                aToopConcept.setConceptTypeCode (ToopXSDHelper140.createCode (EConceptType.TC.getID ()));
                aToopConcept.setSemanticMappingExecutionIndicator (ToopXSDHelper140.createIndicator (false));
                aToopConcept.setConceptNamespace (ToopXSDHelper140.createIdentifier (aMV.getDestination ()
                                                                                        .getNamespace ()));
                aToopConcept.setConceptName (ToopXSDHelper140.createText (aMV.getDestination ().getValue ()));
                c.addConceptRequest (aToopConcept);
              }
            });

            ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Finished mapping to shared concept");
          }
        }

        ICommonsList <IR2D2Endpoint> aEndpoints = null;

        if (aErrors.isEmpty ())
        {
          // 2. invoke R2D2 client
          final String sTransportProfileID = TCConfig.getMEMProtocol ().getTransportProfileID ();

          final IdentifierType aExplicitQueryAddress = aRoutingInfo.getDataProviderElectronicAddressIdentifier ();
          final boolean bIsExplicitParticipant = aExplicitQueryAddress != null;
          if (bIsExplicitParticipant)
          {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug (sLogPrefix +
                            "Starting SMP lookup for an explicit participant: " +
                            aExplicitQueryAddress.toString ());

            // Query one participant only
            final IParticipantIdentifier aRecipientID = aIF.createParticipantIdentifier (aExplicitQueryAddress.getSchemeID (),
                                                                                         aExplicitQueryAddress.getValue ());

            // Find all endpoints of recipient
            try
            {
              aEndpoints = new R2D2Client ().getEndpoints (sLogPrefix,
                                                           aRecipientID,
                                                           aDocTypeID,
                                                           aProcessID,
                                                           sTransportProfileID);
            }
            catch (final ToopErrorException ex)
            {
              // send back async error
              aErrors.add (_createError (sLogPrefix,
                                         EToopErrorCategory.DYNAMIC_DISCOVERY,
                                         ex.getErrorCode (),
                                         ex.getMessage (),
                                         ex.getCause ()));
            }
          }
          else
          {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug (sLogPrefix + "Starting SMP lookup with country code and document type");

            // Find destination country code
            final String sDestinationCountryCode = aRoutingInfo.getDataProviderCountryCode ().getValue ();
            if (StringHelper.hasNoText (sDestinationCountryCode))
            {
              aErrors.add (_createError (sLogPrefix,
                                         EToopErrorCategory.DYNAMIC_DISCOVERY,
                                         EToopErrorCode.IF_001,
                                         "Failed to find destination country code to query!",
                                         null));
            }

            if (aErrors.isEmpty ())
            {
              // Find all endpoints by country
              try
              {
                aEndpoints = new R2D2Client ().getEndpoints (sLogPrefix,
                                                             sDestinationCountryCode,
                                                             aDocTypeID,
                                                             aProcessID,
                                                             sTransportProfileID);
              }
              catch (final ToopErrorException ex)
              {
                // send back async error
                aErrors.add (_createError (sLogPrefix,
                                           EToopErrorCategory.DYNAMIC_DISCOVERY,
                                           ex.getErrorCode (),
                                           ex.getMessage (),
                                           ex.getCause ()));
              }
            }
          }

          if (aErrors.isEmpty ())
          {
            final int nEnpointCount = aEndpoints.size ();
            ToopKafkaClient.send (EErrorLevel.INFO,
                                  () -> sLogPrefix +
                                        "R2D2 found " +
                                        nEnpointCount +
                                        " endpoints for " +
                                        (bIsExplicitParticipant ? "single participant" : "multi participant") +
                                        " lookup");
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug (sLogPrefix + "Endpoint details: " + aEndpoints);

            if (aEndpoints.isEmpty ())
            {
              aErrors.add (_createError (sLogPrefix,
                                         EToopErrorCategory.DYNAMIC_DISCOVERY,
                                         EToopErrorCode.DD_006,
                                         "Found no endpoints for transport profile '" +
                                                                sTransportProfileID +
                                                                "' by querying Directory and SMP",
                                         null));
            }
          }
        }

        if (aErrors.isEmpty ())
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug (sLogPrefix + "Started creating TOOP request ASIC container");

          // 3. start message exchange to DC
          // Combine MS data and TOOP data into a single ASiC message
          // Do this only once and not for every endpoint
          ByteArrayWrapper aPayloadBytes = null;
          try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
          {
            // Ensure flush/close of DumpOS!
            try (
                final OutputStream aDumpOS = TCDumpHelper.getDumpOutputStream (aBAOS,
                                                                               TCConfig.getDebugToDPDumpPathIfEnabled (),
                                                                               "to-dp.asic"))
            {
              ToopMessageBuilder140.createRequestMessageAsic (aRequest, aBAOS, MPWebAppConfig.getSignatureHelper ());
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

            if (LOGGER.isDebugEnabled ())
              LOGGER.debug (sLogPrefix + "Created TOOP request ASIC container has " + aPayloadBytes.size () + " bytes");
          }

          if (aErrors.isEmpty ())
          {
            // build MEM once
            final MEPayload aPayload = new MEPayload (AsicUtils.MIMETYPE_ASICE, sRequestID, aPayloadBytes);
            final MEMessage aMEMessage = MEMessage.create (aPayload);

            // For all matching endpoints
            for (final IR2D2Endpoint aEP : aEndpoints)
            {
              ToopKafkaClient.send (EErrorLevel.INFO,
                                    () -> sLogPrefix +
                                          "Sending MEM message to '" +
                                          aEP.getEndpointURL () +
                                          "' using transport protocol '" +
                                          aEP.getTransportProtocol () +
                                          "'");

              // Message exchange information
              final MERoutingInformation aMERoutingInfo = new MERoutingInformation (aSenderID,
                                                                                    aEP.getParticipantID (),
                                                                                    aDocTypeID,
                                                                                    aProcessID,
                                                                                    aEP.getTransportProtocol (),
                                                                                    aEP.getEndpointURL (),
                                                                                    aEP.getCertificate ());
              try
              {
                // Main message exchange
                MessageExchangeManager.getConfiguredImplementation ().sendDCOutgoing (aMERoutingInfo, aMEMessage);

                if (LOGGER.isDebugEnabled ())
                  LOGGER.debug (sLogPrefix + "sendDCOutgoing returned without exception");
              }
              catch (final MEException ex)
              {
                aErrors.add (_createError (sLogPrefix,
                                           EToopErrorCategory.E_DELIVERY,
                                           EToopErrorCode.ME_001,
                                           "Error sending message",
                                           ex));
              }

              /*
               * XXX just send to the first one, to mimic, that this is how it
               * will be in the final version (where step 4/4 will aggregate)
               */
              break;
            }
          }
        }
      }
    }

    final int nErrorCount = aErrors.size ();
    if (nErrorCount > 0)
    {
      // We have errors

      ToopKafkaClient.send (EErrorLevel.INFO,
                            () -> sLogPrefix + nErrorCount + " error(s) were found - directly pushing to queue 4/4.");

      final TDETOOPResponseType aResponseMsg = ToopMessageBuilder140.createResponse (aRequest);
      MPHelper.fillDefaultResponseFields (aResponseMsg);

      // Wrap with source attachments
      aResponseMsg.getError ().addAll (aErrors);

      final ToopResponseWithAttachments140 aResponse = new ToopResponseWithAttachments140 (aResponseMsg,
                                                                                           aRequestWA.attachments ());
      // Put the error in queue 4/4
      MessageProcessorDCIncoming.getInstance ().enqueue (aResponse);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "End of processing");
  }
}
