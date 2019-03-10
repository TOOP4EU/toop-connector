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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * Main handler for the /sarch-dp servlet
 *
 * @author Philip Helger
 */
final class SearchDPXServletHandler implements IXServletSimpleHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SearchDPXServletHandler.class);
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("/search-dp" + aRequestScope.getPathWithinServlet () + " executed");

    // TODO
    final String sResponse = "<xml />";

    // Put JSON on response
    aUnifiedResponse.disableCaching ();
    aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                         CHARSET.name ()));
    aUnifiedResponse.setContentAndCharset (sResponse, CHARSET);
  }
}
