package org.ineto.niosocks;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

public class SocksServerHandler extends SimpleChannelHandler {

  private final static Logger log = Logger.getLogger(SocksServerHandler.class);
  
  private final Properties props;
  private final ClientSocketChannelFactory clientFactory;
  
  private static final byte[] RESPONSE_OK = new byte[] { 0x00, 0x5a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  
  private Channel outboundChannel = null;
  
  public SocksServerHandler(Properties props, ClientSocketChannelFactory clientFactory) {
    super();
    this.props = props;
    this.clientFactory = clientFactory;
  }

  public static String toHexString(byte blob[]) {
    StringBuffer out = new StringBuffer(blob.length * 2 + 1);
    for (int i = 0; i < blob.length; i++)
      out.append(Integer.toHexString(blob[i]));

    return out.toString();
  }
  
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    ChannelBuffer msg = (ChannelBuffer) e.getMessage();

    if (outboundChannel != null) {
      if (outboundChannel.isWritable()) {
        Channels.write(outboundChannel, msg);
      }
      else {
        e.getChannel().setReadable(false);
      }
      return;
    }
    
    if (msg.capacity() > 8 && msg.getByte(0) == 4 && msg.getByte(1) == 1) {
      byte[] addr = new byte[] { msg.getByte(4), msg.getByte(5), msg.getByte(6), msg.getByte(7) };
      final int outboundClientPort = (((0xFF & msg.getByte(2)) << 8) + (0xFF & msg.getByte(3)));
      final InetAddress outboundClientIP = InetAddress.getByAddress(addr);
      System.out.println("Connect " + outboundClientIP.getHostAddress() + ":" + outboundClientPort);
      
      final Channel inboundChannel = e.getChannel();
      inboundChannel.setReadable(false);

      ClientBootstrap outboundClientBootstrap = new ClientBootstrap(clientFactory);
      if (props.getProperty("outbound.connect.timeout") != null) { 
        outboundClientBootstrap.setOption("connectTimeoutMillis", props.getProperty("outbound.connect.timeout"));
      }
      outboundClientBootstrap.getPipeline().addLast("handler", new TrafficHandler(inboundChannel));
      ChannelFuture outboundClientFuture = outboundClientBootstrap.connect(new InetSocketAddress(outboundClientIP, outboundClientPort));
      outboundClientFuture.addListener(new ChannelFutureListener() {

        @Override
        public void operationComplete(ChannelFuture outboundClientFuture) throws Exception {
          if (outboundClientFuture.isSuccess()) {
            System.out.println("Outbound Channel SUCCESS " + outboundClientFuture.getChannel().getRemoteAddress());
            outboundChannel = outboundClientFuture.getChannel();
            inboundChannel.setReadable(true);
          }
          else {
            System.out.println("Outbound Channel FAIL " + outboundClientIP.getHostAddress() + ":" + outboundClientPort);
            Channels.close(inboundChannel);
           }
        }
        
      });
      System.out.println("Send OK");
      Channels.write(e.getChannel(), ChannelBuffers.wrappedBuffer(RESPONSE_OK));
    }
    else {
      Channels.close(e.getChannel());
    }
    
  }
  
  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    closeOnFlush(e.getChannel());
  }
  
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
    log.error("Unexpected exception from downstream.", e.getCause());
    Channels.close(e.getChannel());
    if (outboundChannel != null && outboundChannel.isConnected()) {
      Channels.close(outboundChannel);
    }
  }
  
  private static void closeOnFlush(Channel ch) {
    if (ch.isConnected()) {
      ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }
  
}
