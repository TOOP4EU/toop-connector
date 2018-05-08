package eu.toop.connector.me.test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 * @author yerlibilgin
 */
public class GWMocServletContainer {

  private static Server server;

  public static void createServletOn(final int port, final String localPath) {
    new Thread(() -> {
      try {
        server = new Server(port);

        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        servletHandler.addServletWithMapping(SampleGWServlet.class, localPath);

        server.start();
        server.join();
      } catch (Exception ex) {
        throw new IllegalStateException(ex.getMessage(), ex);
      }

    }).start();
  }

  public static void stop() {
    try {
      server.stop();
    } catch (Exception ignored) {
    }
  }
}
