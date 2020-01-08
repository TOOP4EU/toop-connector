/**
 * Copyright (C) 2018-2020 toop.eu
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
package eu.toop.connector.mem.def.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.mem.def.DateTimeUtils;
import eu.toop.connector.mem.def.EBMSUtils;
import eu.toop.connector.mem.def.MEMConstants;
import eu.toop.connector.mem.def.SoapUtil;
import eu.toop.connector.mem.def.SoapXPathUtil;

/**
 * @author myildiz
 */
public class DummyEBMSUtils {
  private static boolean failOnSubmissionResult;
  private static boolean failOnRelayResult;
  private static String relayEbmsError;

  final static String xmlTemplate = com.helger.commons.io.stream.StreamHelper
      .getAllBytesAsString(DummyEBMSUtils.class.getResourceAsStream("/relay-sr-template.txt"),
          StandardCharsets.UTF_8);

  /*
   * Process the soap message and create a SubmissionResult from it
   */
  public static SOAPMessage inferSubmissionResult(final SOAPMessage receivedMessage) {
    final String action = MEMConstants.ACTION_SUBMISSION_RESULT;

    String theAS4Message;
    try {
      theAS4Message = SoapXPathUtil
          .safeFindSingleNode(receivedMessage.getSOAPHeader(), ".//:Property[@name='MessageId']/text()")
          .getTextContent();
    } catch (final SOAPException e) {
      throw new MEException(e.getMessage(), e);
    }

    String xml =
        xmlTemplate.replace("${timestamp}", DateTimeUtils.getCurrentTimestamp()).
            replace("${messageId}", EBMSUtils.genereateEbmsMessageId("test")).
            replace("${action}", action).
            replace("${propMessageId}", theAS4Message).
            replace("${propRefToMessageId}", EBMSUtils.getMessageId(receivedMessage)
            );

    if (failOnSubmissionResult) {
      xml = xml.replace("${result}", "Error");
      xml = xml.replace("${description}", "Shit happens!");
      xml = xml.replace("${errorCode}", "Shit happens!");
    } else {
      xml = xml.replace("${result}", "Receipt");
    }

    try {
      return SoapUtil.createMessage(null, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (final Exception e) {
      throw new MEException(e.getMessage(), e);
    }
  }


  /*
   * Process the soap message and create a RelayResult from it
   */
  public static SOAPMessage inferRelayResult(final SOAPMessage receivedMessage) {
    final String action = MEMConstants.ACTION_RELAY;

    String refToMessageId;
    try {
      refToMessageId = SoapXPathUtil
          .safeFindSingleNode(receivedMessage.getSOAPHeader(), ".//:Property[@name='MessageId']/text()")
          .getTextContent();
    } catch (final SOAPException e) {
      throw new MEException(e.getMessage(), e);
    }

    String xml =
        xmlTemplate.replace("${timestamp}", DateTimeUtils.getCurrentTimestamp()).
            replace("${messageId}", EBMSUtils.genereateEbmsMessageId("test")).
            replace("${action}", action).
            replace("${propMessageId}", refToMessageId).
            replace("${propRefToMessageId}", refToMessageId
            );

    if (failOnRelayResult) {
      xml = xml.replace("${result}", "Error");
      xml = xml.replace("${errorCode}", relayEbmsError);
    } else {
      xml = xml.replace("${result}", "Receipt");
    }

    try {
      return SoapUtil.createMessage(null, new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (final Exception e) {
      throw new MEException(e.getMessage(), e);
    }
  }

  /*
   * Process the soap message and create a Deliver message from it
   */
  public static SOAPMessage inferDelivery(final SOAPMessage receivedMessage) {
    return receivedMessage;
  }


  public static void setFailOnSubmissionResult(boolean fail) {
    DummyEBMSUtils.failOnSubmissionResult = fail;
  }

  public static void setFailOnRelayResult(boolean fail, String errorCode) {
    DummyEBMSUtils.failOnRelayResult = fail;
    DummyEBMSUtils.relayEbmsError = errorCode;

  }

}
