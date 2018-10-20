/**
 * Copyright (C) 2018 toop.eu
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

import com.helger.asic.AsicUtils;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.text.MultilingualText;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.SMMDocumentTypeMapping;
import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.concept.EConceptType;
import eu.toop.commons.dataexchange.TDEConceptRequestType;
import eu.toop.commons.dataexchange.TDEDataElementRequestType;
import eu.toop.commons.dataexchange.TDEErrorType;
import eu.toop.commons.dataexchange.TDELegalEntityType;
import eu.toop.commons.dataexchange.TDENaturalPersonType;
import eu.toop.commons.dataexchange.TDETOOPErrorMessageType;
import eu.toop.commons.dataexchange.TDETOOPRequestType;
import eu.toop.commons.error.EToopErrorCategory;
import eu.toop.commons.error.EToopErrorCode;
import eu.toop.commons.error.EToopErrorOrigin;
import eu.toop.commons.error.EToopErrorSeverity;
import eu.toop.commons.error.ToopErrorException;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.commons.jaxb.ToopXSDHelper;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.me.EActingSide;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.MEPayload;
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
  private static TDEErrorType _createError (@Nonnull final String sLogPrefix,
                                            @Nonnull final EToopErrorCategory eCategory,
                                            @Nonnull final EToopErrorCode eErrorCode,
                                            @Nonnull final String sErrorText,
                                            @Nullable final Throwable t)
  {
    // Surely no DP here
    ToopKafkaClient.send (EErrorLevel.ERROR, () -> sLogPrefix + "[" + eErrorCode.getID () + "] " + sErrorText);
    return ToopMessageBuilder.createError (null,
                                           EToopErrorOrigin.REQUEST_SUBMISSION,
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

  public void runAsync (@Nonnull final TDETOOPRequestType aRequest)
  {
    /*
     * This is the unique ID of this request message and must be used throughout
     * the whole process for identification
     */
    final String sRequestID = GlobalIDFactory.getNewPersistentStringID ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    final ICommonsList <TDEErrorType> aErrors = new CommonsArrayList <> ();

    // TODO Schematron

    // Select document type
    final EPredefinedDocumentTypeIdentifier eDocType = EPredefinedDocumentTypeIdentifier.getFromDocumentTypeIdentifierOrNull (aRequest.getDocumentTypeIdentifier ()
                                                                                                                                      .getSchemeID (),
                                                                                                                              aRequest.getDocumentTypeIdentifier ()
                                                                                                                                      .getValue ());
    if (eDocType == null)
    {
      final String sErrorMsg = "Failed to resolve document type " +
                               aRequest.getDocumentTypeIdentifier ().getSchemeID () +
                               "::" +
                               aRequest.getDocumentTypeIdentifier ().getValue ();
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
      final IIdentifierFactory aIF = TCSettings.getIdentifierFactory ();
      final IParticipantIdentifier aSenderID = aIF.createParticipantIdentifier (aRequest.getDataConsumer ()
                                                                                        .getDCElectronicAddressIdentifier ()
                                                                                        .getSchemeID (),
                                                                                aRequest.getDataConsumer ()
                                                                                        .getDCElectronicAddressIdentifier ()
                                                                                        .getValue ());
      final IDocumentTypeIdentifier aDocTypeID = aIF.createDocumentTypeIdentifier (aRequest.getDocumentTypeIdentifier ()
                                                                                           .getSchemeID (),
                                                                                   aRequest.getDocumentTypeIdentifier ()
                                                                                           .getValue ());
      final IProcessIdentifier aProcessID = aIF.createProcessIdentifier (aRequest.getProcessIdentifier ()
                                                                                 .getSchemeID (),
                                                                         aRequest.getProcessIdentifier ().getValue ());

      if (aErrors.isEmpty ())
      {
        // 2. invoke R2D2 client

        // Find destination country code
        String sDestinationCountryCode = null;
        final TDELegalEntityType aLegalEntity = aRequest.getDataRequestSubject ().getLegalEntity ();
        if (aLegalEntity != null)
          sDestinationCountryCode = aLegalEntity.getLegalEntityLegalAddress ().getCountryCode ().getValue ();
        if (StringHelper.hasNoText (sDestinationCountryCode))
        {
          final TDENaturalPersonType aNaturalPerson = aRequest.getDataRequestSubject ().getNaturalPerson ();
          if (aNaturalPerson != null)
            sDestinationCountryCode = aNaturalPerson.getNaturalPersonLegalAddress ().getCountryCode ().getValue ();
        }
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
        MEMessage aMEMessage;
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

          // build MEM once
          final MEPayload aPayload = new MEPayload (AsicUtils.MIMETYPE_ASICE, sRequestID, aBAOS.toByteArray ());
          aMEMessage = new MEMessage (aPayload);
        }

        if (aErrors.isEmpty ())
        {
          // For all matching endpoints
          for (final IR2D2Endpoint aEP : aEndpoints)
          {
            final GatewayRoutingMetadata aMetadata = new GatewayRoutingMetadata (aSenderID.getURIEncoded (),
                                                                                 aDocTypeID.getURIEncoded (),
                                                                                 aProcessID.getURIEncoded (),
                                                                                 aEP,
                                                                                 EActingSide.DC);
            ToopKafkaClient.send (EErrorLevel.INFO,
                                  sLogPrefix +
                                                    "Sending MEM message to '" +
                                                    aEP.getEndpointURL () +
                                                    "' using transport protocol '" +
                                                    aEP.getTransportProtocol () +
                                                    "'");

            if (!MEMDelegate.getInstance ().sendMessage (aMetadata, aMEMessage))
            {
              aErrors.add (_createError (sLogPrefix,
                                         EToopErrorCategory.E_DELIVERY,
                                         EToopErrorCode.ME_001,
                                         "Error sending message",
                                         null));
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

    if (aErrors.isNotEmpty ())
    {
      final TDETOOPErrorMessageType aErrorMsg = ToopMessageBuilder.createErrorMessage (aRequest);
      aErrorMsg.getError ().addAll (aErrors);
      MessageProcessorDCIncoming.getInstance ().enqueue (aErrorMsg);
    }
  }
}
