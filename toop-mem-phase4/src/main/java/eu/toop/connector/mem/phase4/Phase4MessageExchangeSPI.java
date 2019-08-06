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
package eu.toop.connector.mem.phase4;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as4.CAS4;
import com.helger.as4.attachment.EAS4CompressionMode;
import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.client.AS4ClientSentMessage;
import com.helger.as4.client.AS4ClientUserMessage;
import com.helger.as4.client.IAS4ClientBuildMessageCallback;
import com.helger.as4.crypto.AS4CryptoFactory;
import com.helger.as4.crypto.AS4CryptoProperties;
import com.helger.as4.crypto.ECryptoAlgorithmSign;
import com.helger.as4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.as4.http.AS4HttpDebug;
import com.helger.as4.messaging.domain.MessageHelperMethods;
import com.helger.as4.mgr.MetaAS4Manager;
import com.helger.as4.model.pmode.PMode;
import com.helger.as4.model.pmode.PModeManager;
import com.helger.as4.model.pmode.PModePayloadService;
import com.helger.as4.model.pmode.resolve.DefaultPModeResolver;
import com.helger.as4.servlet.AS4ServerInitializer;
import com.helger.as4.soap.ESOAPVersion;
import com.helger.as4.util.AS4ResourceHelper;
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
import com.helger.datetime.util.PDTIOHelper;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.photon.app.io.WebFileIO;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;
import com.helger.servlet.ServletHelper;

import eu.toop.commons.error.EToopErrorCode;
import eu.toop.connector.api.as4.IMEIncomingHandler;
import eu.toop.connector.api.as4.IMERoutingInformation;
import eu.toop.connector.api.as4.IMessageExchangeSPI;
import eu.toop.connector.api.as4.MEException;
import eu.toop.connector.api.as4.MEMessage;
import eu.toop.connector.api.as4.MEPayload;
import eu.toop.connector.api.http.TCHttpClientFactory;
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

  public Phase4MessageExchangeSPI ()
  {}

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return ID;
  }

  @Nonnull
  private static AS4CryptoFactory _getCF ()
  {
    return AS4CryptoFactory.DEFAULT_INSTANCE;
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

    // Register server once
    AS4ServerInitializer.initAS4Server (DefaultPModeResolver.DEFAULT_PMODE_RESOLVER, _getCF ());

    final PModeManager aPModeMgr = MetaAS4Manager.getPModeMgr ();
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

  @Nonnull
  private static String _getCN (final String sPrincipal)
  {
    try
    {
      for (final Rdn aRdn : new LdapName (sPrincipal).getRdns ())
        if (aRdn.getType ().equalsIgnoreCase ("CN"))
          return (String) aRdn.getValue ();
    }
    catch (final InvalidNameException ex)
    {
      // Ignore
    }
    throw new IllegalStateException ("Failed to get CN from '" + sPrincipal + "'");
  }

  private void _sendOutgoing (@Nonnull final AS4CryptoProperties aCP,
                              @Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {
    final StopWatch aSW = StopWatch.createdStarted ();

    final X509Certificate aTheirCert = aRoutingInfo.getCertificate ();

    // Start crypto stuff
    {
      // Sanity check
      final LoadedKeyStore aLKS = KeyStoreHelper.loadKeyStore (aCP.getKeyStoreType (),
                                                               aCP.getKeyStorePath (),
                                                               aCP.getKeyStorePassword ());
      final KeyStore aOurKS = aLKS.getKeyStore ();
      if (aOurKS == null)
        throw new InitializationException ("Failed to load keystore: " + aLKS.getErrorText (Locale.US));

      final LoadedKey <KeyStore.PrivateKeyEntry> aLK = KeyStoreHelper.loadPrivateKey (aOurKS,
                                                                                      aCP.getKeyStorePath (),
                                                                                      aCP.getKeyAlias (),
                                                                                      aCP.getKeyPassword ()
                                                                                         .toCharArray ());
      final PrivateKeyEntry aOurCert = aLK.getKeyEntry ();
      if (aOurCert == null)
        throw new InitializationException ("Failed to load key: " + aLK.getErrorText (Locale.US));
    }

    try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
    {
      final AS4ClientUserMessage aClient = new AS4ClientUserMessage (aResHelper);
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
      aClient.setToPartyID (_getCN (aTheirCert.getSubjectDN ().getName ()));
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
      aClient.setHttpClientFactory (new TCHttpClientFactory ());

      // Main sending
      final IAS4ClientBuildMessageCallback aCallback = null;
      final AS4ClientSentMessage <byte []> aResponseEntity = aClient.sendMessageWithRetries (aRoutingInfo.getEndpointURL (),
                                                                                             new ResponseHandlerByteArray (),
                                                                                             aCallback);
      LOGGER.info ("[phase4] Successfully transmitted document with message ID '" +
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
        final String sFilename = PDTIOHelper.getCurrentLocalDateTimeForFilename () +
                                 "-" +
                                 FilenameHelper.getAsSecureValidASCIIFilename (sMessageID) +
                                 "-response.xml";
        final File aResponseFile = new File (Phase4Config.getSendResponseFolderName (), sFilename);
        if (SimpleFileIO.writeFile (aResponseFile, aResponseEntity.getResponse ()).isSuccess ())
          LOGGER.info ("[phase4] Response file was written to '" + aResponseFile.getAbsolutePath () + "'");
        else
          LOGGER.error ("[phase4] Error writing response file to '" + aResponseFile.getAbsolutePath () + "'");
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
    _sendOutgoing (_getCF ().getCryptoProperties (), aRoutingInfo, aMessage);
  }

  public void sendDPOutgoing (@Nonnull final IMERoutingInformation aRoutingInfo,
                              @Nonnull final MEMessage aMessage) throws MEException
  {
    LOGGER.info ("[phase4] sendDPOutgoing");
    // No difference
    _sendOutgoing (_getCF ().getCryptoProperties (), aRoutingInfo, aMessage);
  }

  public void shutdown (@Nonnull final ServletContext aServletContext)
  {
    // Nothing to do here
  }
}
