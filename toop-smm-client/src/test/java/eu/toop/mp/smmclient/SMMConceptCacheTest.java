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
