package eu.toop.mp.me;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class MocAS4 {
  private final ServerSocket serverSocket;
  private final int port;

  public MocAS4(int port) throws IOException {
    this.port = port;
    serverSocket = new ServerSocket();
  }


  public void start() throws IOException {
    serverSocket.bind(new InetSocketAddress(port));

    new Thread(() -> {
      while (true) {
        Socket accept = null;
        try {
          accept = serverSocket.accept();
          echo(accept);
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
      }
    }).start();
  }

  private void echo(Socket accept) throws IOException, SOAPException {
    System.out.println("New client connected");

    //read one packet, assume that it is small.
    PushbackInputStream pbis = new PushbackInputStream(accept.getInputStream(), 2);
    OutputStream os = accept.getOutputStream();

    accept.setSoTimeout(10000);


    //read the http headers
    String line;
    //leave the rest to soap factory
    MimeHeaders headers = new MimeHeaders();
    while (!"".equals(line = getNextLineNoBuffer(pbis))) {
      System.out.println("[" + line + "]");
      String kvp[] = line.split(":");
      if (kvp.length == 2)
        headers.addHeader(kvp[0].trim(), kvp[1].trim());
    }

    headers.getAllHeaders().forEachRemaining(header -> {
      MimeHeader mh = (MimeHeader) header;
      System.out.println(mh.getName() + ": " + mh.getValue());
    });

    System.out.println("Read the soap message");
    SOAPMessage message = SoapUtil.createMessage(headers, pbis);
    System.out.println(SoapUtil.prettyPrint(message.getSOAPPart()));
    System.out.println("Write the response");
    byte[] successReceipt = EBMSUtils.createSuccessReceipt(message);

    os.write("HTTP/1.1 200 OK\r\n".getBytes());
    os.write("Date: Thu, 09 Dec 2004 12:07:48 GMT\n".getBytes());
    os.write("Server: IBM_CICS_Transaction_Server/3.1.0(zOS)\n".getBytes());
    os.write("Content-type: text/xml\n".getBytes());
    os.write("\r\n".getBytes());
    os.flush();
    os.write(successReceipt);
    os.flush();
    System.out.println("Done");
    os.close();
    pbis.close();
    accept.close();
  }

  private String getNextLineNoBuffer(PushbackInputStream is) throws IOException {
    //read the next line from the stream without buffering it further.
    byte[] next = new byte[500];
    int read = -1;
    int len = 0;

    while ((read = is.read()) != -1) {
      if (read == '\r') {
        //possibly eliminate the '\n'
        read = is.read();
        if (read != '\n')
          is.unread(read);

        return new String(next, 0, len);
      }

      next[len++] = (byte) read;
    }

    throw new

        EOFException();
  }

}
