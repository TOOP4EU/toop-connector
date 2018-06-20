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
package eu.toop.connector.me.test;

import java.net.URL;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.url.URLHelper;
import com.helger.scope.mock.ScopeAwareTestSetup;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.me.EActingSide;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.notifications.IMessageHandler;

/**
 * This test suite tests the whole sending/receiving of a simple MEMessage by mocking the as4 gateway
 *
 * @author myildiz at 16.02.2018.
 */
public class TestSendReceive {

  static {
    System.setProperty(TCConfig.SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH,
        "toop-connector.elonia.unitTest.properties");
    TCConfig.reloadConfiguration();
  }

  //this must be created after the above level setting statement
  private static final Logger LOG = LoggerFactory.getLogger(TestSendReceive.class);

  /**
   * Create a mock server on localhost that reads and sends back a MEMessage.
   */
  @BeforeAll
  public static void prepare() {
    // Port must match the message-processor.properties
    if (LOG.isDebugEnabled ())
      LOG.debug("Prepare for the test");

    final URL backendURL = URLHelper.getAsURL("http://localhost:10001/backend");
    final URL gwURL = URLHelper.getAsURL(TCConfig.getMEMAS4Endpoint());

    if (LOG.isInfoEnabled ()) {
      LOG.info("backend port: " + backendURL.getPort());
      LOG.info("backend localpath: " + backendURL.getPath());
      LOG.info("GW port: " + gwURL.getPort());
      LOG.info("GW localpath: " + gwURL.getPath());
    }

    BackendServletContainer.createServletOn(backendURL.getPort(), backendURL.getPath());

    GWMocServletContainer.createServletOn(gwURL.getPort(), gwURL.getPath());

    ScopeAwareTestSetup.setupScopeTests();

    ThreadHelper.sleep(1000);

  }

  @AfterAll
  public static void shutdown() {
    ScopeAwareTestSetup.shutdownScopeTests();

    BackendServletContainer.stop();
    GWMocServletContainer.stop();
  }

  @Test
  public void testSendReceive() {
    final GatewayRoutingMetadata metadata = SampleDataProvider
        .createGatewayRoutingMetadata(EActingSide.DC, TCConfig.getMEMAS4Endpoint());
    final MEMessage meMessage = SampleDataProvider.createSampleMessage();

    final boolean result = MEMDelegate.getInstance().sendMessage(metadata, meMessage);

    Assertions.assertTrue(result, "Message sending result must be true");

    final IMessageHandler handler = meMessage1 -> LOG.info("hooray! I Got a message");

    MEMDelegate.getInstance().registerMessageHandler(handler);

  }

}
