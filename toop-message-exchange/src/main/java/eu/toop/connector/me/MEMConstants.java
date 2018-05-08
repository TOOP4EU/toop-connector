package eu.toop.connector.me;

/**
 * @author myildiz
 */
public class MEMConstants {

  public static final String MEM_AS4_SUFFIX = "message-exchange.toop.eu";
  public static final String MEM_PARTY_ROLE = "http://toop.eu/identifiers/roles/backend";
  public static final String GW_PARTY_ROLE = "http://toop.eu/identifiers/roles/gateway";

  public static final String ACTION_SUBMIT = "Submit";
  public static final String ACTION_DELIVER = "Deliver";
  //this is recommended to be a Relay instead of Notify
  //but its kept like this for a while
  public static final String ACTION_RELAY = "Notify";
  public static final String ACTION_SUBMISSION_RESULT = "SubmissionResult";
  public static final String SERVICE = "http://www.toop.eu/as4/backend";
}
