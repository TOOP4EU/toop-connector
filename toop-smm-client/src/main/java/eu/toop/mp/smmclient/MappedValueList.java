package eu.toop.mp.smmclient;

import java.util.Iterator;
import java.util.function.Predicate;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.ToStringGenerator;

@NotThreadSafe
public class MappedValueList implements IMappedValueList {
  private final ICommonsList<MappedValue> m_aList;

  public MappedValueList () {
    this (new CommonsArrayList<> ());
  }

  /**
   * Protected constructor
   *
   * @param aValues
   *          The value list to be used. May not be <code>null</code>. Important:
   *          the values are NOT cloned!
   */
  protected MappedValueList (@Nonnull final ICommonsList<MappedValue> aValues) {
    m_aList = ValueEnforcer.notNull (aValues, "Values");
  }

  public void addAllMappedValues (@Nonnull final IMappedValueList aValues) {
    ValueEnforcer.notNull (aValues, "Values");
    m_aList.addAll (aValues);
  }

  public void addMappedValue (@Nonnull final MappedValue aValue) {
    ValueEnforcer.notNull (aValue, "Value");
    m_aList.add (aValue);
  }

  public void addMappedValue (@Nonnull @Nonempty final String sSourceNamespace,
                              @Nonnull @Nonempty final String sSourceValue,
                              @Nonnull @Nonempty final String sDestNamespace,
                              @Nonnull @Nonempty final String sDestValue) {
    addMappedValue (new MappedValue (new ConceptValue (sSourceNamespace, sSourceValue),
                                     new ConceptValue (sDestNamespace, sDestValue)));
  }

  @Nonnull
  public Iterator<MappedValue> iterator () {
    return m_aList.iterator ();
  }

  public boolean isEmpty () {
    return m_aList.isEmpty ();
  }

  @Nonnegative
  public int size () {
    return m_aList.size ();
  }

  @Nullable
  public MappedValue getAtIndex (@Nonnegative final int nIndex) {
    return m_aList.getAtIndex (nIndex);
  }

  @Nonnull
  @ReturnsMutableObject
  public MappedValueList getAllBySource (@Nonnull final Predicate<? super ConceptValue> aFilter) {
    ValueEnforcer.notNull (aFilter, "Filter");
    return new MappedValueList (m_aList.getAll (x -> aFilter.test (x.getSource ())));
  }

  @Nonnull
  @ReturnsMutableObject
  public MappedValueList getAllByDestination (@Nonnull final Predicate<? super ConceptValue> aFilter) {
    ValueEnforcer.notNull (aFilter, "Filter");
    return new MappedValueList (m_aList.getAll (x -> aFilter.test (x.getDestination ())));
  }

  @Override
  public String toString () {
    return new ToStringGenerator (this).append ("List", m_aList).getToString ();
  }
}