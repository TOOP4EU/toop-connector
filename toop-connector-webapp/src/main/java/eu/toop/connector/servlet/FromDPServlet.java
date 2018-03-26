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
package eu.toop.connector.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.error.level.EErrorLevel;
import com.helger.servlet.response.UnifiedResponse;

import eu.toop.commons.dataexchange.TDETOOPDataResponseType;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.connector.mp.MessageProcessorDPOutgoing;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * This method is called by the <code>toop-interface</code> project in the
 * direction DP to DC.<br>
 * The input is an ASiC archive that contains a {@link TDETOOPDataResponseType}.
 * If extracted successfully it is put in {@link MessageProcessorDPOutgoing} for
 * further processing.
 *
 * @author Philip Helger
 */
@WebServlet ("/from-dp")
public class FromDPServlet extends HttpServlet {
  @Override
  protected void doPost (final HttpServletRequest aHttpServletRequest,
                         final HttpServletResponse aHttpServletResponse) throws ServletException, IOException {
    ToopKafkaClient.send (EErrorLevel.INFO, "MP got /from-dp request (3/4)");

    final UnifiedResponse aUR = UnifiedResponse.createSimple (aHttpServletRequest);

    // Parse POST data
    // No IToopDataResponse contained here
    final TDETOOPDataResponseType aResponseMsg = ToopMessageBuilder.parseResponseMessage (aHttpServletRequest.getInputStream ());

    if (aResponseMsg == null) {
      // The message content is invalid
      ToopKafkaClient.send (EErrorLevel.ERROR,
                            "The request does not contain an ASiC archive or the ASiC archive does not contain a TOOP Response Message!");
      aUR.setStatus (HttpServletResponse.SC_BAD_REQUEST);
    } else {
      // Enqueue to processor and we're good
      MessageProcessorDPOutgoing.getInstance ().enqueue (aResponseMsg);

      // Done - no content
      aUR.setStatus (HttpServletResponse.SC_NO_CONTENT);
    }

    // Done
    aUR.applyToResponse (aHttpServletResponse);
  }
}