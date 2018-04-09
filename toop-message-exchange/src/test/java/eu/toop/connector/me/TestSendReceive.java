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

import eu.toop.commons.doctype.EToopDocumentType;
import eu.toop.commons.doctype.EToopProcess;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.IMessageHandler;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.MEPayload;
import eu.toop.connector.me.mocAS4.MockAS4;
import eu.toop.connector.r2d2client.IR2D2Endpoint;
import eu.toop.connector.r2d2client.R2D2Endpoint;

/**
 * This test suite tests the whole sending/receiving of a simple MEMessage by
 * mocking the as4 gateway
 *
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class TestSendReceive {
  private static final Logger LOG = LoggerFactory.getLogger(TestSendReceive.class);
  private static MockAS4 mockAS4;

  /**
   * Create a mock server on localhost that reads and sends back a MEMessage.
   *
   * @throws Exception on error
   */
  @BeforeAll
  public static void prepare() throws Exception {
    // Port must match the message-processor.properties
    final int gwPort = 10001;
    mockAS4 = new MockAS4(gwPort);
    mockAS4.start();

    ScopeAwareTestSetup.setupScopeTests();
  }

  @AfterAll
  public static void shutdown() {
    ScopeAwareTestSetup.shutdownScopeTests();

    mockAS4.finish();
  }

  @Test
  public void testSendReceive() throws Exception {
    final GatewayRoutingMetadata metadata = new GatewayRoutingMetadata("iso6523-actorid-upis::0088:123456", EToopDocumentType.DOCTYPE_REGISTERED_ORGANIZATION_REQUEST.getURIEncoded (),
                                                                       EToopProcess.PROCESS_REQUEST_RESPONSE.getURIEncoded (), createSampleEndpoint());

    final String payloadId = "xmlpayload@dp";
    final IMimeType contentType = CMimeType.APPLICATION_XML;
    final byte[] payloadData = "<sample>xml</sample>".getBytes(StandardCharsets.ISO_8859_1);

    final MEPayload payload = new MEPayload(contentType, payloadId, payloadData);
    final MEMessage meMessage = new MEMessage(payload);

    MEMDelegate.getInstance().sendMessage(metadata, meMessage);

    final IMessageHandler handler = meMessage1 -> LOG.info("hooray! I Got a message");

    MEMDelegate.getInstance().registerMessageHandler(handler);

    final INotificationHandler notificationHandler = notification -> LOG.info("A [" + notification.getSignalType() +
        "] notification received for the message [" + notification.getRefToMessageID() + "]");

    MEMDelegate.getInstance().registerNotificationHandler(notificationHandler);
  }

  @Nonnull
  private IR2D2Endpoint createSampleEndpoint() throws Exception {
    final IParticipantIdentifier identifier = TCSettings.getIdentifierFactory().createParticipantIdentifier("var1",
        "var2");

    final X509Certificate x509 = (X509Certificate) CertificateFactory.getInstance("X509")
        .generateCertificate(this.getClass()
            .getResourceAsStream("/testcert.der"));
    final R2D2Endpoint endpoint = new R2D2Endpoint(identifier, "protocol", "http://sampleendpointurl", x509);
    return endpoint;
  }

}
