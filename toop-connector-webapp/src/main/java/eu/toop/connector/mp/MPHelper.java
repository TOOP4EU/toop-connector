package eu.toop.connector.mp;

import javax.annotation.Nonnull;

import eu.toop.commons.codelist.EPredefinedParticipantIdentifierScheme;
import eu.toop.commons.dataexchange.v140.TDEAddressType;
import eu.toop.commons.dataexchange.v140.TDEDataProviderType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.jaxb.ToopXSDHelper140;

public final class MPHelper
{
  private MPHelper ()
  {}

  public static void fillDefaultResponseFields (@Nonnull final TDETOOPResponseType aResponse)
  {
    // Hard coded value
    aResponse.setSpecificationIdentifier (ToopXSDHelper140.createSpecificationIdentifierResponse ());

    // Required for response
    // TODO would be good in configuration file
    aResponse.getRoutingInformation ()
             .setDataProviderElectronicAddressIdentifier (ToopXSDHelper140.createIdentifier ("error@toop-connector.toop.eu"));

    {
      final TDEDataProviderType p = new TDEDataProviderType ();
      // TODO would be good in configuration file
      p.setDPIdentifier (ToopXSDHelper140.createIdentifier ("demo-agency",
                                                            EPredefinedParticipantIdentifierScheme.EU_NAL.getID (),
                                                            "0000000000"));
      // TODO would be good in configuration file
      p.setDPName (ToopXSDHelper140.createText ("Error@ToopConnector"));
      final TDEAddressType pa = new TDEAddressType ();
      // DataProviderCountryCode is mandatory
      pa.setCountryCode (ToopXSDHelper140.createCodeWithLOA (aResponse.getRoutingInformation ()
                                                                      .getDataProviderCountryCode ()
                                                                      .getValue ()));
      p.setDPLegalAddress (pa);
      aResponse.addDataProvider (p);
    }
  }
}
