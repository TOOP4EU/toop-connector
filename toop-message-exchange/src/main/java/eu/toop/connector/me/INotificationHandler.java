package eu.toop.connector.me;

import javax.annotation.Nonnull;

/**
 * Implement this interface and register it to the MEMDelegate in order to receive Notifications about the dispatch of
 * the outbound message to the inner corner of the receiving side
 *
 * @author yerlibilgin
 */
public interface INotificationHandler {

  /**
   * Implement this method in order to receive Notifications about the dispatch of the outbound message to
   * the inner corner of the receiving side
   *
   * @throws Exception in case of error
   */
  void handleNotification(@Nonnull Notification notification) throws Exception;
}
