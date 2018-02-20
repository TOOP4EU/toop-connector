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
package eu.toop.mp.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Test class for class {@link MPConfig}.
 *
 * @author Philip Helger
 *
 */
public final class MPConfigTest {
  @Test
  public void testBasic () {
    assertEquals ("http://directory.central.toop/pd", MPConfig.getR2D2DirectoryBaseUrl ());
    assertFalse (MPConfig.isR2D2UseDNS ());
  }
}
