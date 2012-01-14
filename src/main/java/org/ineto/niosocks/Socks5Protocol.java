package org.ineto.niosocks;

import io.netty.buffer.ChannelBuffer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.UnknownHostException;

public class Socks5Protocol implements SocksProtocol {

  private enum Step {
    ASK_AUTH,
    REQUEST,
    CONNECT;
  }

  // Start with the AUTH
  private Step step = Step.ASK_AUTH;
  private byte[] response = null;
  private InetSocketAddress address = null;

  // Stores the IP and prot of the connection
  private byte[] ip = null;
  private byte[] port = null;

  @Override
  public void processMessage(ChannelBuffer msg) throws ProtocolException {

    response = null;
    switch(step) {
	  /* The client connects to the server, and sends a version
	   * identifier/method selection message:
	   *
       * +----+----------+----------+
       * |VER | NMETHODS | METHODS  |
       * +----+----------+----------+
       * | 1  |    1     | 1 to 255 |
       * +----+----------+----------+
       *
	   */
    case ASK_AUTH:
      if (isAskAuth(msg)) {
    	  /*
    	   * The server selects from one of the methods given in METHODS, and
    	   * sends a METHOD selection message:
		   *
           * +----+--------+
           * |VER | METHOD |
           * +----+--------+
           * | 1  |   1    |
           * +----+--------+
           *
           */
    	  // We just accept NO AUTHENTICATION (0x00), so lets search for it
    	  response = new byte[2];
    	  response[0] = (byte) 0x05;
    	  response[1] = (byte) 0x00;
    	  /*
    	  int nmethods = msg.getByte(1);
    	  for(int i = 0; i < nmethods; i++) {
    		  if(msg.getByte(1 + i) == 0x00) {
    			  // Generate the METHOD selection message
    		  }
    	  }
    	  */
      }
      else {
        throw new ProtocolException("invalid auth request");
      }
      break;
    case REQUEST:

    	/*
        The SOCKS request is formed as follows:

            +----+-----+-------+------+----------+----------+
            |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
            +----+-----+-------+------+----------+----------+
            | 1  |  1  | X'00' |  1   | Variable |    2     |
            +----+-----+-------+------+----------+----------+
     */

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

	  ip = new byte[4];
	  port = new byte[2];

    checkCapacity(msg, 4);

/*
 *
	The server evaluates the request, and
   	returns a reply formed as follows:
    +----+-----+-------+------+----------+----------+
    |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
    +----+-----+-------+------+----------+----------+
    | 1  |  1  | X'00' |  1   | Variable |    2     |
    +----+-----+-------+------+----------+----------+
 *
 */

    int addressType = msg.getByte(3);

    // Type is IPv4
    if (addressType == 0x01) {
        response = new byte[10];
        response[0] = 0x05; // Version is SOCKS 5
        response[2] = 0x00; // RSV is reserved
    	response[3] = 0x01; // Address type is IP V4

    	try {
    		connectIPv4(msg);
    		response[1] = 0x00; // succeeded
		} catch (UnknownHostException e) {
			response[1] = 0x04; // Host unreachable
		} catch (ProtocolException e) {
			response[1] = 0x01; // General SOCKS server failure
		}

    	// Port and ip are known because IP V4 is uses
    	// parsed in the connectIPv4()
        response[4] = this.ip[0];
        response[5] = this.ip[1];
        response[6] = this.ip[2];
        response[7] = this.ip[3];
        response[8] = this.port[0];
        response[9] = this.port[1];

    }

    // Type is domain name
    else if (addressType == 0x03) {

    	response = new byte[10];
        response[0] = 0x05; // Version is SOCKS 5
        response[2] = 0x00; // RSV is reserved
    	response[3] = 0x03; // Address type is DOMAINNAME

    	try {
    		connectDomain(msg);
    		response[1] = 0x00; // succeeded
    	  	// Port and ip are known after the connection to the domain was successful.
            response[4] = this.ip[0];
            response[5] = this.ip[1];
            response[6] = this.ip[2];
            response[7] = this.ip[3];
            response[8] = this.port[0];
            response[9] = this.port[1];
		} catch (ProtocolException e) {
			response[1] = 0x01; // General SOCKS server failure
		}
    }

    // Type is IPv6
    else if (addressType == 0x04) {
    	// not verified until now.
    	//response[3] = 0x04;
        //connectIPv6(msg);
    }
    else {
      throw new ProtocolException("unsupported address type " + addressType);
    }
  }

  public void connectIPv4(ChannelBuffer msg) throws ProtocolException, UnknownHostException {
    checkCapacity(msg, 10);
    msg.getBytes(4, this.ip);

    this.port[0] = msg.getByte(8);
    this.port[1] = msg.getByte(9);

	int port = (((0xFF & msg.getByte(8)) << 8) + (0xFF & msg.getByte(9)));
	address = new InetSocketAddress(InetAddress.getByAddress(this.ip), port);
    addressToByte(address);
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
    addressToByte(address);
  }

  private void addressToByte(InetSocketAddress address) {
	    String ipAddress = address.getAddress().getHostAddress().toString();
	    String[] ipParts = ipAddress.split("\\.");
	    this.ip[0] = (byte) Integer.parseInt(ipParts[0]);
	    this.ip[1] = (byte) Integer.parseInt(ipParts[1]);
	    this.ip[2] = (byte) Integer.parseInt(ipParts[2]);
	    this.ip[3] = (byte) Integer.parseInt(ipParts[3]);
  }


  /*
   * NOT TESTED
   */
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

  /**
   *
   * @param msg
   * @return
   */
  public static boolean isAskAuth(ChannelBuffer msg) {
    if (msg.capacity() >= 3 && msg.capacity() < 257) {
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
