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
package eu.toop.connector.app.searchdp;

import java.io.Serializable;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.locale.country.CountryCache;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;

import eu.toop.connector.api.TCSettings;

public final class SearchDPByCountryInputParams implements Serializable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SearchDPByCountryInputParams.class);

  private Locale m_aCountryCode;
  private IDocumentTypeIdentifier m_aDocTypeID;

  @Nonnull
  public ESuccess setCountryCode (@Nullable final String sCountryCode)
  {
    if (StringHelper.hasText (sCountryCode))
    {
      final String sTrimmedCountryCode = sCountryCode.trim ();
      final Locale aCountry = CountryCache.getInstance ().getCountry (sTrimmedCountryCode);
      if (aCountry != null)
      {
        m_aCountryCode = aCountry;
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Using country code '" + sTrimmedCountryCode + "' for /search-dp-by-country");
        return ESuccess.SUCCESS;
      }
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Country code '" + sTrimmedCountryCode + "' could not be resolved to a valid country");
    }
    return ESuccess.FAILURE;
  }

  @Nullable
  public Locale getCountryCode ()
  {
    return m_aCountryCode;
  }

  public boolean hasCountryCode ()
  {
    return m_aCountryCode != null;
  }

  @Nonnull
  public ESuccess setDocumentType (@Nullable final String sDocTypeID)
  {
    if (StringHelper.hasText (sDocTypeID))
    {
      final String sTrimmedDocTypeID = sDocTypeID.trim ();
      final IDocumentTypeIdentifier aDocTypeID = TCSettings.getIdentifierFactory ()
                                                           .parseDocumentTypeIdentifier (sTrimmedDocTypeID);
      if (aDocTypeID != null)
      {
        m_aDocTypeID = aDocTypeID;
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Using document type ID '" + sTrimmedDocTypeID + "' for /search-dp-by-country");
        return ESuccess.SUCCESS;
      }
      if (LOGGER.isWarnEnabled ())
        LOGGER.warn ("Document type ID '" + sTrimmedDocTypeID + "' could not be parsed");
    }
    return ESuccess.FAILURE;
  }

  @Nullable
  public IDocumentTypeIdentifier getDocumentTypeID ()
  {
    return m_aDocTypeID;
  }

  public boolean hasDocumentTypeID ()
  {
    return m_aDocTypeID != null;
  }
}
