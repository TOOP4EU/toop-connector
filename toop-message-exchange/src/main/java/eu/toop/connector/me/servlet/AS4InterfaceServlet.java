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

import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;

import eu.toop.connector.me.EBMSUtils;
import eu.toop.connector.me.MEMConstants;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.SoapUtil;
import eu.toop.connector.me.SoapXPathUtil;

/**
 * @author myildiz at 15.02.2018.
 */
@WebServlet("/from-as4")
public class AS4InterfaceServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(AS4InterfaceServlet.class);

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

    LOG.info("Received a message from the gateway");

    // Convert the request headers into MimeHeaders
    final MimeHeaders mimeHeaders = readMimeHeaders(req);

    // no matter what happens, we will return either a receipt or a fault
    resp.setContentType(CMimeType.TEXT_XML.getAsString ());

    SOAPMessage receivedMessage = null;
    try {
      final byte[] bytes = StreamHelper.getAllBytes(req.getInputStream());

      if (LOG.isDebugEnabled()) {
        LOG.debug("Read inbound message");
      }

      // Todo, remove buffering later
      receivedMessage = SoapUtil.createMessage(mimeHeaders, new NonBlockingByteArrayInputStream(bytes));

      // check if the message is a notification message

      if (LOG.isTraceEnabled()) {
        LOG.trace(SoapUtil.describe(receivedMessage));
      }

      // get the action from the soap message
      final String action = SoapXPathUtil
          .getSingleNodeTextContent(receivedMessage.getSOAPHeader(), "//:CollaborationInfo/:Action");

      switch (action) {
        case MEMConstants.ACTION_DELIVER:
          processDelivery(receivedMessage);
          break;

        case MEMConstants.ACTION_RELAY:
          processRelayResult(receivedMessage);
          break;

        //does not exist in the standard CIT interface.
        case MEMConstants.ACTION_SUBMISSION_RESULT:
          processSubmissionResult(receivedMessage);
          break;

        default:
          throw new UnsupportedOperationException("Action '" + action + "' is not supported");
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("Create success receipt");
      }
      final byte[] successReceipt = EBMSUtils.createSuccessReceipt(receivedMessage);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Send success receipt");
      }
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.getOutputStream().write(successReceipt);
      resp.getOutputStream().flush();

      if (LOG.isDebugEnabled ())
        LOG.debug("Done processing inbound AS4 message");
    } catch (final Throwable th) {
      LOG.error("Error processing the message", th);
      sendBackFault(resp, receivedMessage, th);
    } finally {
      if (LOG.isDebugEnabled ())
        LOG.debug("End doPost");
    }

    // Don't close output stream
    // resp.getOutputStream().close();
  }

  /**
   * Create a fault message from the given input data and send it back to the client
   * @param resp HTTP Servlet response
   * @param receivedMessage Received SOAP message
   * @param th Exception that occurred
   * @throws IOException In case of IO error
   */
  protected void sendBackFault(final HttpServletResponse resp, final SOAPMessage receivedMessage, final Throwable th) throws IOException {
    LOG.error("Failed to process incoming AS4 message", th);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Create fault");
    }
    final byte[] fault = EBMSUtils.createFault(receivedMessage, th.getMessage());
    if (LOG.isDebugEnabled()) {
      LOG.debug("Write fault to the stream");
    }
    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    resp.getOutputStream().write(fault);
    resp.getOutputStream().flush();
  }

  protected void processSubmissionResult(final SOAPMessage submissionResult) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("------->> Received SubmissionResult <<-------");
      LOG.debug("Dispatch SubmissionResult");
      LOG.debug("\n" + SoapUtil.describe(submissionResult));
    }

    MEMDelegate.getInstance().dispatchSubmissionResult(submissionResult);
  }

  protected void processRelayResult(final SOAPMessage notification) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("------->> Received RelayResult <<-------");
      LOG.debug("Dispatch notification");
      LOG.debug("\n" + SoapUtil.describe(notification));
    }

    MEMDelegate.getInstance().dispatchRelayResult(notification);
  }

  protected void processDelivery(final SOAPMessage receivedMessage) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("------->> Received Delivery <<-------");
      LOG.debug("Dispatch inbound message");
      LOG.debug("\n" + SoapUtil.describe(receivedMessage));
    }

    MEMDelegate.getInstance().dispatchInboundMessage(receivedMessage);
  }

  protected MimeHeaders readMimeHeaders(final HttpServletRequest req) {
    final MimeHeaders mimeHeaders = new MimeHeaders();
    final Enumeration<String> headerNames = req.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      final String header = headerNames.nextElement();
      final String reqHeader = req.getHeader(header);
      if (LOG.isDebugEnabled()) {
        LOG.debug("HEADER " + header + " " + reqHeader);
      }
      mimeHeaders.addHeader(header, reqHeader);
    }
    return mimeHeaders;
  }
}
