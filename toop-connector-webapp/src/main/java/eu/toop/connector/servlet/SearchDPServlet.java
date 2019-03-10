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
package eu.toop.connector.servlet;

import javax.servlet.annotation.WebServlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.xservlet.AbstractXServlet;

/**
 * Search the TOOP Directory for identifier types used in certain countries
 * (maybe further limited to certain document types as well). See
 * http://jira.ds.unipi.gr/browse/CCTF-102 for the definition.
 *
 * @author Philip Helger
 */
@WebServlet ("/search-dp")
public class SearchDPServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "search-dp";
  public static final String SERVLET_DEFAULT_PATH = '/' + SERVLET_DEFAULT_NAME;

  public SearchDPServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new SearchDPXServletHandler ());
  }
}
