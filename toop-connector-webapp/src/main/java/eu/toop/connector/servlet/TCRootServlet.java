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

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.string.StringHelper;

/**
 * Servlet for handling the initial calls without any path. This servlet
 * redirects to "/index.html".
 *
 * @author Philip Helger
 */
@WebServlet ("")
public class TCRootServlet extends HttpServlet {
  @Override
  protected void doGet (@Nonnull final HttpServletRequest req,
                        @Nonnull final HttpServletResponse resp) throws ServletException, IOException {
    String sRedirectURL = req.getContextPath () + "/index.html";

    final String sQueryString = req.getQueryString ();
    if (StringHelper.hasText (sQueryString))
      sRedirectURL += "?" + sQueryString;

    resp.sendRedirect (sRedirectURL);
  }
}
