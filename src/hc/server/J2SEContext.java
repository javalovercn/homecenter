package hc.server;

import hc.App;
import hc.core.ClientInitor;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.EventCenter;
import hc.core.HCMessage;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IEventHCListener;
import hc.core.IStatusListen;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.ReceiveServer;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.SessionManager;
import hc.core.cache.CacheManager;
import hc.core.cache.PendStore;
import hc.core.data.ServerConfig;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLCacher;
import hc.core.util.HCURLUtil;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.WiFiDeviceManager;
import hc.res.ImageSrc;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.msb.WiFiManagerRemoteWrapper;
import hc.server.ui.ClientDesc;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.JcipManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.server.ui.design.J2SESession;
import hc.server.util.CacheComparator;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.HCEventQueue;
import hc.server.util.HCJDialog;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.ServerCUtil;
import hc.server.util.StarterParameter;
import hc.server.util.VerifyEmailManager;
import hc.util.BaseResponsor;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.TokenManager;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

public class J2SEContext extends CommJ2SEContext implements IStatusListen{
	private final HCEventQueue hcEventQueue = HCLimitSecurityManager.getHCEventQueue();
	private final Thread eventDispatchThread = HCLimitSecurityManager.getEventDispatchThread();
	protected final ThreadGroup threadPoolToken = App.getThreadPoolToken();

    @Override
    public final boolean isInLimitThread(){
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
			return true;
		}
		return false;
    }
    
	public J2SEContext(final J2SESession j2seSocketSession) {
		super(false, j2seSocketSession, new EventCenter(j2seSocketSession));
		addListenerForServer();
		j2seSocketSession.initScreenEvent();
		setStatusListen(this);

		new J2SESIPContext(j2seSocketSession){

			@Override
			public Object buildSocket(final int localPort, final String targetServer, final int targetPort){
				final Object s = super.buildSocket(localPort, targetServer, targetPort);
				if(s == null){
					//要进行重连，不同于其它如Root，
//					SIPManager.notifyLineOff(coreSS, false, false);//复用idle session
				}
				return s;
			}
		};
		
		ContextManager.setContextInstance(this);

		coreSS.setReceiver(new ReceiveServer(coreSS), new J2SEUDPReceiveServer((J2SESession)coreSS));

//		ServerUIUtil.restartResponsorServer(null, null);
		
		j2seSocketSession.keepaliveManager.keepalive.doNowAsynchronous();
		j2seSocketSession.keepaliveManager.keepalive.setEnable(true);
		
	}
	
	@Override
	public void interrupt(final Thread thread){
		thread.interrupt();
	}
	
	@Override
	public void exit(){
//		ExitManager.exit();	//ExitManager.startExitSystem()			
	}
	
	@Override
	public void notifyShutdown(){
		//获得全部通讯，并通知下线。
		L.V = L.O ? false : LogManager.log("close a session for shutdown...");
		
    	ContextManager.shutDown(coreSS);
    }
	
//	private static long lastCanvasMainAction = 0;
	private void doCanvasMain(final String url){
		//删除时间过滤，由于版本普遍更新后，该过滤将失去作用
		//为了防服务器推送后,旧版本的客户端的请求再次到来,加时间过滤.
//		final long currentTimeMillis = System.currentTimeMillis();
//		if(currentTimeMillis - lastCanvasMainAction < 10000){
//			return;
//		}
//		lastCanvasMainAction = currentTimeMillis;
		
		//检查UDP通道的可达性
		if(isDoneUDPChannelCheck == false){
			L.V = L.O ? false : LogManager.log("Ready check UDP channel usable");
			if(isBuildedUPDChannel){
				L.V = L.O ? false : LogManager.log("Auto Disable UDP Channel");
				//关闭不可通达的UDP
				isBuildedUPDChannel = false;
			}else{
				L.V = L.O ? false : LogManager.log("UDP Channel is Disabled by NO_MSG_RECEIVE");
			}
			isDoneUDPChannelCheck = true;
		}
		
//		L.V = L.O ? false : LogManager.log("Receive Req:" + url);
		
		//不能入pool，必须synchronized
		final BaseResponsor responsor = ServerUIUtil.getResponsor();
		responsor.onEvent((J2SESession)coreSS, ProjectContext.EVENT_SYS_MOBILE_LOGIN);
		
		HCURLUtil.process(coreSS, url, coreSS.urlAction);
	}
	
	@Override
	public void run() {
		super.run();
		
		new ClientInitor(this, eventCenter);
		
		eventCenter.addListener(new IEventHCListener() {
			@Override
			public byte getEventTag() {
				return MsgBuilder.E_CLASS;
			}
			
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				//classNameLen(4) + classBS + paraLen(4) + paraBS
				
				int nextReadIdx = MsgBuilder.INDEX_MSG_DATA;
				final int classBSLen = (int)ByteUtil.fourBytesToLong(bs, nextReadIdx);
				nextReadIdx += 4;
				final String className = ByteUtil.buildString(bs, nextReadIdx, classBSLen, IConstant.UTF_8);
				nextReadIdx += classBSLen;
				final int paraBSLen = (int)ByteUtil.fourBytesToLong(bs, nextReadIdx);
				nextReadIdx += 4;
				
				J2SEEClassHelper.dispatch((J2SESession)coreSS, className, bs, nextReadIdx, paraBSLen);
				return true;
			}
		});
		
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
//				if(SIPManager.isSameUUID(event.data_bs)){
					if(IConstant.serverSide){
						//客户端主动下线
						final String token = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
						if(token.equals(RootServerConnector.getHideToken())){//注意：此逻辑一般只在HCCtrl内触发。
							notifyExitByMobi((J2SESession)coreSS);
						}else{
							L.V = L.O ? false : LogManager.log("Error Token at client shutdown");
						}
					}else{
						//TODO j2se客户机
					}
					return true;
//				}
//				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_TAG_SHUT_DOWN_BETWEEN_CS;
			}});
		
		startAllServers(coreSS);
		
//		KeepaliveManager.keepalive.setEnable(true);
//		KeepaliveManager.keepalive.doNowAsynchronous();
		
		if(IConstant.serverSide){
			//服务器端增加各种MobiUI应答逻辑
			
			eventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs, final CoreSession coreSS) {
					final String jcip = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
					JcipManager.responseCtrlSubmit((J2SESession)coreSS, jcip);
					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_CTRL_SUBMIT;
				}});

			eventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs, final CoreSession coreSS) {
					final String url = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);

					doCanvasMain(url);

					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_CANVAS_MAIN;
				}});
			
			eventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs, final CoreSession coreSS) {
					final J2SESession j2seCoreSS = (J2SESession)coreSS;
					final ClientDesc clientDesc = j2seCoreSS.clientDesc;
					
					clientDesc.refreshClientInfo(j2seCoreSS, HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA));

					//TODO YYH 旧版本的通知，已被新版本替代，未来需要关闭。如果手机版本过低，产生通知
					final String pcReqMobiVer = (String)doExtBiz(BIZ_GET_REQ_MOBI_VER_FROM_PC, null);
					if(StringUtil.higher(pcReqMobiVer, clientDesc.getHCClientVer())){
						send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_OLD_MOBI_VER_STATUS));
						LogManager.err("mobile min version : [" + pcReqMobiVer + "] is required, current mobile version : [" + clientDesc.getHCClientVer() + "]");
						L.V = L.O ? false : LogManager.log("Cancel mobile login process");
						sleepAfterError();
						SIPManager.notifyLineOff(coreSS, false, false);
						return true;
					}
					
					coreSS.context.setCheck(true);
					
//						if(ServerUIAPIAgent.getMobileAgent(j2seCoreSS).getRMSCacheServerUID().length() == 0){
//							//有可能手机端删除了曾登录账号，又重新连接时，出现此情况。
//							CacheManager.removeUIDFrom(ServerUIAPIAgent.getMobileUID());
//						}//注意：如果不一致，serverUID到客户端时，手机会主动断开。
					
					CUtil.setUserExtFactor(J2SEContext.this, UserThreadResourceUtil.getMobileAgent(j2seCoreSS).getEncryptionStrength());
					
					//服务器产生一个随机数，用CertKey和密码处理后待用，
					final byte[] randomBS = new byte[MsgBuilder.UDP_BYTE_SIZE];
					CCoreUtil.generateRandomKey(ResourceUtil.getStartMS(), randomBS, MsgBuilder.INDEX_MSG_DATA, CUtil.TRANS_CERT_KEY_LEN);
					J2SEContext.this.resetCheck();
					J2SEContext.this.SERVER_READY_TO_CHECK = randomBS;
					
					final byte[] randomEvent = ContextManager.cloneDatagram(randomBS);
					L.V = L.O ? false : LogManager.log("Send random data to client for check CK and password");
					send(MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD, randomEvent, CUtil.TRANS_CERT_KEY_LEN);

					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_CLIENT_INFO;
				}});
			
			eventCenter.addListener(new IEventHCListener() {
				final Vector<PendStore> vector = CacheComparator.getPendStoreVector((J2SESession)coreSS);
				
				@Override
				public final boolean action(final byte[] bs, final CoreSession coreSS) {
					final int offset = MsgBuilder.INDEX_MSG_DATA;
					final int len = HCMessage.getMsgLen(bs);
					
					final long currMS = System.currentTimeMillis();
					
					synchronized(vector){
						final int size = vector.size();
						for (int k = size; k > 0; k--) {
							final int index = k - 1;
							final PendStore ps = vector.elementAt(index);
							
							if(currMS - ps.createMS > 20000){//删除超时的。
								if(L.isInWorkshop){
									L.V = L.O ? false : LogManager.log("delete overtime pending cache store item.");//不用提示给用户
								}
								vector.removeElementAt(index);
								continue;
							}
							
							final byte[] psCode = ps.codeBS;
							boolean match = (len == psCode.length);
							if(match){
								for (int j = 0; j < len; j++) {
									if(psCode[j] != bs[j + offset]){
										match = false;
										break;
									}
								}
							}
							
							if(match){
								CacheManager.storeCache(ps.projID, ps.softUID, ps.urlID, 
										ps.projIDbs, 0, ps.projIDbs.length, 
										ps.softUidBS, 0, ps.softUidBS.length, 
										ps.urlIDBS, 0, ps.urlIDBS.length, 
										ps.codeBS, 0, ps.codeBS.length, 
										ps.scriptBS, 0, ps.scriptBS.length, true);
								vector.removeElementAt(index);
								return true;
							}
						}
					}
					
					LogManager.errToLog("[cache] pend store is not matched. [" + ByteUtil.toHex(bs, offset, len) + "]");
					
					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_RESP_CACHE_OK;
				}
			});
			
			eventCenter.addListener(new IEventHCListener(){
				@Override
				public final boolean action(final byte[] bs, final CoreSession coreSS) {
					ServerUIUtil.getResponsor().activeNewOneTimeKeys((J2SESession)coreSS);//服务器端收到确认，由于是在EventCenter进程，所以不需加锁
//					LogManager.info("successful E_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL");

					return true;
				}

				@Override
				public final byte getEventTag() {
					return MsgBuilder.E_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL;
				}});
		}
	}
	
    @Override
	public void displayMessage(final String caption, final String text, final int type, final Object imageData, final int timeOut){
    	TrayMenuUtil.displayMessage(caption, text, type, imageData, timeOut);
    }
    
	public static String[][] splitFileAndVer(final String files, final boolean isTempMode){
		final String[] fs = files.split(";");
		
		final int size = fs.length;
		final String[][] out = new String[size][2];
		
		for (int i = 0; i < size; i++) {
			final String[] tmp = fs[i].split(":");
			if(tmp.length < 2){
				return null;
			}
			out[i][0] = tmp[0];
			out[i][1] = tmp[1];
		}
		
		return out;
	}
	
	@Override
	public Object getSysImg() {
		return App.SYS_LOGO;
	}

	@Override
	public boolean isSoundOnMode() {
		//TODO isSoundOff
		return false;
	}
	
//	public final void send(byte type_request, InetAddress address, int port) {
//		DatagramPacket p = (DatagramPacket)dpCacher.getFree();
//		
//		byte[] bs = p.getData();
//		
//		Message.setSendUUID(bs, selfUUID, selfUUID.length);
//		
//		bs[MsgBuilder.INDEX_CTRL_TAG] = type_request;
//		bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_NOT_SPLIT;
//		p.setLength(MsgBuilder.MIN_LEN_MSG);
////		p.setData(bs, 0, MsgBuilder.MIN_LEN_MSG);
//		
//		p.setAddress(address);
//		p.setPort(port);
//		
//		sServer.pushIn(p);
//	}
	
//	/**
//	 * 该逻辑被切换中继，j2se客户端，服务器进入接入等调用
//	 * 注意：各调用环境的条件状态
//	 */
//	public final void setTargetPeer(String ip, String port, Object datagram) throws Exception {
//		if(datagram == null){
//			int status = ContextManager.cmStatus;
//			
//			try {
//				if(status == ContextManager.STATUS_SERVER_SELF || status == ContextManager.STATUS_CLIENT_SELF){
//					//Relay切换到另一个Relay
//				}else{
//					J2SESIPContext.endPunchHoleProcess();
//				}
//				L.V = L.O ? false : LogManager.log("setTargetPeer, IP:" + ip + ":" + port);
//				TargetPeer tp = new TargetPeer();
//				tp.clientInet = InetAddress.getByName(ip);
//				tp.clientPort = Integer.parseInt(port);
//				KeepaliveManager.setClient(tp);
//			} catch (Exception e) {
//				ExceptionReporter.printStackTrace(e);
//			}
//		}else{
//			DatagramPacket packet = (DatagramPacket)datagram;
//			try {
//				packet.setAddress(InetAddress.getByName(ip));
//			} catch (UnknownHostException e) {
//				ExceptionReporter.printStackTrace(e);
//			}
//			packet.setPort(Integer.parseInt(port));
//		}
//	}

	@Override
	public Object doExtBiz(final short bizNo, final Object newParam) {
		if(bizNo == IContext.BIZ_LOAD_SERVER_CONFIG){
			if(IConstant.serverSide){
//				if(SIPManager.isRelayServerNATType(NatHcTimer.LocalNATType)){
//					//具备中继型的服务器，对客户机进行最优发现，上限不超过均值
//					sc.setTryMaxMTU(MsgBuilder.UDP_DEFAULT_BYTE_SIZE);
//				}else if(SIPManager.isOnRelay()){
//					//依赖于中继的服务器，上限不能高于均值
//					sc.setTryMaxMTU(MsgBuilder.UDP_DEFAULT_BYTE_SIZE);					
//				}else{
//					//TODO 从用户配置中，获得尝试的最大传送块
//					sc.setTryMaxMTU(MsgBuilder.UDP_DEFAULT_BYTE_SIZE);
//				}
//
//				//TODO 从用户配置中，获得是否要求进行最优发现，缺省为0
//				sc.setMTU(1448);//MTU设置为0，表示通知客户端，要进行最优发现
//				sc.setTryMaxMTU(Integer.parseInt(
//						RootConfig.getInstance().getProperty(RootConfig.p_MaxUDPSize)));//仅在MTU设置为0下，最优化时，尝试的上限(含)
				
				final int[] screenSize = ResourceUtil.getSimuScreenSize();

				//abc###efg
				final ServerConfig sc = new ServerConfig("");
				sc.setProperty(ServerConfig.p_HC_VERSION, StarterManager.getHCVersion());
				sc.setProperty(ServerConfig.p_MIN_MOBI_VER_REQUIRED_BY_PC, minMobiVerRequiredByServer);
				sc.setProperty(ServerConfig.P_SERVER_COLOR_ON_RELAY, RootConfig.getInstance().getProperty(RootConfig.p_Color_On_Relay));
				
				sc.setProperty(ServerConfig.P_SERVER_WIDTH, String.valueOf(screenSize[0]));
				sc.setProperty(ServerConfig.P_SERVER_HEIGHT, String.valueOf(screenSize[1]));
//				sc.setProperty(ServerConfig.P_SERVER_WIDTH, 360);
//				sc.setProperty(ServerConfig.P_SERVER_HEIGHT, 360);				
				return sc.toTransString();
			}else{
				return null;
			}
//		}else if(bizNo == IContext.BIZ_IS_ON_SERVICE){
//			if(IConstant.serverSide){
//				KeepaliveManager.keepalive.setEnable(true);
//			}
		}else if(bizNo == IContext.BIZ_UPLOAD_LINE_ON){
//			String hostAddress = SIPManager.LocalNATIPAddress;
//
//			if(hostAddress.equals(SIPManager.getExternalUPnPIPAddress())){
//				L.V = L.O ? false : LogManager.log("UPnP external IP == Public IP");
//				SIPManager.LocalNATPort = SIPManager.getExternalUPnPPort();
//				SIPManager.LocalNATType = EnumNAT.OPEN_INTERNET;
//			}
			
//			if(IConstant.serverSide && SIPManager.isRelayFullOpenMode(SIPManager.LocalNATType)){
//				RelayManager.notifyIsRelayServer();
//			}

//			ti.putTip(JPTrayIcon.NAT_DESC, EnumNAT.getNatDesc(UDPChannel.nattype));
//			ti.putTip(JPTrayIcon.PUBLIC_IP, hostAddress + ":" + String.valueOf(UDPChannel.publicPort));
			
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("upload direct server : " + StarterParameter.homeWirelessIpPort.ip + ", port : " + StarterParameter.homeWirelessIpPort.port);
			}
			final String out = RootServerConnector.lineOn(
					IConstant.getUUID(), StarterParameter.homeWirelessIpPort.ip, 
					StarterParameter.homeWirelessIpPort.port, 0, 1, 
					StarterParameter.relayServerUPnPIP, StarterParameter.relayServerUPnPPort,
					coreSS.relayIpPort.ip, coreSS.relayIpPort.port, TokenManager.getToken(), 
					DefaultManager.isHideIDForErrCert(), RootServerConnector.getHideToken());
			
			if(out == null){
				LogManager.err("unable to connect root server");
				SIPManager.notifyLineOff(coreSS, true, false);
				final String[] ret = {"false"};
				return ret;
			}else if(out.equals("e")){
				String msg = (String)ResourceUtil.get(9113);
				msg = StringUtil.replace(msg, "{uuid}", IConstant.getUUID());//html tag is in it.
				
				LogManager.errToLog(msg);//html can't be displayMessage in TrayIcon.
				
				LineFailManager.showLineFailWindow((J2SESession)coreSS, msg);
				
				final String[] ret = {"false"};
				return ret;
			}else if(out.equals("d")){
				RootConfig.getInstance().setProperty(RootConfig.p_Color_On_Relay, "5");
				RootConfig.getInstance().setProperty(RootConfig.p_MS_On_Relay, 100);
			}else if(out.equals("v")){
				if(VerifyEmailManager.isVerifiedEmail() == false){
					PropertiesManager.setValue(PropertiesManager.p_IsVerifiedEmail, IConstant.TRUE);
				}
			}else if(out.equals("ov")){
				VerifyEmailManager.showVerifyEmailWarning(threadPoolToken);
			}else if(out.length() == 0){
				if(VerifyEmailManager.isVerifiedEmail()){//本机以前认证过，但是又有后认证的机器替换掉本机token
					VerifyEmailManager.showVerifyEmailWarning(threadPoolToken);
				}
			}
			
			LineFailManager.closeLineFailWindow();//外部网络故障消失，关闭窗口。
			
			if(TrayMenuUtil.setTrayEnable(true)){
				final String msg = (String) ResourceUtil.get(9008);//Line On, Ready for client
				TrayMenuUtil.displayMessage(ResourceUtil.getInfoI18N(), 
					msg, IContext.INFO, null, 0);
			}
			
			SingleMessageNotify.closeDialog(SingleMessageNotify.TYPE_ERROR_CONNECTION);
			
//			本地无线信息，不需在日志中出现。
//				L.V = L.O ? false : LogManager.log("Success line on " 
//						+ ServerUIUtil.replaceIPWithHC(KeepaliveManager.homeWirelessIpPort.ip) + ", port : " 
//						+ KeepaliveManager.homeWirelessIpPort.port + " for client connection");
			
			//上传上线信息
			if(coreSS.relayIpPort.port > 0){	
				L.V = L.O ? false : LogManager.log("Success line on " 
						+ HttpUtil.replaceIPWithHC(coreSS.relayIpPort.ip)  + ", port : " 
						+ coreSS.relayIpPort.port + " for client connection");
			}
			
			return null;
		}else if(bizNo == IContext.BIZ_GET_FORBID_UPDATE_CERT_I18N){
			return ResourceUtil.get(9121);
		}else if(bizNo == IContext.BIZ_AFTER_HOLE){
			if(IConstant.serverSide){
				//服务器端
				final String sc = (String)doExtBiz(IContext.BIZ_LOAD_SERVER_CONFIG, null);
				send(
						MsgBuilder.E_TRANS_SERVER_CONFIG, sc != null?sc:"");//必须发送，因为手机端会返回
				L.V = L.O ? false : LogManager.log("Transed Server Config");
				

				//等待，让ServerConfig先于后面的包到达目标

				//并将该随机数发送给客户机，客户机用同法处理后回转给服务器
				//服务器据此判断客户机的CertKey和密码状态				
			}
			
			return null;
		}
		
//		if(bizNo == IContext.BIZ_OPEN_REQ_BUILD_CONN_LISTEN){
//			eventCenter.addListener(new IEventHCListener(){
//				public final boolean action(final byte[] bs) {
//					if(ContextManager.isNotWorkingStatus()){
//						send(MsgBuilder.E_TAG_REQUEST_BUILD_CONNECTION);
//						L.V = L.O ? false : LogManager.log("Successful connect to the target peer");
//						ContextManager.setStatus(ContextManager.STATUS_READY_FOR_CLIENT);
//						if(IConstant.serverSide){
//							//将askForService移出，因为有可能密码验证不通过
//							NatHcTimer.LISTENER_FROM_HTTP.setEnable(false);
//						}
//					}
//					return true;
//				}
//	
//				public final byte getEventTag() {
//					return MsgBuilder.E_TAG_REQUEST_BUILD_CONNECTION;
//				}});
//			return null;
//		}
		if(bizNo == IContext.BIZ_CHANGE_RELAY){
			final String[] ips = (String[])newParam;
			RootServerConnector.changeRelay(IConstant.getUUID(), ips[0], ips[1], TokenManager.getToken());
			return null;
		}else if(bizNo == IContext.BIZ_START_WATCH_KEEPALIVE_FOR_RECALL_LINEOFF){
			if (((J2SESession)coreSS).keepaliveManager.keepalive.isEnable() == false){
				if(L.isInWorkshop){
					L.V = L.O ? false : LogManager.log("BIZ_START_WATCH_KEEPALIVE keepalive : false");
				}
				coreSS.eventCenterDriver.addWatcher(new IWatcher() {
					final long startMS = System.currentTimeMillis();
					@Override
					public boolean watch() {
						if(System.currentTimeMillis() - startMS > 5000){
							if(((J2SESession)coreSS).keepaliveManager.keepalive.isEnable() == false){
								if(L.isInWorkshop){
									L.V = L.O ? false : LogManager.log("BIZ_START_WATCH_KEEPALIVE set keepalive : true");
								}
								((J2SESession)coreSS).keepaliveManager.keepalive.setEnable(true);
							}
							return true;
						}
						return false;
					}
					@Override
					public void setPara(final Object p) {
					}
					@Override
					public boolean isCancelable() {
						return false;
					}
					@Override
					public void cancel() {
					}
				});
			}else{
				if(L.isInWorkshop){
					L.V = L.O ? false : LogManager.log("BIZ_START_WATCH_KEEPALIVE keepalive still : true");
				}
			}
		}else if(bizNo == IContext.BIZ_GET_TOKEN){
			return TokenManager.getTokenBS();
		}else if(bizNo == IContext.BIZ_SET_TRAY_ENABLE){
			if(newParam instanceof Boolean){
				final boolean b = (Boolean)newParam;
				TrayMenuUtil.setTrayEnable(b);
			}
			return null;
		}else if(bizNo == IContext.BIZ_DATA_CHECK_ERROR){
		}
		
		if(IConstant.serverSide){
			if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS){
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR){
				doAfterCertKeyError(coreSS);
				RootServerConnector.notifyLineOffType(coreSS, RootServerConnector.LOFF_CertErr_STR);
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_PWD_ERROR){
				doAfterPwdError(coreSS);
				RootServerConnector.notifyLineOffType(coreSS, RootServerConnector.LOFF_pwdErr_STR);
			}
		}else{
			if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS){
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR){
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_PWD_ERROR){
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_SERVICE_IS_FULL){
				
			}else if(bizNo == IContext.BIZ_SERVER_AFTER_UNKNOW_STATUS){
				
			}
		}
		if(bizNo == IContext.BIZ_VERSION_MID_OR_PC){
			return StarterManager.getHCVersion();
		}else if(bizNo == IContext.BIZ_GET_REQ_MOBI_VER_FROM_PC){
			return minMobiVerRequiredByServer;
		}
		return null;
	}

	public final void startTransMobileContent() {
//		if(true || ScreenServer.askForService((J2SESocketSession)coreSS)){//多路服务模式
			ServerCUtil.transOneTimeCertKey(this);
			
			L.V = L.O ? false : LogManager.log("Pass Certification Key and password");
			
			HCURLUtil.sendEClass(coreSS, HCURLUtil.CLASS_TRANS_SERVER_UID, PropertiesManager.getValue(PropertiesManager.p_RMSServerUID));
			
			//由于会传送OneTimeKey，所以不需要下步
			//instance.send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS));

			//必须要提前到sleep(1000)之前，以便接收相应状态的消息。否则Invalid statue tag received
			setStatus(ContextManager.STATUS_SERVER_SELF);

			if(ConfigManager.isTCPOnly == false){//UDP情形下，等待OneTimeCertKey数据到达
				try{
					Thread.sleep(1000);
				}catch (final Exception e) {
				}
			}
			
			//由原来的客户端请求,改为服务器推送
			doCanvasMain("menu://root");
			
//			final TargetPeer target = KeepaliveManager.target;

			final String msg = TrayMenuUtil.buildMobileConnectionTip();
			displayMessage(ResourceUtil.getInfoI18N(), msg, 
					IContext.INFO, null, 0);
			TrayMenuUtil.notifyMobileLineOn();
			
	}

	private static void doAfterPwdError(final CoreSession coreSS) {
		L.V = L.O ? false : LogManager.log("Send Error password status to client");
		coreSS.context.send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_PWD_ERROR));
		sleepAfterError();
		
		SIPManager.notifyLineOff(coreSS, true, false);
	}
	
	public static void sleepAfterError(){
		try{
			Thread.sleep(1000);
		}catch (final Exception e) {
		}
	}

	private static void doAfterCertKeyError(final CoreSession coreSS) {
		if(DefaultManager.isEnableTransNewCertNow()){
			//传输证书
			TrayMenuUtil.transNewCertKey(coreSS.context);
//			try{
//				//增加时间，确保transOneTimeCertKey后于NewCertKey
//				Thread.sleep(300);
//			}catch (Exception e) {
//				
//			}
			L.V = L.O ? false : LogManager.log(RootServerConnector.unObfuscate("rtnapsro teCtrK yet  olceitn."));
		}else{
			L.V = L.O ? false : LogManager.log("reject a mobile login with invalid certification.");
			coreSS.context.send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR));
			final String errCertTitle = (String)ResourceUtil.get(9182);
			final String errCert = (String)ResourceUtil.get(9183);
			SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_ERROR_CERT, 
					errCert, errCertTitle, 1000 * 60,
					App.getSysIcon(App.SYS_ERROR_ICON));
//			LogManager.errToLog("Mobile login with ERROR CertKey");
			sleepAfterError();
			SIPManager.notifyLineOff(coreSS, true, false);
		}
	}

	@Override
	public Object getProperty(final Object propertyID) {
		return PropertiesManager.getValue((String)propertyID);
	}

	/**
	 * 如果jruby.jar升级，由于Android环境下依赖此，重要测试
	 */
	public static final String jrubyjarname = ResourceUtil.getLibNameForAllPlatforms("jruby.jar");

	@Override
	public void notify(final short statusFrom, final short statusTo) {
		final J2SESession j2seCoreSS = (J2SESession)coreSS;

		if(statusTo == ContextManager.STATUS_LINEOFF){
			j2seCoreSS.keepaliveManager.release();
			
			if(statusFrom == ContextManager.STATUS_SERVER_SELF){
				ScreenServer.emptyScreen(j2seCoreSS);
				final BaseResponsor responsor = ServerUIUtil.getResponsor();
			
//						ClientDesc.agent.set(ConfigManager.UI_IS_BACKGROUND, IConstant.TRUE);
				if(responsor != null){//退出时，多进程可能导致已关闭为null
//							responsor.onEvent(ProjectContext.EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND);
					responsor.onEvent(j2seCoreSS, ProjectContext.EVENT_SYS_MOBILE_LOGOUT);
				}
			}
		}
		
		if(statusTo == ContextManager.STATUS_NEED_NAT){
			TrayMenuUtil.setTrayEnable(false);
		}else if(statusFrom == ContextManager.STATUS_NEED_NAT){
		}

		if(statusFrom == ContextManager.STATUS_READY_TO_LINE_ON){
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("remove HCTimer [aliveRefresher].");
			}
			HCTimer.remove(j2seCoreSS.keepaliveManager.aliveToRootRefresher);
			j2seCoreSS.isIdelSession = false;
			
			J2SESessionManager.startSession();
		}
		
		if(statusTo == ContextManager.STATUS_READY_TO_LINE_ON){
		}

		if(statusTo == ContextManager.STATUS_READY_MTU){
			//直联或中继初始接入，但未进入验证参数传送
			j2seCoreSS.keepaliveManager.connBuilderWatcher.resetTimerCount();
			j2seCoreSS.keepaliveManager.connBuilderWatcher.setEnable(true);				
		}
		
		if(statusTo == ContextManager.STATUS_SERVER_SELF){
//			L.V = L.O ? false : LogManager.log("set setIntervalMS to " + KeepaliveManager.KEEPALIVE_MS);
			j2seCoreSS.keepaliveManager.keepalive.setIntervalMS(j2seCoreSS.keepaliveManager.KEEPALIVE_MS);
			
			//由于接收菜单大数据可能需要消耗较多时间，故resetTimerCount变相增加时间量。
			//注意：要置于上行之后，以使用新的时间间隔来重算
			j2seCoreSS.keepaliveManager.keepalive.resetTimerCount();
			
			j2seCoreSS.keepaliveManager.resetSendData();//为接收Menu增加时间
		}
	}
	
	public static final String MAX_HC_VER = "9999999";//注意与Starter.NO_UPGRADE_VER保持同步
	
	private final String minMobiVerRequiredByServer = "7.12";//(含)，
	//你可能 还 需要修改服务器版本，StarterManager HCVertion = "6.97";
	public WiFiDeviceManager remoteWrapper;
	
	public static final String getSampleHarVersion(){
		return "7.2";
	}
	
	public static void notifyExitByMobi(final J2SESession coreSS) {
		L.V = L.O ? false : LogManager.log("Client/Relay request lineoff!");

		coreSS.context.displayMessage(
				ResourceUtil.getInfoI18N(), 
				(String)ResourceUtil.get(9006), IContext.INFO, null, 0);

		final String mobileUID = UserThreadResourceUtil.getMobileSoftUID(coreSS);//注意：需提前取得，否则可能关闭ClientSession，而得不到。
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				//不在用户上线时，传递数据前处理，而是置于下线时。以节省用户时间。
				CacheManager.checkAndDelCacheOverflow(mobileUID);//可能超载，只限于服务器端		
			}
		});

		RootServerConnector.notifyLineOffType(coreSS, RootServerConnector.LOFF_MobReqExitToPC_STR);

		SIPManager.notifyLineOff(coreSS, true, false);
	}

	@Override
	public final WiFiDeviceManager getWiFiDeviceManager() {
		final WiFiDeviceManager platManager = PlatformManager.getService().getWiFiManager();
		if(ResourceUtil.canCreateWiFiAccountOnPlatform(platManager)){
			//优先使用服务器自带WiFi
			return platManager;
		}else if(ContextManager.isMobileLogin(this) && UserThreadResourceUtil.getMobileAgent((J2SESession)coreSS).ctrlWiFi()){
			//如果手机上线，借用手机WiFi功能
			synchronized (coreSS) {
				if(remoteWrapper == null){
					remoteWrapper = new WiFiManagerRemoteWrapper(coreSS);
				}
				return remoteWrapper;
			}
		}else{
			//无WiFi环境
			return platManager;
		}
	}

	@Override
	public void notifyStreamReceiverBuilder(final boolean isInputStream, final String className,
			final int streamID, final byte[] bs, final int offset, final int len) {
	}

	private final void addListenerForServer() {
		final ThreadGroup threadPoolToken = App.getThreadPoolToken();
		eventCenter.addListener(new IEventHCListener(){
			IHCURLAction urlAction;
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final String cmd = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
//					L.V = L.O ? false : LogManager.log("Receive URL:" + cmd);
				if(urlAction == null){
					urlAction = coreSS.urlAction;
				}
				HCURLUtil.process(coreSS, cmd, urlAction);
				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_GOTO_URL;
			}});

		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final String url = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
				
				final HCURL hu = HCURLUtil.extract(url);

				final String protocal = hu.protocal;
				final String elementID = hu.elementID;

				if(protocal == HCURL.CMD_PROTOCAL){
					if(elementID.equals(HCURL.DATA_CMD_SendPara)){
						final String para1= hu.getParaAtIdx(0);
						if(para1 != null && para1.equals(HCURL.DATA_PARA_CERT_RECEIVED)){
							final String value1 = hu.getValueofPara(para1);
							if(value1.equals(CCoreUtil.RECEIVE_CERT_OK)){
								if(cmStatus != ContextManager.STATUS_READY_TO_LINE_ON){
//										if(PropertiesManager.isTrue(PropertiesManager.p_NewCertIsNotTransed)){
										PropertiesManager.setValue(PropertiesManager.p_NewCertIsNotTransed, IConstant.FALSE);
										PropertiesManager.saveFile();

										TrayMenuUtil.enableTransNewCertMenuItem();
										
										TrayMenuUtil.showTransQues(threadPoolToken);
									}
//									}
							}else if(value1.equals(CCoreUtil.RECEIVE_CERT_FORBID)){
								if(cmStatus != ContextManager.STATUS_READY_TO_LINE_ON){
									final String forbid_update_cert = (String)doExtBiz(IContext.BIZ_GET_FORBID_UPDATE_CERT_I18N, null);
									
									LogManager.err("mobile side:" + forbid_update_cert);
									SIPManager.notifyLineOff(coreSS, true, false);
									RootServerConnector.notifyLineOffType(coreSS, RootServerConnector.LOFF_forbidCert_STR);
									SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_FORBID_CERT, 
											StringUtil.replace((String)ResourceUtil.get(9078), "{forbid}", forbid_update_cert), 
											(String)ResourceUtil.get(IContext.ERROR),
											60000 * 5, App.getSysIcon(App.SYS_ERROR_ICON));
									
								}
							}

						}
					}
				}
				HCURLCacher.getInstance().cycle(hu);
				
				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_GOTO_URL_UN_XOR;
			}});

		
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag(){
				return MsgBuilder.E_LINE_OFF_EXCEPTION;
			}
			
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {					
				final Boolean isClientReq = Boolean.parseBoolean(HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA));

				doLineOffProcess((J2SESession)coreSS, isClientReq);
				
				J2SESessionManager.startSession();
				return true;
			}});
		
		eventCenter.addListener(new IEventHCListener() {
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_RANDOM_FOR_CHECK_SERVER;
			}
			int count = 0;
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final J2SESession j2seCoreSS = (J2SESession)coreSS;
				
				if(j2seCoreSS.isWillCheckServer){
					j2seCoreSS.isWillCheckServer = false;
					count = 0;
					final int len = HCMessage.getBigMsgLen(bs);
					//收到客户端发来的随机信息，验证后，发回客户端
					final byte[] certKey = CUtil.getCertKey();
					final byte[] passwordBS = IConstant.getPasswordBS();
					CUtil.checkServer(bs, MsgBuilder.INDEX_MSG_DATA, len, certKey, 0, certKey.length, passwordBS, 0, passwordBS.length);
					sendWrap(MsgBuilder.E_RANDOM_FOR_CHECK_SERVER, 
							bs, MsgBuilder.INDEX_MSG_DATA, len);
					
					//必须先发回服务器签名，才能发送正常的内容。
					//客户端认可服务器失败后，会拒收以下的数据
					
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							startTransMobileContent();
						}
					});
				}else{
					if(count++ > 0){//忽略第一次
						LogManager.errToLog("ignore random data for identify server.");
					}
				}
				return true;
			}
		});
		
		eventCenter.addListener(new IEventHCListener(){
			short pwdErrTry;
			long lastErrMS;
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				L.V = L.O ? false : LogManager.log("Receive the back data which to check CK and password");
				
//					System.out.println("pwdErrTry : " + pwdErrTry + ",  MAXTimers : " + LockManager.MAXTIMES);
				
				if(pwdErrTry < SystemLockManager.MAXTIMES){
				}else{
					if(System.currentTimeMillis() - lastErrMS < SystemLockManager.LOCK_MS){
						SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_LOCK_CERT, 
								"System is locking now!!!<BR><BR>Err Password or certification more than "+SystemLockManager.MAXTIMES+" times.", 
								"Lock System now!!", 1000 * 60 * 1, App.getSysIcon(App.SYS_WARN_ICON));
						LogManager.errToLog("Err Password or certification more than "+SystemLockManager.MAXTIMES+" times.");
						send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_UNKNOW_STATUS));
						sleepAfterError();
						SIPManager.notifyLineOff(coreSS, true, false);
						
						return true;
					}else{
						resetErrInfo();
					}
				}

				//并将该随机数发送给客户机，客户机用同法处理后回转给服务器
				//服务器据此判断客户机的CertKey和密码状态
				
				if(SERVER_READY_TO_CHECK == null){
					L.V = L.O ? false : LogManager.log("Server Reconnected, Skip the back data");
					return true;
				}
				
				//由于每次检验，所以保留备份
				final byte[] oldbs = new byte[SERVER_READY_TO_CHECK.length];
				for (int i = 0; i < oldbs.length; i++) {
					oldbs[i] = SERVER_READY_TO_CHECK[i];
				}
				
				final short b = ServerCUtil.checkCertKeyAndPwd(coreSS.context, coreSS.OneTimeCertKey, bs, 
						MsgBuilder.INDEX_MSG_DATA, IConstant.getPasswordBS(), CUtil.getCertKey(), oldbs);
				if(b == IContext.BIZ_SERVER_AFTER_PWD_ERROR){
					doExtBiz(b, null);
					lastErrMS = System.currentTimeMillis();
					L.V = L.O ? false : LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");
				}else if(b == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR && isSecondCertKeyError == true){
					lastErrMS = System.currentTimeMillis();
					
					L.V = L.O ? false : LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");

					send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_PWD_ERROR));
					L.V = L.O ? false : LogManager.log("Second Cert Key Error, send Err Password status");		
					
					ServerCUtil.notifyErrPWDDialog();
					
					SIPManager.notifyLineOff(coreSS, true, false);
				}else{
					doExtBiz(b, null);
					
					if(b == IContext.BIZ_SERVER_AFTER_CERTKEY_ERROR && isSecondCertKeyError == false){
						L.V = L.O ? false : LogManager.log("Error Pwd OR Certifcation try "+ (++pwdErrTry) +" time(s)");
						isSecondCertKeyError = true;
					}else{
						resetCheck();
						resetErrInfo();
						((J2SESession)coreSS).isWillCheckServer = true;//开启只接收一次的状态，接收后置false，以防止被滥用。
					}
				}
				return true;
			}

			private void resetErrInfo() {
				pwdErrTry = 0;
				lastErrMS = 0;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_RANDOM_FOR_CHECK_CK_PWD;
			}});
		
		eventCenter.addListener(new IEventHCListener() {
			
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_TAG_SERVER_RELAY_START;
			}
			
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				setConnectionModeStatus(ContextManager.MODE_CONNECTION_RELAY);
				
				if(SIPManager.isOnRelay(coreSS)){
					setStatus(ContextManager.STATUS_READY_MTU);
				}
				return true;
			}
		});
	}

	static void doLineOffProcess(final J2SESession coreSS, final boolean isClientReq) {
		coreSS.eventCenterDriver.notifyShutdown();//考虑到用户应用的复杂度，不回收
		
		coreSS.streamBuilder.notifyExceptionForReleaseStreamResources(new IOException("System Line Off Exception"));
		
		final IContext ctx = coreSS.context;
		
		ctx.setStatus(ContextManager.STATUS_NEED_NAT);
		if(ctx != null){
			ctx.setCheck(false);
		}
		
		//可能更新p_Encrypt_Factor，故刷新(不论正常断线或手机请求)
		RootConfig.reset(true);

		ctx.resetCheck();
		
		coreSS.keepaliveManager.keepalive.setEnable(false);
		coreSS.keepaliveManager.keepalive.setIntervalMS(coreSS.keepaliveManager.KEEPALIVE_MS);
		coreSS.keepaliveManager.connBuilderWatcher.setEnable(false);
		
		ctx.coreSS.sipContext.resetNearDeployTime();
		
		SIPManager.close(coreSS);
		try{
			//setClient(null)之前，稍等，以响应当前客户可能存在的包
			Thread.sleep(CCoreUtil.WAIT_MS_FOR_NEW_CONN);
		}catch (final Exception e) {
			
		}
		
		ctx.coreSS.sipContext.resender.reset();
//		HCTimer.remove(coreSS.eventConditionWatcher.watcherTimer);
//		ConditionWatcher.cancelAllWatch();//释放在途EventBack事件，须在sleep之后
		
		SessionManager.release(coreSS);
	}
}

class PWDDialog extends HCJDialog {
	JPanel pwdPanel = new JPanel();
	JPanel btnPanel = new JPanel();
	Border border1;
	TitledBorder titledBorder1;
	JButton jbOK = null;
	JButton jbExit = null;
	JPasswordField jPasswordField1 = new JPasswordField(15);// 20个字符宽度

	public PWDDialog() {
		setModal(true);
		
		try {
			init();
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	private void init() throws Exception {

		setTitle(ResourceUtil.getProductName());
		this.setIconImage(App.SYS_LOGO);
		
		final java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				jbExit_actionPerformed(e);
			}
		};

		this.getRootPane().registerKeyboardAction(exitActionListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		//必须有此行代码，作为窗口右上的关闭响应
//		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new HCWindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				jbExit_actionPerformed(null);
			}
		});
		
		jbOK = new JButton("", new ImageIcon(ImageSrc.OK_ICON));
		jbExit = new JButton("", new ImageIcon(ImageSrc.CANCEL_ICON));

		// new LineBorder(Color.LIGHT_GRAY, 1, true)
		titledBorder1 = new TitledBorder((String)ResourceUtil.get(1007));// BorderFactory.createEtchedBorder()
		
		final JPanel root = new JPanel();
		
		App.addBorderGap(this.getContentPane(), root);
		
		root.setLayout(new BorderLayout());
		pwdPanel.setLayout(new FlowLayout());
		pwdPanel.setBorder(titledBorder1);

		jbOK.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
//				System.out.println(e);
				if(e.getSource() == jbOK){ 
					jbOK_actionPerformed(e);
				}
			}
		});

//		jlPassword.setHorizontalAlignment(SwingConstants.RIGHT);
		jbOK.setNextFocusableComponent(jbExit);
		jbOK.setSelected(true);
		jbOK.setText((String) ResourceUtil.get(IContext.OK));

		jbExit.setText((String) ResourceUtil.get(1018));
		jbExit.addActionListener(exitActionListener);

		jPasswordField1.setEchoChar('*');
		jPasswordField1.setHorizontalAlignment(SwingUtilities.RIGHT);
		jPasswordField1.enableInputMethods(true);
		
		pwdPanel.add(new JLabel("", new ImageIcon(ImageSrc.PASSWORD_ICON), SwingConstants.CENTER));
		pwdPanel.add(jPasswordField1);
		root.add(
				pwdPanel,
				BorderLayout.CENTER);

		btnPanel.setLayout(new GridLayout(1, 2, 5, 5));
		btnPanel.add(jbExit);
		btnPanel.add(jbOK);
		root.add(
				btnPanel,
				BorderLayout.SOUTH);

		this.getRootPane().setDefaultButton(jbOK);

		pack();
		// int width = 400, height = 270;
		final int width = getWidth(), height = getHeight();
		setSize(width, height);

		App.showCenter(this);
		
	}
	
	void jbOK_actionPerformed(final ActionEvent e) {
		pwd = jPasswordField1.getText();
		endDialog();
	}

	void jbExit_actionPerformed(final ActionEvent e) {
		endDialog();
	}

	private void endDialog() {
		super.dispose();
	}
	
	public String pwd;
}

abstract class LineonAndServingExecWatcher implements IWatcher{
	long currMS = System.currentTimeMillis();
	final String opName;
	protected final CoreSession coreSS;
	
	LineonAndServingExecWatcher(final CoreSession coreSS, final String opName){
		this.coreSS = coreSS;
		this.opName = opName;
	}
	
	public abstract void doBiz();
	
	@Override
	public boolean watch() {
		//防止在连接中途发生关闭传输，从而导致证书不予传输的情形
		if(coreSS.context.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON
				|| UserThreadResourceUtil.isInServing(coreSS.context)){
        	doBiz();
        	return true;
		}else{
			if(System.currentTimeMillis() - currMS > 1000 * 10){
				L.V = L.O ? false : LogManager.log("Unknow status, skip execute op [" + opName + "]");
				return true;
			}
			return false;
		}
	}

	@Override
	public void setPara(final Object p) {
	}

	@Override
	public void cancel() {
	}

	@Override
	public boolean isCancelable() {
		return false;
	}
}