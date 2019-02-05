/**
 * Copyright (C) 2019 toop.eu
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
package eu.toop.mem.phase4;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;

import eu.toop.connector.api.as4.IMERoutingInformation;
import eu.toop.connector.api.as4.IMessageExchangeSPI;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;

/**
 * {@link IMessageExchangeSPI} implementation using ph-as4
 *
 * @author Philip Helger
 */
public class Phase4MessageExchangeSPI implements IMessageExchangeSPI
{
  public static final String ID = "mem-phase4";

  private IIncomingHandler m_aIncomingHandler;

  public Phase4MessageExchangeSPI ()
  {}

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return ID;
  }

  public void registerIncomingHandler (@Nonnull final ServletContext aServletContext,
                                       @Nonnull final IIncomingHandler aIncomingHandler) throws MEException
  {
    ValueEnforcer.notNull (aServletContext, "ServletContext");
    ValueEnforcer.notNull (aIncomingHandler, "IncomingHandler");
    if (m_aIncomingHandler != null)
      throw new IllegalStateException ("Another incoming handler was already registered!");
    m_aIncomingHandler = aIncomingHandler;

    // TODO register for servlet
  }

  public void sendDCOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {}

  public void sendDPOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {}
}
