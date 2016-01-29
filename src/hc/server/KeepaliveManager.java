package hc.server;

import hc.core.ClientInitor;
import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.data.DataNatReqConn;
import hc.core.sip.IPAndPort;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.server.nio.NIOServer;
import hc.server.relay.RelayActionRead;
import hc.util.ConnectionManager;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.TokenManager;
import hc.util.UPnPUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class KeepaliveManager {
	public static final int KEEPALIVE_MS = Integer.parseInt(RootConfig.getInstance().
			getProperty(RootConfig.p_KeepAliveMS));

	private static boolean usingUPnP = false;
	public static void removeUPnPMapping(){
		if(usingUPnP){
			UPnPUtil.removeUPnPMapping(homeWirelessIpPort.port);
			UPnPUtil.removeUPnPMapping(relayServerUPnPPort);
		}
	}
    
	public static final HCTimer ConnBuilderWatcher = buildKeepAliveWatcher();
	
	private static HCTimer buildKeepAliveWatcher(){
		if(IConstant.serverSide){
			return new HCTimer("ConnBuilderWatcher", 15000, false){
				final int doubleKeepTime = KEEPALIVE_MS + 5000;
				@Override
				public void doBiz(){
					if(ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF){
						setEnable(false);
					}else if((System.currentTimeMillis() - startTime) > doubleKeepTime){
						LogManager.err("Time over for building connection");
						RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_OverTimeConn_STR);
						SIPManager.notifyRelineon(true);
						setEnable(false);
					}
				}
				long startTime ;
				@Override
				public void setEnable(final boolean enable){
					startTime = System.currentTimeMillis();
					
//					if(enable == false){
//						L.V = L.O ? false : LogManager.log("-----------ConnBuilderWatcher DIS able----------");
//					}else{
//						L.V = L.O ? false : LogManager.log("-----------ConnBuilderWatcher EN able----------");
//					}
					super.setEnable(enable);
				}
			};
		}
		return null;
	}
	public static IPAndPort homeWirelessIpPort = new IPAndPort();
    public static int relayServerLocalPort, relayServerUPnPPort;
    //各型IP必须初始化为空串，以提供上传时不出现问题
    public static String relayServerUPnPIP = "";
    public static String relayUPnPToken;
    
    protected static NIOServer nioRelay;
    protected static DirectServer dServer;
    
    public final static NIOServer getNIORelay(){
    	CCoreUtil.checkAccess();
    	
    	return nioRelay;
    }
    
    public final static DirectServer getDirectServer(){
    	CCoreUtil.checkAccess();
    	
    	return dServer;
    }
    
    public static String getStunIP(final String remoteIP, final int remotePort){
    	byte[] bs = null;
    	Socket socket = null;
    	try{
    		socket = (Socket)SIPManager.getSIPContext().buildSocket(0, remoteIP, remotePort);
			if(socket == null){
				return "";
			}
			bs = new byte[MsgBuilder.UDP_BYTE_SIZE];//DatagramPacketCacher.getInstance().getFree();
			
			final DataNatReqConn nrn = new DataNatReqConn();
			nrn.setBytes(bs);
			
			nrn.setRemotePort(0);
			
			bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_ROOT;
			bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = MsgBuilder.DATA_ROOT_UPNP_TEST;		
//			bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_NOT_SPLIT;
		
			final OutputStream os = socket.getOutputStream();
			final int len = MsgBuilder.INDEX_MSG_DATA + nrn.getLength();
			
			SIPManager.send(os, bs, 0, len);
			
			socket.setSoTimeout(6000);
			
			final InputStream is = socket.getInputStream();
			is.read(bs);
			
			final String natIP = nrn.getRemoteIP();
			
			socket.close();
			
			return natIP;
    	}catch (final Exception e) {
    		if(L.isInWorkshop){
    			e.printStackTrace();
    		}
		}
    	return "";
    }
    
    public static boolean checkDirectPublic(final InetAddress iaddress, String ip, final boolean useUPnP){    	
    	String[] ups = null;
    	ServerSocket upnplisten = null;
    	try{
			
    		upnplisten = new ServerSocket(0);
		    int testDPPort = upnplisten.getLocalPort();
		    		
			upnplisten.setSoTimeout(5000);
	
			if(useUPnP){
				ups = UPnPUtil.startUPnP(iaddress, testDPPort, 
					100, "checkDP");
				
				testDPPort = Integer.parseInt(ups[1]);
				ip = ups[0];
			}
	
			final byte[] bs = new byte[MsgBuilder.UDP_BYTE_SIZE];//DatagramPacketCacher.getInstance().getFree();
			if(pingRoot(ip, testDPPort, bs) == false){
				return false;
			}

			final Socket publicIn = upnplisten.accept();
			L.V = L.O ? false : LogManager.log("Finding public (UPnP) ip/port");
			
			publicIn.close();
			
			return true;
		}catch (final Exception e) {
		}finally{
			if(ups != null){
				UPnPUtil.removeUPnPMapping(Integer.parseInt(ups[1]));
			}
			if(upnplisten != null){
				try {
					upnplisten.close();
				} catch (final IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		return false;
    }

	private static boolean pingRoot(final String echoIP, final int udpPort, final byte[] bs) {
		final DataNatReqConn nrn = new DataNatReqConn();
		nrn.setBytes(bs);
		nrn.setRemotePort(udpPort);
		nrn.setRemoteIP(echoIP);
		
		bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_ROOT;
		bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = MsgBuilder.DATA_ROOT_UPNP_TEST;
//		bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_NOT_SPLIT;

		final Socket socket = (Socket)SIPManager.getSIPContext().buildSocket(0, 
				RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServer), 
				Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServerPort)));
		if(socket == null){
			//增加null判断，以减少大量后续的错误
			return false;
		}
		
		OutputStream os = null;
		try{
			os = socket.getOutputStream();
			
			SIPManager.send(os, bs, 0, nrn.getLength() + MsgBuilder.INDEX_MSG_DATA);
			
			os.close();
			socket.close();
			
			return true;
		}catch (final Exception e) {
			if(L.isInWorkshop){
				e.printStackTrace();
			}
			//因为已经混淆，所以此处无必要。
			//e.printStackTrace();
			return false;
		}
	}
    public static String publicShowIP = "";
    
    private static boolean reconnect(){
		if(SIPManager.isOnRelay() && SIPManager.getSIPContext().isClose() == false){
			L.V = L.O ? false : LogManager.log("Use exist socket");
			return true;
		}
		
		//removeUPnPMapping方法内使用变量，所以hcgd，
		//使用情形：有可能网络中断后，又正常。
		UPnPUtil.hcgd = null;
		
		final String rootIP = RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServer);
		final int rootPort = Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServerPort));

//		暂停stun IP
//		L.V = L.O ? false : LogManager.log("try connect homecenter.mobi [" + rootIP + ":" + rootPort + "] for stun IP");
//		publicShowIP = getStunIP(rootIP, rootPort);
		
    	//    	SIPManager.resetAllConnection();
		String networkInterfacename = PropertiesManager.getValue(PropertiesManager.p_selectedNetwork, HttpUtil.AUTO_DETECT_NETWORK);
		InetAddress ia = null;
		if(networkInterfacename.equals(HttpUtil.AUTO_DETECT_NETWORK)){
		}else{
			ia = HttpUtil.getInetAddressByDeviceName(networkInterfacename);
			if(ia == null){
				L.V = L.O ? false : LogManager.log("fail build direct server");
				L.V = L.O ? false : LogManager.log("  on network interface [" + networkInterfacename + "]");
				networkInterfacename = HttpUtil.AUTO_DETECT_NETWORK;
			}
		}
		if(ia == null){
			ia = HttpUtil.getLocal();
		}
    	if(PropertiesManager.isTrue(PropertiesManager.p_ForceRelay) 
    			|| RootConfig.getInstance().isTrue(RootConfig.p_forceRelay)){
    		if( ! PropertiesManager.isTrue(PropertiesManager.p_DisableHomeWireless)){
    			buildNIOs(ia, networkInterfacename, false);
    		}else{
    			L.V = L.O ? false : LogManager.log("Disable home direct server.");
    		}
    	}else{
	    	if(( ! PropertiesManager.isTrue(PropertiesManager.p_DisableDirect)) 
	    			&& checkDirectPublic(ia, publicShowIP, false)){
	    		//localUPnPIP = publicShowIP; homeWirelessIpPort.ip = publicShowIP
	    		relayServerUPnPIP = publicShowIP;
	    		
	    		final boolean b = buildNIOs(ia, networkInterfacename, true);
	    		
	    		relayServerUPnPPort = relayServerLocalPort;
	    		
	    		return b;
	    	}
	    	if(UPnPUtil.isSupportUPnP(ia)){
	    		//检查是否直接Public	
		    		
				if(checkDirectPublic(ia, publicShowIP, true)){
	    			usingUPnP = true;
	    			
	    			final boolean b = buildNIOs(ia, networkInterfacename, true);
	    			
	    			//startUPnP(ia, 0 0原为localPort成员变量
	        		final String[] ups = UPnPUtil.startUPnP(ia, 0, 
	        				getUPnPPortFromP(PropertiesManager.p_DirectUPnPExtPort), TokenManager.getToken());
	        		homeWirelessIpPort.port = Integer.parseInt(ups[1]);
	        		homeWirelessIpPort.ip = ups[0];

	        		final String[] relayUPnP = UPnPUtil.startUPnP(ia, relayServerLocalPort, 
	        				getUPnPPortFromP(PropertiesManager.p_RelayServerUPnPExtPort), TokenManager.getRelayToken());
	        		relayServerUPnPPort = Integer.parseInt(relayUPnP[1]);
	        		relayServerUPnPIP = relayUPnP[0];
	        		
	        		PropertiesManager.setValue(PropertiesManager.p_DirectUPnPExtPort, ups[1]);
	        		PropertiesManager.setValue(PropertiesManager.p_RelayServerUPnPExtPort, relayUPnP[1]);
	        		PropertiesManager.saveFile();
	        		
	        		return b;
	    		}else{
	    			usingUPnP = false;
	    		}
	    	}
    	}
    	
		return buildRelay();
    }

	private static int getUPnPPortFromP(final String extPort) {
		final String directUPnPExtPort = PropertiesManager.getValue(extPort);
		if(directUPnPExtPort == null){
			return 0;
		}else{
			return Integer.parseInt(directUPnPExtPort);
		}
	}
	
	private static boolean buildRelay() {
		//完全Relay
		Vector relays = (Vector)RootServerConnector.getNewRelayServers(IConstant.getUUID(), TokenManager.getToken());
		if(relays == null || relays.size() == 0){
			LogManager.errToLog("No HomeCenter root relay server.");
			RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_NO_ROOT_RELAY_Err_STR);
			return false;
		}
		
		int size = relays.size();
		
		//循环替补端口
//		int backPort = (Integer)ContextManager.getContextInstance().doExtBiz(
//				IContext.BIZ_GET_BACK_PORT, null);

		//测速，仅出现多个中继服务器时
		if(size > 1){
			final UDPTestThread[] tts = new UDPTestThread[size];
			final Vector speedSortedRelays = new Vector();
			for (int i = 0; i < size; i++) {
				final String[] ipAndPorts = (String[])relays.elementAt(i);
				
				final UDPTestThread tt = new UDPTestThread(speedSortedRelays, ipAndPorts);
				tt.start();
				tts[i] = tt;
			}
			long now = System.currentTimeMillis();
			final long afterSleep = now + 2000;
			try{
				while(afterSleep > now){
					Thread.sleep(30);
					now += 30;
					
					if(speedSortedRelays.size() == size){
						break;
					}
				}
			}catch (final Exception e) {
			}
			for (int i = 0; i < tts.length; i++) {
				try{
					tts[i].interrupt();
				}catch (final Throwable e) {
				}
			}
			
			if(speedSortedRelays.size() != 0){
//				L.V = L.O ? false : LogManager.log("use hige speed for connection!");
				relays = speedSortedRelays;
				size = relays.size();
			}
		}
		
		for (int i = 0; i < size; i++) {
			SIPManager.setOnRelay(true);

			final String[] ipAndPorts = (String[])relays.elementAt(i);
			final String ip = ipAndPorts[0];
			final String port = ipAndPorts[1];

			//原为backPort
			L.V = L.O ? false : LogManager.log("try connect relay server " 
					+ HttpUtil.replaceIPWithHC(ip) + ", port:" + port);
			
			IPAndPort ipport = new IPAndPort(ip, Integer.parseInt(port));
			
			ipport = SIPManager.proccReg(ipport, MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_FIRST, SIPManager.REG_WAITING_MS);
			if(ipport != null){
				SIPManager.relayIpPort = ipport;
				return true;
			}
		}
		SIPManager.setOnRelay(false);
		return false;
	}

	private static boolean buildNIOs(final InetAddress ia, final String networkName, final boolean buildNIO) {
//		try {
//			ia = InetAddress.getByName("0.0.0.0");
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}
    	if(nioRelay != null && nioRelay.isOpen()){
    		return true;
    	}

    	if(buildNIO){
    		nioRelay = new NIOServer(ia.getHostAddress(), 0, 0, new RelayActionRead());
    	}
    	
		if(dServer != null){
			dServer.shutdown();
			dServer = null;
			try{
				Thread.sleep(200);
			}catch (final Exception e) {
			}
    	}
		dServer = new DirectServer(ia, networkName);
		dServer.buildServer();
		dServer.start();
		return true;
	}
    
    public static final HCTimer keepalive = new HCTimer("KeepAlive", KEEPALIVE_MS, false, true, ThreadPriorityManager.KEEP_ALIVE_PRIORITY){
    	private static final int ErrorNeedNatDelay = 30 * 1000;//比如连接Socket出错，而非Http。两分钟
    	private final int lineWatcherMS = RootConfig.getInstance().getIntProperty(RootConfig.p_enableLineWatcher);//60 * 1000 * 5;
    	private long sendLineMS;
    	private boolean isSendLive = false;
    	private final byte[] zeroUDPBS = new byte[0];
		
		@Override
		public final void doBiz() {
			final int mode = ContextManager.cmStatus;
	    	if(mode == ContextManager.STATUS_EXIT){
	    		return;
	    	}else if(mode == ContextManager.STATUS_NEED_NAT){
	    		isSendLive = false;
	    		
	    		//执行重连前置逻辑
	    		ConnectionManager.startBeforeConnectBiz();
	    		
				final boolean isConn = reconnect();
				if(isConn == false){
//					if(getIntervalMS() == KEEPALIVE_MS){
						//改为长时间，扫描网络状态
						setIntervalMS(ErrorNeedNatDelay);
//					}
				}else{
//					if(getIntervalMS() == ErrorNeedNatDelay){
						//改为短时间
						setIntervalMS(KEEPALIVE_MS);
//					}
				}
//				setEnable(!isConn);
				if(isConn){
					
					//上传联络信息
					SIPManager.startConnectionInit();
				}
			}else if(mode == ContextManager.STATUS_LINEOFF){
				L.V = L.O ? false : LogManager.log("Line Off Status");
				isSendLive = false;
				ContextManager.setStatus(ContextManager.STATUS_NEED_NAT);
			}else{
				//lineWatcherMS == 0表示关闭
				if(lineWatcherMS > 0){
					//setIntervalMS(lineWatcherMS);
					if(isSendLive){
						//检查上次发送包是否正常收到，必须要加=，不能仅>，因为在单机极速环境下，出现相同情形
						if(Math.abs(ClientInitor.rootTagListener.getServerReceiveMS() - sendLineMS) < lineWatcherMS){
							//通
							//L.V = L.O ? false : LogManager.log("Received last line watcher package");
						}else{
							//不通
							L.V = L.O ? false : LogManager.log("remote lineoff detected by keepalive");
							SIPManager.notifyRelineon(false);
							return;
						}
					}
					try{
	//					L.V = L.O ? false : LogManager.log("Send line watcher package");
						final IContext contextInstance = ContextManager.getContextInstance();
						if(mode == ContextManager.STATUS_SERVER_SELF){
							//保持UDP不断，不需要回应的，由于内网连接时，本逻辑是disable状态。
							if(contextInstance.isBuildedUPDChannel && contextInstance.isDoneUDPChannelCheck){
//								if(isSendUDPCheckAlive == false){//本行代码，仅供模拟手机端UDP断线效果
								contextInstance.udpSender.
									sendUDP(MsgBuilder.E_TAG_ONLY_SUB_TAG_MSG, MsgBuilder.DATA_SUB_TAG_MSG_UDP_CHECK_ALIVE,
											zeroUDPBS, 0, 0, 0, false);
	//								isSendUDPCheckAlive = true;
	//							}
							}
							//保持TCP不断，同时需要回应，并检测联线状态
							contextInstance.send(null, MsgBuilder.E_TAG_ROOT, MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_SERVERING);
						}else{
							contextInstance.send(null, MsgBuilder.E_TAG_ROOT, MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_RELAY);
						}
					}catch (final Exception e) {
						SIPManager.notifyRelineon(false);
					}
					sendLineMS = System.currentTimeMillis();
					isSendLive = true;
				}
			}
	    	
		}
		@Override
		public void setEnable(final boolean enable){
			if(enable == false){
				L.V = L.O ? false : LogManager.log("Disable keepalive");
			}else{
				L.V = L.O ? false : LogManager.log("Enable keepalive");
			}
			super.setEnable(enable);
		}
	};
}

class UDPTestThread extends Thread{
	Vector sorted;
	String[] updInfo;
	
	public UDPTestThread(final Vector sorted, final String[] updInfo) {
		this.sorted = sorted;
		this.updInfo = updInfo;
	}
	
	@Override
	public void run(){
		final String udpTestIP = updInfo[3];
		final String udpTestPort = updInfo[4];
		
		try{
			final InetAddress inetAddr = InetAddress.getByName(udpTestIP);
			final InetSocketAddress isocket = new InetSocketAddress(inetAddr, Integer.parseInt(udpTestPort));
			final DatagramSocket udpSocket = new DatagramSocket();
			udpSocket.connect(isocket);
			
			final byte[] helloServer = {'h', 'e', 'l', 'l', 'o', ',', 's','e','r', 'v', 'e', 'r'};
			final int size = helloServer.length + MsgBuilder.LEN_UDP_CONTROLLER_HEAD;
			byte[] bf = new byte[size];
			ByteUtil.integerToTwoBytes(MsgBuilder.E_UDP_CONTROLLER_TEST_SPEED, bf, MsgBuilder.LEN_UDP_CONTROLLER_HEAD);
			for (int i = MsgBuilder.LEN_UDP_CONTROLLER_HEAD; i < bf.length; i++) {
				bf[i] = helloServer[i - MsgBuilder.LEN_UDP_CONTROLLER_HEAD];
			}
			final DatagramPacket dp = new DatagramPacket(bf, size);
			udpSocket.send(dp);
			
			//receive和send不能在同一个线程中，
			udpSocket.receive(dp);
			final int receiveLen = dp.getLength();
			if(receiveLen == size){
				bf = dp.getData();
				boolean isRight = true;
				for (int i = 0; i < receiveLen; i++) {
					if(bf[i] != helloServer[i]){
						isRight = false;
						break;
					}
				}
				if(isRight){
					synchronized (sorted) {
						L.V = L.O ? false : LogManager.log("Success test udp speed");
						sorted.add(updInfo);
					}
					udpSocket.close();
				}
			}
		}catch (final Exception e) {
		}
	}
}