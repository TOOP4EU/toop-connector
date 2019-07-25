/**
 * Copyright (C) 2018-2019 toop.eu
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

import eu.toop.commons.exchange.ToopResponseWithAttachments140;

/**
 * Customization interface for forwarding messages from step 4/4 to the real DC.
 *
 * @author Philip Helger
 */
public interface IToDC extends Serializable
{
  /**
   * Forward the TOOP Response with attachments to the DC
   *
   * @param aResponseWA
   *        The TOOP response with attachments for the DC.
   * @return {@link ESuccess}
   */
  @Nonnull
  ESuccess passResponseOnToDC (@Nonnull ToopResponseWithAttachments140 aResponseWA);
}
