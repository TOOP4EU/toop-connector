package eu.toop.mp.me;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.xml.parsers.DocumentBuilderFactory;
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
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.commons.mime.MimeTypeParser;

public final class EBMSUtils {
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EBMSUtils.class);

  private EBMSUtils() {}

  /**
   * See
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/os/AS4-profile-v1.0-os.html#__RefHeading__26454_1909778835
   *
   * @param message
   * @return
   */
  public static byte[] createSuccessReceipt(final SOAPMessage message) {
    ValueEnforcer.notNull(message, "SOAPMessage");

    try {
      final StreamSource stylesource = new StreamSource(EBMSUtils.class.getResourceAsStream("/receipt-generator.xslt"));
      final Transformer transformer = TransformerFactory.newInstance().newTransformer(stylesource);
      transformer.setParameter("messageid", genereateEbmsMessageId(MessageExchangeEndpointConfig.getMEMName()));
      transformer.setParameter("timestamp", DateTimeUtils.getCurrentTimestamp());
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      transformer.transform(new DOMSource(message.getSOAPPart()), new StreamResult(baos));
      return baos.toByteArray();
    } catch (final RuntimeException ex) {
      //throw RTE's directly
      throw ex;
    } catch (final Exception ex) {
      //force exceptions to runtime
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }

  /**
   * Create a fault message based on the error message
   *
   * @param soapMessage
   * @param faultMessage
   * @return
   */
  public byte[] createFault(@Nonnull final SOAPMessage soapMessage, @Nullable final String faultMessage) {
    ValueEnforcer.notNull(soapMessage, "SOAPMessage");

    String xml = StreamHelper.getAllBytesAsString(EBMSUtils.class.getResourceAsStream("/fault-template.xml"), StandardCharsets.UTF_8);

    Node element;
    try {
      element = SoapXPathUtil.findSingleNode(soapMessage.getSOAPHeader(), "//:MessageInfo/:MessageId");
    } catch (final SOAPException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

    final String refToMessageInError = element.getTextContent();

    String fm = faultMessage;
    if (fm == null)
      fm = "Unknown Error";

    final String ebmsMessageId = genereateEbmsMessageId(MessageExchangeEndpointConfig.getMEMName());
    final String category = "CONTENT";
    final String errorCode = "EBMS:0004";
    final String origin = "ebms";
    final String severity = "failure";
    final String shortDescription = "Error";
    final String description = fm;
    final String errorDetail = fm;
    final String reason = fm;
    final String faultCode = "env:Receiver";
    final String keyCategory = "${category}";
    final String keyErrorCode = "${errorCode}";
    final String keyOrigin = "${origin}";
    final String keySeverity = "${severity}";
    final String keyShortDescription = "${shortDescription}";
    final String keyDescription = "${description}";
    final String keyErrorDetail = "${errorDetail}";
    final String keyFaultCode = "${faultCode}";
    final String keyReason = "${reason}";
    xml = xml
        .replace("${timeStamp}", DateTimeUtils.getCurrentTimestamp())
        .replace("${refToMessageInError}", refToMessageInError)
        .replace("${messageId}", ebmsMessageId)
        .replace(keyCategory, category)
        .replace(keyErrorCode, errorCode)
        .replace(keyOrigin, origin)
        .replace(keySeverity, severity)
        .replace(keyShortDescription, shortDescription)
        .replace(keyDescription, description)
        .replace(keyErrorDetail, errorDetail)
        .replace(keyFaultCode, faultCode)
        .replace(keyReason, reason);
    return xml.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Generate a random ebms message id with the format
   * <code>&lt;RANDOM UUID&gt;@ext</code>
   *
   * @param ext
   * @return
   */
  public static String genereateEbmsMessageId(final String ext) {
    return UUID.randomUUID().toString() + "@" + ext;
  }


  /**
   * The conversion procedure goes here
   *
   * @param metadata
   * @param meMessage
   * @return
   */
  public static SOAPMessage convert2MEOutboundAS4Message(final SubmissionData metadata, final MEMessage meMessage) {
    try {
      if (LOG.isDebugEnabled ())
      LOG.debug("Convert submission data to SOAP Message");
      String xml = StreamHelper.getAllBytesAsString(EBMSUtils.class.getResourceAsStream("/as4template.xml"), StandardCharsets.UTF_8);

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

      xml = xml
          .replace(keyTimeStamp, DateTimeUtils.getCurrentTimestamp())
          .replace(keyMessageId, genereateEbmsMessageId(MessageExchangeEndpointConfig.ME_NAME))
          .replace(keyConversationId, conversationId)
          .replace(keyFrom, MessageExchangeEndpointConfig.ME_PARTY_ID)
          .replace(keyFromPartyRole, MessageExchangeEndpointConfig.ME_PARTY_ROLE)
          .replace(keyTo, MessageExchangeEndpointConfig.GW_PARTY_ID)
          .replace(keyToPartyRole, MessageExchangeEndpointConfig.GW_PARTY_ROLE)
          .replace(keyAction, MessageExchangeEndpointConfig.SUBMIT_ACTION)
          .replace(keyService, MessageExchangeEndpointConfig.SUBMIT_SERVICE)
          .replace(keyMessageProps, generateMessageProperties(metadata))
          .replace(keyPartInfo, generatePartInfo(meMessage));

      System.out.println(xml);

      final DocumentBuilderFactory instance = DocumentBuilderFactory.newInstance();
      instance.setNamespaceAware(true);
      final Document document = instance.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

      //create a soap message based on this XML
      final SOAPMessage message = SoapUtil.createEmptyMessage();
      final Element element = document.getDocumentElement();
      final Node importNode = message.getSOAPHeader().getOwnerDocument().importNode(element, true);
      message.getSOAPHeader().appendChild(importNode);

      meMessage.getPayloads().forEach(payload -> {
        final AttachmentPart attachmentPart = message.createAttachmentPart();
        attachmentPart.setContentId('<' + payload.getPayloadId() + '>');
        try {
          final byte[] data = payload.getData();
          attachmentPart.setRawContentBytes(data, 0, data.length, payload.getMimeTypeString ());
        } catch (final SOAPException e) {
          throw new RuntimeException(e);
        }
        message.addAttachmentPart(attachmentPart);
      });

      if (message.saveRequired())
        message.saveChanges();

      if (LOG.isTraceEnabled())
        LOG.trace(SoapUtil.describe(message));
      return message;
    } catch (final RuntimeException ex) {
      //throw RTE's directly
      throw ex;
    } catch (final Exception ex) {
      //force exceptions to runtime
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }

  /**
   * <table><tr><th>Property name	</th><th>     Required?</th></tr>
   * <tr><td>MessageId	    </td><td>     M (should be Y)</td></tr>
   * <tr><td>ConversationId	</td><td>   Y</td></tr>
   * <tr><td>RefToMessageId	</td><td>   N</td></tr>
   * <tr><td>ToPartyId	    </td><td>     Y</td></tr>
   * <tr><td>ToPartyRole	  </td><td>     Y</td></tr>
   * <tr><td>Service	      </td><td>     Y</td></tr>
   * <tr><td>ServiceType	  </td><td>     N // not used</td></tr>
   * <tr><td>Action	        </td><td>   Y</td></tr>
   * <tr><td>originalSender	</td><td>   Y</td></tr>
   * <tr><td>finalRecipient	</td><td>   Y</td></tr></table>
   */
  public static String generateMessageProperties(final SubmissionData submissionData) {
    final StringBuilder propertiesBuilder = new StringBuilder();
    propertiesBuilder.append("      <ns2:Property name=\"MessageId\">" + submissionData.messageId + "</ns2:Property>\n");
    propertiesBuilder.append("      <ns2:Property name=\"ConversationId\">" + submissionData.conversationId + "</ns2:Property>\n");

    if (submissionData.refToMessageId != null) {
      propertiesBuilder.append("      <ns2:Property name=\"RefToMessageId\">" + submissionData.refToMessageId + "</ns2:Property>\n");
    }

    propertiesBuilder.append("      <ns2:Property name=\"Service\">" + submissionData.service + "</ns2:Property>\n");
    propertiesBuilder.append("      <ns2:Property name=\"Action\">" + submissionData.action + "</ns2:Property>\n");
    propertiesBuilder.append("      <ns2:Property name=\"ToPartyId\">" + submissionData.to + "</ns2:Property>\n");
    propertiesBuilder.append("      <ns2:Property name=\"ToPartyRole\">" + submissionData.toPartyRole + "</ns2:Property>\n");
    //the GW is the sender of the C2 ---> C3 Message
    propertiesBuilder.append("      <ns2:Property name=\"FromPartyId\">" + MessageExchangeEndpointConfig.GW_PARTY_ID + "</ns2:Property>\n");
    propertiesBuilder.append("      <ns2:Property name=\"FromPartyRole\">" + submissionData.fromPartyRole + "</ns2:Property>\n");
    propertiesBuilder.append("      <ns2:Property name=\"originalSender\">" + submissionData.originalSender + "</ns2:Property>\n");
    propertiesBuilder.append("      <ns2:Property name=\"finalRecipient\">" + submissionData.finalRecipient + "</ns2:Property>");


    return propertiesBuilder.toString();
  }

  /**
   * Generate PayloadInfo/PartInfo part of the UserMessage
   *
   * @param meMessage
   * @return
   */
  public static String generatePartInfo(final MEMessage meMessage) {
    final StringBuilder partInfoBuilder = new StringBuilder();

    meMessage.getPayloads().forEach(mEPayload -> {
      partInfoBuilder
          .append("<ns2:PartInfo href=\"cid:")
          .append(mEPayload.getPayloadId())
          .append("\">\n        <ns2:PartProperties>\n          <ns2:Property name=\"MimeType\">")
          .append(mEPayload.getMimeTypeString())
          .append("</ns2:Property>")
          .append("\n        </ns2:PartProperties>\n")
          .append("      </ns2:PartInfo>");
    });
    return partInfoBuilder.toString();
  }

  /**
   * Process the inbound SOAPMessage and convert it to the MEMEssage
   *
   * @param message the soap message to be converted to a MEMessage. Cannot be null
   * @return the MEMessage object created from the supplied SOAPMessage
   * @throws Exception
   */
  public static MEMessage soap2MEMessage(@Nonnull final SOAPMessage message) throws Exception {
    ValueEnforcer.notNull(message, "SOAPMessage");

    if (LOG.isDebugEnabled ())
      LOG.debug("Convert message to submission data");

    final MEMessage meMessage = new MEMessage();
    if (message.countAttachments() > 0) {
      // Read all attachments
      message.getAttachments().forEachRemaining(attObj -> {
        final AttachmentPart att = (AttachmentPart) attObj;
        //remove surplus characters
        final String href = att.getContentId().replace("<|>", "");
        //throws exception if part info does not exist
        Node partInfo;
        try {
          partInfo = SoapXPathUtil.findSingleNode(message.getSOAPHeader(), "//:PayloadInfo/:PartInfo[@href='cid:" + href + "']");
        } catch (final Exception ex) {
          throw new RuntimeException("ContentId: " + href + " was not found in PartInfo");
        }

        final MimeType contentType = MimeTypeParser.parseMimeType (att.getContentType());
        MimeType mimeType = null;

        try {
          final Node singleNode = SoapXPathUtil.findSingleNode(partInfo, ".//:PartProperties/:Property[@name='MimeType']/text()");
          String sMimeType = singleNode.getNodeValue();
          if (sMimeType.startsWith("cid:"))
            sMimeType = sMimeType.substring(4);

          mimeType = MimeTypeParser.parseMimeType (sMimeType);
        } catch (final Throwable throwable) {
          //if there is a problem wrt the processing of the mimetype, simply grab the content type
          //FIXME: Do not swallow the error, there might a problem with the mimtype
          mimeType = contentType;
        }

        final Node charSetNode = SoapXPathUtil.findSingleNode(partInfo, ".//:PartProperties/:Property[@name='CharacterSet']/text()");
        final Charset aCharset = CharsetHelper.getCharsetFromNameOrNull (charSetNode.getNodeValue());
        if (aCharset != null) {
          // Add charset to MIME type
          mimeType.addParameter (CMimeType.PARAMETER_NAME_CHARSET, aCharset.name ());
        }

        byte[] rawContentBytes;
        try {
          rawContentBytes = att.getRawContentBytes();
        } catch (final SOAPException e) {
          throw new RuntimeException(e.getMessage(), e);
        }

        final MEPayload payload = new MEPayload (mimeType, href, rawContentBytes);
        if (LOG.isDebugEnabled ()) {
          LOG.debug("\tpayload.payloadId: " + payload.getPayloadId());
          LOG.debug("\tpayload.mimeType: " + payload.getMimeTypeString());
        }

        meMessage.getPayloads ().add(payload);
      });
    }
    return meMessage;
  }


  /**
   * process the GatewayRoutingMetadata object and obtain the actual submission data
   *
   * @param gatewayRoutingMetadata
   * @return SubmissionData
   */
  static SubmissionData inferSubmissionData(final GatewayRoutingMetadata gatewayRoutingMetadata) {
    final X509Certificate certificate = gatewayRoutingMetadata.getEndpoint().getCertificate();
    //we need the certificate to obtain the to party id
    ValueEnforcer.notNull(certificate, "Endpoint Certificate");
    final SubmissionData submissionData = new SubmissionData();
    submissionData.messageId = genereateEbmsMessageId(MessageExchangeEndpointConfig.getMEMName());
    submissionData.action = gatewayRoutingMetadata.getDocumentTypeId();
    submissionData.service = gatewayRoutingMetadata.getProcessId();

    final String dn = certificate.getSubjectX500Principal().getName();
    LdapName ldapDN;
    try {
      ldapDN = new LdapName(dn);
    } catch (final InvalidNameException e) {
      throw new IllegalArgumentException("Invalid certificate name", e);
    }
    submissionData.to = ldapDN.getRdn(0).getValue().toString();

    //TODO: infer it from the transaction id
    submissionData.conversationId = "1";

    //TODO: read if from config maybe
    submissionData.toPartyRole = "http://toop.eu/identifiers/roles/dc";
    submissionData.fromPartyRole = "http://toop.eu/identifiers/roles/dp";
    //FIXME: The original sender must be some real value
    submissionData.originalSender = "originalSender";
    submissionData.finalRecipient = gatewayRoutingMetadata.getEndpoint().getParticipantID().getURIEncoded();

    return submissionData;
  }
}
