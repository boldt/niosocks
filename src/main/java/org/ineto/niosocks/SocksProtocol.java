package org.ineto.niosocks;

import io.netty.buffer.ChannelBuffer;

import java.net.InetSocketAddress;
import java.net.ProtocolException;

public interface SocksProtocol {

  public void processMessage(ChannelBuffer msg) throws ProtocolException;

  public boolean hasResponse();

  public byte[] getResponse();

  public boolean isReady();

  public InetSocketAddress getOutboundAddress();

  public void setConnected(boolean connected);

}
