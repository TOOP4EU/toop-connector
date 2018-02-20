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
package eu.toop.mp.me;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.soap.SOAPMessage;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.scope.singleton.AbstractGlobalSingleton;

/**
 * The API Entry class for the Message Exchange API.
 *
 * @author: myildiz
 * @date: 15.02.2018.
 */
@NotThreadSafe
public class MEMDelegate extends AbstractGlobalSingleton {
  private final List<IMessageHandler> _messageHandlers = new ArrayList<> ();

  @Deprecated
  @UsedViaReflection
  public MEMDelegate () {
  }

  @Nonnull
  public static MEMDelegate getInstance () {
    return getGlobalSingleton (MEMDelegate.class);
  }

  /**
   * The V1 message sending interface for the message exchange module
   *
   * @param gatewayRoutingMetadata
   *          The container for the endpoint information and docid/procid
   * @param meMessage
   *          the payloads and their metadata to be sent to the gateway.
   */
  public void sendMessage (final GatewayRoutingMetadata gatewayRoutingMetadata, final MEMessage meMessage) {
    final SubmissionData submissionData = EBMSUtils.inferSubmissionData (gatewayRoutingMetadata);
    final SOAPMessage soapMessage = EBMSUtils.convert2MEOutboundAS4Message (submissionData, meMessage);
    SoapUtil.sendSOAPMessage (soapMessage, MessageExchangeEndpointConfig.GW_URL);
  }

  /**
   * Register a new message handler to be able to handle the inbound messages from
   * the AS4 gateway.
   * <p>
   * Duplicate checking skipped for now. So if you register a handler twice, its
   * handle method will be called twice.
   *
   * @param aMessageHandler
   *          message handler to be added
   */
  public void registerMessageHandler (@Nonnull final IMessageHandler aMessageHandler) {
    ValueEnforcer.notNull (aMessageHandler, "MessageHandler");
    _messageHandlers.add (aMessageHandler);
  }

  /**
   * Remove a message handler from this delegate
   *
   * @param aMessageHandler
   *          Message handler to be removed
   */
  public void deregisterMessageHandler (@Nonnull final IMessageHandler aMessageHandler) {
    ValueEnforcer.notNull (aMessageHandler, "MessageHandler");
    _messageHandlers.remove (aMessageHandler);
  }

  /**
   * Dispatch the received inbound message form the AS4 gateway to the handlers
   *
   * @param message
   *          message to be dispatched
   */
  public void dispatchInboundMessage (@Nonnull final SOAPMessage message) {
    for (final IMessageHandler messageHandler : _messageHandlers) {
      try {
        messageHandler.handleMessage (SoapUtil.soap2MEMessage (message));
      } catch (final Exception e) {
        throw new IllegalStateException ("Error handling message via " + messageHandler, e);
      }
    }
  }
}
