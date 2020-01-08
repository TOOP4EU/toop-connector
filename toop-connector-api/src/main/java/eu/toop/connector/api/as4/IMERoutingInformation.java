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
package eu.toop.connector.api.as4;

import java.io.Serializable;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;

/**
 * Message Exchange Routing Information.
 *
 * @author Philip Helger
 */
public interface IMERoutingInformation extends Serializable
{
  /**
   * @return Sender participant ID. Never <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getSenderID ();

  /**
   * @return Receiver participant ID. Never <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getReceiverID ();

  /**
   * @return Document type ID. Never <code>null</code>.
   */
  @Nonnull
  IDocumentTypeIdentifier getDocumentTypeID ();

  /**
   * @return Process ID. Never <code>null</code>.
   */
  @Nonnull
  IProcessIdentifier getProcessID ();

  /**
   * @return The transport profile ID from the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  String getTransportProtocol ();

  /**
   * @return The endpoint URL from the SMP lookup. Neither <code>null</code> nor
   *         empty.
   */
  @Nonnull
  @Nonempty
  String getEndpointURL ();

  /**
   * @return The encoded certificate from the SMP look up. May not be
   *         <code>null</code>.
   */
  @Nonnull
  X509Certificate getCertificate ();
}
