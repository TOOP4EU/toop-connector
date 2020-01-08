/**
 * Copyright (C) 2019-2020 toop.eu
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
package eu.toop.connector.mem.phase4.config;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.functional.ISupplier;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.profile.AS4Profile;
import com.helger.phase4.profile.IAS4Profile;
import com.helger.phase4.profile.IAS4ProfileRegistrar;
import com.helger.phase4.profile.IAS4ProfileRegistrarSPI;
import com.helger.phase4.profile.IAS4ProfileValidator;

/**
 * TOOP specific implementation of {@link IAS4ProfileRegistrarSPI}.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class Phase4ProfileRegistarSPI implements IAS4ProfileRegistrarSPI
{
  public static final String AS4_PROFILE_ID = "toop1";
  public static final String AS4_PROFILE_NAME = "TOOP v1";

  public void registerAS4Profile (@Nonnull final IAS4ProfileRegistrar aRegistrar)
  {
    final ISupplier <? extends IAS4ProfileValidator> aProfileValidatorProvider = () -> null;
    final IPModeIDProvider aPModeIDProvider = IPModeIDProvider.DEFAULT_DYNAMIC;
    final IAS4Profile aProfile = new AS4Profile (AS4_PROFILE_ID,
                                                 AS4_PROFILE_NAME,
                                                 aProfileValidatorProvider,
                                                 (i,
                                                  r,
                                                  a) -> TOOPPMode.createTOOPMode (i, r, a, aPModeIDProvider, true),
                                                 aPModeIDProvider,
                                                 false);
    aRegistrar.registerProfile (aProfile);
    aRegistrar.setDefaultProfile (aProfile);
  }
}
