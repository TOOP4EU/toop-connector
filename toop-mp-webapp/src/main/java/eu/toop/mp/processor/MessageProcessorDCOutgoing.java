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
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

import eu.toop.commons.exchange.IMSDataRequest;
import eu.toop.commons.exchange.IToopDataRequest;
import eu.toop.commons.exchange.message.ToopMessageBuilder;
import eu.toop.commons.exchange.mock.ToopDataRequest;
import eu.toop.mp.api.MPSettings;
import eu.toop.mp.me.GatewayRoutingMetadata;
import eu.toop.mp.me.MEMDelegate;
import eu.toop.mp.me.MEMessage;
import eu.toop.mp.me.MEPayload;
import eu.toop.mp.r2d2client.IR2D2Endpoint;
import eu.toop.mp.r2d2client.R2D2Client;

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
  static final class Performer implements IConcurrentPerformer<IMSDataRequest> {
    public void runAsync (@Nonnull final IMSDataRequest aCurrentObject) throws Exception {
      // This is the unique ID of this request message and must be used throughout the
      // whole process for identification
      final String sRequestID = GlobalIDFactory.getNewPersistentStringID () + UUID.randomUUID ().toString ();
      final String sLogPrefix = "[" + sRequestID + "] ";

      s_aLogger.info (sLogPrefix + "Received asynch request: " + aCurrentObject);
      // 1. invoke SMM
      IToopDataRequest aToopDataRequest;
      {
        // TODO mock only
        aToopDataRequest = new ToopDataRequest (sRequestID);
      }

      // 2. invoke R2D2 client
      ICommonsList<IR2D2Endpoint> aEndpoints;
      {
        final IDocumentTypeIdentifier aDocTypeID = MPSettings.getIdentifierFactory ()
                                                             .parseDocumentTypeIdentifier (aCurrentObject.getDocumentTypeID ());
        final IProcessIdentifier aProcessID = MPSettings.getIdentifierFactory ()
                                                        .parseProcessIdentifier (aCurrentObject.getProcessID ());
        aEndpoints = new R2D2Client ().getEndpoints (aCurrentObject.getDestinationCountryCode (), aDocTypeID,
                                                     aProcessID);
        s_aLogger.info (sLogPrefix + "R2D2 found the following endpoints[" + aEndpoints.size () + "]: " + aEndpoints);
      }

      // 3. start message exchange to DC
      {
        // Combine MS data and TOOP data into a single ASiC message
        // Do this only once and not for every endpoint
        MEMessage meMessage;
        try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ()) {
          ToopMessageBuilder.createRequestMessage (aCurrentObject, aToopDataRequest, aBAOS,
                                                   MPWebAppConfig.getSignatureHelper ());

          // build MEM once
          final MEPayload aPayload = new MEPayload (AsicUtils.MIMETYPE_ASICE, sRequestID, aBAOS.toByteArray ());
          meMessage = new MEMessage (aPayload);
        }

        // TODO filter endpoint for supported transport protocols
        for (final IR2D2Endpoint aEP : aEndpoints) {
          final GatewayRoutingMetadata metadata = new GatewayRoutingMetadata (aCurrentObject.getSenderParticipantID (),
                                                                              aCurrentObject.getDocumentTypeID (),
                                                                              aCurrentObject.getProcessID (), aEP);
          MEMDelegate.getInstance ().sendMessage (metadata, meMessage);
        }
      }
    }
  }

  // Just to have custom named threads....
  private static final ThreadFactory s_aThreadFactory = new BasicThreadFactory.Builder ().setNamingPattern ("MP-DC-Out-%d")
                                                                                         .setDaemon (true).build ();
  private final ConcurrentCollectorSingle<IMSDataRequest> m_aCollector = new ConcurrentCollectorSingle<> ();
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
  public ESuccess enqueue (@Nonnull final IMSDataRequest aMsg) {
    ValueEnforcer.notNull (aMsg, "Msg");
    try {
      m_aCollector.queueObject (aMsg);
      return ESuccess.SUCCESS;
    } catch (final IllegalStateException ex) {
      // Queue is stopped!
      s_aLogger.warn ("Cannot enqueue: " + ex.getMessage ());
      return ESuccess.FAILURE;
    }
  }
}
