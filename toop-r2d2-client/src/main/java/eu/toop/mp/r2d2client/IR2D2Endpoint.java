package eu.toop.mp.r2d2client;

import java.io.Serializable;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * Read-only base interface for a single result endpoint. See
 * {@link R2D2Endpoint} for the default implementation.
 * 
 * @author Philip Helger
 */
@MustImplementEqualsAndHashcode
public interface IR2D2Endpoint extends Serializable
{
  /**
   * @return The participant or service group as specified in the constructor.
   *         Never <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getParticipantID ();

  /**
   * @return The transport profile ID from the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  String getTransportProtocol ();

  /**
   * @return The endpoint URL from the constructor. Neither <code>null</code>
   *         nor empty.
   */
  @Nonnull
  @Nonempty
  String getEndpointURL ();

  /**
   * @return The encoded certificate as specified in the constructor.
   */
  @Nonnull
  X509Certificate getCertificate ();
}
