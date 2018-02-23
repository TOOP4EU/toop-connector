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

import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.string.StringHelper;

import eu.toop.mp.api.MPConfig;

public class SemanticMappingModule implements MappingModule {

  /**
   * The Log facility of this class.
   */
  private static final Logger LOG = LoggerFactory.getLogger (SemanticMappingModule.class);

  public void addTOOPConcepts (final List<DataElementRequestType> dataElements) {
    LOG.info ("Hi, you have called the addTOOPConcepts class...please stay tuned!");
    convertConcepts (dataElements, true);
  }

  public void addCountryConcepts (final List<DataElementRequestType> dataElements) {
    LOG.info ("Hi, you have called the addCountryConcepts class...please stay tuned!");
    convertConcepts (dataElements, false);
  }

  /**
   * Lots of the logic is the same when converting between TOOP <-> Country. So a
   * single method is convenient. The boolean indicates which route we need to
   * take.
   *
   * @param messageXml
   *          The message to convert.
   * @param fromTOOPtoCountry
   *          {@code true} when to convert from TOOP concepts to Country concepts,
   *          {@code false} when converting from Country concepts to TOOP
   *          concepts.
   * @return The converted (complemented) message.
   */
  public void convertConcepts (final List<DataElementRequestType> dataElements, final boolean fromTOOPtoCountry) {

    // for now we use fromTOOPtoCountry only as a test.
    /*
     * For now we just change some random field, but when finished it will call the
     * Semantic Mapping Service here and request the translation of a particular
     * concept.
     */
    final Client client = ClientBuilder.newClient ();

    final String sBaseURL = MPConfig.getSMMGRLCURL ();
    if (StringHelper.hasNoText (sBaseURL))
      throw new IllegalArgumentException ("SMM GRLC URL is missing!");

    final WebTarget target = client.target (sBaseURL).path ("api/JackJackie/toop-sparql/get-all-triples");

    LOG.info ("Sending request to: {}", target.getUri ());
    final Response r = target.request ().get ();
    LOG.info ("Response: {}", r);
    // You want this as JSON?
    final String value = r.readEntity (String.class);
    LOG.info ("Retrieved value {} from service.", value);

    if (!dataElements.isEmpty ()) {
      if (fromTOOPtoCountry) {
        dataElements.forEach ( (i) -> i.setDataConsumerConcept ("Modified!"));
      } else {
        dataElements.forEach ( (i) -> i.setToopConcept ("Modified!"));
      }
    }
  }

}
