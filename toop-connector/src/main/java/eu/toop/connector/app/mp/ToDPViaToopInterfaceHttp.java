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
package eu.toop.connector.app.mp;

import javax.annotation.Nonnull;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;

import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.state.ESuccess;
import com.helger.httpclient.HttpClientManager;

import eu.toop.commons.dataexchange.v140.TDETOOPRequestType;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.http.TCHttpClientFactory;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * Implementation of {@link IToDP} using the HTTP based message passing to the
 * Toop-Interface URL
 *
 * @author Philip Helger
 */
public class ToDPViaToopInterfaceHttp implements IToDP
{
  @Nonnull
  public ESuccess passOnToDP (@Nonnull final TDETOOPRequestType aRequest)
  {
    // Send to DP (see ToDPServlet in toop-interface)
    final String sDestinationUrl = TCConfig.getMPToopInterfaceDPUrl ();

    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      ToopMessageBuilder140.createRequestMessageAsic (aRequest, aBAOS, MPConfig.getSignatureHelper (), null);

      // Forward to the DP at /to-dp interface
      final TCHttpClientFactory aHCFactory = new TCHttpClientFactory ();
      try (final HttpClientManager aMgr = new HttpClientManager (aHCFactory))
      {
        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Posting signed ASiC request to " + sDestinationUrl);

        final HttpPost aHttpPost = new HttpPost (sDestinationUrl);
        aHttpPost.setEntity (new InputStreamEntity (aBAOS.getAsInputStream ()));
        try (final CloseableHttpResponse aHttpResponse = aMgr.execute (aHttpPost))
        {
          EntityUtils.consume (aHttpResponse.getEntity ());
        }

        ToopKafkaClient.send (EErrorLevel.INFO, () -> "Done posting signed ASiC request to " + sDestinationUrl);
        return ESuccess.SUCCESS;
      }
    }
    catch (final Exception ex)
    {
      ToopKafkaClient.send (EErrorLevel.ERROR,
                            () -> "Error posting signed ASiC request to '" + sDestinationUrl + "'",
                            ex);
      return ESuccess.FAILURE;
    }
  }
}
