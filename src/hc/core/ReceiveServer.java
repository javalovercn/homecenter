package hc.core;

import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.CUtil;
import hc.core.util.EventBackCacher;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;
import hc.core.util.ThreadPriorityManager;

import java.io.DataInputStream;

public class ReceiveServer implements Runnable{
	DataInputStream dataInputStream;
	public static final ByteArrayCacher recBytesCacher = ByteUtil.byteArrayCacher;
    public Thread thread;
    final CoreSession coreSocketSession;
    final static boolean isEnableTestRebuildConn = isEnableTestRebuildConnection();//限客户端
    boolean isSimuRetransError = false;
    private long last_rebuild_swap_sock_ms;
    private boolean isExchangeStatus = false;
    private boolean isRelayModeSendSlow = true;
    private final SenderSlowCounter sendSlowCounter;
    
	public ReceiveServer(final CoreSession coreSocketSession){
		thread = new Thread(this);//"Receive Server"
		thread.setPriority(ThreadPriorityManager.DATA_TRANS_PRIORITY);
		this.coreSocketSession = coreSocketSession;
		this.sendSlowCounter = coreSocketSession.hcConnection.sendSlowPackageCounter;
        //J2ME不支持setName
		//thread.setName("Receive Server");
    }
	
	private static boolean isEnableTestRebuildConnection() {
		return IConstant.serverSide == false 
	    		&& IConstant.TRUE.equals(RootBuilder.getInstance().doBiz(RootBuilder.ROOT_GET_MB_PROP, "isEnableTestRebuildConn"));
	}
	
	public void start(){
		if(thread != null && thread.isAlive() == false){//手机端登录时，服务器正忙，导致重连
			thread.start();
		}
	}
    
	private final Object LOCK = new Object();
	
//	private int readFullyShort(final DataInputStream in, final byte b[], final int off, final int len) throws Exception {
//		int c = 0;
//		int index = off;
//		final int endIdx = off + len;
//		while (index < endIdx) {
//			b[index++] = (byte)in.read();
//		}
//		return len;
//	}
//	private void readFully(final DataInputStream in, final byte b[], final int off, final int len) throws Exception {
//		LogManager.log("try new read " + len + "...");
//		if(IConstant.serverSide == false){
//		if(len > 1447){
//			int n = 0;
//		    int readOnceCount = 0;
//			while (n < len) {
//			    while((readOnceCount = in.available()) == 0){
//			    	Thread.sleep(50);
//			    }
//			    
//	//		    if(readOnceCount < 0){
//	//		    	LogManager.log("ReadBuff available:" + readOnceCount);
//	//		    	throw new Exception();
//	//		    }
//			    
//	    		final int bestReadNum = len - n;
////	    		final int realReadedNum = in.read(b, off + n, bestReadNum>readOnceCount?readOnceCount:bestReadNum);
//	    		LogManager.log("readOnceCount : " + readOnceCount);
//	    		final int realReadedNum = readFullyShort(in, b, off + n, bestReadNum>readOnceCount?readOnceCount:bestReadNum);
//				n += realReadedNum;
//			    
//	//			LogManager.log("receiveFull readOnce:" + realReadedNum + ", readed:" + n + ", targetNum:" + len);
//			}
//		}else{
////			readFullyShort(in, b, off, len);
//			in.readFully(b, off, len);
//		}
//		}else{
//			in.readFully(b, off, len);
//		}
//	}
	
	private long readyReceiveXorPackageID = 1;

	static final Exception overflowException = new Exception("Overflow or Error data, maybe clientReset Error");
	static final Exception checkException = new Exception("fail on check integrity of package data");
	String threadID;
	long lastRetransXorPackageMS;
	
	public void run(){
		threadID = Thread.currentThread().toString();
		
		byte checkTotal = 0;
		byte checkAND = 0;
		byte checkMINUS = 0;
		
		final short checkBitLen = MsgBuilder.EXT_BYTE_NUM;
		final byte[] ackXorPackageID = new byte[MsgBuilder.XOR_PACKAGE_ID_LEN];
		
		int bsDataLen = 1024 * 200 - MsgBuilder.MIN_LEN_MSG;
		byte[] bs = null;
		final EventBackCacher ebCacher = EventBackCacher.getInstance();
		final int WAIT_MODI_STATUS_MS = HCTimer.HC_INTERNAL_MS + 100;
		final boolean isInWorkshop = L.isInWorkshop;
		final HCConnection hcConnection = coreSocketSession.hcConnection;
		
		byte ctrlTag = 0;
		boolean isXor = false;
		
    	while (!isShutdown) {
			if(dataInputStream == null){
	    		synchronized (LOCK) {
	    			if(isShutdown){
	    				break;
	    			}
	    			if(dataInputStream == null){
	    				try {
							LOCK.wait();
						} catch (final Exception e) {
//							ExceptionReporter.printStackTrace(e);
						}
						continue;
	    			}
				}
			}
            try {
            	isXor = false;
            	bs = recBytesCacher.getFree(bsDataLen);
            	
            	if(isEnableTestRebuildConn){
            		if(coreSocketSession.context.cmStatus == ContextManager.STATUS_CLIENT_SELF){
	            		final long currMS = System.currentTimeMillis();
	            		if(currMS - last_rebuild_swap_sock_ms > 1000 * 30){
	            			L.V = L.WShop ? false : LogManager.log("[ReceiveServer] start test close connection and rebuild/renewal.");
	            			coreSocketSession.closeSocket(coreSocketSession.getSocket());
	            			throw overflowException;//模拟断线
	            		}
	            	}
            	}
            	
				if(isInWorkshop){
					LogManager.log("[ReceiveServer]  inputStream : [" + dataInputStream.hashCode() + "] ready receive in ReceiveServer : " + threadID);
				}
				
            	dataInputStream.readFully(bs, 0, MsgBuilder.MIN_LEN_MSG);
//				LogManager.log("Receive head len:" + len);
				
				ctrlTag = bs[MsgBuilder.INDEX_CTRL_TAG];
				
				final int temp0 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
				final int temp1 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
				final int temp2 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;
				final int dataLen = ((temp0 << 16) + (temp1 << 8) + temp2);
				
				isXor = !(dataLen == 0 || ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN);
				final int dataLenWithCheckLen = isXor?(dataLen+checkBitLen):dataLen;

				if(isInWorkshop){
					LogManager.log("[ReceiveServer]  [" + dataInputStream.hashCode() + "]:" + threadID + " Receive One packet [" + ctrlTag + "][" + bs[MsgBuilder.INDEX_CTRL_SUB_TAG] + "], data len : " + dataLen);
				}
				
				//有可能因ClientReset而导致收到不完整包，产生虚假大数据
				if(dataLen > MsgBuilder.MAX_LEN_TCP_PACKAGE_BLOCK_BUF){
					throw overflowException;
				}
				
				if(bsDataLen < dataLenWithCheckLen){
					bsDataLen = dataLenWithCheckLen;
					final byte[] temp = recBytesCacher.getFree(dataLenWithCheckLen + MsgBuilder.MIN_LEN_MSG);
					for (int i = 0; i < MsgBuilder.MIN_LEN_MSG; i++) {
						temp[i] = bs[i];
					}
					recBytesCacher.cycle(bs);
					bs = temp;
				}
				
				//以下段不用用 bs 变量，因为上行已发生变更可能。
				if(dataLenWithCheckLen > 0){
					dataInputStream.readFully(bs, MsgBuilder.MIN_LEN_MSG, dataLenWithCheckLen);
				}
				
				//先解密再进行check
				if(ctrlTag == MsgBuilder.E_PACKAGE_SPLIT_TCP){
					final int eachLen = dataLen - MsgBuilder.LEN_TCP_PACKAGE_SPLIT_DATA_BLOCK_LEN;
					CUtil.superXor(hcConnection, hcConnection.OneTimeCertKey, bs, MsgBuilder.TCP_SPLIT_STORE_IDX, eachLen, null, false, true);
				}else{
					if(ctrlTag == MsgBuilder.E_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL){
						//优先提交密码更新
						coreSocketSession.context.doExtBiz(IContext.BIZ_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL, null);
					}
					if(isXor == false){
			    	}else{
			    		if(hcConnection.OneTimeCertKey == null){
			    			int sleepTotal = 0;
			    			while(hcConnection.OneTimeCertKey == null && sleepTotal < 3000){//Android模拟环境下600偏小
			    				try{
			    					sleepTotal += ThreadPriorityManager.UI_WAIT_OTHER_THREAD_EXEC_MS;
			    					Thread.sleep(ThreadPriorityManager.UI_WAIT_OTHER_THREAD_EXEC_MS);
			    				}catch (Exception e) {
			    				}
			    			}
			    		}
			    		//解密
			    		CUtil.superXor(hcConnection, hcConnection.OneTimeCertKey, bs, MsgBuilder.INDEX_MSG_DATA, dataLen, null, false, true);
			    	}
				}
				
				//检查check
				if(isXor){
					final int dataCheckLen = dataLen + MsgBuilder.MIN_LEN_MSG;
					final int xorPackgeIdxOff = dataCheckLen + 2;

					boolean isAddNextXorPackageID = false;
					//检查是否为重发包
					final long realXorPackageID = ByteUtil.eightBytesToLong(bs, xorPackgeIdxOff);
//						final boolean isForceXorError = isSimuRetransError == false && isEnableTestRebuildConn && ((realXorPackageID % 50) == 0);
//						if(isForceXorError == false && readyReceiveXorPackageID == realXorPackageID){
					if(realXorPackageID == readyReceiveXorPackageID){
						if(isInWorkshop){
							LogManager.log("[ReceiveServer] received XorPackageID : " + realXorPackageID + ", ready to check...");
						}
						isAddNextXorPackageID = true;
						readyReceiveXorPackageID++;
					}else if(realXorPackageID < readyReceiveXorPackageID){
						//为重发包
						recBytesCacher.cycle(bs);
						if(isInWorkshop){
							LogManager.log("[ReceiveServer] skip received XorPackageID : " + realXorPackageID + ", expected XorPackageID : " + readyReceiveXorPackageID);
						}
//							doFailCheck(false);//不需通知重发
						continue;
					}else{//大于时，出现断包
						recBytesCacher.cycle(bs);
						if(isInWorkshop){
							LogManager.log("[ReceiveServer] skip received XorPackageID : " + realXorPackageID + ", expected XorPackageID : " + readyReceiveXorPackageID);
						}
						doFailCheck(false);
						continue;
					}
				
					byte realCheckTotal = checkTotal, realCheckAnd = checkAND, realCheckMinus = checkMINUS;
					{
						final byte oneByte = bs[0];
						realCheckTotal += oneByte;
						realCheckAnd ^= realCheckTotal;
						realCheckAnd += oneByte;
						realCheckMinus ^= realCheckTotal;
						realCheckMinus -= oneByte;
					}
				
//						LogManager.log("dataLen : " + dataLen + ", data : " + ByteUtil.toHex(bs, 0, dataCheckLen + checkBitLen));
					
					for (int i = 2; i < dataCheckLen; i++) {
						final byte oneByte = bs[i];
						realCheckTotal += oneByte;
						realCheckAnd ^= realCheckTotal;
						realCheckAnd += oneByte;
						realCheckMinus ^= realCheckTotal;
						realCheckMinus -= oneByte;
					}
				
					if(realCheckAnd == bs[dataCheckLen] && realCheckMinus == bs[dataCheckLen + 1]){
//						LogManager.log("pass check num!");
						checkTotal = realCheckTotal;
						checkAND = realCheckAnd;
						checkMINUS = realCheckMinus;
						
						lastRetransXorPackageMS = 0;
						
						if(isInWorkshop){
							LogManager.log("[ReceiveServer] success check integrity xor : " + (readyReceiveXorPackageID - 1));
						}
							
						System.arraycopy(bs, xorPackgeIdxOff, ackXorPackageID, 0, MsgBuilder.XOR_PACKAGE_ID_LEN);
						coreSocketSession.context.sendWrapWithoutLockForKeepAliveOnly(MsgBuilder.E_ACK_XOR_PACKAGE_ID, 
							ackXorPackageID, 0, MsgBuilder.XOR_PACKAGE_ID_LEN);//注意：必须走明文且impl层
					}else{//fail on check
//						LogManager.errToLog("check idx : " + dataCheckLen + ", real : " + realCheckAnd + "" + realCheckMinus + ", expected : " + bs[dataCheckLen] + "" + bs[dataCheckLen + 1]);
						recBytesCacher.cycle(bs);
						doFailCheck(isAddNextXorPackageID);
						continue;
					}
				}//end isXor

//				if(ctrlTag == MsgBuilder.E_PACKAGE_SPLIT_TCP){
//					LogManager.log("[workshop] skip E_PACKAGE_SPLIT_TCP");
//					recBytesCacher.cycle(bs);
//					continue;
//				}
				
				if(isNeverReceivedAfterNewConn){
					isNeverReceivedAfterNewConn = false;
				}
				
				boolean isBigMsg = false;
				
				if(ctrlTag == MsgBuilder.E_PACKAGE_SPLIT_TCP){
					isBigMsg = true;
					
					//TCP合并包
					final int newPackageID = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_TCP_SPLIT_SUB_GROUP_ID);
					if(hcConnection.package_tcp_id != 0 && hcConnection.package_tcp_id != newPackageID){
						LogManager.errToLog("[ReceiveServer] invalid TCP sub package id : " + newPackageID + ", expected id : " + hcConnection.package_tcp_id);
						resetForNextBigData(hcConnection);
//								cancel();
						continue;
					}
					if(hcConnection.package_tcp_id == 0){
						hcConnection.package_tcp_id = newPackageID;
						hcConnection.packaeg_tcp_num = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_TCP_SPLIT_SUB_GROUP_NUM);
						if(isInWorkshop){
							System.out.println("[ReceiveServer] ----[Big Msg]-----package tcp id : " + newPackageID + ", num : " + hcConnection.packaeg_tcp_num);
						}
						hcConnection.package_tcp_bs = new byte[MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT * hcConnection.packaeg_tcp_num + MsgBuilder.TCP_PACKAGE_SPLIT_EXT_BUF_SIZE];
						hcConnection.packaeg_tcp_appended_num = 0;
						hcConnection.package_tcp_last_store_idx = MsgBuilder.INDEX_MSG_DATA;
						
						for (int i = 0; i < MsgBuilder.INDEX_MSG_DATA; i++) {
							hcConnection.package_tcp_bs[i] = bs[i];
						}
						
						hcConnection.package_tcp_bs[MsgBuilder.INDEX_CTRL_TAG] = bs[MsgBuilder.INDEX_TCP_SPLIT_TAG];
						hcConnection.package_tcp_bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = bs[MsgBuilder.INDEX_TCP_SPLIT_SUB_TAG];
					}
					
					final int eachLen = dataLen - MsgBuilder.LEN_TCP_PACKAGE_SPLIT_DATA_BLOCK_LEN;
					System.arraycopy(bs, MsgBuilder.TCP_SPLIT_STORE_IDX, hcConnection.package_tcp_bs, hcConnection.package_tcp_last_store_idx, eachLen);
					hcConnection.package_tcp_last_store_idx += eachLen;

					if(isInWorkshop){
						System.out.println("[ReceiveServer] ----[Big Msg]-----append data tcp id : " + newPackageID + ", num : " + (hcConnection.packaeg_tcp_appended_num + 1) + ", curr len : " + eachLen);
					}
					
					if(++hcConnection.packaeg_tcp_appended_num == hcConnection.packaeg_tcp_num){
						HCMessage.setBigMsgLen(hcConnection.package_tcp_bs, hcConnection.package_tcp_last_store_idx - MsgBuilder.INDEX_MSG_DATA);//还原数据块总长度
						final byte[] snap_bs = hcConnection.package_tcp_bs;
						resetForNextBigData(hcConnection);//先执行，以下下块逻辑可能产生异常
						
						ctrlTag = snap_bs[MsgBuilder.INDEX_CTRL_TAG];//可能后续需要直接处理，比如E_STREAM_DATA
						bs = snap_bs;
						
						if(isInWorkshop){
							LogManager.log("[ReceiveServer] [" + dataInputStream.hashCode() + "]:" + threadID + " Receive One packet [" + ctrlTag + "][" + bs[MsgBuilder.INDEX_CTRL_SUB_TAG] + "], data len : " + dataLen);
						}

					}else{
//						cancel();//释放当前块
						continue;
					}
				}
				
//				LogManager.log("Receive data len:" + dataLen);
				if(ctrlTag == MsgBuilder.E_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL){
					coreSocketSession.context.doExtBiz(IContext.BIZ_UPDATE_ONE_TIME_KEYS_IN_CHANNEL, bs);
					if(isBigMsg == false){
						recBytesCacher.cycle(bs);
					}
					continue;
				}else if(ctrlTag == MsgBuilder.E_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL){
//					优先以完成，所以此处不作
//					coreSocketSession.context.doExtBiz(IContext.BIZ_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL, bs);
					if(isBigMsg == false){
						recBytesCacher.cycle(bs);
					}
					continue;
				}else if(ctrlTag == MsgBuilder.E_TAG_ROOT){//服务器客户端都走E_TAG_ROOT捷径 isClient && 
					//由于大数据可能导致过载，所以此处直接处理。
					coreSocketSession.context.rootTagListener.action(bs, coreSocketSession, hcConnection);
					if(isBigMsg == false){
						recBytesCacher.cycle(bs);
					}
					continue;
				}else if(ctrlTag == MsgBuilder.E_RE_TRANS_XOR_PACKAGE){
					L.V = L.WShop ? false : LogManager.log("[ReceiveServer] receive E_RE_TRANS_XOR_PACKAGE, resend UnReachable packages!!!");
					coreSocketSession.hcConnection.resendUnReachablePackage();
					continue;
				}else if(ctrlTag == MsgBuilder.E_ACK_XOR_PACKAGE_ID){
					coreSocketSession.hcConnection.ackXorPackage(bs, coreSocketSession);
					if(isRelayModeSendSlow){
						sendSlowCounter.minusOne();
					}
					if(isBigMsg == false){
						recBytesCacher.cycle(bs);
					}
					continue;
				}else if(ctrlTag == MsgBuilder.E_GOTO_URL_SUPER_LEVEL){
					HCURLUtil.processGotoUrlForNormalAndSuperLevel(bs, coreSocketSession);
					if(isBigMsg == false){
						recBytesCacher.cycle(bs);
					}
					continue;
				}else if(ctrlTag == MsgBuilder.E_STREAM_MANAGE){//不影响用户或事件分发线程
					coreSocketSession.manageStream(bs);
					if(isBigMsg == false){
						recBytesCacher.cycle(bs);
					}
					continue;
				}else if(ctrlTag == MsgBuilder.E_STREAM_DATA){//不影响用户或事件分发线程
					coreSocketSession.dispatchStream(bs);
					if(isBigMsg == false){
						recBytesCacher.cycle(bs);
					}
					continue;
				}else if(ctrlTag == MsgBuilder.E_SWAP_SOCK_SYN_XOR_PACKAGE_ID){//不影响用户或事件分发线程
					if(ContextManager.isServerStatus(coreSocketSession.context)){//数据交换状态忽略，仅限新连接且未会话。
					}else{
						if(coreSocketSession.context.isServerSide){
							coreSocketSession.synXorPackageID(bs);//仅服务器
							L.V = L.WShop ? false : LogManager.log("[ReceiveServer] shutdown ReceiveServer :" + threadID + " for done swap socket.");
							isShutdown = true;
						}
					}
					if(isBigMsg == false){
						recBytesCacher.cycle(bs);
					}
					continue;
				}else{
					final EventBack eb = ebCacher.getFreeEB();
					eb.setBSAndDatalen(coreSocketSession, null, bs, dataLen);
					
					if(isExchangeStatus == false){
						isExchangeStatus = coreSocketSession.isExchangeStatus();
						if(isExchangeStatus == false){
							L.V = L.WShop ? false : LogManager.log("[ReceiveServer] process event in Receive, because not in exchange status.");
							eb.watch();
							continue;
						}
					}
					
					if(ctrlTag == MsgBuilder.E_JS_EVENT_TO_SERVER){
						coreSocketSession.getJSEventProcessor().addWatcher(eb);//JSEvent会提交执行后，定时检查线程状态，如果完全wait，则终止等待，另开线程执行后续任务。
					}else{
						coreSocketSession.eventCenterDriver.addWatcher(eb);
					}
				}
//				LogManager.log("Finished Receive Biz Action");
            }catch (final Exception e) {
            	if(isInWorkshop){
            		if(dataInputStream != null){
            			LogManager.errToLog("[ReceiveServer] Receive Server [" + dataInputStream.hashCode() + "]:" + threadID + " exception : " + e.toString());
            		}else{
            			LogManager.errToLog("[ReceiveServer] : " + threadID + ", inputStream is null, exception : " + e.toString());
            		}
            	}
            	
				//因为重连手机端,可能导致bs重分配，而导致错误
            	recBytesCacher.cycle(bs);

            	if(isShutdown){
            		continue;
            	}
            	
            	if(hcConnection.isInitialCloseReceiveForJ2ME){
            		//比如：需要返回重新登录
            		LogManager.log("[ReceiveServer] close is initial closed, ready to connection.");
            		continue;
            	}
            	
            	if(System.currentTimeMillis() - receiveUpdateMS < 500){//Android环境下100太小，
            		L.V = L.WShop ? false : LogManager.log("[ReceiveServer] Exception, receive is changing new socket. continue");
//            		try{
//            			Thread.sleep(100);
//            		}catch (Exception ex) {
//					}
            		continue;
            	}
            	
            	final IContext ctx = coreSocketSession.context;
            	final short cmStatus = ctx.cmStatus;
            	if(cmStatus == ContextManager.STATUS_EXIT || cmStatus == ContextManager.STATUS_READY_EXIT){
            		try{
            			Thread.sleep(100);
            		}catch (final Exception ex) {
						
					}
            		continue;
            	}
            	if(hcConnection.sipContext.isNearDeployTime()){
            		LogManager.log("[ReceiveServer] ReceiveServer Exception near deploy time");
            		
            		
//            		if(IConstant.serverSide 
//            				&& (ContextManager.cmStatus == ContextManager.STATUS_NEED_NAT) 
//            				&& (ContextManager.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON)){
//            		}else{
////            			LogManager.log("UDP mode , relineon");
//            			SIPManager.notifyRelineon(true);
//            		}
            		
            		//无线内网接入时，会关闭RelaySocket，并将新本地连接替换，所以会出现此情形，中继模式下测试通过
            		try{
            			Thread.sleep(100);
            		}catch (final Exception ex) {
						
					}
            		continue;
            	}
            	
            	if(e != checkException){
            		if(isNeverReceivedAfterNewConn){
            			isNeverReceivedAfterNewConn = false;
            			continue;
            		}
            		
            		if(System.currentTimeMillis() - last_rebuild_swap_sock_ms > ThreadPriorityManager.REBUILD_SWAP_SOCK_MIN_MS){
	            		L.V = L.WShop ? false : LogManager.log("[ReceiveServer] [ConnectionRebuilder] exception on receiver : " + threadID + ", inputStream : " + dataInputStream.hashCode());
	            		final boolean out = hcConnection.connectionRebuilder.notifyBuildNewConnection(false, cmStatus);//注意：成功后，本receive将复用
	            		if(out){
	        				last_rebuild_swap_sock_ms = System.currentTimeMillis();
	            			L.V = L.WShop ? false : LogManager.log("[ReceiveServer] [ConnectionRebuilder] successful build new connection in ReceiveServer : " + threadID);
	            			continue;
	            		}
	            		LogManager.errToLog("[ReceiveServer] [ConnectionRebuilder] fail to build new connection in ReceiveServer : " + threadID);
            		}else{
            			LogManager.errToLog("[ReceiveServer] [ConnectionRebuilder] last rebuild swap socket is very frenquence, cancel rebuild swap.");
            		}
        		}
            	
            	LogManager.errToLog("[ReceiveServer] Receive Exception:[" + e.getMessage() + "], maybe skip receive.");
            	
            	try{
            		hcConnection.sipContext.deploySocket(coreSocketSession.hcConnection, null);
            	}catch (final Exception ex) {
				}
            	
				if(overflowException == e){
            		//请求关闭
            		LogManager.log("[ReceiveServer] Unvalid data len, stop application");
            		if(hcConnection.isUsingUDPAtMobile() == false){
            			coreSocketSession.notifyLineOff(true, false);
            		}
            	}else{
        			//关闭和sleep要提前到此，因为reset后重连依赖于ContextManager.getStatus，
        			//有可能用户主动下线，则等待相应线程会更新状态后，不需要进行reConnectAfterResetExcep
    				try{
						Thread.sleep(WAIT_MODI_STATUS_MS);
    					//***等待相应线程(如用户主动退出，或切换Relay)会更新状态后，不需要进行reConnectAfterResetExcep***
    				}catch (final Exception ee) {
    				}
    				
        			try{
        				dataInputStream.close();
        			}catch (final Throwable ex) {
					}
        			dataInputStream = null;
					if(hcConnection.isUsingUDPAtMobile() == false){
						coreSocketSession.notifyLineOff(false, false);
					}
            	}
			}
        }//while
    	
    	L.V = L.WShop ? false : LogManager.log("[ReceiveServer] Receiver shutdown " + threadID);
	}
	
	private final void resetForNextBigData(final HCConnection hcConnection) {
		hcConnection.package_tcp_id = 0;
		hcConnection.packaeg_tcp_appended_num = 0;
		hcConnection.package_tcp_bs = null;//释放合并后的块
	}
	
	private final void doFailCheck(final boolean isAddNextXorPackageID) throws Exception {
		final long currMS = System.currentTimeMillis();
		final long diffMS = currMS - lastRetransXorPackageMS;
		if(diffMS < 10000){//最近多个包连续错误
			L.V = L.WShop ? false : LogManager.log("[ReceiveServer] fail on check integrity of package data in next ten seconds!");
			//注意：要置于下段之前
		}else if(diffMS < 15000){
			LogManager.errToLog("[ReceiveServer] fail on check integrity of package data, force close current connection!");
			coreSocketSession.context.doExtBiz(IContext.BIZ_DATA_CHECK_ERROR, null);
			throw checkException;
		}else{//首次出错
			lastRetransXorPackageMS = currMS;
			
			if(isAddNextXorPackageID){
				readyReceiveXorPackageID--;
			}
			L.V = L.WShop ? false : LogManager.log("[ReceiveServer] fail check xorPackage : " + readyReceiveXorPackageID + ", send E_RE_TRANS_XOR_PACKAGE!!!");
			coreSocketSession.context.send(MsgBuilder.E_RE_TRANS_XOR_PACKAGE);
		}
	}
	
	private boolean isShutdown = false;
	
	public void shutDown() {
		L.V = L.WShop ? false : LogManager.log("[ReceiveServer] notify shutdown ReceiveServer threadName : " + threadID);
    	isShutdown = true;
    	synchronized (LOCK) {
			LOCK.notify();
		}
	}

	long receiveUpdateMS;
	boolean isNeverReceivedAfterNewConn;
	
	void setUdpServerSocket(final Object tcpOrUDPsocket, final boolean isCloseOld) {
		isRelayModeSendSlow = coreSocketSession.isOnRelay();
		
		final DataInputStream oldIS = this.dataInputStream;
		if(tcpOrUDPsocket != null){
			L.V = L.WShop ? false : LogManager.log("[ReceiveServer] update socket threadName : " + threadID + " setDataInputStream : " + tcpOrUDPsocket.hashCode());
		}
		this.dataInputStream = (DataInputStream)tcpOrUDPsocket;
		if(dataInputStream != null){
			receiveUpdateMS = System.currentTimeMillis();
			isNeverReceivedAfterNewConn = true;
			last_rebuild_swap_sock_ms = receiveUpdateMS;
			synchronized (LOCK) {
				LOCK.notify();
			}
		}
		if(isCloseOld && oldIS != null){
			try{
				oldIS.close();
			}catch (Exception e) {
			}
		}
	}
}