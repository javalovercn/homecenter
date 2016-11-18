package hc.server;

import hc.core.ContextManager;
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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
				}else if((System.currentTimeMillis() - startTime) > doubleKeepTime){
					LogManager.err("Time over for building connection");
					RootServerConnector.notifyLineOffType(coreSS, RootServerConnector.LOFF_OverTimeConn_STR);
					SIPManager.notifyLineOff(coreSS, true, false);
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
	
	private boolean isSendLive = false;
	
	public final void resetSendData(){
		isSendLive = false;
	}
	
	public final J2SESession coreSS;

	public KeepaliveManager(final J2SESession coreSS){
		this.coreSS = coreSS;
		aliveToRootRefresher = ResourceUtil.buildAliveRefresher(coreSS, false);
	}
	
	public final void release(){
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("remove all KepaliveManager HCTimer.");
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
				SIPManager.setOnRelay(coreSS, true);
	
				final String[] ipAndPorts = (String[])relays.elementAt(i);
				final String ip = ipAndPorts[0];
				final String port = ipAndPorts[1];
	
				//原为backPort
				L.V = L.O ? false : LogManager.log("try connect relay server " 
						+ HttpUtil.replaceIPWithHC(ip) + ", port:" + port);
				
				IPAndPort ipport = new IPAndPort(ip, Integer.parseInt(port));
				
				ipport = SIPManager.proccReg(coreSS, ipport, MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_FIRST, SIPManager.REG_WAITING_MS);
				if(ipport != null){
					coreSS.relayIpPort = ipport;
					return true;
				}
			}
			SIPManager.setOnRelay(coreSS, false);
			return false;
		}

	public final HCTimer keepalive = new HCTimer("KeepAlive", KEEPALIVE_MS, false, true, ThreadPriorityManager.KEEP_ALIVE_PRIORITY){
    	private final int ErrorNeedNatDelay = 30 * 1000;//比如连接Socket出错，而非Http。两分钟
    	private final int lineWatcherMS = RootConfig.getInstance().getIntProperty(RootConfig.p_enableLineWatcher);//60 * 1000 * 5;
    	private long sendLineMS;
    	private final byte[] zeroUDPBS = new byte[0];
    	
    	private final boolean reconnect(){
			if(SIPManager.isOnRelay(coreSS) && coreSS.sipContext.isClose() == false){
				L.V = L.O ? false : LogManager.log("Use exist socket");
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
	    		isSendLive = false;
	    		
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
					SIPManager.startConnectionInit(coreSS);
				}
			}else if(mode == ContextManager.STATUS_LINEOFF){
				L.V = L.O ? false : LogManager.log("Line Off Status");
				isSendLive = false;
				coreSS.context.setStatus(ContextManager.STATUS_NEED_NAT);
			}else{
				//lineWatcherMS == 0表示关闭
				if(lineWatcherMS > 0){
					//setIntervalMS(lineWatcherMS);
					if(isSendLive){
						//检查上次发送包是否正常收到，必须要加=，不能仅>，因为在单机极速环境下，出现相同情形
						if(Math.abs(coreSS.context.rootTagListener.getServerReceiveMS() - sendLineMS) < lineWatcherMS){
							//通
//							L.V = L.O ? false : LogManager.log("Received last line watcher package");
						}else{
							//不通
							L.V = L.O ? false : LogManager.log("remote lineoff detected by keepalive");
							SIPManager.notifyLineOff(coreSS, false, false);
							return;
						}
					}
					try{
//						L.V = L.O ? false : LogManager.log("Send line watcher package");
						final IContext contextInstance = coreSS.context;
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
						SIPManager.notifyLineOff(coreSS, false, false);
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