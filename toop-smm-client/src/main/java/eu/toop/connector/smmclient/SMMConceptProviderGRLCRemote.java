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
package eu.toop.connector.smmclient;

import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerJson;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.http.TCHttpClientFactory;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * This is the SMM client concept cache. It asks remotely if something is not in
 * the cache. Currently the cache is not persisted.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SMMConceptProviderGRLCRemote {
  // Static cache
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static final ICommonsMap<String, ICommonsMap<String, MappedValueList>> s_aCache = new CommonsHashMap<> ();

  private SMMConceptProviderGRLCRemote () {
  }

  /**
   * Remove all cache values.
   */
  public static void clearCache () {
    s_aRWLock.writeLocked (s_aCache::clear);
  }

  @Nullable
  private static MappedValueList _getFromCache (@Nonnull @Nonempty final String sSourceNamespace,
                                                @Nonnull @Nonempty final String sDestNamespace) {
    final ICommonsMap<String, MappedValueList> aPerSrcMap = s_aCache.get (sSourceNamespace);
    if (aPerSrcMap != null)
      return aPerSrcMap.get (sDestNamespace);
    return null;
  }

  /**
   * Get all mapped values from source to target namespace. If not present (in
   * cache) it is retrieved from the remote server.
   *
   * @param sLogPrefix
   *          Log prefix. May not be <code>null</code> but may be empty.
   * @param sSourceNamespace
   *          Source namespace to map from. May not be <code>null</code>.
   * @param sDestNamespace
   *          Target namespace to map to. May not be <code>null</code>.
   * @return The non-<code>null</code> but maybe empty list of mapped values.
   * @throws IOException
   *           In case fetching from server failed
   */
  @Nonnull
  public static MappedValueList getAllMappedValues (@Nonnull final String sLogPrefix,
                                                    @Nonnull final String sSourceNamespace,
                                                    @Nonnull final String sDestNamespace) throws IOException {
    ValueEnforcer.notNull (sLogPrefix, "LogPrefix");
    ValueEnforcer.notNull (sSourceNamespace, "SourceNamespace");
    ValueEnforcer.notNull (sDestNamespace, "DestNamespace");

    // First in read-lock for maximum speed
    MappedValueList ret = s_aRWLock.readLocked ( () -> _getFromCache (sSourceNamespace, sDestNamespace));
    if (ret == null) {
      // Not found - slow path
      s_aRWLock.writeLock ().lock ();
      try {
        // Try in write lock
        ret = _getFromCache (sSourceNamespace, sDestNamespace);
        if (ret == null) {
          // Not in cache - query from server and put in cache
          ret = remoteQueryAllMappedValues (sLogPrefix, sSourceNamespace, sDestNamespace);
          s_aCache.computeIfAbsent (sSourceNamespace, k -> new CommonsHashMap<> ()).put (sDestNamespace, ret);

          // Put in the map the other way around as well (inference)
          s_aCache.computeIfAbsent (sDestNamespace, k -> new CommonsHashMap<> ()).put (sSourceNamespace,
                                                                                       ret.getSwappedSourceAndDest ());
        }
      } finally {
        s_aRWLock.writeLock ().unlock ();
      }
    }

    return ret;
  }

  /**
   * HTTP GET caller
   *
   * @param aDestinationURL
   *          destination URL
   * @param aResponseHandler
   *          Response handler - basically defines the data type
   * @param aResultHandler
   *          The result handler that accepts the data type
   * @throws IOException
   *           In case of IO error
   * @param <T>
   *          data type to be handled
   */
  private static <T> void _httpClientGetJson (@Nonnull final ISimpleURL aDestinationURL,
                                              @Nonnull final ResponseHandler<T> aResponseHandler,
                                              @Nonnull final Consumer<T> aResultHandler) throws IOException {
    ValueEnforcer.notNull (aDestinationURL, "DestinationURL");
    ValueEnforcer.notNull (aResponseHandler, "ResponseHandler");
    ValueEnforcer.notNull (aResultHandler, "ResultHandler");

    final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();

    try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory)) {
      final HttpGet aGet = new HttpGet (aDestinationURL.getAsStringWithEncodedParameters ());
      // Add HTTP header - to get JSON and not XML
      aGet.addHeader ("Accept", "application/json,*;q=0");

      final T aResponse = aMgr.execute (aGet, aResponseHandler);
      aResultHandler.accept (aResponse);
    }
  }

  /**
   * This method unconditionally executes a remote request to retrieve all
   * mappings from source namespace to target namespace.
   *
   * @param sLogPrefix
   *          Log prefix. May not be <code>null</code> but may be empty.
   * @param sSourceNamespace
   *          Source namespace. May not be <code>null</code>.
   * @param sDestNamespace
   *          Target namespace. May not be <code>null</code>.
   * @return A list with all mapped values. Never <code>null</code>.
   * @throws IOException
   *           In case the HTTP connection has a problem
   */
  @Nonnull
  public static MappedValueList remoteQueryAllMappedValues (@Nonnull final String sLogPrefix,
                                                            @Nonnull final String sSourceNamespace,
                                                            @Nonnull final String sDestNamespace) throws IOException {
    ValueEnforcer.notNull (sSourceNamespace, "SourceNamespace");
    ValueEnforcer.notNull (sDestNamespace, "DestinationNamespace");

    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Remote querying SMM mappings from '" + sSourceNamespace
                                                  + "' to '" + sDestNamespace + "'");

    // Build URL with params etc.
    String sBaseURL = TCConfig.getSMMGRLCURL ();
    if (StringHelper.hasNoText (sBaseURL))
      throw new IllegalArgumentException ("SMM GRLC URL is missing!");
    if (!sBaseURL.endsWith ("/"))
      sBaseURL += "/";
    final ISimpleURL aDestinationURL = new SimpleURL (sBaseURL
                                                      + "api/JackJackie/toop-sparql/get-all-mapped-concepts-between-two-namespaces").add ("sourcenamespace",
                                                                                                                                          sSourceNamespace)
                                                                                                                                    .add ("targetnamespace",
                                                                                                                                          sDestNamespace);
    // Always no-debug
    final ResponseHandlerJson aJsonHandler = new ResponseHandlerJson (false);

    // Result object to be filled
    final MappedValueList ret = new MappedValueList ();

    // Execute HTTP request
    _httpClientGetJson (aDestinationURL, aJsonHandler, aJson -> {
      // Interpret result
      if (aJson.isObject ()) {
        final IJsonObject aResults = aJson.getAsObject ().getAsObject ("results");
        if (aResults != null) {
          final IJsonArray aBindings = aResults.getAsArray ("bindings");
          if (aBindings != null)
            for (final IJson aBinding : aBindings) {
              // subject (contains source namespace which needs to be cut)
              String sSourceValue = aBinding.getAsObject ().getAsObject ("s").getAsString ("value");
              sSourceValue = StringHelper.trimStart (sSourceValue, sSourceNamespace + "#");

              // object (contains destination namespace which needs to be cut)
              String sDestValue = aBinding.getAsObject ().getAsObject ("o").getAsString ("value");
              sDestValue = StringHelper.trimStart (sDestValue, sDestNamespace + "#");

              // Add result entry into list
              ret.addMappedValue (sSourceNamespace, sSourceValue, sDestNamespace, sDestValue);
            }
        }
      }
    });

    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> sLogPrefix + "SMM remote call returned " + ret.size () + " mapped values");

    return ret;
  }

  @Nonnull
  public static ICommonsOrderedSet<String> remoteQueryAllNamespaces (@Nonnull final String sLogPrefix) throws IOException {
    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Remote querying all SMM namespaces");

    // Build URL with params etc.
    String sBaseURL = TCConfig.getSMMGRLCURL ();
    if (StringHelper.hasNoText (sBaseURL))
      throw new IllegalArgumentException ("SMM GRLC URL is missing!");
    if (!sBaseURL.endsWith ("/"))
      sBaseURL += "/";
    final ISimpleURL aDestinationURL = new SimpleURL (sBaseURL + "api/JackJackie/toop-sparql/get-all-namespaces");
    // Always no-debug
    final ResponseHandlerJson aJsonHandler = new ResponseHandlerJson (false);

    // Result object to be filled
    final ICommonsOrderedSet<String> ret = new CommonsLinkedHashSet<> ();

    // Execute HTTP request
    _httpClientGetJson (aDestinationURL, aJsonHandler, aJson -> {
      // Interpret result
      if (aJson.isObject ()) {
        final IJsonObject aResults = aJson.getAsObject ().getAsObject ("results");
        if (aResults != null) {
          final IJsonArray aBindings = aResults.getAsArray ("bindings");
          if (aBindings != null)
            for (final IJson aBinding : aBindings) {
              final IJsonObject aNSObj = aBinding.getAsObject ().getAsObject ("ns");
              if (aNSObj != null) {
                final String sURL = aNSObj.getAsString ("value");
                if (StringHelper.hasText (sURL))
                  ret.add (sURL);
              }
            }
        }
      }
    });

    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> sLogPrefix + "SMM remote call returned " + ret.size () + " mapped values");

    return ret;
  }
}
