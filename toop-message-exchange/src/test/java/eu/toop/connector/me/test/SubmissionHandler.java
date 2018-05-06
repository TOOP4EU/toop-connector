package eu.toop.connector.me.test;

import com.helger.commons.url.URLHelper;
import eu.toop.connector.me.EBMSUtils;
import java.net.URL;
import javax.xml.soap.SOAPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An internal representation of a simple gateway that handles a submitted message.
 *
 * Since it represents both c2 and c3 It does three things:
 *
 * 1. send back a submission result 2. send back a relay result 3. deliver a message back to the backend
 *
 * @author myildiz
 */
public class SubmissionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SubmissionHandler.class);
  private static final URL BACKEND_URL = URLHelper.getAsURL("http://localhost:10001/backend");

  public static void handle(SOAPMessage receivedMessage) {
    Thread th = new Thread(() -> {
      //send back a submission result

      LOG.info("Handle submission for " + EBMSUtils.getMessageId(receivedMessage));



      LOG.info("Send back a submission result");
      SOAPMessage submissionResult = TestEBMSUtils.inferSubmissionResult(receivedMessage);

      EBMSUtils.sendSOAPMessage(submissionResult, BACKEND_URL);

      //wait a bit
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }


      LOG.info("Send back a relay result");
      SOAPMessage relayResult = TestEBMSUtils.inferRelayResult(receivedMessage);

      EBMSUtils.sendSOAPMessage(relayResult, BACKEND_URL);

      //wait a bit
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }

      //LOG.info("Send back a delivery message");
      //SOAPMessage deliveryMessage = TestEBMSUtils.inferDelivery(receivedMessage);
      //EBMSUtils.sendSOAPMessage(deliveryMessage, BACKEND_URL);

      //done
      LOG.info("DONE");
    });
    th.setName("submission-handler");
    th.start();
  }
}
