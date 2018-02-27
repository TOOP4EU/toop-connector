package eu.toop.mp.smmclient;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;

@ThreadSafe
public class SMMClient {
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMMClient.class);

  private final ICommonsMap<String, ICommonsList<String>> m_aSrcMap = new CommonsHashMap<> ();

  public SMMClient () {
  }

  /**
   * Add a new concept that requires mapping
   *
   * @param sConceptNamespace
   *          The concept namespace to be used. May neither be <code>null</code>
   *          nor empty.
   * @param sConceptValue
   *          The concept value to be used. May neither be <code>null</code> nor
   *          empty.
   * @return this for chaining
   */
  @Nonnull
  public SMMClient addConceptToBeMapped (@Nonnull @Nonempty final String sConceptNamespace,
                                         @Nonnull @Nonempty final String sConceptValue) {
    ValueEnforcer.notEmpty (sConceptNamespace, "Scheme");
    ValueEnforcer.notEmpty (sConceptValue, "Value");
    m_aSrcMap.computeIfAbsent (sConceptNamespace, k -> new CommonsArrayList<> ()).add (sConceptValue);
    return this;
  }

  /**
   * Add a new concept that requires mapping
   *
   * @param aConceptValue
   *          The concept value to be mapped. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public SMMClient addConceptToBeMapped (@Nonnull final ConceptValue aConceptValue) {
    ValueEnforcer.notNull (aConceptValue, "aConceptValue");
    return addConceptToBeMapped (aConceptValue.getNamespace (), aConceptValue.getValue ());
  }

  /**
   * Perform a mapping of all provided source values to the provided destination
   * namespace.
   *
   * @param sDestNamespace
   *          Destination namespace to map the concepts to. May neither be
   *          <code>null</code> nor empty.
   * @return A non-<code>null</code> but maybe empty list of mappings.
   * @throws IOException
   *           in case of HTTP IO error
   */
  @Nonnull
  @ReturnsMutableCopy
  public IMappedValueList performMapping (@Nonnull final String sDestNamespace) throws IOException {
    final MappedValueList ret = new MappedValueList ();
    // for all source namespaces (maybe many)
    for (final Map.Entry<String, ICommonsList<String>> aEntry : m_aSrcMap.entrySet ()) {
      final String sSourceNamespace = aEntry.getKey ();
      if (sSourceNamespace.equals (sDestNamespace)) {
        // No mapping needed - use unmapped values
        for (final String sSourceValue : aEntry.getValue ()) {
          final ConceptValue aValue = new ConceptValue (sSourceNamespace, sSourceValue);
          ret.addMappedValue (new MappedValue (aValue, aValue));
        }
      } else {
        // Namespace are different - get the mapping list
        // This eventually performs a remote call!
        final MappedValueList aValueList = SMMConceptCache.getAllMappedValues (sSourceNamespace, sDestNamespace);

        // Map all source values
        for (final String sSourceValue : aEntry.getValue ()) {
          // The source value may be mapped to 0, 1 or n elements
          final MappedValueList aMatching = aValueList.getAllBySource (x -> x.hasValue (sSourceValue));
          if (aMatching.isEmpty ()) {
            // Found no mapping
            s_aLogger.info ("Found no mapping for '" + sSourceNamespace + '#' + sSourceValue
                            + "' to destination namespace '" + sDestNamespace + "'");
          } else {
            if (aMatching.size () > 1)
              s_aLogger.warn ("Found " + aMatching.size () + " mappings for '" + sSourceNamespace + '#' + sSourceValue
                              + "' to destination namespace '" + sDestNamespace + "'");
            ret.addAllMappedValues (aMatching);
          }
        }
      }
    }
    return ret;
  }
}
