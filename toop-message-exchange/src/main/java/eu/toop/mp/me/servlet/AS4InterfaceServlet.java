package eu.toop.mp.me.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.mp.me.EBMSUtils;
import eu.toop.mp.me.MEMDelegate;
import eu.toop.mp.me.SoapUtil;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
@WebServlet ("/as4Interface")
public class AS4InterfaceServlet extends HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger (AS4InterfaceServlet.class);

  @Override
  protected void doGet (final HttpServletRequest req,
                        final HttpServletResponse resp) throws ServletException, IOException {
    resp.setStatus (HttpServletResponse.SC_OK);
    resp.getOutputStream ().println ("Please use POST");
  }

  @Override
  protected void doPost (final HttpServletRequest req,
                         final HttpServletResponse resp) throws ServletException, IOException {
    // Convert the request headers into MimeHeaders
    final MimeHeaders mimeHeaders = _readMimeHeaders (req);
    try {
      final SOAPMessage message = SoapUtil.createMessage (mimeHeaders, req.getInputStream ());
      MEMDelegate.getInstance ().dispatchInboundMessage (message);
      final byte[] successReceipt = EBMSUtils.createSuccessReceipt (message);

      final OutputStream aOS = resp.getOutputStream ();
      aOS.write (successReceipt);
      aOS.flush ();
      resp.setStatus (HttpServletResponse.SC_OK);
    } catch (final SOAPException e) {
      LOG.error ("Failed to process incoming AS4 message", e);
      resp.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      throw new ServletException ("Failed to process incoming AS4 message", e);
    }
  }

  private MimeHeaders _readMimeHeaders (final HttpServletRequest req) {
    final MimeHeaders mimeHeaders = new MimeHeaders ();
    final Enumeration<String> headerNames = req.getHeaderNames ();
    while (headerNames.hasMoreElements ()) {
      final String header = headerNames.nextElement ();
      mimeHeaders.addHeader (header, req.getHeader (header));
    }
    return mimeHeaders;
  }
}
