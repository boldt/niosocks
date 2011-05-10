package org.ineto.niosocks;

import java.net.InetSocketAddress;
import java.net.ProtocolException;

import org.jboss.netty.buffer.ChannelBuffer;

public interface SocksProtocol {
  
  public void processMessage(ChannelBuffer msg) throws ProtocolException;
  
  public boolean hasResponse();
  
  public byte[] getResponse();
  
  public boolean isReady();
  
  public InetSocketAddress getOutboundAddress();
  
}
