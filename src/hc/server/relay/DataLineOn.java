package hc.server.relay;

public class DataLineOn {
	public String id;
	public String ip;
	public String port;
	public String nattype;
	public String agent;
	public String upnpip;
	public String upnpport;
	public String relayip = "";
	public String relayport = "";
	public String token;
	public String hideIP;
	public int serverNum = 0;
	public String hideToken;
	public long alive = System.currentTimeMillis();
	
	public int getLenid(){
		return id.length();
	}
}
