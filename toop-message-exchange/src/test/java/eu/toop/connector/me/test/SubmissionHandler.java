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

import javax.xml.soap.SOAPMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.url.URLHelper;

import eu.toop.connector.me.EBMSUtils;

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

  public static void handle(final SOAPMessage receivedMessage) {
    final Thread th = new Thread(() -> {
      //send back a submission result

      LOG.info("Handle submission for " + EBMSUtils.getMessageId(receivedMessage));



      LOG.info("Send back a submission result");
      final SOAPMessage submissionResult = DummyEBMSUtils.inferSubmissionResult(receivedMessage);

      EBMSUtils.sendSOAPMessage(submissionResult, BACKEND_URL);

      //wait a bit
      ThreadHelper.sleep(1000);

      LOG.info("Send back a relay result");
      final SOAPMessage relayResult = DummyEBMSUtils.inferRelayResult(receivedMessage);

      EBMSUtils.sendSOAPMessage(relayResult, BACKEND_URL);

      //wait a bit
      ThreadHelper.sleep(1000);

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
