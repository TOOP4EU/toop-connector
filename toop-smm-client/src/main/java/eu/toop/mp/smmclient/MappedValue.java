package eu.toop.mp.smmclient;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

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