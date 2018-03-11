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
package eu.toop.mp.api;

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
 * This class contains global configuration elements for the MessageProcessor.
 *
 * @author Philip Helger, BRZ, AT
 */
@Immutable
public final class MPConfig {
  private static final Logger s_aLogger = LoggerFactory.getLogger (MPConfig.class);
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static ConfigFile s_aConfigFile;

  static {
    reloadConfiguration ();
  }

  /**
   * The name of the primary system property which points to the
   * <code>message-processor.properties</code> files
   */
  public static final String SYSTEM_PROPERTY_TOOP_MP_SERVER_PROPERTIES_PATH = "toop.mp.server.properties.path";

  /** The default primary properties file to load */
  public static final String PATH_PRIVATE_MESSAGE_PROCESSOR_PROPERTIES = "private-message-processor.properties";
  /** The default secondary properties file to load */
  public static final String PATH_MESSAGE_PROCESSOR_PROPERTIES = "message-processor.properties";

  private static ISMLInfo s_aCachedSMLInfo;

  /**
   * Reload the configuration file. It checks if the system property
   * {@link #SYSTEM_PROPERTY_TOOP_MP_SERVER_PROPERTIES_PATH} is present and if so,
   * tries it first, than {@link #PATH_PRIVATE_MESSAGE_PROCESSOR_PROPERTIES} is
   * checked and finally the {@link #PATH_MESSAGE_PROCESSOR_PROPERTIES} path is
   * checked.
   *
   * @return {@link ESuccess}
   */
  @Nonnull
  public static ESuccess reloadConfiguration () {
    final ConfigFileBuilder aCFB = new ConfigFileBuilder ().addPathFromSystemProperty (SYSTEM_PROPERTY_TOOP_MP_SERVER_PROPERTIES_PATH)
                                                           .addPath (PATH_PRIVATE_MESSAGE_PROCESSOR_PROPERTIES)
                                                           .addPath (PATH_MESSAGE_PROCESSOR_PROPERTIES);

    return s_aRWLock.writeLocked ( () -> {
      s_aConfigFile = aCFB.build ();
      if (s_aConfigFile.isRead ()) {
        // Clear cache
        s_aCachedSMLInfo = null;

        s_aLogger.info ("Read TOOP MP server properties from " + s_aConfigFile.getReadResource ().getPath ());
        return ESuccess.SUCCESS;
      }

      s_aLogger.warn ("Failed to read TOOP MP server properties from " + aCFB.getAllPaths ());
      return ESuccess.FAILURE;
    });
  }

  public static final boolean DEFAULT_TOOP_TRACKER_ENABLED = false;
  public static final String DEFAULT_DIRECTORY_BASE_URL = "http://directory.central.toop";
  public static final boolean DEFAULT_USE_SML = false;
  public static final String DEFAULT_SMP_URI = "http://smp.central.toop";

  private MPConfig () {
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
    return getConfigFile ().getAsString ("mp.smm.grlc.url");
  }

  /**
   * @return The PEPPOL Directory base URL for R2D2.Should never end with a slash.
   *         Example: {@link #DEFAULT_DIRECTORY_BASE_URL}.
   */
  @Nonnull
  @Nonempty
  public static String getR2D2DirectoryBaseUrl () {
    return getConfigFile ().getAsString ("mp.r2d2.directory.baseurl", DEFAULT_DIRECTORY_BASE_URL);
  }

  /**
   * @return <code>true</code> to use SML lookup, <code>false</code> to not do it.
   * @see #getR2D2SML()
   * @see #getR2D2SMPUrl()
   */
  public static boolean isR2D2UseDNS () {
    return getConfigFile ().getAsBoolean ("mp.r2d2.usedns", DEFAULT_USE_SML);
  }

  /**
   * @return The SML URL to be used. Must only contain a value if
   *         {@link #isR2D2UseDNS()} returned <code>true</code>.
   */
  @Nonnull
  public static ISMLInfo getR2D2SML () {
    ISMLInfo ret = s_aCachedSMLInfo;
    if (ret == null) {
      final String sSMLID = getConfigFile ().getAsString ("mp.r2d2.sml.id");
      final ESML eSML = ESML.getFromIDOrNull (sSMLID);
      if (eSML != null)
        // Pre-configured SML
        ret = eSML;
      else {
        // Custom SML
        final String sDisplayName = getConfigFile ().getAsString ("mp.r2d2.sml.name", "MP SML");
        // E.g. edelivery.tech.ec.europa.eu.
        final String sDNSZone = getConfigFile ().getAsString ("mp.r2d2.sml.dnszone");
        // E.g. https://edelivery.tech.ec.europa.eu/edelivery-sml
        final String sManagementServiceURL = getConfigFile ().getAsString ("mp.r2d2.sml.serviceurl");
        final boolean bClientCertificateRequired = getConfigFile ().getAsBoolean ("mp.r2d2.sml.clientcert", false);
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
    final String sURI = getConfigFile ().getAsString ("mp.r2d2.smp.url", DEFAULT_SMP_URI);
    return URLHelper.getAsURI (sURI);
  }

  /**
   * Get the overall protocol to be used. Depending on that output different other
   * properties might be queried.
   *
   * @return The overall protocol to use. Never <code>null</code>.
   */
  @Nonnull
  public static EMPProtocol getMEMProtocol () {
    final String sID = getConfigFile ().getAsString ("mp.mem.protocol", EMPProtocol.DEFAULT.getID ());
    final EMPProtocol eProtocol = EMPProtocol.getFromIDOrNull (sID);
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
  public static EMPAS4Interface getMEMAS4Interface () {
    final String sID = getConfigFile ().getAsString ("mp.mem.as4.interface", EMPAS4Interface.DEFAULT.getID ());
    final EMPAS4Interface eProtocol = EMPAS4Interface.getFromIDOrNull (sID);
    if (eProtocol == null)
      throw new IllegalStateException ("Failed to resolve AS4 interface with ID '" + sID + "'");
    return eProtocol;
  }

  // GW_URL
  @Nullable
  public static String getMEMAS4Endpoint () {
    return getConfigFile ().getAsString ("mp.mem.as4.endpoint");
  }

  // ME_NAME
  @Nullable
  public static String getMEMAS4IDSuffix () {
    return getConfigFile ().getAsString ("mp.mem.as4.idsuffix");
  }

  // ME_PARTY_ID
  @Nullable
  public static String getMEMAS4FromPartyID () {
    return getConfigFile ().getAsString ("mp.mem.as4.from.partyid");
  }

  // ME_PARTY_ROLE
  @Nullable
  public static String getMEMAS4FromRole () {
    return getConfigFile ().getAsString ("mp.mem.as4.from.role");
  }

  // GW_PARTY_ID
  @Nullable
  public static String getMEMAS4ToPartyID () {
    return getConfigFile ().getAsString ("mp.mem.as4.to.partyid");
  }

  // Receiving GW party id
  @Nullable
  public static String getMEMAS4ReceivingPartyID () {
    return getConfigFile ().getAsString ("mp.mem.as4.receiving.partyid");
  }

  // GW_PARTY_ROLE
  @Nullable
  public static String getMEMAS4ToRole () {
    return getConfigFile ().getAsString ("mp.mem.as4.to.role");
  }

  // SUBMIT_ACTION
  @Nullable
  public static String getMEMAS4Action () {
    return getConfigFile ().getAsString ("mp.mem.as4.action");
  }

  // SUBMIT_SERVICE
  @Nonnull
  @Nonempty
  public static String getMEMAS4Service () {
    return getConfigFile ().getAsString ("mp.mem.as4.service", "http://www.toop.eu/as4/backend");
  }

  @Nullable
  public static String getMPKeyStorePath () {
    return getConfigFile ().getAsString ("mp.keystore.path");
  }

  @Nullable
  public static String getMPKeyStorePassword () {
    return getConfigFile ().getAsString ("mp.keystore.password");
  }

  @Nullable
  public static String getMPKeyStoreKeyAlias () {
    return getConfigFile ().getAsString ("mp.keystore.key.alias");
  }

  @Nullable
  public static String getMPKeyStoreKeyPassword () {
    return getConfigFile ().getAsString ("mp.keystore.key.password");
  }
}
