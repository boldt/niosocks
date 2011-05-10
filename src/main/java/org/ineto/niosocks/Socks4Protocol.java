package org.ineto.niosocks;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.UnknownHostException;

import org.jboss.netty.buffer.ChannelBuffer;

public class Socks4Protocol implements SocksProtocol {

  private static final byte[] RESPONSE_OK = new byte[] { 0x00, 0x5a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  
  private InetSocketAddress address = null;
  private byte[] response = null;
  
  @Override
  public void processMessage(ChannelBuffer msg) throws ProtocolException {
    if (msg.capacity() >= 8 && msg.getByte(0) == 4 && msg.getByte(1) == 1) {
      byte[] addr = new byte[] { msg.getByte(4), msg.getByte(5), msg.getByte(6), msg.getByte(7) };
      int port = (((0xFF & msg.getByte(2)) << 8) + (0xFF & msg.getByte(3)));
      try {
        address = new InetSocketAddress(InetAddress.getByAddress(addr), port);
      }
      catch(UnknownHostException e) {
        throw new ProtocolException("invalid ip address " + addr);
      }
      response = RESPONSE_OK;
    }    
    throw new ProtocolException("invalid request type");
  }

  @Override
  public boolean hasResponse() {
    return response != null;
  }

  @Override
  public byte[] getResponse() {
    return response;
  }

  @Override
  public boolean isReady() {
    return address != null;
  }

  @Override
  public InetSocketAddress getOutboundAddress() {
    return address;
  }

  @Override
  public String toString() {
    return "Socks4Protocol";
  }

}
