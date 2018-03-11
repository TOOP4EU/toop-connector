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
package eu.toop.connector.smmclient;

import java.util.function.Predicate;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.ICommonsIterable;
import com.helger.commons.lang.IHasSize;

import eu.toop.commons.concept.ConceptValue;

/**
 * Read-only interface for {@link MappedValueList}.
 *
 * @author Philip Helger
 */
public interface IMappedValueList extends IHasSize, ICommonsIterable<MappedValue> {
  @Nullable
  MappedValue getAtIndex (@Nonnegative int nIndex);

  @Nullable
  default MappedValue getFirst () {
    return getAtIndex (0);
  }

  @Nonnull
  @ReturnsMutableObject
  IMappedValueList getAllBySource (@Nonnull final Predicate<? super ConceptValue> aFilter);

  @Nonnull
  @ReturnsMutableObject
  IMappedValueList getAllByDestination (@Nonnull final Predicate<? super ConceptValue> aFilter);
}
