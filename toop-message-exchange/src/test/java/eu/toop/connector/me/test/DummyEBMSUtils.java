package eu.toop.connector.me.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import eu.toop.connector.me.DateTimeUtils;
import eu.toop.connector.me.EBMSUtils;
import eu.toop.connector.me.MEException;
import eu.toop.connector.me.MEMConstants;
import eu.toop.connector.me.SoapUtil;
import eu.toop.connector.me.SoapXPathUtil;

/**
 * @author myildiz
 */
public class DummyEBMSUtils {

  final static String xmlTemplate = com.helger.commons.io.stream.StreamHelper
      .getAllBytesAsString(DummyEBMSUtils.class.getResourceAsStream("/relay-sr-template.txt"),
          StandardCharsets.UTF_8);

  /**
   * Process the soap message and create a SubmissionResult from it
   */
  public static SOAPMessage inferSubmissionResult(SOAPMessage receivedMessage) {
    String action = MEMConstants.ACTION_SUBMISSION_RESULT;

    String theAS4Message;
    try {
      theAS4Message = SoapXPathUtil
          .safeFindSingleNode(receivedMessage.getSOAPHeader(), ".//:Property[@name='MessageId']/text()")
          .getTextContent();
    } catch (SOAPException e) {
      throw new MEException(e.getMessage(), e);
    }

    String xml =
        xmlTemplate.replace("${timestamp}", DateTimeUtils.getCurrentTimestamp()).
            replace("${messageId}", EBMSUtils.genereateEbmsMessageId("test")).
            replace("${action}", action).
            replace("${propMessageId}", theAS4Message).
            replace("${propRefToMessageId}", EBMSUtils.getMessageId(receivedMessage)
            );

    try {
      return SoapUtil.createMessage(null, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new MEException(e.getMessage(), e);
    }
  }


  /**
   * Process the soap message and create a RelayResult from it
   */
  public static SOAPMessage inferRelayResult(SOAPMessage receivedMessage) {
    String action = MEMConstants.ACTION_RELAY;

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
            replace("${propMessageId}", refToMessageId).
            replace("${propRefToMessageId}", refToMessageId
            );

    try {
      return SoapUtil.createMessage(null, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new MEException(e.getMessage(), e);
    }
  }

  /**
   * Process the soap message and create a Deliver message from it
   */
  public static SOAPMessage inferDelivery(SOAPMessage receivedMessage) {
    return receivedMessage;
  }


}
