package eu.toop.mp.servlet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.id.factory.StringIDFromGlobalLongIDFactory;
import com.helger.web.servlets.scope.WebScopeListener;

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
    GlobalIDFactory.setPersistentStringIDFactory (new StringIDFromGlobalLongIDFactory ("toop-mp-"));
    s_aLogger.info ("MP WebApp startup");
  }

  @Override
  public void contextDestroyed (@Nonnull final ServletContextEvent aEvent) {
    s_aLogger.info ("MP WebApp shutdown");
    super.contextDestroyed (aEvent);
  }
}
