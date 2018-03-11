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
package eu.toop.connector.me.mocAS4;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;


/**
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class MockAS4 {
  private final int port;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private ChannelFuture channelFuture;
  private ServerBootstrap serverBootstrap;

  public MockAS4(final int port) {
    this.port = port;
  }


  public void start() {
    //Create a simple netty http server
    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();

    serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(bossGroup, workerGroup);
    serverBootstrap.channel(NioServerSocketChannel.class);
    serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(final SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new HttpServerCodec(), new HttpPacketHandler());
      }
    });
    serverBootstrap.option(ChannelOption.SO_BACKLOG, Integer.valueOf (1024));

    // Bind and start to accept incoming connections.
    channelFuture = serverBootstrap.bind(port);
  }

  public void finish() {
    channelFuture.channel().close();
    workerGroup.shutdownGracefully();
    bossGroup.shutdownGracefully();
  }
}
