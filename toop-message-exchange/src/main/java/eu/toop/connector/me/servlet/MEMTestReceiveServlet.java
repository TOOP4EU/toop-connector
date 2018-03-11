/**
 * Copyright (C) 2018 toop.eu <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package eu.toop.connector.me.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.connector.me.IMessageHandler;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEMessage;
import eu.toop.connector.me.MEPayload;

/**
 * display the last message received and update the time
 *
 * @author: myildiz
 * @date: 15.02.2018.
 */
@WebServlet("/memTestR")
@Deprecated
public class MEMTestReceiveServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(MEMTestReceiveServlet.class);

  /**
   * The last message received from the other side
   */
  private static MEMessage lastMessage;

  /**
   * When did we receive the last message
   */
  private static String when;

  private static IMessageHandler handler = new IMessageHandler() {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss:SSS");

    @Override
    public void handleMessage(@Nonnull final MEMessage meMessage) throws Exception {
      LOG.debug("Handler received an inbound MEMessage");
      lastMessage = meMessage;
      //couldn't use PDTFactory.createLocalDate(when).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME
      //was throwing an exception (Caused by: java.time.temporal.UnsupportedTemporalTypeException: Unsupported field: HourOfDay)
      when = sdf.format(new Date(System.currentTimeMillis()));
      LOG.debug("When: " + when);
    }
  };

  static {
    MEMDelegate.getInstance().registerMessageHandler(handler);
  }

  @Override
  protected void doGet(final HttpServletRequest req,
      final HttpServletResponse resp) throws ServletException, IOException {
    resp.setStatus(HttpServletResponse.SC_OK);
    if (lastMessage == null) {
      resp.getOutputStream().println("No message received yet");
    } else {
      resp.getOutputStream().println("Date: " + when);

      resp.getOutputStream().println("Payloads:");
      for (final MEPayload payload : lastMessage.getPayloads()) {
        resp.getOutputStream().println("\tMime Type: " + payload.getMimeTypeString());

        final byte[] data = payload.getData();
        if (data.length <= 100) {
          resp.getOutputStream().println("\tData     : " + new String(data));
        } else {
          resp.getOutputStream()
              .println("\tData     : " + DatatypeConverter.printHexBinary(Arrays.copyOfRange(data, 0, 50)));
        }
      }
    }

    resp.getOutputStream().flush();
  }
}
