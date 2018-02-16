package eu.toop.mp.me;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Properties;

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
