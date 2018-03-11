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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;

import com.helger.commons.exception.InitializationException;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.security.certificate.CertificateHelper;

import eu.toop.connector.api.TCSettings;
import eu.toop.connector.r2d2client.R2D2Endpoint;

/**
 * Test class for class {@link R2D2Endpoint}.
 *
 * @author Philip Helger
 */
public final class R2D2EndpointTest {
  private static X509Certificate TEST_CERT;

  static {
    // The web page certificate from Google
    final String sValidCert = "MIIDVDCCAjygAwIBAgIDAjRWMA0GCSqGSIb3DQEBBQUAMEIxCzAJBgNVBAYTAlVT\r\n"
                              + "MRYwFAYDVQQKEw1HZW9UcnVzdCBJbmMuMRswGQYDVQQDExJHZW9UcnVzdCBHbG9i\r\n"
                              + "YWwgQ0EwHhcNMDIwNTIxMDQwMDAwWhcNMjIwNTIxMDQwMDAwWjBCMQswCQYDVQQG\r\n"
                              + "EwJVUzEWMBQGA1UEChMNR2VvVHJ1c3QgSW5jLjEbMBkGA1UEAxMSR2VvVHJ1c3Qg\r\n"
                              + "R2xvYmFsIENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2swYYzD9\r\n"
                              + "9BcjGlZ+W988bDjkcbd4kdS8odhM+KhDtgPpTSEHCIjaWC9mOSm9BXiLnTjoBbdq\r\n"
                              + "fnGk5sRgprDvgOSJKA+eJdbtg/OtppHHmMlCGDUUna2YRpIuT8rxh0PBFpVXLVDv\r\n"
                              + "iS2Aelet8u5fa9IAjbkU+BQVNdnARqN7csiRv8lVK83Qlz6cJmTM386DGXHKTubU\r\n"
                              + "1XupGc1V3sjs0l44U+VcT4wt/lAjNvxm5suOpDkZALeVAjmRCw7+OC7RHQWa9k0+\r\n"
                              + "bw8HHa8sHo9gOeL6NlMTOdReJivbPagUvTLrGAMoUgRx5aszPeE4uwc2hGKceeoW\r\n"
                              + "MPRfwCvocWvk+QIDAQABo1MwUTAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBTA\r\n"
                              + "ephojYn7qwVkDBF9qn1luMrMTjAfBgNVHSMEGDAWgBTAephojYn7qwVkDBF9qn1l\r\n"
                              + "uMrMTjANBgkqhkiG9w0BAQUFAAOCAQEANeMpauUvXVSOKVCUn5kaFOSPeCpilKIn\r\n"
                              + "Z57QzxpeR+nBsqTP3UEaBU6bS+5Kb1VSsyShNwrrZHYqLizz/Tt1kL/6cdjHPTfS\r\n"
                              + "tQWVYrmm3ok9Nns4d0iXrKYgjy6myQzCsplFAMfOEVEiIuCl6rYVSAlk6l5PdPcF\r\n"
                              + "PseKUgzbFbS9bZvlxrFUaKnjaZC2mqUPuLk/IH2uSrW4nOQdtqvmlKXBx4Ot2/Un\r\n"
                              + "hw4EbNX/3aBd7YdStysVAq45pmp06drE57xNNB6pXE0zX5IJL4hmXXeXxx12E6nV\r\n"
                              + "5fEWCRE11azbJHFwLJhWC9kXtNHjUStedejV0NxPNO3CBWaAocvmMw==";
    try {
      TEST_CERT = CertificateHelper.convertStringToCertficate (sValidCert);
      assertNotNull (TEST_CERT);
    } catch (final CertificateException ex) {
      throw new InitializationException (ex);
    }
  }

  @Test
  public void testGetter () {
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
  public void testEquals () {
    final IParticipantIdentifier aPI = TCSettings.getIdentifierFactory ()
                                                   .createParticipantIdentifierWithDefaultScheme ("1234:test");
    final IParticipantIdentifier aPI2 = TCSettings.getIdentifierFactory ()
                                                    .createParticipantIdentifierWithDefaultScheme ("1234:test2");
    final String sTransportProtocol = "AS4";
    final String sTransportProtocol2 = "AS2";
    final String sEndpointURL = "http://example.org/as4";
    final String sEndpointURL2 = "http://example.org/as2";
    assertEquals (new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT),
                  new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT));
    assertNotEquals (new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT),
                     new R2D2Endpoint (aPI2, sTransportProtocol, sEndpointURL, TEST_CERT));
    assertNotEquals (new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT),
                     new R2D2Endpoint (aPI, sTransportProtocol2, sEndpointURL, TEST_CERT));
    assertNotEquals (new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL, TEST_CERT),
                     new R2D2Endpoint (aPI, sTransportProtocol, sEndpointURL2, TEST_CERT));
  }
}
