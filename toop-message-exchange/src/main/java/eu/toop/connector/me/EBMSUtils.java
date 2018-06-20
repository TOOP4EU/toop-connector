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
package eu.toop.connector.me;

import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
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
import com.helger.commons.mime.MimeTypeParserException;
import com.helger.commons.regex.RegExHelper;
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
import eu.toop.connector.me.notifications.RelayResult;
import eu.toop.connector.me.notifications.SubmissionResult;
import eu.toop.connector.r2d2client.IR2D2Endpoint;
import eu.toop.kafkaclient.ToopKafkaClient;

public final class EBMSUtils {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EBMSUtils.class);
  // SOAP 1.2 NS
  public static final String NS_SOAPENV = "http://www.w3.org/2003/05/soap-envelope";
  public static final String NS_EBMS = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";

  private EBMSUtils() {
  }

  /*
   * See
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/os/
   * AS4-profile-v1.0-os.html#__RefHeading__26454_1909778835
   */
  public static byte[] createSuccessReceipt(final SOAPMessage message) {
    ValueEnforcer.notNull(message, "SOAPMessage");

    try {
      final StreamSource stylesource = TransformSourceFactory.create(new ClassPathResource("/receipt-generator.xslt"));
      final TransformerFactory transformerFactory = TransformerFactory.newInstance ();
      transformerFactory.setFeature (XMLConstants.FEATURE_SECURE_PROCESSING, true);
      final Transformer transformer = transformerFactory.newTransformer(stylesource);
      transformer.setParameter("messageid", genereateEbmsMessageId(MEMConstants.MEM_AS4_SUFFIX));
      transformer.setParameter("timestamp", DateTimeUtils.getCurrentTimestamp());
      try (final NonBlockingByteArrayOutputStream baos = new NonBlockingByteArrayOutputStream()) {
        transformer.transform(new DOMSource(message.getSOAPPart()), new StreamResult(baos));
        return baos.toByteArray();
      }
    } catch (final RuntimeException ex) {
      // throw RTE's directly
      throw ex;
    } catch (final Exception ex) {
      // force exceptions to runtime
      throw new MEException(ex.getMessage(), ex);
    }
  }

  /**
   * Create a fault message based on the error message
   *
   * @param soapMessage Source SOAP message. May be <code>null</code>
   * @param faultMessage Fault message. May be <code>null</code>.
   * @return byte[] with result XML SOAP Fault message
   */
  public static byte[] createFault(@Nullable final SOAPMessage soapMessage, @Nullable final String faultMessage) {
    final String fm = faultMessage != null ? faultMessage : "Unknown Error";
    String refToMessageInError;

    if (soapMessage != null) {
      refToMessageInError = getMessageId(soapMessage);
    } else {
      refToMessageInError = "";
    }

    final IMicroDocument aDoc = new MicroDocument();
    final IMicroElement eEnvelope = aDoc.appendElement(NS_SOAPENV, "Envelope");
    {
      final IMicroElement eHeader = eEnvelope.appendElement(NS_SOAPENV, "Head");
      final IMicroElement eMessaging = eHeader.appendElement(NS_EBMS, "Messaging");
      final IMicroElement eSignalMessage = eMessaging.appendElement(NS_EBMS, "SignalMessage");
      {
        final IMicroElement eMessageInfo = eSignalMessage.appendElement(NS_EBMS, "MessageInfo");
        eMessageInfo.appendElement(NS_EBMS, "Timestamp").appendText(DateTimeUtils.getCurrentTimestamp());
        final String ebmsMessageId = genereateEbmsMessageId(MEMConstants.MEM_AS4_SUFFIX);
        eMessageInfo.appendElement(NS_EBMS, "MessageId").appendText(ebmsMessageId);
      }
      {
        final IMicroElement eError = eSignalMessage.appendElement(NS_EBMS, "Error");
        eError.setAttribute("category", "CONTENT");
        eError.setAttribute("errorCode", "EBMS:0004");
        eError.setAttribute("origin", "ebms");
        eError.setAttribute("refToMessageInError", refToMessageInError);
        eError.setAttribute("severity", "failure");
        eError.setAttribute("shortDescription", "Error");
        eError.appendElement(NS_EBMS, "Description").setAttribute(XMLConstants.XML_NS_URI, "lang", "en")
            .appendText(fm);
        eError.appendElement(NS_EBMS, "ErrorDetail").appendText(fm);
      }
    }
    {
      final IMicroElement eBody = eEnvelope.appendElement(NS_SOAPENV, "Body");
      final IMicroElement eFault = eBody.appendElement(NS_SOAPENV, "Fault");
      {
        final IMicroElement eCode = eFault.appendElement(NS_SOAPENV, "Code");
        eCode.appendElement(NS_SOAPENV, "Value").appendText("env:Receiver");
      }
      {
        final IMicroElement eReason = eFault.appendElement(NS_SOAPENV, "Reason");
        eReason.appendElement(NS_SOAPENV, "Text").setAttribute(XMLConstants.XML_NS_URI, "lang", "en").appendText(fm);
      }
    }

    final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext();
    aNSCtx.addMapping("env", NS_SOAPENV);
    aNSCtx.addMapping("eb", NS_EBMS);

    return MicroWriter.getNodeAsBytes(aDoc, new XMLWriterSettings().setNamespaceContext(aNSCtx));
  }

  /**
   * Generate a random ebms message id with the format <code>&lt;RANDOM UUID&gt;@ext</code>
   *
   * @param ext suffix to use. May not be <code>null</code>.
   * @return EBMS Message ID. Never <code>null</code> nor empty.
   */
  public static String genereateEbmsMessageId(final String ext) {
    return UUID.randomUUID().toString() + "@" + ext;
  }

  @Nullable
  private static IMicroElement _property(@Nonnull final String sName, @Nullable final String sValue) {
    if (sValue == null) {
      return null;
    }

    final IMicroElement ret = new MicroElement(NS_EBMS, "Property");
    ret.setAttribute("name", sName).appendText(sValue);
    return ret;
  }

  /*
   * The conversion procedure goes here
   */
  public static SOAPMessage convert2MEOutboundAS4Message(final SubmissionMessageProperties metadata,
      final MEMessage meMessage) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Convert submission data to SOAP Message");
    }

    try {
      final IMicroDocument aDoc = new MicroDocument();
      final IMicroElement eMessaging = aDoc.appendElement(NS_EBMS, "Messaging");
      eMessaging.setAttribute(NS_SOAPENV, "mustUnderstand", "true");
      final IMicroElement eUserMessage = eMessaging.appendElement(NS_EBMS, "UserMessage");

      {
        final IMicroElement eMessageInfo = eUserMessage.appendElement(NS_EBMS, "MessageInfo");
        eMessageInfo.appendElement(NS_EBMS, "Timestamp").appendText(DateTimeUtils.getCurrentTimestamp());
        final String ebmsMessageId = genereateEbmsMessageId(MEMConstants.MEM_AS4_SUFFIX);
        eMessageInfo.appendElement(NS_EBMS, "MessageId").appendText(ebmsMessageId);
      }
      {
        final IMicroElement ePartyInfo = eUserMessage.appendElement(NS_EBMS, "PartyInfo");
        {
          final IMicroElement eFrom = ePartyInfo.appendElement(NS_EBMS, "From");
          eFrom.appendElement(NS_EBMS, "PartyId")
              // .setAttribute ("type", "urn:oasis:names:tc:ebcore:partyid-type:unregistered")
              .appendText(TCConfig.getMEMAS4TcPartyid());
          eFrom.appendElement(NS_EBMS, "Role").appendText(MEMConstants.MEM_PARTY_ROLE);
        }
        {
          final IMicroElement eTo = ePartyInfo.appendElement(NS_EBMS, "To");
          eTo.appendElement(NS_EBMS, "PartyId").appendText(TCConfig.getMEMAS4GwPartyID());
          eTo.appendElement(NS_EBMS, "Role").appendText(MEMConstants.GW_PARTY_ROLE);
        }
      }

      {
        final IMicroElement eCollaborationInfo = eUserMessage.appendElement(NS_EBMS, "CollaborationInfo");
        eCollaborationInfo.appendElement(NS_EBMS, "Service").appendText(MEMConstants.SERVICE);
        eCollaborationInfo.appendElement(NS_EBMS, "Action").appendText(MEMConstants.ACTION_SUBMIT);
        eCollaborationInfo.appendElement(NS_EBMS, "ConversationId").appendText(metadata.conversationId);
      }

      {
        final IMicroElement eMessageProperties = eUserMessage.appendElement(NS_EBMS, "MessageProperties");
        eMessageProperties.appendChild(_property("MessageId", metadata.messageId));
        eMessageProperties.appendChild(_property("ConversationId", metadata.conversationId));
        eMessageProperties.appendChild(_property("RefToMessageId", metadata.refToMessageId));
        eMessageProperties.appendChild(_property("Service", metadata.service));
        eMessageProperties.appendChild(_property("Action", metadata.action));
        eMessageProperties.appendChild(_property("ToPartyId", metadata.toPartyId));
        eMessageProperties.appendChild(_property("ToPartyIdType", metadata.toPartyIdType));
        eMessageProperties.appendChild(_property("ToPartyRole", metadata.toPartyRole));
        // NOTE: ToPartyCertificate is the DER+BASE64 encoded X509 certificate.
        // First decode as byte array, then parse it using
        // CertificateFactory.getInstance("X509", "BC")
        // recommended provider: BouncyCastleProvider
        eMessageProperties.appendChild(_property("ToPartyCertificate", metadata.toPartyCertificate));
        eMessageProperties.appendChild(_property("TargetURL", metadata.targetURL));
      }

      {
        final IMicroElement ePayloadInfo = eUserMessage.appendElement(NS_EBMS, "PayloadInfo");
        for (final MEPayload aPayload : meMessage.getPayloads()) {
          final IMicroElement ePartInfo = ePayloadInfo.appendElement(NS_EBMS, "PartInfo");
          ePartInfo.setAttribute("href", "cid:" + aPayload.getPayloadId());

          final IMicroElement ePartProperties = ePartInfo.appendElement(NS_EBMS, "PartProperties");
          ePartProperties.appendChild(_property("MimeType", aPayload.getMimeTypeString()));
        }
      }

      final MapBasedNamespaceContext aNSCtx = new MapBasedNamespaceContext();
      aNSCtx.addMapping("env", NS_SOAPENV);
      aNSCtx.addMapping("eb", NS_EBMS);

      // Convert to org.w3c.dom ....
      final Element element = DOMReader.readXMLDOM(MicroWriter.getNodeAsBytes(aDoc,
          new XMLWriterSettings().setNamespaceContext(aNSCtx)))
          .getDocumentElement();

      // create a soap message based on this XML
      final SOAPMessage message = SoapUtil.createEmptyMessage();
      final Node importNode = message.getSOAPHeader().getOwnerDocument().importNode(element, true);
      message.getSOAPHeader().appendChild(importNode);

      meMessage.getPayloads().forEach(payload -> {
        final AttachmentPart attachmentPart = message.createAttachmentPart();
        attachmentPart.setContentId('<' + payload.getPayloadId() + '>');
        try {
          final byte[] data = payload.getData();
          attachmentPart.setRawContentBytes(data, 0, data.length, payload.getMimeTypeString());
        } catch (final SOAPException e) {
          throw new MEException(e);
        }
        message.addAttachmentPart(attachmentPart);
      });

      if (message.saveRequired()) {
        message.saveChanges();
      }

      if (LOG.isTraceEnabled()) {
        LOG.trace(SoapUtil.describe(message));
      }
      return message;
    } catch (final Exception ex) {
      throw new MEException(ex);
    }
  }

  /**
   * Process the inbound SOAPMessage and convert it to the MEMEssage
   *
   * @param message the soap message to be converted to a MEMessage. Cannot be null
   * @return the MEMessage object created from the supplied SOAPMessage
   * @throws MEException in case of error
   */
  public static MEMessage soap2MEMessage(@Nonnull final SOAPMessage message) {
    ValueEnforcer.notNull(message, "SOAPMessage");

    if (LOG.isDebugEnabled()) {
      LOG.debug("Convert message to submission data");
    }

    final MEMessage meMessage = new MEMessage();
    if (message.countAttachments() > 0) {
      // Read all attachments
      message.getAttachments().forEachRemaining(attObj -> {
        final AttachmentPart att = (AttachmentPart) attObj;
        // remove surplus characters
        final String href = RegExHelper.stringReplacePattern("<|>", att.getContentId(), "");
        Node partInfo;
        try {
          // throws exception if part info does not exist
          partInfo = SoapXPathUtil.safeFindSingleNode(message.getSOAPHeader(),
              "//:PayloadInfo/:PartInfo[@href='cid:" + href + "']");
        } catch (final Exception ex) {
          throw new MEException("ContentId: " + href + " was not found in PartInfo");
        }

        MimeType mimeType;
        try {
          String sMimeType = SoapXPathUtil.getSingleNodeTextContent(partInfo,
              ".//:PartProperties/:Property[@name='MimeType']");
          if (sMimeType.startsWith("cid:")) {
            sMimeType = sMimeType.substring(4);
          }

          mimeType = MimeTypeParser.parseMimeType(sMimeType);
        } catch (final MimeTypeParserException ex) {
          LOG.warn("Error parsing MIME type: " + ex.getMessage());
          // if there is a problem wrt the processing of the mimetype, simply grab the
          // content type
          // FIXME: Do not swallow the error, there might a problem with the mimtype
          mimeType = MimeTypeParser.parseMimeType(att.getContentType());
        }

        try {
          final Node charSetNode = SoapXPathUtil.findSingleNode(partInfo,
              ".//:PartProperties/:Property[@name='CharacterSet']/text()");
          if (charSetNode != null) {
            final Charset aCharset = CharsetHelper.getCharsetFromNameOrNull(charSetNode.getNodeValue());
            if (aCharset != null) {
              // Add charset to MIME type
              mimeType.addParameter(CMimeType.PARAMETER_NAME_CHARSET, aCharset.name());
            }
          }
        } catch (final MEException ex) {
          // ignore
        }

        byte[] rawContentBytes;
        try {
          rawContentBytes = att.getRawContentBytes();
        } catch (final SOAPException e) {
          throw new MEException(e);
        }

        final MEPayload payload = new MEPayload(mimeType, href, rawContentBytes);
        if (LOG.isDebugEnabled()) {
          LOG.debug("\tpayload.payloadId: " + payload.getPayloadId());
          LOG.debug("\tpayload.mimeType: " + payload.getMimeTypeString());
        }

        meMessage.getPayloads().add(payload);
      });
    }
    return meMessage;
  }

  public static RelayResult soap2RelayResult(final SOAPMessage sNotification) {
    ValueEnforcer.notNull(sNotification, "Notification");

    final RelayResult notification = new RelayResult();

    try {
      final Node messagePropsNode = SoapXPathUtil.safeFindSingleNode(sNotification.getSOAPHeader(),
          "//:MessageProperties");

      final String messageId = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode,
          ".//:Property[@name='MessageId']/text()");
      notification.setMessageID(messageId);

      final String refToMessageId = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode,
          ".//:Property[@name='RefToMessageId']/text()");
      notification.setRefToMessageID(refToMessageId);

      final String sSignalType = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode, ".//:Property[@name='Result']");
      if (!"ERROR".equalsIgnoreCase(sSignalType)) {
        notification.setResult(ResultType.RECEIPT);
      } else {
        notification.setResult(ResultType.ERROR);

        try {
          final String errorCode = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode,
              ".//:Property[@name='ErrorCode']");
          notification.setErrorCode(errorCode);
        } catch (final MEException e) {
          throw new IllegalStateException("ErrorCode is mandatory for relay result errors.");
        }

        try {
          final String severity = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode, ".//:Property[@name='severity']");
          notification.setSeverity(severity);
        } catch (final MEException e) {
          // TODO so what?
        }

        try {
          final String shortDesc = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode,
              ".//:Property[@name='ShortDescription']");
          notification.setShortDescription(shortDesc);
        } catch (final MEException ignored) {
          // TODO so what?
        }

        try {
          final String desc = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode, ".//:Property[@name='Description']");
          notification.setDescription(desc);
        } catch (final MEException ignored) {
          // TODO so what?
        }
      }

      return notification;
    } catch (final RuntimeException ex) {
      throw ex;
    } catch (final SOAPException ex) {
      throw new MEException(ex);
    }
  }

  public static SubmissionResult soap2SubmissionResult(final SOAPMessage sSubmissionResult) {
    ValueEnforcer.notNull(sSubmissionResult, "SubmissionResult");

    final SubmissionResult submissionResult = new SubmissionResult();

    try {
      final Node messagePropsNode = SoapXPathUtil
          .safeFindSingleNode(sSubmissionResult.getSOAPHeader(), "//:MessageProperties");

      final String refToMessageID = SoapXPathUtil
          .getSingleNodeTextContent(messagePropsNode, ".//:Property[@name='RefToMessageId']/text()");
      submissionResult.setRefToMessageID(refToMessageID);

      final String sSignalType = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode, ".//:Property[@name='Result']");
      if ("ERROR".equalsIgnoreCase(sSignalType)) {
        submissionResult.setResult(ResultType.ERROR);

        //description must be there when there is an error
        final String description = SoapXPathUtil.getSingleNodeTextContent(messagePropsNode, ".//:Property[@name='Description']");
        submissionResult.setDescription(description);

      } else {
        submissionResult.setResult(ResultType.RECEIPT);

        //message id is conditional, it must be there only in case of receipt
        final String messageID = SoapXPathUtil
            .getSingleNodeTextContent(messagePropsNode, ".//:Property[@name='MessageId']/text()");
        submissionResult.setMessageID(messageID);
      }

      return submissionResult;
    } catch (final RuntimeException ex) {
      throw ex;
    } catch (final Exception ex) {
      throw new MEException(ex);
    }
  }

  @Nonnull
  private static String _getCN(@Nonnull final String sPrincipal) {
    try {
      for (final Rdn aRdn : new LdapName(sPrincipal).getRdns()) {
        if (aRdn.getType().equalsIgnoreCase("CN")) {
          return (String) aRdn.getValue();
        }
      }
      throw new IllegalStateException("Failed to get CN from '" + sPrincipal + "'");
    } catch (final InvalidNameException ex) {
      throw new IllegalStateException("Failed to get CN from '" + sPrincipal + "'", ex);
    }
  }

  /**
   * process the GatewayRoutingMetadata object and obtain the actual submission data. Use for any direction (DC -&gt; DP
   * and DP -&gt; DC)
   *
   * @return SubmissionData
   */
  static SubmissionMessageProperties inferSubmissionData(final GatewayRoutingMetadata gatewayRoutingMetadata) {
    final IR2D2Endpoint endpoint = gatewayRoutingMetadata.getEndpoint();
    final X509Certificate certificate = endpoint.getCertificate();
    // we need the certificate to obtain the to party id
    ValueEnforcer.notNull(certificate, "Endpoint Certificate");
    final SubmissionMessageProperties submissionData = new SubmissionMessageProperties();
    submissionData.messageId = genereateEbmsMessageId(MEMConstants.MEM_AS4_SUFFIX);
    submissionData.action = gatewayRoutingMetadata.getDocumentTypeId();
    submissionData.service = gatewayRoutingMetadata.getProcessId();

    submissionData.toPartyId = _getCN(gatewayRoutingMetadata.getEndpoint().getCertificate()
        .getSubjectX500Principal().getName());

    // TODO: infer it from the transaction id
    submissionData.conversationId = "1";

    //this is the role of the RECEIVING gateway, it has been set
    //to the role of the SENDING gateway (i.e MEMConstants.GW_PARTY_ROLE)
    submissionData.toPartyRole = MEMConstants.GW_PARTY_ROLE;

    submissionData.targetURL = endpoint.getEndpointURL();

    try {
      // DER encoded X509 certificate
      final byte[] certBytes = endpoint.getCertificate().getEncoded();
      // base 64 encoded DER bytes (i.e. converted to CER)
      submissionData.toPartyCertificate = DatatypeConverter.printBase64Binary(certBytes);
    } catch (final CertificateEncodingException e) {
      throw new MEException(e);
    }
    return submissionData;
  }

  /**
   * Calls {@link SoapUtil#sendSOAPMessage(SOAPMessage, URL)} but checks the return value for a fault or a receipt.
   *
   * @param soapMessage Message to be sent. May not be <code>null</code>
   * @param url Target URL. May not be <code>null</code>
   * @throws MEException if a fault is received instead of an ebms receipt
   */
  public static void sendSOAPMessage(@Nonnull final SOAPMessage soapMessage, @Nonnull final URL url) {
    ValueEnforcer.notNull(soapMessage, "SOAP Message");
    ValueEnforcer.notNull(url, "Target url");

    if (LOG.isTraceEnabled()) {
      LOG.trace(SoapUtil.describe(soapMessage));
    }
    final SOAPMessage response = SoapUtil.sendSOAPMessage(soapMessage, url);

    if (response != null) {
      if (LOG.isTraceEnabled()) {
        LOG.info(SoapUtil.describe(response));
      }
      validateReceipt(response);
    } // else the receipt is null and we received a HTTP.OK, isn't that great?
  }

  /**
   * Check if the response is a soap fault (i.e. the response contains an error)
   */
  private static void validateReceipt(@Nonnull final SOAPMessage response) {

    ValueEnforcer.notNull(response, "Soap message");
    final Element errorElement = (Element) SoapXPathUtil.findSingleNode(response.getSOAPPart(),
        "//:SignalMessage/:Error");

    if (errorElement != null) {
      final String cat = StringHelper.getNotNull(errorElement.getAttribute("category")).toUpperCase(Locale.US);
      final String shortDescription = StringHelper.getNotNull(errorElement.getAttribute("shortDescription"))
          .toUpperCase(Locale.US);
      final String severity = StringHelper.getNotNull(errorElement.getAttribute("severity")).toUpperCase(Locale.US);
      final String code = StringHelper.getNotNull(errorElement.getAttribute("errorCode")).toUpperCase(Locale.US);

      final StringBuilder errBuff = new StringBuilder();
      errBuff.append("CODE: [" + code + "]\n");
      errBuff.append("Severity: [" + severity + "]\n");
      errBuff.append("Category: [" + cat + "]\n");
      errBuff.append("ShortDescription: [" + shortDescription
          + "]\n");
      ToopKafkaClient.send(EErrorLevel.ERROR, () -> "Error from AS4 transmission:" + errBuff.toString());
      throw new MEException(errBuff.toString());

    }

    // Short info that it worked
    ToopKafkaClient.send(EErrorLevel.INFO, () -> "AS4 transmission seemed to have worked out fine");
  }

  /**
   * Find the //:MessageInfo/eb:MessageId.text and return it.
   *
   * @param soapMessage SOAP message to extract data from
   * @return The message ID
   * @throws MEException if the message header does not contain an ebms message id
   */
  public static String getMessageId(final SOAPMessage soapMessage) {
    try {
      return SoapXPathUtil.getSingleNodeTextContent(soapMessage.getSOAPHeader(),
          "//:MessageInfo/:MessageId");
    } catch (final SOAPException e) {
      throw new MEException(e);
    }
  }
}
