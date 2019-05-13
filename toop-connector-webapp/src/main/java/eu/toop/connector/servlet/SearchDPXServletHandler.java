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

import java.io.Serializable;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.locale.country.CountryCache;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SimpleURL;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerMicroDom;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.api.http.TCHttpClientFactory;

/**
 * Main handler for the /sarch-dp servlet
 *
 * @author Philip Helger
 */
final class SearchDPXServletHandler implements IXServletSimpleHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SearchDPXServletHandler.class);

  private static final class InputParams implements Serializable
  {
    private Locale m_aCountryCode;
    private IDocumentTypeIdentifier m_aDocTypeID;

    @Nonnull
    public ESuccess setCountryCode (@Nullable final String sCountryCode)
    {
      if (StringHelper.hasText (sCountryCode))
      {
        final String sTrimmedCountryCode = sCountryCode.trim ();
        final Locale aCountry = CountryCache.getInstance ().getCountry (sTrimmedCountryCode);
        if (aCountry != null)
        {
          m_aCountryCode = aCountry;
          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Using country code '" + sTrimmedCountryCode + "' for /search-dp");
          return ESuccess.SUCCESS;
        }
        if (LOGGER.isWarnEnabled ())
          LOGGER.warn ("Country code '" + sTrimmedCountryCode + "' could not be resolved to a valid country");
      }
      return ESuccess.FAILURE;
    }

    @Nullable
    public Locale getCountryCode ()
    {
      return m_aCountryCode;
    }

    public boolean hasCountryCode ()
    {
      return m_aCountryCode != null;
    }

    @Nonnull
    public ESuccess setDocumentType (@Nullable final String sDocTypeID)
    {
      if (StringHelper.hasText (sDocTypeID))
      {
        final String sTrimmedDocTypeID = sDocTypeID.trim ();
        final IDocumentTypeIdentifier aDocTypeID = TCSettings.getIdentifierFactory ()
                                                             .parseDocumentTypeIdentifier (sTrimmedDocTypeID);
        if (aDocTypeID != null)
        {
          m_aDocTypeID = aDocTypeID;
          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Using document type ID '" + sTrimmedDocTypeID + "' for /search-dp");
          return ESuccess.SUCCESS;
        }
        if (LOGGER.isWarnEnabled ())
          LOGGER.warn ("Document type ID '" + sTrimmedDocTypeID + "' could not be parsed");
      }
      return ESuccess.FAILURE;
    }

    @Nullable
    public IDocumentTypeIdentifier getDocumentTypeID ()
    {
      return m_aDocTypeID;
    }

    public boolean hasDocumentTypeID ()
    {
      return m_aDocTypeID != null;
    }
  }

  @Nonnull
  private static InputParams _extractInputParams (@Nonnull final String sPathWithinServlet)
  {
    final InputParams ret = new InputParams ();
    final String sBase = StringHelper.trimStartAndEnd (sPathWithinServlet, '/');
    final String [] aParts = StringHelper.getExplodedArray ('/', sBase);
    if (aParts.length >= 1)
    {
      ret.setCountryCode (aParts[0]);
      if (aParts.length >= 2)
      {
        ret.setDocumentType (aParts[1]);
      }
    }
    return ret;
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("/search-dp" + aRequestScope.getPathWithinServlet () + " executed");

    // Extract the parameters as in "/search-dp/<countryCode>[/<docType>]"
    final InputParams aInputParams = _extractInputParams (aRequestScope.getPathWithinServlet ());

    if (!aInputParams.hasCountryCode ())
    {
      // Mandatory fields are missing
      aUnifiedResponse.disableCaching ();
      aUnifiedResponse.setStatus (HttpServletResponse.SC_BAD_REQUEST);
    }
    else
    {
      // Invoke TOOP Directory search API
      final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();

      try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory))
      {
        // Build base URL and fetch all records per HTTP request
        final SimpleURL aBaseURL = new SimpleURL (TCConfig.getR2D2DirectoryBaseUrl () + "/search/1.0/xml");
        // More than 1000 is not allowed
        aBaseURL.add ("rpc", 1_000);
        // Constant defined in CCTF-103
        aBaseURL.add ("identifierScheme", "DataSubjectIdentifierScheme");
        // Parameters to this servlet
        aBaseURL.add ("country", aInputParams.getCountryCode ().getCountry ());
        if (aInputParams.hasDocumentTypeID ())
          aBaseURL.add ("doctype", aInputParams.getDocumentTypeID ().getURIEncoded ());

        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Querying " + aBaseURL.getAsStringWithEncodedParameters ());

        final HttpGet aGet = new HttpGet (aBaseURL.getAsURI ());
        final ResponseHandlerMicroDom aRH = new ResponseHandlerMicroDom ();
        final IMicroDocument aDoc = aMgr.execute (aGet, aRH);
        if (aDoc == null || aDoc.getDocumentElement () == null)
        {
          // Mandatory fields are missing
          if (LOGGER.isErrorEnabled ())
            LOGGER.error ("Failed to invoke the Directory query '" +
                          aBaseURL.getAsStringWithEncodedParameters () +
                          "'");

          aUnifiedResponse.disableCaching ();
          aUnifiedResponse.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        else
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

          if (LOGGER.isInfoEnabled ())
            LOGGER.info ("Returning " + sResponse.length () + " characters of successul XML response back");

          // Put XML on response
          aUnifiedResponse.disableCaching ();
          aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_XML).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                               aXWS.getCharset ()
                                                                                                   .name ()));
          aUnifiedResponse.setContentAndCharset (sResponse, aXWS.getCharset ());
        }
      }
    }
  }
}
