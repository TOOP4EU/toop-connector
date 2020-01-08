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
package eu.toop.connector.api.r2d2;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;

/**
 * The interface to retrieve the technical endpoints of a participant. Usually
 * this is done by an SMP client lookup.
 *
 * @author Philip Helger
 */
public interface IR2D2EndpointProvider extends Serializable
{
  /**
   * Get a list of all endpoints that match the specified requirements. This is
   * the API that is to be invoked in the case, where the ServiceGroup IDs of
   * the receiver is known and NO TOOP Directory invocation is needed.<br>
   * Internally the SMP of the service group ID is queried and all matching
   * endpoints are parsed and converted to simpler R2D2Endpoint instances.
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
   * @param aErrorHandler
   *        The error handler to be used. May not be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of all matching
   *         endpoints.
   */
  @Nonnull
  ICommonsList <IR2D2Endpoint> getEndpoints (@Nonnull String sLogPrefix,
                                             @Nonnull IParticipantIdentifier aRecipientID,
                                             @Nonnull IDocumentTypeIdentifier aDocumentTypeID,
                                             @Nonnull IProcessIdentifier aProcessID,
                                             @Nonnull @Nonempty String sTransportProfileID,
                                             @Nonnull IR2D2ErrorHandler aErrorHandler);
}
