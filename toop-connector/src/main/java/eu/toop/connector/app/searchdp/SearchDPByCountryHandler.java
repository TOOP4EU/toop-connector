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
package eu.toop.connector.app.searchdp;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SimpleURL;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerMicroDom;
import com.helger.xml.microdom.IMicroDocument;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.http.TCHttpClientFactory;

/**
 * Handler to perform the /search-dp-by-country servlet functionality.
 *
 * @author Philip Helger
 */
@Immutable
public final class SearchDPByCountryHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SearchDPByCountryHandler.class);

  private SearchDPByCountryHandler ()
  {}

  /**
   * Extract from the URL in the form
   * <code>/search-dp-by-country/<countryCode>[/<docType>]</code>
   *
   * @param sPathWithinServlet
   *        Path to extract from
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static SearchDPByCountryInputParams extractInputParams (@Nonnull final String sPathWithinServlet)
  {
    final SearchDPByCountryInputParams ret = new SearchDPByCountryInputParams ();
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

  public static void performSearch (@Nonnull final SearchDPByCountryInputParams aInputParams,
                                    @Nonnull final ISearchDPCallback aCallback) throws IOException
  {
    ValueEnforcer.notNull (aInputParams, "InputParams");
    ValueEnforcer.isTrue ( () -> aInputParams.hasCountryCode (), "InputParams must have a country code");
    ValueEnforcer.notNull (aCallback, "Callback");

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
          LOGGER.error ("Failed to invoke the Directory query '" + aBaseURL.getAsStringWithEncodedParameters () + "'");

        aCallback.onQueryDirectoryError (aBaseURL);
      }
      else
      {
        // Return "as is"
        aCallback.onQueryDirectorySuccess (aDoc);
      }
    }
  }
}
