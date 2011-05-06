package org.ineto.niosocks;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

public class SocksServerHandler extends SimpleChannelHandler {

  private final static Logger log = Logger.getLogger(SocksServerHandler.class);
  
  private final ClientSocketChannelFactory clientFactory;
  private final ChannelGroup clientsGroup;
  
  private static final byte[] RESPONSE_OK = new byte[] { 0x00, 0x5a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  
  private ConcurrentMap<Channel, Channel> in2outMap = new ConcurrentHashMap<Channel, Channel>();
  
  public SocksServerHandler(ClientSocketChannelFactory clientFactory, ChannelGroup clientsGroup) {
    super();
    this.clientFactory = clientFactory;
    this.clientsGroup = clientsGroup;
  }

  public static String toHexString(byte abyte0[]) {
    StringBuffer stringbuffer = new StringBuffer(abyte0.length * 2 + 1);
    for (int i = 0; i < abyte0.length; i++)
      stringbuffer.append(Integer.toHexString(abyte0[i]));

    return stringbuffer.toString();
  }
  
  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    ChannelBuffer msg = (ChannelBuffer) e.getMessage();
    
    Channel outboundChannel = in2outMap.get(e.getChannel());
    if (outboundChannel != null) {
      Channels.write(outboundChannel, msg);
      if (!outboundChannel.isWritable()) {
        e.getChannel().setReadable(false);
      }
      return;
    }
    
    if (msg.capacity() > 8 && msg.getByte(0) == 4 && msg.getByte(1) == 1) {
      byte[] addr = new byte[] { msg.getByte(4), msg.getByte(5), msg.getByte(6), msg.getByte(7) };
      int outboundClientPort = (((0xFF & msg.getByte(2)) << 8) + (0xFF & msg.getByte(3)));
      InetAddress outboundClientIP = InetAddress.getByAddress(addr);
      log.info("Connect " + outboundClientIP.getHostAddress() + ":" + outboundClientPort);
      
      final Channel inboundChannel = e.getChannel();
      inboundChannel.setReadable(false);

      ClientBootstrap outboundClientBootstrap = new ClientBootstrap(clientFactory);
      outboundClientBootstrap.getPipeline().addLast("handler", new TrafficHandler(e.getChannel()));
      ChannelFuture outboundClientFuture = outboundClientBootstrap.connect(new InetSocketAddress(outboundClientIP, outboundClientPort));
      outboundClientFuture.addListener(new ChannelFutureListener() {

        @Override
        public void operationComplete(ChannelFuture outboundClientFuture) throws Exception {
          System.out.println("Outbound Channel " + outboundClientFuture.getChannel() + ", success = " + outboundClientFuture.isSuccess());
          if (outboundClientFuture.isSuccess()) {
            in2outMap.put(inboundChannel, outboundClientFuture.getChannel());
            //inboundChannel.getPipeline().addLast("handler", new TrafficHandler(outboundClientFuture.getChannel()));
            inboundChannel.setReadable(true);
          }
          else {
            inboundChannel.close();
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
    in2outMap.remove(e.getChannel());
  }
  
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
    log.error("Unexpected exception from downstream.", e.getCause());
    clientsGroup.remove(e.getChannel());
    in2outMap.remove(e.getChannel());
    Channels.close(e.getChannel());
  }
  
}
