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
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.io.ByteArrayWrapper;
import com.helger.commons.mime.IMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * A single payload of an AS4 message. Used inside {@link MEMessage}
 *
 * @author myildiz at 15.02.2018.
 */
@Immutable
public final class MEPayload implements Serializable
{
  /**
   * Type of the payload
   */
  private final IMimeType m_aMimeType;

  /**
   * Optional id for the payload. If left empty, a default id will be used. i.e.
   * <em>uuid</em>@mp.toop (see {@link #createRandomPayloadID()}).
   */
  private final String m_sPayloadID;

  /**
   * The actual payload content
   */
  private final ByteArrayWrapper m_aData;

  @Nonnull
  @Nonempty
  public static String createRandomPayloadID ()
  {
    // Must use RFC 2822 style
    return UUID.randomUUID ().toString () + "@mp.toop";
  }

  public MEPayload (@Nonnull final IMimeType aMimeType,
                    @Nullable final String sPayloadID,
                    @Nonnull final ByteArrayWrapper aData)
  {
    ValueEnforcer.notNull (aMimeType, "MimeType");
    ValueEnforcer.notNull (aData, "Data");

    m_aMimeType = aMimeType;
    // Ensure a payload is present
    m_sPayloadID = StringHelper.hasText (sPayloadID) ? sPayloadID : createRandomPayloadID ();
    m_aData = aData;
  }

  @Nonnull
  public IMimeType getMimeType ()
  {
    return m_aMimeType;
  }

  @Nonnull
  public String getMimeTypeString ()
  {
    return m_aMimeType.getAsString ();
  }

  @Nonnull
  public String getPayloadId ()
  {
    return m_sPayloadID;
  }

  @Nonnull
  @ReturnsMutableObject
  public ByteArrayWrapper getData ()
  {
    return m_aData;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("MimeType", m_aMimeType)
                                       .append ("PayloadID", m_sPayloadID)
                                       .append ("Data", m_aData)
                                       .getToString ();
  }
}
