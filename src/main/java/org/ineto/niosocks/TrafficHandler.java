package org.ineto.niosocks;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class TrafficHandler extends SimpleChannelUpstreamHandler {

  private final int connectionId;
  private final Channel channel;
  private byte[] contentRemoveByteArray;
  private AtomicInteger num = new AtomicInteger(0);
  
  public TrafficHandler(Properties props, Channel channel, int connectionId) {
    this.channel = channel;
    this.connectionId = connectionId;
    String contentRemove = props.getProperty("content.modifier.remove", null);
    if (contentRemove != null) {
      try {
        contentRemoveByteArray = contentRemove.getBytes("UTF-8");
      }
      catch(Exception e) {
      }
    }
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
    ChannelBuffer msg = (ChannelBuffer) e.getMessage();
    
    /*
    byte[] content = msg.toByteBuffer().array();

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    for (int i = 0; i != content.length; ++i) {
      if (contentRemoveByteArray != null && content[i] == contentRemoveByteArray[0]) {
        boolean match = true;
        for (int j = 0; j != contentRemoveByteArray.length; ++j) {
          if (i + j == content.length || content[i+j] != contentRemoveByteArray[j]) {
            match = false;
            break;
          }
        }
        if (match) {
          i += contentRemoveByteArray.length - 1;
        }
        else {
          bout.write(content[i]);
        }
      }
      else {
        bout.write(content[i]);
      }
    }
    */
    
    if (channel.isWritable()) {
      FileUtils.writeLog(connectionId, "recv", num.incrementAndGet(), msg.array());
      Channels.write(channel, msg);//ChannelBuffers.wrappedBuffer(bout.toByteArray()));
    }
    else {
      e.getChannel().setReadable(false);
    }
  }

  @Override
  public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    if (e.getChannel().isWritable()) {
      channel.setReadable(true);
    }
  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    closeOnFlush(channel);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    e.getCause().printStackTrace();
    closeOnFlush(e.getChannel());
  }

  private static void closeOnFlush(Channel ch) {
    if (ch.isConnected()) {
      ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

}
