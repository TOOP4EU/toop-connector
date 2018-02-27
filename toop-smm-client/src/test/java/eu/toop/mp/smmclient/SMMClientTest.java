package eu.toop.mp.smmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Test class for class {@link SMMClient}.
 *
 * @author Philip Helger
 */
public final class SMMClientTest {
  private static final String NS_TOOP = "http://toop.eu/organization";
  private static final String NS_FREEDONIA = "http://example.register.fre/freedonia-business-register";
  private static final ConceptValue CONCEPT_TOOP_1 = new ConceptValue (NS_TOOP, "CompanyCode");
  private static final ConceptValue CONCEPT_FR_1 = new ConceptValue (NS_FREEDONIA, "FreedoniaBusinessCode");

  @Test
  public void testEmpty () throws IOException {
    final SMMClient aClient = new SMMClient ();
    final IMappedValueList ret = aClient.performMapping (NS_FREEDONIA);
    assertNotNull (ret);
    assertTrue (ret.isEmpty ());
    assertEquals (0, ret.size ());
  }

  @Test
  public void testOneMatch () throws IOException {
    final SMMClient aClient = new SMMClient ();
    aClient.addConceptToBeMapped (CONCEPT_TOOP_1);
    final IMappedValueList ret = aClient.performMapping (NS_FREEDONIA);
    assertNotNull (ret);
    assertEquals (1, ret.size ());

    // Check concept has a 1:1 mapping
    final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_TOOP_1));
    assertNotNull (aMapped);
    assertEquals (1, aMapped.size ());
    final MappedValue aValue = aMapped.getFirst ();
    assertNotNull (aValue);
    assertEquals (CONCEPT_TOOP_1, aValue.getSource ());
    assertEquals (CONCEPT_FR_1, aValue.getDestination ());
  }

  @Test
  public void testOneMatchOneNotFound () throws IOException {
    final SMMClient aClient = new SMMClient ();
    aClient.addConceptToBeMapped (CONCEPT_TOOP_1);
    aClient.addConceptToBeMapped (new ConceptValue (NS_TOOP, "NonExistingField"));
    final IMappedValueList ret = aClient.performMapping (NS_FREEDONIA);
    assertNotNull (ret);
    assertEquals (1, ret.size ());

    // Check concept has a 1:1 mapping
    final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_TOOP_1));
    assertNotNull (aMapped);
    assertEquals (1, aMapped.size ());
    final MappedValue aValue = aMapped.getFirst ();
    assertNotNull (aValue);
    assertEquals (CONCEPT_TOOP_1, aValue.getSource ());
    assertEquals (CONCEPT_FR_1, aValue.getDestination ());
  }

  @Test
  public void testNoMappingNeeded () throws IOException {
    final SMMClient aClient = new SMMClient ();
    aClient.addConceptToBeMapped (CONCEPT_FR_1);
    final IMappedValueList ret = aClient.performMapping (NS_FREEDONIA);
    assertNotNull (ret);
    assertEquals (1, ret.size ());

    // Check concept has a 1:1 mapping
    final IMappedValueList aMapped = ret.getAllBySource (x -> x.equals (CONCEPT_FR_1));
    assertNotNull (aMapped);
    assertEquals (1, aMapped.size ());
    final MappedValue aValue = aMapped.getFirst ();
    assertNotNull (aValue);
    assertEquals (CONCEPT_FR_1, aValue.getSource ());
    assertEquals (CONCEPT_FR_1, aValue.getDestination ());
  }
}
