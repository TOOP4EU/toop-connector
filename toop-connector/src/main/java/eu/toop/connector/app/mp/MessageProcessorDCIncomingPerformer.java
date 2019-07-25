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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.id.factory.GlobalIDFactory;

import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The nested performer class that does the hard work in step 4/4.
 *
 * @author Philip Helger
 */
final class MessageProcessorDCIncomingPerformer implements IConcurrentPerformer <ToopResponseWithAttachments140>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MessageProcessorDCIncomingPerformer.class);

  public void runAsync (@Nonnull final ToopResponseWithAttachments140 aResponseWA) throws Exception
  {
    final TDETOOPResponseType aResponse = aResponseWA.getResponse ();
    final String sRequestID = aResponse.getDataRequestIdentifier () != null ? aResponse.getDataRequestIdentifier ()
                                                                                       .getValue ()
                                                                            : "temp-tc4-id-" +
                                                                              GlobalIDFactory.getNewIntID ();
    final String sLogPrefix = "[" + sRequestID + "] ";

    ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "Received DC Incoming Request (4/4)");

    // Pass to DC
    MPConfig.getToDC ().passResponseOnToDC (aResponseWA);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (sLogPrefix + "End of processing");
  }
}
