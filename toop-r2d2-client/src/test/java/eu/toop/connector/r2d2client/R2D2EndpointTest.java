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
package eu.toop.connector.r2d2client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;

import com.helger.commons.exception.InitializationException;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.security.certificate.CertificateHelper;

import eu.toop.connector.api.TCSettings;

/**
 * Test class for class {@link R2D2Endpoint}.
 *
 * @author Philip Helger
 */
public final class R2D2EndpointTest
{
  private static final X509Certificate TEST_CERT;
  private static final X509Certificate TEST_CERT2;

  static
  {
    // The web page certificate from Google
    final String sValidCert = "MIIDVDCCAjygAwIBAgIDAjRWMA0GCSqGSIb3DQEBBQUAMEIxCzAJBgNVBAYTAlVT\r\n" +
                              "MRYwFAYDVQQKEw1HZW9UcnVzdCBJbmMuMRswGQYDVQQDExJHZW9UcnVzdCBHbG9i\r\n" +
                              "YWwgQ0EwHhcNMDIwNTIxMDQwMDAwWhcNMjIwNTIxMDQwMDAwWjBCMQswCQYDVQQG\r\n" +
                              "EwJVUzEWMBQGA1UEChMNR2VvVHJ1c3QgSW5jLjEbMBkGA1UEAxMSR2VvVHJ1c3Qg\r\n" +
                              "R2xvYmFsIENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2swYYzD9\r\n" +
                              "9BcjGlZ+W988bDjkcbd4kdS8odhM+KhDtgPpTSEHCIjaWC9mOSm9BXiLnTjoBbdq\r\n" +
                              "fnGk5sRgprDvgOSJKA+eJdbtg/OtppHHmMlCGDUUna2YRpIuT8rxh0PBFpVXLVDv\r\n" +
                              "iS2Aelet8u5fa9IAjbkU+BQVNdnARqN7csiRv8lVK83Qlz6cJmTM386DGXHKTubU\r\n" +
                              "1XupGc1V3sjs0l44U+VcT4wt/lAjNvxm5suOpDkZALeVAjmRCw7+OC7RHQWa9k0+\r\n" +
                              "bw8HHa8sHo9gOeL6NlMTOdReJivbPagUvTLrGAMoUgRx5aszPeE4uwc2hGKceeoW\r\n" +
                              "MPRfwCvocWvk+QIDAQABo1MwUTAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBTA\r\n" +
                              "ephojYn7qwVkDBF9qn1luMrMTjAfBgNVHSMEGDAWgBTAephojYn7qwVkDBF9qn1l\r\n" +
                              "uMrMTjANBgkqhkiG9w0BAQUFAAOCAQEANeMpauUvXVSOKVCUn5kaFOSPeCpilKIn\r\n" +
                              "Z57QzxpeR+nBsqTP3UEaBU6bS+5Kb1VSsyShNwrrZHYqLizz/Tt1kL/6cdjHPTfS\r\n" +
                              "tQWVYrmm3ok9Nns4d0iXrKYgjy6myQzCsplFAMfOEVEiIuCl6rYVSAlk6l5PdPcF\r\n" +
                              "PseKUgzbFbS9bZvlxrFUaKnjaZC2mqUPuLk/IH2uSrW4nOQdtqvmlKXBx4Ot2/Un\r\n" +
                              "hw4EbNX/3aBd7YdStysVAq45pmp06drE57xNNB6pXE0zX5IJL4hmXXeXxx12E6nV\r\n" +
                              "5fEWCRE11azbJHFwLJhWC9kXtNHjUStedejV0NxPNO3CBWaAocvmMw==";
    try
    {
      TEST_CERT = CertificateHelper.convertStringToCertficate (sValidCert);
      assertNotNull (TEST_CERT);
    }
    catch (final CertificateException ex)
    {
      throw new InitializationException (ex);
    }

    final String sValidCert2 = "-----BEGIN CERTIFICATE-----\r\n" +
                               "MIIHgzCCBmugAwIBAgIISPRinMSib3kwDQYJKoZIhvcNAQELBQAwSTELMAkGA1UE\r\n" +
                               "BhMCVVMxEzARBgNVBAoTCkdvb2dsZSBJbmMxJTAjBgNVBAMTHEdvb2dsZSBJbnRl\r\n" +
                               "cm5ldCBBdXRob3JpdHkgRzIwHhcNMTgwNDE3MTMxMTA4WhcNMTgwNzEwMTIzODAw\r\n" +
                               "WjBmMQswCQYDVQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwN\r\n" +
                               "TW91bnRhaW4gVmlldzETMBEGA1UECgwKR29vZ2xlIEluYzEVMBMGA1UEAwwMKi5n\r\n" +
                               "b29nbGUuY29tMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEMltn+LBdz8P01u3E\r\n" +
                               "JGdNTQTKWGmolOmP7hqEgfeMFJ81CH5idU5YCYH23+dZ8aLerJwCOZlgDZrfDZLy\r\n" +
                               "9p5ooaOCBRswggUXMBMGA1UdJQQMMAoGCCsGAQUFBwMBMA4GA1UdDwEB/wQEAwIH\r\n" +
                               "gDCCA+EGA1UdEQSCA9gwggPUggwqLmdvb2dsZS5jb22CDSouYW5kcm9pZC5jb22C\r\n" +
                               "FiouYXBwZW5naW5lLmdvb2dsZS5jb22CEiouY2xvdWQuZ29vZ2xlLmNvbYIUKi5k\r\n" +
                               "YjgzMzk1My5nb29nbGUuY26CBiouZy5jb4IOKi5nY3AuZ3Z0Mi5jb22CFiouZ29v\r\n" +
                               "Z2xlLWFuYWx5dGljcy5jb22CCyouZ29vZ2xlLmNhggsqLmdvb2dsZS5jbIIOKi5n\r\n" +
                               "b29nbGUuY28uaW6CDiouZ29vZ2xlLmNvLmpwgg4qLmdvb2dsZS5jby51a4IPKi5n\r\n" +
                               "b29nbGUuY29tLmFygg8qLmdvb2dsZS5jb20uYXWCDyouZ29vZ2xlLmNvbS5icoIP\r\n" +
                               "Ki5nb29nbGUuY29tLmNvgg8qLmdvb2dsZS5jb20ubXiCDyouZ29vZ2xlLmNvbS50\r\n" +
                               "coIPKi5nb29nbGUuY29tLnZuggsqLmdvb2dsZS5kZYILKi5nb29nbGUuZXOCCyou\r\n" +
                               "Z29vZ2xlLmZyggsqLmdvb2dsZS5odYILKi5nb29nbGUuaXSCCyouZ29vZ2xlLm5s\r\n" +
                               "ggsqLmdvb2dsZS5wbIILKi5nb29nbGUucHSCEiouZ29vZ2xlYWRhcGlzLmNvbYIP\r\n" +
                               "Ki5nb29nbGVhcGlzLmNughQqLmdvb2dsZWNvbW1lcmNlLmNvbYIRKi5nb29nbGV2\r\n" +
                               "aWRlby5jb22CDCouZ3N0YXRpYy5jboINKi5nc3RhdGljLmNvbYIKKi5ndnQxLmNv\r\n" +
                               "bYIKKi5ndnQyLmNvbYIUKi5tZXRyaWMuZ3N0YXRpYy5jb22CDCoudXJjaGluLmNv\r\n" +
                               "bYIQKi51cmwuZ29vZ2xlLmNvbYIWKi55b3V0dWJlLW5vY29va2llLmNvbYINKi55\r\n" +
                               "b3V0dWJlLmNvbYIWKi55b3V0dWJlZWR1Y2F0aW9uLmNvbYIHKi55dC5iZYILKi55\r\n" +
                               "dGltZy5jb22CGmFuZHJvaWQuY2xpZW50cy5nb29nbGUuY29tggthbmRyb2lkLmNv\r\n" +
                               "bYIbZGV2ZWxvcGVyLmFuZHJvaWQuZ29vZ2xlLmNughxkZXZlbG9wZXJzLmFuZHJv\r\n" +
                               "aWQuZ29vZ2xlLmNuggRnLmNvggZnb28uZ2yCFGdvb2dsZS1hbmFseXRpY3MuY29t\r\n" +
                               "ggpnb29nbGUuY29tghJnb29nbGVjb21tZXJjZS5jb22CGHNvdXJjZS5hbmRyb2lk\r\n" +
                               "Lmdvb2dsZS5jboIKdXJjaGluLmNvbYIKd3d3Lmdvby5nbIIIeW91dHUuYmWCC3lv\r\n" +
                               "dXR1YmUuY29tghR5b3V0dWJlZWR1Y2F0aW9uLmNvbYIFeXQuYmUwaAYIKwYBBQUH\r\n" +
                               "AQEEXDBaMCsGCCsGAQUFBzAChh9odHRwOi8vcGtpLmdvb2dsZS5jb20vR0lBRzIu\r\n" +
                               "Y3J0MCsGCCsGAQUFBzABhh9odHRwOi8vY2xpZW50czEuZ29vZ2xlLmNvbS9vY3Nw\r\n" +
                               "MB0GA1UdDgQWBBSggdBiyQZ4Kfb+O8mWQTGfnc4TYDAMBgNVHRMBAf8EAjAAMB8G\r\n" +
                               "A1UdIwQYMBaAFErdBhYbvPZotXb1gba7Yhq6WoEvMCEGA1UdIAQaMBgwDAYKKwYB\r\n" +
                               "BAHWeQIFATAIBgZngQwBAgIwMAYDVR0fBCkwJzAloCOgIYYfaHR0cDovL3BraS5n\r\n" +
                               "b29nbGUuY29tL0dJQUcyLmNybDANBgkqhkiG9w0BAQsFAAOCAQEASgUea/LYHy70\r\n" +
                               "7j15xmwsrCvJU2zIZehz4iy+bvPzKMtDY6461Y28+RsWy7nV3FFKHd6ShJAwySjd\r\n" +
                               "EsDELMkCJAGPMd/Nyy14U8aiBlO1sUtKo9dOfHt/b68kOPI2/9X1nhVqlBNtX/ll\r\n" +
                               "TW8rvqnlna8DZPA2UbCxXMF8uH8WcTRO3h8fMURTdt6WySk3VrCt+2miIFzpcZE+\r\n" +
                               "hPLP9r24n4f4sgR9u+eZs+BDEvRq1ClzNXw6WNYPz7dmnL63/GeU11s/h6AyQLVE\r\n" +
                               "ud8bZ/xAGHVFN9RGshIzQPf8rEUCWfjAxVyGXnBuJeORHWIeT9Epf51Ar9X8+Fr5\r\n" +
                               "tz9hKnhkAg==\r\n" +
                               "-----END CERTIFICATE-----\r\n";
    try
    {
      TEST_CERT2 = CertificateHelper.convertStringToCertficate (sValidCert2);
      assertNotNull (TEST_CERT2);
    }
    catch (final CertificateException ex)
    {
      throw new InitializationException (ex);
    }
  }

  @Test
  public void testGetter ()
  {
    final IParticipantIdentifier aPI = TCSettings.getIdentifierFactory ()
                                                 .createParticipantIdentifierWithDefaultScheme ("1234:test");
    final String sTransportProtocol = "AS4";
    final String sEndpointURL = "http://example.org/as4";
    final R2D2Endpoint aEP = new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT);
    assertEquals (aPI, aEP.getParticipantID ());
    assertEquals (sTransportProtocol, aEP.getTransportProtocol ());
    assertEquals (sEndpointURL, aEP.getEndpointURL ());
    assertEquals (TEST_CERT, aEP.getCertificate ());
  }

  @Test
  public void testEquals ()
  {
    final IParticipantIdentifier aPI = TCSettings.getIdentifierFactory ()
                                                 .createParticipantIdentifierWithDefaultScheme ("1234:test");
    final IParticipantIdentifier aPI2 = TCSettings.getIdentifierFactory ()
                                                  .createParticipantIdentifierWithDefaultScheme ("1234:test2");
    final String sTransportProtocol = "AS4";
    final String sTransportProtocol2 = "AS2";
    assertNotEquals (sTransportProtocol, sTransportProtocol2);

    final String sEndpointURL = "http://example.org/as4";
    final String sEndpointURL2 = "http://example.org/as2";
    assertNotEquals (sEndpointURL, sEndpointURL2);

    final R2D2Endpoint aEP = new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT);
    assertEquals (aEP, aEP);
    assertEquals (aEP, new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT));
    assertNotEquals (aEP, new R2D2Endpoint (aPI2, sTransportProtocol, sEndpointURL, TEST_CERT));
    assertNotEquals (aEP, new R2D2Endpoint (aPI, sTransportProtocol2, sEndpointURL, TEST_CERT));
    assertNotEquals (aEP, new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL2, TEST_CERT));
    assertNotEquals (aEP, new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT2));
    assertNotEquals (aEP, null);
    assertNotEquals (aEP, "bla");
    assertEquals (aEP.hashCode (), aEP.hashCode ());
    assertTrue (aEP.hashCode () != new R2D2Endpoint (aPI2, sTransportProtocol, sEndpointURL, TEST_CERT).hashCode ());
    assertNotNull (aEP.toString ());
  }
}
