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

import eu.toop.commons.doctype.EToopDocumentType;

/**
 * Test class for class SMMConceptCache.
 *
 * @author Philip Helger
 */
public final class SMMConceptCacheTest {
  private static final String LOG_PREFIX = "[unit test] ";
  private static final String NS_TOOP = EToopDocumentType.DOCTYPE_REGISTERED_ORGANIZATION_REQUEST.getSharedToopSMMNamespace ();
  private static final String NS_ELONIA = "http://example.register.elo/elonia-business-register";
  private static final String NS_FREEDONIA = "http://example.register.fre/freedonia-business-register";

  @BeforeEach
  public void reset () {
    SMMConceptCache.clearCache ();
  }

  @Test
  public void testRemoteQueryToopFreedonia () throws IOException {
    final MappedValueList aMVL = SMMConceptCache.remoteQueryAllMappedValues (LOG_PREFIX, NS_TOOP, NS_FREEDONIA);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
  }

  @Test
  public void testRemoteQueryFreedoniaToop () throws IOException {
    final MappedValueList aMVL = SMMConceptCache.remoteQueryAllMappedValues (LOG_PREFIX, NS_FREEDONIA, NS_TOOP);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
  }

  @Test
  public void testRemoteQueryToopElonia () throws IOException {
    final MappedValueList aMVL = SMMConceptCache.remoteQueryAllMappedValues (LOG_PREFIX, NS_TOOP, NS_ELONIA);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
  }

  @Test
  public void testRemoteQueryEloniaToop () throws IOException {
    final MappedValueList aMVL = SMMConceptCache.remoteQueryAllMappedValues (LOG_PREFIX, NS_ELONIA, NS_TOOP);
    assertNotNull (aMVL);
    assertFalse (aMVL.isEmpty ());
  }
}