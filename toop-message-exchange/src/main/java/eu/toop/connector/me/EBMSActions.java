package eu.toop.connector.me;

/**
 * The TOOP AS4 gateway backend interface action types are defined here.
 *
 * @author yerlibilgin
 */
public class EBMSActions {
  public static final String ACTION_SUBMIT = "Submit";
  public static final String ACTION_DELIVER = "Deliver";
  //this is recommended to be a Relay instead of Notify
  //but its kept like this for a while
  public static final String ACTION_RELAY = "Notify";
  public static final String ACTION_SUBMISSION_RESULT = "SubmissionResult";
}
