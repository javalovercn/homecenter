package hc.server;

import hc.core.ContextManager;
import hc.core.HCConnection;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.sip.IPAndPort;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.util.HttpUtil;
import hc.util.ResourceUtil;
import hc.util.TokenManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Vector;


public class KeepaliveManager {
	public final int KEEPALIVE_MS = Integer.parseInt(RootConfig.getInstance().
			getProperty(RootConfig.p_KeepAliveMS));
	
	public final HCTimer connBuilderWatcher = buildKeepAliveWatcher();
	
	/**
	 * 每小时刷新alive变量到Root服务器
	 */
	public final HCTimer aliveToRootRefresher;
	
	private final HCTimer buildKeepAliveWatcher(){
		return new HCTimer("ConnBuilderWatcher", 15000, false){
			final int doubleKeepTime = KEEPALIVE_MS + 5000;
			@Override
			public void doBiz(){
				if(UserThreadResourceUtil.isInServing(coreSS.context)){
					setEnable(false);
				}else if((System.currentTimeMillis() - hcConnection.startTime) > doubleKeepTime){
					LogManager.err("Time over for building connection");
					RootServerConnector.notifyLineOffType(coreSS, RootServerConnector.LOFF_OverTimeConn_STR);
					coreSS.notifyLineOff(true, false);
					setEnable(false);
				}
			}
			@Override
			public void setEnable(final boolean enable){
				hcConnection.startTime = System.currentTimeMillis();
				
//					if(enable == false){
//						LogManager.log("-----------ConnBuilderWatcher DIS able----------");
//					}else{
//						LogManager.log("-----------ConnBuilderWatcher EN able----------");
//					}
				super.setEnable(enable);
			}
		};
	}
	
	public final void resetSendData(){
		hcConnection.isSendLive = false;
	}
	
	public final J2SESession coreSS;
	private final HCConnection hcConnection;
	
	public KeepaliveManager(final J2SESession coreSS){
		this.coreSS = coreSS;
		this.hcConnection = coreSS.getHCConnection();
		aliveToRootRefresher = ResourceUtil.buildAliveRefresher(coreSS, false);
	}
	
	public final void release(){
		if(L.isInWorkshop){
			LogManager.log("remove all KepaliveManager HCTimer.");
		}
		
		HCTimer.remove(connBuilderWatcher);
		HCTimer.remove(keepalive);
	}

	public final boolean buildRelay() {
			//完全Relay
			Vector relays = (Vector)RootServerConnector.getNewRelayServers(IConstant.getUUID(), TokenManager.getToken());
			if(relays == null || relays.size() == 0){
				LogManager.errToLog("No HomeCenter root relay server.");
				RootServerConnector.notifyLineOffType(null, RootServerConnector.LOFF_NO_ROOT_RELAY_Err_STR);
				return false;
			}
			
			final int size = relays.size();
			
			//循环替补端口
	//		int backPort = (Integer)ContextManager.getContextInstance().doExtBiz(
	//				IContext.BIZ_GET_BACK_PORT, null);
	
			//测速，仅出现多个中继服务器时
			if(size > 1){
				final int timeOut = 5000;

				final TCPTestThread[] tts = new TCPTestThread[size];
				final Vector speedSortedRelays = new Vector();
				for (int i = 0; i < size; i++) {
					final String[] ipAndPorts = (String[])relays.elementAt(i);
					
					final TCPTestThread tt = new TCPTestThread(speedSortedRelays, ipAndPorts, timeOut);
					tt.start();
					tts[i] = tt;
				}
				long now = System.currentTimeMillis();
				final long afterSleep = now + timeOut;
				try{
					while(afterSleep > now){
						Thread.sleep(30);
						now += 30;
						
						final int echoSize = speedSortedRelays.size();
						if(echoSize == size || echoSize >= 2){
							break;
						}
					}
				}catch (final Exception e) {
				}
				
				if(speedSortedRelays.size() != 0){
	//				LogManager.log("use hige speed for connection!");
					relays = speedSortedRelays;
				}
			}
			
			for (int i = 0; i < relays.size(); i++) {//size()有可能正在增加中
				coreSS.setOnRelay(true);
	
				final String[] ipAndPorts = (String[])relays.elementAt(i);
				final String ip = ipAndPorts[0];
				final String port = ipAndPorts[1];
	
				//原为backPort
				LogManager.log("try connect relay server " 
						+ HttpUtil.replaceIPWithHC(ip) + ", port:" + port);
				
				IPAndPort ipport = new IPAndPort(ip, Integer.parseInt(port));
				
				ipport = SIPManager.proccReg(hcConnection, ipport, MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_FIRST,
						SIPManager.REG_WAITING_MS, (byte[])coreSS.context.doExtBiz(IContext.BIZ_GET_TOKEN, null));
				if(ipport != null){
					hcConnection.relayIpPort = ipport;
					return true;
				}
			}
			coreSS.setOnRelay(false);
			return false;
		}

	public final void sendAlive(final int mode) {
		try{
//			LogManager.log("Send line watcher package");
			if(mode == ContextManager.STATUS_SERVER_SELF){
				//保持UDP不断，不需要回应的，由于内网连接时，本逻辑是disable状态。
				if(hcConnection.isBuildedUPDChannel && hcConnection.isDoneUDPChannelCheck){
//								if(isSendUDPCheckAlive == false){//本行代码，仅供模拟手机端UDP断线效果
					hcConnection.udpSender.
						sendUDP(MsgBuilder.E_TAG_ONLY_SUB_TAG_MSG, MsgBuilder.DATA_SUB_TAG_MSG_UDP_CHECK_ALIVE,
								zeroUDPBS, 0, 0, 0, false);
//								isSendUDPCheckAlive = true;
//							}
				}
				//保持TCP不断，同时需要回应，并检测联线状态
				coreSS.context.sendWithoutLockForKeepAliveOnly(null, MsgBuilder.E_TAG_ROOT, MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_SERVERING);
//				L.V = L.WShop ? false : LogManager.log("[keepalive] try DATA_ROOT_LINE_WATCHER_ON_SERVERING");
			}else{
				coreSS.context.sendWithoutLockForKeepAliveOnly(null, MsgBuilder.E_TAG_ROOT, MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_RELAY);
//				L.V = L.WShop ? false : LogManager.log("[keepalive] try DATA_ROOT_LINE_WATCHER_ON_RELAY");
			}
		}catch (final Exception e) {
			coreSS.notifyLineOff(false, false);
		}
		hcConnection.sendLineMS = System.currentTimeMillis();
		hcConnection.isSendLive = true;
	}

	private final byte[] zeroUDPBS = new byte[0];

	public final HCTimer keepalive = new HCTimer("KeepAlive", KEEPALIVE_MS, false, true, ThreadPriorityManager.KEEP_ALIVE_PRIORITY){
    	private final int ErrorNeedNatDelay = 30 * 1000;//比如连接Socket出错，而非Http。两分钟
    	private final int lineWatcherMS = RootConfig.getInstance().getIntProperty(RootConfig.p_enableLineWatcher);//60 * 1000 * 5;
    	
    	private final boolean isOnRelay(){
			if(SIPManager.isOnRelay(hcConnection) && hcConnection.sipContext.isClose() == false){
				LogManager.log("Use exist socket");
				return true;
			}
			
			return buildRelay();
    	}
		
		@Override
		public final void doBiz() {
			final int mode = coreSS.context.cmStatus;
	    	if(mode == ContextManager.STATUS_EXIT){
	    		return;
	    	}else if(mode == ContextManager.STATUS_NEED_NAT){
	    		hcConnection.isSendLive = false;
	    		
				final boolean isConn = isOnRelay();
				L.V = L.WShop ? false : LogManager.log("keepalive onRelay : " + isConn + ", at coreSS : " + coreSS.hashCode());
				
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
					SIPManager.startConnectionInit(coreSS, hcConnection);
				}
			}else if(mode == ContextManager.STATUS_LINEOFF){
//				LogManager.log("Line Off Status");
//				hcConnection.isSendLive = false;
//				coreSS.context.setStatus(ContextManager.STATUS_NEED_NAT);
			}else{
				//lineWatcherMS == 0表示关闭
				if(lineWatcherMS > 0){
					//setIntervalMS(lineWatcherMS);
					if(hcConnection.isSendLive){
						//检查上次发送包是否正常收到，必须要加=，不能仅>，因为在单机极速环境下，出现相同情形
						if(Math.abs(coreSS.context.rootTagListener.getServerReceiveMS() - hcConnection.sendLineMS) < lineWatcherMS){
							//通
//							LogManager.log("Received last line watcher package");
						}else{
							//不通
							LogManager.log("remote lineoff detected by keepalive");
							coreSS.notifyLineOff(false, false);
							return;
						}
					}
					sendAlive(mode);
				}
			}
	    	
		}
		@Override
		public void setEnable(final boolean enable){
			if(enable == false){
				L.V = L.WShop ? false : LogManager.log("Disable keepalive");
			}else{
				L.V = L.WShop ? false : LogManager.log("Enable keepalive");
			}
			super.setEnable(enable);
		}
	};
}

class TCPTestThread extends Thread{
	Vector sorted;
	String[] tcpInfo;
	final int timeOut;
	
	public TCPTestThread(final Vector sorted, final String[] tcpInfo, final int timeOut) {
		this.sorted = sorted;
		this.tcpInfo = tcpInfo;
		this.timeOut = timeOut;
	}
	
	@Override
	public void run(){
		final String tcpTestIP = tcpInfo[0];
		final String tcpTestPort = tcpInfo[1];
		
		Socket socket = null;
		try{
			final InetAddress inetAddr = InetAddress.getByName(tcpTestIP);
			
			socket = new Socket();//inetAddr, Integer.parseInt(tcpTestPort)
			socket.connect(new InetSocketAddress(inetAddr, Integer.parseInt(tcpTestPort)), timeOut); 
	        final OutputStream out=socket.getOutputStream(); 
	        
			final byte[] zeroLenbs = new byte[MsgBuilder.MIN_LEN_MSG];
			
			zeroLenbs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_ROOT;
			zeroLenbs[MsgBuilder.INDEX_CTRL_SUB_TAG] = MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_RELAY;
		    
			out.write(zeroLenbs, 0, MsgBuilder.MIN_LEN_MSG);
			out.flush();
			
			final InputStream in = socket.getInputStream();  
			final int bytesRcvd=in.read(zeroLenbs, 0, zeroLenbs.length);
			if(bytesRcvd == zeroLenbs.length){
				synchronized (sorted) {
					LogManager.log("success receive TCP test speed echo from IP : " + tcpTestIP + ", port : " + tcpTestPort);
					sorted.add(tcpInfo);
				}
			}
		}catch (final Throwable e) {
		}finally{
			try{
				socket.close();
			}catch (final Throwable e) {
			}
		}
	}
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
						LogManager.log("Success test udp speed");
						sorted.add(updInfo);
					}
					udpSocket.close();
				}
			}
		}catch (final Exception e) {
		}
	}
}