package eu.toop.mp.me;

import javax.annotation.Nonnull;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public interface IMessageHandler {
  /**
   * implement this method to receive messages when an inbound message arrives to the AS4 endpoint
   * @param meMessage the object that contains the payloads and their metadata
   */
  void handleMessage(@Nonnull MEMessage meMessage);
}
