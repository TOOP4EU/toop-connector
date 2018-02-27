package eu.toop.mp.smmclient;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * This is a single value that consist of a namespace and a concept name.
 *
 * @author Philip Helger
 */
@Immutable
@MustImplementEqualsAndHashcode
public final class ConceptValue implements Serializable {
  private final String m_sNamespace;
  private final String m_sValue;

  public ConceptValue (@Nonnull @Nonempty final String sNamespace, @Nonnull @Nonempty final String sValue) {
    ValueEnforcer.notEmpty (sNamespace, "Namespace");
    ValueEnforcer.notEmpty (sValue, "Value");
    m_sNamespace = sNamespace;
    m_sValue = sValue;
  }

  @Nonnull
  @Nonempty
  public String getNamespace () {
    return m_sNamespace;
  }

  public boolean hasNamespace (@Nullable final String sNamespace) {
    return m_sNamespace.equals (sNamespace);
  }

  @Nonnull
  @Nonempty
  public String getValue () {
    return m_sValue;
  }

  public boolean hasValue (@Nullable final String sValue) {
    return m_sValue.equals (sValue);
  }

  /**
   * @return <code>scheme</code>#<code>value</code>. Neither <code>null</code> nor
   *         empty.
   */
  @Nonnull
  @Nonempty
  public String getConcatenatedValue () {
    return m_sNamespace + '#' + m_sValue;
  }

  @Override
  public boolean equals (final Object o) {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final ConceptValue rhs = (ConceptValue) o;
    return m_sNamespace.equals (rhs.m_sNamespace) && m_sValue.equals (rhs.m_sValue);
  }

  @Override
  public int hashCode () {
    return new HashCodeGenerator (this).append (m_sNamespace).append (m_sValue).getHashCode ();
  }

  @Override
  public String toString () {
    return new ToStringGenerator (null).append ("Namespace", m_sNamespace).append ("Value", m_sValue).getToString ();
  }
}