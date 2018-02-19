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

  private final Module m;
  private final String exampleXml;

  public SMMTest() {
    m = new SemanticMappingModule();
    exampleXml = StreamHelper.getAllBytesAsString(new ClassPathResource(EXAMPLE), StandardCharsets.UTF_8);
  }

  @Test
  void testAddTOOPConcepts() {
    final String result = m.addCountryConcepts(this.exampleXml);
    LOG.info(result);
    assertNotNull(result);
    assertTrue(!result.isEmpty());
  }

  @Test
  void testAddCountryConcepts() {
    final String result = m.addTOOPConcepts(this.exampleXml);

    LOG.info(result);

    assertNotNull(result);
    assertTrue(!result.isEmpty());
  }
}
