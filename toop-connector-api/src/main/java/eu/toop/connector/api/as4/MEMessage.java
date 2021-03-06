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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * List of {@link MEPayload} objects.
 * 
 * @author myildiz at 12.02.2018.
 */
public class MEMessage implements Serializable
{
  private final ICommonsList <MEPayload> m_aPayloads = new CommonsArrayList <> ();

  public MEMessage ()
  {}

  /**
   * For ease of use, get the first payload
   *
   * @return the first payload
   * @throws MEException
   *         in case non is contained
   */
  @Nonnull
  public MEPayload head ()
  {
    if (m_aPayloads.isEmpty ())
      throw new MEException ("There is no payload");
    return m_aPayloads.getFirst ();
  }

  @Nonnull
  @ReturnsMutableObject
  public ICommonsList <MEPayload> payloads ()
  {
    return m_aPayloads;
  }

  @Nonnull
  @ReturnsMutableObject
  public static MEMessage create (@Nonnull final MEPayload aPayload)
  {
    ValueEnforcer.notNull (aPayload, "Payload");
    final MEMessage ret = new MEMessage ();
    ret.payloads ().add (aPayload);
    return ret;
  }
}
