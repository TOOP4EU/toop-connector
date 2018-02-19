package eu.toop.mp.servlet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global startup/shutdown listener for the whole web application
 * 
 * @author Philip Helger
 *
 */
@WebListener
public final class MPWebAppListener implements ServletContextListener {
	private static final Logger s_aLogger = LoggerFactory.getLogger(MPWebAppListener.class);

	public void contextInitialized(@Nonnull final ServletContextEvent aSce) {
		s_aLogger.info("MP WebApp startup");
	}

	public void contextDestroyed(@Nonnull final ServletContextEvent aSce) {
		s_aLogger.info("MP WebApp shutdown");
	}
}
