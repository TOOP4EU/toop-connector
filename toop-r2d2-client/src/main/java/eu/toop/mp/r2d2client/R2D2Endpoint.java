package eu.toop.mp.r2d2client;

import java.io.Serializable;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * This class contains a single result endpoint for an R2D2 query.
 *
 * @author Philip Helger, BRZ, AT
 */
@Immutable
@MustImplementEqualsAndHashcode
public class R2D2Endpoint implements Serializable
{
  private final IParticipantIdentifier m_aParticipantID;
  private final String m_sTransportProtocol;
  private final String m_sEndpointURL;
  private final X509Certificate m_aCert;

  /**
   * Constructor
   *
   * @param aParticipantID
   *        The participant ID to which this endpoint belongs.
   * @param sTransportProtocol
   *        The transport protocol from the SMP endpoint. May neither be
   *        <code>null</code> nor empty.
   * @param sEndpointURL
   *        The endpoint URL from the SMP endpoint. May neither be
   *        <code>null</code> nor empty.
   * @param aCert
   *        The encoded certificate from the SMP endpoint. May not be
   *        <code>null</code>.
   */
  public R2D2Endpoint (@Nonnull final IParticipantIdentifier aParticipantID,
                       @Nonnull @Nonempty final String sTransportProtocol,
                       @Nonnull @Nonempty final String sEndpointURL,
                       @Nonnull final X509Certificate aCert)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notEmpty (sTransportProtocol, "TransportProtocol");
    ValueEnforcer.notEmpty (sEndpointURL, "EndpointURL");
    ValueEnforcer.notNull (aCert, "Cert");
    m_aParticipantID = aParticipantID;
    m_sTransportProtocol = sTransportProtocol;
    m_sEndpointURL = sEndpointURL;
    m_aCert = aCert;
  }

  /**
   * @return The participant or service group as specified in the constructor.
   *         Never <code>null</code>.
   */
  @Nonnull
  public IParticipantIdentifier getParticipantID ()
  {
    return m_aParticipantID;
  }

  /**
   * @return The transport profile ID from the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getTransportProtocol ()
  {
    return m_sTransportProtocol;
  }

  /**
   * @return The endpoint URL from the constructor. Neither <code>null</code>
   *         nor empty.
   */
  @Nonnull
  @Nonempty
  public String getEndpointURL ()
  {
    return m_sEndpointURL;
  }

  /**
   * @return The encoded certificate as specified in the constructor.
   */
  @Nonnull
  public X509Certificate getCertificate ()
  {
    return m_aCert;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final R2D2Endpoint rhs = (R2D2Endpoint) o;
    return m_aParticipantID.equals (rhs.m_aParticipantID) &&
           m_sTransportProtocol.equals (rhs.m_sTransportProtocol) &&
           m_sEndpointURL.equals (rhs.m_sEndpointURL) &&
           m_aCert.equals (rhs.m_aCert);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aParticipantID)
                                       .append (m_sTransportProtocol)
                                       .append (m_sEndpointURL)
                                       .append (m_aCert)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("ParticipantID", m_aParticipantID)
                                       .append ("TransportProtocol", m_sTransportProtocol)
                                       .append ("EndpointURL", m_sEndpointURL)
                                       .append ("Cert", m_aCert)
                                       .getToString ();
  }
}
