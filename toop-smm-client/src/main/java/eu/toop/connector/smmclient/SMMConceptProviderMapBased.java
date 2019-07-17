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
package eu.toop.connector.smmclient;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.string.ToStringGenerator;

import eu.toop.connector.api.smm.ISMMConceptProvider;
import eu.toop.connector.api.smm.MappedValueList;

/**
 * Implementation of {@link ISMMConceptProvider} using a static Map. This class
 * is mainly for simulating SMM or providing alternate SMM mappings that cannot
 * be provided by the central SMS.
 *
 * @author Philip Helger
 * @since 0.10.6
 */
@Immutable
public class SMMConceptProviderMapBased implements ISMMConceptProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMMConceptProviderMapBased.class);

  private final String m_sSourceNamespace;
  private final String m_sDestNamespace;
  private final ICommonsMap <String, String> m_aFieldMapping;

  /**
   * Constructor with predefined values
   *
   * @param sSourceNamespace
   *        Source namespace to be mapped from. May neither be <code>null</code>
   *        nor empty.
   * @param sDestNamespace
   *        Destination namespace to be mapped to. May neither be
   *        <code>null</code> nor empty.
   * @param aFieldMapping
   *        The 1:1 mapping from source concept name to destination concept
   *        name. May not be <code>null</code> but maybe empty.
   */
  public SMMConceptProviderMapBased (@Nonnull @Nonempty final String sSourceNamespace,
                                     @Nonnull @Nonempty final String sDestNamespace,
                                     @Nonnull final Map <String, String> aFieldMapping)
  {
    ValueEnforcer.notEmpty (sSourceNamespace, "SourceNamespace");
    ValueEnforcer.notEmpty (sDestNamespace, "DestNamespace");
    ValueEnforcer.notNull (aFieldMapping, "FieldMapping");
    m_sSourceNamespace = sSourceNamespace;
    m_sDestNamespace = sDestNamespace;
    m_aFieldMapping = new CommonsHashMap <> (aFieldMapping);
  }

  @Nonnull
  @Nonempty
  public final String getSourceNamespace ()
  {
    return m_sSourceNamespace;
  }

  @Nonnull
  @Nonempty
  public final String getDestNamespace ()
  {
    return m_sDestNamespace;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsMap <String, String> getAllFieldMappings ()
  {
    return m_aFieldMapping.getClone ();
  }

  @Nonnull
  public MappedValueList getAllMappedValues (@Nonnull final String sLogPrefix,
                                             @Nonnull final String sSourceNamespace,
                                             @Nonnull final String sDestNamespace)
  {
    LOGGER.info (sLogPrefix +
                 "Using static Map for SMM mappings from '" +
                 sSourceNamespace +
                 "' to '" +
                 sDestNamespace +
                 "'");

    final MappedValueList ret = new MappedValueList ();
    if (m_sSourceNamespace.equals (sSourceNamespace) && m_sDestNamespace.equals (sDestNamespace))
    {
      // Namespace match
      for (final Map.Entry <String, String> aEntry : m_aFieldMapping.entrySet ())
        ret.addMappedValue (m_sSourceNamespace, aEntry.getKey (), m_sDestNamespace, aEntry.getValue ());
    }
    else
    {
      // Namespace mismatch
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Namespace query mismatch. Queried '" +
                      sSourceNamespace +
                      "' to '" +
                      sDestNamespace +
                      "'; having data for '" +
                      m_sSourceNamespace +
                      "' to '" +
                      m_sDestNamespace +
                      "'");
    }
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("SourceNS", m_sSourceNamespace)
                                       .append ("DestNS", m_sDestNamespace)
                                       .append ("FieldMapping", m_aFieldMapping)
                                       .getToString ();
  }
}
