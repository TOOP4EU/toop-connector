package eu.toop.mp.me;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;

/**
 * @author: myildiz
 * @date: 12.02.2018.
 */
public class SoapUtil {
  /*
   * Borrowed from  org.apache.wss4j.common.util.AttachmentUtils
   */
  public static final String MIME_HEADER_CONTENT_DESCRIPTION = "Content-Description";
  public static final String MIME_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
  public static final String MIME_HEADER_CONTENT_ID = "Content-ID";
  public static final String MIME_HEADER_CONTENT_LOCATION = "Content-Location";
  public static final String MIME_HEADER_CONTENT_TYPE = "Content-Type";

  private static MessageFactory messageFactory;
  private static SOAPConnectionFactory soapConnectionFactory = null;


  private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  static {
    try {
      messageFactory = MessageFactory.newInstance();
      soapConnectionFactory = SOAPConnectionFactory.newInstance();
    } catch (final SOAPException e) {
      throw new InitializationException ("Failed to init factories", e);
    }
    factory.setNamespaceAware(true);
  }

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SoapUtil.class);

  /**
   * Print an org.w3c.dom.Node as XML
   *
   * @param node
   * @return
   */
  public static String prettyPrint(final org.w3c.dom.Node node) {
    Transformer transformer = null;
    try {
      transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      final StreamResult result = new StreamResult(new StringWriter());
      final DOMSource source = new DOMSource(node);
      transformer.transform(source, result);
      final String xmlString = result.getWriter().toString();
      return xmlString;
    } catch (final Exception e) {
      e.printStackTrace();
      return "";
    }
  }

  /**
   * A utility method to create a SOAP1.2 With Attachments message
   *
   * @return
   */
  public static SOAPMessage createEmptyMessage() {
    try {
      return messageFactory.createMessage();
    } catch (final SOAPException e) {
      throw new IllegalStateException(e);
    }
  }


  /**
   * Write the XML to byte array
   *
   * @param message the SOAPMessage to be serialized to XML
   * @return XML as byte array
   */
  public static byte[] messageToXml(final SOAPMessage message) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      message.writeTo(baos);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
    return baos.toByteArray();
  }

  /**
   * Deserialize a soap xml serialized by <code>serializeSOAPMessage</code> method
   *
   * @param serialized XML as byte array
   * @return
   */
  public static SOAPMessage xmlToMessage(final byte[] serialized) {
    return deserializeSOAPMessage(new ByteArrayInputStream(serialized));
  }


  /**
   * Deserialize a soap xml serialized by <code>serializeSOAPMessage</code> method
   *
   * @param inputStream InputStream containing the XML.
   * @return
   */
  public static SOAPMessage deserializeSOAPMessage(final InputStream inputStream) {
    final PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 100);
    try {
      final byte[] identifier2 = new byte[100];
      MimeHeaders headers = null;
      final int len = pushbackInputStream.read(identifier2, 0, identifier2.length);
      pushbackInputStream.unread(identifier2, 0, len);
      //check the first two characters and see if they are '-'

      if (identifier2[0] == '-' && identifier2[1] == '-') {
        //get the line and parse the part identifier
        final byte[] partIdentifier = new byte[100];

        int index = 0;

        byte current = 0;

        do {
          current = (byte) pushbackInputStream.read();
          partIdentifier[index++] = current;
        } while (current != '\n');

        //now put back
        pushbackInputStream.unread(partIdentifier, 0, index);

        final String part = new String(partIdentifier, 2, index - 2).trim();

        System.out.println(part);
        headers = new MimeHeaders();
        headers.addHeader("Content-Type", "multipart/related; boundary=\"" + part + "\"; type=\"application/soap+xml\"");
        return messageFactory.createMessage(headers, pushbackInputStream);
      } else {
        //try to read directly
        return messageFactory.createMessage(null, pushbackInputStream);
      }
    } catch (final RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw e;
    } catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw new IllegalStateException(e);
    }
  }

  /**
   * This method sends a SOAP1.2 message to the given url.
   *
   * @param message
   * @param endpoint
   * @return
   */
  public static SOAPMessage sendSOAPMessage(final SOAPMessage message, final URL endpoint) {
    try {
      final SOAPConnection connection = soapConnectionFactory.createConnection();
      return connection.call(message, endpoint);
    } catch (final SOAPException e) {
      throw new IllegalStateException(e);
    }
  }

  public static SOAPMessage createMessage(final MimeHeaders headers, final InputStream is) throws IOException, SOAPException {
    return messageFactory.createMessage(headers, is);
  }

  protected static Map<String, String> getHeaders(final String attachmentId) {
    final Map<String, String> headers = new HashMap<>();
    headers.put(MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    headers.put(MIME_HEADER_CONTENT_DISPOSITION, "attachment; filename=\"fname.ext\"");
    headers.put(MIME_HEADER_CONTENT_ID, "<attachment=" + attachmentId + ">");
    headers.put(MIME_HEADER_CONTENT_LOCATION, "http://ws.apache.org");
    headers.put(MIME_HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
    // TODO mock only
    headers.put("TestHeader", "testHeaderValue");
    return headers;
  }

  public static List<AttachmentPart> getAttachments(final SOAPMessage msg) {
    final List<AttachmentPart> atp = new ArrayList<>();
    msg.getAttachments().forEachRemaining(o -> atp.add((AttachmentPart) o));
    try {
      msg.saveChanges();
    } catch (final SOAPException e) {
      throw new IllegalStateException(e);
    }
    return atp;
  }

  /**
   * Read the contents of the SOAP Attachment as byte array
   *
   * @param atp
   * @return
   */
  public static byte[] getAttachmentContent(final AttachmentPart atp) {
    try {
      return atp.getRawContentBytes();
    } catch (final SOAPException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void setAttachmentContent(final AttachmentPart atp, final byte[] content) {
    try {
      atp.setRawContentBytes(content, 0, content.length, atp.getContentType());
    } catch (final SOAPException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void replaceAttachments(final SOAPMessage msg, final List<AttachmentPart> attachmentParts) {
    try {
      msg.removeAllAttachments();
      msg.saveChanges();
      attachmentParts.forEach(msg::addAttachmentPart);
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static MEMessage soap2MEMessage(final SOAPMessage message) throws Exception {
    return EBMSUtils.soap2MEMessage(message);
  }

  public static String describe(final SOAPMessage message) {
    final StringBuilder sb = new StringBuilder();
    final NonBlockingByteArrayOutputStream baos = new NonBlockingByteArrayOutputStream();

    try {
      message.writeTo(baos);

      sb.append(baos.getAsString (StandardCharsets.UTF_8));
    } catch (final Exception e) {
      LOG.error ("Whatsoever", e);
      sb.append("Error");
    }

    return sb.toString();
  }
}