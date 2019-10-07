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

import javax.annotation.Nonnull;

import com.helger.commons.url.ISimpleURL;
import com.helger.xml.microdom.IMicroDocument;

/**
 * Callback used in
 * {@link SearchDPByCountryHandler#performSearch(SearchDPByCountryInputParams, ISearchDPCallback)}
 * to handle the response states.
 *
 * @author Philip Helger
 */
public interface ISearchDPCallback extends Serializable
{
  /**
   * Invoked when querying the directory failed
   *
   * @param aQueryURL
   *        The Query URL that failed Never <code>null</code>.
   */
  void onQueryDirectoryError (@Nonnull ISimpleURL aQueryURL);

  /**
   * Invoked when querying the Directory was successful
   *
   * @param aDoc
   *        The response XML document. Never <code>null</code>.
   */
  void onQueryDirectorySuccess (@Nonnull IMicroDocument aDoc);
}
