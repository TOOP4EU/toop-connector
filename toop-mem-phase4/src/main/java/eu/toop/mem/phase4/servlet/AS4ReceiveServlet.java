package eu.toop.mem.phase4.servlet;

import javax.servlet.annotation.WebServlet;

import com.helger.as4.servlet.AS4XServletHandler;
import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

/**
 * Local AS4 servlet
 * 
 * @author Philip Helger
 */
@WebServlet ("/phase4")
public class AS4ReceiveServlet extends AbstractXServlet
{
  public AS4ReceiveServlet ()
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);
    handlerRegistry ().registerHandler (EHttpMethod.POST, new AS4XServletHandler ());
  }
}
