package eu.toop.connector.me;

import java.io.Serializable;

/**
 *
 * A java representation of a notification. See TOOP AS4 GW backend interface specification
 * @author yerlibilgin
 */
public class Notification implements Serializable {

  /**
   * reference to the message (i.e. the ebms message id of the actual message)
   */
  private String refToMessageID;

  /**
   * The type of this notification
   */
  private SignalType signalType;

  /**
   * The context specific error code (or null in case of success)
   */
  private String errorCode;

  private String shortDescription;


  /**
   * Long description if any
   */
  private String description;


  public String getRefToMessageID() {
    return refToMessageID;
  }

  public void setRefToMessageID(String refToMessageID) {
    this.refToMessageID = refToMessageID;
  }

  public SignalType getSignalType() {
    return signalType;
  }

  public void setSignalType(SignalType signalType) {
    this.signalType = signalType;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "Notification for " + refToMessageID;
  }
}
