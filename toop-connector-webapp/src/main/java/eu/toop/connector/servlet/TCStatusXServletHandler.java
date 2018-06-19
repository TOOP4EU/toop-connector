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
package eu.toop.connector.servlet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.system.SystemProperties;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.settings.ISettings;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.app.CTC;

public class TCStatusXServletHandler implements IXServletSimpleHandler {
  private static final Logger s_aLogger = LoggerFactory.getLogger (TCStatusXServletHandler.class);
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  @Nonnull
  @ReturnsMutableCopy
  public static IJsonObject getDefaultStatusData () {
    final ISettings aSettings = TCConfig.getConfigFile ().getSettings ();

    final IJsonObject aStatusData = new JsonObject ();
    aStatusData.add ("status.datetime", PDTWebDateHelper.getAsStringXSD (PDTFactory.getCurrentZonedDateTimeUTC ()));
    aStatusData.add ("version.toop-connector", CTC.getVersionNumber ());
    aStatusData.add ("version.java", SystemProperties.getJavaVersion ());
    aStatusData.add ("global.debug", GlobalDebug.isDebugMode ());
    aStatusData.add ("global.production", GlobalDebug.isProductionMode ());

    // Add all entries except the password entries
    for (final Map.Entry<String, Object> aEntry : aSettings.entrySet ()) {
      final String sKey = aEntry.getKey ();
      if (!sKey.contains ("password"))
        aStatusData.add (sKey, aEntry.getValue ());
    }

    return aStatusData;
  }

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception {
    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Status information requested");

    // Build data to provide
    IJsonObject aStatusData;
    if (TCConfig.isStatusEnabled ())
      aStatusData = getDefaultStatusData ();
    else {
      // Status is disabled in the configuration
      aStatusData = new JsonObject ();
      aStatusData.add ("status.enabled", false);
    }

    // Put JSON on response
    aUnifiedResponse.disableCaching ();
    aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_JSON).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                          CHARSET.name ()));
    aUnifiedResponse.setContentAndCharset (aStatusData.getAsJsonString (), CHARSET);
  }
}
