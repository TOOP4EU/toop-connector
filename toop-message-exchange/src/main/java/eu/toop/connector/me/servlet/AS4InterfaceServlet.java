/**
 * Copyright (C) 2018 toop.eu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package eu.toop.connector.me.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.StreamHelper;

import eu.toop.connector.me.EBMSUtils;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.SoapUtil;
import eu.toop.connector.me.SoapXPathUtil;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
@WebServlet ("/from-as4")
public class AS4InterfaceServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger (AS4InterfaceServlet.class);

  // No need to overwrite doGet - returns HTTP 405 by default

  @Override
  protected void doPost (final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

    LOG.info ("Received a message from the gateway");

    // Convert the request headers into MimeHeaders
    final MimeHeaders mimeHeaders = readMimeHeaders (req);

    // no matter what happens, we will return either a receipt or a fault
    resp.setContentType ("text/xml");

    SOAPMessage receivedMessage = null;
    try {
      final byte[] bytes = StreamHelper.getAllBytes (req.getInputStream ());

      if (LOG.isDebugEnabled ()) {
        LOG.debug ("Read inbound message");
      }

      // Todo, remove buffering later
      receivedMessage = SoapUtil.createMessage (mimeHeaders, new NonBlockingByteArrayInputStream (bytes));

      // check if the message is a notification message

      if (LOG.isTraceEnabled ()) {
        LOG.trace (SoapUtil.describe (receivedMessage));
      }

      // get the action from the soap message
      final Node node = SoapXPathUtil.safeFindSingleNode (receivedMessage.getSOAPHeader (), "//:CollaborationInfo/:Action");
      final String action = node.getTextContent ();

      switch (action) {
      case "Deliver":
        processDelivery (receivedMessage);
        break;

      case "Notify":
        processNotification (receivedMessage);
        break;

      default:
        throw new UnsupportedOperationException ("Action '" + action + "' is not supported");
      }

      if (LOG.isDebugEnabled ()) {
        LOG.debug ("Create success receipt");
      }
      final byte[] successReceipt = EBMSUtils.createSuccessReceipt (receivedMessage);

      if (LOG.isDebugEnabled ()) {
        LOG.debug ("Send success receipt");
      }
      resp.setStatus (HttpServletResponse.SC_OK);
      resp.getOutputStream ().write (successReceipt);
      resp.getOutputStream ().flush ();
    } catch (final Throwable th) {
      LOG.error ("Failed to process incoming AS4 message", th);
      if (LOG.isDebugEnabled ()) {
        LOG.debug ("Create fault");
      }
      final byte[] fault = EBMSUtils.createFault (receivedMessage, th.getMessage ());
      if (LOG.isDebugEnabled ()) {
        LOG.debug ("Write fault to the stream");
      }
      resp.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      resp.getOutputStream ().write (fault);
      resp.getOutputStream ().flush ();
    }
  }

  private void processNotification (final SOAPMessage notification) {
    if (LOG.isDebugEnabled ()) {
      LOG.debug ("------->> Received notification <<-------");
      LOG.debug ("Dispatch notification");
    }

    if (LOG.isTraceEnabled ()) {
      LOG.debug (SoapUtil.describe (notification));
    }

    MEMDelegate.getInstance ().dispatchNotification (notification);
  }

  private void processDelivery (final SOAPMessage receivedMessage) {
    if (LOG.isDebugEnabled ()) {
      LOG.debug ("------->> Received notification <<-------");
      LOG.debug ("Dispatch inbound message");
    }

    if (LOG.isTraceEnabled ()) {
      LOG.debug (SoapUtil.describe (receivedMessage));
    }

    MEMDelegate.getInstance ().dispatchInboundMessage (receivedMessage);
  }

  private MimeHeaders readMimeHeaders (final HttpServletRequest req) {
    final MimeHeaders mimeHeaders = new MimeHeaders ();
    final Enumeration<String> headerNames = req.getHeaderNames ();
    while (headerNames.hasMoreElements ()) {
      final String header = headerNames.nextElement ();
      final String reqHeader = req.getHeader (header);
      if (LOG.isDebugEnabled ()) {
        LOG.debug ("HEADER " + header + " " + reqHeader);
      }
      mimeHeaders.addHeader (header, reqHeader);
    }
    return mimeHeaders;
  }
}
