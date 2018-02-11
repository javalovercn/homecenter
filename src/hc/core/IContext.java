package hc.core;

import java.io.OutputStream;

import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.core.util.WiFiDeviceManager;

public abstract class IContext {
	final boolean isServerSide = IConstant.serverSide;
	private IStatusListen statusListen;
	private short modeStatus = ContextManager.MODE_CONNECTION_NONE;
	public short cmStatus;
	protected final EventCenter eventCenter;
	public final RootTagEventHCListener rootTagListener;
	
	public abstract void onEventLineOff(final boolean isClientReq);

	/**
	 * 注意：本方法被override
	 * @param modeStat
	 */
	public void setConnectionModeStatus(final short modeStat){
		modeStatus = modeStat;
	}

	public final short getConnectionModeStatus(){
		return modeStatus;
	}

	public final void setStatusListen(IStatusListen listener){
		statusListen = listener;
	}
	
	final Object lock = new Object();
	
	public final void setStatus(final short newCmStatus){
		synchronized (lock) {
			if(newCmStatus == cmStatus){
				return;
			}
			
			final short oldCmStatus = cmStatus;
			
			cmStatus = newCmStatus;
	
			if((newCmStatus != ContextManager.STATUS_EXIT) && (oldCmStatus == ContextManager.STATUS_EXIT)){
				LogManager.log("forbid change status from [" + oldCmStatus + "] to [" + newCmStatus + "]");
				return;
			}
	
			L.V = L.WShop ? false : LogManager.log("Change Status, From [" + oldCmStatus + "] to [" + newCmStatus + "]");
			if(statusListen != null){
				statusListen.notify(oldCmStatus, newCmStatus);
			}
	
			if(newCmStatus == ContextManager.STATUS_LINEOFF){
				modeStatus = ContextManager.MODE_CONNECTION_NONE;
			}
	
			if(newCmStatus == ContextManager.STATUS_READY_MTU){
				if(IConstant.serverSide){
					try{
						//服务器稍等，提供客户初始化时间
						Thread.sleep(200);
					}catch (final Exception e) {
					}
				}
	
				//			LogManager.log("Do biz after Hole");
	
				//激活KeepAlive hctimer
				doExtBiz(IContext.BIZ_AFTER_HOLE, null);
	
				//			if(IConstant.serverSide){
				//			}else{
				//			}
			}
		}
	}
	
	private final ByteArrayCacher cache = ByteUtil.byteArrayCacher;
	private final LinkedSet sendBSBuffer = new LinkedSet();
	private final LinkedSet sendBSLenBuffer = new LinkedSet();
	private final LinkedSet screenIDBuffer = new LinkedSet();
	private byte[] toServerBS = new byte[1024];
	
	public final CoreSession coreSS;
	protected final HCConnection hcConnection;
	private final ConnectionRebuilder connectionRebuilder;
	public final Object sendLock = new Object();
	
	public IContext(final CoreSession coreSocketSession, final EventCenter eventCenter){
		rootTagListener = new RootTagEventHCListener(coreSocketSession.hcConnection);
		coreSocketSession.context = this;
		this.coreSS = coreSocketSession;
		this.hcConnection = this.coreSS.getHCConnection();//阻止继承获得
		this.connectionRebuilder = hcConnection.connectionRebuilder;
		this.eventCenter = eventCenter;
		
		//发送UDP_ADDRESS_REG包，进行转发器的地址注册，供正常数据转发时所需之地址
		eventCenter.addListener(new IEventHCListener() {
			public final byte getEventTag() {
				return MsgBuilder.E_TAG_ROOT_UDP_ADDR_REG;
			}
			
			public final boolean action(byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				//有可能收到，有可能收不到。不作任何处理。仅供UDP中继之用
//				LogManager.log("Receive E_TAG_ROOT_UDP_ADDR_REG");
				final boolean isRight = UDPPacketResender.checkUDPBlockData(bs, MsgBuilder.UDP_MTU_DATA_MIN_SIZE);
				if(isRight && (hcConnection.isDoneUDPChannelCheck == false)){
					LogManager.log("Done UDP Channel Check by E_TAG_ROOT_UDP_ADDR_REG");
					
					hcConnection.isDoneUDPChannelCheck = true;
				}
				return true;
			}
		});
		
		HCMessage.setMsgLen(hcConnection.oneTagBS, 0);
		sendThread.start();
	}
	
	public final void send(final byte event_type, final String body) {
		L.V = L.WShop ? false : LogManager.log("send [" + event_type + "], message body : " + body);
		
		synchronized (sendLock) {
			if(connectionRebuilder.isEnterBuildWaitNewConnection){
				connectionRebuilder.waitBeforeSend();
			}
			hcConnection.sendImpl(event_type, body, cmStatus);//注意：与下段重建连接重发，同步
		}
	}
	
	public final void sendWrap(final byte ctrlTag, final byte[] jcip_bs, final int offset, final int len) {
		synchronized (sendLock) {
			if(connectionRebuilder.isEnterBuildWaitNewConnection){
				connectionRebuilder.waitBeforeSend();
			}
			hcConnection.sendWrapActionImpl(ctrlTag, jcip_bs, offset, len, cmStatus);//注意：与下段重建连接重发，同步
		}
	}
	
	public final void send(final byte ctrlTag, byte[] bsModi, final int data_len) {
		synchronized (sendLock) {
			if(connectionRebuilder.isEnterBuildWaitNewConnection){
				connectionRebuilder.waitBeforeSend();
			}
			hcConnection.sendImpl(ctrlTag, bsModi, data_len, cmStatus);//注意：与下段重建连接重发，同步
		}
	}
	
	public final void send(final byte ctrlTag){
		synchronized (sendLock) {
			if(connectionRebuilder.isEnterBuildWaitNewConnection){
				connectionRebuilder.waitBeforeSend();
			}
			hcConnection.sendImpl(ctrlTag);//注意：与下段重建连接重发，同步
		}
	}
	
	public final void send(OutputStream os, final byte ctrlTag, final byte subTag){
		synchronized (sendLock) {
			if(connectionRebuilder.isEnterBuildWaitNewConnection){
				connectionRebuilder.waitBeforeSend();
			}
			hcConnection.sendImpl(os, ctrlTag, subTag);//注意：与下段重建连接重发，同步
		}
	}
	
	public final void sendWrapWithoutLockForKeepAliveOnly(final byte ctrlTag, final byte[] jcip_bs, final int offset, final int len) {
//		synchronized (sendLock) {//大数据在发送时，外围层并没释放本锁，尽管内层间隔性释放。
//			if(connectionRebuilder.isEnterBuildWaitNewConnection){
//				connectionRebuilder.waitBeforeSend();
//			}
			hcConnection.sendWrapActionImpl(ctrlTag, jcip_bs, offset, len, cmStatus);//注意：与下段重建连接重发，同步
//		}
	}
	
	public final void sendWithoutLockForKeepAliveOnly(OutputStream os, final byte ctrlTag, final byte subTag){//因为可能在传送大数据时，keepAlive
		hcConnection.sendImpl(os, ctrlTag, subTag);
	}

	final Thread sendThread = new Thread(){
		public final void run(){
			byte[] bs;
			int cmdLen = -1;
			byte[] screenIDBS = null;
			while(true){
				synchronized (this) {
					bs = (byte[])sendBSBuffer.getFirst();
					if(bs != null){
						cmdLen = ((Integer)sendBSLenBuffer.getFirst()).intValue();
						screenIDBS = (byte[])screenIDBuffer.getFirst();
					}else{
						cmdLen = -1;
					}
				}
				
				if(cmdLen >= 0){
					sendMobileUIEventToServer(bs, 0, cmdLen, screenIDBS);
					cache.cycle(bs);
					continue;
				}
				
				synchronized (this) {
					if(isExit){
						break;
					}
					try {
						this.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
	};
	
	public final void sendMobileUIEventToBackServer(final byte[] cmdBS, final int offset, final int cmdLen,
			final byte[] screenIDBS){
		final byte[] bs = cache.getFree(cmdLen);
		System.arraycopy(cmdBS, offset, bs, 0, cmdLen);
		
		synchronized (sendThread) {
			sendBSBuffer.addTail(bs);
			sendBSLenBuffer.addTail(new Integer(cmdLen));
			screenIDBuffer.addTail(screenIDBS);
			sendThread.notify();
		}
	}

	public final void sendMobileUIEventToServer(final byte[] cmdBS, final int offset, final int cmdLen,
			final byte[] screenIDBS){
		final int idLen = screenIDBS.length;
		final int toServerScreenIDStoreIdx = 1;
		final int maxLen = cmdLen + idLen + toServerScreenIDStoreIdx;
		synchronized (sendBSBuffer) {
			if(toServerBS.length < maxLen){
				toServerBS = new byte[maxLen<1024?1024:maxLen];
			}
			
			toServerBS[0] = ByteUtil.integerToOneByte(idLen);
			System.arraycopy(screenIDBS, 0, toServerBS, toServerScreenIDStoreIdx, idLen);

			final int cmdStoreIdx = toServerScreenIDStoreIdx + idLen;
			System.arraycopy(cmdBS, offset, toServerBS, cmdStoreIdx, cmdLen);
			
			sendWrap(MsgBuilder.E_JS_EVENT_TO_SERVER, toServerBS, 0, cmdStoreIdx + cmdLen);
		}
	}
	
	public boolean isInLimitThread(){
		return false;
	}
	
	private boolean isExit = false;

	public final void shutDown(final CoreSession coreSS){
		if(isExit){
			return;
		}
		isExit = true;

		synchronized (sendThread) {
			sendThread.notify();
		}
		
		setStatus(ContextManager.STATUS_EXIT);
		
		coreSS.hcConnection.shutdownEncryptor();
		
		if(coreSS.hcConnection.rServer != null){
			coreSS.hcConnection.rServer.shutDown();
		}
		
		if(coreSS.hcConnection.udpReceivServer != null){
			coreSS.hcConnection.udpReceivServer.shutDown();
		}
		
		//手机端可能证书没更新，需要重新连接，所以此处不能exit
//		if(isServerSide == false){
//			exit();				
//		}
	}

	public final void startAllServers(final CoreSession coreSS) {
		//注意：SendServer应最先启动，ReceiverServer应最后启动，与流执行的次序相反。
		coreSS.hcConnection.rServer.start();
		if(coreSS.hcConnection.udpReceivServer.isAlive() == false){//手机端登录时，服务器正忙，导致重连
			coreSS.hcConnection.udpReceivServer.start();
		}
	}

	
	public static final int ERROR = 1;
	public static final int WARN = 2;
	public static final int INFO = 3;
	public static final int ALARM = 4;
	public static final int CONFIRMATION = 5;
	public static final int TIP = 6;
	
	public static final int OK = 1010;
	public static final int CANCEL = 1018;
	public static final int EXIT = 1011;
	public static final int SAVE = 1012;
	
	public static final short BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS = 1;
	public static final short BIZ_SERVER_AFTER_PWD_ERROR = 2;
	public static final short BIZ_SERVER_AFTER_CERTKEY_ERROR = 3;
	public static final short BIZ_SERVER_AFTER_SERVICE_IS_FULL = 4;
	public static final short BIZ_SERVER_AFTER_UNKNOW_STATUS = 5;
//	/**
//	 * @deprecated
//	 * 本逻辑统一移到ServerConfig.p_MIN_MOBI_VER_REQUIRED_BY_PC
//	 */
//	public static final short BIZ_SERVER_AFTER_OLD_MOBI_VER_STATUS = 6;
	public static final short BIZ_AFTER_HOLE = 7;
	public static final short BIZ_LOAD_SERVER_CONFIG = 8;
	public static final short BIZ_UPLOAD_LINE_ON = 9;
	public static final short BIZ_NEW_NOTIFICATION = 10;
	public static final short BIZ_VERSION_MID_OR_PC = 11;
	public static final short BIZ_CHANGE_RELAY = 12;
	public static final short BIZ_PLAYSOUND = 13;
	public static final short BIZ_SET_TRAY_ENABLE = 14;
	public static final short BIZ_GET_TOKEN = 15;
	//服务器绑定的最低手机端版本要求
	/**
	 * @deprecated
	 */
	public static final short BIZ_GET_REQ_MOBI_VER_FROM_PC = 16;
	public static final short BIZ_SERVER_LINEOFF = 17;
	public static final short BIZ_NOTIFY_MOBI_IN_LOCK = 18;
	public static final short BIZ_NOTIFY_SERVER_IN_DIRECT_MODE = 19;
	public static final short BIZ_FORBID_UPDATE_CERT = 20;
	public static final short BIZ_MOVING_SCREEN_TIP = 21;
	public static final short BIZ_MOBI_FAIL_CONN = 22;
	public static final short BIZ_I18N_KEY = 23;
	public static final short BIZ_CTRL_BTN_TXT = 24;
	public static final short BIZ_MOBILE_AGENT = 25;
	public static final short BIZ_GET_FORBID_UPDATE_CERT_I18N = 26;//服务器、客户端重复该配置值
	public static final short BIZ_START_WATCH_KEEPALIVE_FOR_RECALL_LINEOFF = 27;
	public static final short BIZ_VIBRATE = 28;
//	public static final short BIZ_REPORT_EXCEPTION = 29;//已废弃
	public static final short BIZ_DATA_CHECK_ERROR = 30;
	public static final short BIZ_ASSISTANT_SPEAK = 31;
	public static final short BIZ_SERVER_BUSY = 32;//手机登录时，获得的状态为占线状态。占线状态不作未来考虑，故关闭。参见BIZ_SERVER_LINEOFF
	public static final short BIZ_SERVER_ACCOUNT_BUSY = 33;
	public static final short BIZ_UPDATE_ONE_TIME_KEYS_IN_CHANNEL = 34;
	public static final short BIZ_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL = 35;
//	public static final short BIZ_MATCHED_FOR_CLIENT_ON_RELAY = 36;
	public static final short BIZ_SHOW_ONCE_SAME_ID = 37;
	public static final short BIZ_VOICE = 38;
	public static final short BIZ_CLIENT_RELOGIN = 39;

	public abstract WiFiDeviceManager getWiFiDeviceManager();
	public abstract void exit();
	public abstract void notifyShutdown();
	public abstract void run();
	public abstract void displayMessage(boolean isFromServerAlertMsg, String caption, String text, int type, Object imageData, int timeOut);

	public abstract Object getSysImg();
	
	public abstract Object doExtBiz(short bizNo, Object newParam);

	public abstract boolean isSoundOnMode();
    	
	public abstract void interrupt(Thread thread);
	
	public abstract Object getProperty(Object propertyID);
	
	public abstract void notifyStreamReceiverBuilder(final boolean isInputStream, final String className, final int streamID, final byte[] bs, final int offset, final int len);
	
	public final FastSender getFastSender(){
		final IContext ictx = this;
		return new FastSender() {
			public final void sendWrapAction(final byte ctrlTag, final byte[] jcip_bs, final int offset, final int len) {
				ictx.sendWrap(ctrlTag, jcip_bs, offset, len);
			}
		};
	}

}
