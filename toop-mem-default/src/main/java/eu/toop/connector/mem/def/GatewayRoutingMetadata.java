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
package eu.toop.connector.mem.def;

import java.io.Serializable;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

/**
 * @author myildiz at 15.02.2018.
 */
@Immutable
public class GatewayRoutingMetadata implements Serializable {

  /**
   * C1 participant ID
   */
  private final String senderParticipantId;

  /**
   * document type ID
   */
  private final String documentTypeId;

  /**
   * Process ID
   */
  private final String processId;

  /**
   * The target endpoint
   */
  private final String endpointUrl;

  private final X509Certificate cert;

  private final EActingSide side;

  public GatewayRoutingMetadata(@Nonnull @Nonempty final String sSenderParticipantId,
      @Nonnull @Nonempty final String sDocumentTypeId,
      @Nonnull @Nonempty final String sProcessId, 
      @Nonnull @Nonempty final String sEndpointUrl, 
      @Nonnull final X509Certificate aCert,
      @Nonnull final EActingSide eSide) {
    ValueEnforcer.notEmpty(sSenderParticipantId, "SenderParticipantID");
    ValueEnforcer.notEmpty(sDocumentTypeId, "DocumentTypeID");
    ValueEnforcer.notEmpty(sProcessId, "ProcessID");
    ValueEnforcer.notEmpty(sEndpointUrl, "EndpointURL");
    ValueEnforcer.notNull(aCert, "Cert");
    ValueEnforcer.notNull(eSide, "Side");
    senderParticipantId = sSenderParticipantId;
    documentTypeId = sDocumentTypeId;
    processId = sProcessId;
    endpointUrl = sEndpointUrl;
    cert = aCert;
    side = eSide;
  }

  @Nonnull
  @Nonempty
  public String getSenderParticipantId() {
    return senderParticipantId;
  }

  @Nonnull
  @Nonempty
  public String getDocumentTypeId() {
    return documentTypeId;
  }

  @Nonnull
  @Nonempty
  public String getProcessId() {
    return processId;
  }

  @Nonnull
  @Nonempty
  public String getEndpointUrl() {
    return endpointUrl;
  }

  @Nonnull
  public X509Certificate getCertificate() {
    return cert;
  }

  @Nonnull
  public EActingSide getSide() {
    return side;
  }
}
