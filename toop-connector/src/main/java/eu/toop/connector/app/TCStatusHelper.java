/**
 * Copyright (C) 2018-2019 toop.eu
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
package eu.toop.connector.app;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.system.SystemProperties;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.settings.ISettings;

import eu.toop.connector.api.TCConfig;

/**
 * Helper to create the TOOP Connector status reachable via the "/tc-status/"
 * servlet.
 *
 * @author Philip Helger
 */
@Immutable
public final class TCStatusHelper
{
  private TCStatusHelper ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static IJsonObject getDefaultStatusData ()
  {
    final ISettings aSettings = TCConfig.getConfigFile ().getSettings ();

    final IJsonObject aStatusData = new JsonObject ();
    aStatusData.add ("status.datetime", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aStatusData.add ("version.toop-connector", CTC.getVersionNumber ());
    aStatusData.add ("version.build-datetime", CTC.getBuildTimestamp ());
    aStatusData.add ("version.java", SystemProperties.getJavaVersion ());
    aStatusData.add ("global.debug", GlobalDebug.isDebugMode ());
    aStatusData.add ("global.production", GlobalDebug.isProductionMode ());

    // Add all entries except the password entries
    for (final Map.Entry <String, Object> aEntry : aSettings.entrySet ())
    {
      final String sKey = aEntry.getKey ();
      if (!sKey.contains ("password"))
        aStatusData.add (sKey, aEntry.getValue ());
    }

    return aStatusData;
  }

}
