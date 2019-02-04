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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsOrderedSet;

/**
 * Test class for class SMMConceptCache.
 *
 * @author Philip Helger
 */
public final class SMMConceptProviderGRLCRemoteTest {
  private static final Logger LOGGER = LoggerFactory.getLogger (SMMConceptProviderGRLCRemoteTest.class);

  @Before
  public void reset () {
    SMMConceptProviderGRLCRemote.clearCache ();
  }

  @Test
  public void testRemoteQueryToopFreedonia () throws IOException {
    final MappedValueList aMVL = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (CMockSMM.LOG_PREFIX,
                                                                                          CMockSMM.NS_TOOP,
                                                                                          CMockSMM.NS_FREEDONIA);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
    if (false)
      aMVL.forEach (x -> LOGGER.info (x.getSource ().getValue ()));
  }

  @Test
  public void testRemoteQueryFreedoniaToop () throws IOException {
    final MappedValueList aMVL = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (CMockSMM.LOG_PREFIX,
                                                                                          CMockSMM.NS_FREEDONIA,
                                                                                          CMockSMM.NS_TOOP);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
    if (false)
      aMVL.forEach (x -> LOGGER.info (x.getSource ().getValue ()));
  }

  @Test
  public void testRemoteQueryToopElonia () throws IOException {
    final MappedValueList aMVL = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (CMockSMM.LOG_PREFIX,
                                                                                          CMockSMM.NS_TOOP,
                                                                                          CMockSMM.NS_ELONIA);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
    if (false)
      aMVL.forEach (x -> LOGGER.info (x.getSource ().getValue ()));
  }

  @Test
  public void testRemoteQueryEloniaToop () throws IOException {
    final MappedValueList aMVL = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (CMockSMM.LOG_PREFIX,
                                                                                          CMockSMM.NS_ELONIA,
                                                                                          CMockSMM.NS_TOOP);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
    if (false)
      aMVL.forEach (x -> LOGGER.info (x.getSource ().getValue ()));
  }

  @Test
  public void testGetAllNamespaces () throws IOException {
    final ICommonsOrderedSet<String> aNSs = SMMConceptProviderGRLCRemote.remoteQueryAllNamespaces (CMockSMM.LOG_PREFIX);
    assertNotNull (aNSs);
    assertFalse (aNSs.isEmpty ());
    if (false)
      aNSs.forEach (LOGGER::info);
  }
}
