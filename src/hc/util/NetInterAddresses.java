package hc.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Vector;

public class NetInterAddresses {
	public final NetworkInterface ni;
	final Vector<InetAddress> addres;

	public NetInterAddresses(final NetworkInterface ni) {
		this.ni = ni;
		addres = getInetAddresses(ni.getInetAddresses());
	}

	public final InetAddress getWLANInetAddress() {
		final int size = addres.size();
		for (int i = 0; i < size; i++) {
			final InetAddress ia = addres.elementAt(i);
			// ---isLinkLocalAddress---
			// 当IP地址是本地连接地址(LinkLocalAddress)时返回true，否则返回false。IPv4的本地连接地址的范围是169.254.0.0
			// ~ 169.254.255.255。
			// IPv6的本地连接地址的前12位是FE8，其他的位可以是任意取值，如FE88::、FE80::ABCD::都是本地连接地址。
			// ---isSiteLocalAddress---
			// 当IP地址是地区本地地址（SiteLocalAddress）时返回true，否则返回false。IPv4的地址本地地址分为三段：10.0.0.0
			// ~ 10.255.255.255、172.16.0.0 ~ 172.31.255.255、192.168.0.0 ~
			// 192.168.255.255。
			// IPv6的地区本地地址的前12位是FEC，其他的位可以是任意取值，如FED0::、FEF1::都是地区本地地址。
			if (ia.isSiteLocalAddress() && (ia.isLinkLocalAddress() == false) && (ia.isLoopbackAddress() == false)
					&& (ia.isMulticastAddress() == false)) {
				return ia;
			}
		}
		return null;
	}

	private final Vector<InetAddress> getInetAddresses(final Enumeration<InetAddress> en) {
		final Vector<InetAddress> out = new Vector<InetAddress>();
		while (en.hasMoreElements()) {
			out.add(en.nextElement());
		}
		return out;
	}
}
