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
package eu.toop.connector.mp;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.asic.SignatureHelper;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.smmclient.ISMMConceptProvider;
import eu.toop.connector.smmclient.SMMConceptProviderGRLCRemote;

/**
 * Message Processor WebApp configuration
 *
 * @author Philip Helger
 */
@Immutable
public final class MPWebAppConfig {
  private static final SignatureHelper SH = new SignatureHelper (TCConfig.getKeystoreType (),
                                                                 TCConfig.getKeystorePath (),
                                                                 TCConfig.getKeystorePassword (),
                                                                 TCConfig.getKeystoreKeyAlias (),
                                                                 TCConfig.getKeystoreKeyPassword ());
  private static final ISMMConceptProvider CP = SMMConceptProviderGRLCRemote::getAllMappedValues;

  private MPWebAppConfig () {
  }

  /**
   * @return The {@link SignatureHelper} singleton. Never <code>null</code>.
   */
  @Nonnull
  public static SignatureHelper getSignatureHelper () {
    return SH;
  }

  /**
   * @return The {@link ISMMConceptProvider} singleton. Never <code>null</code>.
   */
  @Nonnull
  public static ISMMConceptProvider getSMMConceptProvider () {
    return CP;
  }

}
