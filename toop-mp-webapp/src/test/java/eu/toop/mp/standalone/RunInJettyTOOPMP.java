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
package eu.toop.mp.standalone;

import javax.annotation.concurrent.Immutable;

import com.helger.photon.jetty.JettyStarter;

/**
 * Run as a standalone web application in Jetty on port 8080.<br>
 * http://localhost:8080/
 *
 * @author Philip Helger
 */
@Immutable
public final class RunInJettyTOOPMP
{
  public static void main (final String [] args) throws Exception
  {
    JettyStarter js = new JettyStarter (RunInJettyTOOPMP.class).setAllowAnnotationBasedConfig(false);
      System.out.println("resource: "+js.getWebXmlResource());
      //js.setWebXmlResource("/WEB-INF/web.xml");
      System.out.println("resource: "+js.getWebXmlResource());
      js.run();
  }
}
