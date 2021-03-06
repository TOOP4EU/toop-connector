/**
 * Copyright (C) 2018-2020 toop.eu
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
package eu.toop.connector.api;

import java.io.File;
import java.net.URI;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.IKeyStoreType;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

import eu.toop.commons.codelist.EPredefinedParticipantIdentifierScheme;
import eu.toop.connector.api.as4.MessageExchangeManager;

/**
 * This class contains global configuration elements for the TOOP Connector.
 *
 * @author Philip Helger, BRZ, AT
 */
@ThreadSafe
public final class TCConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (TCConfig.class);
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static ConfigFile s_aConfigFile;

  static
  {
    reloadConfiguration ();
  }

  /**
   * The name of the primary system property which points to the
   * <code>toop-connector.properties</code> files
   */
  public static final String SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH = "toop.connector.server.properties.path";

  /**
   * The default primary properties file to load
   */
  public static final String PATH_PRIVATE_TOOP_CONNECTOR_PROPERTIES = "private-toop-connector.properties";
  /**
   * The default secondary properties file to load
   */
  public static final String PATH_TOOP_CONNECTOR_PROPERTIES = "toop-connector.properties";

  private static ISMLInfo s_aCachedSMLInfo;

  /**
   * Reload the configuration file. It checks if the environment variable
   * <code>TOOP_CONNECTOR_CONFIG</code> the system property
   * {@link #SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH} is present
   * and if so, tries it first, than
   * {@link #PATH_PRIVATE_TOOP_CONNECTOR_PROPERTIES} is checked and finally the
   * {@link #PATH_TOOP_CONNECTOR_PROPERTIES} path is checked.
   *
   * @return {@link ESuccess}
   */
  @Nonnull
  public static ESuccess reloadConfiguration ()
  {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromEnvVar ("TOOP_CONNECTOR_CONFIG")
                                                           .addPathFromSystemProperty (SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH)
                                                           .addPath (PATH_PRIVATE_TOOP_CONNECTOR_PROPERTIES)
                                                           .addPath (PATH_TOOP_CONNECTOR_PROPERTIES);

    return s_aRWLock.writeLocked ( () -> {
      s_aConfigFile = aCFB.build ();
      if (!s_aConfigFile.isRead ())
      {
        LOGGER.warn ("Failed to read TOOP Connector server properties from " + aCFB.getAllPaths ());
        return ESuccess.FAILURE;
      }

      // Clear cache
      s_aCachedSMLInfo = null;

      // Ensure dump directories are present if enabled
      if (isDebugFromDCDumpEnabled ())
      {
        FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (getDebugFromDCDumpPath ());
      }
      if (isDebugFromDPDumpEnabled ())
      {
        FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (getDebugFromDPDumpPath ());
      }

      LOGGER.info ("Read TOOP Connector server properties from " + s_aConfigFile.getReadResource ().getPath ());
      return ESuccess.SUCCESS;
    });
  }

  public static final boolean DEFAULT_TOOP_TRACKER_ENABLED = false;
  public static final String DEFAULT_TOOP_TRACKER_TOPIC = "toop";
  public static final boolean DEFAULT_USE_SML = true;

  @GuardedBy ("s_aRWLock")
  private static String s_sMPToopInterfaceDPOverrideUrl = null;
  @GuardedBy ("s_aRWLock")
  private static String s_sMPToopInterfaceDCOverrideUrl = null;

  private TCConfig ()
  {}

  /**
   * @return The configuration file. Never <code>null</code>.
   */
  @Nonnull
  public static ConfigFile getConfigFile ()
  {
    return s_aRWLock.readLocked ( () -> s_aConfigFile);
  }

  public static boolean isGlobalDebug ()
  {
    return getConfigFile ().getAsBoolean ("global.debug", GlobalDebug.isDebugMode ());
  }

  public static boolean isGlobalProduction ()
  {
    return getConfigFile ().getAsBoolean ("global.production", GlobalDebug.isProductionMode ());
  }

  /**
   * @return A debug name to identify an instance. If none is provided, the IP
   *         address is used.
   */
  @Nullable
  public static String getToopInstanceName ()
  {
    return getConfigFile ().getAsString ("toop.instancename");
  }

  public static boolean isToopTrackerEnabled ()
  {
    return getConfigFile ().getAsBoolean ("toop.tracker.enabled", DEFAULT_TOOP_TRACKER_ENABLED);
  }

  @Nullable
  public static String getToopTrackerUrl ()
  {
    return getConfigFile ().getAsString ("toop.tracker.url");
  }

  @Nullable
  public static String getToopTrackerTopic ()
  {
    return getConfigFile ().getAsString ("toop.tracker.topic", DEFAULT_TOOP_TRACKER_TOPIC);
  }

  // toop.smm.grlc.url was removed in 0.10.8

  /**
   * @return The destination namespace URI to map to (for DP incoming step 2/4)
   */
  @Nullable
  public static String getSMMMappingNamespaceURIForDP ()
  {
    return getConfigFile ().getAsString ("toop.smm.namespaceuri");
  }

  /**
   * @return <code>true</code> if an unmappable concept on DP side breaks the
   *         operation, <code>false</code> if not
   * @since 0.10.5
   */
  public static boolean isSMMDPMappingErrorFatal ()
  {
    // true for backwards compatibility
    return getConfigFile ().getAsBoolean ("toop.smm.dp.mapping.error.fatal", true);
  }

  /**
   * @return The TOOP Directory base URL for R2D2. Should never end with a
   *         slash.
   */
  @Nullable
  public static String getR2D2DirectoryBaseUrl ()
  {
    return getConfigFile ().getAsString ("toop.r2d2.directory.baseurl");
  }

  /**
   * @return <code>true</code> to use SML lookup, <code>false</code> to not do
   *         it.
   * @see #getR2D2SML()
   * @see #getR2D2SMPUrl()
   */
  public static boolean isR2D2UseDNS ()
  {
    return getConfigFile ().getAsBoolean ("toop.r2d2.usedns", DEFAULT_USE_SML);
  }

  /**
   * @return The SML URL to be used. Must only contain a value if
   *         {@link #isR2D2UseDNS()} returned <code>true</code>.
   */
  @Nonnull
  public static ISMLInfo getR2D2SML ()
  {
    ISMLInfo ret = s_aCachedSMLInfo;
    if (ret == null)
    {
      final String sSMLID = getConfigFile ().getAsString ("toop.r2d2.sml.id");
      final ESML eSML = ESML.getFromIDOrNull (sSMLID);
      if (eSML != null)
      {
        // Pre-configured SML it is
        ret = eSML;
      }
      else
      {
        // Custom SML
        final String sDisplayName = getConfigFile ().getAsString ("toop.r2d2.sml.name", "TOOP SML");
        // E.g. edelivery.tech.ec.europa.eu.
        final String sDNSZone = getConfigFile ().getAsString ("toop.r2d2.sml.dnszone");
        // E.g. https://edelivery.tech.ec.europa.eu/edelivery-sml
        final String sManagementServiceURL = getConfigFile ().getAsString ("toop.r2d2.sml.serviceurl");
        final boolean bClientCertificateRequired = getConfigFile ().getAsBoolean ("toop.r2d2.sml.clientcert", false);
        // No need for a persistent ID here
        ret = new SMLInfo (GlobalIDFactory.getNewStringID (),
                           sDisplayName,
                           sDNSZone,
                           sManagementServiceURL,
                           bClientCertificateRequired);
      }
      // Remember in cache
      s_aCachedSMLInfo = ret;
    }
    return ret;
  }

  /**
   * @return The constant SMP URI to be used. Must only contain a value if
   *         {@link #isR2D2UseDNS()} returned <code>false</code>.
   */
  @Nullable
  public static URI getR2D2SMPUrl ()
  {
    // E.g. http://smp.central.toop
    final String sURI = getConfigFile ().getAsString ("toop.r2d2.smp.url");
    return URLHelper.getAsURI (sURI);
  }

  /**
   * @return The MEM implementation ID or the default value. Never
   *         <code>null</code>.
   * @since 0.10.0
   */
  @Nonnull
  public static String getMEMImplementationID ()
  {
    return getConfigFile ().getAsString ("toop.mem.implementation", MessageExchangeManager.DEFAULT_ID);
  }

  /**
   * Get the overall protocol to be used. Depending on that output different
   * other properties might be queried.
   *
   * @return The overall protocol to use. Never <code>null</code>.
   */
  @Nonnull
  public static ETCProtocol getMEMProtocol ()
  {
    final String sID = getConfigFile ().getAsString ("toop.mem.protocol", ETCProtocol.DEFAULT.getID ());
    final ETCProtocol eProtocol = ETCProtocol.getFromIDOrNull (sID);
    if (eProtocol == null)
    {
      throw new IllegalStateException ("Failed to resolve protocol with ID '" + sID + "'");
    }
    return eProtocol;
  }

  // GW_URL
  @Nullable
  public static String getMEMAS4Endpoint ()
  {
    return getConfigFile ().getAsString ("toop.mem.as4.endpoint");
  }

  @Nullable
  public static String getMEMAS4GwPartyID ()
  {
    return getConfigFile ().getAsString ("toop.mem.as4.gw.partyid");
  }

  public static String getMEMAS4TcPartyid ()
  {
    return getConfigFile ().getAsString ("toop.mem.as4.tc.partyid");
  }

  public static long getGatewayNotificationWaitTimeout ()
  {
    return getConfigFile ().getAsLong ("toop.mem.as4.notificationWaitTimeout", 20000);
  }

  /**
   * @return <code>true</code> if Schematron validation is enabled,
   *         <code>false</code> if not. Default is true.
   * @since 0.10.0
   */
  public static boolean isMPSchematronValidationEnabled ()
  {
    return getConfigFile ().getAsBoolean ("toop.mp.schematron.enabled", true);
  }

  /**
   * Override the toop-interface DP URL with the custom URL. This URL has
   * precedence over the value in the configuration file.
   *
   * @param sMPToopInterfaceDPOverrideUrl
   *        The new override URL to set. May be <code>null</code>.
   * @since 0.10.6
   */
  public static void setMPToopInterfaceDPOverrideUrl (@Nullable final String sMPToopInterfaceDPOverrideUrl)
  {
    if (LOGGER.isWarnEnabled ())
      LOGGER.warn ("Overriding the MP Toop Interface DP URL with '" + sMPToopInterfaceDPOverrideUrl + "'");
    s_aRWLock.writeLocked ( () -> s_sMPToopInterfaceDPOverrideUrl = sMPToopInterfaceDPOverrideUrl);
  }

  /**
   * @return The override URL for the toop-interface DP side. May be
   *         <code>null</code>. Default is <code>null</code>.
   * @since 0.10.6
   */
  @Nullable
  public static String getMPToopInterfaceDPOverrideUrl ()
  {
    return s_aRWLock.readLocked ( () -> s_sMPToopInterfaceDPOverrideUrl);
  }

  /**
   * @return The URL of the DP backend for steps 2/4 and 3/4. May be
   *         <code>null</code>.
   * @see #getMPToopInterfaceDPOverrideUrl()
   * @see #setMPToopInterfaceDPOverrideUrl(String)
   */
  @Nullable
  public static String getMPToopInterfaceDPUrl ()
  {
    String ret = getMPToopInterfaceDPOverrideUrl ();
    if (StringHelper.hasNoText (ret))
      ret = getConfigFile ().getAsString ("toop.mp.dp.url");
    return ret;
  }

  /**
   * Override the toop-interface DC URL with the custom URL. This URL has
   * precedence over the value in the configuration file.
   *
   * @param sMPToopInterfaceDCOverrideUrl
   *        The new override URL to set. May be <code>null</code>.
   * @since 0.10.6
   */
  public static void setMPToopInterfaceDCOverrideUrl (@Nullable final String sMPToopInterfaceDCOverrideUrl)
  {
    if (LOGGER.isWarnEnabled ())
      LOGGER.warn ("Overriding the MP Toop Interface DC URL with '" + sMPToopInterfaceDCOverrideUrl + "'");
    s_aRWLock.writeLocked ( () -> s_sMPToopInterfaceDCOverrideUrl = sMPToopInterfaceDCOverrideUrl);
  }

  /**
   * @return The override URL for the toop-interface DC side. May be
   *         <code>null</code>. Default is <code>null</code>.
   * @since 0.10.6
   */
  @Nullable
  public static String getMPToopInterfaceDCOverrideUrl ()
  {
    return s_aRWLock.readLocked ( () -> s_sMPToopInterfaceDCOverrideUrl);
  }

  /**
   * @return The URL of the DC backend for step 4/4. May be <code>null</code>.
   * @see #getMPToopInterfaceDCOverrideUrl()
   * @see #setMPToopInterfaceDCOverrideUrl(String)
   */
  @Nullable
  public static String getMPToopInterfaceDCUrl ()
  {
    String ret = getMPToopInterfaceDCOverrideUrl ();
    if (StringHelper.hasNoText (ret))
      ret = getConfigFile ().getAsString ("toop.mp.dc.url");
    return ret;
  }

  /**
   * @return The value of automatically created responses element
   *         <code>RoutingInformation/DataProviderElectronicAddressIdentifier</code>
   * @since 0.10.5
   */
  @Nonnull
  public static String getMPAutoResponseDPAddressID ()
  {
    return getConfigFile ().getAsString ("toop.mp.autoresponse.dpaddressid", "error@toop-connector.toop.eu");
  }

  /**
   * @return The ID scheme for the DP in case of automatic responses
   * @since 0.10.5
   */
  @Nonnull
  public static String getMPAutoResponseDPIDScheme ()
  {
    return getConfigFile ().getAsString ("toop.mp.autoresponse.dpidscheme",
                                         EPredefinedParticipantIdentifierScheme.EU_NAL.getID ());
  }

  /**
   * @return The ID value for the DP in case of automatic responses
   * @since 0.10.5
   */
  @Nonnull
  public static String getMPAutoResponseDPIDValue ()
  {
    return getConfigFile ().getAsString ("toop.mp.autoresponse.dpidvalue", "0000000000");
  }

  /**
   * @return The name for the DP in case of automatic responses
   * @since 0.10.5
   */
  @Nonnull
  public static String getMPAutoResponseDPName ()
  {
    return getConfigFile ().getAsString ("toop.mp.autoresponse.dpname", "Error@ToopConnector");
  }

  @Nullable
  public static IKeyStoreType getKeystoreType ()
  {
    /** Configurable since 0.10.5 */
    final String sKeystoreType = getConfigFile ().getAsString ("toop.keystore.type");
    return EKeyStoreType.getFromIDCaseInsensitiveOrDefault (sKeystoreType, EKeyStoreType.JKS);
  }

  @Nullable
  public static String getKeystorePath ()
  {
    return getConfigFile ().getAsString ("toop.keystore.path");
  }

  @Nullable
  public static String getKeystorePassword ()
  {
    return getConfigFile ().getAsString ("toop.keystore.password");
  }

  @Nullable
  public static String getKeystoreKeyAlias ()
  {
    return getConfigFile ().getAsString ("toop.keystore.key.alias");
  }

  @Nullable
  public static String getKeystoreKeyPassword ()
  {
    return getConfigFile ().getAsString ("toop.keystore.key.password");
  }

  /**
   * @return <code>true</code> if the status servlet at <code>/tc-status/</code>
   *         is enabled, <code>false</code> if it is disabled. By default it is
   *         enabled.
   */
  public static boolean isStatusEnabled ()
  {
    return getConfigFile ().getAsBoolean ("toop.status.enabled", true);
  }

  // Servlet "/from-dc", step 1/4:

  public static boolean isDebugFromDCDumpEnabled ()
  {
    return getConfigFile ().getAsBoolean ("toop.debug.from-dc.dump.enabled", false);
  }

  @Nullable
  public static File getDebugFromDCDumpPath ()
  {
    final String sPath = getConfigFile ().getAsString ("toop.debug.from-dc.dump.path");
    return sPath == null ? null : new File (sPath);
  }

  @Nullable
  public static File getDebugFromDCDumpPathIfEnabled ()
  {
    return isDebugFromDCDumpEnabled () ? getDebugFromDCDumpPath () : null;
  }

  // Servlet "/from-dp", step 3/4:

  public static boolean isDebugFromDPDumpEnabled ()
  {
    return getConfigFile ().getAsBoolean ("toop.debug.from-dp.dump.enabled", false);
  }

  @Nullable
  public static File getDebugFromDPDumpPath ()
  {
    final String sPath = getConfigFile ().getAsString ("toop.debug.from-dp.dump.path");
    return sPath == null ? null : new File (sPath);
  }

  @Nullable
  public static File getDebugFromDPDumpPathIfEnabled ()
  {
    return isDebugFromDPDumpEnabled () ? getDebugFromDPDumpPath () : null;
  }

  // MessageProcessorDPOutgoingPerformer, step 3/4

  public static boolean isDebugToDCDumpEnabled ()
  {
    return getConfigFile ().getAsBoolean ("toop.debug.to-dc.dump.enabled", false);
  }

  @Nullable
  public static File getDebugToDCDumpPath ()
  {
    final String sPath = getConfigFile ().getAsString ("toop.debug.to-dc.dump.path");
    return sPath == null ? null : new File (sPath);
  }

  @Nullable
  public static File getDebugToDCDumpPathIfEnabled ()
  {
    return isDebugToDCDumpEnabled () ? getDebugToDCDumpPath () : null;
  }

  // MessageProcessorDCOutgoingPerformer, step 1/4

  public static boolean isDebugToDPDumpEnabled ()
  {
    return getConfigFile ().getAsBoolean ("toop.debug.to-dp.dump.enabled", false);
  }

  @Nullable
  public static File getDebugToDPDumpPath ()
  {
    final String sPath = getConfigFile ().getAsString ("toop.debug.to-dp.dump.path");
    return sPath == null ? null : new File (sPath);
  }

  @Nullable
  public static File getDebugToDPDumpPathIfEnabled ()
  {
    return isDebugToDPDumpEnabled () ? getDebugToDPDumpPath () : null;
  }

  public static boolean isUseHttpSystemProperties ()
  {
    return getConfigFile ().getAsBoolean ("toop.http.usesysprops", false);
  }

  public static boolean isProxyServerEnabled ()
  {
    return getConfigFile ().getAsBoolean ("toop.proxy.enabled", false);
  }

  @Nullable
  public static String getProxyServerAddress ()
  {
    // Scheme plus hostname or IP address
    return getConfigFile ().getAsString ("toop.proxy.address");
  }

  @CheckForSigned
  public static int getProxyServerPort ()
  {
    return getConfigFile ().getAsInt ("toop.proxy.port", -1);
  }

  @Nullable
  public static String getProxyServerNonProxyHosts ()
  {
    // Separated by pipe
    return getConfigFile ().getAsString ("toop.proxy.non-proxy");
  }

  public static boolean isTLSTrustAll ()
  {
    return getConfigFile ().getAsBoolean ("toop.tls.trustall", false);
  }
}
