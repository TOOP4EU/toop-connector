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

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;

import eu.toop.connector.api.r2d2.IR2D2Endpoint;
import eu.toop.connector.api.r2d2.IR2D2EndpointProvider;
import eu.toop.connector.api.r2d2.IR2D2ErrorHandler;

/**
 * This class implements the {@link IR2D2EndpointProvider} interface using a
 * constant set of endpoints. This implementation is meant for testing purposes
 * only. Don't use in production.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public class R2D2EndpointProviderConstant implements IR2D2EndpointProvider
{
  private final ICommonsOrderedSet <IR2D2Endpoint> m_aSet = new CommonsLinkedHashSet <> ();

  /**
   * Constructor to return an empty endpoint set.
   */
  public R2D2EndpointProviderConstant ()
  {}

  /**
   * Constructor with a single endpoint.
   *
   * @param aEndpoint
   *        The participant ID to return. May not be <code>null</code>.
   */
  public R2D2EndpointProviderConstant (@Nonnull final IR2D2Endpoint aEndpoint)
  {
    ValueEnforcer.notNull (aEndpoint, "Endpoint");
    m_aSet.add (aEndpoint);
  }

  /**
   * Constructor with a collection of endpoints
   *
   * @param aEndpoints
   *        The endpoints to be returned. May not be <code>null</code> and may
   *        not contain <code>null</code> values.
   */
  public R2D2EndpointProviderConstant (@Nonnull final Iterable <? extends IR2D2Endpoint> aEndpoints)
  {
    ValueEnforcer.notNullNoNullValue (aEndpoints, "Endpoints");
    m_aSet.addAll (aEndpoints);
  }

  /**
   * Constructor with an array of endpoints
   *
   * @param aEndpoints
   *        The endpoints to be returned. May not be <code>null</code> and may
   *        not contain <code>null</code> values.
   */
  public R2D2EndpointProviderConstant (@Nonnull final IR2D2Endpoint... aEndpoints)
  {
    ValueEnforcer.notNullNoNullValue (aEndpoints, "Endpoints");
    m_aSet.addAll (aEndpoints);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IR2D2Endpoint> getEndpoints (@Nonnull final String sLogPrefix,
                                                    @Nonnull final IParticipantIdentifier aRecipientID,
                                                    @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                                    @Nonnull final IProcessIdentifier aProcessID,
                                                    @Nonnull @Nonempty final String sTransportProfileID,
                                                    @Nonnull final IR2D2ErrorHandler aErrorHandler)
  {
    return m_aSet.getCopyAsList ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Endpoints", m_aSet).getToString ();
  }
}
