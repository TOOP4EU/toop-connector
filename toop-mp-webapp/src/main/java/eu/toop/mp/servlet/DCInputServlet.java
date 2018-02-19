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

import eu.toop.commons.exchange.message.ToopMessageBuilder;
import eu.toop.commons.exchange.message.ToopRequestMessage;
import eu.toop.commons.exchange.mock.MSDataRequest;
import eu.toop.commons.exchange.mock.ToopDataRequest;

/**
 * This method is called by the <code>toop-interface</code> project in the
 * direction DC to DP.<br>
 * The input is an ASiC archive that contains the fields for a
 * {@link ToopRequestMessage}.
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
        MSDataRequest.getDeserializerFunction(), ToopDataRequest.getDeserializerFunction());

    if (aMsg == null) {
      // The message content is invalid
      s_aLogger.error("The request does not contain an ASiC archive!");
      aUR.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } else {
      // TODO ctd
      aUR.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    // Done
    aUR.applyToResponse(aHttpServletResponse);
  }
}
