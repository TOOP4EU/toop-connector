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
import eu.toop.commons.codelist.SMMDocumentTypeMapping;
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
import eu.toop.commons.error.ToopErrorException;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.commons.jaxb.ToopWriter;
import eu.toop.commons.jaxb.ToopXSDHelper;
import eu.toop.commons.schematron.TOOPSchematronValidator;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.api.as4.MEPayload;
import eu.toop.connector.api.as4.MERoutingInformation;
import eu.toop.connector.me.EActingSide;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.r2d2client.IR2D2Endpoint;
import eu.toop.connector.r2d2client.R2D2Client;
import eu.toop.connector.smmclient.IMappedValueList;
import eu.toop.connector.smmclient.MappedValue;
import eu.toop.connector.smmclient.SMMClient;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The nested performer class that does the hard work in step 1/4.
 *
 * @author Philip Helger
 */
final class MessageProcessorDCOutgoingPerformer implements IConcurrentPerformer <TDETOOPRequestType>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MessageProcessorDCOutgoingPerformer.class);

  @Nonnull
  private static TDEErrorType _createError (@Nonnull final IErrorLevel aErrorLevel,
                                            @Nonnull final String sLogPrefix,
                                            @Nonnull final EToopErrorCategory eCategory,
                                            @Nonnull final EToopErrorCode eErrorCode,
                                            @Nonnull final String sErrorText,
                                            @Nullable final Throwable t)
  {
    // Surely no DP here
    ToopKafkaClient.send (aErrorLevel, () -> sLogPrefix + "[" + eErrorCode.getID () + "] " + sErrorText);
    return ToopMessageBuilder.createError (null,
                                           EToopErrorOrigin.REQUEST_SUBMISSION,
                                           eCategory,
                                           eErrorCode,
                                           aErrorLevel.isError () ? EToopErrorSeverity.FAILURE
                                                                  : EToopErrorSeverity.WARNING,
                                           new MultilingualText (Locale.US, sErrorText),
                                           t == null ? null : StackTraceHelper.getStackAsString (t));
  }

  @Nonnull
  private static TDEErrorType _createError (@Nonnull final String sLogPrefix,
                                            @Nonnull final EToopErrorCategory eCategory,
                                            @Nonnull final EToopErrorCode eErrorCode,
                                            @Nonnull final String sErrorText,
                                            @Nullable final Throwable t)
  {
    return _createError (EErrorLevel.ERROR, sLogPrefix, eCategory, eErrorCode, sErrorText, t);
  }

  @Nonnull
  private static TDEErrorType _createGenericError (@Nonnull final String sLogPrefix, @Nonnull final Throwable t)
  {
    return _createError (sLogPrefix, EToopErrorCategory.TECHNICAL_ERROR, EToopErrorCode.GEN, t.getMessage (), t);
  }

  public void runAsync (@Nonnull final TDETOOPRequestType aRequest)
  {
    /*
     * This is the unique ID of this request message and must be used throughout the
     * whole process for identification
     */
    final String sRequestID = GlobalIDFactory.getNewPersistentStringID ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    final ICommonsList <TDEErrorType> aErrors = new CommonsArrayList <> ();

    // Schematron validation
    {
      final ErrorList aErrorList = new ErrorList ();
      // XML creation
      final Document aDoc = ToopWriter.request ()
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

    final IIdentifierFactory aIF = TCSettings.getIdentifierFactory ();
    final TDERoutingInformationType aRoutingInfo = aRequest.getRoutingInformation ();
    final IParticipantIdentifier aSenderID = aRoutingInfo == null ? null
                                                                  : aIF.createParticipantIdentifier (aRequest.getDataConsumer ()
                                                                                                             .getDCElectronicAddressIdentifier ()
                                                                                                             .getSchemeID (),
                                                                                                     aRequest.getDataConsumer ()
                                                                                                             .getDCElectronicAddressIdentifier ()
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
      // Remember request ID
      aRequest.setDataRequestIdentifier (ToopXSDHelper.createIdentifier (sRequestID));

      ToopKafkaClient.send (EErrorLevel.INFO, () -> "Created new unique request ID [" + sRequestID + "]");
      ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DC Request (1/4)");

      // 1. invoke SMM
      {
        // Map to TOOP concepts
        final SMMClient aClient = new SMMClient ();
        for (final TDEDataElementRequestType aDER : aRequest.getDataElementRequest ())
        {
          final TDEConceptRequestType aSrcConcept = aDER.getConceptRequest ();
          // Only if not yet mapped
          if (!aSrcConcept.getSemanticMappingExecutionIndicator ().isValue ())
          {
            aClient.addConceptToBeMapped (ConceptValue.create (aSrcConcept));
          }
        }

        // Main mapping
        IMappedValueList aMappedValues = null;
        try
        {
          // send back error if some value could not be mapped
          aMappedValues = aClient.performMapping (sLogPrefix,
                                                  SMMDocumentTypeMapping.getToopSMNamespace (eDocType),
                                                  MPWebAppConfig.getSMMConceptProvider (),
                                                  (sLogPrefix1, sSourceNamespace, sSourceValue, sDestNamespace) -> {
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
        }
        catch (final IOException ex)
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
          // add all the mapped values in the request
          for (final TDEDataElementRequestType aDER : aRequest.getDataElementRequest ())
          {
            final TDEConceptRequestType aSrcConcept = aDER.getConceptRequest ();
            if (!aSrcConcept.getSemanticMappingExecutionIndicator ().isValue ())
            {
              // Now the source was mapped
              aSrcConcept.getSemanticMappingExecutionIndicator ().setValue (true);

              final ConceptValue aSrcCV = ConceptValue.create (aSrcConcept);
              for (final MappedValue aMV : aMappedValues.getAllBySource (x -> x.equals (aSrcCV)))
              {
                final TDEConceptRequestType aToopConcept = new TDEConceptRequestType ();
                aToopConcept.setConceptTypeCode (ToopXSDHelper.createCode (EConceptType.TC.getID ()));
                aToopConcept.setSemanticMappingExecutionIndicator (ToopXSDHelper.createIndicator (false));
                aToopConcept.setConceptNamespace (ToopXSDHelper.createIdentifier (aMV.getDestination ()
                                                                                     .getNamespace ()));
                aToopConcept.setConceptName (ToopXSDHelper.createText (aMV.getDestination ().getValue ()));
                aSrcConcept.addConceptRequest (aToopConcept);
              }
            }
          }
        }
      }

      ICommonsList <IR2D2Endpoint> aEndpoints = null;

      if (aErrors.isEmpty ())
      {
        // 2. invoke R2D2 client

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
          ICommonsList <IR2D2Endpoint> aTotalEndpoints = null;
          try
          {
            aTotalEndpoints = new R2D2Client ().getEndpoints (sLogPrefix,
                                                              sDestinationCountryCode,
                                                              aDocTypeID,
                                                              aProcessID);
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

          if (aErrors.isEmpty ())
          {
            // Filter all endpoints with the corresponding transport profile
            final String sTransportProfileID = TCConfig.getMEMProtocol ().getTransportProfileID ();
            aEndpoints = aTotalEndpoints.getAll (x -> x.getTransportProtocol ().equals (sTransportProfileID));

            ToopKafkaClient.send (EErrorLevel.INFO,
                                  sLogPrefix +
                                                    "R2D2 found [" +
                                                    aEndpoints.size () +
                                                    "/" +
                                                    aTotalEndpoints.size () +
                                                    "] endpoints");
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug (sLogPrefix + "Endpoint details: " + aEndpoints);

            if (aTotalEndpoints.isEmpty ())
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
      }

      if (aErrors.isEmpty ())
      {
        // 3. start message exchange to DC
        // Combine MS data and TOOP data into a single ASiC message
        // Do this only once and not for every endpoint
        ByteArrayWrapper aPayloadBytes = null;
        try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
        {
          // Ensure flush/close of DumpOS!
          try (final OutputStream aDumpOS = TCDumpHelper.getDumpOutputStream (aBAOS,
                                                                              TCConfig.getDebugToDPDumpPathIfEnabled (),
                                                                              "to-dp.asic"))
          {
            ToopMessageBuilder.createRequestMessageAsic (aRequest, aBAOS, MPWebAppConfig.getSignatureHelper ());
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

          // For all matching endpoints
          for (final IR2D2Endpoint aEP : aEndpoints)
          {
            ToopKafkaClient.send (EErrorLevel.INFO,
                                  sLogPrefix +
                                                    "Sending MEM message to '" +
                                                    aEP.getEndpointURL () +
                                                    "' using transport protocol '" +
                                                    aEP.getTransportProtocol () +
                                                    "'");

            if (false)
              new MERoutingInformation (aSenderID,
                                        aEP.getParticipantID (),
                                        aDocTypeID,
                                        aProcessID,
                                        aEP.getTransportProtocol (),
                                        aEP.getEndpointURL (),
                                        aEP.getCertificate ());

            final GatewayRoutingMetadata aGRM = new GatewayRoutingMetadata (aSenderID.getURIEncoded (),
                                                                            aDocTypeID.getURIEncoded (),
                                                                            aProcessID.getURIEncoded (),
                                                                            aEP.getEndpointURL (),
                                                                            aEP.getCertificate (),
                                                                            EActingSide.DC);
            try
            {
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

            /*
             * XXX just send to the first one, to mimic, that this is how it will be in the
             * final version (where step 4/4 will aggregate)
             */
            break;
          }
        }
      }
    }

    if (aErrors.isNotEmpty ())
    {
      final TDETOOPResponseType aResponseMsg = ToopMessageBuilder.createResponse (aRequest);
      aResponseMsg.getError ().addAll (aErrors);
      // Put the error in queue 4/4
      MessageProcessorDCIncoming.getInstance ().enqueue (aResponseMsg);
    }
  }
}
