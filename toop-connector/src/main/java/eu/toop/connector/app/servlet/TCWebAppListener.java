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
package eu.toop.connector.app.servlet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import com.helger.web.servlets.scope.WebScopeListener;

import eu.toop.connector.app.TCInit;

/**
 * Global startup/shutdown listener for the whole web application. Extends from
 * {@link WebScopeListener} to ensure global scope is created and maintained.
 *
 * @author Philip Helger
 */
@WebListener
public class TCWebAppListener extends WebScopeListener
{
  @Override
  public void contextInitialized (@Nonnull final ServletContextEvent aEvent)
  {
    // Must be first
    super.contextInitialized (aEvent);

    TCInit.initGlobally (aEvent.getServletContext ());
  }

  @Override
  public void contextDestroyed (@Nonnull final ServletContextEvent aEvent)
  {
    TCInit.shutdownGlobally (aEvent.getServletContext ());

    // Must be last
    super.contextDestroyed (aEvent);
  }
}
