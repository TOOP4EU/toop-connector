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
package eu.toop.connector.app.r2d2;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.string.StringHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;

import eu.toop.commons.error.EToopErrorCode;
import eu.toop.connector.api.r2d2.IR2D2Endpoint;
import eu.toop.connector.api.r2d2.IR2D2EndpointProvider;
import eu.toop.connector.api.r2d2.IR2D2ErrorHandler;
import eu.toop.connector.api.r2d2.IR2D2ParticipantIDProvider;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * Helper class to easily perform the multi participant lookup of the Resilient
 * Registry-based Dynamic Discovery
 *
 * @author Philip Helger, BRZ, AT
 */
@Immutable
public class R2D2Client
{
  private R2D2Client ()
  {}

  /**
   * Get a list of all endpoints that match the specified requirements. This is
   * the API that is to be invoked in the case, where ServiceGroup IDs of the
   * receiver are unknown and an additional PEPPOL Directory query needs to be
   * performed.<br>
   * Internally country code and document type are queried against the correct
   * TOOP Directory instance (depending on the production or test flag). The
   * SMPs of the resulting service group IDs are than queried in a loop for all
   * matching endpoints (of participant ID and document type ID) which are
   * parsed and converted to simpler R2D2Endpoint instances.
   *
   * @param sLogPrefix
   *        Log prefix to use. May not be <code>null</code> but maybe empty.
   * @param sCountryCode
   *        The country code to be queried. Must be a 2-char string. May not be
   *        <code>null</code>.
   * @param aDocumentTypeID
   *        The document type ID to be queried. May not be <code>null</code>.
   * @param aParticipantIDProvider
   *        The participant ID provider that uses country code and document type
   *        ID to determine the set of matching participant identifiers. May not
   *        be <code>null</code>.
   * @param aProcessID
   *        The process ID to be queried. May not be <code>null</code>.
   * @param sTransportProfileID
   *        The transport profile ID to be used. May neither be
   *        <code>null</code> nor empty.
   * @param aEndpointProvider
   *        The R2D2 endpoint ID provider to be used. May not be
   *        <code>null</code>.
   * @param aErrorHandler
   *        The error handler to be used. May not be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of all matching
   *         endpoints.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <IR2D2Endpoint> getParticipantIDsAndEndpoints (@Nonnull final String sLogPrefix,
                                                                            @Nonnull @Nonempty final String sCountryCode,
                                                                            @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                                                            @Nonnull final IR2D2ParticipantIDProvider aParticipantIDProvider,
                                                                            @Nonnull final IProcessIdentifier aProcessID,
                                                                            @Nonnull @Nonempty final String sTransportProfileID,
                                                                            @Nonnull final IR2D2EndpointProvider aEndpointProvider,
                                                                            @Nonnull final IR2D2ErrorHandler aErrorHandler)
  {
    ValueEnforcer.notEmpty (sCountryCode, "CountryCode");
    ValueEnforcer.isTrue (sCountryCode.length () == 2, "CountryCode must have length 2");
    ValueEnforcer.notNull (aDocumentTypeID, "DocumentTypeID");
    ValueEnforcer.notNull (aParticipantIDProvider, "ParticipantIDProvider");
    ValueEnforcer.notNull (aProcessID, "ProcessID");
    ValueEnforcer.notEmpty (sTransportProfileID, "TransportProfileID");
    ValueEnforcer.notNull (aEndpointProvider, "EndpointProvider");
    ValueEnforcer.notNull (aErrorHandler, "ErrorHandler");

    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> sLogPrefix +
                                "Participant ID lookup (" +
                                sCountryCode +
                                ", " +
                                aDocumentTypeID.getURIEncoded () +
                                ", " +
                                aProcessID.getURIEncoded () +
                                ", " +
                                sTransportProfileID +
                                ") using participant ID provider " +
                                aParticipantIDProvider +
                                " and endpoint ID provider " +
                                aEndpointProvider);

    final ICommonsList <IR2D2Endpoint> ret = new CommonsArrayList <> ();

    // Query TOOP Directory
    final ICommonsSet <IParticipantIdentifier> aPIs = aParticipantIDProvider.getAllParticipantIDs (sLogPrefix,
                                                                                                   sCountryCode,
                                                                                                   aDocumentTypeID,
                                                                                                   aErrorHandler);

    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> sLogPrefix +
                                "Participant ID lookup result[" +
                                aPIs.size () +
                                "]: " +
                                StringHelper.getImplodedMapped (", ", aPIs, IParticipantIdentifier::getURIEncoded));

    if (aPIs.isEmpty ())
      aErrorHandler.onError ("Participant ID lookup returned no matches", EToopErrorCode.DD_004);
    else
    {
      // For all matching IDs (if any)
      for (final IParticipantIdentifier aPI : aPIs)
      {
        // Single SMP query
        final ICommonsList <IR2D2Endpoint> aLocal = aEndpointProvider.getEndpoints (sLogPrefix,
                                                                                    aPI,
                                                                                    aDocumentTypeID,
                                                                                    aProcessID,
                                                                                    sTransportProfileID,
                                                                                    aErrorHandler);
        ret.addAll (aLocal);

        if (aLocal.isEmpty ())
        {
          // emit warning DD_005
          aErrorHandler.onWarning ("Endpoint lookup for '" +
                                   aPI.getURIEncoded () +
                                   "' and document type ID '" +
                                   aDocumentTypeID.getURIEncoded () +
                                   "' and process ID '" +
                                   aProcessID.getURIEncoded () +
                                   "' and transport profile '" +
                                   sTransportProfileID +
                                   "' returned in no endpoints",
                                   EToopErrorCode.DD_005);
        }
      }
    }
    return ret;
  }
}
