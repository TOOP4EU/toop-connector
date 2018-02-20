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
package eu.toop.mp.r2d2client;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerJson;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.peppol.bdxr.EndpointType;
import com.helger.peppol.bdxr.ProcessType;
import com.helger.peppol.bdxr.ServiceInformationType;
import com.helger.peppol.bdxr.SignedServiceMetadataType;
import com.helger.peppol.bdxrclient.BDXRClient;
import com.helger.peppol.bdxrclient.BDXRClientReadOnly;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.smpclient.exception.SMPClientException;

import eu.toop.mp.api.MPConfig;
import eu.toop.mp.api.MPSettings;

/**
 * The default implementation of {@link IR2D2Client}. It performs the query
 * every time and does not cache results!
 *
 * @author Philip Helger, BRZ, AT
 */
@Immutable
public class R2D2Client implements IR2D2Client {
  private static final Logger s_aLogger = LoggerFactory.getLogger (R2D2Client.class);

  @Nullable
  private static IJsonObject _fetchJsonObject (@Nonnull final HttpClientManager aMgr,
                                               @Nonnull final ISimpleURL aURL) throws IOException {
    final HttpPost aPost = new HttpPost (aURL.getAsURI ());
    final ResponseHandlerJson aRH = new ResponseHandlerJson ();
    final IJson aJson = aMgr.execute (aPost, aRH);
    if (aJson != null && aJson.isObject ())
      return aJson.getAsObject ();

    s_aLogger.error ("Failed to fetch " + aURL.getAsStringWithEncodedParameters () + " - stopping");
    return null;
  }

  /**
   * Query PEPPPOL Directory for all matching recipient IDs.
   *
   * @param sCountryCode
   *          Country code to use. Must be a 2-digit string. May not be
   *          <code>null</code>.
   * @param aDocumentTypeID
   *          Document type ID to query. May not be <code>null</code>.
   * @param bProductionSystem
   *          <code>true</code> to query production PEPPOL Directory or
   *          <code>false</code> to query test PEPPOL Directory
   * @return A non-<code>null</code> but maybe empty set of Participant IDs.
   */
  @Nonnull
  private static ICommonsSet<IParticipantIdentifier> _getAllRecipientIDsFromDirectory (@Nonnull @Nonempty final String sCountryCode,
                                                                                       @Nonnull final IDocumentTypeIdentifier aDocumentTypeID) {
    final ICommonsSet<IParticipantIdentifier> ret = new CommonsHashSet<> ();

    final HttpClientFactory aHCFactory = new HttpClientFactory ();
    // For proxy etc
    aHCFactory.setUseSystemProperties (true);

    try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory)) {
      // Build base URL and fetch x records per HTTP request
      final int nMaxResultsPerPage = 100;
      final SimpleURL aBaseURL = new SimpleURL (MPConfig.getR2D2DirectoryBaseUrl ()
                                                + "/search/1.0/json").add ("doctype", aDocumentTypeID.getURIEncoded ())
                                                                     .add ("country", sCountryCode)
                                                                     .add ("rpc", nMaxResultsPerPage);

      // Fetch first object
      IJsonObject aResult = _fetchJsonObject (aMgr, aBaseURL);
      if (aResult != null) {
        // Start querying results
        int nResultPageIndex = 0;
        int nLoops = 0;
        while (true) {
          int nMatchCount = 0;
          final IJsonArray aMatches = aResult.getAsArray ("matches");
          if (aMatches != null) {
            for (final IJson aMatch : aMatches) {
              ++nMatchCount;
              final IJsonObject aID = aMatch.getAsObject ().getAsObject ("participantID");
              if (aID != null) {
                final String sScheme = aID.getAsString ("scheme");
                final String sValue = aID.getAsString ("value");
                final IParticipantIdentifier aPI = MPSettings.getIdentifierFactory ()
                                                             .createParticipantIdentifier (sScheme, sValue);
                if (aPI != null)
                  ret.add (aPI);
                else
                  s_aLogger.warn ("Failed to create participant identifier from '" + sScheme + "' and '" + sValue
                                  + "'");
              } else
                s_aLogger.warn ("Match does not contain participant ID");
            }
          } else
            s_aLogger.warn ("JSON response contains no 'matches'");

          if (nMatchCount < nMaxResultsPerPage) {
            // Got less results than expected - end of list
            break;
          }

          if (++nLoops > 100) {
            // Avoid endless loop
            s_aLogger.error ("Endless loop in PD fetching?");
            break;
          }

          // Query next page
          nResultPageIndex++;
          aResult = _fetchJsonObject (aMgr, aBaseURL.getClone ().add ("rpi", nResultPageIndex));
          if (aResult == null) {
            // Unexpected error - stop querying
            // Error was already logged
            break;
          }
        }
      }
    } catch (final IOException ex) {
      s_aLogger.warn ("Error querying PEPPOL Directory for matches (" + sCountryCode + ", "
                      + aDocumentTypeID.getURIEncoded () + ")", ex);
    }

    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList<IR2D2Endpoint> getEndpoints (@Nonnull @Nonempty final String sCountryCode,
                                                   @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                                   @Nonnull final IProcessIdentifier aProcessID,
                                                   final boolean bProductionSystem) {
    ValueEnforcer.notEmpty (sCountryCode, "CountryCode");
    ValueEnforcer.isTrue (sCountryCode.length () == 2, "CountryCode must have length 2");
    ValueEnforcer.notNull (aDocumentTypeID, "DocumentTypeID");
    ValueEnforcer.notNull (aProcessID, "ProcessID");

    final ICommonsList<IR2D2Endpoint> ret = new CommonsArrayList<> ();

    // Query PEPPOL Directory
    final ICommonsSet<IParticipantIdentifier> aPIs = _getAllRecipientIDsFromDirectory (sCountryCode, aDocumentTypeID);

    // For all matching IDs (if any)
    for (final IParticipantIdentifier aPI : aPIs) {
      // Single SMP query
      final ICommonsList<IR2D2Endpoint> aLocal = getEndpoints (aPI, aDocumentTypeID, aProcessID);
      ret.addAll (aLocal);
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList<IR2D2Endpoint> getEndpoints (@Nonnull final IParticipantIdentifier aRecipientID,
                                                   @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                                   @Nonnull final IProcessIdentifier aProcessID) {
    ValueEnforcer.notNull (aRecipientID, "Recipient");
    ValueEnforcer.notNull (aDocumentTypeID, "DocumentTypeID");
    ValueEnforcer.notNull (aProcessID, "ProcessID");

    final ICommonsList<IR2D2Endpoint> ret = new CommonsArrayList<> ();
    BDXRClient aSMPClient;
    if (MPConfig.isR2D2UseDNS ()) {
      // Use dynamic lookup via DNS
      aSMPClient = new BDXRClient (MPSettings.getSMPUrlProvider (), aRecipientID, MPConfig.getR2D2SML ());
    } else {
      // Use a constant SMP URL
      aSMPClient = new BDXRClient (MPConfig.getR2D2SMPUrl ());
    }
    try {
      // Query SMP
      final SignedServiceMetadataType aSG = aSMPClient.getServiceRegistration (aRecipientID, aDocumentTypeID);
      final ServiceInformationType aSI = aSG.getServiceMetadata ().getServiceInformation ();
      if (aSI != null) {
        // Find the first process that matches (should be only one!)
        final ProcessType aProcess = CollectionHelper.findFirst (aSI.getProcessList ().getProcess (),
                                                                 x -> x.getProcessIdentifier ()
                                                                       .hasSameContent (aProcessID));
        if (aProcess != null) {
          // Add all endpoints to the result list
          for (final EndpointType aEP : aProcess.getServiceEndpointList ().getEndpoint ()) {
            // Convert String to X509Certificate
            final X509Certificate aCert = BDXRClientReadOnly.getEndpointCertificate (aEP);

            // Convert to our data structure
            final R2D2Endpoint aDestEP = new R2D2Endpoint (aRecipientID, aEP.getTransportProfile (),
                                                           aEP.getEndpointURI (), aCert);
            ret.add (aDestEP);
          }
        }
      }
      // else redirect
    } catch (final CertificateException | SMPClientException ex) {
      s_aLogger.error ("Error fetching SMP endpoint " + aRecipientID.getURIEncoded () + "/"
                       + aDocumentTypeID.getURIEncoded () + "/" + aProcessID.getURIEncoded (), ex);
    }
    return ret;
  }
}
