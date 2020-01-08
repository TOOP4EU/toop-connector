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
package eu.toop.connector.app.r2d2;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.bdxrclient.BDXRClient;
import com.helger.peppol.bdxrclient.BDXRClientReadOnly;
import com.helger.peppol.smpclient.exception.SMPClientException;
import com.helger.peppol.url.PeppolDNSResolutionException;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xsds.bdxr.smp1.EndpointType;
import com.helger.xsds.bdxr.smp1.ProcessType;
import com.helger.xsds.bdxr.smp1.ServiceInformationType;
import com.helger.xsds.bdxr.smp1.SignedServiceMetadataType;

import eu.toop.commons.error.EToopErrorCode;
import eu.toop.connector.api.TCConfig;
import eu.toop.connector.api.TCSettings;
import eu.toop.connector.api.r2d2.IR2D2Endpoint;
import eu.toop.connector.api.r2d2.IR2D2EndpointProvider;
import eu.toop.connector.api.r2d2.IR2D2ErrorHandler;
import eu.toop.connector.api.r2d2.R2D2Endpoint;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * The default implementation of {@link IR2D2EndpointProvider} using the OASIS
 * BDXR SMP v1 lookup. It performs the query every time and does not cache
 * results!
 *
 * @author Philip Helger
 */
@Immutable
public class R2D2EndpointProviderBDXRSMP1 implements IR2D2EndpointProvider
{
  public R2D2EndpointProviderBDXRSMP1 ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IR2D2Endpoint> getEndpoints (@Nonnull final String sLogPrefix,
                                                    @Nonnull final IParticipantIdentifier aRecipientID,
                                                    @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                                    @Nonnull final IProcessIdentifier aProcessID,
                                                    @Nonnull @Nonempty final String sTransportProfileID,
                                                    @Nonnull final IR2D2ErrorHandler aErrorHandler)
  {
    ValueEnforcer.notNull (aRecipientID, "Recipient");
    ValueEnforcer.notNull (aDocumentTypeID, "DocumentTypeID");
    ValueEnforcer.notNull (aProcessID, "ProcessID");
    ValueEnforcer.notEmpty (sTransportProfileID, "TransportProfileID");
    ValueEnforcer.notNull (aErrorHandler, "ErrorHandler");

    ToopKafkaClient.send (EErrorLevel.INFO,
                          () -> sLogPrefix +
                                "SMP lookup (" +
                                aRecipientID.getURIEncoded () +
                                ", " +
                                aDocumentTypeID.getURIEncoded () +
                                ", " +
                                aProcessID.getURIEncoded () +
                                ", " +
                                sTransportProfileID +
                                ")");

    final ICommonsList <IR2D2Endpoint> ret = new CommonsArrayList <> ();
    try
    {
      BDXRClient aSMPClient;
      if (TCConfig.isR2D2UseDNS ())
      {
        // Use dynamic lookup via DNS - can throw exception
        aSMPClient = new BDXRClient (TCSettings.getSMPUrlProvider (), aRecipientID, TCConfig.getR2D2SML ());
      }
      else
      {
        // Use a constant SMP URL
        aSMPClient = new BDXRClient (TCConfig.getR2D2SMPUrl ());
      }

      // Query SMP
      final SignedServiceMetadataType aSG = aSMPClient.getServiceRegistration (aRecipientID, aDocumentTypeID);
      final ServiceInformationType aSI = aSG.getServiceMetadata ().getServiceInformation ();
      if (aSI != null)
      {
        // Find the first process that matches (should be only one!)
        final ProcessType aProcess = CollectionHelper.findFirst (aSI.getProcessList ().getProcess (),
                                                                 x -> SimpleProcessIdentifier.wrap (x.getProcessIdentifier ())
                                                                                             .hasSameContent (aProcessID));
        if (aProcess != null)
        {
          // Add all endpoints to the result list
          for (final EndpointType aEP : aProcess.getServiceEndpointList ().getEndpoint ())
            if (sTransportProfileID.equals (aEP.getTransportProfile ()))
            {
              // Convert String to X509Certificate
              X509Certificate aCert;
              if (true)
              {
                final CertificateFactory aCertificateFactory = CertificateHelper.getX509CertificateFactory ();
                aCert = (X509Certificate) aCertificateFactory.generateCertificate (new NonBlockingByteArrayInputStream (aEP.getCertificate ()));
              }
              else
                aCert = BDXRClientReadOnly.getEndpointCertificate (aEP);

              if (StringHelper.hasNoText (aEP.getEndpointURI ()))
              {
                ToopKafkaClient.send (EErrorLevel.WARN, () -> sLogPrefix + "SMP lookup result: endpoint has no URI");
                continue;
              }

              // Convert to our data structure
              final R2D2Endpoint aDestEP = new R2D2Endpoint (aRecipientID,
                                                             aEP.getTransportProfile (),
                                                             aEP.getEndpointURI (),
                                                             aCert);
              ret.add (aDestEP);

              ToopKafkaClient.send (EErrorLevel.INFO,
                                    () -> sLogPrefix +
                                          "SMP lookup result: " +
                                          aEP.getTransportProfile () +
                                          ", " +
                                          aEP.getEndpointURI ());
            }
        }
      }
      else
      {
        // else redirect
        ToopKafkaClient.send (EErrorLevel.INFO, () -> sLogPrefix + "SMP lookup result: maybe a redirect?");
      }
    }
    catch (final PeppolDNSResolutionException | SMPClientException ex)
    {
      aErrorHandler.onError (sLogPrefix +
                             "Error fetching SMP endpoint " +
                             aRecipientID.getURIEncoded () +
                             "/" +
                             aDocumentTypeID.getURIEncoded () +
                             "/" +
                             aProcessID.getURIEncoded (),
                             ex,
                             EToopErrorCode.DD_002);
    }
    catch (final CertificateException ex)
    {
      aErrorHandler.onError (sLogPrefix +
                             "Error validating the signature from SMP response for endpoint " +
                             aRecipientID.getURIEncoded () +
                             "/" +
                             aDocumentTypeID.getURIEncoded () +
                             "/" +
                             aProcessID.getURIEncoded (),
                             ex,
                             EToopErrorCode.DD_003);
    }
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
