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
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.client.AS4ClientUserMessage;
import com.helger.as4.client.AbstractAS4Client.SentMessage;
import com.helger.as4.crypto.CryptoProperties;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.model.pmode.PModePayloadService;
import com.helger.as4.servlet.AS4ServerInitializer;
import com.helger.as4.servlet.mgr.AS4ServerSettings;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceManager;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.io.file.SimpleFileIO;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.servlet.ServletHelper;

import eu.toop.commons.error.EToopErrorCode;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.as4.IMERoutingInformation;
import eu.toop.connector.api.as4.IMessageExchangeSPI;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.api.as4.MEPayload;
import eu.toop.mem.phase4.config.TOOPPMode;

/**
 * {@link IMessageExchangeSPI} implementation using ph-as4
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class Phase4MessageExchangeSPI implements IMessageExchangeSPI
{
  public static final String ID = "mem-phase4";
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4MessageExchangeSPI.class);

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
      final String sServletContextPath = ServletHelper.getServletContextBasePath (aServletContext);

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
      final PMode aPMode = TOOPPMode.createTOOPMode ("AnyInitiatorID",
                                                     "AnyResponderID",
                                                     "AnyResponderAddress",
                                                     (i, r) -> "TOOP_PMODE",
                                                     false);
      aPMode.setPayloadService (new PModePayloadService (EAS4CompressionMode.GZIP));
      aPMode.getReceptionAwareness ().setRetry (false);
      aPMode.getLeg1 ().getBusinessInfo ().setAction ("Deliver");
      aPModeMgr.createOrUpdatePMode (aPMode);
    }
  }

  private void _sendOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {
    final StopWatch aSW = StopWatch.createdStarted ();

    final X509Certificate aTheirCert = aRoutingInfo.getCertificate ();

    // Start crypto stuff
    final CryptoProperties aCP = AS4ServerSettings.getAS4CryptoFactory ().getCryptoProperties ();
    final KeyStore aOurKS = KeyStoreHelper.loadKeyStore (aCP.getKeyStoreType (),
                                                         aCP.getKeyStorePath (),
                                                         aCP.getKeyStorePassword ())
                                          .getKeyStore ();
    if (aOurKS == null)
      throw new InitializationException ("Failed to load keystore");

    final PrivateKeyEntry aOurCert = KeyStoreHelper.loadPrivateKey (aOurKS,
                                                                    aCP.getKeyStorePath (),
                                                                    aCP.getKeyAlias (),
                                                                    aCP.getKeyPassword ().toCharArray ())
                                                   .getKeyEntry ();
    if (aOurCert == null)
      throw new InitializationException ("Failed to load key");

    final AS4ResourceManager aResMgr = new AS4ResourceManager ();
    final AS4ClientUserMessage aClient = new AS4ClientUserMessage (aResMgr);
    aClient.setSOAPVersion (ESOAPVersion.SOAP_12);

    // Keystore data
    IReadableResource aRes = new ClassPathResource (aCP.getKeyStorePath ());
    if (!aRes.exists ())
      aRes = new FileSystemResource (aCP.getKeyStorePath ());
    aClient.setKeyStoreResource (aRes);
    aClient.setKeyStorePassword (aCP.getKeyStorePassword ());
    aClient.setKeyStoreType (aCP.getKeyStoreType ());
    aClient.setKeyStoreAlias (aCP.getKeyAlias ());
    aClient.setKeyStoreKeyPassword (aCP.getKeyPassword ());

    aClient.setCryptoAlgorithmSign (ECryptoAlgorithmSign.RSA_SHA_512);
    aClient.setCryptoAlgorithmSignDigest (ECryptoAlgorithmSignDigest.DIGEST_SHA_512);

    aClient.setAction (aRoutingInfo.getDocumentTypeID ().getURIEncoded ());
    aClient.setServiceType (null);
    aClient.setServiceValue (aRoutingInfo.getProcessID ().getURIEncoded ());
    aClient.setConversationID (MessageHelperMethods.createRandomConversationID ());
    aClient.setAgreementRefValue (null);

    aClient.setFromRole ("http://www.toop.eu/edelivery/gateway");
    aClient.setFromPartyID (TCConfig.getMEMAS4TcPartyid ());
    aClient.setToRole ("http://www.toop.eu/edelivery/gateway");
    aClient.setToPartyID (TCConfig.getMEMAS4GwPartyID ());
    aClient.setPayload (null);

    for (final MEPayload aPayload : aMessage.payloads ())
    {
      try
      {
        aClient.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aPayload.getData ().bytes (),
                                                                             "",
                                                                             aPayload.getMimeType (),
                                                                             null,
                                                                             aResMgr));
      }
      catch (final IOException ex)
      {
        throw new MEException (EToopErrorCode.ME_001, ex);
      }
    }

    try
    {
      final SentMessage <byte []> aResponseEntity = aClient.sendMessage (aRoutingInfo.getEndpointURL (),
                                                                         new ResponseHandlerByteArray ());
      LOGGER.info ("Successfully transmitted document with message ID '" +
                   aResponseEntity.getMessageID () +
                   "' for '" +
                   aRoutingInfo.getReceiverID ().getURIEncoded () +
                   "' to '" +
                   aRoutingInfo.getEndpointURL () +
                   "' in " +
                   aSW.stopAndGetMillis () +
                   " ms");

      if (aResponseEntity.hasResponse ())
      {
        final String sMessageID = aResponseEntity.getMessageID ();
        final String sFilename = FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) + "-response.xml";
        final File aResponseFile = new File (sFilename);
        if (SimpleFileIO.writeFile (aResponseFile, aResponseEntity.getResponse ()).isSuccess ())
          LOGGER.info ("Response file was written to '" + aResponseFile.getAbsolutePath () + "'");
        else
          LOGGER.error ("Error writing response file to '" + aResponseFile.getAbsolutePath () + "'");
      }
    }
    catch (final Exception ex)
    {
      throw new MEException (EToopErrorCode.ME_001, ex);
    }

  }

  public void sendDCOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {
    LOGGER.info ("[phase4] sendDCOutgoing");
    // No difference
    _sendOutgoing (aRoutingInfo, aMessage);
  }

  public void sendDPOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {
    LOGGER.info ("[phase4] sendDPOutgoing");
    // No difference
    _sendOutgoing (aRoutingInfo, aMessage);
  }

  public void shutdown (@Nonnull final ServletContext aServletContext)
  {}
}
