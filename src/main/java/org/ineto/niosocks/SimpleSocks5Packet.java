package org.ineto.niosocks;

public class SimpleSocks5Packet implements Socks5Packet {

	byte[] bytes;

	public SimpleSocks5Packet(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public byte[] getBytes() {
		return bytes;
	}

}
