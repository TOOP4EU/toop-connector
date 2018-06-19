/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.toop.connector.app;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.properties.SettingsPersistenceProperties;

/**
 * This class contains global TC server constants.
 *
 * @author Philip Helger
 */
@Immutable
public final class CTC {
  public static final String TOOP_CONNECTOR_VERSION_FILENAME = "toop-connector-version.properties";

  private static final String s_sVersionNumber;

  static {
    // Read version number
    final SettingsPersistenceProperties aSPP = new SettingsPersistenceProperties ();
    final ISettings aVersionProps = aSPP.readSettings (new ClassPathResource (TOOP_CONNECTOR_VERSION_FILENAME));
    s_sVersionNumber = aVersionProps.getAsString ("tc.version");
    if (s_sVersionNumber == null)
      throw new InitializationException ("Error determining TOOP Connector version number!");
  }

  @Deprecated
  @UsedViaReflection
  private CTC () {
  }

  /**
   * @return The version number of the TC server read from the internal properties
   *         file. Never <code>null</code>.
   */
  @Nonnull
  public static String getVersionNumber () {
    return s_sVersionNumber;
  }
}
