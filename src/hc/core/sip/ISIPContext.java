package hc.core.sip;

import hc.core.ContextManager;
import hc.core.ReceiveServer;
import hc.core.UDPPacketResender;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class ISIPContext {
	public UDPPacketResender resender;
	public abstract boolean buildUDPChannel();
	public abstract Object getDatagramPacket(Object dp);
	public abstract Object getDatagramPacketFromConnection(Object conn);
	public abstract byte[] getDatagramBytes(Object dp);
	public abstract void setDatagramLength(Object dp, int len);
	public abstract boolean tryRebuildUDPChannel();
	
	/**
	 * 获得上次成功Stun的服务器
	 * @return
	 */
	public abstract String getSTUNServerAndPort();
	
	/**
	 * 将本次的成功Stun记入存储中，以备下次Stun之用
	 * @param serverAndPort
	 */
	public abstract void saveStunServerAndPort(String serverAndPort);
	
//	public abstract StunDesc stun(Object iaddress, String stunServer, int stunServerPort, int udpPort);
	
	public abstract Object buildSocket(int localPort, String targetServer, int targetPort);
	
	/**
	 * 如果被覆盖的旧Socket存在，请close旧连接
	 * @param connector
	 */
	public abstract void setSocket(Object connector);
	
	public abstract Object getSocket();
	
	public abstract void closeSocket(Object connector) throws IOException;
	
	public abstract DataInputStream getInputStream(Object socket) throws IOException;
	
	public abstract DataOutputStream getOutputStream(Object socket) throws IOException;
	
	protected boolean isClose = true;
	
	public boolean isClose(){
		return isClose;
	}
	
	public abstract void closeDeploySocket();
	
//	public abstract Object getDeploySocket();
	
	/**
	 * 是发布Socket的唯一入口
	 * @param socket
	 */
	public void deploySocket(final Object socket) throws Exception{
		enterDeployStatus();
		
		final ReceiveServer receiveServer = ContextManager.getReceiveServer();
		receiveServer.setUdpServerSocket(socket);
		ContextManager.getContextInstance().setOutputStream(socket);
		setSocket(socket);
	}
	public void enterDeployStatus() {
		deploySocketMS = System.currentTimeMillis();
	}
	
	/**
	 * 由于发布后导致其它旧连接的异常被动触发，从而将新连接被认为正常断开，从而触发其后逻辑，通过此条件阻断之。
	 * @return
	 */
	public boolean isNearDeployTime(){
		if((System.currentTimeMillis() - deploySocketMS) < 1000){
			return true;
		}else{
			return false;
		}
	}
	
	public void resetNearDeployTime(){
		deploySocketMS = 0;
	}
	
	private long deploySocketMS = 0;
	
}
