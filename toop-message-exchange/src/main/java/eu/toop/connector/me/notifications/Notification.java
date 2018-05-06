package eu.toop.connector.me.notifications;

import eu.toop.connector.me.ResultType;
import java.io.Serializable;

/**
 * @author yerlibilgin
 */
public class Notification implements Serializable {

  private static final long EXPIRATION_PERIOD = 5 * 60 * 1000;
  /**
   * The message id of the SUBMIT message (C1 --> C2)
   */
  private String messageID;
  /**
   * The message id of the outbound message (C2 --> C3)
   */
  private String refToMessageID;
  /**
   * The type of this notification
   */
  private ResultType result;
  /**
   * The context specific error code (or null in case of success)
   */
  private String errorCode;
  /**
   * Long description if any
   */
  private String description;

  /**
   * The local milliseconds time when this object was created
   */
  private long creationTime;

  Notification(){
    creationTime = System.currentTimeMillis();
  }

  public String getRefToMessageID() {
    return refToMessageID;
  }

  public void setRefToMessageID(String refToMessageID) {
    this.refToMessageID = refToMessageID;
  }

  public ResultType getResult() {
    return result;
  }

  public void setResult(ResultType result) {
    this.result = result;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
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

  public String getMessageID() {
    return messageID;
  }

  public void setMessageID(String messageID) {
    this.messageID = messageID;
  }


  public boolean isExpired(long currentTime){
    return (currentTime - creationTime) > EXPIRATION_PERIOD;
  }
}
