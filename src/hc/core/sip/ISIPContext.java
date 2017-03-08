package hc.core.sip;

import hc.core.AckBatchHCTimer;
import hc.core.HCConnection;
import hc.core.UDPPacketResender;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class ISIPContext {
	public AckBatchHCTimer ackbatchTimer;
	public UDPPacketResender resender;
	public abstract boolean buildUDPChannel(final HCConnection hcConnection);
	public abstract Object getDatagramPacket(Object dp);
	public abstract Object getDatagramPacketFromConnection(Object conn);
	public abstract byte[] getDatagramBytes(Object dp);
	public abstract void setDatagramLength(Object dp, int len);
	public abstract boolean tryRebuildUDPChannel(final HCConnection hcConnection);
	
	public boolean isOnRelay = false;

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
	public abstract void setSocket(Object connector, boolean isForSwap);
	
	public abstract Object getSocket();
	
	public abstract void closeSocket(Object connector) throws IOException;
	
	public abstract DataInputStream getInputStream(Object socket) throws IOException;
	
	public abstract DataOutputStream getOutputStream(Object socket) throws IOException;
	
	public abstract void setInputOutputStream(final DataInputStream dis, final DataOutputStream dos);
	
	protected boolean isClose = true;
	
	public final boolean isClose(){
		return isClose;
	}
	
	public abstract void closeDeploySocket(final HCConnection hcConnection);
	
//	public abstract Object getDeploySocket();
	
	/**
	 * 是发布Socket的唯一入口
	 * @param socket
	 */
	public void deploySocket(final HCConnection hcConnection, final Object socket) throws Exception{
		enterDeployStatus();
		
		final Object inputStream = (socket==null)?null:getInputStream(socket);
		hcConnection.setReceiveServerInputStream(inputStream, false, false);
		
		final Object outputStream = (socket==null)?null:getOutputStream(socket);
		hcConnection.setOutputStream(outputStream);
		
		setSocket(socket, false);
	}
	
	public final void enterDeployStatus() {
		deploySocketMS = System.currentTimeMillis();
	}
	
	/**
	 * 由于发布后导致其它旧连接的异常被动触发，从而将新连接被认为正常断开，从而触发其后逻辑，通过此条件阻断之。
	 * @return
	 */
	public final boolean isNearDeployTime(){
		if((System.currentTimeMillis() - deploySocketMS) < 1000){
			return true;
		}else{
			return false;
		}
	}
	
	public final void resetNearDeployTime(){
		deploySocketMS = 0;
	}
	
	private long deploySocketMS = 0;
	
}
