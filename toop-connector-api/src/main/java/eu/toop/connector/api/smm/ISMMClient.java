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
package eu.toop.connector.api.smm;

import java.io.Serializable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;

import eu.toop.commons.concept.ConceptValue;

/**
 * Semantic mapping module client.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
public interface ISMMClient extends Serializable
{
  /**
   * Add a new concept that requires mapping
   *
   * @param sSrcConceptNamespace
   *        The concept namespace to be used. May neither be <code>null</code>
   *        nor empty.
   * @param sSrcConceptValue
   *        The concept value to be used. May neither be <code>null</code> nor
   *        empty.
   * @return this for chaining
   */
  @Nonnull
  ISMMClient addConceptToBeMapped (@Nonnull @Nonempty String sSrcConceptNamespace,
                                   @Nonnull @Nonempty String sSrcConceptValue);

  /**
   * Add a new concept that requires mapping
   *
   * @param aSrcConceptValue
   *        The concept value to be mapped. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  default ISMMClient addConceptToBeMapped (@Nonnull final ConceptValue aSrcConceptValue)
  {
    ValueEnforcer.notNull (aSrcConceptValue, "ConceptValue");
    return addConceptToBeMapped (aSrcConceptValue.getNamespace (), aSrcConceptValue.getValue ());
  }

  /**
   * @return The total number of mapped artefacts. Always &ge; 0.
   */
  @Nonnegative
  int getTotalCountConceptsToBeMapped ();

  /**
   * Perform a mapping of all provided source values to the provided destination
   * namespace.
   *
   * @param sLogPrefix
   *        Logging prefix to easily fit together what belongs together. May not
   *        be <code>null</code> but may be empty.
   * @param sDestNamespace
   *        Destination namespace to map the concepts to. May neither be
   *        <code>null</code> nor empty.
   * @param aConceptProvider
   *        The concept provider implementation to use. May not be
   *        <code>null</code>.
   * @param aUnmappableCallback
   *        Callback to be invoked if a non-mappable entry was found. May be
   *        <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of mappings.
   */
  @Nonnull
  @ReturnsMutableCopy
  IMappedValueList performMapping (@Nonnull String sLogPrefix,
                                   @Nonnull @Nonempty String sDestNamespace,
                                   @Nonnull ISMMConceptProvider aConceptProvider,
                                   @Nullable ISMMUnmappableCallback aUnmappableCallback);
}
