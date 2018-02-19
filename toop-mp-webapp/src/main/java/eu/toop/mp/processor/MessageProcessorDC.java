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
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

import eu.toop.commons.exchange.IMSDataRequest;

/**
 * The global message processor that handles DC requests. This is only the queue
 * and it spawns external threads for processing the incoming data.
 *
 * @author Philip Helger
 */
public class MessageProcessorDC extends AbstractGlobalWebSingleton {
  /**
   * The nested performer class that does the hard work.
   *
   * @author Philip Helger
   */
  static final class Performer implements IConcurrentPerformer<IMSDataRequest> {
    private static final Logger s_aLogger = LoggerFactory.getLogger(MessageProcessorDC.Performer.class);

    public void runAsync(@Nonnull final IMSDataRequest aCurrentObject) throws Exception {
      s_aLogger.info("Received asynch request: " + aCurrentObject);
      // 1. invoke SMM

      // 2. invoke R2D2 client

      // 3. execute message exchange

      // 4. gather results

      // 5. send back to DC

    }
  }

  // Just to have custom named threads....
  private static final ThreadFactory s_aThreadFactory = new BasicThreadFactory.Builder().setNamingPattern("MPDC")
      .setDaemon(true).build();
  private final ConcurrentCollectorSingle<IMSDataRequest> m_aCollector = new ConcurrentCollectorSingle<>();
  private final ExecutorService m_aSenderThreadPool;

  @Deprecated
  @UsedViaReflection
  public MessageProcessorDC() {
    m_aCollector.setPerformer(new Performer());
    m_aSenderThreadPool = Executors.newSingleThreadExecutor(s_aThreadFactory);
    m_aSenderThreadPool.submit(m_aCollector::collect);
  }

  /**
   *
   * @return The global accessor method.
   */
  @Nonnull
  public static MessageProcessorDC getInstance() {
    return getGlobalSingleton(MessageProcessorDC.class);
  }

  /**
   * Queue a new action item.
   * 
   * @param aMsg
   *          The message to be queued. May not be <code>null</code>.
   */
  public void enqueue(@Nonnull final IMSDataRequest aMsg) {
    ValueEnforcer.notNull(aMsg, "Msg");
    m_aCollector.queueObject(aMsg);
  }
}
