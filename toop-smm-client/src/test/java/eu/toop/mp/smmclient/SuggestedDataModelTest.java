package eu.toop.mp.smmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.mp.smmclient.SuggestedDataModel.RequestValue;

public class SuggestedDataModelTest {
  
  private static final Logger LOG = LoggerFactory.getLogger (SuggestedDataModelTest.class);
  
  @Test
  public void testBasic () throws IOException {
    final SuggestedDataModel aSDM = new SuggestedDataModel ();
    aSDM.addConceptToBeMapped ("bla", "CompanyCode");
    final Map<RequestValue, String> aResponse = aSDM.performMapping ();
    assertNotNull (aResponse);
    assertEquals (1, aResponse.size ());

    final RequestValue aRV = aResponse.keySet ().iterator ().next ();
    assertEquals ("bla", aRV.getScheme ());
    assertEquals ("CompanyCode", aRV.getValue ());

    assertEquals ("http://toop.eu/organization#CompanyCode", aResponse.values ().iterator ().next ());
  }
}
