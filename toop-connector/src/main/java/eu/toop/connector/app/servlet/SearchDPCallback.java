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
import javax.servlet.http.HttpServletResponse;

import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.write.XMLWriterSettings;

import eu.toop.connector.app.searchdp.ISearchDPCallback;

final class SearchDPCallback implements ISearchDPCallback
{
  private final UnifiedResponse m_aUnifiedResponse;

  SearchDPCallback (@Nonnull final UnifiedResponse aUnifiedResponse)
  {
    m_aUnifiedResponse = aUnifiedResponse;
  }

  public void onQueryDirectoryError (@Nonnull final ISimpleURL aQueryURL)
  {
    m_aUnifiedResponse.disableCaching ();
    m_aUnifiedResponse.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  public void onQueryDirectorySuccess (@Nonnull final IMicroDocument aDoc)
  {
    // Return "as is"
    final XMLWriterSettings aXWS = new XMLWriterSettings ();
    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    if (StringHelper.hasText (aDoc.getDocumentElement ().getNamespaceURI ()))
    {
      aNSCtx.setDefaultNamespaceURI (aDoc.getDocumentElement ().getNamespaceURI ());
    }
    aXWS.setNamespaceContext (aNSCtx);
    final String sResponse = MicroWriter.getNodeAsString (aDoc, aXWS);

    if (SearchDPByCountryServlet.MainHandler.LOGGER.isInfoEnabled ())
      SearchDPByCountryServlet.MainHandler.LOGGER.info ("Returning " +
                                                        sResponse.length () +
                                                        " characters of successul XML response back");

    // Put XML on response
    m_aUnifiedResponse.disableCaching ();
    m_aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                           aXWS.getCharset ().name ()));
    m_aUnifiedResponse.setContentAndCharset (sResponse, aXWS.getCharset ());
  }
}
