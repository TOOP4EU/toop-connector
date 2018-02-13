package eu.toop.mp.r2d2client;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.SimpleIdentifierFactory;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.url.EsensURLProvider;
import com.helger.peppol.url.IPeppolURLProvider;

/**
 * This class contains global settings for the R2D2 client.
 *
 * @author Philip Helger, BRZ, AT
 */
@Immutable
public final class R2D2Settings
{
  private R2D2Settings ()
  {}

  /**
   * Get the PEPPOL Directory URL to be used.
   *
   * @param bProduction
   *        <code>true</code> for production system, <code>false</code> for test
   *        system.
   * @return A new URL and never <code>null</code>. Never ends with a "/".
   */
  @Nonnull
  public static String getPEPPOLDirectoryURL (final boolean bProduction)
  {
    // TODO use correct URLs
    return "http://directory.central.toop";
  }

  /**
   * Get the SML to be used.
   *
   * @param bProduction
   *        <code>true</code> for SML, <code>false</code> for SMK.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static ESML getSML (final boolean bProduction)
  {
    return bProduction ? ESML.DIGIT_PRODUCTION : ESML.DIGIT_TEST;
  }

  @Nonnull
  public static IIdentifierFactory getIdentifierFactory ()
  {
    return SimpleIdentifierFactory.INSTANCE;
  }

  @Nonnull
  public static IPeppolURLProvider getSMPUrlProvider ()
  {
    return EsensURLProvider.INSTANCE;
  }
}
