package org.ineto.niosocks;

import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

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
  private final TrafficLogger trafficLogger;
  
  private static AtomicInteger connectionIdFactory = new AtomicInteger(10000);
  
  private Channel outboundChannel = null;
  private int connectionId = 0;
  private AtomicInteger num = new AtomicInteger(0);
  
  private SocksProtocol socksProtocol = null;
  
  public SocksServerHandler(Properties props, ClientSocketChannelFactory clientFactory, TrafficLogger trafficLogger) {
    super();
    this.props = props;
    this.clientFactory = clientFactory;
    this.trafficLogger = trafficLogger;
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
        trafficLogger.log(connectionId, "send", num.incrementAndGet(), msg.array());
        Channels.write(outboundChannel, msg);
      }
      else {
        e.getChannel().setReadable(false);
      }
      return;
    }
    
    System.out.println("Msg = " + toHexString(msg.array()) + ", cap = " + msg.capacity());

    if (socksProtocol == null) {
      try {
        socksProtocol = SocksProtocols.create(msg);
      }
      catch(ProtocolException ex) {
        log.warn("unknown protocol for " + toHexString(msg.array()), ex);
        Channels.close(e.getChannel());
        return;
      }
    }
    
    try {
      socksProtocol.processMessage(msg);
    }
    catch(ProtocolException ex) {
      log.warn("invalid protocol " + socksProtocol + " for " + toHexString(msg.array()), ex);
      Channels.close(e.getChannel());
      return;
    }
    
    if (socksProtocol.hasResponse()) {
      Channels.write(e.getChannel(), ChannelBuffers.wrappedBuffer(socksProtocol.getResponse()));
    }
    
    if (socksProtocol.isReady()) {
      final InetSocketAddress outboundAddress = socksProtocol.getOutboundAddress();
      System.out.println("Connect " + outboundAddress);
      
      final Channel inboundChannel = e.getChannel();
      inboundChannel.setReadable(false);
      connectionId = connectionIdFactory.incrementAndGet();

      ClientBootstrap outboundClientBootstrap = new ClientBootstrap(clientFactory);
      outboundClientBootstrap.setOption("connectTimeoutMillis", props.getProperty("outbound.connect.timeout", "30000"));
      outboundClientBootstrap.getPipeline().addLast("handler", new TrafficHandler(props, inboundChannel, connectionId, trafficLogger));
      ChannelFuture outboundClientFuture = outboundClientBootstrap.connect(outboundAddress);
      outboundClientFuture.addListener(new ChannelFutureListener() {

        @Override
        public void operationComplete(ChannelFuture outboundClientFuture) throws Exception {
          if (outboundClientFuture.isSuccess()) {
            System.out.println("Outbound Channel SUCCESS " + outboundClientFuture.getChannel().getRemoteAddress());
            outboundChannel = outboundClientFuture.getChannel();
            inboundChannel.setReadable(true);
          }
          else {
            System.out.println("Outbound Channel FAIL " + outboundAddress);
            Channels.close(inboundChannel);
           }
        }
        
      });
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
