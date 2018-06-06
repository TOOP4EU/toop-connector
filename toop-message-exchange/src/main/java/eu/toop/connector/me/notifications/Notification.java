/**
 * Copyright (C) 2018 toop.eu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.toop.connector.me.notifications;

import java.io.Serializable;

import com.helger.commons.CGlobal;

import eu.toop.connector.me.ResultType;

/**
 * @author yerlibilgin
 */
public class Notification implements Serializable {

  private static final long EXPIRATION_PERIOD = 5 * CGlobal.MILLISECONDS_PER_MINUTE;
  /**
   * The message id of the SUBMIT message (C1 --&gt; C2)
   */
  private String messageID;
  /**
   * The message id of the outbound message (C2 --&gt; C3)
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
  private final long creationTime;

  Notification(){
    creationTime = System.currentTimeMillis();
  }

  public String getRefToMessageID() {
    return refToMessageID;
  }

  public void setRefToMessageID(final String refToMessageID) {
    this.refToMessageID = refToMessageID;
  }

  public ResultType getResult() {
    return result;
  }

  public void setResult(final ResultType result) {
    this.result = result;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(final String errorCode) {
    this.errorCode = errorCode;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  @Override
  public String toString() {
    return "Notification for " + refToMessageID;
  }

  public String getMessageID() {
    return messageID;
  }

  public void setMessageID(final String messageID) {
    this.messageID = messageID;
  }


  public boolean isExpired(final long currentTime){
    return (currentTime - creationTime) > EXPIRATION_PERIOD;
  }
}
