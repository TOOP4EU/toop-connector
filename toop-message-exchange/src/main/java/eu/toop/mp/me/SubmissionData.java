/**
 * Copyright (C) 2018 toop.eu
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.toop.mp.me;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class SubmissionData {
  /**
   * The actual sender of the data
   */
  @Nullable
  public String originalSender;

  /**
   * The final recipient (on the other MS side)
   */
  @Nonnull
  public String finalRecipient;

  /**
   * Ref to message id - referencing to the previous ebms message id if any
   */
  @Nullable
  public String refToMessageId;
  /**
   * Conversation ID
   */
  @Nullable
  public String conversationId;
  /**
   * EBMS message ID
   */
  @Nullable
  public String messageId;
  /**
   * TO party ID
   */
  @Nonnull
  public String to;
  /**
   * TO party Role
   */
  @Nonnull
  public String toPartyRole;
  /**
   * FROM party ID
   */
  @Nonnull
  public String from;
  /**
   * FROM party ID
   */
  @Nonnull
  public String fromPartyRole;
  /**
   * //CollaborationInfo/service
   */
  @Nonnull
  public String service;

  /**
   * //CollaborationInfo/action
   */
  @Nonnull
  public String action;
}
