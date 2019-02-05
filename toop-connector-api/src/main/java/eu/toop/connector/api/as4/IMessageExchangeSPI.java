package eu.toop.connector.api.as4;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;

/**
 * Abstract API to be implemented for sending and receiving messages.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public interface IMessageExchangeSPI
{
  /**
   * @return The unique ID of the SPI implementation, so that it can be referenced
   *         from a configuration file. The implementer must ensure the uniqueness
   *         of the ID.
   */
  @Nonnull
  @Nonempty
  String getID ();

  void sendDCOutgoing (@Nonnull IMERoutingInformation aRoutingInfo, @Nonnull MEMessage aMessage) throws MEException;
}
