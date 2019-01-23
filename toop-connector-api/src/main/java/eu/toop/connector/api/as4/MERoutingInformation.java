package eu.toop.connector.api.as4;

import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;

/**
 * Default implementation of {@link IMERoutingInformation}.
 *
 * @author Philip Helger
 */
public class MERoutingInformation implements IMERoutingInformation
{
  private final IParticipantIdentifier m_aSenderID;
  private final IParticipantIdentifier m_aReceiverID;
  private final IDocumentTypeIdentifier m_aDocTypeID;
  private final IProcessIdentifier m_aProcessID;
  private final String m_sEndpointURL;
  private final X509Certificate m_aCert;

  public MERoutingInformation (@Nonnull final IParticipantIdentifier aSenderID,
                               @Nonnull final IParticipantIdentifier aReceiverID,
                               @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                               @Nonnull final IProcessIdentifier aProcessID,
                               @Nonnull @Nonempty final String sEndpointURL,
                               @Nonnull final X509Certificate aCert)
  {
    ValueEnforcer.notNull (aSenderID, "SenderID");
    ValueEnforcer.notNull (aReceiverID, "ReceiverID");
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (aProcessID, "ProcessID");
    ValueEnforcer.notEmpty (sEndpointURL, "EndpointURL");
    ValueEnforcer.notNull (aCert, "Cert");

    m_aSenderID = aSenderID;
    m_aReceiverID = aReceiverID;
    m_aDocTypeID = aDocTypeID;
    m_aProcessID = aProcessID;
    m_sEndpointURL = sEndpointURL;
    m_aCert = aCert;
  }

  @Nonnull
  public IParticipantIdentifier getSenderID ()
  {
    return m_aSenderID;
  }

  @Nonnull
  public IParticipantIdentifier getReceiverID ()
  {
    return m_aReceiverID;
  }

  @Nonnull
  public IDocumentTypeIdentifier getDocumentTypeID ()
  {
    return m_aDocTypeID;
  }

  @Nonnull
  public IProcessIdentifier getProcessID ()
  {
    return m_aProcessID;
  }

  @Nonnull
  @Nonempty
  public String getEndpointURL ()
  {
    return m_sEndpointURL;
  }

  @Nonnull
  public X509Certificate getCertificate ()
  {
    return m_aCert;
  }
}
