package org.ineto.niosocks;

import java.net.ProtocolException;

import org.jboss.netty.buffer.ChannelBuffer;

public class SocksProtocols {

  public static SocksProtocol create(ChannelBuffer msg) throws ProtocolException {
    if (msg.capacity() >= 1) {
      if (msg.getByte(0) == 4) {
        return new Socks4Protocol();
      }
      else if (msg.getByte(0) == 5) {
        return new Socks5Protocol();
      }      
    }
    throw new ProtocolException("unknown protocol");
  }
  
}
