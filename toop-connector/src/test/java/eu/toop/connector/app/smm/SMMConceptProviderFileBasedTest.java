package eu.toop.connector.app.smm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import eu.toop.commons.usecase.SMMDocumentTypeMapping;
import eu.toop.connector.api.smm.MappedValueList;

/**
 * Test class for class {@link SMMConceptProviderFileBased}.
 *
 * @author Philip Helger
 */
public class SMMConceptProviderFileBasedTest
{
  @Test
  public void testBasic ()
  {
    final SMMConceptProviderFileBased cp = new SMMConceptProviderFileBased ();
    final MappedValueList x = cp.getAllMappedValues ("",
                                                     SMMDocumentTypeMapping.SMM_DOMAIN_REGISTERED_ORGANIZATION,
                                                     CMockSMM.NS_FREEDONIA);
    final MappedValueList y = cp.getAllMappedValues ("",
                                                     CMockSMM.NS_FREEDONIA,
                                                     SMMDocumentTypeMapping.SMM_DOMAIN_REGISTERED_ORGANIZATION);
    assertNotNull (x);
    assertNotNull (y);
    assertEquals (x.size (), y.size ());
    assertNotEquals (x, y);

    assertEquals (1, x.getAllBySource (c -> c.getValue ().equals ("CompanyCode")).size ());
    assertEquals (0, x.getAllBySource (c -> c.getValue ().equals ("FreedoniaCompanyCode")).size ());
    assertEquals (0, x.getAllByDestination (c -> c.getValue ().equals ("CompanyCode")).size ());
    assertEquals (1, x.getAllByDestination (c -> c.getValue ().equals ("FreedoniaCompanyCode")).size ());

    assertEquals (0, y.getAllBySource (c -> c.getValue ().equals ("CompanyCode")).size ());
    assertEquals (1, y.getAllBySource (c -> c.getValue ().equals ("FreedoniaCompanyCode")).size ());
    assertEquals (1, y.getAllByDestination (c -> c.getValue ().equals ("CompanyCode")).size ());
    assertEquals (0, y.getAllByDestination (c -> c.getValue ().equals ("FreedoniaCompanyCode")).size ());
  }
}
