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
package eu.toop.connector.me;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableObject;

/**
 * @author myildiz at 12.02.2018.
 */
public class MEMessage {
  private final List<MEPayload> payloads = new ArrayList<> ();

  public MEMessage () {
  }

  public MEMessage (@Nonnull final MEPayload aPayload) {
    ValueEnforcer.notNull (aPayload, "Payload");
    payloads.add (aPayload);
  }

  /**
   * For ease of use, get the first payload
   *
   * @return the first payload
   * @throws MEException
   *           in case non is contained
   */
  @Nonnull
  public MEPayload head () {
    if (payloads.isEmpty ())
      throw new MEException ("There is no payload");
    return payloads.get (0);
  }

  @Nonnull
  @ReturnsMutableObject
  public List<MEPayload> getPayloads () {
    return payloads;
  }
}
