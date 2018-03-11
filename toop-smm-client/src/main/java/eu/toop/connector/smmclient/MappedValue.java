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
package eu.toop.connector.smmclient;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.ReturnsImmutableObject;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

import eu.toop.commons.concept.ConceptValue;

/**
 * This class contains a source schemed value and destination mapped value.
 *
 * @author Philip Helger
 *
 */
@Immutable
@MustImplementEqualsAndHashcode
public final class MappedValue implements Serializable {
  private final ConceptValue m_aSource;
  private final ConceptValue m_aDest;

  public MappedValue (@Nonnull final ConceptValue aSource, @Nonnull final ConceptValue aDest) {
    ValueEnforcer.notNull (aSource, "Source");
    ValueEnforcer.notNull (aDest, "Destination");
    m_aSource = aSource;
    m_aDest = aDest;
  }

  @Nonnull
  public ConceptValue getSource () {
    return m_aSource;
  }

  @Nonnull
  public ConceptValue getDestination () {
    return m_aDest;
  }

  @Nonnull
  @ReturnsImmutableObject
  public MappedValue getSwappedSourceAndDest () {
    // Swap source and dest
    return new MappedValue (m_aDest, m_aSource);
  }

  @Override
  public boolean equals (final Object o) {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final MappedValue rhs = (MappedValue) o;
    return m_aSource.equals (rhs.m_aSource) && m_aDest.equals (rhs.m_aDest);
  }

  @Override
  public int hashCode () {
    return new HashCodeGenerator (this).append (m_aSource).append (m_aDest).getHashCode ();
  }

  @Override
  public String toString () {
    return new ToStringGenerator (null).append ("Source", m_aSource).append ("Destination", m_aDest).getToString ();
  }
}