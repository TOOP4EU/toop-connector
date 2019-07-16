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
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

import eu.toop.commons.error.ToopErrorException;

/**
 * Interface to resolve Country Code and Document Type ID to a set of matching
 * participant identifiers.<br>
 * Am implementation of this class e.g. queries the TOOP Directory for the
 * respective participants.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
@FunctionalInterface
public interface IR2D2ParticipantIDProvider extends Serializable
{
  /**
   * Get all participant IDs that match the provided country code and document
   * type ID.
   *
   * @param sLogPrefix
   *        The logging prefix to be used. May not be <code>null</code>.
   * @param sCountryCode
   *        Country code to use. Must be a 2-digit string. May not be
   *        <code>null</code>.
   * @param aDocumentTypeID
   *        Document type ID to query. May not be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty set of Participant IDs.
   * @throws ToopErrorException
   *         On query exception
   */
  @Nonnull
  ICommonsSet <IParticipantIdentifier> getAllParticipantIDs (@Nonnull String sLogPrefix,
                                                             @Nonnull @Nonempty String sCountryCode,
                                                             @Nonnull IDocumentTypeIdentifier aDocumentTypeID) throws ToopErrorException;
}
