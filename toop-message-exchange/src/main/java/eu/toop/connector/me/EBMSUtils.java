/**
 * Copyright (C) 2018 toop.eu <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package eu.toop.connector.me;

import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CharsetHelper;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.commons.string.StringHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xml.transform.TransformSourceFactory;

import eu.toop.connector.api.TCConfig;
import eu.toop.kafkaclient.ToopKafkaClient;

public final class EBMSUtils {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (EBMSUtils.class);
  // SOAP 1.2 NS
  public static final String NS_SOAPENV = "http://www.w3.org/2003/05/soap-envelope";
  public static final String NS_EBMS = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";

  private EBMSUtils () {
  }

  /**
   * See
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/os/AS4-profile-v1.0-os.html#__RefHeading__26454_1909778835
   */
  public static byte[] createSuccessReceipt (final SOAPMessage message) {
    ValueEnforcer.notNull (message, "SOAPMessage");

    try {
      final StreamSource stylesource = TransformSourceFactory.create (new ClassPathResource ("/receipt-generator.xslt"));
      final Transformer transformer = TransformerFactory.newInstance ().newTransformer (stylesource);
      transformer.setParameter ("messageid", genereateEbmsMessageId (TCConfig.getMEMAS4IDSuffix ()));
      transformer.setParameter ("timestamp", DateTimeUtils.getCurrentTimestamp ());
      try (final NonBlockingByteArrayOutputStream baos = new NonBlockingByteArrayOutputStream ()) {
        transformer.transform (new DOMSource (message.getSOAPPart ()), new StreamResult (baos));
        return baos.toByteArray ();
      }
    } catch (final RuntimeException ex) {
      // throw RTE's directly
      throw ex;
    } catch (final Exception ex) {
      // force exceptions to runtime
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  /**
   * Create a fault message based on the error message
   *
   * @param soapMessage
   *          Source SOAP message. May be <code>null</code>
   * @param faultMessage
   *          Fault message. May be <code>null</code>.
   * @return byte[] with result XML SOAP Fault message
   */
  public static byte[] createFault (@Nullable final SOAPMessage soapMessage, @Nullable final String faultMessage) {
    final String fm = faultMessage != null ? faultMessage : "Unknown Error";
    String refToMessageInError;
    try {
      if (soapMessage != null) {
        final Node element = SoapXPathUtil.safeFindSingleNode (soapMessage.getSOAPHeader (),
                                                               "//:MessageInfo/:MessageId");
        refToMessageInError = element.getTextContent ();
      } else {
        refToMessageInError = "";
      }
    } catch (final SOAPException e) {
      throw new IllegalStateException (e.getMessage (), e);
    }

    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eEnvelope = aDoc.appendElement (NS_SOAPENV, "Envelope");
    {
      final IMicroElement eHeader = eEnvelope.appendElement (NS_SOAPENV, "Head");
      final IMicroElement eMessaging = eHeader.appendElement (NS_EBMS, "Messaging");
      final IMicroElement eSignalMessage = eMessaging.appendElement (NS_EBMS, "SignalMessage");
      {
        final IMicroElement eMessageInfo = eSignalMessage.appendElement (NS_EBMS, "MessageInfo");
        eMessageInfo.appendElement (NS_EBMS, "Timestamp").appendText (DateTimeUtils.getCurrentTimestamp ());
        final String ebmsMessageId = genereateEbmsMessageId (TCConfig.getMEMAS4IDSuffix ());
        eMessageInfo.appendElement (NS_EBMS, "MessageId").appendText (ebmsMessageId);
      }
      {
        final IMicroElement eError = eSignalMessage.appendElement (NS_EBMS, "Error");
        eError.setAttribute ("category", "CONTENT");
        eError.setAttribute ("errorCode", "EBMS:0004");
        eError.setAttribute ("origin", "ebms");
        eError.setAttribute ("refToMessageInError", refToMessageInError);
        eError.setAttribute ("severity", "failure");
        eError.setAttribute ("shortDescription", "Error");
        eError.appendElement (NS_EBMS, "Description").setAttribute (XMLConstants.XML_NS_URI, "lang", "en")
              .appendText (fm);
        eError.appendElement (NS_EBMS, "ErrorDetail").appendText (fm);
      }
    }
    {
      final IMicroElement eBody = eEnvelope.appendElement (NS_SOAPENV, "Body");
      final IMicroElement eFault = eBody.appendElement (NS_SOAPENV, "Fault");
      {
        final IMicroElement eCode = eFault.appendElement (NS_SOAPENV, "Code");
        eCode.appendElement (NS_SOAPENV, "Value").appendText ("env:Receiver");
      }
      {
        final IMicroElement eReason = eFault.appendElement (NS_SOAPENV, "Reason");
        eReason.appendElement (NS_SOAPENV, "Text").setAttribute (XMLConstants.XML_NS_URI, "lang", "en").appendText (fm);
      }
    }

    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
    aNSCtx.addMapping ("env", NS_SOAPENV);
    aNSCtx.addMapping ("eb", NS_EBMS);

    return MicroWriter.getNodeAsBytes (aDoc, new XMLWriterSettings ().setNamespaceContext (aNSCtx));
  }

  /**
   * Generate a random ebms message id with the format
   * <code>&lt;RANDOM UUID&gt;@ext</code>
   *
   * @param ext
   *          suffix to use. May not be <code>null</code>.
   * @return EBMS Message ID. Never <code>null</code> nor empty.
   */
  public static String genereateEbmsMessageId (final String ext) {
    return UUID.randomUUID ().toString () + "@" + ext;
  }

  @Nullable
  private static IMicroElement _property (@Nonnull final String sName, @Nullable final String sValue) {
    if (sValue == null) {
      return null;
    }

    final IMicroElement ret = new MicroElement (NS_EBMS, "Property");
    ret.setAttribute ("name", sName).appendText (sValue);
    return ret;
  }

  /**
   * The conversion procedure goes here
   */
  public static SOAPMessage convert2MEOutboundAS4Message (final SubmissionData metadata, final MEMessage meMessage) {
    if (LOG.isDebugEnabled ()) {
      LOG.debug ("Convert submission data to SOAP Message");
    }

    try {
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement eMessaging = aDoc.appendElement (NS_EBMS, "Messaging");
      eMessaging.setAttribute (NS_SOAPENV, "mustUnderstand", "true");
      final IMicroElement eUserMessage = eMessaging.appendElement (NS_EBMS, "UserMessage");

      {
        final IMicroElement eMessageInfo = eUserMessage.appendElement (NS_EBMS, "MessageInfo");
        eMessageInfo.appendElement (NS_EBMS, "Timestamp").appendText (DateTimeUtils.getCurrentTimestamp ());
        final String ebmsMessageId = genereateEbmsMessageId (TCConfig.getMEMAS4IDSuffix ());
        eMessageInfo.appendElement (NS_EBMS, "MessageId").appendText (ebmsMessageId);
      }
      {
        final IMicroElement ePartyInfo = eUserMessage.appendElement (NS_EBMS, "PartyInfo");
        {
          final IMicroElement eFrom = ePartyInfo.appendElement (NS_EBMS, "From");
          eFrom.appendElement (NS_EBMS, "PartyId")
               // .setAttribute ("type", "urn:oasis:names:tc:ebcore:partyid-type:unregistered")
               .appendText (TCConfig.getMEMAS4FromPartyID ());
          eFrom.appendElement (NS_EBMS, "Role").appendText (TCConfig.getMEMAS4FromRole ());
        }
        {
          final IMicroElement eTo = ePartyInfo.appendElement (NS_EBMS, "To");
          eTo.appendElement (NS_EBMS, "PartyId")
             // .setAttribute ("type", "urn:oasis:names:tc:ebcore:partyid-type:unregistered")
             .appendText (TCConfig.getMEMAS4ToPartyID ());
          eTo.appendElement (NS_EBMS, "Role").appendText (TCConfig.getMEMAS4ToRole ());
        }
      }
      {
        final IMicroElement eCollaborationInfo = eUserMessage.appendElement (NS_EBMS, "CollaborationInfo");
        eCollaborationInfo.appendElement (NS_EBMS, "Service").appendText (TCConfig.getMEMAS4Service ());
        eCollaborationInfo.appendElement (NS_EBMS, "Action").appendText (TCConfig.getMEMAS4Action ());
        eCollaborationInfo.appendElement (NS_EBMS, "ConversationId").appendText (metadata.conversationId);
      }

      {
        final IMicroElement eMessageProperties = eUserMessage.appendElement (NS_EBMS, "MessageProperties");
        eMessageProperties.appendChild (_property ("MessageId", metadata.messageId));
        eMessageProperties.appendChild (_property ("ConversationId", metadata.conversationId));
        eMessageProperties.appendChild (_property ("RefToMessageId", metadata.refToMessageId));
        eMessageProperties.appendChild (_property ("Service", metadata.service));
        eMessageProperties.appendChild (_property ("Action", metadata.action));
        eMessageProperties.appendChild (_property ("ToPartyId", metadata.to));
        eMessageProperties.appendChild (_property ("ToPartyRole", metadata.toPartyRole));
        eMessageProperties.appendChild (_property ("FromPartyId", metadata.from));
        eMessageProperties.appendChild (_property ("FromPartyRole", metadata.fromPartyRole));
        eMessageProperties.appendChild (_property ("originalSender", metadata.originalSender));
        eMessageProperties.appendChild (_property ("finalRecipient", metadata.finalRecipient));
      }
      {
        final IMicroElement ePayloadInfo = eUserMessage.appendElement (NS_EBMS, "PayloadInfo");
        for (final MEPayload aPayload : meMessage.getPayloads ()) {
          final IMicroElement ePartInfo = ePayloadInfo.appendElement (NS_EBMS, "PartInfo");
          ePartInfo.setAttribute ("href", "cid:" + aPayload.getPayloadId ());

          final IMicroElement ePartProperties = ePartInfo.appendElement (NS_EBMS, "PartProperties");
          ePartProperties.appendChild (_property ("MimeType", aPayload.getMimeTypeString ()));
        }
      }

      final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext ();
      aNSCtx.addMapping ("env", NS_SOAPENV);
      aNSCtx.addMapping ("eb", NS_EBMS);

      // Convert to org.w3c.dom ....
      final Element element = DOMReader.readXMLDOM (MicroWriter.getNodeAsBytes (aDoc,
                                                                                new XMLWriterSettings ().setNamespaceContext (aNSCtx)))
                                       .getDocumentElement ();

      // create a soap message based on this XML
      final SOAPMessage message = SoapUtil.createEmptyMessage ();
      final Node importNode = message.getSOAPHeader ().getOwnerDocument ().importNode (element, true);
      message.getSOAPHeader ().appendChild (importNode);

      meMessage.getPayloads ().forEach (payload -> {
        final AttachmentPart attachmentPart = message.createAttachmentPart ();
        attachmentPart.setContentId ('<' + payload.getPayloadId () + '>');
        try {
          final byte[] data = payload.getData ();
          attachmentPart.setRawContentBytes (data, 0, data.length, payload.getMimeTypeString ());
        } catch (final SOAPException e) {
          throw new IllegalStateException (e);
        }
        message.addAttachmentPart (attachmentPart);
      });

      if (message.saveRequired ()) {
        message.saveChanges ();
      }

      if (LOG.isTraceEnabled ()) {
        LOG.trace (SoapUtil.describe (message));
      }
      return message;
    } catch (final Exception ex) {
      throw new IllegalStateException (ex);
    }
  }

  /**
   * Process the inbound SOAPMessage and convert it to the MEMEssage
   *
   * @param message
   *          the soap message to be converted to a MEMessage. Cannot be null
   * @return the MEMessage object created from the supplied SOAPMessage
   * @throws Exception
   *           in case of error
   */
  public static MEMessage soap2MEMessage (@Nonnull final SOAPMessage message) throws Exception {
    ValueEnforcer.notNull (message, "SOAPMessage");

    if (LOG.isDebugEnabled ()) {
      LOG.debug ("Convert message to submission data");
    }

    final MEMessage meMessage = new MEMessage ();
    if (message.countAttachments () > 0) {
      // Read all attachments
      message.getAttachments ().forEachRemaining (attObj -> {
        final AttachmentPart att = (AttachmentPart) attObj;
        // remove surplus characters
        final String href = att.getContentId ().replaceAll ("<|>", "");
        Node partInfo;
        try {
          // throws exception if part info does not exist
          partInfo = SoapXPathUtil.safeFindSingleNode (message.getSOAPHeader (),
                                                       "//:PayloadInfo/:PartInfo[@href='cid:" + href + "']");
        } catch (final Exception ex) {
          throw new IllegalStateException ("ContentId: " + href + " was not found in PartInfo");
        }

        MimeType mimeType;
        try {
          final Node singleNode = SoapXPathUtil.safeFindSingleNode (partInfo,
                                                                    ".//:PartProperties/:Property[@name='MimeType']/text()");
          String sMimeType = singleNode.getNodeValue ();
          if (sMimeType.startsWith ("cid:")) {
            sMimeType = sMimeType.substring (4);
          }

          mimeType = MimeTypeParser.parseMimeType (sMimeType);
        } catch (final Exception ex) {
          LOG.warn ("Error parsing MIME type: " + ex.getMessage ());
          // if there is a problem wrt the processing of the mimetype, simply grab the
          // content type
          // FIXME: Do not swallow the error, there might a problem with the mimtype
          mimeType = MimeTypeParser.parseMimeType (att.getContentType ());
        }

        try {
          final Node charSetNode = SoapXPathUtil.findSingleNode (partInfo,
                                                                 ".//:PartProperties/:Property[@name='CharacterSet']/text()");
          if (charSetNode != null) {
            final Charset aCharset = CharsetHelper.getCharsetFromNameOrNull (charSetNode.getNodeValue ());
            if (aCharset != null) {
              // Add charset to MIME type
              mimeType.addParameter (CMimeType.PARAMETER_NAME_CHARSET, aCharset.name ());
            }
          }
        } catch (final IllegalStateException ex) {
          // ignore
        }

        byte[] rawContentBytes;
        try {
          rawContentBytes = att.getRawContentBytes ();
        } catch (final SOAPException e) {
          throw new IllegalStateException (e);
        }

        final MEPayload payload = new MEPayload (mimeType, href, rawContentBytes);
        if (LOG.isDebugEnabled ()) {
          LOG.debug ("\tpayload.payloadId: " + payload.getPayloadId ());
          LOG.debug ("\tpayload.mimeType: " + payload.getMimeTypeString ());
        }

        meMessage.getPayloads ().add (payload);
      });
    }
    return meMessage;
  }

  /**
   * process the GatewayRoutingMetadata object and obtain the actual submission
   * data
   *
   * @return SubmissionData
   */
  static SubmissionData inferSubmissionData (final GatewayRoutingMetadata gatewayRoutingMetadata) {
    final X509Certificate certificate = gatewayRoutingMetadata.getEndpoint ().getCertificate ();
    // we need the certificate to obtain the to party id
    ValueEnforcer.notNull (certificate, "Endpoint Certificate");
    final SubmissionData submissionData = new SubmissionData ();
    submissionData.messageId = genereateEbmsMessageId (TCConfig.getMEMAS4IDSuffix ());
    submissionData.action = gatewayRoutingMetadata.getDocumentTypeId ();
    submissionData.service = gatewayRoutingMetadata.getProcessId ();

    // LdapName ldapDN;
    // try {
    // ldapDN = new LdapName(certificate.getSubjectX500Principal().getName());
    // } catch (final InvalidNameException e) {
    // throw new IllegalArgumentException("Invalid certificate name", e);
    // }

    // TODO: get it from the certificate
    submissionData.to = TCConfig.getMEMAS4ReceivingPartyID ();
    submissionData.from = TCConfig.getMEMAS4ToPartyID ();
    // ldapDN.getRdn(0).getValue().toString();

    // TODO: infer it from the transaction id
    submissionData.conversationId = "1";

    // TODO: read if from config maybe
    submissionData.toPartyRole = "http://toop.eu/identifiers/roles/dc";
    submissionData.fromPartyRole = "http://toop.eu/identifiers/roles/dp";
    // FIXME: The original sender must be some real value
    submissionData.originalSender = "var1::var2";
    submissionData.finalRecipient = gatewayRoutingMetadata.getEndpoint ().getParticipantID ().getURIEncoded ();

    return submissionData;
  }

  /**
   * Calls {@link SoapUtil#sendSOAPMessage(SOAPMessage, URL)} but checks the
   * return value for a fault or a receipt.
   *
   * @param soapMessage
   *          Message to be sent. May not be <code>null</code>
   * @param url
   *          Target URL. May not be <code>null</code>
   * @throws IllegalStateException
   *           if a fault is received instead of an ebms receipt
   */
  public static void sendSOAPMessage (@Nonnull final SOAPMessage soapMessage, @Nonnull final URL url) {
    ValueEnforcer.notNull (soapMessage, "SOAP Message");
    ValueEnforcer.notNull (url, "Target url");

    if (false) LOG.info (SoapUtil.describe (soapMessage));
    final SOAPMessage response = SoapUtil.sendSOAPMessage (soapMessage, url);

    if (response != null) {
      if (false) LOG.info (SoapUtil.describe (response));
      validateReceipt (response);
    } // else the receipt is null and we received a HTTP.OK, isn't that great?
  }

  /**
   * Check if the response is a soap fault (i.e. the response contains an error)
   */
  private static void validateReceipt (@Nonnull final SOAPMessage response) {

    ValueEnforcer.notNull (response, "Soap message");
    final Element errorElement = (Element) SoapXPathUtil.findSingleNode (response.getSOAPPart (),
                                                                         "//:SignalMessage/:Error");

    if (errorElement != null) {
      final String cat = StringHelper.getNotNull (errorElement.getAttribute ("category")).toUpperCase ();
      final String shortDescription = StringHelper.getNotNull (errorElement.getAttribute ("shortDescription"))
                                                  .toUpperCase ();
      final String severity = StringHelper.getNotNull (errorElement.getAttribute ("severity")).toUpperCase ();
      final String code = StringHelper.getNotNull (errorElement.getAttribute ("errorCode")).toUpperCase ();

      final StringBuilder errBuff = new StringBuilder ();
      if (!"EBMS:0303".equals (code)) {
        errBuff.append ("Error code invalid. Expected [EBMS:0303], Received: [" + code + "]\n");
      }
      if (!"FAILURE".equals (severity)) {
        errBuff.append ("Severity invalid. Expected [Failure], Received: [" + severity + "]\n");
      }
      if (!"COMMUNICATION".equals (cat)) {
        errBuff.append ("Invalid category . Expected [Communication], Received: [" + cat + "]\n");
      }
      if (!"DECOMPRESSIONFAILURE".equals (shortDescription)) {
        errBuff.append ("ShortDescription invalid. Expected [DecompressionFailure], Received: [" + shortDescription
                        + "]\n");
      }

      if (errBuff.length () > 0) {
        ToopKafkaClient.send (EErrorLevel.ERROR, () -> "Error from AS4 transmission: " + errBuff.toString ());
        throw new IllegalStateException (errBuff.toString ());
      }
    } else {
      // Short info that it worked
      ToopKafkaClient.send (EErrorLevel.INFO, () -> "AS4 transmission seemed to have worked out fine");
    }
  }
}
