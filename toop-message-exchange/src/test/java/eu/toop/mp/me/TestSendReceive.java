package eu.toop.mp.me;

import com.helger.peppol.identifier.factory.SimpleIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import eu.toop.mp.r2d2client.IR2D2Endpoint;
import eu.toop.mp.r2d2client.R2D2Endpoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This test suite tests the whole sending/receiving of a simple MEMessage by mocking the as4 gateway
 *
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class TestSendReceive {
  /**
   * Create a moc server on localhost that reads and sends back a MEMessage.
   */
  @BeforeAll
  public static void prepare() throws Exception {
    int gwPort = 10001;
    MocAS4 mocAS4 = new MocAS4(gwPort);
    mocAS4.start();

    //set the url of the gateway to the moc
    MessageExchangeEndpointConfig.GW_URL = new URL("http://localhost:" + gwPort);
  }

  @Test
  public void testSendReceive() throws Exception {
    GatewayRoutingMetadata metadata = new GatewayRoutingMetadata();
    metadata.setDocumentTypeId("top-sercret-pdf-documents-only");
    metadata.setProcessId("dummy-process");
    metadata.setEndpoint(sampleEndpoint());

    MEPayload payload = MEPayloadFactory.createPayload("xmlpayload@dp","application/xml", "<sample>xml</sample>".getBytes());
    MEMessage meMessage = MEMessageFactory.createMEMessage(payload);

    MEMDelegate.get().sendMessage(metadata, meMessage);


    IMessageHandler handler = new IMessageHandler() {
      @Override
      public void handleMessage(MEMessage meMessage) {
        System.out.println("hooray! I Got a message");
      }
    };

    MEMDelegate.get().registerMessageHandler(handler);

    //TODO: receive side is not implemented yet
  }

  private IR2D2Endpoint sampleEndpoint() throws Exception {
    SimpleParticipantIdentifier identifier = SimpleIdentifierFactory.INSTANCE.createParticipantIdentifier("var1", "var2");

    X509Certificate x509 = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(this.getClass().getResourceAsStream("/testcert.der"));
    R2D2Endpoint endpoint = new R2D2Endpoint(identifier, "protocol", "http://sampleendpointurl", x509);
    return endpoint;
  }

}
