package eu.toop.mp.me.mocAS4;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import java.io.*;
import java.util.Iterator;
import java.util.Map;


/**
 * @author: myildiz
 * @date: 16.02.2018.
 */
public class MocAS4 {
  private final int port;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private ChannelFuture channelFuture;
  private ServerBootstrap serverBootstrap;

  public MocAS4(int port) throws IOException {
    this.port = port;
  }


  public void start() throws IOException, InterruptedException {
    //Create a simple netty http server
    bossGroup = new NioEventLoopGroup();
    workerGroup = new NioEventLoopGroup();

    serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(bossGroup, workerGroup);
    serverBootstrap.channel(NioServerSocketChannel.class);
    serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
      @Override
      protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(new HttpServerCodec(), new HttpPacketHandler());
      }
    });
    serverBootstrap.option(ChannelOption.SO_BACKLOG, 1024);

    // Bind and start to accept incoming connections.
    channelFuture = serverBootstrap.bind(port);
  }

  public void finish() {
    channelFuture.channel().close();
    workerGroup.shutdownGracefully();
    bossGroup.shutdownGracefully();
  }
}
