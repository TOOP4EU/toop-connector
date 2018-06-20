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
package eu.toop.connector.app;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

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

  private static final String VERSION_NUMBER;

  static {
    // Read version number
    final SettingsPersistenceProperties aSPP = new SettingsPersistenceProperties ();
    final ISettings aVersionProps = aSPP.readSettings (new ClassPathResource (TOOP_CONNECTOR_VERSION_FILENAME));
    VERSION_NUMBER = aVersionProps.getAsString ("tc.version");
    if (VERSION_NUMBER == null)
      throw new InitializationException ("Error determining TOOP Connector version number!");
  }

  private CTC () {
  }

  /**
   * @return The version number of the TC server read from the internal properties
   *         file. Never <code>null</code>.
   */
  @Nonnull
  public static String getVersionNumber () {
    return VERSION_NUMBER;
  }
}
