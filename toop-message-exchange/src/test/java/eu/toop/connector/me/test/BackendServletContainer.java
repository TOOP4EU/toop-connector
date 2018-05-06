package eu.toop.connector.me.test;

import eu.toop.connector.me.servlet.AS4InterfaceServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * @author yerlibilgin
 */
public class BackendServletContainer {
  public static void init(final int port) {
    new Thread(() -> {
      try {
        Server server = new Server(port);

        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        ServletHolder servletHolder = servletHandler.addServletWithMapping(AS4InterfaceServlet.class, "/backend");

        servletHolder.getServlet();

        server.start();
        server.join();
      } catch (Exception ex) {
        throw new IllegalStateException(ex.getMessage(), ex);
      }

    }).start();
  }

  public static void stop() {

  }
}
