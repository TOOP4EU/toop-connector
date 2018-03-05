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
package eu.toop.mp.me.mocAS4;

import java.util.Iterator;
import java.util.Map;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.toop.mp.me.EBMSUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

/**
 * A channel handler that reads the decoded HTTP packets from the underlying pipeline.
 *
 * @author: myildiz
 * @date: 20.02.2018.
 */
public class HttpPacketHandler extends ChannelInboundHandlerAdapter {
  private static final Logger LOG = LoggerFactory.getLogger (HttpPacketHandler.class);
  private final SOAPMessageAccumulator messageAccumulator;

  public HttpPacketHandler() {
    messageAccumulator = new SOAPMessageAccumulator();
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof HttpRequest) {
      //when we receive the request, initiate a new message with the headers,
      //the next call sequence will be HttpContent objects where we parse
      //the actual post
      final HttpRequest req = (HttpRequest) msg;
      try {
        messageAccumulator.reset(getMimeHeaders(req.headers()));
      } catch (final Exception e) {
        e.printStackTrace();
      }
      if (HttpUtil.is100ContinueExpected(req)) {
        ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        return;
      }
    }

    if (msg instanceof HttpContent) {
      final HttpContent co = (HttpContent) msg;

      try {
        LOG.info("MOC AS4 Read SOAP MESSAGE");
        final ByteBuf content = co.content();
        final ByteBufInputStream bbis = new ByteBufInputStream(content);
        messageAccumulator.accumulate(bbis);

        if (msg instanceof DefaultLastHttpContent) {
          final SOAPMessage soapMessage = messageAccumulator.doFinal();
          LOG.info("Create receipt");
          final byte[] receipt = EBMSUtils.createSuccessReceipt(soapMessage);
          final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(receipt));
          response.headers().set(HttpHeaderNames.SERVER, "MOCAS4");
          response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/xml");
          response.headers().set(HttpHeaderNames.CONTENT_LENGTH, Integer.valueOf (receipt.length));
          ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        }

      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }

  private MimeHeaders getMimeHeaders(final HttpHeaders headers) {
    //leave the rest to soap factory
    final MimeHeaders mimeHeaders = new MimeHeaders();

    final Iterator<Map.Entry<String, String>> allHeaders = headers.iteratorAsString();
    while (allHeaders.hasNext()) {
      final Map.Entry<String, String> next = allHeaders.next();
      mimeHeaders.addHeader(next.getKey(), next.getValue());
    }

    mimeHeaders.getAllHeaders().forEachRemaining(header -> {
      final MimeHeader mh = (MimeHeader) header;
    });
    return mimeHeaders;
  }
}
