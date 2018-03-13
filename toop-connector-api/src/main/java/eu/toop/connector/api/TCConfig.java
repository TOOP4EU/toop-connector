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
package eu.toop.connector.api;

import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.state.ESuccess;
import com.helger.commons.url.URLHelper;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.settings.exchange.configfile.ConfigFile;
import com.helger.settings.exchange.configfile.ConfigFileBuilder;

/**
 * This class contains global configuration elements for the TOOP COnnector.
 *
 * @author Philip Helger, BRZ, AT
 */
@Immutable
public final class TCConfig {
  private static final Logger s_aLogger = LoggerFactory.getLogger (TCConfig.class);
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static ConfigFile s_aConfigFile;

  static {
    reloadConfiguration ();
  }

  /**
   * The name of the primary system property which points to the
   * <code>toop-connector.properties</code> files
   */
  public static final String SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH = "toop.connector.server.properties.path";

  /** The default primary properties file to load */
  public static final String PATH_PRIVATE_TOOP_CONNECTOR_PROPERTIES = "private-toop-connector.properties";
  /** The default secondary properties file to load */
  public static final String PATH_TOOP_CONNECTOR_PROPERTIES = "toop-connector.properties";

  private static ISMLInfo s_aCachedSMLInfo;

  /**
   * Reload the configuration file. It checks if the system property
   * {@link #SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH} is present and
   * if so, tries it first, than {@link #PATH_PRIVATE_TOOP_CONNECTOR_PROPERTIES}
   * is checked and finally the {@link #PATH_TOOP_CONNECTOR_PROPERTIES} path is
   * checked.
   *
   * @return {@link ESuccess}
   */
  @Nonnull
  public static ESuccess reloadConfiguration () {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromSystemProperty (SYSTEM_PROPERTY_TOOP_CONNECTOR_SERVER_PROPERTIES_PATH)
                                                           .addPath (PATH_PRIVATE_TOOP_CONNECTOR_PROPERTIES)
                                                           .addPath (PATH_TOOP_CONNECTOR_PROPERTIES);

    return s_aRWLock.writeLocked ( () -> {
      s_aConfigFile = aCFB.build ();
      if (s_aConfigFile.isRead ()) {
        // Clear cache
        s_aCachedSMLInfo = null;

        s_aLogger.info ("Read TOOP Connector server properties from " + s_aConfigFile.getReadResource ().getPath ());
        return ESuccess.SUCCESS;
      }

      s_aLogger.warn ("Failed to read TOOP Connector server properties from " + aCFB.getAllPaths ());
      return ESuccess.FAILURE;
    });
  }

  public static final boolean DEFAULT_TOOP_TRACKER_ENABLED = false;
  public static final String DEFAULT_DIRECTORY_BASE_URL = "http://directory.central.toop:7071";
  public static final boolean DEFAULT_USE_SML = false;
  public static final String DEFAULT_SMP_URI = "http://smp.central.toop:80";

  private TCConfig () {
  }

  /**
   * @return The configuration file. Never <code>null</code>.
   */
  @Nonnull
  public static ConfigFile getConfigFile () {
    return s_aRWLock.readLocked ( () -> s_aConfigFile);
  }

  public static boolean isGlobalDebug () {
    return getConfigFile ().getAsBoolean ("global.debug", GlobalDebug.isDebugMode ());
  }

  public static boolean isGlobalProduction () {
    return getConfigFile ().getAsBoolean ("global.production", GlobalDebug.isProductionMode ());
  }

  public static boolean isToopTrackerEnabled () {
    return getConfigFile ().getAsBoolean ("toop.tracker.enabled", DEFAULT_TOOP_TRACKER_ENABLED);
  }

  @Nullable
  public static String getToopTrackerUrl () {
    return getConfigFile ().getAsString ("toop.tracker.url");
  }

  /**
   *
   * @return The SMM query URL to GRLC. Should end with a slash. May be
   *         <code>null</code> - no default.
   */
  @Nullable
  public static String getSMMGRLCURL () {
    // E.g.
    // http://localhost:8001/
    // https://hamster.tno.nl/plasido-grlc/
    return getConfigFile ().getAsString ("toop.smm.grlc.url");
  }

  /**
   *
   * @return The destination namespace URI to map to (for DP incoming step 2/4)
   */
  @Nullable
  public static String getSMMMappingNamespaceURI () {
    return getConfigFile ().getAsString ("toop.smm.namespaceuri");
  }

  /**
   * @return The PEPPOL Directory base URL for R2D2.Should never end with a slash.
   *         Example: {@link #DEFAULT_DIRECTORY_BASE_URL}.
   */
  @Nonnull
  @Nonempty
  public static String getR2D2DirectoryBaseUrl () {
    return getConfigFile ().getAsString ("toop.r2d2.directory.baseurl", DEFAULT_DIRECTORY_BASE_URL);
  }

  /**
   * @return <code>true</code> to use SML lookup, <code>false</code> to not do it.
   * @see #getR2D2SML()
   * @see #getR2D2SMPUrl()
   */
  public static boolean isR2D2UseDNS () {
    return getConfigFile ().getAsBoolean ("toop.r2d2.usedns", DEFAULT_USE_SML);
  }

  /**
   * @return The SML URL to be used. Must only contain a value if
   *         {@link #isR2D2UseDNS()} returned <code>true</code>.
   */
  @Nonnull
  public static ISMLInfo getR2D2SML () {
    ISMLInfo ret = s_aCachedSMLInfo;
    if (ret == null) {
      final String sSMLID = getConfigFile ().getAsString ("toop.r2d2.sml.id");
      final ESML eSML = ESML.getFromIDOrNull (sSMLID);
      if (eSML != null)
        // Pre-configured SML
        ret = eSML;
      else {
        // Custom SML
        final String sDisplayName = getConfigFile ().getAsString ("toop.r2d2.sml.name", "MP SML");
        // E.g. edelivery.tech.ec.europa.eu.
        final String sDNSZone = getConfigFile ().getAsString ("toop.r2d2.sml.dnszone");
        // E.g. https://edelivery.tech.ec.europa.eu/edelivery-sml
        final String sManagementServiceURL = getConfigFile ().getAsString ("toop.r2d2.sml.serviceurl");
        final boolean bClientCertificateRequired = getConfigFile ().getAsBoolean ("toop.r2d2.sml.clientcert", false);
        ret = new SMLInfo (sDisplayName, sDNSZone, sManagementServiceURL, bClientCertificateRequired);
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
  public static URI getR2D2SMPUrl () {
    // E.g. http://smp.central.toop
    final String sURI = getConfigFile ().getAsString ("toop.r2d2.smp.url", DEFAULT_SMP_URI);
    return URLHelper.getAsURI (sURI);
  }

  /**
   * Get the overall protocol to be used. Depending on that output different other
   * properties might be queried.
   *
   * @return The overall protocol to use. Never <code>null</code>.
   */
  @Nonnull
  public static ETCProtocol getMEMProtocol () {
    final String sID = getConfigFile ().getAsString ("toop.mem.protocol", ETCProtocol.DEFAULT.getID ());
    final ETCProtocol eProtocol = ETCProtocol.getFromIDOrNull (sID);
    if (eProtocol == null)
      throw new IllegalStateException ("Failed to resolve protocol with ID '" + sID + "'");
    return eProtocol;
  }

  /**
   *
   * @return The basic AS4 interface that must be fulfilled. Never
   *         <code>null</code>.
   */
  @Nonnull
  public static ETCAS4Interface getMEMAS4Interface () {
    final String sID = getConfigFile ().getAsString ("toop.mem.as4.interface", ETCAS4Interface.DEFAULT.getID ());
    final ETCAS4Interface eProtocol = ETCAS4Interface.getFromIDOrNull (sID);
    if (eProtocol == null)
      throw new IllegalStateException ("Failed to resolve AS4 interface with ID '" + sID + "'");
    return eProtocol;
  }

  // GW_URL
  @Nullable
  public static String getMEMAS4Endpoint () {
    return getConfigFile ().getAsString ("toop.mem.as4.endpoint");
  }

  // ME_NAME
  @Nullable
  public static String getMEMAS4IDSuffix () {
    return getConfigFile ().getAsString ("toop.mem.as4.idsuffix");
  }

  // ME_PARTY_ID
  @Nullable
  public static String getMEMAS4FromPartyID () {
    return getConfigFile ().getAsString ("toop.mem.as4.from.partyid");
  }

  // ME_PARTY_ROLE
  @Nullable
  public static String getMEMAS4FromRole () {
    return getConfigFile ().getAsString ("toop.mem.as4.from.role");
  }

  // GW_PARTY_ID
  @Nullable
  public static String getMEMAS4ToPartyID () {
    return getConfigFile ().getAsString ("toop.mem.as4.to.partyid");
  }

  // Receiving GW party id
  @Nullable
  public static String getMEMAS4ReceivingPartyID () {
    return getConfigFile ().getAsString ("toop.mem.as4.receiving.partyid");
  }

  // GW_PARTY_ROLE
  @Nullable
  public static String getMEMAS4ToRole () {
    return getConfigFile ().getAsString ("toop.mem.as4.to.role");
  }

  // SUBMIT_ACTION
  @Nullable
  public static String getMEMAS4Action () {
    return getConfigFile ().getAsString ("toop.mem.as4.action");
  }

  // SUBMIT_SERVICE
  @Nullable
  public static String getMEMAS4Service () {
    return getConfigFile ().getAsString ("toop.mem.as4.service");
  }

  @Nullable
  public static String getMPToopInterfaceDPUrl () {
    return getConfigFile ().getAsString ("toop.mp.dp.url");
  }

  @Nullable
  public static String getKeystorePath () {
    return getConfigFile ().getAsString ("toop.keystore.path");
  }

  @Nullable
  public static String getKeystorePassword () {
    return getConfigFile ().getAsString ("toop.keystore.password");
  }

  @Nullable
  public static String getKeystoreKeyAlias () {
    return getConfigFile ().getAsString ("toop.keystore.key.alias");
  }

  @Nullable
  public static String getKeystoreKeyPassword () {
    return getConfigFile ().getAsString ("toop.keystore.key.password");
  }
}
