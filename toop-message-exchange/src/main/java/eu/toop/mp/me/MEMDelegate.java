package eu.toop.mp.me;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.soap.SOAPMessage;

/**
 * The API Entry class for the Message Exchange API.
 *
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class MEMDelegate {
  /**
   * Singleton instance
   */
  private static final MEMDelegate instance = new MEMDelegate();

  public static MEMDelegate get() {
    return instance;
  }


  private final List<IMessageHandler> messageHandlers = new ArrayList<>();

  private MEMDelegate() {

  }


  /**
   * The V1 message sending interface for the message exchange module
   *
   * @param gatewayRoutingMetadata The container for the endpoint information and docid/procid
   * @param meMessage              the payloads and their metadata to be sent to the gateway.
   */
  public void sendMessage(final GatewayRoutingMetadata gatewayRoutingMetadata, final MEMessage meMessage) {
    final SubmissionData submissionData = EBMSUtils.inferSubmissionData(gatewayRoutingMetadata);
    final SOAPMessage soapMessage = EBMSUtils.convert2MEOutboundAS4Message(submissionData, meMessage);
    SoapUtil.sendSOAPMessage(soapMessage, MessageExchangeEndpointConfig.GW_URL);
  }


  /**
   * Register a new message handler to be able to handle the inbound messages from the AS4 gateway.
   * <p>
   * Duplicate checking skipped for now. So if you register a handler twice, its handle method will
   * be called twice.
   *
   * @param iMessageHandler message handler to be added
   */
  public void registerMessageHandler(@Nonnull final IMessageHandler iMessageHandler) {
    this.messageHandlers.add(iMessageHandler);
  }

  /**
   * Remove a message handler from this delegate
   *
   * @param iMessageHandler Message handler to be removed
   */
  public void deRegisterMessageHandler(@Nonnull final IMessageHandler iMessageHandler) {
    this.messageHandlers.remove(iMessageHandler);
  }


  /**
   * Dispatch the received inbound message form the AS4 gateway to the handlers
   *
   * @param message message to be dispatched
   */
  public void dispatchInboundMessage(@Nonnull final SOAPMessage message) {
    for (final IMessageHandler messageHandler : messageHandlers) {
      try {
        messageHandler.handleMessage(SoapUtil.soap2MEMessage(message));
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
