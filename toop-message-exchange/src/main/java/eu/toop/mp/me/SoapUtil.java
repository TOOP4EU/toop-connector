package eu.toop.mp.me;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

  private static MessageFactory messageFactory = null;
  private static SOAPConnectionFactory soapConnectionFactory = null;


  private static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  static {
    factory.setNamespaceAware(true);
  }

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SoapUtil.class);

  /**
   * Print an org.w3c.dom.Node as XML
   *
   * @param node
   * @return
   */
  public static String prettyPrint(org.w3c.dom.Node node) {
    Transformer transformer = null;
    try {
      transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      StreamResult result = new StreamResult(new StringWriter());
      DOMSource source = new DOMSource(node);
      transformer.transform(source, result);
      String xmlString = result.getWriter().toString();
      return xmlString;
    } catch (Exception e) {
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
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Write the XML to byte array
   *
   * @param message the SOAPMessage to be serialized to XML
   * @return XML as byte array
   */
  public static byte[] messageToXml(SOAPMessage message) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      message.writeTo(baos);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return baos.toByteArray();
  }

  /**
   * Deserialize a soap xml serialized by <code>serializeSOAPMessage</code> method
   *
   * @param serialized XML as byte array
   * @return
   */
  public static SOAPMessage xmlToMessage(byte[] serialized) {
    return deserializeSOAPMessage(new ByteArrayInputStream(serialized));
  }


  /**
   * Deserialize a soap xml serialized by <code>serializeSOAPMessage</code> method
   *
   * @param inputStream InputStream containing the XML.
   * @return
   */
  public static SOAPMessage deserializeSOAPMessage(InputStream inputStream) {
    PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 100);
    try {
      byte[] identifier2 = new byte[100];
      MimeHeaders headers = null;
      int len = pushbackInputStream.read(identifier2, 0, identifier2.length);
      pushbackInputStream.unread(identifier2, 0, len);
      //check the first two characters and see if they are '-'

      if (identifier2[0] == '-' && identifier2[1] == '-') {
        //get the line and parse the part identifier
        byte[] partIdentifier = new byte[100];

        int index = 0;

        byte current = 0;

        do {
          current = (byte) pushbackInputStream.read();
          partIdentifier[index++] = current;
        } while (current != '\n');

        //now put back
        pushbackInputStream.unread(partIdentifier, 0, index);

        String part = new String(partIdentifier, 2, index - 2).trim();

        System.out.println(part);
        headers = new MimeHeaders();
        headers.addHeader("Content-Type", "multipart/related; boundary=\"" + part + "\"; type=\"application/soap+xml\"");
        return messageFactory.createMessage(headers, pushbackInputStream);
      } else {
        //try to read directly
        return messageFactory.createMessage(null, pushbackInputStream);
      }
    } catch (RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /**
   * This method sends a SOAP1.2 message to the given url.
   *
   * @param message
   * @param endpoint
   * @return
   */
  public static SOAPMessage sendSOAPMessage(SOAPMessage message, URL endpoint) {
    try {
      SOAPConnection connection = soapConnectionFactory.createConnection();
      return connection.call(message, endpoint);
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
  }

  public static SOAPMessage createEmptyMessage(MimeHeaders headers, InputStream is) throws IOException, SOAPException {
    return messageFactory.createMessage(headers, is);
  }

  protected static Map<String, String> getHeaders(String attachmentId) {
    Map<String, String> headers = new HashMap<>();
    headers.put(MIME_HEADER_CONTENT_DESCRIPTION, "Attachment");
    headers.put(MIME_HEADER_CONTENT_DISPOSITION, "attachment; filename=\"fname.ext\"");
    headers.put(MIME_HEADER_CONTENT_ID, "<attachment=" + attachmentId + ">");
    headers.put(MIME_HEADER_CONTENT_LOCATION, "http://ws.apache.org");
    headers.put(MIME_HEADER_CONTENT_TYPE, "text/xml; charset=UTF-8");
    headers.put("TestHeader", "testHeaderValue");
    return headers;
  }

  public static List<AttachmentPart> getAttachments(SOAPMessage msg) {
    final List<AttachmentPart> atp = new ArrayList<>();

    msg.getAttachments().forEachRemaining(new Consumer<AttachmentPart>() {
      @Override
      public void accept(AttachmentPart o) {
        atp.add(o);
      }
    });

    try {
      msg.saveChanges();
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
    return atp;
  }

  /**
   * Read the contents of the SOAP Attachment as byte array
   *
   * @param atp
   * @return
   */
  public static byte[] getAttachmentContent(AttachmentPart atp) {
    try {
      return atp.getRawContentBytes();
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
  }

  public static void setAttachmentContent(AttachmentPart atp, byte[] content) {
    try {
      atp.setRawContentBytes(content, 0, content.length, atp.getContentType());
    } catch (SOAPException e) {
      throw new RuntimeException(e);
    }
  }

  public static void replaceAttachments(SOAPMessage msg, List<AttachmentPart> attachmentParts) {
    try {
      msg.removeAllAttachments();
      msg.saveChanges();
      attachmentParts.forEach(msg::addAttachmentPart);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static MEMessage soap2MEMessage(SOAPMessage message) throws Exception {
    return EBMSUtils.soap2MEMessage(message);
  }

  public static String describe(SOAPMessage message) {
    final StringBuilder sb = new StringBuilder();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    try {
      message.writeTo(baos);

      sb.append(new String(baos.toByteArray()));
    } catch (Exception e) {
      e.printStackTrace();
      sb.append("Error");
    }

    return sb.toString();
  }
}