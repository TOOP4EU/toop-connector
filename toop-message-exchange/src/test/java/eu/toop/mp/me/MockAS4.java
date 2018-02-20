package eu.toop.mp.me;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;

/**
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class MockAS4 {
  private final ServerSocket serverSocket;
  private final int port;

  public MockAS4 (final int port) throws IOException {
    this.port = port;
    serverSocket = new ServerSocket ();
  }

  public void start () throws IOException {
    serverSocket.bind (new InetSocketAddress (port));

    new Thread ( () -> {
      while (true) {
        Socket accept = null;
        try {
          accept = serverSocket.accept ();
          echo (accept);
        } catch (final Exception e) {
          e.printStackTrace ();
          break;
        }
      }
    }).start ();
  }

  private void echo (final Socket accept) throws IOException, SOAPException {
    System.out.println ("New client connected");

    // read one packet, assume that it is small.
    final PushbackInputStream pbis = new PushbackInputStream (accept.getInputStream (), 2);
    final OutputStream os = accept.getOutputStream ();

    accept.setSoTimeout (10000);

    // read the http headers
    String line;
    // leave the rest to soap factory
    final MimeHeaders headers = new MimeHeaders ();
    while (!"".equals (line = getNextLineNoBuffer (pbis))) {
      System.out.println ("[" + line + "]");
      final String kvp[] = line.split (":");
      if (kvp.length == 2)
        headers.addHeader (kvp[0].trim (), kvp[1].trim ());
    }

    headers.getAllHeaders ().forEachRemaining (header -> {
      final MimeHeader mh = (MimeHeader) header;
      System.out.println (mh.getName () + ": " + mh.getValue ());
    });

    System.out.println ("Read the soap message");
    final SOAPMessage message = SoapUtil.createMessage (headers, pbis);
    System.out.println (prettyPrint (message.getSOAPPart ()));
    System.out.println ("Write the response");
    final byte[] successReceipt = EBMSUtils.createSuccessReceipt (message);

    os.write ("HTTP/1.1 200 OK\r\n".getBytes (StandardCharsets.ISO_8859_1));
    os.write ("Date: Thu, 09 Dec 2004 12:07:48 GMT\r\n".getBytes (StandardCharsets.ISO_8859_1));
    os.write ("Server: IBM_CICS_Transaction_Server/3.1.0(zOS)\r\n".getBytes (StandardCharsets.ISO_8859_1));
    os.write ("Content-type: text/xml\r\n".getBytes (StandardCharsets.ISO_8859_1));
    os.write ("\r\n".getBytes (StandardCharsets.ISO_8859_1));
    os.flush ();
    os.write (successReceipt);
    os.flush ();
    System.out.println ("Done");
    os.close ();
    pbis.close ();
    accept.close ();
  }

  private String getNextLineNoBuffer (final PushbackInputStream is) throws IOException {
    // read the next line from the stream without buffering it further.
    try (final NonBlockingByteArrayOutputStream next = new NonBlockingByteArrayOutputStream ()) {
      int read = -1;

      while ((read = is.read ()) != -1) {
        if (read == '\r') {
          // possibly eliminate the '\n'
          read = is.read ();
          if (read != '\n')
            is.unread (read);

          return next.getAsString (StandardCharsets.ISO_8859_1);
        }

        next.write ((byte) read);
      }
      throw new EOFException ();
    }
  }

  /**
   * Print an org.w3c.dom.Node as XML
   *
   * @param node
   * @return
   */
  public static String prettyPrint (final org.w3c.dom.Node node) {
    Transformer transformer = null;
    try {
      transformer = TransformerFactory.newInstance ().newTransformer ();
      transformer.setOutputProperty (OutputKeys.INDENT, "yes");
      transformer.setOutputProperty (OutputKeys.DOCTYPE_PUBLIC, "yes");
      transformer.setOutputProperty ("{http://xml.apache.org/xslt}indent-amount", "2");
      final StreamResult result = new StreamResult (new StringWriter ());
      final DOMSource source = new DOMSource (node);
      transformer.transform (source, result);
      final String xmlString = result.getWriter ().toString ();
      return xmlString;
    } catch (final Exception e) {
      e.printStackTrace ();
      return "";
    }
  }
}
