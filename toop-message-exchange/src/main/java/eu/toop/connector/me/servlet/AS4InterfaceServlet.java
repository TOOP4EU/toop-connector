/**
 * Copyright (C) 2018 toop.eu <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package eu.toop.connector.me.servlet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.me.EBMSUtils;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.SoapUtil;
import eu.toop.connector.me.SoapXPathUtil;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
@WebServlet("/as4Interface")
public class AS4InterfaceServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(AS4InterfaceServlet.class);

  @Override
  protected void doGet(final HttpServletRequest req,
      final HttpServletResponse resp) throws ServletException, IOException {

    resp.setStatus(HttpServletResponse.SC_OK);
    resp.getOutputStream().println(TCConfig.getMEMAS4FromPartyID() + ": Please use POST");
  }

  @Override
  protected void doPost(final HttpServletRequest req,
      final HttpServletResponse resp) throws ServletException, IOException {

    LOG.info("Received a message from the gateway");

    // Convert the request headers into MimeHeaders
    final MimeHeaders mimeHeaders = readMimeHeaders(req);

    //no matter what happens, we will return either a receipt or a fault
    resp.setContentType("text/xml");

    SOAPMessage receivedMessage = null;
    try {
      byte[] bytes = IOUtils.toByteArray(req.getInputStream());

      LOG.debug("Read inbound message");
      //Todo, remove buffering later
      receivedMessage = SoapUtil.createMessage(mimeHeaders, new ByteArrayInputStream(bytes));

      //check if the message is a notification message

      if (LOG.isTraceEnabled()) {
        LOG.trace(SoapUtil.describe(receivedMessage));
      }

      //get the action from the soap message
      String action = SoapXPathUtil
          .safeFindSingleNode(receivedMessage.getSOAPHeader(), "//:CollaborationInfo/:Action").getTextContent();


      switch(action)  {
        case "Deliver" :
          processDelivery(receivedMessage);
          break;

        case "Notify" :
          processNotification(receivedMessage);
          break;

        default:
          throw new UnsupportedOperationException("Action " + action + " is not supported");
      }

      LOG.debug("Create success receipt");
      final byte[] successReceipt = EBMSUtils.createSuccessReceipt(receivedMessage);

      LOG.debug("Send success receipt");
      resp.setStatus(HttpServletResponse.SC_OK);
      IOUtils.write(successReceipt, resp.getOutputStream());
    } catch (final Throwable th) {
      LOG.error("Failed to process incoming AS4 message", th);
      LOG.debug("Create fault");
      byte[] fault = EBMSUtils.createFault(receivedMessage, th.getMessage());
      LOG.debug("Write fault to the stream");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      IOUtils.write(fault, resp.getOutputStream());
    }
  }

  private void processNotification(SOAPMessage receivedMessage) {
    LOG.debug("Received notification");
    LOG.debug(SoapUtil.describe(receivedMessage));
  }

  private void processDelivery(SOAPMessage receivedMessage) {
    LOG.debug("Dispatch inbound message");
    MEMDelegate.getInstance().dispatchInboundMessage(receivedMessage);
  }

  private MimeHeaders readMimeHeaders(final HttpServletRequest req) {
    final MimeHeaders mimeHeaders = new MimeHeaders();
    final Enumeration<String> headerNames = req.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      final String header = headerNames.nextElement();
      String reqHeader = req.getHeader(header);
      LOG.debug("HEADER " + header + " " + reqHeader);
      mimeHeaders.addHeader(header, reqHeader);
    }
    return mimeHeaders;
  }
}
