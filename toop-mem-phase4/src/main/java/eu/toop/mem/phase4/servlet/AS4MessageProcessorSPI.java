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
package eu.toop.mem.phase4.servlet;

import java.io.InputStream;
import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.helger.as4.attachment.WSS4JAttachment;
import com.helger.as4.model.pmode.IPMode;
import com.helger.as4.servlet.IAS4MessageState;
import com.helger.as4.servlet.spi.AS4MessageProcessorResult;
import com.helger.as4.servlet.spi.AS4SignalMessageProcessorResult;
import com.helger.as4.servlet.spi.IAS4ServletMessageProcessorSPI;
import com.helger.as4lib.ebms3header.Ebms3SignalMessage;
import com.helger.as4lib.ebms3header.Ebms3UserMessage;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.xml.serialize.write.XMLWriter;

import eu.toop.commons.dataexchange.v140.TDETOOPRequestType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.exchange.AsicReadEntry;
import eu.toop.commons.exchange.ToopMessageBuilder140;
import eu.toop.connector.api.as4.IMessageExchangeSPI.IIncomingHandler;
import eu.toop.kafkaclient.ToopKafkaClient;

/**
 * Test implementation of {@link IAS4ServletMessageProcessorSPI}
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class AS4MessageProcessorSPI implements IAS4ServletMessageProcessorSPI
{
  public static final String ACTION_FAILURE = "Failure";
  private static final Logger LOGGER = LoggerFactory.getLogger (AS4MessageProcessorSPI.class);

  private static IIncomingHandler s_aIncomingHandler;

  public static void setIncomingHandler (@Nonnull final IIncomingHandler aIncomingHandler)
  {
    ValueEnforcer.notNull (aIncomingHandler, "IncomingHandler");
    ValueEnforcer.isNull (s_aIncomingHandler, "s_aIncomingHandler");
    s_aIncomingHandler = aIncomingHandler;
  }

  @Nonnull
  public AS4MessageProcessorResult processAS4UserMessage (@Nonnull final Ebms3UserMessage aUserMessage,
                                                          @Nonnull final IPMode aPMode,
                                                          @Nullable final Node aPayload,
                                                          @Nullable final ICommonsList <WSS4JAttachment> aIncomingAttachments,
                                                          @Nonnull final IAS4MessageState aState)
  {
    // Needed for AS4_TA13 because we want to force a decompression failure and
    // for that to happen the stream has to be read
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
            try
            {
              final InputStream aIS = x.getSourceStream ();
              LOGGER.info ("    Attachment Stream Class: " + aIS.getClass ().getName ());
              final String sContent = StreamHelper.getAllBytesAsString (x.getSourceStream (), x.getCharset ());
              LOGGER.info ("    Attachment Content: " + sContent.length () + " chars");
            }
            catch (final IllegalStateException ex)
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
          s_aIncomingHandler.handleIncomingResponse ((TDETOOPResponseType) aMsg, aAttachments);
        }
        else
          if (aMsg instanceof TDETOOPRequestType)
          {
            // This is the way from DC to DP; we're in DP incoming mode
            s_aIncomingHandler.handleIncomingRequest ((TDETOOPRequestType) aMsg, aAttachments);
          }
          else
            ToopKafkaClient.send (EErrorLevel.ERROR, () -> "Unsuspported Message: " + aMsg);
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
  public AS4SignalMessageProcessorResult processAS4SignalMessage (@Nonnull final Ebms3SignalMessage aSignalMessage,
                                                                  @Nonnull final IPMode aPmode,
                                                                  @Nonnull final IAS4MessageState aState)
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
