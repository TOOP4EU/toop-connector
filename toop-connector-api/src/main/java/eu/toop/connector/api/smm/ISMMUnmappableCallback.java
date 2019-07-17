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
package eu.toop.connector.api.smm;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

/**
 * Callback interface to customize the handling of failed SMM mappings at
 * runtime.
 * 
 * @author Philip Helger
 */
@FunctionalInterface
public interface ISMMUnmappableCallback extends Serializable
{
  /**
   * Invoked for every unmappable value.
   *
   * @param sLogPrefix
   *        Logging prefix
   * @param aSourceNamespace
   *        Source namespace URI
   * @param aSourceValue
   *        Source value
   * @param aDestNamespace
   *        Destination namespace URI
   */
  void onUnmappableValue (@Nonnull String sLogPrefix,
                          @Nonnull @Nonempty String aSourceNamespace,
                          @Nonnull @Nonempty String aSourceValue,
                          @Nonnull @Nonempty String aDestNamespace);
}
