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

import eu.toop.commons.exchange.IMSDataRequest;
import eu.toop.commons.exchange.message.ToopMessageBuilder;
import eu.toop.commons.exchange.message.ToopRequestMessage;
import eu.toop.commons.exchange.mock.MSDataRequest;
import eu.toop.mp.processor.MessageProcessorDC;

/**
 * This method is called by the <code>toop-interface</code> project in the
 * direction DC to DP.<br>
 * The input is an ASiC archive that contains the fields for a
 * {@link ToopRequestMessage} where only {@link IMSDataRequest} is used.
 *
 * @author Philip Helger
 */
@WebServlet("/dcinput")
public class DCInputServlet extends HttpServlet {
  private static final Logger s_aLogger = LoggerFactory.getLogger(DCInputServlet.class);

  @Override
  protected void doPost(final HttpServletRequest aHttpServletRequest, final HttpServletResponse aHttpServletResponse)
      throws ServletException, IOException {
    final UnifiedResponse aUR = UnifiedResponse.createSimple(aHttpServletRequest);

    // Parse POST data
    final ToopRequestMessage aMsg = ToopMessageBuilder.parseRequestMessage(aHttpServletRequest.getInputStream(),
        MSDataRequest.getDeserializerFunction());

    if (aMsg == null) {
      // The message content is invalid
      s_aLogger.error("The request does not contain an ASiC archive!");
      aUR.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } else {
      final IMSDataRequest aMSRequest = aMsg.getMSDataRequest();
      if (aMSRequest == null) {
        // The message content is invalid
        s_aLogger.error("The ASiC archive does not contain an MSDataRequest!");
        aUR.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } else {
        // Enqueue to processor and we're good
        MessageProcessorDC.getInstance().enqueue(aMSRequest);

        // Done - no content
        aUR.setStatus(HttpServletResponse.SC_NO_CONTENT);
      }
    }

    // Done
    aUR.applyToResponse(aHttpServletResponse);
  }
}
