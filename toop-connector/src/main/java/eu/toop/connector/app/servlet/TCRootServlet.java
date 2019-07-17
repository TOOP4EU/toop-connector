/**
 * Copyright (C) 2018-2019 toop.eu
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
package eu.toop.connector.app.servlet;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.mime.CMimeType;

import eu.toop.connector.api.as4.IMessageExchangeSPI;
import eu.toop.connector.api.as4.MessageExchangeManager;
import eu.toop.connector.app.CTC;
import eu.toop.connector.app.mp.MPConfig;

/**
 * Servlet for handling the initial calls without any path. This servlet
 * redirects to "/index.html".
 *
 * @author Philip Helger
 */
@WebServlet ("")
public class TCRootServlet extends HttpServlet
{
  @Override
  protected void doGet (@Nonnull final HttpServletRequest req,
                        @Nonnull final HttpServletResponse resp) throws ServletException, IOException
  {
    final String sCSS = "* { font-family: sans-serif; }" +
                        " a:link, a:visited, a:hover, a:active { color: #2255ff; }" +
                        " code { font-family:monospace; color:#e83e8c; }";

    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("<html><head><title>TOOP Connector</title><style>").append (sCSS).append ("</style></head><body>");
    aSB.append ("<h1>TOOP Connector</h1>");
    aSB.append ("<div>Version: ").append (CTC.getVersionNumber ()).append ("</div>");
    aSB.append ("<div>Build timestamp: ").append (CTC.getBuildTimestamp ()).append ("</div>");
    aSB.append ("<div>Current time: ").append (PDTFactory.getCurrentZonedDateTimeUTC ().toString ()).append ("</div>");
    aSB.append ("<a href='tc-status'>Check /tc-status</a>");

    {
      aSB.append ("<h2>Registered Message Exchange implementations</h2>");
      for (final Map.Entry <String, IMessageExchangeSPI> aEntry : CollectionHelper.getSortedByKey (MessageExchangeManager.getAll ())
                                                                                  .entrySet ())
      {
        aSB.append ("<div>ID <code>")
           .append (aEntry.getKey ())
           .append ("</code> mapped to ")
           .append (aEntry.getValue ())
           .append ("</div>");
      }
    }

    {
      aSB.append ("<h2>Message Processor Configuration</h2>");

      aSB.append ("<div><code>SignatureHelper</code>=").append (MPConfig.getSignatureHelper ()).append ("</div>");
      aSB.append ("<div><code>SMMConceptProvider</code>=").append (MPConfig.getSMMConceptProvider ()).append ("</div>");
      aSB.append ("<div><code>ParticipantIDProvider</code>=")
         .append (MPConfig.getParticipantIDProvider ())
         .append ("</div>");
      aSB.append ("<div><code>EndpointProvider</code>=").append (MPConfig.getEndpointProvider ()).append ("</div>");
    }

    if (GlobalDebug.isDebugMode ())
    {
      aSB.append ("<h2>Debug information</h2>");
      for (final Map.Entry <String, ? extends ServletRegistration> aEntry : CollectionHelper.getSortedByKey (req.getServletContext ()
                                                                                                                .getServletRegistrations ())
                                                                                            .entrySet ())
      {
        aSB.append ("<div>Servlet <code>")
           .append (aEntry.getKey ())
           .append ("</code> mapped to ")
           .append (aEntry.getValue ().getMappings ())
           .append ("</div>");
      }
    }

    aSB.append ("</body></html>");

    resp.addHeader (CHttpHeader.CONTENT_TYPE, CMimeType.TEXT_HTML.getAsString ());
    resp.getWriter ().write (aSB.toString ());
    resp.getWriter ().flush ();
  }
}
