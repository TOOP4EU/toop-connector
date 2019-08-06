/**
 * Copyright (C) 2019 toop.eu
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
package eu.toop.connector.mem.phase4;

import javax.annotation.Nullable;

import com.helger.commons.debug.GlobalDebug;

import eu.toop.connector.api.TCConfig;

/**
 * Wrapper to access the configuration for the phase4 module.
 *
 * @author Philip Helger
 */
public final class Phase4Config
{
  private Phase4Config ()
  {}

  @Nullable
  public static String getDataPath ()
  {
    return TCConfig.getConfigFile ().getAsString ("toop.phase4.datapath");
  }

  @Nullable
  public static String getFromPartyID ()
  {
    return TCConfig.getMEMAS4TcPartyid ();
  }

  public static boolean isHttpDebugEnabled ()
  {
    return TCConfig.getConfigFile ().getAsBoolean ("toop.phase4.debug.http", false);
  }

  public static boolean isDebugIncoming ()
  {
    return TCConfig.getConfigFile ().getAsBoolean ("toop.phase4.debug.incoming", GlobalDebug.isDebugMode ());
  }

  @Nullable
  public static String getSendResponseFolderName ()
  {
    // Can be relative or absolute
    return TCConfig.getConfigFile ().getAsString ("toop.phase4.send.response.folder");
  }
}
