package eu.toop.connector.me.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.scope.mock.ScopeAwareTestSetup;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.me.EActingSide;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.notifications.IMessageHandler;

/**
 * This class runs an and to end integration test with a locally running Toop Compatible gateway. The simulation is from
 * elonia-gw-gw-freedonia and back
 *
 * This is not a unit test that is why it has a main method
 *
 * @author myildiz83
 */
public class IntegrationTest {

  static {
    System.setProperty(TCConfig.SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH,
        "toop-connector.elonia.integrationTest.properties");
    PropertyConfigurator.configure(IntegrationTest.class.getResourceAsStream("/log4j.properties"));
  }

  private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);

  public static void main(String[] args) throws Exception {

    ScopeAwareTestSetup.setupScopeTests();
    //initialize c1
    LOG.info("Initialize corner1");
    BackendServletContainer.createServletOn(8585, "/msh");
    //initialize c4
    LOG.info("Initialize corner4");
    BackendServletContainer.createServletOn(8686, "/msh");

    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    //wait for the other side to receive the message
    final DeliveryWatcher deliveryWatcher = new DeliveryWatcher();
    MEMDelegate.getInstance().registerMessageHandler(deliveryWatcher);

    Thread.sleep(1000);

    //the URL of both C2 and C3 were set to the same endpoint
    //for this test.
    String recivingSideURL = TCConfig.getMEMAS4Endpoint();

    String command;
    inputLoop:
    while ((command = waitForUserInput(bufferedReader)) != null) {
      switch (command) {
        case "e2f":
          updateConfig(command);
          sendMessage(deliveryWatcher, EActingSide.DC, recivingSideURL);
          break;

        case "f2e":
          updateConfig(command);
          sendMessage(deliveryWatcher, EActingSide.DP, recivingSideURL);
          break;

        case "q":
          LOG.info("Exit gracefully");
          break inputLoop;

        default:
          break;
      }

      LOG.info("");
    }
  }

  private static void updateConfig(String line) {
    String targetConfiguration = "toop-connector.elonia.integrationTest.properties";

    switch (line) {
      case "e2f":
        targetConfiguration = "toop-connector.elonia.integrationTest.properties";
        break;

      case "f2e":
        targetConfiguration = "toop-connector.freedonia.integrationTest.properties";
        break;
    }

    System.setProperty(TCConfig.SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH,
        targetConfiguration);

    TCConfig.reloadConfiguration();
  }

  /**
   * Send a message from one side to the other
   */
  private static void sendMessage(DeliveryWatcher deliveryWatcher, EActingSide actingSide,
      String recivingSideURL) {
    deliveryWatcher.reset();

    //set the address of the receiving gateway to t
    final GatewayRoutingMetadata gatewayRoutingMetadata = SampleDataProvider.createGatewayRoutingMetadata(actingSide,
        recivingSideURL);
    final MEMessage meMessage = SampleDataProvider.createSampleMessage();

    boolean result = MEMDelegate.getInstance().sendMessage(gatewayRoutingMetadata, meMessage);

    if (!result) {
      LOG.info("TEST for " + actingSide + " FAILED");
      LOG.error("Failed to send a message");

      return;
    }

    LOG.info("Wait for the delivery");
    MEMessage obtainedMessage = deliveryWatcher.obtainMessage(60, TimeUnit.SECONDS);
    if (obtainedMessage == null) {
      LOG.error("Failed to receive a message from the other side within 20 seconds timeout");
      LOG.info("TEST for " + actingSide + " FAILED");
    } else {
      LOG.info("The message was successfully received from the other side");
      LOG.info("TEST for " + actingSide + " SUCCESSFUL");
    }
  }

  private static String waitForUserInput(BufferedReader bufferedReader) throws Exception {
    LOG.info("Wait for user input.");
    LOG.info("e2f: send a message from elonia to freedonia");
    LOG.info("f2e: send a message from freedonia to elonia");
    return bufferedReader.readLine();
  }


  static class DeliveryWatcher implements IMessageHandler {

    private CountDownLatch latch = new CountDownLatch(1);
    private volatile MEMessage meMessage;


    public void reset() {
      latch = new CountDownLatch(1);
      meMessage = null;
    }

    /**
     * implement this method to receive messages when an inbound message arrives to the AS4 endpoint
     *
     * @param meMessage the object that contains the payloads and their metadata´
     * @throws Exception in case of error
     */
    @Override
    public void handleMessage(@Nonnull MEMessage meMessage) throws Exception {
      synchronized (this) {
        this.meMessage = meMessage;
      }

      latch.countDown();
    }

    public MEMessage obtainMessage(long timeout, TimeUnit timeUnit) {

      if (this.meMessage != null) {
        return meMessage;
      }

      try {
        latch.await(timeout, timeUnit);
      } catch (InterruptedException e) {
        LOG.error("wait interrupted", e);
      }
      synchronized (this) {
        try {
          return meMessage;
        } finally {
          meMessage = null;
        }
      }
    }
  }
}
