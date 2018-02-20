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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.StreamHelper;

public final class SMMTest {
  private static final Logger LOG = LoggerFactory.getLogger(SMMTest.class);
  private static final String EXAMPLE = "/datarequest.xml";

  private final Module m = new SemanticMappingModule();
  private final String exampleXml = StreamHelper.getAllBytesAsString(new ClassPathResource(EXAMPLE),
      StandardCharsets.UTF_8);

  @Test
  public void testAddTOOPConcepts() {
    final String result = m.addCountryConcepts(this.exampleXml);
    LOG.info(result);
    assertNotNull(result);
    assertTrue(!result.isEmpty());
  }

  @Test
  public void testAddCountryConcepts() {
    final String result = m.addTOOPConcepts(this.exampleXml);

    LOG.info(result);

    assertNotNull(result);
    assertTrue(!result.isEmpty());
  }
}
