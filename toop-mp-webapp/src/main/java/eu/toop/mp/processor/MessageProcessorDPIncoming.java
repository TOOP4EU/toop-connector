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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.concurrent.BasicThreadFactory;
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.state.ESuccess;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

import eu.toop.commons.exchange.message.ToopResponseMessage;

/**
 * The global message processor that handles DC to DP (= DP incoming) requests
 * (step 2/4). This is only the queue and it spawns external threads for
 * processing the incoming data.
 *
 * @author Philip Helger
 */
public final class MessageProcessorDPIncoming extends AbstractGlobalWebSingleton {
  protected static final Logger s_aLogger = LoggerFactory.getLogger (MessageProcessorDPIncoming.class);

  /**
   * The nested performer class that does the hard work.
   *
   * @author Philip Helger
   */
  static final class Performer implements IConcurrentPerformer<ToopResponseMessage> {
    public void runAsync (@Nonnull final ToopResponseMessage aCurrentObject) throws Exception {
      final String sRequestID = aCurrentObject.getToopDataRequest ().getRequestID ();
      final String sLogPrefix = "[" + sRequestID + "] ";
      s_aLogger.info (sLogPrefix + "Received asynch request: " + aCurrentObject);

      // TODO forward to toop-interface DP input
    }
  }

  // Just to have custom named threads....
  private static final ThreadFactory s_aThreadFactory = new BasicThreadFactory.Builder ().setNamingPattern ("MP-DP-In-%d")
                                                                                         .setDaemon (true).build ();
  private final ConcurrentCollectorSingle<ToopResponseMessage> m_aCollector = new ConcurrentCollectorSingle<> ();
  private final ExecutorService m_aExecutorPool;

  @Deprecated
  @UsedViaReflection
  public MessageProcessorDPIncoming () {
    m_aCollector.setPerformer (new Performer ());
    m_aExecutorPool = Executors.newSingleThreadExecutor (s_aThreadFactory);
    m_aExecutorPool.submit (m_aCollector::collect);
  }

  /**
   * The global accessor method.
   *
   * @return The one and only {@link MessageProcessorDPIncoming} instance.
   */
  @Nonnull
  public static MessageProcessorDPIncoming getInstance () {
    return getGlobalSingleton (MessageProcessorDPIncoming.class);
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
  public ESuccess enqueue (@Nonnull final ToopResponseMessage aMsg) {
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
