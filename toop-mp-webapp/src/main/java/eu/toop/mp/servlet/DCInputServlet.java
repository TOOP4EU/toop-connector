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
package eu.toop.mp.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.mime.CMimeType;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.servlet.response.UnifiedResponse;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.dataexchange.TDETOOPDataRequestType;
import eu.toop.commons.doctype.EToopDocumentType;
import eu.toop.commons.doctype.EToopProcess;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.kafkaclient.ToopKafkaClient;
import eu.toop.mp.api.MPSettings;
import eu.toop.mp.processor.MPWebAppConfig;
import eu.toop.mp.processor.MessageProcessorDCOutgoing;

/**
 * This method is called by the <code>toop-interface</code> project in the
 * direction DC to DP.<br>
 * The input is an ASiC archive that contains a {@link TDETOOPDataRequestType}.
 * If extracted successfully it is put in {@link MessageProcessorDCOutgoing} for
 * further processing.
 *
 * @author Philip Helger
 */
@WebServlet ("/dcinput")
public class DCInputServlet extends HttpServlet {
  private static final Logger s_aLogger = LoggerFactory.getLogger (DCInputServlet.class);

  /**
   * This is a demo method to easily send an MSDataRequest to itself. Invoke with
   * <code>http://localhost:8090/dcinput?demo</code>. This method must be disabled
   * in production!
   */
  @Override
  protected void doGet (final HttpServletRequest aHttpServletRequest,
                        final HttpServletResponse aHttpServletResponse) throws ServletException, IOException {
    // XXX DEMO code!
    if (aHttpServletRequest.getParameter ("demo") != null) {
      // Put a fake DC request in the queue
      final MockHttpServletRequest aMockRequest = new MockHttpServletRequest ();

      try (final NonBlockingByteArrayOutputStream archiveOutput = new NonBlockingByteArrayOutputStream ()) {
        // Create dummy request
        // TODO use correct document type ID/process ID
        ToopMessageBuilder.createRequestMessage (ToopMessageBuilder.createMockRequest (MPSettings.getIdentifierFactory ()
                                                                                                 .createParticipantIdentifier ("toop-actorid-upis",
                                                                                                                               "dcinput")
                                                                                                 .getURIEncoded (),
                                                                                       "DE",
                                                                                       EToopDocumentType.DOCTYPE1.getURIEncoded (),
                                                                                       EToopProcess.PROC.getURIEncoded (),
                                                                                       new CommonsArrayList<> (new ConceptValue ("company",
                                                                                                                                 "demo"))),
                                                 archiveOutput, MPWebAppConfig.getSignatureHelper ());
        // Get ASiC bytes
        aMockRequest.setContent (archiveOutput.toByteArray ());
      }

      // Post dummy request
      doPost (aMockRequest, aHttpServletResponse);

      if (aHttpServletResponse.getStatus () == HttpServletResponse.SC_NO_CONTENT) {
        // So that something is visible
        aHttpServletResponse.setStatus (HttpServletResponse.SC_OK);
        aHttpServletResponse.setContentType (CMimeType.TEXT_HTML.getAsString ());
        aHttpServletResponse.getWriter ()
                            .println ("<html><body><h1>Demo request processed successfully!</h1></body></html>");
        aHttpServletResponse.getWriter ().flush ();
      }
    } else {
      // Response with HTTP 405 (Method not supported)
      super.doGet (aHttpServletRequest, aHttpServletResponse);
    }
  }

  @Override
  protected void doPost (final HttpServletRequest aHttpServletRequest,
                         final HttpServletResponse aHttpServletResponse) throws ServletException, IOException {
    ToopKafkaClient.send ("MP got /dcinput request (1/4)");

    final UnifiedResponse aUR = UnifiedResponse.createSimple (aHttpServletRequest);

    // Parse POST data
    // No IToopDataRequest contained here
    final TDETOOPDataRequestType aRequestMsg = ToopMessageBuilder.parseRequestMessage (aHttpServletRequest.getInputStream ());

    if (aRequestMsg == null) {
      // The message content is invalid
      s_aLogger.error ("The request does not contain an ASiC archive, or the ASiC archive does not contain a TOOP DataRequest!");
      aUR.setStatus (HttpServletResponse.SC_BAD_REQUEST);
    } else {
      // Enqueue to processor and we're good
      MessageProcessorDCOutgoing.getInstance ().enqueue (aRequestMsg);

      // Done - no content
      aUR.setStatus (HttpServletResponse.SC_NO_CONTENT);
    }

    // Done
    aUR.applyToResponse (aHttpServletResponse);
  }
}
