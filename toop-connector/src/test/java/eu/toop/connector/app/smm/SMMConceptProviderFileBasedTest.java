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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import eu.toop.commons.usecase.SMMDocumentTypeMapping;
import eu.toop.connector.api.smm.MappedValueList;

/**
 * Test class for class {@link SMMConceptProviderFileBased}.
 *
 * @author Philip Helger
 */
public class SMMConceptProviderFileBasedTest
{
  @Test
  public void testBasic ()
  {
    final SMMConceptProviderFileBased cp = new SMMConceptProviderFileBased ();
    final MappedValueList x = cp.getAllMappedValues ("",
                                                     SMMDocumentTypeMapping.SMM_DOMAIN_REGISTERED_ORGANIZATION,
                                                     CMockSMM.NS_FREEDONIA);
    final MappedValueList y = cp.getAllMappedValues ("",
                                                     CMockSMM.NS_FREEDONIA,
                                                     SMMDocumentTypeMapping.SMM_DOMAIN_REGISTERED_ORGANIZATION);
    assertNotNull (x);
    assertNotNull (y);
    assertEquals (x.size (), y.size ());
    assertNotEquals (x, y);

    assertEquals (1, x.getAllBySource (c -> c.getValue ().equals ("CompanyCode")).size ());
    assertEquals (0, x.getAllBySource (c -> c.getValue ().equals ("FreedoniaCompanyCode")).size ());
    assertEquals (0, x.getAllByDestination (c -> c.getValue ().equals ("CompanyCode")).size ());
    assertEquals (1, x.getAllByDestination (c -> c.getValue ().equals ("FreedoniaCompanyCode")).size ());

    assertEquals (0, y.getAllBySource (c -> c.getValue ().equals ("CompanyCode")).size ());
    assertEquals (1, y.getAllBySource (c -> c.getValue ().equals ("FreedoniaCompanyCode")).size ());
    assertEquals (1, y.getAllByDestination (c -> c.getValue ().equals ("CompanyCode")).size ());
    assertEquals (0, y.getAllByDestination (c -> c.getValue ().equals ("FreedoniaCompanyCode")).size ());
  }
}
