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
