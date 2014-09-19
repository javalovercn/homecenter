package hc.server;

import hc.App;
import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.L;
import hc.core.Message;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.UDPPacketResender;
import hc.core.sip.ISIPContext;
import hc.core.sip.SIPManager;
import hc.core.util.CUtil;
import hc.core.util.LogManager;
import hc.server.ui.SingleMessageNotify;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;

public class J2SESIPContext extends ISIPContext {
	boolean isFailed = false;
	private InetAddress outputInetAddress;
	
	public J2SESIPContext() {
		resender = new UDPPacketResender(){
			protected final void resend(Object packet) {
				try{
					if(socket != null){
						socket.send((DatagramPacket)packet);
					}
				}catch (Exception e) {
//					e.printStackTrace();//UDP断线时，会输出很多的异常信息，故关闭
					SIPManager.notifyRelineon(false);
				}
			}
			
			public final Object getUDPSocket(){
				return socket;
			}
			
			DatagramSocket socket;
			
			public final void setUDPSocket(Object sender) {
				final DatagramSocket snapSocket = socket;
				socket = (DatagramSocket)sender;
				try{
					snapSocket.close();
				}catch (Exception e) {
					
				}
			}
	
			private int real_len_upd_data = MsgBuilder.UDP_MTU_DATA_MIN_SIZE;
			private InetAddress udpTargetAddr;
			private int udpTargetPort;
			
			public void setUDPTargetAddress(Object address, int port){
				udpTargetAddr = (InetAddress)address;
				udpTargetPort = port;
			}
			
			@Override
			public final void sendUDP(final byte ctrlTag, final byte subCtrlTag, final byte[] jcip_bs, int offset, final int j_len, final int mtuLen, final boolean isFlushNow) {
				int len = j_len;
				final int len_msg_data = (mtuLen==0?real_len_upd_data:mtuLen);
				final int split_num = len/ len_msg_data + ((len%len_msg_data > 0)?1:0);
				int groupID = 0;
				int SplitNo = 0;
				DatagramPacket p = null;
				try{
				do{
					final int realLen = (len>len_msg_data)?len_msg_data:len;
	
					p = (DatagramPacket)cacher.getFree();
					
					final byte[] bs = p.getData();
					
					bs[IDX_HEADER_1] = UDP_HEADER[0];
					bs[IDX_HEADER_2] = UDP_HEADER[1];
					
					//以下逻辑要与J2MEContext同步
	//				Message.setSendUUID(bs, selfUUID, selfUUID.length);
					Message.setMsgBody(bs, MsgBuilder.INDEX_UDP_MSG_DATA, jcip_bs, offset, realLen);
					//生成序列号
					groupID = Message.setSplitPara(bs, groupID, SplitNo++, split_num);
					
					bs[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;
					bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = subCtrlTag;
					
					p.setLength(realLen + MsgBuilder.INDEX_UDP_MSG_DATA);
	
					//属于分包型
					if(realLen == j_len){
						bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_NOT_SPLIT;
					}else{
						bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_SPLIT;
					}
	
					p.setAddress(udpTargetAddr);
					p.setPort(udpTargetPort);
					
	        		if(realLen == 0 || ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN){
	        			
	        		}else{
		        		CUtil.superXor(bs, MsgBuilder.INDEX_UDP_MSG_DATA, realLen, CUtil.OneTimeCertKey, true);
	        		}
	        		if(ctrlTag != MsgBuilder.E_TAG_ACK){
	        			resender.needAckAtSend(p, Message.getAndSetAutoMsgID(bs));
	        		}
	        		if(isFlushNow){
	        			socket.send(p);
	        		}
		    		if(ctrlTag == MsgBuilder.E_TAG_ACK){
	        			cacher.cycle(p);
	        		}
	        		
					len -= realLen;
					offset += realLen;
				}while(len > 0);
				}catch (IOException e) {
					cacher.cycle(p);
				}
				if(split_num > 50){
//					hc.core.L.V=hc.core.L.O?false:LogManager.log("Send out blob UDP data. num:" + split_num);
				}
			}
	    };
	}
	
	@Override
	public final byte[] getDatagramBytes(Object dp){
		return ((DatagramPacket)dp).getData();
	}

	@Override
	public final void setDatagramLength(Object dp, int len){
		((DatagramPacket)dp).setLength(len);
	}
	
	@Override
	public final Object getDatagramPacket(Object dp) {
		DatagramPacket p;
		if(dp == null){
			p = (DatagramPacket)getDatagramPacketFromConnection(null);
		}else{
			p = (DatagramPacket)dp;
		}
		return p;
	}
	
	public Object getDatagramPacketFromConnection(Object conn){
		return new DatagramPacket(new byte[MsgBuilder.UDP_BYTE_SIZE], MsgBuilder.UDP_BYTE_SIZE);
	}
	
	public boolean tryRebuildUDPChannel(){
		L.V = L.O ? false : LogManager.log("Server Side UDP rebuild NOT implement!");
		return false;
	}
	
	@Override
	public boolean buildUDPChannel() {
		InetSocketAddress isocket;
		final int targetPort = SIPManager.relayIpPort.udpPort;
		try {
			final InetAddress inetAddr = InetAddress.getByName(SIPManager.relayIpPort.ip);
			isocket = new InetSocketAddress(inetAddr, targetPort);
			DatagramSocket udpSocket;
			if(outputInetAddress != null){
				udpSocket = new DatagramSocket(new InetSocketAddress(outputInetAddress, 0));
			}else{
				udpSocket = new DatagramSocket();
			}
			udpSocket.connect(isocket);
			
//			L.V = L.O ? false : LogManager.log("buildUDPChannel : " + udpSocket.getLocalSocketAddress());
			
			resender.setUDPTargetAddress(inetAddr, targetPort);
			resender.setUDPSocket(udpSocket);
			
			ContextManager.getContextInstance().getUDPReceiveServer().setUdpServerSocket(udpSocket);
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}

	public Object buildSocket(final int localPort, final String targetServer, final int targetPort){
		try {
			return buildSocketInnal(localPort, targetServer, targetPort);
		} catch (Exception e) {
			if(isFailed == false){
				isFailed = true;
				
				SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_ERROR_CONNECTION, 
						("<img src='http://homecenter.mobi/images/bad_32.png' width='32' height='32'></img><STRONG>" + (String)ResourceUtil.get(1000) + "</STRONG>"
						+ "<BR><BR>" + new Date().toLocaleString())
						+ "<BR>TCP IP : " + HttpUtil.replaceIPWithHC(targetServer) + ", port : " + targetPort
						+ "<BR>Exception : " + e.getMessage()
						+ "<BR><BR>Please contact administrator!", (String)ResourceUtil.get(IContext.ERROR),
						60000 * 5, App.getSysIcon(App.SYS_ERROR_ICON));
				
				LogManager.errToLog("Fail build Socket to " + HttpUtil.replaceIPWithHC(targetServer) + ", port : " + targetPort + " from localPort: " + localPort);
				LogManager.errToLog("Fail build Socket Exception : " + e.getMessage());
			}
		}
		return null;
	}
	
	protected Object buildSocketInnal(final int localPort,
			final String targetServer, final int targetPort)
			throws Exception {
		Socket s = null;

		//优先采用PointToPoint
		outputInetAddress = HttpUtil.getServerInetAddress(true);
		if(outputInetAddress != null){
			s = newSocket(targetServer, targetPort, outputInetAddress, localPort);
		}
		if(s == null){
			try{
				s = new Socket(InetAddress.getByName(targetServer), targetPort);
			}catch (Throwable e) {
				s = null;
				//无法到达，可能选用了错误的
				//java.net.ConnectException: Connection refused: connect
			}
		}
		if(s == null){
			//遍历所有的可用NetworkInterface
			try {
				Enumeration nis = NetworkInterface.getNetworkInterfaces();
				while (nis.hasMoreElements()) {
					NetworkInterface ni = (NetworkInterface) nis.nextElement();
					outputInetAddress = HttpUtil.filerInetAddress(ni);
					if(outputInetAddress != null){
						s = newSocket(targetServer, targetPort, outputInetAddress, localPort);
						if(s != null){
							break;
						}
					}
				}
			} catch (Throwable f) {
			}
			if(s == null){
				LogManager.errToLog("No response from server or no networkInterface to " + HttpUtil.replaceIPWithHC(targetServer));
				throw new Exception();
			}
		}
		
		int tc = RootConfig.getInstance().getIntProperty(RootConfig.p_TrafficClass);
		if(tc != 0){
			s.setTrafficClass(tc);
		}

//			例如, 如果参数 connectionTime 为 2, 参数 latency 为 1, 而参数bandwidth 为 3, 就表示最高带宽最重要, 其次是最少连接时间, 最后是最小延迟.
		s.setPerformancePreferences(5, 2, 1);

		//KeepAlive_Tag
		s.setKeepAlive(false);
		s.setTcpNoDelay(true);
		s.setSoLinger(true, 5);
		final int ServerSndBF = RootConfig.getInstance().getIntProperty(RootConfig.p_ClientServerSendBufferSize);
		if(ServerSndBF != 0){
			s.setSendBufferSize(ServerSndBF);
			s.setReceiveBufferSize(1024 * 250);
		}
		
//			L.V = L.O ? false : LogManager.log("SndBuf:" + s.getSendBufferSize());
//			L.V = L.O ? false : LogManager.log("RcvBuf:" + s.getReceiveBufferSize());

		L.V = L.O ? false : LogManager.log("Succ build Socket to " + HttpUtil.replaceIPWithHC(targetServer) +", port:" 
				+ targetPort + " from local" + s.getLocalAddress() + ":" + s.getLocalPort());

		isFailed = false;
		
		return s;
	}

	private Socket newSocket(final String targetServer, final int targetPort,
			final InetAddress localAddress, final int localPort) {
		outputInetAddress = localAddress;
		try{
			return new Socket(InetAddress.getByName(targetServer), targetPort, localAddress, localPort);
		}catch (Throwable e) {
			//无法到达，可能选用了错误的
			//java.net.ConnectException: Connection refused: connect
		}
		outputInetAddress = null;
		return null;
	}

	public DataInputStream getInputStream(Object socket) throws IOException{
		return new DataInputStream(((Socket)socket).getInputStream());
	}
	
	public DataOutputStream getOutputStream(Object socket) throws IOException{
		return new DataOutputStream(((Socket)socket).getOutputStream());
	}
		
	public void closeSocket(Object socket) throws IOException{
		
		Socket socket2 = (Socket)socket;
		try{
			socket2.shutdownInput();
			socket2.shutdownOutput();
		}catch (Exception e) {
			
		}
		socket2.close();
	}
	
	public Object getSocket() {
		return socket;
	}
	
	private Socket socket;

	public String getSTUNServerAndPort() {
		return PropertiesManager.getValue(PropertiesManager.p_stunServerPorts);
	}

	public void saveStunServerAndPort(String serverAndPort) {
		PropertiesManager.setValue(PropertiesManager.p_stunServerPorts, serverAndPort);
		PropertiesManager.saveFile();
	}

	/**
	 * 成功建立UDPSocket时，返回true
	 * @param stunServer
	 * @param stunServerPort
	 * @return
	 * @throws Exception
	 */
//	public StunDesc stun(Object iaddress, String stunServer, int stunServerPort, int udpPort) {
//		DatagramSocket upnplisten = null, send = null;
//		byte[] bs = null;
//		bs = new byte[2000];
//
//		try{
//			upnplisten = new Socket(new InetSocketAddress((InetAddress)iaddress, udpPort));
//			upnplisten.setSoTimeout(10000);
//
//			DataNatReqConn nrn = new DataNatReqConn();
//			nrn.setBytes(bs);
//			nrn.setRemotePort(0);
//			
//			buildPacket(bs, nrn);
//			
//			upnplisten.getosend(dp);
//			L.V = L.O ? false : LogManager.log("Finding public ip/port");
//		
//			upnplisten.receive(dp);
//			StunDesc sd = buildDesc(bs);
//			
////			try {
////				upnplisten.setSoTimeout(0);
////			} catch (SocketException e) {
////				e.printStackTrace();
////			}
//			sd.setSocket(upnplisten);
//
//			return sd;
//		}catch (Exception e) {
//			e.printStackTrace();
//			
//			//超时关闭本连接
//			upnplisten.close();
//		}
//		return null;
//	}

//	public static void buildPacket(byte[] bs, DataNatReqConn nrn)
//			throws UnknownHostException {
//		
//		bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_ROOT;
//		bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = MsgBuilder.DATA_ROOT_UPNP_TEST;
//		
//		bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_NOT_SPLIT;
//		
////		bs[MsgBuilder.INDEX_PROTOCAL_TAG] = IConstant.DATA_PROTOCAL_HEAD_H;
////		bs[MsgBuilder.INDEX_PROTOCAL_TAG + 1] = IConstant.DATA_PROTOCAL_HEAD_C;
//		
////		Message.setSendUUID(bs, IConstant.uuidBS, IConstant.uuidBS.length);
//		
//	}

//	private StunDesc buildDesc(byte[] bs) {
//		DataNatReqConn drc = new DataNatReqConn();
//		drc.setBytes(bs);
//		
//		String ip = drc.getRemoteIP();
//		int port = drc.getRemotePort();
//		
//		StunDesc sd = new StunDesc() {
//			final DatagramPacket dp = buildPacket();
//			final int len = dp.getData().length;
//			
//			private DatagramPacket buildPacket() {
//				//一个不需要回应的包
//				byte[] buf = new byte[MsgBuilder.INDEX_CTRL_SUB_TAG];
////				buf[0] = IConstant.DATA_PROTOCAL_HEAD_H;
////				buf[1] = IConstant.DATA_PROTOCAL_HEAD_C;
//				buf[0] = MsgBuilder.E_TAG_ROOT;
//				buf[1] = MsgBuilder.DATA_ROOT_KEEP_ALIVE;
//				return new DatagramPacket(buf, buf.length);
//			}
			
//			@Override
//			public void keepalive() {
//				try {
//					InetAddress ia = InetAddress.getByName(RootConfig.getInstance().
//							getProperty(RootConfig.p_RootRelayServer));
//					
//					int port = Integer.parseInt(RootConfig.getInstance().getProperty(
//							RootConfig.p_RootRelayServerPort));
//					
//					dp.setAddress(ia);
//					dp.setPort(port);
//					
//					ContextManager.getSendServer().send(dp, len);
//					
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		};
//		
//		sd.setPublicIP(ip);
//		sd.setPublicPort(port);
//
//		//获得本机公网IP和Port
//		sd.setSymmetric();
//
//		return sd;
//	}

	public void closeDeploySocket(){
		//必须要置前
		KeepaliveManager.removeUPnPMapping();
		
		//网络环境发生变化，所以要重新
		KeepaliveManager.relayServerLocalPort = 0;
		
		KeepaliveManager.homeWirelessIpPort.reset();
		KeepaliveManager.relayServerUPnPPort = 0;
		SIPManager.relayIpPort.reset();
		
		isClose = true;		
		Socket snapSocket = socket;
		
		deploySocket(null, null, null);
		
		if(KeepaliveManager.nioRelay != null){
			KeepaliveManager.nioRelay.close();
		}
		
		try{
			snapSocket.close();
			snapSocket = null;
		}catch (Exception e) {
			
		}
	}

	public void setSocket(Object connector) {
		final Socket snapSocket = socket;
		if(connector != null){
			isClose = false;
		}
		this.socket = (Socket)(connector);
		try{
			snapSocket.close();
		}catch (Exception e) {
			
		}
	}

//	public Object getDeploySocket() {
//		return socket;
//	}

}