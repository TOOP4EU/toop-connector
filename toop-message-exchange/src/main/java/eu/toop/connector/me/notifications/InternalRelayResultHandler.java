package eu.toop.connector.me.notifications;

import javax.annotation.Nonnull;

/**
 * @author yerlibilgin
 */
public class InternalRelayResultHandler extends InternalNotificationHandler implements IRelayResultHandler {

  public InternalRelayResultHandler() {
    super(RelayResult.class);
  }

  @Override
  public void handleNotification(@Nonnull RelayResult notification) {
    super.handleNotification(notification);
  }
}
