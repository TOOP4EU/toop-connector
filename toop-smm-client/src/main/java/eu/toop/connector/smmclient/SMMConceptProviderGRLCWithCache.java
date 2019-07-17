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
package eu.toop.connector.smmclient;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.string.ToStringGenerator;

/**
 * Implementation of {@link ISMMConceptProvider} using caching. The retrieval of
 * remote data happens via {@link SMMConceptProviderGRLCRemote}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class SMMConceptProviderGRLCWithCache implements ISMMConceptProvider
{
  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <String, ICommonsMap <String, MappedValueList>> m_aCache = new CommonsHashMap <> ();

  public SMMConceptProviderGRLCWithCache ()
  {}

  /**
   * Remove all cache values.
   */
  public void clearCache ()
  {
    m_aRWLock.writeLocked (m_aCache::clear);
  }

  @Nullable
  private MappedValueList _getFromCache (@Nonnull @Nonempty final String sSourceNamespace,
                                         @Nonnull @Nonempty final String sDestNamespace)
  {
    final ICommonsMap <String, MappedValueList> aPerSrcMap = m_aCache.get (sSourceNamespace);
    if (aPerSrcMap != null)
      return aPerSrcMap.get (sDestNamespace);
    return null;
  }

  /**
   * Get all mapped values from source to target namespace. If not present (in
   * cache) it is retrieved from the remote server.<br>
   * Note: this method must follow the {@link ISMMConceptProvider} interface
   * signature
   *
   * @param sLogPrefix
   *        Log prefix. May not be <code>null</code> but may be empty.
   * @param sSourceNamespace
   *        Source namespace to map from. May not be <code>null</code>.
   * @param sDestNamespace
   *        Target namespace to map to. May not be <code>null</code>.
   * @return The non-<code>null</code> but maybe empty list of mapped values.
   * @throws IOException
   *         In case fetching from server failed
   */
  @Nonnull
  public MappedValueList getAllMappedValues (@Nonnull final String sLogPrefix,
                                             @Nonnull final String sSourceNamespace,
                                             @Nonnull final String sDestNamespace) throws IOException
  {
    ValueEnforcer.notNull (sLogPrefix, "LogPrefix");
    ValueEnforcer.notNull (sSourceNamespace, "SourceNamespace");
    ValueEnforcer.notNull (sDestNamespace, "DestNamespace");

    // First in read-lock for maximum speed
    MappedValueList ret = m_aRWLock.readLocked ( () -> _getFromCache (sSourceNamespace, sDestNamespace));
    if (ret == null)
    {
      // Not found - slow path
      m_aRWLock.writeLock ().lock ();
      try
      {
        // Try in write lock
        ret = _getFromCache (sSourceNamespace, sDestNamespace);
        if (ret == null)
        {
          // Not in cache - query from server and put in cache
          ret = SMMConceptProviderGRLCRemote.remoteQueryAllMappedValues (sLogPrefix, sSourceNamespace, sDestNamespace);
          m_aCache.computeIfAbsent (sSourceNamespace, k -> new CommonsHashMap <> ()).put (sDestNamespace, ret);

          // Put in the map the other way around as well (inference)
          m_aCache.computeIfAbsent (sDestNamespace, k -> new CommonsHashMap <> ())
                  .put (sSourceNamespace, ret.getSwappedSourceAndDest ());
        }
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }
    }

    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("Cache", m_aCache).getToString ();
  }
}
