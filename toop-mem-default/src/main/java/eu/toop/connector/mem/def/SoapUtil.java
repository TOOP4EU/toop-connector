/**
 * Copyright (C) 2018-2019 toop.eu
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
package eu.toop.connector.mem.def;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import com.helger.commons.error.level.EErrorLevel;

import eu.toop.commons.error.EToopErrorCode;
import eu.toop.connector.api.as4.MEException;
import eu.toop.kafkaclient.ToopKafkaClient;
import javafx.beans.binding.StringBinding;
import org.w3c.dom.Node;

/**
 * @author myildiz at 12.02.2018.
 */
public class SoapUtil {
  private static final MessageFactory messageFactory;
  private static final SOAPConnectionFactory soapConnectionFactory;
  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  private static final Transformer serializer;

  static {
    try {
      // Ensure to use SOAP 1.2
      messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
      soapConnectionFactory = SOAPConnectionFactory.newInstance();
      serializer = SAXTransformerFactory.newInstance().newTransformer();
      serializer.setOutputProperty(OutputKeys.INDENT, "yes");
      serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    } catch (final Exception e) {
      throw new MEException("Failed to initialize factories", e);
    }
    factory.setNamespaceAware(true);
  }

  /**
   * Hidden constructor
   */
  private SoapUtil() {
  }

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
      throw new MEException(EToopErrorCode.ME_001, e);
    }
  }

  /**
   * Create a SOAP message from the provided mime headers and an input stream
   * @param headers the MIME headers that will be used during the transportation
   *                as a HTTP package
   * @param is the input stream that the soap message has been serialized to previously
   * @return
   * @throws IOException
   * @throws SOAPException
   */
  public static SOAPMessage createMessage(final MimeHeaders headers,
                                          final InputStream is) throws IOException, SOAPException {
    return messageFactory.createMessage(headers, is);
  }

  /**
   * returns a String description of the provided soap message as XML appended to
   * an enumeration of the attachments and provides info such as id, type and length
   * @param message
   * @return
   */
  public static String describe(final SOAPMessage message) {
    StringBuilder attSummary = new StringBuilder();
    message.getAttachments().forEachRemaining(att -> {
      AttachmentPart ap = (AttachmentPart) att;
      attSummary.append("ID: ").append(ap.getContentId()).append("\n");
      attSummary.append("   TYPE: ").append(ap.getContentType()).append("\n");
      try {
        attSummary.append("   LEN: ").append(ap.getRawContentBytes().length).append("\n");
      } catch (SOAPException e) {
      }

    });

    return prettyPrint(message.getSOAPPart()) + "\n\n" +
        attSummary;
  }

  /**
   * Print the given org.w3c.dom.Node object in an indented XML format
   * @param node the node to be serialized to XML
   * @return
   */
  public static String prettyPrint(Node node) {
    try {
      Source xmlSource = new DOMSource(node);
      StreamResult res = new StreamResult(new ByteArrayOutputStream());
      serializer.transform(xmlSource, res);
      serializer.reset();
      return new String(((ByteArrayOutputStream) res.getOutputStream()).toByteArray());
    } catch (Exception e) {
      return node.getTextContent();
    }
  }
}
