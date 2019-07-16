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
package eu.toop.connector.servlet;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.apache.kafka.clients.producer.ProducerConfig;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.id.factory.StringIDFromGlobalLongIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.IURLProtocol;
import com.helger.commons.url.URLProtocolRegistry;
import com.helger.web.servlets.scope.WebScopeListener;

import eu.toop.commons.exchange.ToopRequestWithAttachments140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.as4.IMessageExchangeSPI.IIncomingHandler;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MessageExchangeManager;
import eu.toop.connector.app.CTC;
import eu.toop.connector.app.mp.MessageProcessorDCIncoming;
import eu.toop.connector.app.mp.MessageProcessorDPIncoming;
import eu.toop.kafkaclient.ToopKafkaClient;
import eu.toop.kafkaclient.ToopKafkaSettings;

/**
 * Global startup/shutdown listener for the whole web application. Extends from
 * {@link WebScopeListener} to ensure global scope is created and maintained.
 *
 * @author Philip Helger
 */
@WebListener
public class TCWebAppListener extends WebScopeListener
{
  private String m_sLogPrefix;

  @Override
  public void contextInitialized (@Nonnull final ServletContextEvent aEvent)
  {
    // Must be first
    super.contextInitialized (aEvent);

    GlobalIDFactory.setPersistentStringIDFactory (new StringIDFromGlobalLongIDFactory ("toop-mp-"));
    GlobalDebug.setDebugModeDirect (TCConfig.isGlobalDebug ());
    GlobalDebug.setProductionModeDirect (TCConfig.isGlobalProduction ());

    m_sLogPrefix = TCConfig.getToopInstanceName ();
    if (StringHelper.hasNoText (m_sLogPrefix))
    {
      // Get my IP address for debugging as default
      try
      {
        m_sLogPrefix = "[" + InetAddress.getLocalHost ().getHostAddress () + "] ";
      }
      catch (final UnknownHostException ex)
      {
        m_sLogPrefix = "";
      }
    }
    else
    {
      if (!m_sLogPrefix.startsWith ("["))
        m_sLogPrefix = "[" + m_sLogPrefix + "]";

      // Would have been trimmed when reading the properties file, so add
      // manually
      m_sLogPrefix += " ";
    }

    {
      // Init tracker client
      ToopKafkaSettings.setKafkaEnabled (TCConfig.isToopTrackerEnabled ());
      if (TCConfig.isToopTrackerEnabled ())
      {
        final String sToopTrackerUrl = TCConfig.getToopTrackerUrl ();
        if (StringHelper.hasNoText (sToopTrackerUrl))
          throw new InitializationException ("If the tracker is enabled, the tracker URL MUST be provided in the configuration file!");

        // Consistency check - no protcol like "http://" or so may be present
        final IURLProtocol aProtocol = URLProtocolRegistry.getInstance ().getProtocol (sToopTrackerUrl);
        if (aProtocol != null)
          throw new InitializationException ("The tracker URL MUST NOT start with a protocol like '" +
                                             aProtocol.getProtocol () +
                                             "'!");
        ToopKafkaSettings.defaultProperties ().put (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sToopTrackerUrl);

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
                          () -> m_sLogPrefix + "TOOP Connector WebApp " + CTC.getVersionNumber () + " startup");

    // Init incoming message handler
    MessageExchangeManager.getConfiguredImplementation ()
                          .registerIncomingHandler (aEvent.getServletContext (), new IIncomingHandler ()
                          {
                            public void handleIncomingRequest (@Nonnull final ToopRequestWithAttachments140 aRequest) throws MEException
                            {
                              ToopKafkaClient.send (EErrorLevel.INFO,
                                                    () -> "TC got DP incoming MEM request (2/4) with " +
                                                          aRequest.attachments ().size () +
                                                          " attachments");
                              MessageProcessorDPIncoming.getInstance ().enqueue (aRequest);
                            }

                            public void handleIncomingResponse (@Nonnull final ToopResponseWithAttachments140 aResponse) throws MEException
                            {
                              ToopKafkaClient.send (EErrorLevel.INFO,
                                                    () -> "TC got DC incoming MEM request (4/4) with " +
                                                          aResponse.attachments ().size () +
                                                          " attachments");
                              MessageProcessorDCIncoming.getInstance ().enqueue (aResponse);
                            }
                          });

    ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "TOOP Connector started");
  }

  @Override
  public void contextDestroyed (@Nonnull final ServletContextEvent aEvent)
  {
    ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "TOOP Connector shutting down");

    // Shutdown message exchange
    MessageExchangeManager.getConfiguredImplementation ().shutdown (aEvent.getServletContext ());

    // Shutdown tracker
    ToopKafkaClient.close ();

    // Must be last
    super.contextDestroyed (aEvent);
  }
}
