/**
 * Copyright (C) 2018-2019 toop.eu
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
package eu.toop.connector.app.mp;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.asic.SignatureHelper;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.concurrent.SimpleReadWriteLock;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.r2d2.IR2D2EndpointProvider;
import eu.toop.connector.api.r2d2.IR2D2ParticipantIDProvider;
import eu.toop.connector.api.smm.ISMMConceptProvider;
import eu.toop.connector.app.r2d2.R2D2EndpointProviderBDXRSMP1;
import eu.toop.connector.app.r2d2.R2D2ParticipantIDProviderTOOPDirectory;
import eu.toop.connector.app.smm.SMMConceptProviderGRLCWithCache;

/**
 * Message Processor WebApp configuration
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class MPConfig
{
  private static final SimpleReadWriteLock s_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("s_aRWLock")
  private static SignatureHelper s_aSH;
  @GuardedBy ("s_aRWLock")
  private static ISMMConceptProvider s_aCP;
  @GuardedBy ("s_aRWLock")
  private static IR2D2ParticipantIDProvider s_aPIDP;
  @GuardedBy ("s_aRWLock")
  private static IR2D2EndpointProvider s_aEPP;
  @GuardedBy ("s_aRWLock")
  private static IToDP s_aToDP;

  static
  {
    setToDefault ();
  }

  private MPConfig ()
  {}

  public static void setToDefault ()
  {
    s_aRWLock.writeLocked ( () -> {
      s_aSH = null;
      s_aCP = new SMMConceptProviderGRLCWithCache ();
      s_aPIDP = new R2D2ParticipantIDProviderTOOPDirectory ();
      s_aEPP = new R2D2EndpointProviderBDXRSMP1 ();
      s_aToDP = new ToDPViaToopInterfaceHttp ();
    });
  }

  /**
   * @return The {@link SignatureHelper} singleton. Never <code>null</code>.
   */
  @Nonnull
  public static SignatureHelper getSignatureHelper ()
  {
    SignatureHelper ret = s_aRWLock.readLocked ( () -> s_aSH);
    if (ret == null)
    {
      // Lazy init to avoid exception on misconfiguration
      ret = new SignatureHelper (TCConfig.getKeystoreType (),
                                 TCConfig.getKeystorePath (),
                                 TCConfig.getKeystorePassword (),
                                 TCConfig.getKeystoreKeyAlias (),
                                 TCConfig.getKeystoreKeyPassword ());
      setSignatureHelper (ret);
    }
    return ret;
  }

  /**
   * @param aSH
   *        The {@link SignatureHelper} to use. May not be <code>null</code>.
   * @since 0.10.6
   */
  public static void setSignatureHelper (@Nonnull final SignatureHelper aSH)
  {
    ValueEnforcer.notNull (aSH, "SignatureHelper");
    s_aRWLock.writeLocked ( () -> s_aSH = aSH);
  }

  /**
   * @return The {@link ISMMConceptProvider} singleton. Never <code>null</code>.
   */
  @Nonnull
  public static ISMMConceptProvider getSMMConceptProvider ()
  {
    return s_aRWLock.readLocked ( () -> s_aCP);
  }

  /**
   * @param aCP
   *        The {@link ISMMConceptProvider} to use. May not be
   *        <code>null</code>.
   * @since 0.10.6
   */
  public static void setSMMConceptProvider (@Nonnull final ISMMConceptProvider aCP)
  {
    ValueEnforcer.notNull (aCP, "ConceptProvider");
    s_aRWLock.writeLocked ( () -> s_aCP = aCP);
  }

  /**
   * @return The R2D2 participant ID provider from country code and document
   *         type ID. Never <code>null</code>.
   * @since 0.10.6
   */
  @Nonnull
  public static IR2D2ParticipantIDProvider getParticipantIDProvider ()
  {
    return s_aRWLock.readLocked ( () -> s_aPIDP);
  }

  /**
   * @param aPIDP
   *        The R2D2 participant ID provider from country code and document type
   *        ID to be used. May not be <code>null</code>.
   * @since 0.10.6
   */
  public static void setParticipantIDProvider (@Nonnull final IR2D2ParticipantIDProvider aPIDP)
  {
    ValueEnforcer.notNull (aPIDP, "ParticipantIDProvider");
    s_aRWLock.writeLocked ( () -> s_aPIDP = aPIDP);
  }

  /**
   * @return The R2D2 endpoint provider. Never <code>null</code>.
   * @since 0.10.6
   */
  @Nonnull
  public static IR2D2EndpointProvider getEndpointProvider ()
  {
    return s_aRWLock.readLocked ( () -> s_aEPP);
  }

  /**
   * @param aEPP
   *        The R2D2 endpoint provider. May not be <code>null</code>.
   * @since 0.10.6
   */
  public static void setEndpointProvider (@Nonnull final IR2D2EndpointProvider aEPP)
  {
    ValueEnforcer.notNull (aEPP, "EndpointProvider");
    s_aRWLock.writeLocked ( () -> s_aEPP = aEPP);
  }

  /**
   * @return The To-DP implementation for step 2/4. Never <code>null</code>.
   * @since 0.10.6
   */
  @Nonnull
  public static IToDP getToDP ()
  {
    return s_aRWLock.readLocked ( () -> s_aToDP);
  }

  /**
   * @param aToDP
   *        The To-DP implementation for step 2/4. May not be <code>null</code>.
   * @since 0.10.6
   */
  public static void setToDP (@Nonnull final IToDP aToDP)
  {
    ValueEnforcer.notNull (aToDP, "ToDP");
    s_aRWLock.writeLocked ( () -> s_aToDP = aToDP);
  }
}
