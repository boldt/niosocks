package org.ineto.niosocks;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.UnknownHostException;

import io.netty.buffer.ChannelBuffer;

public class Socks4Protocol implements SocksProtocol {

  private static final byte[] RESPONSE_OK = new byte[] { 0x00, 0x5a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
  private static final byte[] RESPONSE_FAIL = new byte[] { 0x00, 0x5b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

  private InetSocketAddress address = null;
  private byte[] response = null;

  @Override
  public void processMessage(ChannelBuffer msg) throws ProtocolException {
    response = null;
    if (msg.capacity() >= 8 && msg.getByte(0) == 4 && msg.getByte(1) == 1) {
      byte[] addr = new byte[4];
      msg.getBytes(4, addr);
      int port = (((0xFF & msg.getByte(2)) << 8) + (0xFF & msg.getByte(3)));
      try {
        address = new InetSocketAddress(InetAddress.getByAddress(addr), port);
      }
      catch(UnknownHostException e) {
        throw new ProtocolException("invalid ip address " + addr);
      }
    }
    else {
      throw new ProtocolException("invalid request type");
    }
  }

  @Override
  public void setConnected(boolean connected) {
    response = connected ? RESPONSE_OK : RESPONSE_FAIL;
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
