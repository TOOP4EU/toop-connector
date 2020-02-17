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
package eu.toop.connector.mem.phase4;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.datetime.util.PDTIOHelper;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.phase4.CAS4;
import com.helger.phase4.attachment.EAS4CompressionMode;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientSentMessage;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.IAS4OutgoingDumper;
import com.helger.phase4.http.AS4HttpDebug;
import com.helger.phase4.messaging.domain.MessageHelperMethods;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.pmode.IPModeManager;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.servlet.AS4ServerInitializer;
import com.helger.phase4.soap.ESoapVersion;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.photon.app.io.WebFileIO;
import com.helger.servlet.ServletHelper;

import eu.toop.commons.error.EToopErrorCode;
import eu.toop.connector.api.as4.IMEIncomingHandler;
import eu.toop.connector.api.as4.IMERoutingInformation;
import eu.toop.connector.api.as4.IMessageExchangeSPI;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.api.as4.MEPayload;
import eu.toop.connector.api.http.TCHttpClientSettings;
import eu.toop.connector.mem.phase4.config.TOOPPMode;
import eu.toop.connector.mem.phase4.servlet.AS4MessageProcessorSPI;

/**
 * TOOP {@link IMessageExchangeSPI} implementation using ph-as4.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class Phase4MessageExchangeSPI implements IMessageExchangeSPI
{
  public static final String ID = "mem-phase4";
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4MessageExchangeSPI.class);

  private final IAS4CryptoFactory m_aCF;

  public Phase4MessageExchangeSPI ()
  {
    m_aCF = Phase4Config.getCryptoFactory ();
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return ID;
  }

  public void registerIncomingHandler (@Nonnull final ServletContext aServletContext,
                                       @Nonnull final IMEIncomingHandler aIncomingHandler) throws MEException
  {
    ValueEnforcer.notNull (aServletContext, "ServletContext");
    ValueEnforcer.notNull (aIncomingHandler, "IncomingHandler");

    if (!WebFileIO.isInited ())
    {
      // Get the ServletContext base path
      final String sServletContextPath = ServletHelper.getServletContextBasePath (aServletContext);

      // Get the data path
      final String sDataPath = Phase4Config.getDataPath ();
      if (StringHelper.hasNoText (sDataPath))
        throw new InitializationException ("No data path was provided!");
      final File aDataPath = new File (sDataPath).getAbsoluteFile ();
      // Init the IO layer
      WebFileIO.initPaths (aDataPath, sServletContextPath, false);
    }

    // Sanity check
    {
      final KeyStore aOurKS = m_aCF.getKeyStore ();
      if (aOurKS == null)
        throw new InitializationException ("Failed to load configured phase4 keystore (crypto.properties)");

      final PrivateKeyEntry aOurKey = m_aCF.getPrivateKeyEntry ();
      if (aOurKey == null)
        throw new InitializationException ("Failed to load configured phase4 key (crypto.properties)");
    }

    // Register server once
    AS4ServerInitializer.initAS4Server ();

    final IPModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
    {
      final PMode aPMode = TOOPPMode.createTOOPMode ("AnyInitiatorID",
                                                     "AnyResponderID",
                                                     "AnyResponderAddress",
                                                     (i, r) -> "TOOP_PMODE",
                                                     false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPModeMgr.createOrUpdatePMode (aPMode);
    }

    // Remember handler
    AS4MessageProcessorSPI.setIncomingHandler (aIncomingHandler);

    // Enable debug (incoming and outgoing)
    if (Phase4Config.isHttpDebugEnabled ())
      AS4HttpDebug.setEnabled (true);
  }

  private void _sendOutgoing (@Nonnull final IAS4CryptoFactory aCF,
                              @Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {
    final StopWatch aSW = StopWatch.createdStarted ();

    final X509Certificate aTheirCert = aRoutingInfo.getCertificate ();

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4ClientUserMessage aClient = new AS4ClientUserMessage (aResHelper);
      aClient.setSoapVersion (ESoapVersion.SOAP_12);
      aClient.setAS4CryptoFactory (aCF);

      aClient.signingParams ()
             .setAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_512)
             .setAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);

      aClient.setAction ("RequestDocuments");
      aClient.setServiceType (null);
      aClient.setServiceValue ("TOOPDataProvisioning");
      aClient.setConversationID (MessageHelperMethods.createRandomConversationID ());
      aClient.setAgreementRefValue (null);

      // Backend or gateway?
      aClient.setFromRole ("http://www.toop.eu/edelivery/backend");
      aClient.setFromPartyID (Phase4Config.getFromPartyID ());
      aClient.setToRole ("http://www.toop.eu/edelivery/gateway");
      aClient.setToPartyID (PeppolCertificateHelper.getCN (aTheirCert.getSubjectDN ().getName ()));
      aClient.setPayload (null);

      aClient.ebms3Properties ()
             .setAll (MessageHelperMethods.createEbms3Property (CAS4.ORIGINAL_SENDER,
                                                                "urn:oasis:names:tc:ebcore:partyid-type:unregistered:dc"),
                      MessageHelperMethods.createEbms3Property (CAS4.FINAL_RECIPIENT,
                                                                "urn:oasis:names:tc:ebcore:partyid-type:unregistered:dp"));

      for (final MEPayload aPayload : aMessage.payloads ())
      {
        try
        {
          aClient.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aPayload.getData ().bytes (),
                                                                               (String) null,
                                                                               "payload.asic",
                                                                               aPayload.getMimeType (),
                                                                               (EAS4CompressionMode) null,
                                                                               aResHelper));
        }
        catch (final IOException ex)
        {
          throw new MEException (EToopErrorCode.ME_001, ex);
        }
      }

      // Proxy config etc
      aClient.setHttpClientFactory (new HttpClientFactory (new TCHttpClientSettings ()));

      // Main sending
      final IAS4ClientBuildMessageCallback aCallback = null;
      final IAS4OutgoingDumper aOutgoingDumper = null;
      final AS4ClientSentMessage <byte []> aResponseEntity = aClient.sendMessageWithRetries (aRoutingInfo.getEndpointURL (),
                                                                                             new ResponseHandlerByteArray (),
                                                                                             aCallback,
                                                                                             aOutgoingDumper);
      LOGGER.info ("[phase4] Successfully transmitted document with message ID '" +
                   aResponseEntity.getMessageID () +
                   "' for '" +
                   aRoutingInfo.getReceiverID ().getURIEncoded () +
                   "' to '" +
                   aRoutingInfo.getEndpointURL () +
                   "' in " +
                   aSW.stopAndGetMillis () +
                   " ms");

      if (aResponseEntity.hasResponse () && aResponseEntity.getResponse ().length > 0)
      {
        final String sFolderName = Phase4Config.getSendResponseFolderName ();
        if (StringHelper.hasText (sFolderName))
        {
          final String sMessageID = aResponseEntity.getMessageID ();
          final String sFilename = PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                                   "-" +
                                   FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) +
                                   "-response.xml";
          final File aResponseFile = new File (sFolderName, sFilename);
          if (SimpleFileIO.writeFile (aResponseFile, aResponseEntity.getResponse ()).isSuccess ())
            LOGGER.info ("[phase4] Response file was written to '" + aResponseFile.getAbsolutePath () + "'");
          else
            LOGGER.error ("[phase4] Error writing response file to '" + aResponseFile.getAbsolutePath () + "'");
        }
      }
      else
        LOGGER.info ("[phase4] ResponseEntity is empty");
    }
    catch (final Exception ex)
    {
      LOGGER.error ("[phase4] Error sending message", ex);
      throw new MEException (EToopErrorCode.ME_001, ex);
    }
  }

  public void sendDCOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {
    LOGGER.info ("[phase4] sendDCOutgoing");
    // No difference
    _sendOutgoing (m_aCF, aRoutingInfo, aMessage);
  }

  public void sendDPOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {
    LOGGER.info ("[phase4] sendDPOutgoing");
    // No difference
    _sendOutgoing (m_aCF, aRoutingInfo, aMessage);
  }

  public void shutdown (@Nonnull final ServletContext aServletContext)
  {
    // Nothing to do here
  }
}
