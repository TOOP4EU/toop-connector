package eu.toop.mp.smmclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.toop.mp.smmclient.SuggestedDataModel.RequestValue;

public class SuggestedDataModelTest {
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

    assertEquals ("http://toop.tno.nl/organization#CompanyCode", aResponse.values ().iterator ().next ());
  }
}
