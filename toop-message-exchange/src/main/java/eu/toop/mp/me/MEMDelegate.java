package eu.toop.mp.me;

import javax.annotation.Nonnull;
import javax.xml.soap.SOAPMessage;
import java.util.ArrayList;

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


  private ArrayList<IMessageHandler> messageHandlers = new ArrayList<IMessageHandler>();

  private MEMDelegate() {

  }


  /**
   * The V1 message sending interface for the message exchange module
   *
   * @param gatewayRoutingMetadata The container for the endpoint information and docid/procid
   * @param meMessage              the payloads and their metadata to be sent to the gateway.
   */
  public void sendMessage(GatewayRoutingMetadata gatewayRoutingMetadata, MEMessage meMessage) {
    SubmissionData submissionData = EBMSUtils.inferSubmissionData(gatewayRoutingMetadata);
    SOAPMessage soapMessage = EBMSUtils.convert2MEOutboundAS4Message(submissionData, meMessage);
    SoapUtil.sendSOAPMessage(soapMessage, MessageExchangeEndpointConfig.GW_URL);
  }


  /**
   * Register a new message handler to be able to handle the inbound messages from the AS4 gateway.
   * <p>
   * Duplicate checking skipped for now. So if you register a handler twice, its handle method will
   * be called twice.
   *
   * @param iMessageHandler
   */
  public void registerMessageHandler(@Nonnull IMessageHandler iMessageHandler) {
    this.messageHandlers.add(iMessageHandler);
  }

  /**
   * Remove a message handler from this delegate
   *
   * @param iMessageHandler
   */
  public void deRegisterMessageHandler(@Nonnull IMessageHandler iMessageHandler) {
    this.messageHandlers.remove(iMessageHandler);
  }


  /**
   * Dispatch the received inobund message form the AS4 gateway to the handlers
   *
   * @param message
   */
  public void dispatchMessage(SOAPMessage message) {
    for (IMessageHandler messageHandler : messageHandlers) {
      try {
        messageHandler.handleMessage(SoapUtil.soap2MEMessage(message));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
