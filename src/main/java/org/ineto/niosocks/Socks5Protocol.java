package org.ineto.niosocks;

import io.netty.buffer.ChannelBuffer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.UnknownHostException;

public class Socks5Protocol implements SocksProtocol {

  public static final int VERSION = 5;

  public enum Step {
    ASK_AUTH,
    REQUEST,
    CONNECT;
  }

  private Step step = Step.ASK_AUTH;
  private byte[] response = null;
  private InetSocketAddress address = null;

  private byte[] ip = null;
  private byte[] port = null;

  @Override
  public void processMessage(ChannelBuffer msg) throws ProtocolException {

	  ip = new byte[4];
	  port = new byte[2];
    response = null;
    switch(step) {
    case ASK_AUTH:
      if (isAskAuth(msg)) {
        response = new byte[2];
        response[0] = (byte) VERSION;
        response[1] = (byte) 0x00;
      }
      else {
        throw new ProtocolException("invalid auth request");
      }
      break;
    case REQUEST:
      if (isConnectionRequest(msg)) {
    	  processConnection(msg);
      }
      else {
        throw new ProtocolException("unsupported command");
      }
      break;
    }
    step = Step.values()[step.ordinal() + 1];
  }

  @Override
  public void setConnected(boolean connected) {
    if (step == Step.CONNECT && response != null && response.length >= 2) {
      response[1] = connected ? (byte) 0 : 1;
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

	  System.out.println("processConnection(..)");

    if (!isConnectionRequest(msg)) {
      throw new ProtocolException("unsupported command");
    }
    checkCapacity(msg, 4);

    response = new byte[10];
    response[0] = 0x05;
    response[1] = 0x01;
    response[2] = 0x00;

    int addressType = msg.getByte(3);

    // Type is IPv4
    if (addressType == 0x01) {

    	response[3] = 0x01;
      connectIPv4(msg);
    }

    // Type is domain name
    else if (addressType == 0x03) {
    	response[3] = 0x03;
      connectDomain(msg);
    }

    // Type is IPv6
    else if (addressType == 0x04) {
    	response[3] = 0x04;
      connectIPv6(msg);
    }
    else {
      throw new ProtocolException("unsupported address type " + addressType);
    }
    response[4] = this.ip[0];
    response[5] = this.ip[1];
    response[6] = this.ip[2];
    response[7] = this.ip[3];
    response[8] = this.port[0];
    response[9] = this.port[1];
  }

  public void connectIPv4(ChannelBuffer msg) throws ProtocolException {
    checkCapacity(msg, 10);
    msg.getBytes(4, this.ip);

    this.port[0] = msg.getByte(8);
    this.port[1] = msg.getByte(9);

    try {
    	System.out.println("CONNECT TO:");
    	System.out.println(this.ip);
    	System.out.println(this.port);
    	int port = (((0xFF & msg.getByte(8)) << 8) + (0xFF & msg.getByte(9)));
    	address = new InetSocketAddress(InetAddress.getByAddress(this.ip), port);
    }
    catch(UnknownHostException e) {
      throw new ProtocolException("invalid ip address " + this.ip);
    }
  }

  public void connectDomain(ChannelBuffer msg) throws ProtocolException {
    checkCapacity(msg, 5);
    int cnt = msg.getByte(4);
    checkCapacity(msg, 5 + cnt + 2);
    byte[] domain = new byte[cnt];
    msg.getBytes(5, domain);

    this.port[0] = msg.getByte(5 + cnt);
    this.port[1] = msg.getByte(5 + cnt + 1);

    int port = (((0xFF & msg.getByte(5 + cnt)) << 8) + (0xFF & msg.getByte(5 + cnt + 1)));
    address = new InetSocketAddress(new String(domain), port);

    String ipAddress = address.getAddress().getHostAddress().toString();
    String[] ipParts = ipAddress.split("\\.");

    this.ip[0] = (byte) Integer.parseInt(ipParts[0]);
    this.ip[1] = (byte) Integer.parseInt(ipParts[1]);
    this.ip[2] = (byte) Integer.parseInt(ipParts[2]);
    this.ip[3] = (byte) Integer.parseInt(ipParts[3]);
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
    // version = 0x05 && connection = 0x01
    if (msg.getByte(0) == 5 && msg.getByte(1) == 1) {
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

  @Override
  public String toString() {
    return "Socks5Protocol";
  }


}
