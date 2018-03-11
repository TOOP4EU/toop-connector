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
package eu.toop.connector.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.peppol.smp.ESMPTransportProfile;

/**
 * Contains all protocols supported by MP
 *
 * @author Philip Helger
 */
public enum ETCProtocol implements IHasID<String> {
  /**
   * AS4 using the common transport profile introduced by eSENS
   */
  AS4 ("as4", ESMPTransportProfile.TRANSPORT_PROFILE_BDXR_AS4.getID ());

  public static final ETCProtocol DEFAULT = AS4;

  private final String m_sID;

  private String m_sTransportProfileID;

  private ETCProtocol (@Nonnull @Nonempty final String sID, @Nonnull @Nonempty final String sTransportProfileID) {
    m_sID = sID;
    m_sTransportProfileID = sTransportProfileID;
  }

  @Nonnull
  @Nonempty
  public String getID () {
    return m_sID;
  }

  /**
   * @return The transport profile ID for the SMP to be used for this MP protocol.
   *         Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getTransportProfileID () {
    return m_sTransportProfileID;
  }

  @Nullable
  public static ETCProtocol getFromIDOrNull (@Nullable final String sID) {
    return EnumHelper.getFromIDOrNull (ETCProtocol.class, sID);
  }
}
