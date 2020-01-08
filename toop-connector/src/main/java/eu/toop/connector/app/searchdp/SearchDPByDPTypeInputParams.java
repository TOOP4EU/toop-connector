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
package eu.toop.connector.app.searchdp;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;

public final class SearchDPByDPTypeInputParams implements Serializable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SearchDPByDPTypeInputParams.class);

  private String m_sDPType;

  @Nonnull
  public ESuccess setDPType (@Nullable final String sDPType)
  {
    if (StringHelper.hasText (sDPType))
    {
      final String sTrimmedDPType = sDPType.trim ();
      if (StringHelper.hasText (sTrimmedDPType))
      {
        m_sDPType = sTrimmedDPType;
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Using DP type '" + sTrimmedDPType + "' for /search-dp-by-dptype");
        return ESuccess.SUCCESS;
      }
    }
    return ESuccess.FAILURE;
  }

  @Nullable
  public String getDPType ()
  {
    return m_sDPType;
  }

  public boolean hasDPType ()
  {
    return StringHelper.hasText (m_sDPType);
  }
}
