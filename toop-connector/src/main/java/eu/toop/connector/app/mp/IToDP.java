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

import eu.toop.commons.dataexchange.v140.TDETOOPRequestType;

/**
 * Customization interface for forwarding messages from step 2/4 to the real DP.
 *
 * @author Philip Helger
 */
public interface IToDP extends Serializable
{
  /**
   * Forward the signed TOOP Request to the DP
   *
   * @param aRequest
   *        The TOOP request for the DP.
   * @return {@link ESuccess}
   */
  @Nonnull
  ESuccess passOnToDP (@Nonnull TDETOOPRequestType aRequest);
}
