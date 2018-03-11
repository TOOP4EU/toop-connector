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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;

import com.helger.asic.AsicUtils;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.BasicThreadFactory;
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

import eu.toop.commons.dataexchange.TDETOOPDataResponseType;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.MEPayload;
import eu.toop.connector.r2d2client.IR2D2Endpoint;
import eu.toop.connector.r2d2client.R2D2Client;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The global message processor that handles DP to DC (=DP outgoing) requests
 * (step 3/4). This is only the queue and it spawns external threads for
 * processing the incoming data.
 *
 * @author Philip Helger
 */
public final class MessageProcessorDPOutgoing extends AbstractGlobalWebSingleton {
  /**
   * The nested performer class that does the hard work.
   *
   * @author Philip Helger
   */
  static final class Performer implements IConcurrentPerformer<TDETOOPDataResponseType> {
    public void runAsync (@Nonnull final TDETOOPDataResponseType aCurrentObject) throws Exception {
      final String sRequestID = aCurrentObject.getDataRequestIdentifier ().getValue ();
      final String sLogPrefix = "[" + sRequestID + "] ";
      ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received asynch request: " + aCurrentObject);

      // 1. invoke SMM
      {
        // Map to TOOP concepts
        // TODO
      }

      // 2. invoke R2D2 client with a single endpoint
      ICommonsList<IR2D2Endpoint> aEndpoints;
      final IDocumentTypeIdentifier aDocTypeID = TCSettings.getIdentifierFactory ()
                                                           .createDocumentTypeIdentifier (aCurrentObject.getDocumentTypeIdentifier ()
                                                                                                        .getSchemeID (),
                                                                                          aCurrentObject.getDocumentTypeIdentifier ()
                                                                                                        .getValue ());
      final IProcessIdentifier aProcessID = TCSettings.getIdentifierFactory ()
                                                      .createProcessIdentifier (aCurrentObject.getProcessIdentifier ()
                                                                                              .getSchemeID (),
                                                                                aCurrentObject.getProcessIdentifier ()
                                                                                              .getValue ());
      {
        // The destination EP is the sender of the original document!
        final IParticipantIdentifier aOriginalSenderID = TCSettings.getIdentifierFactory ()
                                                                   .createParticipantIdentifier (aCurrentObject.getDataConsumer ()
                                                                                                               .getDCElectronicAddressIdentifier ()
                                                                                                               .getSchemeID (),
                                                                                                 aCurrentObject.getDataConsumer ()
                                                                                                               .getDCElectronicAddressIdentifier ()
                                                                                                               .getValue ());
        final ICommonsList<IR2D2Endpoint> aTotalEndpoints = new R2D2Client ().getEndpoints (sLogPrefix,
                                                                                            aOriginalSenderID,
                                                                                            aDocTypeID, aProcessID);

        // Filter all endpoints with the corresponding transport profile
        final String sTransportProfileID = TCConfig.getMEMProtocol ().getTransportProfileID ();
        aEndpoints = aTotalEndpoints.getAll (x -> x.getTransportProtocol ().equals (sTransportProfileID));

        ToopKafkaClient.send (EErrorLevel.INFO,
                              () -> sLogPrefix + "R2D2 found the following endpoints[" + aEndpoints.size () + "/"
                                    + aTotalEndpoints.size () + "]: " + aEndpoints);
      }

      // 3. start message exchange to DC
      {
        // Combine MS data and TOOP data into a single ASiC message
        // Do this only once and not for every endpoint
        MEMessage meMessage;
        try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ()) {
          ToopMessageBuilder.createResponseMessage (aCurrentObject, aBAOS, MPWebAppConfig.getSignatureHelper ());

          // build MEM once
          final MEPayload aPayload = new MEPayload (AsicUtils.MIMETYPE_ASICE, sRequestID, aBAOS.toByteArray ());
          meMessage = new MEMessage (aPayload);
        }

        final String sDPParticipantID = aCurrentObject.getDataProvider ().getDPElectronicAddressIdentifier ()
                                                      .getSchemeID ()
                                        + "::" + aCurrentObject.getDataProvider ().getDPElectronicAddressIdentifier ()
                                                               .getValue ();

        for (final IR2D2Endpoint aEP : aEndpoints) {
          final GatewayRoutingMetadata metadata = new GatewayRoutingMetadata (sDPParticipantID,
                                                                              aDocTypeID.getURIEncoded (),
                                                                              aProcessID.getURIEncoded (), aEP);
          MEMDelegate.getInstance ().sendMessage (metadata, meMessage);
        }
      }
    }
  }

  // Just to have custom named threads....
  private static final ThreadFactory s_aThreadFactory = new BasicThreadFactory.Builder ().setNamingPattern ("MP-DP-Out-%d")
                                                                                         .setDaemon (true).build ();
  private final ConcurrentCollectorSingle<TDETOOPDataResponseType> m_aCollector = new ConcurrentCollectorSingle<> ();
  private final ExecutorService m_aExecutorPool;

  @Deprecated
  @UsedViaReflection
  public MessageProcessorDPOutgoing () {
    m_aCollector.setPerformer (new Performer ());
    m_aExecutorPool = Executors.newSingleThreadExecutor (s_aThreadFactory);
    m_aExecutorPool.submit (m_aCollector::collect);
  }

  /**
   * The global accessor method.
   *
   * @return The one and only {@link MessageProcessorDPOutgoing} instance.
   */
  @Nonnull
  public static MessageProcessorDPOutgoing getInstance () {
    return getGlobalSingleton (MessageProcessorDPOutgoing.class);
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction) throws Exception {
    // Avoid another enqueue call
    m_aCollector.stopQueuingNewObjects ();

    // Shutdown executor service
    ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (m_aExecutorPool);
  }

  /**
   * Queue a new Toop Response.
   *
   * @param aMsg
   *          The data to be queued. May not be <code>null</code>.
   * @return {@link ESuccess}. Never <code>null</code>.
   */
  @Nonnull
  public ESuccess enqueue (@Nonnull final TDETOOPDataResponseType aMsg) {
    ValueEnforcer.notNull (aMsg, "Msg");
    try {
      m_aCollector.queueObject (aMsg);
      return ESuccess.SUCCESS;
    } catch (final IllegalStateException ex) {
      // Queue is stopped!
      ToopKafkaClient.send (EErrorLevel.WARN, () -> "Cannot enqueue " + aMsg, ex);
      return ESuccess.FAILURE;
    }
  }
}
