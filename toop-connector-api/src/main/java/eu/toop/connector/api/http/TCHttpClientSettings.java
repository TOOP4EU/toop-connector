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
package eu.toop.connector.api.http;

import java.security.GeneralSecurityException;

import org.apache.http.HttpHost;

import com.helger.commons.exception.InitializationException;
import com.helger.httpclient.HttpClientSettings;

import eu.toop.connector.api.TCConfig;

/**
 * Common TOOP Connector HTTPClient factory
 *
 * @author Philip Helger
 */
public class TCHttpClientSettings extends HttpClientSettings
{
  public TCHttpClientSettings ()
  {
    if (TCConfig.isUseHttpSystemProperties ())
    {
      // For proxy etc
      setUseSystemProperties (true);
    }
    else
    {
      // Add settings from configuration file here centrally
      if (TCConfig.isProxyServerEnabled ())
      {
        setProxyHost (new HttpHost (TCConfig.getProxyServerAddress (), TCConfig.getProxyServerPort ()));

        // Non-proxy hosts
        addNonProxyHostsFromPipeString (TCConfig.getProxyServerNonProxyHosts ());
      }

      // Disable SSL checks?
      if (TCConfig.isTLSTrustAll ())
        try
        {
          setSSLContextTrustAll ();
          setHostnameVerifierVerifyAll ();
        }
        catch (final GeneralSecurityException ex)
        {
          throw new InitializationException (ex);
        }
    }
  }
}
