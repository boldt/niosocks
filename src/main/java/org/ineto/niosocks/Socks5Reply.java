package org.ineto.niosocks;

import java.net.InetAddress;

import de.uniluebeck.itm.tr.util.StringUtils;

public class Socks5Reply implements Socks5Packet {

	private Byte ATYP = null;
	private Byte REP = null;
	private byte[] ip = null;
	private byte[] port = null;

	@Override
	public byte[] getBytes() {

		byte[] reply = new byte[10];
		if(ATYP.byteValue() == SOCKS.ATYP_IPV4 || ATYP.byteValue() == SOCKS.ATYP_DOMAINNAME) {
			reply[4] = this.ip[0];
			reply[5] = this.ip[1];
			reply[6] = this.ip[2];
			reply[7] = this.ip[3];
			reply[8] = this.port[0];
			reply[9] = this.port[1];
		} else if(ATYP.byteValue() == SOCKS.ATYP_IPV6) {
			REP = SOCKS.REP_COMMAND_NOT_SUPPORTED;
		}

		reply[0] = SOCKS.VER_5;
		reply[1] = REP.byteValue();
		reply[2] = SOCKS.RSV;
		reply[3] = ATYP.byteValue();

		return reply;
	}

	public void setIp(InetAddress ip) {
		this.ip = ip.getAddress();
	}

	public void setIp(byte[] ip) {
		this.ip = ip;
	}

	public void setPort(Integer port) {
		this.port = new byte[2];
		this.port[0] = (byte) (0xFF & (port.byteValue() >> 8));
		this.port[1] = (byte) (0xFF & port.byteValue());
	}

	public void setPort(byte[] port) {
		if(port.length == 2) {
			this.port = port;
		}
	}

	public void setPort(byte port1, byte port2) {
		this.port = new byte[2];
		this.port[0] = port1;
		this.port[1] = port2;
	}

	public void setATYP(Byte aTYP) {
		this.ATYP = aTYP;
	}

	public void setREP(Byte rEP) {
		this.REP = rEP;
	}

	@Override
	public String toString() {
		return StringUtils.toHexString(this.getBytes());
	}

}
