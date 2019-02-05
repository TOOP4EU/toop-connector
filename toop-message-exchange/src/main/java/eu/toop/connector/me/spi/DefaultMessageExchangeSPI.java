package eu.toop.connector.me.spi;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;

import eu.toop.connector.api.as4.IMERoutingInformation;
import eu.toop.connector.api.as4.IMessageExchangeSPI;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.api.as4.MessageExchangeManager;
import eu.toop.connector.me.EActingSide;
import eu.toop.connector.me.GatewayRoutingMetadata;
import eu.toop.connector.me.MEMDelegate;

/**
 * Implementation of {@link IMessageExchangeSPI} using the "TOOP AS4 Gateway
 * back-end interface".
 * 
 * @author Philip Helger
 */
@IsSPIImplementation
public final class DefaultMessageExchangeSPI implements IMessageExchangeSPI
{
  @Nonnull
  @Nonempty
  public String getID ()
  {
    return MessageExchangeManager.DEFAULT_ID;
  }

  public void sendDCOutgoing (@Nonnull IMERoutingInformation aRoutingInfo, @Nonnull MEMessage aMessage)
  {
    final GatewayRoutingMetadata aGRM = new GatewayRoutingMetadata (aRoutingInfo.getSenderID ().getURIEncoded (),
                                                                    aRoutingInfo.getDocumentTypeID ().getURIEncoded (),
                                                                    aRoutingInfo.getProcessID ().getURIEncoded (),
                                                                    aRoutingInfo.getEndpointURL (),
                                                                    aRoutingInfo.getCertificate (),
                                                                    EActingSide.DC);
    if (!MEMDelegate.getInstance ().sendMessage (aGRM, aMessage))
    {
      throw new MEException ("Error sending message");
    }
  }
}
