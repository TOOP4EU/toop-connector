package eu.toop.connector.me.notifications;

import javax.annotation.Nonnull;

import eu.toop.connector.me.MEException;

/**
 * Implement this interface and register it to the MEMDelegate in order to receive Notifications about the dispatch of
 * the outbound message to the inner corner of the receiving side
 *
 * @author yerlibilgin
 */
public interface IRelayResultHandler {

  /**
   * Implement this method in order to receive Notifications about the dispatch of the outbound message to
   * the inner corner of the receiving side
   *
   * @param notification Notification relay result
   * @throws MEException in case of error
   */
  void handleNotification(@Nonnull RelayResult notification);
}
