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

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.error.level.EErrorLevel;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * Default implementation of {@link ISMMClient}.
 * 
 * @author Philip Helger
 */
@NotThreadSafe
public class SMMClient implements ISMMClient
{
  private final ICommonsMap <String, ICommonsList <String>> m_aSrcMap = new CommonsHashMap <> ();

  public SMMClient ()
  {}

  @Nonnull
  public SMMClient addConceptToBeMapped (@Nonnull @Nonempty final String sConceptNamespace,
                                         @Nonnull @Nonempty final String sConceptValue)
  {
    ValueEnforcer.notEmpty (sConceptNamespace, "Scheme");
    ValueEnforcer.notEmpty (sConceptValue, "Value");
    m_aSrcMap.computeIfAbsent (sConceptNamespace, k -> new CommonsArrayList <> ()).add (sConceptValue);
    return this;
  }

  @Nonnegative
  public int getTotalCountConceptsToBeMapped ()
  {
    int ret = 0;
    for (final ICommonsList <?> aList : m_aSrcMap.values ())
      ret += aList.size ();
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public IMappedValueList performMapping (@Nonnull final String sLogPrefix,
                                          @Nonnull @Nonempty final String sDestNamespace,
                                          @Nonnull final ISMMConceptProvider aConceptProvider,
                                          @Nullable final ISMMUnmappableCallback aUnmappableCallback) throws IOException
  {

    ValueEnforcer.notNull (sLogPrefix, "LogPrefix");
    ValueEnforcer.notEmpty (sDestNamespace, "DestNamespace");
    ValueEnforcer.notNull (aConceptProvider, "ConceptProvider");

    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> sLogPrefix +
                                "SMM client mapping " +
                                getTotalCountConceptsToBeMapped () +
                                " concept(s) from " +
                                m_aSrcMap.size () +
                                " source namespace(s) to '" +
                                sDestNamespace +
                                "'");

    final MappedValueList ret = new MappedValueList ();
    // for all source namespaces (maybe many)
    for (final Map.Entry <String, ICommonsList <String>> aEntry : m_aSrcMap.entrySet ())
    {
      final String sSourceNamespace = aEntry.getKey ();
      if (sSourceNamespace.equals (sDestNamespace))
      {
        // No mapping needed - use unmapped values
        for (final String sSourceValue : aEntry.getValue ())
        {
          final ConceptValue aValue = new ConceptValue (sSourceNamespace, sSourceValue);
          ret.addMappedValue (new MappedValue (aValue, aValue));
        }
      }
      else
      {
        // Namespace are different - get the mapping list
        // This eventually performs a remote call!
        final MappedValueList aValueList = aConceptProvider.getAllMappedValues (sLogPrefix,
                                                                                sSourceNamespace,
                                                                                sDestNamespace);

        // Map all source values
        for (final String sSourceValue : aEntry.getValue ())
        {
          // The source value may be mapped to 0, 1 or n elements
          final MappedValueList aMatching = aValueList.getAllBySource (x -> x.hasValue (sSourceValue));
          if (aMatching.isEmpty ())
          {
            // Found no mapping
            if (aUnmappableCallback != null)
              aUnmappableCallback.onUnmappableValue (sLogPrefix, sSourceNamespace, sSourceValue, sDestNamespace);
            else
            {
              ToopKafkaClient.send (EErrorLevel.WARN,
                                    () -> sLogPrefix +
                                          "Found no mapping for '" +
                                          sSourceNamespace +
                                          '#' +
                                          sSourceValue +
                                          "' to destination namespace '" +
                                          sDestNamespace +
                                          "'");
            }
            // TODO shall we add a mapping to null?
          }
          else
          {
            if (aMatching.size () > 1)
            {
              ToopKafkaClient.send (EErrorLevel.WARN,
                                    () -> sLogPrefix +
                                          "Found " +
                                          aMatching.size () +
                                          " mappings for '" +
                                          sSourceNamespace +
                                          '#' +
                                          sSourceValue +
                                          "' to destination namespace '" +
                                          sDestNamespace +
                                          "'");
            }
            ret.addAllMappedValues (aMatching);
          }
        }
      }
    }
    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> sLogPrefix + "SMM client mapping found " + ret.size () + " mapping(s)");
    return ret;
  }
}
