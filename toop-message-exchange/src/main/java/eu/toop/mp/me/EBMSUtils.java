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
package eu.toop.mp.me;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.xml.XMLConstants;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CharsetHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroWriter;
import com.helger.xml.namespace.MapBasedNamespaceContext;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xml.transform.TransformSourceFactory;

import eu.toop.mp.api.MPConfig;

public final class EBMSUtils {
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger (EBMSUtils.class);
  public static final String NS_SOAPENV = "http://www.w3.org/2003/05/soap-envelope";
  public static final String NS_EBMS = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";

  private EBMSUtils () {
  }

  /**
   * See
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/os/AS4-profile-v1.0-os.html#__RefHeading__26454_1909778835
   *
   * @param message
   * @return
   */
  public static byte[] createSuccessReceipt (final SOAPMessage message) {
    ValueEnforcer.notNull (message, "SOAPMessage");

    try {
      final StreamSource stylesource = TransformSourceFactory.create (new ClassPathResource ("/receipt-generator.xslt"));
      final Transformer transformer = TransformerFactory.newInstance ().newTransformer (stylesource);
      transformer.setParameter ("messageid", genereateEbmsMessageId (MPConfig.getMEMAS4IDSuffix ()));
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
   * @param soapMessage Source SOAP message
   * @param faultMessage Fault message. May be <code>null</code>.
   * @return byte[] with result XML SOAP Fault message
   */
  public static byte[] createFault (@Nonnull final SOAPMessage soapMessage, @Nullable final String faultMessage) {
    ValueEnforcer.notNull (soapMessage, "SOAPMessage");

    final String fm = faultMessage != null ? faultMessage : "Unknown Error";
    String refToMessageInError;
    try {
      final Node element = SoapXPathUtil.findSingleNode (soapMessage.getSOAPHeader (), "//:MessageInfo/:MessageId");
      refToMessageInError = element.getTextContent ();
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
        final String ebmsMessageId = genereateEbmsMessageId (MPConfig.getMEMAS4IDSuffix ());
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
   * @param ext suffix to use. May not be <code>null</code>.
   * @return EBMS Message ID. Never <code>null</code> nor empty.
   */
  public static String genereateEbmsMessageId (final String ext) {
    return UUID.randomUUID ().toString () + "@" + ext;
  }

  /**
   * The conversion procedure goes here
   *
   * @param metadata
   * @param meMessage
   * @return
   */
  public static SOAPMessage convert2MEOutboundAS4Message (final SubmissionData metadata, final MEMessage meMessage) {
    try {
      if (LOG.isDebugEnabled ())
        LOG.debug ("Convert submission data to SOAP Message");
      String xml = StreamHelper.getAllBytesAsString (EBMSUtils.class.getResourceAsStream ("/as4template.xml"),
                                                     StandardCharsets.UTF_8);

      final String keyTimeStamp = "${timeStamp}";
      final String keyMessageId = "${ebmsMessageID}";
      final String keyFrom = "${from}";
      final String keyFromPartyRole = "${fromRole}";
      final String keyTo = "${to}";
      final String keyToPartyRole = "${toRole}";
      final String keyAction = "${action}";
      final String keyService = "${service}";
      final String keyMessageProps = "${messageProperties}";
      final String keyPartInfo = "${partInfo}";
      final String keyConversationId = "${conversationId}";

      final String conversationId = metadata.conversationId;

      // @formatter:off
      xml = xml.replace (keyTimeStamp, DateTimeUtils.getCurrentTimestamp ())
               .replace (keyMessageId, genereateEbmsMessageId (MPConfig.getMEMAS4IDSuffix ()));
      if (conversationId != null)
        xml = xml.replace (keyConversationId, conversationId);
      xml = xml
               .replace (keyFrom, MPConfig.getMEMAS4FromPartyID ())
               .replace (keyFromPartyRole, MPConfig.getMEMAS4FromRole ())
               .replace (keyTo, MPConfig.getMEMAS4ToPartyID ())
               .replace (keyToPartyRole, MPConfig.getMEMAS4ToRole ())
               .replace (keyAction, MPConfig.getMEMAS4Action ())
               .replace (keyService, MPConfig.getMEMAS4Service ())
               .replace (keyMessageProps, generateMessageProperties (metadata))
               .replace (keyPartInfo, generatePartInfo (meMessage));
      // @formatter:off
      if (LOG.isDebugEnabled ())
        LOG.debug (xml);

      final Document document = DOMReader.readXMLDOM (xml);

      // create a soap message based on this XML
      final SOAPMessage message = SoapUtil.createEmptyMessage ();
      final Element element = document.getDocumentElement ();
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

      if (message.saveRequired ())
        message.saveChanges ();

      if (LOG.isTraceEnabled ())
        LOG.trace (SoapUtil.describe (message));
      return message;
    } catch (final RuntimeException ex) {
      // throw RTE's directly
      throw ex;
    } catch (final Exception ex) {
      // force exceptions to runtime
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  /**
   * <table>
   * <tr>
   * <th>Property name</th>
   * <th>Required?</th>
   * </tr>
   * <tr>
   * <td>MessageId</td>
   * <td>M (should be Y)</td>
   * </tr>
   * <tr>
   * <td>ConversationId</td>
   * <td>Y</td>
   * </tr>
   * <tr>
   * <td>RefToMessageId</td>
   * <td>N</td>
   * </tr>
   * <tr>
   * <td>ToPartyId</td>
   * <td>Y</td>
   * </tr>
   * <tr>
   * <td>ToPartyRole</td>
   * <td>Y</td>
   * </tr>
   * <tr>
   * <td>Service</td>
   * <td>Y</td>
   * </tr>
   * <tr>
   * <td>ServiceType</td>
   * <td>N // not used</td>
   * </tr>
   * <tr>
   * <td>Action</td>
   * <td>Y</td>
   * </tr>
   * <tr>
   * <td>originalSender</td>
   * <td>Y</td>
   * </tr>
   * <tr>
   * <td>finalRecipient</td>
   * <td>Y</td>
   * </tr>
   * </table>
   */
  public static String generateMessageProperties (final SubmissionData submissionData) {
    final StringBuilder propertiesBuilder = new StringBuilder ();
    propertiesBuilder.append ("      <ns2:Property name=\"MessageId\">").append (submissionData.messageId)
                     .append ("</ns2:Property>\n");

    propertiesBuilder.append ("      <ns2:Property name=\"ConversationId\">").append (submissionData.conversationId)
                     .append ("</ns2:Property>\n");

    if (submissionData.refToMessageId != null) {
      propertiesBuilder.append ("      <ns2:Property name=\"RefToMessageId\">").append (submissionData.refToMessageId)
                       .append ("</ns2:Property>\n");
    }

    propertiesBuilder.append ("      <ns2:Property name=\"Service\">").append (submissionData.service)
                     .append ("</ns2:Property>\n");
    propertiesBuilder.append ("      <ns2:Property name=\"Action\">").append (submissionData.action)
                     .append ("</ns2:Property>\n");
    propertiesBuilder.append ("      <ns2:Property name=\"ToPartyId\">").append (submissionData.to)
                     .append ("</ns2:Property>\n");
    propertiesBuilder.append ("      <ns2:Property name=\"ToPartyRole\">").append (submissionData.toPartyRole)
                     .append ("</ns2:Property>\n");
    // the GW is the sender of the C2 ---> C3 Message
    propertiesBuilder.append ("      <ns2:Property name=\"FromPartyId\">").append (MPConfig.getMEMAS4ToPartyID ())
                     .append ("</ns2:Property>\n");
    propertiesBuilder.append ("      <ns2:Property name=\"FromPartyRole\">").append (submissionData.fromPartyRole)
                     .append ("</ns2:Property>\n");
    propertiesBuilder.append ("      <ns2:Property name=\"originalSender\">").append (submissionData.originalSender)
                     .append ("</ns2:Property>\n");
    propertiesBuilder.append ("      <ns2:Property name=\"finalRecipient\">").append (submissionData.finalRecipient)
                     .append ("</ns2:Property>");

    return propertiesBuilder.toString ();
  }

  /**
   * Generate PayloadInfo/PartInfo part of the UserMessage
   *
   * @param meMessage
   * @return
   */
  public static String generatePartInfo (final MEMessage meMessage) {
    final StringBuilder partInfoBuilder = new StringBuilder ();

    meMessage.getPayloads ().forEach (mEPayload -> {
      partInfoBuilder.append ("<ns2:PartInfo href=\"cid:").append (mEPayload.getPayloadId ())
                     .append ("\">\n        <ns2:PartProperties>\n          <ns2:Property name=\"MimeType\">")
                     .append (mEPayload.getMimeType ()).append ("</ns2:Property>")
                     .append ("\n        </ns2:PartProperties>\n").append ("      </ns2:PartInfo>");
    });
    return partInfoBuilder.toString ();
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

    if (LOG.isDebugEnabled ())
      LOG.debug ("Convert message to submission data");

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
          partInfo = SoapXPathUtil.findSingleNode (message.getSOAPHeader (),
                                                   "//:PayloadInfo/:PartInfo[@href='cid:" + href + "']");
        } catch (final Exception ex) {
          throw new IllegalStateException ("ContentId: " + href + " was not found in PartInfo");
        }

        MimeType mimeType;
        try {
          final Node singleNode = SoapXPathUtil.findSingleNode (partInfo,
                                                                ".//:PartProperties/:Property[@name='MimeType']/text()");
          String sMimeType = singleNode.getNodeValue ();
          if (sMimeType.startsWith ("cid:"))
            sMimeType = sMimeType.substring (4);

          mimeType = MimeTypeParser.parseMimeType (sMimeType);
        } catch (final Exception ex) {
          LOG.warn ("Error parsing MIME type: " + ex.getMessage ());
          // if there is a problem wrt the processing of the mimetype, simply grab the
          // content type
          // FIXME: Do not swallow the error, there might a problem with the mimtype
          mimeType = MimeTypeParser.parseMimeType (att.getContentType ());
        }

        final Node charSetNode = SoapXPathUtil.findSingleNode (partInfo,
                                                               ".//:PartProperties/:Property[@name='CharacterSet']/text()");
        final Charset aCharset = CharsetHelper.getCharsetFromNameOrNull (charSetNode.getNodeValue ());
        if (aCharset != null) {
          // Add charset to MIME type
          mimeType.addParameter (CMimeType.PARAMETER_NAME_CHARSET, aCharset.name ());
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
   * @param gatewayRoutingMetadata
   * @return SubmissionData
   */
  static SubmissionData inferSubmissionData (final GatewayRoutingMetadata gatewayRoutingMetadata) {
    final X509Certificate certificate = gatewayRoutingMetadata.getEndpoint ().getCertificate ();
    // we need the certificate to obtain the to party id
    ValueEnforcer.notNull (certificate, "Endpoint Certificate");
    final SubmissionData submissionData = new SubmissionData ();
    submissionData.messageId = genereateEbmsMessageId (MPConfig.getMEMAS4IDSuffix ());
    submissionData.action = gatewayRoutingMetadata.getDocumentTypeId ();
    submissionData.service = gatewayRoutingMetadata.getProcessId ();

    LdapName ldapDN;
    try {
      ldapDN = new LdapName (certificate.getSubjectX500Principal ().getName ());
    } catch (final InvalidNameException e) {
      throw new IllegalArgumentException ("Invalid certificate name", e);
    }
    // XXX I'm not sure that is what we want
    submissionData.to = ldapDN.getRdn (0).getValue ().toString ();

    // TODO: infer it from the transaction id
    submissionData.conversationId = "1";

    // TODO: read if from config maybe
    submissionData.toPartyRole = "http://toop.eu/identifiers/roles/dc";
    submissionData.fromPartyRole = "http://toop.eu/identifiers/roles/dp";
    // FIXME: The original sender must be some real value
    submissionData.originalSender = "originalSender";
    submissionData.finalRecipient = gatewayRoutingMetadata.getEndpoint ().getParticipantID ().getURIEncoded ();

    return submissionData;
  }
}
