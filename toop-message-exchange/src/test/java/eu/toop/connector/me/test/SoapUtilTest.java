package eu.toop.connector.me.test;


import eu.toop.connector.me.SoapUtil;
import org.junit.Test;

import javax.xml.soap.SOAPMessage;
import java.net.MalformedURLException;
import java.net.URL;

public class SoapUtilTest {

  @Test
  public void sendSOAPMessage() throws MalformedURLException {
    SOAPMessage emptyMessage = SoapUtil.createEmptyMessage();

    SoapUtil.sendSOAPMessage(emptyMessage, new URL("http://www.google.com"));
  }
}
