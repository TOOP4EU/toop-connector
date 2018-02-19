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

import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;

/**
 * Resilient Registry-based Dynamic Discovery (R2D2) client interface for
 * Message Processor
 *
 * @author Philip Helger, BRZ, AT
 */
public interface IR2D2Client {
  /**
   * Get a list of all endpoints that match the specified requirements. This is
   * the API that is to be invoked in the case, where ServiceGroup IDs of the
   * receiver are unknown and an additional PEPPOL Directory query needs to be
   * performed.<br>
   * Internally country code and document type are queried against the correct
   * PEPPOL Directory instance (depending on the production or test flag). The
   * SMPs of the resulting service group IDs are than queried in a loop for all
   * matching endpoints (of participant ID and document type ID) which are parsed
   * and converted to simpler R2D2Endpoint instances.<br>
   * Note: this method returns endpoints for all found transport protocols, so
   * this must be filtered externally.
   *
   * @param sCountryCode
   *          The country code to be queried. Must be a 2-char string. May not be
   *          <code>null</code>.
   * @param aDocumentTypeID
   *          The document type ID to be queried. May not be <code>null</code>.
   * @param aProcessID
   *          The process ID to be queried. May not be <code>null</code>.
   * @param bProductionSystem
   *          <code>true</code> to query the production system (using production
   *          PEPPOL Directory and SML) or <code>false</code> to query the test
   *          system (using test PEPPOL Directory and SMK).
   * @return A non-<code>null</code> but maybe empty list of all matching
   *         endpoints.
   */
  @Nonnull
  List<IR2D2Endpoint> getEndpoints(@Nonnull @Nonempty String sCountryCode,
      @Nonnull IDocumentTypeIdentifier aDocumentTypeID, @Nonnull IProcessIdentifier aProcessID,
      boolean bProductionSystem);

  /**
   * Get a list of all endpoints that match the specified requirements. This is
   * the API that is to be invoked in the case, where the ServiceGroup IDs of the
   * receiver is known and NO PEPPOL Directory invocation is needed.<br>
   * Internally the SMP of the service group ID is queried and all matching
   * endpoints are parsed and converted to simpler R2D2Endpoint instances.<br>
   * Note: this method returns endpoints for all found transport protocols, so
   * this must be filtered externally.
   *
   * @param aRecipientID
   *          The country code to be queried. Must be a 2-char string. May not be
   *          <code>null</code>.
   * @param aDocumentTypeID
   *          The document type ID to be queried. May not be <code>null</code>.
   * @param aProcessID
   *          The process ID to be queried. May not be <code>null</code>.
   * @param bProductionSystem
   *          <code>true</code> to query the production system (using SML) or
   *          <code>false</code> to query the test system (using SMK).
   * @return A non-<code>null</code> but maybe empty list of all matching
   *         endpoints.
   */
  @Nonnull
  List<IR2D2Endpoint> getEndpoints(@Nonnull IParticipantIdentifier aRecipientID,
      @Nonnull IDocumentTypeIdentifier aDocumentTypeID, @Nonnull IProcessIdentifier aProcessID,
      boolean bProductionSystem);
}
