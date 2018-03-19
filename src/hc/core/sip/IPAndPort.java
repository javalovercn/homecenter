package hc.core.sip;

public class IPAndPort {
	public String ip;
	public int port;
	public int udpPort;

	public IPAndPort() {
		this.ip = "";
	}

	public IPAndPort(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public void reset() {
		ip = "";
		port = 0;
		udpPort = 0;
	}
}
