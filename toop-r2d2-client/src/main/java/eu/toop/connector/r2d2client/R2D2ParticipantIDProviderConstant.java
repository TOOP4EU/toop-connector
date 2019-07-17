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
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

import eu.toop.connector.api.r2d2.IR2D2ErrorHandler;
import eu.toop.connector.api.r2d2.IR2D2ParticipantIDProvider;

/**
 * This class implements the {@link IR2D2ParticipantIDProvider} interface using
 * a constant set of participant identifiers. This implementation is meant for
 * testing purposes only. Don't use in production.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public class R2D2ParticipantIDProviderConstant implements IR2D2ParticipantIDProvider
{
  private final ICommonsOrderedSet <IParticipantIdentifier> m_aSet = new CommonsLinkedHashSet <> ();

  /**
   * Constructor to return an empty participant identifier set.
   */
  public R2D2ParticipantIDProviderConstant ()
  {}

  /**
   * Constructor with a single participant ID.
   *
   * @param aPI
   *        The participant ID to return. May not be <code>null</code>.
   */
  public R2D2ParticipantIDProviderConstant (@Nonnull final IParticipantIdentifier aPI)
  {
    ValueEnforcer.notNull (aPI, "ParticipantID");
    m_aSet.add (aPI);
  }

  /**
   * Constructor with a collection of participant IDs
   *
   * @param aPIs
   *        The participant IDs to be returned. May not be <code>null</code> and
   *        may not contain <code>null</code> values.
   */
  public R2D2ParticipantIDProviderConstant (@Nonnull final Iterable <? extends IParticipantIdentifier> aPIs)
  {
    ValueEnforcer.notNullNoNullValue (aPIs, "ParticipantIDs");
    m_aSet.addAll (aPIs);
  }

  /**
   * Constructor with an array of participant IDs
   *
   * @param aPIs
   *        The participant IDs to be returned. May not be <code>null</code> and
   *        may not contain <code>null</code> values.
   */
  public R2D2ParticipantIDProviderConstant (@Nonnull final IParticipantIdentifier... aPIs)
  {
    ValueEnforcer.notNullNoNullValue (aPIs, "ParticipantIDs");
    m_aSet.addAll (aPIs);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSet <IParticipantIdentifier> getAllParticipantIDs (@Nonnull final String sLogPrefix,
                                                                    @Nonnull @Nonempty final String sCountryCode,
                                                                    @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                                                    @Nonnull final IR2D2ErrorHandler aErrorHandler)
  {
    return m_aSet.getClone ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ParticipantIDs", m_aSet).getToString ();
  }
}
