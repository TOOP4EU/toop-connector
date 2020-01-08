/**
 * Copyright (C) 2018-2020 toop.eu
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
package eu.toop.connector.app.r2d2;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.methods.HttpGet;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerJson;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

import eu.toop.commons.error.EToopErrorCode;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.api.http.TCHttpClientFactory;
import eu.toop.connector.api.r2d2.IR2D2ErrorHandler;
import eu.toop.connector.api.r2d2.IR2D2ParticipantIDProvider;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * This class implements the {@link IR2D2ParticipantIDProvider} interface using
 * a remote query to TOOP Directory.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public class R2D2ParticipantIDProviderTOOPDirectory implements IR2D2ParticipantIDProvider
{
  private static final int MAX_RESULTS_PER_PAGE = 100;

  private final String m_sBaseURL;

  /**
   * Constructor using the TOOP Directory URL from the configuration file.
   */
  public R2D2ParticipantIDProviderTOOPDirectory ()
  {
    this (TCConfig.getR2D2DirectoryBaseUrl ());
  }

  /**
   * Constructor with an arbitrary TOOP Directory URL.
   *
   * @param sBaseURL
   *        The base URL to be used. May neither be <code>null</code> nor empty.
   */
  public R2D2ParticipantIDProviderTOOPDirectory (@Nonnull final String sBaseURL)
  {
    ValueEnforcer.notEmpty (sBaseURL, "BaseURL");
    m_sBaseURL = sBaseURL;
  }

  /**
   * @return The TOOP Directory Base URL as provided in the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public final String getBaseURL ()
  {
    return m_sBaseURL;
  }

  @Nullable
  private static IJsonObject _fetchJsonObject (@Nonnull final String sLogPrefix,
                                               @Nonnull final HttpClientManager aMgr,
                                               @Nonnull final ISimpleURL aURL) throws IOException
  {
    final HttpGet aGet = new HttpGet (aURL.getAsURI ());
    final ResponseHandlerJson aRH = new ResponseHandlerJson ();
    final IJson aJson = aMgr.execute (aGet, aRH);
    if (aJson != null && aJson.isObject ())
      return aJson.getAsObject ();

    ToopKafkaClient.send (EErrorLevel.ERROR,
                          () -> sLogPrefix +
                                "Failed to fetch " +
                                aURL.getAsStringWithEncodedParameters () +
                                " - stopping");
    return null;
  }

  @Nonnull
  public ICommonsSet <IParticipantIdentifier> getAllParticipantIDs (@Nonnull final String sLogPrefix,
                                                                    @Nonnull @Nonempty final String sCountryCode,
                                                                    @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                                                    @Nonnull final IR2D2ErrorHandler aErrorHandler)
  {
    final ICommonsSet <IParticipantIdentifier> ret = new CommonsHashSet <> ();

    final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();

    try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory))
    {
      // Build base URL and fetch x records per HTTP request
      final SimpleURL aBaseURL = new SimpleURL (m_sBaseURL + "/search/1.0/json")
                                                                                .add ("doctype",
                                                                                      aDocumentTypeID.getURIEncoded ())
                                                                                .add ("country", sCountryCode)
                                                                                .add ("rpc", MAX_RESULTS_PER_PAGE);

      // Fetch first object
      IJsonObject aResult = _fetchJsonObject (sLogPrefix, aMgr, aBaseURL);
      if (aResult != null)
      {
        // Start querying results
        int nResultPageIndex = 0;
        int nLoops = 0;
        while (true)
        {
          int nMatchCount = 0;
          final IJsonArray aMatches = aResult.getAsArray ("matches");
          if (aMatches != null)
          {
            for (final IJson aMatch : aMatches)
            {
              ++nMatchCount;
              final IJsonObject aID = aMatch.getAsObject ().getAsObject ("participantID");
              if (aID != null)
              {
                final String sScheme = aID.getAsString ("scheme");
                final String sValue = aID.getAsString ("value");
                final IParticipantIdentifier aPI = TCSettings.getIdentifierFactory ()
                                                             .createParticipantIdentifier (sScheme, sValue);
                if (aPI != null)
                  ret.add (aPI);
                else
                  ToopKafkaClient.send (EErrorLevel.WARN,
                                        () -> sLogPrefix +
                                              "Failed to create participant identifier from '" +
                                              sScheme +
                                              "' and '" +
                                              sValue +
                                              "'");
              }
              else
                ToopKafkaClient.send (EErrorLevel.WARN, () -> sLogPrefix + "Match does not contain participant ID");
            }
          }
          else
            ToopKafkaClient.send (EErrorLevel.WARN, () -> sLogPrefix + "JSON response contains no 'matches'");

          if (nMatchCount < MAX_RESULTS_PER_PAGE)
          {
            // Got less results than expected - end of list
            break;
          }

          if (++nLoops > MAX_RESULTS_PER_PAGE)
          {
            // Avoid endless loop
            ToopKafkaClient.send (EErrorLevel.ERROR, () -> sLogPrefix + "Endless loop in PD fetching?");
            break;
          }

          // Query next page
          nResultPageIndex++;
          aResult = _fetchJsonObject (sLogPrefix, aMgr, aBaseURL.getClone ().add ("rpi", nResultPageIndex));
          if (aResult == null)
          {
            // Unexpected error - stop querying
            // Error was already logged
            break;
          }
        }
      }
    }
    catch (final IOException ex)
    {
      aErrorHandler.onError (sLogPrefix +
                             "Error querying TOOP Directory for matches (" +
                             sCountryCode +
                             ", " +
                             aDocumentTypeID.getURIEncoded () +
                             ")",
                             ex,
                             EToopErrorCode.DD_001);
    }

    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("BaseURL", m_sBaseURL).getToString ();
  }
}
