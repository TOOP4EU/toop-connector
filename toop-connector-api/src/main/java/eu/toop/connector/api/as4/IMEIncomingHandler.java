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

import java.io.Serializable;

import javax.annotation.Nonnull;

import eu.toop.commons.exchange.ToopRequestWithAttachments140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;

/**
 * The callback handler for incoming messages.
 *
 * @author Philip Helger
 */
public interface IMEIncomingHandler extends Serializable
{
  /**
   * Handle an incoming request for step 2/4.
   *
   * @param aRequest
   *        The request to handle. Never <code>null</code>.
   * @throws MEException
   *         In case of error.
   */
  void handleIncomingRequest (@Nonnull ToopRequestWithAttachments140 aRequest) throws MEException;

  /**
   * Handle an incoming response for step 4/4.
   *
   * @param aResponse
   *        The response to handle. Never <code>null</code>.
   * @throws MEException
   *         In case of error.
   */
  void handleIncomingResponse (@Nonnull ToopResponseWithAttachments140 aResponse) throws MEException;
}
