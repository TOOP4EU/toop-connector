package eu.toop.connector.me.notifications;

import javax.annotation.Nonnull;

/**
 * @author yerlibilgin
 */
public class InternalSubmissionResultHandler extends InternalNotificationHandler implements
    ISubmissionResultHandler {

  public InternalSubmissionResultHandler() {
    super(SubmissionResult.class);
  }

  @Override
  public void handleSubmissionResult(@Nonnull SubmissionResult submissionResult) {
    super.handleNotification(submissionResult);
  }
}
