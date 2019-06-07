/**
 * Copyright (C) 2018-2019 toop.eu
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
package eu.toop.connector.mp;

import javax.annotation.Nonnull;

import com.helger.commons.error.level.EErrorLevel;

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.codelist.EPredefinedParticipantIdentifierScheme;
import eu.toop.commons.dataexchange.v140.TDEAddressType;
import eu.toop.commons.dataexchange.v140.TDEDataProviderType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.jaxb.ToopXSDHelper140;
import eu.toop.commons.usecase.ReverseDocumentTypeMapping;
import eu.toop.kafkaclient.ToopKafkaClient;
import oasis.names.specification.ubl.schema.xsd.unqualifieddatatypes_21.IdentifierType;

public final class MPHelper
{
  private MPHelper ()
  {}

  public static void fillDefaultResponseFields (@Nonnull final String sLogPrefix,
                                                @Nonnull final TDETOOPResponseType aResponse)
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

    // Document type must be switch from request to response
    {
      final IdentifierType aDocTypeID = aResponse.getRoutingInformation ().getDocumentTypeIdentifier ();
      final EPredefinedDocumentTypeIdentifier eRequestDocType = EPredefinedDocumentTypeIdentifier.getFromDocumentTypeIdentifierOrNull (aDocTypeID.getSchemeID (),
                                                                                                                                       aDocTypeID.getValue ());
      if (eRequestDocType != null)
      {
        final EPredefinedDocumentTypeIdentifier eResponseDocType = ReverseDocumentTypeMapping.getReverseDocumentTypeOrNull (eRequestDocType);
        if (eResponseDocType == null)
        {
          // Found no reverse document type
          ToopKafkaClient.send (EErrorLevel.ERROR,
                                () -> sLogPrefix +
                                      "Found no response document type for '" +
                                      aDocTypeID.getSchemeID () +
                                      "::" +
                                      aDocTypeID.getValue () +
                                      "'");
        }
        else
        {
          // Set new doc type in response
          ToopKafkaClient.send (EErrorLevel.INFO,
                                () -> sLogPrefix +
                                      "Switching document type '" +
                                      eRequestDocType.getURIEncoded () +
                                      "' to '" +
                                      eResponseDocType.getURIEncoded () +
                                      "'");
          aResponse.getRoutingInformation ()
                   .setDocumentTypeIdentifier (ToopXSDHelper140.createIdentifier (eResponseDocType.getScheme (),
                                                                                  eResponseDocType.getID ()));
        }
      }
    }
  }
}
