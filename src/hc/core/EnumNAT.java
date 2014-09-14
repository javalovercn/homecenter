package hc.core;


public class EnumNAT {
	/**
	 * 主机具有公网IP，允许主动发起和被动响应两种方式的UDP通信。
	 */
	public static final int OPEN_INTERNET = 1;

	/**
	 * 当内网主机创建一个UDP socket并通过它第一次向外发送UDP数据包时，
	 * NAT会为之分配一个固定的公网{IP:端口}。
	 * 此后，通过这个socket发送的任何UDP数据包都是通过这个公网{IP:端口}发送出去的；
	 * 同时，任何外部主机都可以使用这个公网{IP:端口}向该socket发送UDP数据包。
	 * 即是说，NAT维护了一个映射表，内网主机的内网{IP:端口}与公网{IP:端口}是一一对应的关系。
	 * 一旦这个映射关系建立起来（内部主机向某一外部主机发送一次数据即可），
	 * 任何外部主机就可以直接向NAT内的这台主机发起UDP通信了，此时NAT透明化了。
	 */
	public static final int FULL_CONE_NAT = 2;
	
	/**
	 * 主机具有公网IP，但位于防火墙之后，且防火墙阻止了外部主机的主动UDP通信。
	 */
	public static final int SYMMETRIC_FIREWALL = 3;
	
	/**
	 * 位于防火墙之后，并且防火墙阻止了UDP通信。
	 */
	public static final int UDP_BLOCKED = 4;
	
	/**
	 * 当内网主机创建一个UDP socket并通过它第一次向外发送UDP数据包时，NAT会为之分配一个公网{IP:端口}。
	 * 此后，通过这个socket向外发送的任何UDP数据包都是通过这个公网{IP:端口}发送出去的；
	 * 而任何收到过从这个socket发送来的数据的外部主机（由IP标识），都可以通过这个公网{IP:端口}向该socket发送UDP数据包。
	 * 即是说，NAT维护了一个内网{IP:端口}到公网{IP:端口}的映射，还维护了一个{外部主机IP, 公网{IP:端口}}到内网{IP:端口}的映射。
	 * 因此，要想外部主机能够主动向该内部主机发起通信，必须先由该内部主机向这个外部发起一次通信。
	 */
	public static final int RESTRICTED_CONE_NAT = 5;
	
	/**
	 * 当内网主机创建一个UDP socket并通过它第一次向外发送UDP数据包时，NAT会为之分配一个公网{IP:端口}。
	 * 此后，通过这个socket向外部发送的任何UDP数据包都是通过这个公网{IP:端口}发送出去的；
	 * 一旦外部主机在{IP:端口}上收到过从这个socket发送来的数据后，都可以通过这个外部主机{IP:端口}向该socket发送UDP数据包。
	 * 即是说，NAT维护了一个从内网{IP:端口}到公网{IP:端口}的映射，
	 * 还维护了一个从{外部主机{IP:端口}, 公网{IP:端口}}到内网{IP:端口}的映射。
	 */
	public static final int PORT_RESTRICTED_CONE_NAT = 6;
	
	/**
	 * 当内网主机创建一个UDP socket并通过它第一次向外部主机1发送UDP数据包时，NAT为其分配一个公网{IP1:端口1}，
	 * 以后内网主机发送给外部主机1的所有UDP数据包都是通过公网{IP1:端口1}发送的；
	 * 当内网主机通过这个socket向外部主机2发送UDP数据包时，NAT为其分配一个公网{IP2:端口2}，
	 * 以后内网主机发送给外部主机2的所有UDP数据包都是通过公网{IP2:端口2}发送的。
	 * 公网{IP1:端口1}和公网{IP2:端口2}一定不会完全相同（即要么IP不同，要么端口不同，或者都不同）。
	 * 这种情况下，外部主机只能在接收到内网主机发来的数据时，才能向内网主机回送数据。
	 */
	public static final int SYMMETRICT_NAT = 7;
	
	public static final int FULL_AGENT_BY_OTHER = 8;
	
	/**
	 * 服务器以UPnP方式等待接入
	 */
	public static final int SERVER_AT_UPnP = 9;
	
	public static String getNatDesc(int natType){
		if (natType == OPEN_INTERNET) return "Net status : Open access to the Internet";
		if (natType == UDP_BLOCKED) return "Net status : Firewall blocks UDP";
		if (natType == FULL_CONE_NAT) return ("Net status : Full Cone NAT handles connections");
		if (natType == RESTRICTED_CONE_NAT) return ("Net status : Restricted Cone NAT handles connections");
		if (natType == PORT_RESTRICTED_CONE_NAT) return ("Net status : Port restricted Cone NAT handles connections");
		if (natType == SYMMETRICT_NAT) return ("Net status : Symmetric Cone NAT handles connections");
		if (natType == SYMMETRIC_FIREWALL) return ("Net status : Symmetric UDP Firewall handles connections");
		return "Net status : Unknown Nat Type!";
	}
}
