package eu.toop.mp.me.servlet;

import eu.toop.mp.me.MEMDelegate;
import eu.toop.mp.me.SoapUtil;
import eu.toop.mp.me.EBMSUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.util.Enumeration;

/**
 * @author: myildiz
 * @date: 15.02.2018.
 */
public class AS4InterfaceServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.getOutputStream().println("Please use POST");
  }


  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
    //Convert the request headers into MimeHeaders
    MimeHeaders mimeHeaders = readMimeHeaders(req);
    try {
      SOAPMessage message = SoapUtil.createEmptyMessage(mimeHeaders, req.getInputStream());
      MEMDelegate.get().dispatchMessage(message);
      byte[] successReceipt = EBMSUtils.createSuccessReceipt(message);
      resp.getOutputStream().write(successReceipt);
      resp.setStatus(HttpServletResponse.SC_OK);
    } catch (SOAPException e) {
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      throw new IOException(e.getMessage(), e);
    }
  }

  private MimeHeaders readMimeHeaders(HttpServletRequest req) {
    MimeHeaders mimeHeaders = new MimeHeaders();
    Enumeration<String> headerNames = req.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String header = headerNames.nextElement();
      mimeHeaders.addHeader(header, req.getHeader(header));
    }
    return mimeHeaders;
  }
}
