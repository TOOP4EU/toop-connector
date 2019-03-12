/**
 * Copyright (C) 2019 toop.eu
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
package eu.toop.mem.phase4.config;

import javax.annotation.Nonnull;

import com.helger.as4.model.pmode.IPModeIDProvider;
import com.helger.as4.profile.AS4Profile;
import com.helger.as4.profile.IAS4ProfileRegistrar;
import com.helger.as4.profile.IAS4ProfileRegistrarSPI;
import com.helger.commons.annotation.IsSPIImplementation;

/**
 * Library specific implementation of {@link IAS4ProfileRegistrarSPI}.
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
    final IPModeIDProvider aPModeIDProvider = IPModeIDProvider.DEFAULT_DYNAMIC;
    aRegistrar.registerProfile (new AS4Profile (AS4_PROFILE_ID,
                                                AS4_PROFILE_NAME,
                                                () -> null,
                                                (i, r, a) -> TOOPPMode.createTOOPMode (i, r, a, aPModeIDProvider, true),
                                                aPModeIDProvider));
  }
}
