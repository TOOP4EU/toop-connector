package eu.toop.mp.me.mocAS4;

import eu.toop.mp.me.EBMSUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * A channel handler that reads the decoded HTTP packets from the underlying pipeline.
 *
 * @author: myildiz
 * @date: 20.02.2018.
 */
public class HttpPacketHandler extends ChannelInboundHandlerAdapter {
  private SOAPMessageAccumulator messageAccumulator;

  public HttpPacketHandler() {
    messageAccumulator = new SOAPMessageAccumulator();
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
      //when we receive the request, initiate a new message with the headers,
      //the next call sequence will be HttpContent objects where we parse
      //the actual post
      HttpRequest req = (HttpRequest) msg;
      try {
        messageAccumulator.reset(getMimeHeaders(req.headers()));
      } catch (IOException e) {
        e.printStackTrace();
      } catch (SOAPException e) {
        e.printStackTrace();
      }
      if (HttpUtil.is100ContinueExpected(req)) {
        ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        return;
      }
    }

    if (msg instanceof HttpContent) {
      HttpContent co = (HttpContent) msg;

      try {
        System.out.println("MOC AS4 Read SOAP MESSAGE");
        ByteBuf content = co.content();
        ByteBufInputStream bbis = new ByteBufInputStream(content);
        messageAccumulator.accumulate(bbis);

        if (msg instanceof DefaultLastHttpContent) {
          SOAPMessage soapMessage = messageAccumulator.doFinal();
          System.out.println("Create receipt");
          byte[] receipt = EBMSUtils.createSuccessReceipt(soapMessage);
          FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(receipt));
          response.headers().set(HttpHeaderNames.SERVER, "MOCAS4");
          response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/xml");
          response.headers().set(HttpHeaderNames.CONTENT_LENGTH, receipt.length);
          ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        }

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }

  private MimeHeaders getMimeHeaders(HttpHeaders headers) {
    //leave the rest to soap factory
    MimeHeaders mimeHeaders = new MimeHeaders();

    Iterator<Map.Entry<String, String>> allHeaders = headers.iteratorAsString();
    while (allHeaders.hasNext()) {

      Map.Entry<String, String> next = allHeaders.next();

      System.out.println("HEADER: " + next);
      mimeHeaders.addHeader(next.getKey(), next.getValue());
    }

    mimeHeaders.getAllHeaders().forEachRemaining(header -> {
      MimeHeader mh = (MimeHeader) header;
      System.out.println(mh.getName() + ": " + mh.getValue());
    });
    return mimeHeaders;
  }
}
