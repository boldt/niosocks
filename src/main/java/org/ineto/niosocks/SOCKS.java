package org.ineto.niosocks;

/**
 * Constants defined in the RFC 1928
 *
 * @author Dennis Boldt
 *
 */
public class SOCKS {

	public static final byte VER_5 = 0x05;

	public static final byte CMD_CONNECT = 0x01;
	public static final byte CMD_BIND = 0x02;
	public static final byte CMD_UDP = 0x03;

	public static final byte REP_SUCCEEDED = 0x00;
	public static final byte REP_GENERAL_SOCKS_SERVER_FAILURE = 0x01;
	public static final byte REP_CONNECTION_NOT_ALLOWED = 0x02;
	public static final byte REP_NETWORK_UNREACHABLE = 0x03;
	public static final byte REP_HOST_UNREACHABLE = 0x04;
	public static final byte REP_CONNECTION_REFUSED = 0x05;

	public static final byte REP_TTL_EXPIRED = 0x06;
	public static final byte REP_COMMAND_NOT_SUPPORTED = 0x07;
	public static final byte REP_ADDRESS_TYPE_NOT_SUPPORTED = 0x07;

	public static final byte RSV = 0x00;

	public static final byte ATYP_IPV4 = 0x01;
	public static final byte ATYP_DOMAINNAME = 0x03;
	public static final byte ATYP_IPV6 = 0x04;
}
