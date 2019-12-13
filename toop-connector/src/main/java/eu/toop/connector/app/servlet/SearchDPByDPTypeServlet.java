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

import javax.annotation.Nonnull;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.http.EHttpMethod;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXServlet;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

import eu.toop.connector.app.searchdp.ISearchDPCallback;
import eu.toop.connector.app.searchdp.SearchDPByDPTypeHandler;
import eu.toop.connector.app.searchdp.SearchDPByDPTypeInputParams;

/**
 * Search the TOOP Directory for specific identifiers. See
 * http://wiki.ds.unipi.gr/display/TOOP/Process+Variations+on+Discovery for
 * details
 *
 * @author Philip Helger
 */
@WebServlet ("/search-dp-by-dptype/*")
public class SearchDPByDPTypeServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "search-dp-by-dptype";
  public static final String SERVLET_DEFAULT_PATH = '/' + SERVLET_DEFAULT_NAME;

  /**
   * Main handler for the servlet
   *
   * @author Philip Helger
   */
  static final class MainHandler implements IXServletSimpleHandler
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (MainHandler.class);

    public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                               @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (SERVLET_DEFAULT_PATH + aRequestScope.getPathWithinServlet () + " executed");

      // Extract the parameters
      final SearchDPByDPTypeInputParams aInputParams = SearchDPByDPTypeHandler.extractInputParams (aRequestScope.getPathWithinServlet ());
      if (!aInputParams.hasDPType ())
      {
        // Mandatory fields are missing
        aUnifiedResponse.disableCaching ();
        aUnifiedResponse.setStatus (HttpServletResponse.SC_BAD_REQUEST);
      }
      else
      {
        // Search result callback
        final ISearchDPCallback aCallback = new SearchDPCallback (aUnifiedResponse);

        // Perform the search
        SearchDPByDPTypeHandler.performSearch (aInputParams, aCallback);
      }
    }
  }

  public SearchDPByDPTypeServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new MainHandler ());
  }
}
