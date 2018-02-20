package eu.toop.mp.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test class for class {@link R2D2Settings}.
 *
 * @author Philip Helger
 *
 */
public final class R2D2SettingsTest {
  @Test
  public void testBasic () {
    assertNotNull (R2D2Settings.getIdentifierFactory ());
  }
}
