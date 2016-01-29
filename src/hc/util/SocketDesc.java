package hc.util;

import hc.core.util.StringUtil;
import hc.server.ui.design.HCPermissionConstant;

public class SocketDesc {
	private static final String SPLIT = ",";
	public static final String ACCEPT_TIP = "<html>accept TCP connect (server mode) or UDP data (maybe client mode) from remote." +
			"<br>Related API :" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.ServerSocket</STRONG>.accept()" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.DatagramSocket</STRONG>.receive(DatagramPacket p)" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.MulticastSocket</STRONG>.joinGroup(InetAddress mcastaddr)/leaveGroup(InetAddress mcastaddr)" +
			"<br>Permissions may be required :" +
			"<br> <STRONG>·</STRONG> " + HCPermissionConstant.READ_SYSTEM_PROPERTIES +
			"</html>";
	public static final String CONNECT_TIP = "<html>make a TCP connect (client mode) or send UDP data (maybe server mode) to remote." +
			"<br>Related API :" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.Socket</STRONG>.Socket(...)" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.DatagramSocket</STRONG>.send(DatagramPacket p)" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.MulticastSocket</STRONG>.send(DatagramPacket p, byte ttl)/joinGroup(InetAddress mcastaddr)/leaveGroup(InetAddress mcastaddr)" +
			"<br>Permissions may be required :" +
			"<br> <STRONG>·</STRONG> " + HCPermissionConstant.READ_SYSTEM_PROPERTIES +
			"</html>";
	public static final String LISTEN_TIP = "<html>listen on localhost or construct an instance of DatagramSocket for UDP." +
			"<br>Related API :" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.ServerSocket</STRONG>.ServerSocket(...)" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.DatagramSocket</STRONG>.DatagramSocket(...)" +
			"<br> <STRONG>·</STRONG> <STRONG>java.net.MulticastSocket</STRONG>.MulticastSocket(...)" +
			"</html>";
	public static final String RESOLVE_TIP = "resolve host/ip name service lookups. " +
			"The \"resolve\" is automatically added when any of the other three are specified.";
	
	public static final int ACCEPT = 1;
	public static final int CONNECT = 1 << 1;
	public static final int LISTEN = 1 << 2;
	public static final int RESOLVE = 1 << 3;
	
	public static final String STR_ACCEPT = "accept";
	public static final String STR_CONNECT = "connect";
	public static final String STR_LISTEN = "listen";
	public static final String STR_RESOLVE = "resolve";
	
	private String host;
	private String ip;
	private String port;
	private String portFrom;
	private String portTo;
	private int action;
	
	private static final String ITEM_JOIN = "|";
	
	public static final SocketDesc decode(String serial){
		SocketDesc socket = new SocketDesc("", "", "", "", "", "");
		
		String[] items = StringUtil.splitToArray(serial, SPLIT);
		
		int idx = 0;
		
		socket.host = getValueAt(items, idx++);
		socket.ip = getValueAt(items, idx++);
		socket.port = getValueAt(items, idx++);
		socket.portFrom = getValueAt(items, idx++);
		socket.portTo = getValueAt(items, idx++);
		socket.action = Integer.valueOf(getValueAt(items, idx++));
		
		return socket;
	}
	
	private static String getValueAt(String[] items, int idx){
		String item = items[idx];
		return item.substring(item.indexOf(ITEM_JOIN) + 1);
	}
	
	public static final String encode(SocketDesc socket){
		StringBuilder sb = new StringBuilder();
		
		String[] items = {socket.host, socket.ip, socket.port, socket.portFrom, socket.portTo, String.valueOf(socket.action)};
		for (int i = 0; i < items.length; i++) {
			if(i != 0){
				sb.append(SPLIT);
			}
			sb.append(i);
			sb.append(ITEM_JOIN);
			sb.append(items[i]);
		}
		
		return sb.toString();
	}
	
	public SocketDesc(String host, String ip, String port, String portFrom, String portTo, String action){
		this.host = (host==null?"":host);
		this.ip = (ip==null?"":ip);
		this.port = (port==null?"":port);
		this.portFrom = (portFrom==null?"":portFrom);
		this.portTo = (portTo==null?"":portTo);
		
		action = ((action==null)?"":action);
		String[] list = StringUtil.splitToArray(action, SPLIT);
		for (int i = 0; i < list.length; i++) {
			list[i] = list[i].trim();
		}
		if(isAction(list, STR_ACCEPT)){
			setAction(ACCEPT, true);
		}
		if(isAction(list, STR_CONNECT)){
			setAction(CONNECT, true);
		}
		if(isAction(list, STR_LISTEN)){
			setAction(LISTEN, true);
		}
		if(isAction(list, STR_RESOLVE)){
			setAction(RESOLVE, true);
		}
	}
	
	private final boolean isAction(String[] actions, String targetAction){
		for (int i = 0; i < actions.length; i++) {
			if(actions[i].equals(targetAction)){
				return true;
			}
		}
		return false;
	}
	
	public final String getHostIPDesc(){
		StringBuilder sb = new StringBuilder();
		
		if(isIPMode() == false){
			final String hostIP = getHost();
			sb.append(hostIP.length() == 0?"192.168.1.1":hostIP);
		}else{
			sb.append(getIp());
		}
		sb.append(":");
		if(isRangeMode()){
			try{
				int from = Integer.parseInt(getPortFrom());
				int to = Integer.parseInt(getPortTo());
				
				if(from > to){
					int swap = to;
					to = from;
					from = swap;
	
					sb.append(from);
					sb.append("-");
					sb.append(to);
				}else{
					sb.append(getPortFrom());
					sb.append("-");
					sb.append(getPortTo());
				}
			}catch (Throwable e) {
				sb.append(getPortFrom());
				sb.append("-");
				sb.append(getPortTo());
			}
		}else{
			sb.append(getPort());
		}
		
		return sb.toString();
	}
	
	public final String getActionDesc(){
		StringBuilder sb = new StringBuilder();
		
		if(isAcceptAction()){
			sb.append(STR_ACCEPT);
		}
		if(isConnectAction()){
			if(sb.length() > 0){
				sb.append(SPLIT);
			}
			sb.append(STR_CONNECT);
		}
		if(isListenAction()){
			if(sb.length() > 0){
				sb.append(SPLIT);
			}
			sb.append(STR_LISTEN);
		}
		if(isResolveAction()){
			if(sb.length() > 0){
				sb.append(SPLIT);
			}
			sb.append(STR_RESOLVE);
		}
		
		return sb.toString();
	}
	
	public void setAction(int action, boolean checked){
		if(checked){
			this.action |= action;
		}else{
			this.action &= (~action);
		}
	}
	
	public int getActionValue(){
		return action;
	}
	
	public boolean isAcceptAction(){
		return (action & ACCEPT )!= 0;
	}
	
	public boolean isConnectAction(){
		return (action & CONNECT) != 0;
	}
	
	public boolean isListenAction(){
		return (action & LISTEN) != 0;
	}
	
	public boolean isResolveAction(){
		return (action & RESOLVE) != 0;
	}
	
	public String getHost() {
		return host;
	}
	
	private boolean forceIP = false;
	
	public final void setIPMode(boolean ip){
		forceIP = ip;
	}
	
	public final boolean isIPMode(){
		return (forceIP || (host.length() == 0) 
				&& (((ip.length() == 0) || (ip.length() == 3)) == false));
	}
	
	private boolean forcePort = false;
	
	public final void setPortMode(boolean port){
		forcePort = port;
	}
	
	public final boolean isRangeMode(){
		if(forcePort){
			return false;
		}
		return (port.length() == 0) 
				&& (!((portFrom.length() == 0) && (portTo.length() == 0)));
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getPort() {
		if(port.length() == 0){
			return "80";
		}else{
			return port;
		}
	}
	
	public void setPort(String port) {
		this.port = port;
	}
	
	public String getPortFrom() {
		return portFrom;
	}
	
	public void setPortFrom(String portFrom) {
		this.portFrom = portFrom;
	}
	
	public String getPortTo() {
		return portTo;
	}
	
	public void setPortTo(String portTo) {
		this.portTo = portTo;
	}
}
