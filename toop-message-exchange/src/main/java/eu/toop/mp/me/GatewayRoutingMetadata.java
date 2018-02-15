package eu.toop.mp.me;

import com.helger.commons.annotations.DevelopersNote;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
@DevelopersNote("The fields are intentionally left public. No getter/setter needed")
public class RoutingMetadata {
  /**
   * The actual sender of the data
   */
  @NotNull
  public String originalSender;

  /**
   * The final recipient (on the other MS side)
   */
  @NotNull
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
  @NotNull
  public String to;
  /**
   * TO party Role
   */
  @NotNull
  public String toPartyRole;
  /**
   * FROM party ID
   */
  @NotNull
  public String from;
  /**
   * FROM party ID
   */
  @NotNull
  public String fromPartyRole;
  /**
   * //CollaborationInfo/service
   */
  @NotNull
  public String service;

  /**
   * //CollaborationInfo/action
   */
  @NotNull
  public String action;
}
