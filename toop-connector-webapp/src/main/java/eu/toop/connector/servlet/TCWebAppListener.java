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
package eu.toop.connector.servlet;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.id.factory.StringIDFromGlobalLongIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.web.servlets.scope.WebScopeListener;

import eu.toop.commons.dataexchange.TDETOOPRequestType;
import eu.toop.commons.dataexchange.TDETOOPResponseType;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.app.CTC;
import eu.toop.connector.me.MEMDelegate;
import eu.toop.connector.me.MEPayload;
import eu.toop.connector.mp.MessageProcessorDCIncoming;
import eu.toop.connector.mp.MessageProcessorDPIncoming;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * Global startup/shutdown listener for the whole web application. Extends from
 * {@link WebScopeListener} to ensure global scope is created and maintained.
 *
 * @author Philip Helger
 */
@WebListener
public class TCWebAppListener extends WebScopeListener {
  private String m_sLogPrefix;

  @Override
  public void contextInitialized (@Nonnull final ServletContextEvent aEvent) {
    super.contextInitialized (aEvent);

    GlobalIDFactory.setPersistentStringIDFactory (new StringIDFromGlobalLongIDFactory ("toop-mp-"));
    GlobalDebug.setDebugModeDirect (TCConfig.isGlobalDebug ());
    GlobalDebug.setProductionModeDirect (TCConfig.isGlobalProduction ());

    m_sLogPrefix = TCConfig.getToopInstanceName ();
    if (StringHelper.hasNoText (m_sLogPrefix)) {
      // Get my IP address for debugging as default
      try {
        m_sLogPrefix = "[" + InetAddress.getLocalHost ().getHostAddress () + "] ";
      } catch (final UnknownHostException ex) {
        m_sLogPrefix = "";
      }
    } else {
      if (!m_sLogPrefix.startsWith ("["))
        m_sLogPrefix = "[" + m_sLogPrefix + "]";

      // Would have been trimmed when reading the properties file, so add manually
      m_sLogPrefix += " ";
    }

    {
      // Init tracker client
      ToopKafkaClient.setKafkaEnabled (TCConfig.isToopTrackerEnabled ());
      final String sToopTrackerUrl = TCConfig.getToopTrackerUrl ();
      if (StringHelper.hasNoText (sToopTrackerUrl))
        throw new InitializationException ("If the tracker is enabled, the tracker URL MUST be provided in the configuration file!");
      ToopKafkaClient.defaultProperties ().put ("bootstrap.servers", sToopTrackerUrl);
    }

    {
      // Check R2D2 configuration
      final String sDirectoryURL = TCConfig.getR2D2DirectoryBaseUrl ();
      if (StringHelper.hasNoText (sDirectoryURL))
        throw new InitializationException ("The URL of the TOOP Directory is missing in the configuration file!");

      if (!TCConfig.isR2D2UseDNS ()) {
        final URI aSMPURI = TCConfig.getR2D2SMPUrl ();
        if (aSMPURI == null)
          throw new InitializationException ("Since the usage of SML/DNS is disabled, the fixed URL of the SMP to be used must be provided in the configuration file!");
      }
    }

    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> m_sLogPrefix + "TOOP Connector WebApp " + CTC.getVersionNumber () + " startup");

    // Register the AS4 handler needed
    MEMDelegate.getInstance ().registerMessageHandler (aMEMessage -> {
      // Always use response, because it is the super set of request and response
      final MEPayload aPayload = aMEMessage.head ();
      if (aPayload != null) {
        // Extract from ASiC
        final Object aMsg = ToopMessageBuilder.parseRequestOrResponse (aPayload.getDataInputStream ());

        if (aMsg instanceof TDETOOPResponseType) {
          // This is the way from DP back to DC; we're in DC incoming mode
          ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "TC got DC incoming request (4/4)");
          MessageProcessorDCIncoming.getInstance ().enqueue ((TDETOOPResponseType) aMsg);
        } else if (aMsg instanceof TDETOOPRequestType) {
          // This is the way from DC to DP; we're in DP incoming mode
          ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "TC got DP incoming request (2/4)");
          MessageProcessorDPIncoming.getInstance ().enqueue ((TDETOOPRequestType) aMsg);
        } else
          ToopKafkaClient.send (EErrorLevel.ERROR, () -> m_sLogPrefix + "Unsuspported Message: " + aMsg);
      } else
        ToopKafkaClient.send (EErrorLevel.WARN, () -> m_sLogPrefix + "MEMessage contains no payload: " + aMEMessage);
    });

    MEMDelegate.getInstance ().registerNotificationHandler (aRelayResult -> {
      // more to come
      ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "Notification[" + aRelayResult.getErrorCode ()
                                                    + "]: " + aRelayResult.getDescription ());
    });

    MEMDelegate.getInstance ().registerSubmissionResultHandler (aRelayResult -> {
      // more to come
      ToopKafkaClient.send (EErrorLevel.INFO, () -> m_sLogPrefix + "SubmissionResult[" + aRelayResult.getErrorCode ()
                                                    + "]: " + aRelayResult.getDescription ());
    });

    ToopKafkaClient.send (EErrorLevel.INFO, m_sLogPrefix + "TOOP Connector started");
  }

  @Override
  public void contextDestroyed (@Nonnull final ServletContextEvent aEvent) {
    ToopKafkaClient.send (EErrorLevel.INFO, m_sLogPrefix + "TOOP Connector shutting down");

    // Shutdown tracker
    ToopKafkaClient.close ();

    super.contextDestroyed (aEvent);
  }
}
