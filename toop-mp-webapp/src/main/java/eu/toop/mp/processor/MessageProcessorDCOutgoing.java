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
package eu.toop.mp.processor;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.asic.AsicUtils;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.BasicThreadFactory;
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.dataexchange.TDEConceptRequestType;
import eu.toop.commons.dataexchange.TDEDataElementRequestType;
import eu.toop.commons.dataexchange.TDELegalEntityType;
import eu.toop.commons.dataexchange.TDENaturalPersonType;
import eu.toop.commons.dataexchange.TDETOOPDataRequestType;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.commons.jaxb.ToopXSDHelper;
import eu.toop.kafkaclient.ToopKafkaClient;
import eu.toop.mp.api.CMP;
import eu.toop.mp.api.MPConfig;
import eu.toop.mp.api.MPSettings;
import eu.toop.mp.me.GatewayRoutingMetadata;
import eu.toop.mp.me.MEMDelegate;
import eu.toop.mp.me.MEMessage;
import eu.toop.mp.me.MEPayload;
import eu.toop.mp.r2d2client.IR2D2Endpoint;
import eu.toop.mp.r2d2client.R2D2Client;
import eu.toop.mp.smmclient.IMappedValueList;
import eu.toop.mp.smmclient.MappedValue;
import eu.toop.mp.smmclient.SMMClient;

/**
 * The global message processor that handles DC to DP (=DC outgoing) requests
 * (step 1/4). This is only the queue and it spawns external threads for
 * processing the incoming data.
 *
 * @author Philip Helger
 */
public final class MessageProcessorDCOutgoing extends AbstractGlobalWebSingleton {
  protected static final Logger s_aLogger = LoggerFactory.getLogger (MessageProcessorDCOutgoing.class);

  /**
   * The nested performer class that does the hard work.
   *
   * @author Philip Helger
   */
  static final class Performer implements IConcurrentPerformer<TDETOOPDataRequestType> {
    public void runAsync (@Nonnull final TDETOOPDataRequestType aCurrentObject) throws Exception {
      // This is the unique ID of this request message and must be used throughout the
      // whole process for identification
      final String sRequestID = GlobalIDFactory.getNewPersistentStringID () + UUID.randomUUID ().toString ();
      final String sLogPrefix = "[" + sRequestID + "] ";

      ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received asynch request: " + aCurrentObject);
      // 1. invoke SMM
      {
        // Map to TOOP concepts
        final SMMClient aClient = new SMMClient ();
        for (final TDEDataElementRequestType aDER : aCurrentObject.getDataElementRequest ()) {
          final TDEConceptRequestType aSrcConcept = aDER.getConceptRequest ();
          // Only if not yet mapped
          if (!aSrcConcept.getSemanticMappingExecutionIndicator ().isValue ()) {
            aClient.addConceptToBeMapped (ConceptValue.create (aSrcConcept));
          }
        }

        // Main mapping
        // TODO make destination namespace configurable
        final IMappedValueList aMappedValues = aClient.performMapping (sLogPrefix, CMP.NS_TOOP);

        // add all the mapped values in the request
        for (final TDEDataElementRequestType aDER : aCurrentObject.getDataElementRequest ()) {
          final TDEConceptRequestType aSrcConcept = aDER.getConceptRequest ();
          if (!aSrcConcept.getSemanticMappingExecutionIndicator ().isValue ()) {
            // Now the source was mapped
            aSrcConcept.getSemanticMappingExecutionIndicator ().setValue (true);

            final ConceptValue aSrcCV = ConceptValue.create (aSrcConcept);
            for (final MappedValue aMV : aMappedValues.getAllBySource (x -> x.equals (aSrcCV))) {
              final TDEConceptRequestType aToopConcept = new TDEConceptRequestType ();
              aToopConcept.setConceptTypeCode (ToopXSDHelper.createCode ("TOOP"));
              aToopConcept.setSemanticMappingExecutionIndicator (ToopXSDHelper.createIndicator (false));
              aToopConcept.setConceptNamespace (ToopXSDHelper.createIdentifier (aMV.getDestination ().getNamespace ()));
              aToopConcept.setConceptName (ToopXSDHelper.createText (aMV.getDestination ().getValue ()));
              aSrcConcept.addConceptRequest (aToopConcept);
            }
          }
        }
      }

      // 2. invoke R2D2 client
      final ICommonsList<IR2D2Endpoint> aEndpoints;
      final IParticipantIdentifier aSenderID = MPSettings.getIdentifierFactory ()
                                                         .createParticipantIdentifier (aCurrentObject.getDataConsumer ()
                                                                                                     .getDCElectronicAddressIdentifier ()
                                                                                                     .getSchemeID (),
                                                                                       aCurrentObject.getDataConsumer ()
                                                                                                     .getDCElectronicAddressIdentifier ()
                                                                                                     .getValue ());
      final IDocumentTypeIdentifier aDocTypeID = MPSettings.getIdentifierFactory ()
                                                           .createDocumentTypeIdentifier (aCurrentObject.getDocumentTypeIdentifier ()
                                                                                                        .getSchemeID (),
                                                                                          aCurrentObject.getDocumentTypeIdentifier ()
                                                                                                        .getValue ());
      final IProcessIdentifier aProcessID = MPSettings.getIdentifierFactory ()
                                                      .createProcessIdentifier (aCurrentObject.getProcessIdentifier ()
                                                                                              .getSchemeID (),
                                                                                aCurrentObject.getProcessIdentifier ()
                                                                                              .getValue ());
      {

        String sDestinationCountryCode = null;
        final TDELegalEntityType aLegalEntity = aCurrentObject.getDataSubject ().getLegalEntity ();
        if (aLegalEntity != null)
          sDestinationCountryCode = aLegalEntity.getLegalEntityLegalAddress ().getCountryCode ().getValue ();
        if (StringHelper.hasNoText (sDestinationCountryCode)) {
          final TDENaturalPersonType aNaturalPerson = aCurrentObject.getDataSubject ().getNaturalPerson ();
          if (aNaturalPerson != null)
            sDestinationCountryCode = aNaturalPerson.getNaturalPersonLegalAddress ().getCountryCode ().getValue ();
        }
        if (StringHelper.hasNoText (sDestinationCountryCode))
          throw new IllegalStateException ("Failed to find destination country code to query!");

        final ICommonsList<IR2D2Endpoint> aTotalEndpoints = new R2D2Client ().getEndpoints (sDestinationCountryCode,
                                                                                            aDocTypeID, aProcessID);

        // Filter all endpoints with the corresponding transport profile
        final String sTransportProfileID = MPConfig.getMEMProtocol ().getTransportProfileID ();
        aEndpoints = aTotalEndpoints.getAll (x -> x.getTransportProtocol ().equals (sTransportProfileID));

        ToopKafkaClient.send (EErrorLevel.INFO, sLogPrefix + "R2D2 found the following endpoints[" + aEndpoints.size ()
                                                + "/" + aTotalEndpoints.size () + "]: " + aEndpoints);
      }

      // 3. start message exchange to DC
      {
        // Combine MS data and TOOP data into a single ASiC message
        // Do this only once and not for every endpoint
        MEMessage meMessage;
        try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ()) {
          ToopMessageBuilder.createRequestMessage (aCurrentObject, aBAOS, MPWebAppConfig.getSignatureHelper ());

          // build MEM once
          final MEPayload aPayload = new MEPayload (AsicUtils.MIMETYPE_ASICE, sRequestID, aBAOS.toByteArray ());
          meMessage = new MEMessage (aPayload);
        }

        // For all matching endpoints
        for (final IR2D2Endpoint aEP : aEndpoints) {
          final GatewayRoutingMetadata metadata = new GatewayRoutingMetadata (aSenderID.getURIEncoded (),
                                                                              aDocTypeID.getURIEncoded (),
                                                                              aProcessID.getURIEncoded (), aEP);
          ToopKafkaClient.send (EErrorLevel.INFO, sLogPrefix + "Sending MEM message to " + aEP.getEndpointURL ()
                                                  + " using " + aEP.getTransportProtocol ());
          MEMDelegate.getInstance ().sendMessage (metadata, meMessage);
        }
      }
    }
  }

  // Just to have custom named threads....
  private static final ThreadFactory s_aThreadFactory = new BasicThreadFactory.Builder ().setNamingPattern ("MP-DC-Out-%d")
                                                                                         .setDaemon (true).build ();
  private final ConcurrentCollectorSingle<TDETOOPDataRequestType> m_aCollector = new ConcurrentCollectorSingle<> ();
  private final ExecutorService m_aExecutorPool;

  @Deprecated
  @UsedViaReflection
  public MessageProcessorDCOutgoing () {
    m_aCollector.setPerformer (new Performer ());
    m_aExecutorPool = Executors.newSingleThreadExecutor (s_aThreadFactory);
    m_aExecutorPool.submit (m_aCollector::collect);
  }

  /**
   * The global accessor method.
   *
   * @return The one and only {@link MessageProcessorDCOutgoing} instance.
   */
  @Nonnull
  public static MessageProcessorDCOutgoing getInstance () {
    return getGlobalSingleton (MessageProcessorDCOutgoing.class);
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction) throws Exception {
    // Avoid another enqueue call
    m_aCollector.stopQueuingNewObjects ();

    // Shutdown executor service
    ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (m_aExecutorPool);
  }

  /**
   * Queue a new MS Data Request.
   *
   * @param aMsg
   *          The request to be queued. May not be <code>null</code>.
   * @return {@link ESuccess}. Never <code>null</code>.
   */
  @Nonnull
  public ESuccess enqueue (@Nonnull final TDETOOPDataRequestType aMsg) {
    ValueEnforcer.notNull (aMsg, "Msg");
    try {
      m_aCollector.queueObject (aMsg);
      return ESuccess.SUCCESS;
    } catch (final IllegalStateException ex) {
      // Queue is stopped!
      ToopKafkaClient.send (EErrorLevel.WARN, () -> "Cannot enqueue " + aMsg + ": " + ex.getMessage ());
      return ESuccess.FAILURE;
    }
  }
}
