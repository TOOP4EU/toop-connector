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
package eu.toop.connector.app;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.servlet.ServletContext;

import org.apache.kafka.clients.producer.ProducerConfig;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.id.factory.StringIDFromGlobalLongIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.IURLProtocol;
import com.helger.commons.url.URLProtocolRegistry;

import eu.toop.commons.exchange.ToopRequestWithAttachments140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.as4.IMEIncomingHandler;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MessageExchangeManager;
import eu.toop.connector.app.mp.MPTrigger;
import eu.toop.kafkaclient.ToopKafkaClient;
import eu.toop.kafkaclient.ToopKafkaSettings;

/**
 * Contains TOOP Connector global init and shutdown methods.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class TCInit
{
  private static final AtomicBoolean INITED = new AtomicBoolean (false);
  private static String s_sLogPrefix;

  private TCInit ()
  {}

  /**
   * Globally init the TOOP Connector. Calling it, if it is already initialized
   * will thrown an exception.
   *
   * @param aServletContext
   *        The servlet context used for initialization. May not be
   *        <code>null</code> but maybe a mocked one.
   * @throws IllegalStateException
   *         If the TOOP Connector is already initialized
   * @throws InitializationException
   *         If any of the settings are totally bogus
   */
  public static void initGlobally (@Nonnull final ServletContext aServletContext)
  {
    if (!INITED.compareAndSet (false, true))
      throw new IllegalStateException ("TOOP Connector is already initialized");

    GlobalIDFactory.setPersistentStringIDFactory (new StringIDFromGlobalLongIDFactory ("toop-mp-"));
    GlobalDebug.setDebugModeDirect (TCConfig.isGlobalDebug ());
    GlobalDebug.setProductionModeDirect (TCConfig.isGlobalProduction ());

    String sLogPrefix = TCConfig.getToopInstanceName ();
    if (StringHelper.hasNoText (sLogPrefix))
    {
      // Get my IP address for debugging as default
      try
      {
        sLogPrefix = "[" + InetAddress.getLocalHost ().getHostAddress () + "] ";
      }
      catch (final UnknownHostException ex)
      {
        sLogPrefix = "";
      }
    }
    else
    {
      if (!sLogPrefix.startsWith ("["))
        sLogPrefix = "[" + sLogPrefix + "]";

      // Would have been trimmed when reading the properties file, so add
      // manually
      sLogPrefix += " ";
    }
    s_sLogPrefix = sLogPrefix;

    {
      // Init tracker client
      ToopKafkaSettings.setKafkaEnabled (TCConfig.isToopTrackerEnabled ());
      if (TCConfig.isToopTrackerEnabled ())
      {
        // Set tracker URL
        final String sToopTrackerUrl = TCConfig.getToopTrackerUrl ();
        if (StringHelper.hasNoText (sToopTrackerUrl))
          throw new InitializationException ("If the tracker is enabled, the tracker URL MUST be provided in the configuration file!");
        // Consistency check - no protocol like "http://" or so may be present
        final IURLProtocol aProtocol = URLProtocolRegistry.getInstance ().getProtocol (sToopTrackerUrl);
        if (aProtocol != null)
          throw new InitializationException ("The tracker URL MUST NOT start with a protocol like '" +
                                             aProtocol.getProtocol () +
                                             "'!");
        ToopKafkaSettings.defaultProperties ().put (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sToopTrackerUrl);

        // Set the topic
        final String sToopTrackerTopic = TCConfig.getToopTrackerTopic ();
        ToopKafkaSettings.setKafkaTopic (sToopTrackerTopic);
      }
    }

    {
      // Check R2D2 configuration
      final String sDirectoryURL = TCConfig.getR2D2DirectoryBaseUrl ();
      if (StringHelper.hasNoText (sDirectoryURL))
        throw new InitializationException ("The URL of the TOOP Directory is missing in the configuration file!");

      if (!TCConfig.isR2D2UseDNS ())
      {
        final URI aSMPURI = TCConfig.getR2D2SMPUrl ();
        if (aSMPURI == null)
          throw new InitializationException ("Since the usage of SML/DNS is disabled, the fixed URL of the SMP to be used must be provided in the configuration file!");
      }
    }

    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> s_sLogPrefix + "TOOP Connector WebApp " + CTC.getVersionNumber () + " startup");

    // Init incoming message handler
    MessageExchangeManager.getConfiguredImplementation ()
                          .registerIncomingHandler (aServletContext, new IMEIncomingHandler ()
                          {
                            public void handleIncomingRequest (@Nonnull final ToopRequestWithAttachments140 aRequest) throws MEException
                            {
                              ToopKafkaClient.send (EErrorLevel.INFO,
                                                    () -> s_sLogPrefix +
                                                          "TC got DP incoming MEM request (2/4) with " +
                                                          aRequest.attachments ().size () +
                                                          " attachments");
                              MPTrigger.incomingGatewayDP_2_of_4 (aRequest);
                            }

                            public void handleIncomingResponse (@Nonnull final ToopResponseWithAttachments140 aResponse) throws MEException
                            {
                              ToopKafkaClient.send (EErrorLevel.INFO,
                                                    () -> s_sLogPrefix +
                                                          "TC got DC incoming MEM request (4/4) with " +
                                                          aResponse.attachments ().size () +
                                                          " attachments");
                              MPTrigger.incomingGatewayDC_4_of_4 (aResponse);
                            }
                          });

    ToopKafkaClient.send (EErrorLevel.INFO, () -> s_sLogPrefix + "TOOP Connector started");
  }

  /**
   * @return <code>true</code> if the TOOP Connector was initialized (or is
   *         currently initializing or is currently shutdown),
   *         <code>false</code> if not.
   */
  public static boolean isInitialized ()
  {
    return INITED.get ();
  }

  /**
   * Globally shutdown the TOOP Connector. Calling it, if it was not already
   * initialized will thrown an exception.
   *
   * @param aServletContext
   *        The servlet context used for shutdown. May not be <code>null</code>
   *        but maybe a mocked one.
   * @throws IllegalStateException
   *         If the TOOP Connector is not initialized
   */
  public static void shutdownGlobally (@Nonnull final ServletContext aServletContext)
  {
    if (!isInitialized ())
      throw new IllegalStateException ("TOOP Connector is not initialized");

    ToopKafkaClient.send (EErrorLevel.INFO, () -> s_sLogPrefix + "TOOP Connector shutting down");

    // Shutdown message exchange
    MessageExchangeManager.getConfiguredImplementation ().shutdown (aServletContext);

    // Shutdown tracker
    ToopKafkaClient.close ();

    s_sLogPrefix = null;

    if (!INITED.compareAndSet (true, false))
      throw new IllegalStateException ("TOOP Connector is already shutdown");
  }
}
