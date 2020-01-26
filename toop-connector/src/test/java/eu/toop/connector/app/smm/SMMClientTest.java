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
package eu.toop.connector.app.smm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.commons.concept.ConceptValue;
import eu.toop.commons.usecase.EToopConcept;
import eu.toop.commons.usecase.SMMDocumentTypeMapping;
import eu.toop.connector.api.smm.IMappedValueList;
import eu.toop.connector.api.smm.ISMMClient;
import eu.toop.connector.api.smm.ISMMConceptProvider;
import eu.toop.connector.api.smm.ISMMUnmappableCallback;
import eu.toop.connector.api.smm.MappedValue;

/**
 * Test class for class {@link SMMClient}.
 *
 * @author Philip Helger
 */
public final class SMMClientTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMMClientTest.class);

  private static final ConceptValue CONCEPT_TOOP_1 = EToopConcept.COMPANY_CODE.getAsConceptValue ();
  private static final ConceptValue CONCEPT_FR_1 = new ConceptValue (CMockSMM.NS_FREEDONIA, "FreedoniaCompanyCode");

  // Use with cache and remote
  private static final ISMMConceptProvider [] CP = new ISMMConceptProvider [] { new SMMConceptProviderFileBased () };
  private static final ISMMUnmappableCallback UCB = (sLogPrefix, aSourceNamespace, aSourceValue, aDestNamespace) -> {
    // Do nothing
  };

  @Test
  public void testEmpty () throws IOException
  {
    for (final ISMMConceptProvider aCP : CP)
    {
      LOGGER.info ("Starting testEmpty");
      final ISMMClient aClient = new SMMClient ();
      final IMappedValueList ret = aClient.performMapping (CMockSMM.LOG_PREFIX, CMockSMM.NS_FREEDONIA, aCP, UCB);
      assertNotNull (ret);
      assertTrue (ret.isEmpty ());
      assertEquals (0, ret.size ());
    }
  }

  @Test
  public void testOneMatch () throws IOException
  {
    for (final ISMMConceptProvider aCP : CP)
    {
      LOGGER.info ("Starting testOneMatch");
      final ISMMClient aClient = new SMMClient ();
      aClient.addConceptToBeMapped (CONCEPT_TOOP_1);
      final IMappedValueList ret = aClient.performMapping (CMockSMM.LOG_PREFIX, CMockSMM.NS_FREEDONIA, aCP, UCB);
      assertNotNull (ret);
      assertEquals (1, ret.size ());

      // Check concept has a 1:1 mapping
      final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_TOOP_1));
      assertNotNull (aMapped);
      assertEquals (1, aMapped.size ());
      final MappedValue aValue = aMapped.getFirst ();
      assertNotNull (aValue);
      assertEquals (CONCEPT_TOOP_1, aValue.getSource ());
      assertEquals (CONCEPT_FR_1, aValue.getDestination ());
    }
  }

  @Test
  public void testOneMatchOneNotFound () throws IOException
  {
    for (final ISMMConceptProvider aCP : CP)
    {
      LOGGER.info ("Starting testOneMatchOneNotFound");
      final ISMMClient aClient = new SMMClient ();
      aClient.addConceptToBeMapped (CONCEPT_TOOP_1);
      aClient.addConceptToBeMapped (SMMDocumentTypeMapping.SMM_DOMAIN_REGISTERED_ORGANIZATION, "NonExistingField");
      aClient.addConceptToBeMapped ("SourceNamespace", "NonExistingField");
      final IMappedValueList ret = aClient.performMapping (CMockSMM.LOG_PREFIX, CMockSMM.NS_FREEDONIA, aCP, UCB);
      assertNotNull (ret);
      assertEquals (1, ret.size ());

      // Check concept has a 1:1 mapping
      final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_TOOP_1));
      assertNotNull (aMapped);
      assertEquals (1, aMapped.size ());
      final MappedValue aValue = aMapped.getFirst ();
      assertNotNull (aValue);
      assertEquals (CONCEPT_TOOP_1, aValue.getSource ());
      assertEquals (CONCEPT_FR_1, aValue.getDestination ());
    }
  }

  @Test
  public void testNoMappingNeeded () throws IOException
  {
    for (final ISMMConceptProvider aCP : CP)
    {
      LOGGER.info ("Starting testNoMappingNeeded");
      final ISMMClient aClient = new SMMClient ();
      aClient.addConceptToBeMapped (CONCEPT_FR_1);
      final IMappedValueList ret = aClient.performMapping (CMockSMM.LOG_PREFIX, CMockSMM.NS_FREEDONIA, aCP, UCB);
      assertNotNull (ret);
      assertEquals (1, ret.size ());

      // Check concept has a 1:1 mapping
      final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_FR_1));
      assertNotNull (aMapped);
      assertEquals (1, aMapped.size ());
      final MappedValue aValue = aMapped.getFirst ();
      assertNotNull (aValue);
      assertEquals (CONCEPT_FR_1, aValue.getSource ());
      assertEquals (CONCEPT_FR_1, aValue.getDestination ());
    }
  }
}
