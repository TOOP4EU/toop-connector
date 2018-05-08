package eu.toop.connector.me.notifications;

import com.helger.commons.ValueEnforcer;
import eu.toop.connector.me.MEException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author yerlibilgin
 */
public class InternalNotificationHandler {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InternalNotificationHandler.class);

  private final HashMap<String, LockableObjectCarrier<Notification>> messageQueue = new HashMap<>();
  private final String targetTypeName;

  public InternalNotificationHandler(Class<? extends Notification> targetType) {
    this.targetTypeName = targetType.getSimpleName();

    //create a timer to periodically purge the expired notification and submission result
    //messages

    Timer timer = new Timer(targetTypeName + "-purgatory-timer");

    long delay = 5 * 60 * 1000; //5 minutes
    long period = 5 * 60 * 1000; //5 minutes

    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        purgeExpiredNotifications();
      }
    }, delay, period);
  }


  protected void handleNotification(Notification notification) {
    LockableObjectCarrier carrier;

    //check the message quee and see if the new object is already there
    synchronized (messageQueue) {
      String submitMessageID = notification.getRefToMessageID();
      if (messageQueue.containsKey(submitMessageID)) {
        carrier = messageQueue.get(submitMessageID);
      } else {
        carrier = new LockableObjectCarrier();
        messageQueue.put(submitMessageID, carrier);
      }
    }

    //now that we have a carrier, notify anyone who waits for it
    synchronized (carrier) {
      carrier.setActualObject(notification);
      carrier.notifyAll();
    }
  }


  /**
   * Wait for a {@link Notification} for a message with the given <code>submitMessageID</code> and for a maximum timeout of
   * <code>timeout</code>. Return the obtained notification
   *
   * @param submitMessageID the id of the submit message
   * @param timeout maximum amount to wait for the object. 0 means forever
   * @return the obtained {@link Notification}
   */
  public Notification obtainNotification(String submitMessageID, long timeout) {
    ValueEnforcer.isGE0(timeout, "timeout");
    ValueEnforcer.notNull(submitMessageID, "MessageId");

    LockableObjectCarrier<Notification> carrier = null;

    LOG.debug("Wait for a " + targetTypeName + " with a messageID: " + submitMessageID);

    synchronized (messageQueue) {
      if (messageQueue.containsKey(submitMessageID)) {
        LOG.debug("we already have a " + targetTypeName + " message for " + submitMessageID);
        carrier = messageQueue.remove(submitMessageID);
      } else {
        //we don't have a carrier yet. Create one
        LOG.debug("We don't have a " + targetTypeName + " waiter for " + submitMessageID + ". Create a waiter for it");

        carrier = new LockableObjectCarrier();
        messageQueue.put(submitMessageID, carrier);
      }
    }

    //we have a nunnull carrier here
    if (carrier.getActualObject() == null) {
      //we haven't received the actual object yet. So wait for it
      synchronized (carrier) {
        try {
          carrier.wait(timeout);
        } catch (InterruptedException e) {
          LOG.warn("Wait for message " + submitMessageID + " was interrupted.");
          throw new MEException("Wait for message " + submitMessageID + " was interrupted.", e);
        }
      }
    }

    if (carrier.getActualObject() == null) {
      throw new MEException("Couldn't obtain a " + targetTypeName + " with a messageID " + submitMessageID);
    }

    return carrier.getActualObject();
  }


  /**
   * Check the notification and subm.result queue and purge the expired messages
   */
  private void purgeExpiredNotifications() {
    long currentTime = System.currentTimeMillis();
    synchronized (messageQueue) {
      ArrayList<String> trash = new ArrayList<>();

      for (String messageID : messageQueue.keySet()) {
        LockableObjectCarrier<Notification> carrier = messageQueue.get(messageID);
        if (carrier != null && carrier.getActualObject() != null && carrier.getActualObject().isExpired(currentTime)) {
          trash.add(messageID);
        }
      }
      for (String messageID : trash) {
        messageQueue.remove(messageID);
      }
    }
  }
}
