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
package eu.toop.connector.app.mp;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.state.ESuccess;

import eu.toop.commons.exchange.ToopRequestWithAttachments140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;

/**
 * Customization interface for forwarding messages from steps 2/4 and 3/4 to the
 * real DP.
 *
 * @author Philip Helger
 */
public interface IToDP extends Serializable
{
  /**
   * Forward a new TOOP Request to the DP.
   *
   * @param aRequestWA
   *        The TOOP request for the DP.
   * @return {@link ESuccess}
   */
  @Nonnull
  ESuccess passRequestOnToDP (@Nonnull ToopRequestWithAttachments140 aRequestWA);

  /**
   * Return a response with errors back to the DP. This is only called, if the
   * transmission back to the requesting DC was not possible (e.g. because of
   * semantic mapping errors or because of AS4 transmission issues).
   *
   * @param aResponseWA
   *        The TOOP response with the contained errors
   * @return {@link ESuccess}
   */
  @Nonnull
  ESuccess returnErrorResponseToDP (@Nonnull ToopResponseWithAttachments140 aResponseWA);
}
