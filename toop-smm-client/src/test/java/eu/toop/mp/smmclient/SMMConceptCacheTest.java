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
package eu.toop.mp.smmclient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Test class for class SMMConceptCache.
 *
 * @author Philip Helger
 */
public final class SMMConceptCacheTest {
  @Test
  public void testRemoteQuery () throws IOException {
    // The only existing mapping we have atm
    final MappedValueList aMVL = SMMConceptCache.remoteQueryAllMappedValues ("http://toop.eu/organization",
                                                                             "http://example.register.fre/freedonia-business-register");
    assertNotNull (aMVL);
    System.out.println (aMVL.toString ());
  }
}
