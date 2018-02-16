package eu.toop.mp.me;

import com.sun.istack.internal.NotNull;

import javax.xml.soap.SOAPMessage;
import java.net.URL;
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
  private URL gatewayURL;

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
    SubmissionData submissionData = Util.inferSubmissionData(gatewayRoutingMetadata);
    SOAPMessage soapMessage = Util.convert2Soap(submissionData, meMessage);
    SoapUtil.sendSOAPMessage(soapMessage, gatewayURL);
  }


  /**
   * Register a new message handler to be able to handle the inbound messages from the AS4 gateway.
   * <p>
   * Duplicate checking skipped for now. So if you register a handler twice, its handle method will
   * be called twice.
   *
   * @param iMessageHandler
   */
  public void registerMessageHandler(@NotNull IMessageHandler iMessageHandler) {
    this.messageHandlers.add(iMessageHandler);
  }

  /**
   * Remove a message handler from this delegate
   *
   * @param iMessageHandler
   */
  public void deRegisterMessageHandler(@NotNull IMessageHandler iMessageHandler) {
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
