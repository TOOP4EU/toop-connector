package eu.toop.connector.smmclient;

import java.io.IOException;
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
   * @param sConceptNamespace
   *        The concept namespace to be used. May neither be <code>null</code>
   *        nor empty.
   * @param sConceptValue
   *        The concept value to be used. May neither be <code>null</code> nor
   *        empty.
   * @return this for chaining
   */
  @Nonnull
  ISMMClient addConceptToBeMapped (@Nonnull @Nonempty String sConceptNamespace,
                                   @Nonnull @Nonempty String sConceptValue);

  /**
   * Add a new concept that requires mapping
   *
   * @param aConceptValue
   *        The concept value to be mapped. May not be <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  default ISMMClient addConceptToBeMapped (@Nonnull final ConceptValue aConceptValue)
  {
    ValueEnforcer.notNull (aConceptValue, "aConceptValue");
    return addConceptToBeMapped (aConceptValue.getNamespace (), aConceptValue.getValue ());
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
   * @throws IOException
   *         in case of HTTP IO error
   */
  @Nonnull
  @ReturnsMutableCopy
  IMappedValueList performMapping (@Nonnull String sLogPrefix,
                                   @Nonnull @Nonempty String sDestNamespace,
                                   @Nonnull ISMMConceptProvider aConceptProvider,
                                   @Nullable IUnmappableCallback aUnmappableCallback) throws IOException;
}
