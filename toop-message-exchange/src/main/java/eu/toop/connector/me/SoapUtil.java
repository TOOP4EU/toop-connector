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
package eu.toop.connector.me;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;

import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * @author myildiz at 12.02.2018.
 */
public class SoapUtil {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SoapUtil.class);
  private static final MessageFactory messageFactory;
  private static final SOAPConnectionFactory soapConnectionFactory;
  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  static {
    try {
      // Ensure to use SOAP 1.2
      messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
      soapConnectionFactory = SOAPConnectionFactory.newInstance();
    } catch (final SOAPException e) {
      throw new MEException("Failed to initialize factories", e);
    }
    factory.setNamespaceAware(true);
  }

  private SoapUtil () {}

  /**
   * A utility method to create a SOAP1.2 With Attachments message
   *
   * @return new {@link SOAPMessage}.
   */
  public static SOAPMessage createEmptyMessage() {
    try {
      return messageFactory.createMessage();
    } catch (final SOAPException e) {
      throw new MEException(e);
    }
  }

  /**
   * This method sends a SOAP1.2 message to the given url.
   *
   * @param message message to be send
   * @param endpoint endpoint to send the message to
   * @return The response message
   */
  public static SOAPMessage sendSOAPMessage(final SOAPMessage message, final URL endpoint) {
    ToopKafkaClient.send(EErrorLevel.INFO, () -> "Sending AS4 SOAP message to " + endpoint.toExternalForm());
    try {
      final SOAPConnection connection = soapConnectionFactory.createConnection();
      return connection.call(message, endpoint);
    } catch (final SOAPException e) {
      throw new MEException(e);
    }
  }

  public static SOAPMessage createMessage(final MimeHeaders headers,
      final InputStream is) throws IOException, SOAPException {
    return messageFactory.createMessage(headers, is);
  }

  public static String describe(final SOAPMessage message) {
    try (final NonBlockingByteArrayOutputStream baos = new NonBlockingByteArrayOutputStream()) {
      message.writeTo(baos);
      return baos.getAsString(StandardCharsets.UTF_8);
    } catch (final Exception e) {
      LOG.error("Whatsoever", e);
      return "Error:" + e.getMessage();
    }
  }
}
