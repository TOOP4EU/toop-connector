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
package eu.toop.mp.smmclient;

import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerJson;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;

import eu.toop.mp.api.MPConfig;

/**
 * This is the SMM client concept cache. It asks remotely if something is not in
 * the cache. Currently the cache is not persisted.
 *
 * @author Philip Helger
 */
@ThreadSafe
final class SMMConceptCache {
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMMConceptCache.class);

  // Static cache
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static final ICommonsMap<String, ICommonsMap<String, MappedValueList>> s_aCache = new CommonsHashMap<> ();

  private SMMConceptCache () {
  }

  /**
   * Get all mapped values from source to target namespace. If not present (in
   * cache) it is received from the remote server.
   *
   *
   * @param sSourceNamespace
   *          Source namespace to map from. May neither be <code>null</code> nor
   *          empty.
   * @param sDestNamespace
   *          Target namespace to map to. May neither be <code>null</code> nor
   *          empty.
   * @return The non-null list of mapped values.
   * @throws IOException
   *           In case fetching from server failed
   */
  @Nonnull
  public static MappedValueList getAllMappedValues (@Nonnull @Nonempty final String sSourceNamespace,
                                                    @Nonnull @Nonempty final String sDestNamespace) throws IOException {
    // Retrieve from cache
    ICommonsMap<String, MappedValueList> aPerSrcMap = s_aRWLock.readLocked ( () -> s_aCache.get (sSourceNamespace));
    if (aPerSrcMap == null) {
      // Ensure source map is present - we need it anyway afterwards
      aPerSrcMap = s_aRWLock.writeLocked ( () -> s_aCache.computeIfAbsent (sSourceNamespace,
                                                                           k -> new CommonsHashMap<> ()));
    }

    // Try in read-lock
    MappedValueList ret;
    s_aRWLock.readLock ().lock ();
    try {
      ret = aPerSrcMap.get (sDestNamespace);
    } finally {
      s_aRWLock.readLock ().unlock ();
    }

    if (ret == null) {
      // Not found in read-lock
      s_aRWLock.writeLock ().lock ();
      try {
        // Try again in write lock (cannot use computeIfAbsent because of thrown
        // IOException)
        ret = aPerSrcMap.get (sDestNamespace);
        if (ret == null) {
          // Not in cache - query from server and put in cache
          ret = remoteQueryAllMappedValues (sSourceNamespace, sDestNamespace);
          aPerSrcMap.put (sDestNamespace, ret);
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
  private static <T> void _httpClientGet (@Nonnull final ISimpleURL aDestinationURL,
                                          @Nonnull final ResponseHandler<T> aResponseHandler,
                                          @Nonnull final Consumer<T> aResultHandler) throws IOException {
    ValueEnforcer.notNull (aDestinationURL, "DestinationURL");
    ValueEnforcer.notNull (aResponseHandler, "ResponseHandler");
    ValueEnforcer.notNull (aResultHandler, "ResultHandler");

    final HttpClientFactory aHCFactory = new HttpClientFactory ();
    // For proxy etc
    aHCFactory.setUseSystemProperties (true);

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
   * @param sSourceNamespace
   *          Source namespace. May neither be <code>null</code> nor empty.
   * @param sDestNamespace
   *          Target namespace. May neither be <code>null</code> nor empty.
   * @return A list with all mapped values. Never <code>null</code>.
   * @throws IOException
   *           In case the HTTP connection has a problem
   */
  @Nonnull
  public static MappedValueList remoteQueryAllMappedValues (@Nonnull @Nonempty final String sSourceNamespace,
                                                            @Nonnull @Nonempty final String sDestNamespace) throws IOException {
    ValueEnforcer.notEmpty (sSourceNamespace, "SourceNamespace");
    ValueEnforcer.notEmpty (sDestNamespace, "DestinationNamespace");

    s_aLogger.info ("Remote querying SMM mappings from '" + sSourceNamespace + "' to '" + sDestNamespace + "'");

    // Build URL with params etc.
    String sBaseURL = MPConfig.getSMMGRLCURL ();
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
    _httpClientGet (aDestinationURL, aJsonHandler, aJson -> {
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
    return ret;
  }
}
