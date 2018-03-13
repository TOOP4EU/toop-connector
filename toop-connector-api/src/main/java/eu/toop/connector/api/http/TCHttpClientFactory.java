package eu.toop.connector.api.http;

import com.helger.httpclient.HttpClientFactory;

/**
 * Common TOOP Connector HTTPClient factory
 *
 * @author Philip Helger
 *
 */
public final class TCHttpClientFactory extends HttpClientFactory {
  public TCHttpClientFactory () {
    // For proxy etc
    setUseSystemProperties (true);

    // Add settings from configuration file here centrally
  }
}
