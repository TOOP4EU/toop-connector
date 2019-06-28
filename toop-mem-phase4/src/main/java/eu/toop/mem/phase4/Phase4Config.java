package eu.toop.mem.phase4;

import javax.annotation.Nonnull;
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
    return TCConfig.getConfigFile ().getAsBoolean ("toop.phase4.debughttp", false);
  }

  public static boolean isDebugIncoming ()
  {
    return GlobalDebug.isDebugMode ();
  }

  @Nonnull
  public static String getSendResponseFolderName ()
  {
    return TCConfig.getConfigFile ().getAsString ("toop.phase4.send.response.folder", "as4-responses");
  }
}
