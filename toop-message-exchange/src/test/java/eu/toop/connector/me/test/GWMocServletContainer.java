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
