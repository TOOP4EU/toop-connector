package eu.toop.connector.app.mp;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.collection.impl.ICommonsList;

import eu.toop.commons.dataexchange.v140.TDETOOPRequestType;
import eu.toop.commons.dataexchange.v140.TDETOOPResponseType;
import eu.toop.commons.exchange.AsicReadEntry;
import eu.toop.commons.exchange.ToopRequestWithAttachments140;
import eu.toop.commons.exchange.ToopResponseWithAttachments140;

/**
 * This is the central public class that allows access to the Message Processor
 * APIs.
 *
 * @author Philip Helger
 */
@Immutable
public final class MPTrigger
{
  private MPTrigger ()
  {}

  public static void fromDC_1_of_4 (@Nonnull final TDETOOPRequestType aRequestMsg,
                                    @Nonnull final ICommonsList <AsicReadEntry> aAttachments)
  {
    final ToopRequestWithAttachments140 aRequest = new ToopRequestWithAttachments140 (aRequestMsg, aAttachments);
    MessageProcessorDCOutgoing.getInstance ().enqueue (aRequest);
  }

  public static void incomingGatewayDP_2_of_4 (@Nonnull final ToopRequestWithAttachments140 aRequest)
  {
    MessageProcessorDPIncoming.getInstance ().enqueue (aRequest);
  }

  public static void fromDP_3_of_4 (@Nonnull final TDETOOPResponseType aResponseMsg,
                                    @Nonnull final ICommonsList <AsicReadEntry> aAttachments)
  {
    final ToopResponseWithAttachments140 aResponse = new ToopResponseWithAttachments140 (aResponseMsg, aAttachments);
    MessageProcessorDPOutgoing.getInstance ().enqueue (aResponse);
  }

  public static void incomingGatewayDC_4_of_4 (@Nonnull final ToopResponseWithAttachments140 aResponse)
  {
    MessageProcessorDCIncoming.getInstance ().enqueue (aResponse);
  }
}
