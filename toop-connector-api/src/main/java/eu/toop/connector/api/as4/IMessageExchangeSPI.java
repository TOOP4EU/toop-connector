/**
 * Copyright (C) 2018-2020 toop.eu
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
package eu.toop.connector.api.as4;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.commons.annotation.Nonempty;

/**
 * Abstract API to be implemented for sending and receiving messages.
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface IMessageExchangeSPI
{
  /**
   * @return The unique ID of the SPI implementation, so that it can be
   *         referenced from a configuration file. The implementer must ensure
   *         the uniqueness of the ID.
   */
  @Nonnull
  @Nonempty
  String getID ();

  /**
   * Register an incoming handler that takes the request/response to handle. The
   * differentiation between step 2/4 and 4/4 must be inside of the SPI
   * implementation. This method is only called once for the chosen
   * implementation, so the implementation can act as an "init" method and
   * perform further implementation activities. If this method is not called, it
   * is ensured that {@link #sendDCOutgoing(IMERoutingInformation, MEMessage)}
   * and {@link #sendDPOutgoing(IMERoutingInformation, MEMessage)} of this
   * implementation are also never called.
   *
   * @param aServletContext
   *        The servlet context in which the handler should be registered. Never
   *        <code>null</code>.
   * @param aIncomingHandler
   *        The handler to use. May not be <code>null</code>.
   * @throws MEException
   *         In case of error.
   */
  void registerIncomingHandler (@Nonnull ServletContext aServletContext,
                                @Nonnull IMEIncomingHandler aIncomingHandler) throws MEException;

  /**
   * Trigger the message transmission in step 1/4. This method acts synchronous.
   *
   * @param aRoutingInfo
   *        Routing information. May not be <code>null</code>.
   * @param aMessage
   *        The message to be exchanged. May not be <code>null</code>.
   * @throws MEException
   *         In case of error.
   */
  void sendDCOutgoing (@Nonnull IMERoutingInformation aRoutingInfo, @Nonnull MEMessage aMessage) throws MEException;

  /**
   * Trigger the message transmission in step 3/4.
   *
   * @param aRoutingInfo
   *        Routing information. May not be <code>null</code>.
   * @param aMessage
   *        The message to be exchanged. May not be <code>null</code>.
   * @throws MEException
   *         In case of error.
   */
  void sendDPOutgoing (@Nonnull IMERoutingInformation aRoutingInfo, @Nonnull MEMessage aMessage) throws MEException;

  /**
   * Shutdown the Message Exchange.
   *
   * @param aServletContext
   *        The servlet context in which the handler should be registered. Never
   *        <code>null</code>.
   */
  void shutdown (@Nonnull ServletContext aServletContext);
}
