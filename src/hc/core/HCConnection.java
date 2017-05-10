package hc.core;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import hc.core.sip.IPAndPort;
import hc.core.sip.ISIPContext;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.IEncrypter;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;
import hc.core.util.XorPackage;

public final class HCConnection {
	private static long globalConnectID = 1;
	private static final Object hcConnectionClassLock = new Object();
	
	private final boolean isInWorkshop = L.isInWorkshop;
	
	private static long getConnectionID(){
		synchronized (hcConnectionClassLock) {
			return globalConnectID++;
		}
	}
	
	public final void setOnRelay(final boolean isRelay){
		sipContext.isOnRelay = isRelay;
	}
	
	public final long connectionID = getConnectionID();
	private final ByteArrayCacher cache = ByteUtil.byteArrayCacher;
	public int workingFactor = CUtil.getInitFactor();
	public final byte[] udpHeader = new byte[MsgBuilder.LEN_UDP_HEADER];
	public ISIPContext sipContext;
	public ReceiveServer rServer;
	public UDPReceiveServer udpReceivServer;
	public byte[] userPassword = clonePwd();

	public final byte[] clonePwd() {
		final byte[] passwordBS = IConstant.getPasswordBS();
		if(passwordBS == null){
			return null;
		}
		return ByteUtil.cloneBS(passwordBS);
	}
	public IEncrypter userEncryptor = loadEncryptor(userPassword);
	public byte[] OneTimeCertKey;
	public boolean isInitialCloseReceiveForJ2ME = false;
	public IPAndPort relayIpPort = new IPAndPort();
	public boolean isStartLineOffProcess;

	public final ConnectionRebuilder connectionRebuilder = new ConnectionRebuilder();
	long xorPackageID = 0;
	
	public byte[] package_tcp_bs;
	public int package_tcp_id;
	public int package_tcp_last_store_idx = MsgBuilder.INDEX_MSG_DATA;
	public int packaeg_tcp_num;
	public int packaeg_tcp_appended_num;
	
	//------------------以下是KeepAlive段------------------
	//不能初始为0，极端初次条件下可能认为，长时无接收。
	public long receiveMS = System.currentTimeMillis();
	public long sendLineMS;
	public boolean isSendLive = false;
	public long startTime ;

	public Object updateOneTimeKeysRunnable;
	public final Object oneTimeReceiveNotifyLock = new Object();
	
	private final byte splitPackageSubTag = 0;
	public boolean isSecondCertKeyError = false;
	public byte[] SERVER_READY_TO_CHECK;
	
	private boolean hasReceiveUncheckCert = false;
	public byte[] random_for_server;

	public final void receiveUncheckCert(){
		hasReceiveUncheckCert = true;
	}
	
	private UDPController udpController;
	
	public final UDPController getUDPController(){
		synchronized (this) {
			if(udpController == null){
				udpController = new UDPController();
			}
			return udpController;
		}
	}
	
	public final ReceiveServer getReceiveServer() {
		return rServer;
	}

	public final UDPReceiveServer getUDPReceiveServer() {
		return udpReceivServer;
	}

	public final void setReceiver(final ReceiveServer rs, final UDPReceiveServer udpRS){
		rServer = rs;
		udpReceivServer = udpRS;
	}
	
	public final void setUDPChannelPort(final int udpPort){
		relayIpPort.udpPort = udpPort;
	}
	
	//获得远程中继的UDP控制器端口
	public final int getUDPControllerPort(){
		//注意与NIOServer生成时，保持一致
		return relayIpPort.port - 1;
	}
	
	public final boolean hasReceiveUncheckCert(){
		return hasReceiveUncheckCert;
	}
	
	public final void resetReceiveUncheckCert(){
		hasReceiveUncheckCert = false;
	}
	
	//-------------------UpdateOneTime-------------------
	public boolean isStopRunning = false;
	public byte[] oneTime = new byte[CCoreUtil.CERT_KEY_LEN];
	public int updateMinMinutes = RootConfig.getInstance().getIntProperty(RootConfig.p_UpdateOneTimeMinMinutes);
	public boolean isReceivedOneTimeInSecuChannalFromMobile = false;
	
	public final boolean isUsingUDPAtMobile(){
		return (IConstant.serverSide == false) && isBuildedUPDChannel && isDoneUDPChannelCheck;
	}
	
	final void sendImpl(final byte event_type, final String body, final short cmStatus) {
		try {
			final byte[] jcip_bs = body.getBytes(IConstant.UTF_8);
			sendWrapActionImpl(event_type, jcip_bs, 0, jcip_bs.length, cmStatus);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	private final Object BIGLOCK = new Object();
	private final Object LOCK = new Object();
	
	public boolean isCheckOn;
	public final short checkBitLen = MsgBuilder.EXT_BYTE_NUM;
	public byte checkTotal;
	public byte checkAND;
	public byte checkMINUS;
	
	public final void setCheck(final boolean isCheckOn){
		if(isInWorkshop){
			LogManager.log("set CheckDataIntegrity : " + isCheckOn);
		}
		this.isCheckOn = isCheckOn;
		if(isCheckOn){
			checkTotal = 0;
			checkAND = 0;
			checkMINUS = 0;
		}
		
		if(rServer != null){
			rServer.setCheck(isCheckOn);
		}
	}
	
	public final void setOutputStream(final Object tcpOrUDPSocket) {
		if(tcpOrUDPSocket != null){
			L.V = L.WShop ? false : LogManager.log("[Chang] HCConnection Send outputStream : " + tcpOrUDPSocket.hashCode());
		}
		
		this.outStream = (DataOutputStream)tcpOrUDPSocket;
	}

	private DataOutputStream outStream;

	public final Object getOutputStream() {
		CCoreUtil.checkAccess();
		
		return outStream;
	}
	
	public HCConnection(){//注意：此实例被用作KeepAlive和UpdateOneTime的锁
	}
	
	public final void sendWrapActionImpl(final byte ctrlTag, final byte[] jcip_bs, final int offset, final int len, final short cmStatus) {
		Exception hasException = null;
		
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
					
					if(isInWorkshop){
						LogManager.log("Send [" + ctrlTag + "], len:" + eachLen + ", isCheckOn : " + isCheckOn + " from " + outStream.hashCode());
					}
					
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
							
							ByteUtil.longToEightBytes(++xorPackageID, bs, splitSendLen + 2);
						}
						
//							LogManager.log("dataLen : " + (splitSendLen - MsgBuilder.INDEX_MSG_DATA) + ", data : " + ByteUtil.toHex(bs, 0, sendWithCheckLen));
						
			    		//加密
			    		//			    LogManager.log("Xor len:" + eachLen);
		    			CUtil.superXor(this, OneTimeCertKey, bs, MsgBuilder.TCP_SPLIT_STORE_IDX, eachLen, null, true, true);//考虑前段数据较长，不用加密更为安全，所以不从TCP_SPLIT_STORE_IDX开始加密

//    					    LogManager.log("Send BIGMSG split ID : " + tcp_package_split_next_id + "[" + ctrlTag + "], len:" + eachLen);
						try{
							outStream.write(bs, 0, sendWithCheckLen);
							if(leftLen <= 0){
								outStream.flush();
							}
						}catch (Exception e) {
							hasException = e;
						}
						
						if(isCheckOn){
							cloneXorPackage(bs, sendWithCheckLen, xorPackageID);
						}
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
				
	//			LogManager.log("sendWrap blobBS.length:" + blobBS.length + ", jcip_bs.length:" + jcip_bs.length + ", offset:" + offset + ", len:" + len);
				HCMessage.setMsgBody(bs, MsgBuilder.INDEX_MSG_DATA, jcip_bs, offset, len);
				bs[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;
				
				if(isInWorkshop && outStream == null){
					return;
				}
				
				if(isInWorkshop){
					LogManager.log("Send [" + ctrlTag + "], len:" + len + ", isCheckOn : " + isCheckOn + " from " + outStream.hashCode());
				}
				
				int sendWithCheckLen = minSize;
				synchronized (outStream) {
					final boolean isCheck = isCheckOn && len > 0;
					final boolean isXor = !(len == 0 || ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN);

					if(isCheck){
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
						
						if(isXor){
							ByteUtil.longToEightBytes(++xorPackageID, bs, minSize + 2);
						}
					}

//						LogManager.log("dataLen : " + len + ", data : " + ByteUtil.toHex(bs, 0, sendWithCheckLen));
					
					if(isXor){
			    		//加密
			    		//			    LogManager.log("Xor len:" + len);
		    			CUtil.superXor(this, OneTimeCertKey, bs, MsgBuilder.INDEX_MSG_DATA, len, null, true, true);
			    	}

					try{
						outStream.write(bs, 0, sendWithCheckLen);
						outStream.flush();
					}catch (Exception e) {
						hasException = e;
					}
					
					if(isCheck && isXor){
						cloneXorPackage(bs, sendWithCheckLen, xorPackageID);
					}
				}
			}
		}
		
		if(hasException != null){
			if(isInWorkshop){
				LogManager.errToLog("==============>send had exception!");
			}
			if(CUtil.ONE_TIME_CERT_KEY_IS_NULL.equals(hasException.getMessage())){//e.getMessage()有可能为null
				LogManager.errToLog(CUtil.ONE_TIME_CERT_KEY_IS_NULL);
				return;
			}else if(isInWorkshop){
				LogManager.errToLog("[workshop] Error sendWrapAction(bigData)");
				ExceptionReporter.printStackTrace(hasException);
			}
			connectionRebuilder.notifyBuildNewConnection(true, cmStatus);
		}
	}
	

	public void reset(){
		udpReceivServer.setUdpServerSocket(null);

		isDoneUDPChannelCheck = false;
		isBuildedUPDChannel = false;
	}
	
	public boolean isDoneUDPChannelCheck = false;
	public boolean isBuildedUPDChannel = false;
	
	public UDPPacketResender udpSender = null;
	private final LinkedSet xorPackageSet = new LinkedSet();
	
	public final boolean ackXorPackage(final long xorPackageID){
		synchronized (xorPackageSet) {
			XorPackage xp;
			while((xp = (XorPackage)xorPackageSet.getFirst()) != null){
				final int len = xp.len;
				final long xpID = ByteUtil.eightBytesToLong(xp.bs, len - MsgBuilder.XOR_PACKAGE_ID_LEN);
				if(xpID < xorPackageID){
					cache.cycle(xp.bs);
					XorPackage.cycle(xp);
					continue;
				}else if(xpID == xorPackageID){
					cache.cycle(xp.bs);
					XorPackage.cycle(xp);
					return true;
				}else{
					L.V = L.WShop ? false : LogManager.log("ack XorPackage ID : " + xpID + ", should ID : " + xorPackageID);
					return false;
				}
			}
		}
		
		L.V = L.WShop ? false : LogManager.log("no ack XorPackage in set, should ID : " + xorPackageID);
		return false;
	}
	
	public final void resendUnReachablePackage(){
		synchronized (xorPackageSet) {
			final Vector v = xorPackageSet.toVector();
			final int size = v.size();
			for (int i = 0; i < size; i++) {
				final XorPackage xp = (XorPackage)v.elementAt(i);
				L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] re-send XorPackage ID : " + xp.packageID + " in resendUnReachablePackage.");
				try{
					outStream.write(xp.bs, 0, xp.len);
					outStream.flush();
				}catch (Exception ex) {
				}
			}
		}
	}
	
	private final void cloneXorPackage(final byte[] bs, final int len, final long packageID){
		final XorPackage xp = XorPackage.getFree();
		
		final byte[] cpBS = cache.getFree(len);
		System.arraycopy(bs, 0, cpBS, 0, len);
		
		xp.bs = cpBS;
		xp.len = len;
		xp.packageID = packageID;

		synchronized (xorPackageSet) {
			xorPackageSet.addTail(xp);
		}
	}
	
	public final void release(){
		connectionRebuilder.isReleased = true;
		
		synchronized (xorPackageSet) {
			XorPackage xp;
			while((xp = (XorPackage)xorPackageSet.getFirst()) != null){
				cache.cycle(xp.bs);
				XorPackage.cycle(xp);
			}
		}
		if(rServer != null){
			rServer.shutDown();
		}
		HCTimer.remove(sipContext.resender.resenderTimer);
		HCTimer.remove(sipContext.ackbatchTimer);
	}
	
	private Object inputStreamForNullRServer;
	
	public final void setReceiveServerInputStream(final Object inputStream, final boolean isShutDown, final boolean isCloseOld){
		if(inputStream != null){
			L.V = L.WShop ? false : LogManager.log("[Chang] Receive inputStream :" + inputStream.hashCode());
		}
		
		if(rServer != null){
			if(isShutDown){
				rServer.shutDown();
			}
			rServer.setUdpServerSocket(inputStream, isCloseOld);
		}else{
			inputStreamForNullRServer = inputStream;//重建连接，没有ReceiveServer，所以为null
		}
	}
	
	public final Object getReceiveServerInputStream(){
		if(rServer != null){
			return rServer.dataInputStream;
		}else{
			return inputStreamForNullRServer;
		}
	}
	
	/**
	 * 仅限发送控制短数据。
	 * @param ctrlTag
	 * @param bsModi 数据会被加密进程混淆、修改
	 * @param data_len
	 */
	public final void sendImpl(final byte ctrlTag, byte[] bsModi, final int data_len, final short cmStatus) {
		Exception hasException = null;
		
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
					&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, bsModi[MsgBuilder.INDEX_CTRL_SUB_TAG], bsModi, MsgBuilder.INDEX_MSG_DATA, data_len, 0, false);
			return;
		}

		HCMessage.setMsgLen(bsModi, data_len);
		
		bsModi[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;
		boolean isNeedRecyle = false;
		
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
		
		if(isInWorkshop){
			LogManager.log("Send [" + ctrlTag + "], len:" + data_len + ", isCheckOn : " + isCheck + " from " + outStream.hashCode());
		}
		
		synchronized (outStream) {
			final boolean isXor = ! (ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN || data_len == 0);

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
				
				if(isXor){
					ByteUtil.longToEightBytes(++xorPackageID, bsModi, sendLenWithoutCheck + 2);
				}
			}
			
//				LogManager.log("dataLen : " + data_len + ", data : " + ByteUtil.toHex(bs, 0, sendWithCheckLen));
			
			if(isXor){
	    		//加密
//		    		LogManager.log("Xor len:" + data_len);
	    		CUtil.superXor(this, OneTimeCertKey, bsModi, MsgBuilder.INDEX_MSG_DATA, data_len, null, true, true);
	    	}

//			LogManager.log("Send [" + ctrlTag + "], len:" + data_len);
			try{
				outStream.write(bsModi, 0, sendWithCheckLen);
				outStream.flush();
			}catch (Exception e) {
				hasException = e;
			}
			
			if(isCheck && isXor){
				cloneXorPackage(bsModi, sendWithCheckLen, xorPackageID);
			}
		}//end synchronized
		
		if(isNeedRecyle){
			cache.cycle(bsModi);
		}
		
		if(hasException != null){
			if(isInWorkshop){
				LogManager.errToLog("==============>send had exception!");
			}
			connectionRebuilder.notifyBuildNewConnection(true, cmStatus);
		}
	}

	final byte[] oneTagBS = new byte[MsgBuilder.MIN_LEN_MSG];
	final byte[] zeroLenbs = new byte[MsgBuilder.MIN_LEN_MSG];

	byte[] bigMsgBlobBS = new byte[0];
	byte[] blobBS = new byte[40 * 1024];
	int tcp_package_split_next_id = 1;
	private final int MAX_ID_TCP_PACKAGE_SPLIT = 1 << 23;
	
	final void sendImpl(final byte ctrlTag) {
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
				&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, MsgBuilder.NULL_CTRL_SUB_TAG, oneTagBS, MsgBuilder.MIN_LEN_MSG, 0, 0, false);
			return;
		}
	    
		if(isInWorkshop){
			LogManager.log("Send [" + ctrlTag + "], len:" + 0 + ", isCheckOn : false from " + outStream.hashCode());
		}
		
		synchronized (oneTagBS) {
			oneTagBS[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;

//				LogManager.log("Send [" + ctrlTag + "], len:" + 0);
			synchronized (outStream) {
				try{
					outStream.write(oneTagBS, 0, MsgBuilder.MIN_LEN_MSG);
					outStream.flush();
				}catch (Exception e) {
					LogManager.log("Exception:" + e.getMessage());
//					connectionRebuilder.buildNewConnection(true, cmStatus);
				}
			}
		}
	}

	/**
	 * 仅限发送控制短数据
	 * @param os
	 * @param ctrlTag
	 * @param subTag
	 */
	final void sendImpl(OutputStream os, final byte ctrlTag, final byte subTag) {//不能拦截os为null的异常，因为KeepaliveManager.java保活需要此异常
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
			
			if(isInWorkshop){
				LogManager.log("Send [" + ctrlTag + "], len:" + 0 + ", isCheckOn : false from " + os.hashCode());
			}
			
//				LogManager.log("Send [" + ctrlTag + "], subTage:" + subTag);
			synchronized (os) {
				try{
					os.write(zeroLenbs, 0, MsgBuilder.MIN_LEN_MSG);
					os.flush();
				}catch (Exception e) {
					LogManager.log("Exception:" + e.getMessage());
//					connectionRebuilder.buildNewConnection(true, cmStatus);
				}
			}
		}
	}

	public final IEncrypter loadEncryptor(final byte[] pwdBS){
		final String encryptClass = getEncryptorClass();
		if(encryptClass != null){
			try {
				final Class c = IConstant.serverSide?(Class)RootBuilder.getInstance().doBiz(RootBuilder.ROOT_GET_CLASS_FROM_3RD_AND_SERV_LIBS, encryptClass):Class.forName(encryptClass);
				final IEncrypter en = (IEncrypter)c.newInstance();
				en.setUUID(IConstant.getUUIDBS());
				en.setPassword(pwdBS);
				en.initEncrypter(!IConstant.serverSide);
				
//				LogManager.log("Enable user Encryptor [" + encryptClass + "]");
				
				userEncryptor = en;
				return en;
			} catch (final Throwable e) {
				LogManager.err("Error Load Encryptor [" + encryptClass + "]");
				ExceptionReporter.printStackTrace(e);
				userEncryptor = null;
			}
		}else{
//			LogManager.log("Disable user Encryptor");
		}
		return null;
	}
	
	public final void shutdownEncryptor(){
		if(userEncryptor != null){
			try{
				userEncryptor.notifyExit(!IConstant.serverSide);
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	public final IEncrypter getUserEncryptor(){
		return userEncryptor;
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
