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
package eu.toop.connector.mem.phase4.servlet;

import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.phase4.attachment.AS4DecompressException;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3SignalMessage;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.EEbmsError;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.model.pmode.IPMode;
import com.helger.phase4.servlet.IAS4MessageState;
import com.helger.phase4.servlet.spi.AS4MessageProcessorResult;
import com.helger.phase4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.phase4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.xml.serialize.write.XMLWriter;

import eu.toop.commons.dataexchange.v140.TDETOOPRequestType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.exchange.AsicReadEntry;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.commons.exchange.ToopRequestWithAttachments140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;
import eu.toop.connector.api.as4.IMEIncomingHandler;
import eu.toop.connector.mem.phase4.Phase4Config;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * TOOP specific implementation of {@link IAS4ServletMessageProcessorSPI}. It
 * takes incoming AS4 messages and forwards it accordingly to the correct TOOP
 * {@link eu.toop.connector.api.as4.IMEIncomingHandler}.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class AS4MessageProcessorSPI implements IAS4ServletMessageProcessorSPI
{
  public static final String ACTION_FAILURE = "Failure";
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4MessageProcessorSPI.class);

  private static IMEIncomingHandler s_aIncomingHandler;

  public static void setIncomingHandler (@Nonnull final IMEIncomingHandler aIncomingHandler)
  {
    ValueEnforcer.notNull (aIncomingHandler, "IncomingHandler");
    ValueEnforcer.isNull (s_aIncomingHandler, "s_aIncomingHandler");
    s_aIncomingHandler = aIncomingHandler;
  }

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                          @Nonnull final HttpHeaderMap aHttpHeaders,
                                                          @Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4MessageState aState,
                                                          @Nonnull final ICommonsList <Ebms3Error> aProcessingErrors)
  {
    if (Phase4Config.isDebugIncoming ())
    {
      LOGGER.info ("Received AS4 message:");
      LOGGER.info ("  UserMessage: " + aUserMessage);
      LOGGER.info ("  Payload: " +
                   (aPayload == null ? "null" : true ? "present" : XMLWriter.getNodeAsString (aPayload)));

      if (aIncomingAttachments != null)
      {
        LOGGER.info ("  Attachments: " + aIncomingAttachments.size ());
        for (final WSS4JAttachment x : aIncomingAttachments)
        {
          LOGGER.info ("    Attachment Content Type: " + x.getMimeType ());
          if (x.getMimeType ().startsWith ("text") || x.getMimeType ().endsWith ("/xml"))
          {
            try (final InputStream aIS = x.getSourceStream ())
            {
              LOGGER.info ("    Attachment Stream Class: " + aIS.getClass ().getName ());
              final String sContent = StreamHelper.getAllBytesAsString (x.getSourceStream (), x.getCharset ());
              LOGGER.info ("    Attachment Content: " + sContent.length () + " chars");
            }
            catch (final Exception ex)
            {
              LOGGER.warn ("    Attachment Content: CANNOT BE READ", ex);
            }
          }
        }
      }
    }

    if (aIncomingAttachments != null && aIncomingAttachments.size () == 1)
    {
      // This is the ASIC
      final WSS4JAttachment aAttachment = aIncomingAttachments.getFirst ();
      try
      {
        // Extract from ASiC and keep attachments
        final ICommonsList <AsicReadEntry> aAttachments = new CommonsArrayList <> ();
        final Serializable aMsg = ToopMessageBuilder140.parseRequestOrResponse (aAttachment.getSourceStream (),
                                                                                aAttachments::add);

        // Response before Request because it is derived from Request!
        if (aMsg instanceof TDETOOPResponseType)
        {
          // This is the way from DP back to DC; we're in DC incoming mode
          final ToopResponseWithAttachments140 aResponse = new ToopResponseWithAttachments140 ((TDETOOPResponseType) aMsg,
                                                                                               aAttachments);
          s_aIncomingHandler.handleIncomingResponse (aResponse);
        }
        else
          if (aMsg instanceof TDETOOPRequestType)
          {
            // This is the way from DC to DP; we're in DP incoming mode
            final ToopRequestWithAttachments140 aRequest = new ToopRequestWithAttachments140 ((TDETOOPRequestType) aMsg,
                                                                                              aAttachments);
            s_aIncomingHandler.handleIncomingRequest (aRequest);
          }
          else
            ToopKafkaClient.send (EErrorLevel.ERROR, () -> "Unsuspported Message: " + aMsg);
      }
      catch (final AS4DecompressException ex)
      {
        final String sErrorMsg = "Error decompressing a compressed attachment";
        aProcessingErrors.add (EEbmsError.EBMS_DECOMPRESSION_FAILURE.getAsEbms3Error (aState.getLocale (),
                                                                                      aState.getMessageID (),
                                                                                      sErrorMsg));
        ToopKafkaClient.send (EErrorLevel.ERROR, () -> "Error handling incoming AS4 message: " + sErrorMsg);
      }
      catch (final Exception ex)
      {
        ToopKafkaClient.send (EErrorLevel.ERROR, () -> "Error handling incoming AS4 message", ex);
      }
    }

    // To test returning with a failure works as intended
    if (aUserMessage.getCollaborationInfo ().getAction ().equals (ACTION_FAILURE))
    {
      return AS4MessageProcessorResult.createFailure (ACTION_FAILURE);
    }
    return AS4MessageProcessorResult.createSuccess ();
  }

  @Nonnull
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                                                  @Nonnull final HttpHeaderMap aHttpHeaders,
                                                                  @Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nullable final IPMode aPmode,
                                                                  @Nonnull final IAS4MessageState aState,
                                                                  @Nonnull final ICommonsList <Ebms3Error> aProcessingErrors)
  {
    if (aSignalMessage.getReceipt () != null)
    {
      // Receipt - just acknowledge
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    if (!aSignalMessage.getError ().isEmpty ())
    {
      // Error - just acknowledge
      return AS4SignalMessageProcessorResult.createSuccess ();
    }

    return AS4SignalMessageProcessorResult.createSuccess ();
  }
}
