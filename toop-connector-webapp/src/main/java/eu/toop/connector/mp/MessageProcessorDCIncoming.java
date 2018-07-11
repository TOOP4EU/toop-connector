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

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.helger.asic.SignatureHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.concurrent.BasicThreadFactory;
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.state.ESuccess;
import com.helger.httpclient.HttpClientManager;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

import eu.toop.commons.dataexchange.TDETOOPResponseType;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.http.TCHttpClientFactory;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The global message processor that handles DP to DC (= DC incoming) requests
 * (step 4/4). This is only the queue and it spawns external threads for
 * processing the incoming data.
 *
 * @author Philip Helger
 */
public final class MessageProcessorDCIncoming extends AbstractGlobalWebSingleton {
  /**
   * The nested performer class that does the hard work.
   *
   * @author Philip Helger
   */
  static final class Performer implements IConcurrentPerformer<TDETOOPResponseType> {
    public void runAsync (@Nonnull final TDETOOPResponseType aResponse) throws Exception {
      final String sRequestID = aResponse.getDataRequestIdentifier ().getValue ();
      final String sLogPrefix = "[" + sRequestID + "] ";
      ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DC Incoming Request (4/4)");

      // Forward to the DC at /to-dc interface
      final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();

      try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory)) {
        final SignatureHelper aSH = new SignatureHelper (TCConfig.getKeystoreType (), TCConfig.getKeystorePath (),
                                                         TCConfig.getKeystorePassword (),
                                                         TCConfig.getKeystoreKeyAlias (),
                                                         TCConfig.getKeystoreKeyPassword ());

        try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ()) {
          ToopMessageBuilder.createResponseMessage (aResponse, aBAOS, aSH);

          // Send to DC (see ToDCServlet in toop-interface)
          final String sDestinationUrl = TCConfig.getMPToopInterfaceDCUrl ();

          ToopKafkaClient.send (EErrorLevel.INFO, () -> "Posting signed ASiC response to " + sDestinationUrl);

          final HttpPost aPost = new HttpPost (sDestinationUrl);
          aPost.setEntity (new ByteArrayEntity (aBAOS.toByteArray ()));
          aMgr.execute (aPost);
        }
      }
    }
  }

  // Just to have custom named threads....
  private static final ThreadFactory s_aThreadFactory = new BasicThreadFactory.Builder ().setNamingPattern ("MP-DC-In-%d")
                                                                                         .setDaemon (true).build ();
  private final ConcurrentCollectorSingle<TDETOOPResponseType> m_aCollector = new ConcurrentCollectorSingle<> ();
  private final ExecutorService m_aExecutorPool;

  @Deprecated
  @UsedViaReflection
  public MessageProcessorDCIncoming () {
    m_aCollector.setPerformer (new Performer ());
    m_aExecutorPool = Executors.newSingleThreadExecutor (s_aThreadFactory);
    m_aExecutorPool.submit (m_aCollector::collect);
  }

  /**
   * The global accessor method.
   *
   * @return The one and only {@link MessageProcessorDCIncoming} instance.
   */
  @Nonnull
  public static MessageProcessorDCIncoming getInstance () {
    return getGlobalSingleton (MessageProcessorDCIncoming.class);
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
  public ESuccess enqueue (@Nonnull final TDETOOPResponseType aMsg) {
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
