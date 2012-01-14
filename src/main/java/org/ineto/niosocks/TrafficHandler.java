package org.ineto.niosocks;

import io.netty.buffer.ChannelBuffer;
import io.netty.buffer.ChannelBuffers;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelStateEvent;
import io.netty.channel.Channels;
import io.netty.channel.ExceptionEvent;
import io.netty.channel.MessageEvent;
import io.netty.channel.SimpleChannelUpstreamHandler;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class TrafficHandler extends SimpleChannelUpstreamHandler {

  private final Channel channel;
  private final int connectionId;
  private final TrafficLogger trafficLogger;

  private byte[] contentRemoveByteArray;
  private AtomicInteger num = new AtomicInteger(0);

  public TrafficHandler(Properties props, Channel channel, int connectionId, TrafficLogger trafficLogger) {
    this.channel = channel;
    this.connectionId = connectionId;
    this.trafficLogger = trafficLogger;
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
      trafficLogger.log(connectionId, "recv", num.incrementAndGet(), msg.array());
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
