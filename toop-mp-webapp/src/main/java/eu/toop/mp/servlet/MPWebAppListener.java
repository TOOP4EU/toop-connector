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
import com.helger.web.servlets.scope.WebScopeListener;

import eu.toop.mp.api.MPConfig;

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
  }

  @Override
  public void contextDestroyed (@Nonnull final ServletContextEvent aEvent) {
    s_aLogger.info ("MP WebApp shutdown");
    super.contextDestroyed (aEvent);
  }
}
