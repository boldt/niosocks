package org.ineto.niosocks;

import io.netty.buffer.ChannelBuffer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.UnknownHostException;

import de.uniluebeck.itm.tr.util.StringUtils;

public class Socks5Protocol implements SocksProtocol {

	 private enum Step {
		 METHOD_SELECTION,
		 CONNECT,
		 BIND;
	 };

	private Socks5Packet response = null;
	private InetSocketAddress address = null;

	// Stores the IP and port of the connection
	private byte[] ip = null;
	private byte[] port = null;

	private Step lastStep = null ;

	@Override
	public void processMessage(ChannelBuffer msg) throws ProtocolException {

		System.out.println("PROCESS:");
		System.out.println(StringUtils.toHexString(msg.array()));

		response = null;

		if (isIdentifierMethodSelectionMessage(msg) &&  lastStep == null) {
			System.out.println("STEP 1");
			/*
			 * The client connects to the server, and sends a version
			 * identifier/method selection message:
			 *
			 * +----+----------+----------+
			 * |VER | NMETHODS | METHODS  |
			 * +----+----------+----------+
			 * | 1  |    1     | 1 to 255 |
			 * +----+----------+----------+
			 */

			/*
			 * TODO: Seach for 0x00
    	  		int nmethods = msg.getByte(1);
    	  		for(int i = 0; i < nmethods; i++) {
    		  		if(msg.getByte(1 + i) == 0x00) {
    			  		// Generate the METHOD selection message
    		  		}
    	  		}
    	  		else: Set to 0xFF as Error
			 */

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

			// We just accept NO AUTHENTICATION (0x00)
			byte[] response = new byte[2];
			response[0] = (byte) 0x05;
			response[1] = (byte) 0x00;
			this.response = new SimpleSocks5Packet(response);
			this.lastStep = Step.METHOD_SELECTION;
		} else if (isConnectionRequest(msg) && lastStep == Step.METHOD_SELECTION) {
			System.out.println("STEP 2: CONNECT");
			processConnection(msg);
			lastStep = Step.CONNECT;
		} else if(isBindRequest(msg) && lastStep == Step.METHOD_SELECTION) {
			System.out.println("STEP 2: BIND");
    		processBind(msg);
    		lastStep = Step.BIND;
		} else {
			throw new ProtocolException("invalid auth request");
		}
	}

  @Override
  public void setConnected(boolean connected) {
	  if(lastStep == Step.CONNECT && this.response != null && this.response instanceof Socks5Reply) {

		  Socks5Reply reply = (Socks5Reply) this.response;
		  if(connected) {
	    		reply.setREP(SOCKS.REP.SUCCEEDED);
		  } else {
	    		reply.setREP(SOCKS.REP.GENERAL_SOCKS_SERVER_FAILURE);
		  }
	  }

	  /*
    if (step == Step.CONNECT && response != null && response.length >= 2) {
      response[1] = connected ? (byte) 0 : 1;
    }
    */
  }

  @Override
  public boolean hasResponse() {
    return response != null;
  }

  @Override
  public byte[] getResponse() {
    return response.getBytes();
  }

  @Override
  public boolean isReady() {
    return address != null;
  }

  @Override
  public InetSocketAddress getOutboundAddress() {
    return address;
  }

  private void processBind(ChannelBuffer msg) {

	  /*
	      try{
	        ss = new ServerSocket(port,backlog,localIP);
	        log("Starting SOCKS Proxy on:"+ss.getInetAddress().getHostAddress()+":"
	                                      +ss.getLocalPort());
	        while(true){
	          Socket s = ss.accept();
	          log("Accepted from:"+s.getInetAddress().getHostName()+":"
	                              +s.getPort());
	          ProxyServer ps = new ProxyServer(auth,s);
	          (new Thread(ps)).start();
	        }
	      }catch(IOException ioe){
	        ioe.printStackTrace();
	      }finally{
	      }
	  */
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
    	Socks5Reply reply = new Socks5Reply();
        reply.setATYP(SOCKS.ATYP.IPV4);

    	try {
    		connectIPv4(msg);
		} catch (UnknownHostException e) {
			reply.setREP(SOCKS.REP.HOST_UNREACHABLE);
		} catch (ProtocolException e) {
			reply.setREP(SOCKS.REP.GENERAL_SOCKS_SERVER_FAILURE);
		}

    	reply.setIp(this.ip);
    	reply.setPort(this.port);
    	this.response = reply;
    }

    // Type is domain name
    else if (addressType == 0x03) {

    	Socks5Reply reply = new Socks5Reply();
    	reply.setATYP(SOCKS.ATYP.DOMAINNAME);

    	try {
    		connectDomain(msg);
    		reply.setREP(SOCKS.REP.SUCCEEDED);
    		reply.setIp(this.ip);
    		reply.setPort(this.port);
		} catch (ProtocolException e) {
			reply.setREP(SOCKS.REP.GENERAL_SOCKS_SERVER_FAILURE);
			this.response = reply;
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

  private void connectIPv4(ChannelBuffer msg) throws ProtocolException, UnknownHostException {
    checkCapacity(msg, 10);
    msg.getBytes(4, this.ip);

    this.port[0] = msg.getByte(8);
    this.port[1] = msg.getByte(9);

	int port = (((0xFF & msg.getByte(8)) << 8) + (0xFF & msg.getByte(9)));
	address = new InetSocketAddress(InetAddress.getByAddress(this.ip), port);
	this.ip = address.getAddress().getAddress();

  }

  private void connectDomain(ChannelBuffer msg) throws ProtocolException {
    checkCapacity(msg, 5);
    int cnt = msg.getByte(4);
    checkCapacity(msg, 5 + cnt + 2);
    byte[] domain = new byte[cnt];
    msg.getBytes(5, domain);

    this.port[0] = msg.getByte(5 + cnt);
    this.port[1] = msg.getByte(5 + cnt + 1);

    int port = (((0xFF & msg.getByte(5 + cnt)) << 8) + (0xFF & msg.getByte(5 + cnt + 1)));
    address = new InetSocketAddress(new String(domain), port);
	this.ip = address.getAddress().getAddress();

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

    private boolean isConnectionRequest(ChannelBuffer msg) throws ProtocolException {
    	checkCapacity(msg, 2);
    	if (msg.getByte(0) == SOCKS.VER_5 && msg.getByte(1) == SOCKS.CMD.CONNECT.byteValue()) {
    		return true;
    	}
    	return false;
    }

    private boolean isBindRequest(ChannelBuffer msg) throws ProtocolException {
    	checkCapacity(msg, 2);
    	if (msg.getByte(0) == SOCKS.VER_5 && msg.getByte(1) == SOCKS.CMD.BIND.byteValue()) {
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
  private boolean isIdentifierMethodSelectionMessage(ChannelBuffer msg) {
    if (msg.capacity() >= 3 && msg.capacity() < 257) {
      int cnt = msg.getByte(1);
      if (msg.capacity() == cnt + 2) {
        return true;
      }
    }
    return false;
  }

  public boolean isBIND() {
	return (lastStep == Step.BIND);
  }

  public boolean isCONNECT() {
	return (lastStep == Step.CONNECT);
  }

  @Override
  public String toString() {
    return "Socks5Protocol";
  }


}
