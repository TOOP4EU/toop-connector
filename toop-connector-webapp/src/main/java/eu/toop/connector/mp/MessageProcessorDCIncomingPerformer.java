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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;

import com.helger.asic.SignatureHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.httpclient.HttpClientManager;

import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.exchange.AsicReadEntry;
import eu.toop.commons.exchange.AsicWriteEntry;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.http.TCHttpClientFactory;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The nested performer class that does the hard work in step 4/4.
 *
 * @author Philip Helger
 */
final class MessageProcessorDCIncomingPerformer implements IConcurrentPerformer <ToopResponseWithAttachments140>
{
  public void runAsync (@Nonnull final ToopResponseWithAttachments140 aResponseWA) throws Exception
  {
    final TDETOOPResponseType aResponse = aResponseWA.getResponse ();

    final String sRequestID = aResponse.getDataRequestIdentifier () != null ? aResponse.getDataRequestIdentifier ()
                                                                                       .getValue ()
                                                                            : "temp-tc4-id-" +
                                                                              GlobalIDFactory.getNewIntID ();
    final String sLogPrefix = "[" + sRequestID + "] ";
    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DC Incoming Request (4/4)");

    // Forward to the DC at /to-dc interface
    final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();

    // Send to DC (see ToDCServlet in toop-interface)
    final String sDestinationUrl = TCConfig.getMPToopInterfaceDCUrl ();

    try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory))
    {
      final SignatureHelper aSH = new SignatureHelper (TCConfig.getKeystoreType (),
                                                       TCConfig.getKeystorePath (),
                                                       TCConfig.getKeystorePassword (),
                                                       TCConfig.getKeystoreKeyAlias (),
                                                       TCConfig.getKeystoreKeyPassword ());

      try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        // Convert read to write attachments
        final ICommonsList <AsicWriteEntry> aWriteAttachments = new CommonsArrayList <> ();
        for (final AsicReadEntry aEntry : aResponseWA.attachments ())
          aWriteAttachments.add (AsicWriteEntry.create (aEntry));

        ToopMessageBuilder140.createResponseMessageAsic (aResponse, aBAOS, aSH, aWriteAttachments);

        ToopKafkaClient.send (EErrorLevel.INFO,
                              () -> "Start posting signed ASiC response to '" + sDestinationUrl + "'");

        final HttpPost aHttpPost = new HttpPost (sDestinationUrl);
        aHttpPost.setEntity (new InputStreamEntity (aBAOS.getAsInputStream ()));
        try (final CloseableHttpResponse aHttpResponse = aMgr.execute (aHttpPost))
        {
          EntityUtils.consume (aHttpResponse.getEntity ());
        }

        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Done posting signed ASiC response to '" + sDestinationUrl + "'");
      }
    }
    catch (final Exception ex)
    {
      ToopKafkaClient.send (EErrorLevel.ERROR,
                            () -> "Error posting signed ASiC response to '" + sDestinationUrl + "'",
                            ex);
    }
  }
}
