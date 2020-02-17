/**
 * Copyright (C) 2018-2020 toop.eu
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
package eu.toop.connector.app.mp;

import javax.annotation.Nonnull;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.state.ESuccess;
import com.helger.httpclient.HttpClientManager;

import eu.toop.commons.exchange.AsicReadEntry;
import eu.toop.commons.exchange.AsicWriteEntry;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.http.TCHttpClientSettings;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * Implementation of {@link IToDC} using the HTTP based message passing to the
 * Toop-Interface URL
 *
 * @author Philip Helger
 */
public class ToDCViaToopInterfaceHttp implements IToDC
{
  @Nonnull
  public ESuccess passResponseOnToDC (@Nonnull final ToopResponseWithAttachments140 aResponseWA)
  {
    // Send to DC (see ToDCServlet in toop-interface)
    final String sDestinationUrl = TCConfig.getMPToopInterfaceDCUrl ();

    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      // Convert read to write attachments
      final ICommonsList <AsicWriteEntry> aWriteAttachments = new CommonsArrayList <> ();
      for (final AsicReadEntry aEntry : aResponseWA.attachments ())
        aWriteAttachments.add (AsicWriteEntry.create (aEntry));

      // Create ASIC
      ToopMessageBuilder140.createResponseMessageAsic (aResponseWA.getResponse (),
                                                       aBAOS,
                                                       MPConfig.getSignatureHelper (),
                                                       aWriteAttachments);

      try (final HttpClientManager aMgr = HttpClientManager.create (new TCHttpClientSettings ()))
      {
        ToopKafkaClient.send (EErrorLevel.INFO,
                              () -> "Start posting signed ASiC response to '" + sDestinationUrl + "'");

        final HttpPost aHttpPost = new HttpPost (sDestinationUrl);
        aHttpPost.setEntity (new InputStreamEntity (aBAOS.getAsInputStream ()));
        try (final CloseableHttpResponse aHttpResponse = aMgr.execute (aHttpPost))
        {
          EntityUtils.consume (aHttpResponse.getEntity ());
        }

        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Done posting signed ASiC response to '" + sDestinationUrl + "'");
        return ESuccess.SUCCESS;
      }
    }
    catch (final Exception ex)
    {
      ToopKafkaClient.send (EErrorLevel.ERROR,
                            () -> "Error posting signed ASiC response to '" + sDestinationUrl + "'",
                            ex);
      return ESuccess.FAILURE;
    }
  }
}
