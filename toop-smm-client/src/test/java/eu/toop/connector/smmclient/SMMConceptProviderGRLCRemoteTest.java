/**
 * Copyright (C) 2018 toop.eu
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsOrderedSet;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.SMMDocumentTypeMapping;

/**
 * Test class for class SMMConceptCache.
 *
 * @author Philip Helger
 */
public final class SMMConceptProviderGRLCRemoteTest {
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMMConceptProviderGRLCRemoteTest.class);
  private static final String LOG_PREFIX = "[unit test] ";
  private static final String NS_TOOP = SMMDocumentTypeMapping.getToopSMNamespace (EPredefinedDocumentTypeIdentifier.REQUEST_REGISTEREDORGANIZATION);
  private static final String NS_ELONIA = "http://example.register.elo/elonia-business-register";
  private static final String NS_FREEDONIA = "http://example.register.fre/freedonia-business-register";

  @BeforeEach
  public void reset () {
    SMMConceptProviderGRLCRemote.clearCache ();
  }

  @Test
  public void testRemoteQueryToopFreedonia () throws IOException {
    final MappedValueList aMVL = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (LOG_PREFIX, NS_TOOP,
                                                                                          NS_FREEDONIA);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
    if (false)
      aMVL.forEach (x -> s_aLogger.info (x.getSource ().getValue ()));
  }

  @Test
  public void testRemoteQueryFreedoniaToop () throws IOException {
    final MappedValueList aMVL = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (LOG_PREFIX, NS_FREEDONIA,
                                                                                          NS_TOOP);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
    if (false)
      aMVL.forEach (x -> s_aLogger.info (x.getSource ().getValue ()));
  }

  @Test
  public void testRemoteQueryToopElonia () throws IOException {
    final MappedValueList aMVL = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (LOG_PREFIX, NS_TOOP,
                                                                                          NS_ELONIA);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
    if (false)
      aMVL.forEach (x -> s_aLogger.info (x.getSource ().getValue ()));
  }

  @Test
  public void testRemoteQueryEloniaToop () throws IOException {
    final MappedValueList aMVL = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (LOG_PREFIX, NS_ELONIA,
                                                                                          NS_TOOP);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
    if (false)
      aMVL.forEach (x -> s_aLogger.info (x.getSource ().getValue ()));
  }

  @Test
  public void testGetAllNamespaces () throws IOException {
    final ICommonsOrderedSet<String> aNSs = SMMConceptProviderGRLCRemote.remoteQueryAllNamespaces (LOG_PREFIX);
    assertNotNull (aNSs);
    assertFalse (aNSs.isEmpty ());
    if (false)
      aNSs.forEach (s_aLogger::info);
  }
}
