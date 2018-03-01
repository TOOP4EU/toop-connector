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
package eu.toop.mp.servlet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.id.factory.StringIDFromGlobalLongIDFactory;
import com.helger.commons.string.StringHelper;
import com.helger.web.servlets.scope.WebScopeListener;

import eu.toop.commons.dataexchange.TDETOOPDataRequestType;
import eu.toop.commons.dataexchange.TDETOOPDataResponseType;
import eu.toop.commons.exchange.ToopMessageBuilder;
import eu.toop.kafkaclient.ToopKafkaClient;
import eu.toop.mp.api.MPConfig;
import eu.toop.mp.me.MEMDelegate;
import eu.toop.mp.me.MEPayload;
import eu.toop.mp.processor.MessageProcessorDCIncoming;
import eu.toop.mp.processor.MessageProcessorDPIncoming;

/**
 * Global startup/shutdown listener for the whole web application. Extends from
 * {@link WebScopeListener} to ensure global scope is created and maintained.
 *
 * @author Philip Helger
 */
@WebListener
public class MPWebAppListener extends WebScopeListener {
  private static final Logger s_aLogger = LoggerFactory.getLogger (MPWebAppListener.class);

  @Override
  public void contextInitialized (@Nonnull final ServletContextEvent aEvent) {
    super.contextInitialized (aEvent);
    s_aLogger.info ("MP WebApp startup");

    GlobalIDFactory.setPersistentStringIDFactory (new StringIDFromGlobalLongIDFactory ("toop-mp-"));
    GlobalDebug.setDebugModeDirect (MPConfig.isGlobalDebug ());
    GlobalDebug.setProductionModeDirect (MPConfig.isGlobalProduction ());

    {
      // Init tracker client
      ToopKafkaClient.setEnabled (MPConfig.isToopTrackerEnabled ());
      final String sToopTrackerUrl = MPConfig.getToopTrackerUrl ();
      if (StringHelper.hasText (sToopTrackerUrl))
        ToopKafkaClient.defaultProperties ().put ("bootstrap.servers", sToopTrackerUrl);
    }

    // Register the handler need
    MEMDelegate.getInstance ().registerMessageHandler (aMEMessage -> {
      // Always use response, because it is the super set of request and response
      final MEPayload aPayload = aMEMessage.head ();
      if (aPayload != null) {
        // Extract from ASiC
        final Object aMsg = ToopMessageBuilder.parseRequestOrResponse (aPayload.getDataInputStream ());

        if (aMsg instanceof TDETOOPDataRequestType) {
          // This is the way from DC to DP; we're in DP incoming mode
          MessageProcessorDPIncoming.getInstance ().enqueue ((TDETOOPDataRequestType) aMsg);
        } else if (aMsg instanceof TDETOOPDataResponseType) {
          // This is the way from DP back to DC; we're in DC incoming mode
          MessageProcessorDCIncoming.getInstance ().enqueue ((TDETOOPDataResponseType) aMsg);
        } else
          s_aLogger.error ("Unsuspported ToopResponseMessage: " + aMsg);
      } else
        s_aLogger.warn ("MEMessage contains no payload: " + aMEMessage);
    });
  }

  @Override
  public void contextDestroyed (@Nonnull final ServletContextEvent aEvent) {
    s_aLogger.info ("MP WebApp shutdown");

    // Shutdown tracker
    ToopKafkaClient.close ();

    super.contextDestroyed (aEvent);
  }
}
