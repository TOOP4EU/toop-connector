package eu.toop.connector.smmclient;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIInterface;

/**
 * Base interface for a Semantic Mapping Module Concept provider. This is the
 * main abstraction layer for querying mapped namespaces.
 *
 * @author Philip Helger
 * @since 0.9.2
 */
@IsSPIInterface
public interface ISMMConceptProvider extends Serializable {
  /**
   * Get all mapped values from source to target namespace. If not present (in
   * cache) it may be retrieved from an eventually configured remote server.
   *
   * @param sLogPrefix
   *          Log prefix. May not be <code>null</code> but may be empty.
   * @param sSourceNamespace
   *          Source namespace to map from. May not be <code>null</code>.
   * @param sDestNamespace
   *          Target namespace to map to. May not be <code>null</code>.
   * @return The non-<code>null</code> but maybe empty list of mapped values.
   * @throws IOException
   *           In case fetching from server failed
   */
  @Nonnull
  MappedValueList getAllMappedValues (@Nonnull String sLogPrefix, @Nonnull String sSourceNamespace,
                                      @Nonnull String sDestNamespace) throws IOException;
}
