package hc.core;

import hc.core.sip.ISIPContext;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.IEncrypter;
import hc.core.util.IHCURLAction;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.core.util.WiFiDeviceManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class IContext {
	final boolean isServerSide = IConstant.serverSide;
	public int workingFactor = CUtil.getInitFactor();
	private IStatusListen statusListen;
	private short modeStatus = ContextManager.MODE_CONNECTION_NONE;
	public short cmStatus;
	public IEncrypter userEncryptor = loadEncryptor();
	protected final EventCenter eventCenter;
	public boolean isSecondCertKeyError = false;
	public byte[] SERVER_READY_TO_CHECK;
	public final RootTagEventHCListener rootTagListener = new RootTagEventHCListener();

	private boolean hasReceiveUncheckCert = false;

	public final void receiveUncheckCert(){
		hasReceiveUncheckCert = true;
	}
	
	public final boolean hasReceiveUncheckCert(){
		return hasReceiveUncheckCert;
	}
	
	public final void resetReceiveUncheckCert(){
		hasReceiveUncheckCert = false;
	}

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
	
	public final synchronized void setStatus(final short newCmStatus){
		if(newCmStatus == cmStatus){
			return;
		}
		
		final short oldCmStatus = cmStatus;
		
		cmStatus = newCmStatus;

		if((newCmStatus != ContextManager.STATUS_EXIT) && (oldCmStatus == ContextManager.STATUS_EXIT)){
			L.V = L.O ? false : LogManager.log("forbid change status from [" + oldCmStatus + "] to [" + newCmStatus + "]");
			return;
		}

		hc.core.L.V=hc.core.L.O?false:LogManager.log("Change Status, From [" + oldCmStatus + "] to [" + newCmStatus + "]");
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

			//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Do biz after Hole");

			//激活KeepAlive hctimer
			doExtBiz(IContext.BIZ_AFTER_HOLE, null);

			//			if(IConstant.serverSide){
			//			}else{
			//			}
		}

		//		if(mode == ContextManager.STATUS_SERVER_SELF){
		//			ContextManager.getContextInstance().doExtBiz(IContext.BIZ_IS_ON_SERVICE);
		//		}

		//		if(mode == ContextManager.STATUS_READY_TO_LINE_ON){
		//			hc.core.L.V=hc.core.L.O?false:LogManager.log("NO biz for status READY_TO_LINE_ON");
		//		}

		//		if(ContextManager.isClientStatus()){
		//		}

		//		if(ContextManager.isClientStatus() && ContextManager.isNotWorkingStatus() == false){
		//			ScreenClientManager.init();
		//		}
	}
	
	private final ByteArrayCacher cache = ByteUtil.byteArrayCacher;
	private final LinkedSet sendBSBuffer = new LinkedSet();
	private final LinkedSet sendBSLenBuffer = new LinkedSet();
	private final LinkedSet screenIDBuffer = new LinkedSet();
	private byte[] toServerBS = new byte[1024];
	
	private final byte splitPackageSubTag = 0;
	public final CoreSession coreSS;
	
	public IContext(final CoreSession coreSocketSession, final EventCenter eventCenter){
		coreSocketSession.context = this;
		this.coreSS = coreSocketSession;
		this.eventCenter = eventCenter;
		
		//发送UDP_ADDRESS_REG包，进行转发器的地址注册，供正常数据转发时所需之地址
		eventCenter.addListener(new IEventHCListener() {
			public final byte getEventTag() {
				return MsgBuilder.E_TAG_ROOT_UDP_ADDR_REG;
			}
			
			public final boolean action(byte[] bs, final CoreSession coreSS) {
				//有可能收到，有可能收不到。不作任何处理。仅供UDP中继之用
//				L.V = L.O ? false : LogManager.log("Receive E_TAG_ROOT_UDP_ADDR_REG");
				final boolean isRight = UDPPacketResender.checkUDPBlockData(bs, MsgBuilder.UDP_MTU_DATA_MIN_SIZE);
				if(isRight && (coreSS.context.isDoneUDPChannelCheck == false)){
					L.V = L.O ? false : LogManager.log("Done UDP Channel Check by E_TAG_ROOT_UDP_ADDR_REG");
					
					coreSS.context.isDoneUDPChannelCheck = true;
				}
				return true;
			}
		});
		
		HCMessage.setMsgLen(oneTagBS, 0);
		sendThread.start();
	}
	
	final Thread sendThread = new Thread(){
		public final void run(){
			byte[] bs;
			int cmdLen = -1;
			byte[] screenIDBS = null;
			while(isExit == false){
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
	
	InputStream udpInputStream;
	OutputStream udpOutputStream;
	
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
		
		if(userEncryptor != null){
			try{
				userEncryptor.notifyExit(!IConstant.serverSide);
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
		
		if(coreSS.rServer != null){
			coreSS.rServer.shutDown();
		}
		
		if(coreSS.udpReceivServer != null){
			coreSS.udpReceivServer.shutDown();
		}
		
		if(isServerSide == false){
			exit();				
		}
	}

	public final void startAllServers(final CoreSession coreSS) {
		//注意：SendServer应最先启动，ReceiverServer应最后启动，与流执行的次序相反。
		coreSS.rServer.start();
		if(coreSS.udpReceivServer.isAlive() == false){//手机端登录时，服务器正忙，导致重连
			coreSS.udpReceivServer.start();
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
	/**
	 * @deprecated
	 * 本逻辑统一移到ServerConfig.p_MIN_MOBI_VER_REQUIRED_BY_PC
	 */
	public static final short BIZ_SERVER_AFTER_OLD_MOBI_VER_STATUS = 6;
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
	
	public abstract WiFiDeviceManager getWiFiDeviceManager();
	public abstract void exit();
	public abstract void notifyShutdown();
	public abstract void run();
	public abstract void displayMessage(String caption, String text, int type, Object imageData, int timeOut);

	public abstract Object getSysImg();
	
	public abstract Object doExtBiz(short bizNo, Object newParam);

	public abstract boolean isSoundOnMode();
    	
	public abstract void interrupt(Thread thread);
	
	public abstract Object getProperty(Object propertyID);
	
	public abstract void notifyStreamReceiverBuilder(final boolean isInputStream, final String className, final int streamID, final byte[] bs, final int offset, final int len);
	
	public final void send(final byte event_type, final String body) {
		try {
			final byte[] jcip_bs = body.getBytes(IConstant.UTF_8);
			sendWrap(event_type, jcip_bs, 0, jcip_bs.length);
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		
	}
	
	private final Object BIGLOCK = new Object();
	private final Object LOCK = new Object();
	
	private final boolean isInWorkshop = L.isInWorkshop;
	
	public final boolean isUsingUDPAtMobile(){
		return (IConstant.serverSide == false) && isBuildedUPDChannel && isDoneUDPChannelCheck;
	}
	
	/**
	 * 复制数据块到发送块的载荷段
	 * @param event_type
	 * @param jcip_bs
	 * @param offset
	 * @param len
	 */
	public final void sendWrap(final byte ctrlTag, final byte[] jcip_bs, final int offset, final int len) {
		if(isServerSide){
//			if(ctrlTag == MsgBuilder.E_TRANS_NEW_CERT_KEY 
//					|| ctrlTag == MsgBuilder.E_TRANS_NEW_CERT_KEY_IN_SECU_CHANNEL){
				CCoreUtil.checkAccess();
//			}
		}
		sendWrapAction(ctrlTag, jcip_bs, offset, len);
	}
	
	public final FastSender getFastSender(){
		final IContext ictx = this;
		return new FastSender() {
			public final void sendWrapAction(final byte ctrlTag, final byte[] jcip_bs, final int offset, final int len) {
				ictx.sendWrapAction(ctrlTag, jcip_bs, offset, len);
			}
		};
	}
	
	private boolean isCheckOn;
	private final short checkBitLen = MsgBuilder.CHECK_BIT_NUM;
	private byte checkTotal;
	private byte checkAND;
	private byte checkMINUS;
	
	public final void setCheck(final boolean isCheckOn){
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("set CheckDataIntegrity : " + isCheckOn);
		}
		this.isCheckOn = isCheckOn;
		if(isCheckOn){
			checkTotal = 0;
			checkAND = 0;
			checkMINUS = 0;
		}
		
		ReceiveServer rs = ContextManager.getReceiveServer(coreSS);
		if(rs != null){
			rs.setCheck(isCheckOn);
		}
	}

	private final void sendWrapAction(final byte ctrlTag, final byte[] jcip_bs, final int offset, final int len) {
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
				&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, MsgBuilder.NULL_CTRL_SUB_TAG, jcip_bs, offset, len, 0, false);
			return;
		}
		
		final int minSize = len + MsgBuilder.MIN_LEN_MSG;
		
		if(minSize > MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT){//大消息块
			synchronized (BIGLOCK) {
				if(bigMsgBlobBS.length < MsgBuilder.MAX_LEN_TCP_PACKAGE_BLOCK_BUF){
					bigMsgBlobBS = new byte[MsgBuilder.MAX_LEN_TCP_PACKAGE_BLOCK_BUF];//分配更大处理内存，由于TCP_PACKAGE_SPLIT_EXT_BUF_SIZE，所以不checkBitLen
				}
				
				final byte[] bs = bigMsgBlobBS;
				
				if(++tcp_package_split_next_id > MAX_ID_TCP_PACKAGE_SPLIT){
					tcp_package_split_next_id = 1;//重置块号计数器
				}
				
				try{
					int leftLen = len;
					int splitIdx = offset;
					int totalPackageNum = len / MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT;
					if(totalPackageNum * MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT < len){
						totalPackageNum++;
					}
					while(leftLen > 0) {
						final int eachLen = leftLen>MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT?MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT:leftLen;
						System.arraycopy(jcip_bs, splitIdx, bs, MsgBuilder.TCP_SPLIT_STORE_IDX, eachLen);
						HCMessage.setMsgTcpSplitCtrlData(bs, MsgBuilder.INDEX_MSG_DATA, ctrlTag, splitPackageSubTag, tcp_package_split_next_id, totalPackageNum);
						final int splitPackageLen = eachLen + MsgBuilder.LEN_TCP_PACKAGE_SPLIT_DATA_BLOCK_LEN;
						HCMessage.setMsgLen(bs, splitPackageLen);
						bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_PACKAGE_SPLIT_TCP;
						bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = 0;//因为大消息（big msg）会占用此位，所以要重置。
					
		    			//因为有可能大数据占用过多时间，导致keepalive不能发送数据，每次循环加锁
						if(isInWorkshop){
							if(outStream == null){
								return;
							}
						}
						splitIdx += eachLen;
						leftLen -= eachLen;
						final int splitSendLen = MsgBuilder.TCP_SPLIT_STORE_IDX + eachLen;
						int sendWithCheckLen = splitSendLen;
						synchronized (outStream) {
							if(isCheckOn){//不需要检查dataLen
								sendWithCheckLen += checkBitLen;
								{
									final byte oneByte = bs[0];//INDEX_CTRL_SUB_TAG可能不被使用，而存在脏数据
									checkTotal += oneByte;
									checkAND ^= checkTotal;
									checkAND += oneByte;
									checkMINUS ^= checkTotal;
									checkMINUS -= oneByte;
								}
								for (int i = 2; i < splitSendLen; i++) {
									final byte oneByte = bs[i];
									checkTotal += oneByte;
									checkAND ^= checkTotal;
									checkAND += oneByte;
									checkMINUS ^= checkTotal;
									checkMINUS -= oneByte;
								}
								bs[splitSendLen] = checkAND;
								bs[splitSendLen + 1] = checkMINUS;
							}
							
//							L.V = L.O ? false : LogManager.log("dataLen : " + (splitSendLen - MsgBuilder.INDEX_MSG_DATA) + ", data : " + ByteUtil.toHex(bs, 0, sendWithCheckLen));
							
				    		//加密
				    		//			    L.V = L.O ? false : LogManager.log("Xor len:" + eachLen);
			    			CUtil.superXor(this, coreSS.OneTimeCertKey, bs, MsgBuilder.TCP_SPLIT_STORE_IDX, eachLen, null, true, true);//考虑前段数据较长，不用加密更为安全，所以不从TCP_SPLIT_STORE_IDX开始加密

//    					    hc.core.L.V=hc.core.L.O?false:LogManager.log("Send BIGMSG split ID : " + tcp_package_split_next_id + "[" + ctrlTag + "], len:" + eachLen);
							outStream.write(bs, 0, sendWithCheckLen);
							if(leftLen <= 0){
								outStream.flush();
							}
						}
					}
				}catch (final Exception e) {
					if(L.isInWorkshop){
						LogManager.errToLog("[workshop] Error sendWrapAction(bigData)");
						ExceptionReporter.printStackTrace(e);
					}
				}
			}
		}else{//普通大小消息块
			final int minSizeAndCheckLen = minSize + checkBitLen;
			synchronized (LOCK) {
				if(blobBS.length < minSizeAndCheckLen){
					blobBS = new byte[minSizeAndCheckLen];
				}
				
				final byte[] bs = blobBS;
				
	//			L.V = L.O ? false : LogManager.log("sendWrap blobBS.length:" + blobBS.length + ", jcip_bs.length:" + jcip_bs.length + ", offset:" + offset + ", len:" + len);
				HCMessage.setMsgBody(bs, MsgBuilder.INDEX_MSG_DATA, jcip_bs, offset, len);
				bs[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;
				
				try{
					if(isInWorkshop && outStream == null){
						return;
					}
					
					if(L.isInWorkshop){
						hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], len:" + len + ", isCheckOn : " + isCheckOn);
					}
					
					int sendWithCheckLen = minSize;
					synchronized (outStream) {
						if(isCheckOn && len > 0){
							sendWithCheckLen += checkBitLen;

							{
								final byte oneByte = bs[0];//INDEX_CTRL_SUB_TAG可能不被使用，而存在脏数据
								checkTotal += oneByte;
								checkAND ^= checkTotal;
								checkAND += oneByte;
								checkMINUS ^= checkTotal;
								checkMINUS -= oneByte;
							}
							for (int i = 2; i < minSize; i++) {
								final byte oneByte = bs[i];
								checkTotal += oneByte;
								checkAND ^= checkTotal;
								checkAND += oneByte;
								checkMINUS ^= checkTotal;
								checkMINUS -= oneByte;
							}
							bs[minSize] = checkAND;
							bs[minSize + 1] = checkMINUS;
						}

//						L.V = L.O ? false : LogManager.log("dataLen : " + len + ", data : " + ByteUtil.toHex(bs, 0, sendWithCheckLen));
						
						if(len == 0 || ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN){
				    	}else{
				    		//加密
				    		//			    L.V = L.O ? false : LogManager.log("Xor len:" + len);
			    			CUtil.superXor(this, coreSS.OneTimeCertKey, bs, MsgBuilder.INDEX_MSG_DATA, len, null, true, true);
				    	}

						outStream.write(bs, 0, sendWithCheckLen);
						outStream.flush();
					}
				}catch (final Throwable e) {
					if(CUtil.ONE_TIME_CERT_KEY_IS_NULL.equals(e.getMessage())){//e.getMessage()有可能为null
					}else{
						if(L.isInWorkshop){
							LogManager.errToLog("[workshop] Error sendWrapAction");
							e.printStackTrace();
						}
					}
	//				ExceptionReporter.printStackTrace(e);
	//				SIPManager.notifyRelineon(false);
	//				L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
				}
			}
		}
	}
	
	public void reset(final CoreSession coreSS){
		coreSS.udpReceivServer.setUdpServerSocket(null);

		isDoneUDPChannelCheck = false;
		isBuildedUPDChannel = false;
	}
	
	public boolean isDoneUDPChannelCheck = false;
	public boolean isBuildedUPDChannel = false;
	
	public UDPPacketResender udpSender = null;
	
	/**
	 * 仅限发送控制短数据。
	 * @param ctrlTag
	 * @param bsModi 数据会被加密进程混淆、修改
	 * @param data_len
	 */
	public final void send(final byte ctrlTag, byte[] bsModi, final int data_len) {
			if(isBuildedUPDChannel && isDoneUDPChannelCheck
					&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, bsModi[MsgBuilder.INDEX_CTRL_SUB_TAG], bsModi, MsgBuilder.INDEX_MSG_DATA, data_len, 0, false);
			return;
		}

		HCMessage.setMsgLen(bsModi, data_len);
		
		bsModi[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;
		boolean isNeedRecyle = false;
		
		try{
			final int sendLenWithoutCheck = data_len + MsgBuilder.INDEX_MSG_DATA;
			int sendWithCheckLen = sendLenWithoutCheck;
			final boolean isCheck = isCheckOn && data_len > 0;
			if(isCheck){
				sendWithCheckLen += checkBitLen;
				
				if(bsModi.length < sendWithCheckLen){
					byte[] cycleBS = cache.getFree(sendWithCheckLen);
					System.arraycopy(bsModi, 0, cycleBS, 0, sendLenWithoutCheck);
					bsModi = cycleBS;
					isNeedRecyle = true;
				}
			}
			
			synchronized (outStream) {
				if(isCheck){
					{
						final byte oneByte = bsModi[0];//INDEX_CTRL_SUB_TAG可能不被使用，而存在脏数据
						checkTotal += oneByte;
						checkAND ^= checkTotal;
						checkAND += oneByte;
						checkMINUS ^= checkTotal;
						checkMINUS -= oneByte;
					}
					for (int i = 2; i < sendLenWithoutCheck; i++) {
						final byte oneByte = bsModi[i];
						checkTotal += oneByte;
						checkAND ^= checkTotal;
						checkAND += oneByte;
						checkMINUS ^= checkTotal;
						checkMINUS -= oneByte;
					}
					bsModi[sendLenWithoutCheck] = checkAND;
					bsModi[sendLenWithoutCheck + 1] = checkMINUS;
				}
				
//				L.V = L.O ? false : LogManager.log("dataLen : " + data_len + ", data : " + ByteUtil.toHex(bs, 0, sendWithCheckLen));
				
				if(ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN || data_len == 0){
		    	}else{
		    		//加密
//		    		L.V = L.O ? false : LogManager.log("Xor len:" + data_len);
		    		CUtil.superXor(this, coreSS.OneTimeCertKey, bsModi, MsgBuilder.INDEX_MSG_DATA, data_len, null, true, true);
		    	}

//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], len:" + data_len);
				outStream.write(bsModi, 0, sendWithCheckLen);
				outStream.flush();
			}//end synchronized
			
			if(isNeedRecyle){
				cache.cycle(bsModi);
			}
		} catch (final Exception e) {
//			ExceptionReporter.printStackTrace(e);
			L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
			//因为reset重连时，有可能outStream为空，所以，异常问题由ReceiveServer来处理，不在本处调用。
//			SIPManager.notifyRelineon(false);
		}
	}

	final byte[] oneTagBS = new byte[MsgBuilder.MIN_LEN_MSG];
	final byte[] zeroLenbs = new byte[MsgBuilder.MIN_LEN_MSG];

	byte[] bigMsgBlobBS = new byte[0];
	byte[] blobBS = new byte[40 * 1024];
	int tcp_package_split_next_id = 1;
	private final int MAX_ID_TCP_PACKAGE_SPLIT = 1 << 23;
	
	public final void send(final byte ctrlTag){
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
				&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, MsgBuilder.NULL_CTRL_SUB_TAG, oneTagBS, MsgBuilder.MIN_LEN_MSG, 0, 0, false);
			return;
		}
	    
		synchronized (oneTagBS) {
			oneTagBS[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;

			try {
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], len:" + 0);
				synchronized (outStream) {
					outStream.write(oneTagBS, 0, MsgBuilder.MIN_LEN_MSG);
					outStream.flush();
				}
			} catch (final IOException e) {
//				ExceptionReporter.printStackTrace(e);
				L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
//				SIPManager.notifyRelineon(false);
			}
		}
	}

	/**
	 * 仅限发送控制短数据
	 * @param os
	 * @param ctrlTag
	 * @param subTag
	 */
	public final void send(OutputStream os, final byte ctrlTag, final byte subTag){
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
				&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, subTag, zeroLenbs, MsgBuilder.MIN_LEN_MSG, 0, 0, false);
			return;
		}

		synchronized (zeroLenbs) {
			zeroLenbs[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;
			zeroLenbs[MsgBuilder.INDEX_CTRL_SUB_TAG] = subTag;
		    
			if(os == null){
				os = outStream;
			}
			try {
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], subTage:" + subTag);
				synchronized (os) {
					os.write(zeroLenbs, 0, MsgBuilder.MIN_LEN_MSG);
					os.flush();
				}
			} catch (final IOException e) {//不能拦截os为null的异常，因为KeepaliveManager.java保活需要此异常
//				ExceptionReporter.printStackTrace(e);
				L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
//				SIPManager.notifyRelineon(false);
			}
		}
	}

	public final void setOutputStream(final ISIPContext sipCtx, final Object tcpOrUDPSocket) throws Exception{
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("Changed Send Socket");
		this.outStream = (tcpOrUDPSocket==null)?null:sipCtx.getOutputStream(tcpOrUDPSocket);
	}

	private DataOutputStream outStream;

	public final Object getOutputStreamLockObject() {
		CCoreUtil.checkAccess();
		
		return outStream;
	}

	public final IEncrypter getUserEncryptor(){
		return userEncryptor;
	}

	public final IEncrypter loadEncryptor(){
		final String encryptClass = getEncryptorClass();
		if(encryptClass != null){
			try {
				final Class c = Class.forName(encryptClass);
				final IEncrypter en = (IEncrypter)c.newInstance();
				en.setUUID(IConstant.getUUIDBS());
				en.setPassword(IConstant.getPasswordBS());
				en.initEncrypter(!IConstant.serverSide);
				
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Enable user Encryptor [" + encryptClass + "]");
				
				userEncryptor = en;
				return en;
			} catch (final Exception e) {
				LogManager.err("Error Load Encryptor [" + encryptClass + "]");
				ExceptionReporter.printStackTrace(e);
				userEncryptor = null;
			}
		}else{
//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Disable user Encryptor");
		}
		return null;
	}

	public static String getEncryptorClass() {
		CCoreUtil.checkAccess();
		
		return (String)IConstant.getInstance().getObject("encryptClass");
	}

	public final void resetCheck() {
		resetReceiveUncheckCert();
		if(SERVER_READY_TO_CHECK != null){
			SERVER_READY_TO_CHECK = null;
		}
		isSecondCertKeyError = false;
	}
}
