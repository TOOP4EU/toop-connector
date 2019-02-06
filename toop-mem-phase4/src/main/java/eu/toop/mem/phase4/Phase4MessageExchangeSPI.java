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
package eu.toop.mem.phase4;

import java.io.File;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.esens.ESENSPMode;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.model.pmode.PModePayloadService;
import com.helger.as4.servlet.AS4ServerInitializer;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.string.StringHelper;
import com.helger.photon.basic.app.io.WebFileIO;

import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.as4.IMERoutingInformation;
import eu.toop.connector.api.as4.IMessageExchangeSPI;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;

/**
 * {@link IMessageExchangeSPI} implementation using ph-as4
 *
 * @author Philip Helger
 */
public class Phase4MessageExchangeSPI implements IMessageExchangeSPI
{
  public static final String ID = "mem-phase4";

  private IIncomingHandler m_aIncomingHandler;

  public Phase4MessageExchangeSPI ()
  {}

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return ID;
  }

  public void registerIncomingHandler (@Nonnull final ServletContext aServletContext,
                                       @Nonnull final IIncomingHandler aIncomingHandler) throws MEException
  {
    ValueEnforcer.notNull (aServletContext, "ServletContext");
    ValueEnforcer.notNull (aIncomingHandler, "IncomingHandler");
    if (m_aIncomingHandler != null)
      throw new IllegalStateException ("Another incoming handler was already registered!");
    m_aIncomingHandler = aIncomingHandler;

    // TODO register for servlet

    {
      // Get the ServletContext base path
      String sServletContextPath = aServletContext.getRealPath (".");
      if (sServletContextPath == null)
      {
        // Fallback for Undertow
        sServletContextPath = aServletContext.getRealPath ("");
      }
      if (StringHelper.hasNoText (sServletContextPath))
        throw new InitializationException ("No servlet context path was provided!");

      // Get the data path
      final String sDataPath = TCConfig.getConfigFile ().getAsString ("toop.phase4.datapath");
      if (StringHelper.hasNoText (sDataPath))
        throw new InitializationException ("No data path was provided!");
      final File aDataPath = new File (sDataPath).getAbsoluteFile ();
      // Init the IO layer
      WebFileIO.initPaths (aDataPath, sServletContextPath, false);
    }

    AS4ServerInitializer.initAS4Server ();

    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    {
      // SIMPLE_ONEWAY
      // 1. MEP: One way - push
      // 2. Compress: Yes
      // 3. Retry: None
      // 4. Sign: Yes
      // 5. Encrypt: Yes
      // 6. Service: SRV_SIMPLE_ONEWAY
      // 7. Action: ACT_SIMPLE_ONEWAY
      final PMode aPMode = ESENSPMode.createESENSPMode ("AnyInitiatorID",
                                                        "AnyResponderID",
                                                        "AnyResponderAddress",
                                                        (i, r) -> "TOOP_PMODE",
                                                        false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getBusinessInfo ().setService ("SRV_SIMPLE_ONEWAY");
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("ACT_SIMPLE_ONEWAY");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }

  }

  public void sendDCOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {}

  public void sendDPOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {}

  public void shutdown (@Nonnull final ServletContext aServletContext)
  {}
}
