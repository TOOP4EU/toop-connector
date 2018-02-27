package eu.toop.mp.smmclient;

import java.util.function.Predicate;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.ICommonsIterable;
import com.helger.commons.lang.IHasSize;

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
