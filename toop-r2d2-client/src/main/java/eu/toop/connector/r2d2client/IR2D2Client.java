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
package eu.toop.connector.r2d2client;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;

import eu.toop.commons.error.ToopErrorException;

/**
 * Resilient Registry-based Dynamic Discovery (R2D2) client interface for
 * Message Processor
 *
 * @author Philip Helger, BRZ, AT
 */
public interface IR2D2Client extends Serializable
{
  /**
   * Get a list of all endpoints that match the specified requirements. This is
   * the API that is to be invoked in the case, where ServiceGroup IDs of the
   * receiver are unknown and an additional PEPPOL Directory query needs to be
   * performed.<br>
   * Internally country code and document type are queried against the correct
   * TOOP Directory instance (depending on the production or test flag). The
   * SMPs of the resulting service group IDs are than queried in a loop for all
   * matching endpoints (of participant ID and document type ID) which are
   * parsed and converted to simpler R2D2Endpoint instances.<br>
   * Note: this method returns endpoints for all found transport protocols, so
   * this must be filtered externally.
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
   * @return A non-<code>null</code> but maybe empty list of all matching
   *         endpoints.
   * @throws ToopErrorException
   *         On error including the respective error code
   */
  @Nonnull
  ICommonsList <IR2D2Endpoint> getEndpoints (@Nonnull String sLogPrefix,
                                             @Nonnull @Nonempty String sCountryCode,
                                             @Nonnull IDocumentTypeIdentifier aDocumentTypeID,
                                             @Nonnull final IR2D2ParticipantIDProvider aParticipantIDProvider,
                                             @Nonnull IProcessIdentifier aProcessID,
                                             @Nonnull @Nonempty String sTransportProfileID) throws ToopErrorException;

  /**
   * Get a list of all endpoints that match the specified requirements. This is
   * the API that is to be invoked in the case, where the ServiceGroup IDs of
   * the receiver is known and NO TOOP Directory invocation is needed.<br>
   * Internally the SMP of the service group ID is queried and all matching
   * endpoints are parsed and converted to simpler R2D2Endpoint instances.<br>
   * Note: this method returns endpoints for all found transport protocols, so
   * this must be filtered externally.
   *
   * @param sLogPrefix
   *        Log prefix. May not be <code>null</code> but maybe empty.
   * @param aRecipientID
   *        The country code to be queried. Must be a 2-char string. May not be
   *        <code>null</code>.
   * @param aDocumentTypeID
   *        The document type ID to be queried. May not be <code>null</code>.
   * @param aProcessID
   *        The process ID to be queried. May not be <code>null</code>.
   * @param sTransportProfileID
   *        The transport profile ID to be used. May neither be
   *        <code>null</code> nor empty.
   * @return A non-<code>null</code> but maybe empty list of all matching
   *         endpoints.
   * @throws ToopErrorException
   *         On error including the respective error code
   */
  @Nonnull
  ICommonsList <IR2D2Endpoint> getEndpoints (@Nonnull String sLogPrefix,
                                             @Nonnull IParticipantIdentifier aRecipientID,
                                             @Nonnull IDocumentTypeIdentifier aDocumentTypeID,
                                             @Nonnull IProcessIdentifier aProcessID,
                                             @Nonnull @Nonempty String sTransportProfileID) throws ToopErrorException;
}
