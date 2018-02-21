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

import com.helger.servlet.response.UnifiedResponse;

import eu.toop.commons.exchange.IToopDataResponse;
import eu.toop.commons.exchange.message.ToopMessageBuilder;
import eu.toop.commons.exchange.message.ToopResponseMessage;
import eu.toop.commons.exchange.mock.MSDataRequest;
import eu.toop.commons.exchange.mock.MSDataResponse;
import eu.toop.commons.exchange.mock.ToopDataRequest;
import eu.toop.mp.processor.MessageProcessorDPOutgoing;

/**
 * This method is called by the <code>toop-interface</code> project in the
 * direction DP to DC.<br>
 * The input is an ASiC archive that contains all fields of a
 * {@link ToopResponseMessage} except for the {@link IToopDataResponse} is used.
 * If extracted successfully it is put in {@link MessageProcessorDPOutgoing} for
 * further processing.
 *
 * @author Philip Helger
 */
@WebServlet ("/dpinput")
public class DPInputServlet extends HttpServlet {
  private static final Logger s_aLogger = LoggerFactory.getLogger (DPInputServlet.class);

  @Override
  protected void doPost (final HttpServletRequest aHttpServletRequest,
                         final HttpServletResponse aHttpServletResponse) throws ServletException, IOException {
    final UnifiedResponse aUR = UnifiedResponse.createSimple (aHttpServletRequest);

    // Parse POST data
    // No IToopDataResponse contained here
    final ToopResponseMessage aMsg = ToopMessageBuilder.parseResponseMessage (aHttpServletRequest.getInputStream (),
                                                                              MSDataRequest.getDeserializerFunction (),
                                                                              ToopDataRequest.getDeserializerFunction (),
                                                                              MSDataResponse.getDeserializerFunction (),
                                                                              null);

    if (aMsg == null) {
      // The message content is invalid
      s_aLogger.error ("The request does not contain an ASiC archive!");
      aUR.setStatus (HttpServletResponse.SC_BAD_REQUEST);
    } else {
      if (aMsg.getMSDataRequest () == null || aMsg.getToopDataRequest () == null || aMsg.getMSDataResponse () == null) {
        // The message content is invalid
        s_aLogger.error ("The ASiC archive does not contain all mandatory elements!");
        aUR.setStatus (HttpServletResponse.SC_BAD_REQUEST);
      } else {
        // Enqueue to processor and we're good
        MessageProcessorDPOutgoing.getInstance ().enqueue (aMsg);

        // Done - no content
        aUR.setStatus (HttpServletResponse.SC_NO_CONTENT);
      }
    }

    // Done
    aUR.applyToResponse (aHttpServletResponse);
  }
}
