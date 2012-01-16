package org.ineto.niosocks;

/**
 * Constants defined in the RFC 1928
 *
 * @author Dennis Boldt
 *
 */
public class SOCKS {

	public static final byte VER_5 = 0x05;
	public static final byte RSV = 0x00;

	/**
	 * Commands
	 */
	public static enum CMD {
		CONNECT(0x01),
		BIND(0x02),
		UDP(0x03);

		private int value;

		private CMD(int value) {
			this.value = value;
		}

		public byte byteValue() {
			return (new Integer(value)).byteValue();
		}
	}

	/**
	 * Methods
	 */
	public static enum METHODS {
		NO_AUTHENTICATION_REQUIRED(0x00),
		GSSAPI(0x01),
		USERNAME_PASSWORD(0x02),
		NO_ACCEPTABLE_METHODS(0xFF);

		private int value;

		private METHODS(int value) {
			this.value = value;
		}

		public byte byteValue() {
			return (new Integer(value)).byteValue();
		}
	}

	/**
	 * Response codes
	 */
	public static enum REP {
		SUCCEEDED(0x00),
		GENERAL_SOCKS_SERVER_FAILURE(0x01),
		CONNECTION_NOT_ALLOWED(0x02),
		NETWORK_UNREACHABLE(0x03),
		HOST_UNREACHABLE(0x04),
		CONNECTION_REFUSED(0x05),
		TTL_EXPIRED(0x06),
		COMMAND_NOT_SUPPORTED(0x07),
		ADDRESS_TYPE_NOT_SUPPORTED(0x08);

		private int value;

		private REP(int value) {
			this.value = value;
		}

		public byte byteValue() {
			return (new Integer(value)).byteValue();
		}
	}

	/**
	 * Address types
	 */
	public static enum ATYP {
		IPV4(0x01),
		DOMAINNAME(0x03),
		IPV6(0x04);

		private int value;

		private ATYP(int value) {
			this.value = value;
		}

		public byte byteValue() {
			return (new Integer(value)).byteValue();
		}
	}

}
