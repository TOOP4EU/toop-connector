package eu.toop.connector.me.notifications;

/**
 * A java representation of a notification C2 --- C3 message relay. See TOOP AS4 GW backend interface specification
 *
 * @author yerlibilgin
 */
public class RelayResult extends Notification {

  private String shortDescription;
  private String severity;


  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getSeverity() {
    return severity;
  }
}
