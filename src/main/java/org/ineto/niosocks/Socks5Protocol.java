package org.ineto.niosocks;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.UnknownHostException;

import org.jboss.netty.buffer.ChannelBuffer;

public class Socks5Protocol implements SocksProtocol {

  public static final int VERSION = 5;
  
  public enum SocksAuth {
    NO_AUTH,
    GSSAPI,
    USER_PASS;
  }
  
  public enum AddressType {
    UNK,
    IPv4,
    DOMAIN,
    IPv6;
  }
  
  private static final int ASK_AUTH = 0;
  private static final int REQUEST = 1;
  
  private int step = 0;
  private byte[] response = null;
  private InetSocketAddress address = null;
  
  @Override
  public void processMessage(ChannelBuffer msg) throws ProtocolException {
    response = null;
    switch(step++) {
    case ASK_AUTH:
      if (isAskAuth(msg)) {
        response = selectAuth(SocksAuth.NO_AUTH);
      }
      else {
        throw new ProtocolException("invalid auth request");
      }
      break;
    case REQUEST:
      if (isConnectionRequest(msg)) {
        processConnection(msg);
        response = msg.array();
        response[1] = 0;
      }
      else {
        throw new ProtocolException("unsupported command");
      }
      break;
    }
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

  private void processConnection(ChannelBuffer msg) throws ProtocolException {
    if (!isConnectionRequest(msg)) {
      throw new ProtocolException("unsupported command");
    }
    checkCapacity(msg, 4);
    int addressType = msg.getByte(3);
    if (addressType == AddressType.IPv4.ordinal()) {
      connectIPv4(msg);
    }
    else if (addressType == AddressType.DOMAIN.ordinal()) {
      connectDomain(msg);
    }
    else if (addressType == AddressType.IPv6.ordinal()) {
      connectIPv6(msg);
    }
    else {
      throw new ProtocolException("unsupported address type " + addressType);
    }      
  }
  
  public void connectIPv4(ChannelBuffer msg) throws ProtocolException {
    checkCapacity(msg, 10);
    byte[] addr = new byte[4];
    msg.getBytes(4, addr);
    int port = (((0xFF & msg.getByte(8)) << 8) + (0xFF & msg.getByte(9)));
    try {
      address = new InetSocketAddress(InetAddress.getByAddress(addr), port);
    }
    catch(UnknownHostException e) {
      throw new ProtocolException("invalid ip address " + addr);
    }
  }
  
  public void connectDomain(ChannelBuffer msg) throws ProtocolException {
    checkCapacity(msg, 5);
    int cnt = msg.getByte(4);
    checkCapacity(msg, 5 + cnt + 2);
    byte[] domain = new byte[cnt];
    msg.getBytes(5, domain);
    int port = (((0xFF & msg.getByte(5 + cnt)) << 8) + (0xFF & msg.getByte(5 + cnt + 1)));
    address = new InetSocketAddress(new String(domain), port);
  }
  
  public void connectIPv6(ChannelBuffer msg) throws ProtocolException {
    checkCapacity(msg, 22);
    byte[] addr = new byte[16];
    msg.getBytes(4, addr);
    int port = (((0xFF & msg.getByte(20)) << 8) + (0xFF & msg.getByte(21)));
    try {
      address = new InetSocketAddress(InetAddress.getByAddress(addr), port);
    }
    catch(UnknownHostException e) {
      throw new ProtocolException("invalid ip address " + addr);
    }
  }  
  
  public static boolean isConnectionRequest(ChannelBuffer msg) throws ProtocolException {
    checkCapacity(msg, 3);
    if (msg.getByte(0) == 5 && msg.getByte(1) == 1 && msg.getByte(2) == 0) {
      return true;
    }
    return false;
  }
  
  public static void checkCapacity(ChannelBuffer msg, int need) throws ProtocolException {
    if (msg.capacity() < need) {
      throw new ProtocolException("invalid capacity: need " + need + ", has " + msg.capacity());
    }
  }
  
  public static boolean isAskAuth(ChannelBuffer msg) {
    if (msg.capacity() >= 2 && msg.getByte(0) == 5) {
      int cnt = msg.getByte(1);
      if (msg.capacity() == cnt + 2) {
        return true;
      }
    }
    return false;
  }
  
  public static byte[] selectAuth(SocksAuth auth) {
    byte[] response = new byte[2];
    response[0] = (byte) VERSION;
    response[1] = (byte) auth.ordinal();
    return response;
  }
  
  @Override
  public String toString() {
    return "Socks5Protocol";
  }
  
  
}
