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

import eu.toop.commons.codelist.EPredefinedDocumentTypeIdentifier;
import eu.toop.commons.usecase.SMMDocumentTypeMapping;

final class CMockSMM {
  public static final String LOG_PREFIX = "[unit test] ";
  public static final String NS_TOOP = SMMDocumentTypeMapping.getToopSMNamespace (EPredefinedDocumentTypeIdentifier.REQUEST_REGISTEREDORGANIZATION);
  public static final String NS_ELONIA = "http://toop.elo/elonia-business-register";
  public static final String NS_FREEDONIA = "http://toop.fre/freedonia-business-register";

  private CMockSMM () {
  }
}