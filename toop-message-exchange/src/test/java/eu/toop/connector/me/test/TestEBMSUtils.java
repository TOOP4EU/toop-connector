package eu.toop.connector.me.test;

import eu.toop.connector.me.DateTimeUtils;
import eu.toop.connector.me.EBMSActions;
import eu.toop.connector.me.EBMSUtils;
import eu.toop.connector.me.MEException;
import eu.toop.connector.me.SoapUtil;
import eu.toop.connector.me.SoapXPathUtil;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 * @author myildiz
 */
public class TestEBMSUtils {

  final static String xmlTemplate = com.helger.commons.io.stream.StreamHelper
      .getAllBytesAsString(TestEBMSUtils.class.getResourceAsStream("/relay-sr-template.txt"),
          StandardCharsets.UTF_8);

  /**
   * Process the soap message and create a SubmissionResult from it
   */
  public static SOAPMessage inferSubmissionResult(SOAPMessage receivedMessage) {
    String action = EBMSActions.ACTION_SUBMISSION_RESULT;

    return inferNotification(receivedMessage, action);
  }



  /**
   * Process the soap message and create a RelayResult from it
   */
  public static SOAPMessage inferRelayResult(SOAPMessage receivedMessage) {
    String action = EBMSActions.ACTION_RELAY;

    return inferNotification(receivedMessage, action);
  }

  /**
   * Process the soap message and create a Deliver message from it
   */
  public static SOAPMessage inferDelivery(SOAPMessage receivedMessage) {
    return receivedMessage;
  }


  private static SOAPMessage inferNotification(SOAPMessage receivedMessage, String action) {
    String refToMessageId;
    try {
      refToMessageId = SoapXPathUtil
          .safeFindSingleNode(receivedMessage.getSOAPHeader(), ".//:Property[@name='MessageId']/text()")
          .getTextContent();
    } catch (SOAPException e) {
      throw new MEException(e.getMessage(), e);
    }

    String xml =
        xmlTemplate.replace("${timestamp}", DateTimeUtils.getCurrentTimestamp()).
            replace("${messageId}", EBMSUtils.genereateEbmsMessageId("test")).
            replace("${action}", action).
            replace("${submitMessageId}", EBMSUtils.getMessageId(receivedMessage)).
            replace("${c2c3MessageId}",
                refToMessageId);

    try {
      return SoapUtil.createMessage(null, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new MEException(e.getMessage(), e);
    }
  }
}
