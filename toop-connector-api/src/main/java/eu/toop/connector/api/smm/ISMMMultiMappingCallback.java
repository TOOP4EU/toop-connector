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
 * Callback interface to customize the handling of 1:n mappings.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
@FunctionalInterface
public interface ISMMMultiMappingCallback extends Serializable
{
  /**
   * Invoked for every 1:n mapping.
   *
   * @param sLogPrefix
   *        Logging prefix
   * @param sSourceNamespace
   *        Source namespace URI
   * @param sSourceValue
   *        Source value
   * @param sDestNamespace
   *        Destination namespace URI
   * @param aMatching
   *        The matching values. Never <code>null</code>.
   */
  void onMultiMapping (@Nonnull String sLogPrefix,
                       @Nonnull @Nonempty String sSourceNamespace,
                       @Nonnull @Nonempty String sSourceValue,
                       @Nonnull @Nonempty String sDestNamespace,
                       @Nonnull @Nonempty MappedValueList aMatching);
}
