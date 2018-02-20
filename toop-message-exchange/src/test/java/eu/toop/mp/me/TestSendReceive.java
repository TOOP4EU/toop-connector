package eu.toop.mp.me;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.IMimeType;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.scope.mock.ScopeAwareTestSetup;

import eu.toop.mp.api.R2D2Settings;
import eu.toop.mp.r2d2client.IR2D2Endpoint;
import eu.toop.mp.r2d2client.R2D2Endpoint;

/**
 * This test suite tests the whole sending/receiving of a simple MEMessage by
 * mocking the as4 gateway
 *
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class TestSendReceive {
  private static final Logger LOG = LoggerFactory.getLogger (TestSendReceive.class);

  /**
   * Create a mock server on localhost that reads and sends back a MEMessage.
   * @throws Exception on error
   */
  @BeforeAll
  public static void prepare () throws Exception {
    final int gwPort = 10001;
    final MockAS4 mockAS4 = new MockAS4 (gwPort);
    mockAS4.start ();

    // set the url of the gateway to the moc
    MessageExchangeEndpointConfig.GW_URL = new URL ("http://localhost:" + gwPort);

    ScopeAwareTestSetup.setupScopeTests ();
  }

  @AfterAll
  public static void shutdown ()
  {
    ScopeAwareTestSetup.shutdownScopeTests ();
  }

  @Test
  public void testSendReceive () throws Exception {
    final GatewayRoutingMetadata metadata = new GatewayRoutingMetadata ("top-sercret-pdf-documents-only",
                                                                        "dummy-process", _sampleEndpoint ());

    final String payloadId = "xmlpayload@dp";
    final IMimeType contentType = CMimeType.APPLICATION_XML;
    final byte[] payloadData = "<sample>xml</sample>".getBytes (StandardCharsets.ISO_8859_1);

    final MEPayload payload = new MEPayload (contentType, payloadId, payloadData);
    final MEMessage meMessage = new MEMessage (payload);

    MEMDelegate.getInstance ().sendMessage (metadata, meMessage);

    final IMessageHandler handler = meMessage1 -> LOG.info ("hooray! I Got a message");

    MEMDelegate.getInstance ().registerMessageHandler (handler);

    // TODO: receive side is not implemented yet
  }

  @Nonnull
  private IR2D2Endpoint _sampleEndpoint () throws Exception {
    final IParticipantIdentifier identifier = R2D2Settings.getIdentifierFactory ().createParticipantIdentifier ("var1",
                                                                                                                 "var2");

    final X509Certificate x509 = (X509Certificate) CertificateFactory.getInstance ("X509")
                                                                     .generateCertificate (this.getClass ()
                                                                                               .getResourceAsStream ("/testcert.der"));
    final R2D2Endpoint endpoint = new R2D2Endpoint (identifier, "protocol", "http://sampleendpointurl", x509);
    return endpoint;
  }

}
