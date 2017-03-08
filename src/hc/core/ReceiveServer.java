package hc.core;

import hc.core.sip.ISIPContext;
import hc.core.sip.SIPManager;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.CUtil;
import hc.core.util.EventBackCacher;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;
import hc.core.util.ThreadPriorityManager;

import java.io.DataInputStream;

public class ReceiveServer implements Runnable{
	DataInputStream dataInputStream;
	public static final ByteArrayCacher recBytesCacher = ByteUtil.byteArrayCacher;
    public Thread thread;
    CoreSession coreSocketSession;
    final boolean isEnableTestRebuildConn = IConstant.serverSide == false 
    		&& ((Boolean)RootBuilder.getInstance().doBiz(RootBuilder.ROOT_BIZ_IS_SIMU, null)).booleanValue();//限客户端
    long lastBuildConnMS = System.currentTimeMillis();
    
	public ReceiveServer(final CoreSession coreSocketSession){
		thread = new Thread(this);//"Receive Server"
		thread.setPriority(ThreadPriorityManager.DATA_TRANS_PRIORITY);
		this.coreSocketSession = coreSocketSession;
        //J2ME不支持setName
		//thread.setName("Receive Server");
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
	
	private boolean isCheckOn;
	private final short checkBitLen = MsgBuilder.EXT_BYTE_NUM;
	public byte checkTotal;
	public byte checkAND;
	public byte checkMINUS;
	public final byte[] ackXorPackageID = new byte[MsgBuilder.XOR_PACKAGE_ID_LEN];
	private long readyReceiveXorPackageID = 1;
	
	public final void setCheck(final boolean isCheckOn){
		if(L.isInWorkshop){
			LogManager.log("set ReceiveServer CheckDataIntegrity : " + isCheckOn);
		}
		this.isCheckOn = isCheckOn;
		if(isCheckOn){
			checkTotal = 0;
			checkAND = 0;
			checkMINUS = 0;
		}
	}
	
	static final Exception overflowException = new Exception("Overflow or Error data, maybe clientReset Error");
	static final Exception checkException = new Exception("fail on check integrity of package data");
	String threadID;
	
	public void run(){
		threadID = Thread.currentThread().toString();
		
		final int initSize = 2048;
		final int initMaxDataLen = initSize - MsgBuilder.MIN_LEN_MSG;
		byte[] bs = null;
		final EventBackCacher ebCacher = EventBackCacher.getInstance();
		final int WAIT_MODI_STATUS_MS = HCTimer.HC_INTERNAL_MS + 100;
		final boolean isInWorkshop = L.isInWorkshop;
		final HCConnection hcConnection = coreSocketSession.hcConnection;
		
		final byte ackCmStatus = ContextManager.STATUS_CLIENT_SELF;
		
		byte ctrlTag = 0;
		boolean isXor = false;
		
    	while (!isShutdown) {
			if(dataInputStream == null){
	    		synchronized (LOCK) {
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
            	bs = recBytesCacher.getFree(initSize);
            	
            	if(isEnableTestRebuildConn){
            		if(coreSocketSession.context.cmStatus == ContextManager.STATUS_CLIENT_SELF){
	            		final long currMS = System.currentTimeMillis();
	            		if(currMS - lastBuildConnMS > 1000 * 30){
	            			lastBuildConnMS = currMS;
	            			L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] start test close connection and rebuild/renewal.");
	            			final ISIPContext sipContext = hcConnection.sipContext;
	            			sipContext.closeSocket(sipContext.getSocket());
	            			throw overflowException;//模拟断线
	            		}
	            	}
            	}
            	
				if(isInWorkshop){
					LogManager.log("[workshop] inputStream : [" + dataInputStream.hashCode() + "] ready receive in ReceiveServer : " + threadID);
				}
				
            	dataInputStream.readFully(bs, 0, MsgBuilder.MIN_LEN_MSG);
//				LogManager.log("Receive head len:" + len);
				
				ctrlTag = bs[MsgBuilder.INDEX_CTRL_TAG];
				
				final int temp0 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
				final int temp1 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
				final int temp2 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;
				final int dataLen = ((temp0 << 16) + (temp1 << 8) + temp2);
				final boolean isNeedCheck = dataLen > 0 && isCheckOn;
				final int dataLenWithCheckLen = (isNeedCheck?(dataLen+checkBitLen):dataLen);

				if(isInWorkshop){
					LogManager.log("[workshop] [" + dataInputStream.hashCode() + "]:" + threadID + " Receive One packet [" + ctrlTag + "][" + bs[MsgBuilder.INDEX_CTRL_SUB_TAG] + "], data len : " + dataLen);
				}
				
				//有可能因ClientReset而导致收到不完整包，产生虚假大数据
				if(dataLen > MsgBuilder.MAX_LEN_TCP_PACKAGE_BLOCK_BUF){
					throw overflowException;
				}
				
				if(initMaxDataLen < dataLenWithCheckLen){
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
					isXor = true;
				}else{
					if(dataLen == 0 || ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN){
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
			    		isXor = true;
			    	}
				}
				
				//检查check
				if(isNeedCheck){
					final int dataCheckLen = dataLen + MsgBuilder.MIN_LEN_MSG;
					final int xorPackgeIdxOff = dataCheckLen + 2;

					if(isXor){
						//检查是否为重发包
						final long realXorPackageID = ByteUtil.eightBytesToLong(bs, xorPackgeIdxOff);
						if(realXorPackageID < readyReceiveXorPackageID){
							//为重发包
							recBytesCacher.cycle(bs);
							if(isInWorkshop){
								LogManager.log("skip received XorPackageID : " + realXorPackageID + ", expected XorPackageID : " + readyReceiveXorPackageID);
							}
							continue;
						}else if(readyReceiveXorPackageID == realXorPackageID){
							if(isInWorkshop){
								LogManager.log("received XorPackageID : " + realXorPackageID + ", ready to check...");
							}
							readyReceiveXorPackageID++;
						}
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
					
//					LogManager.log("dataLen : " + dataLen + ", data : " + ByteUtil.toHex(bs, 0, dataCheckLen + checkBitLen));
					
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
						
						if(isXor){
							System.arraycopy(bs, xorPackgeIdxOff, ackXorPackageID, 0, MsgBuilder.XOR_PACKAGE_ID_LEN);
							coreSocketSession.hcConnection.sendWrapActionImpl(MsgBuilder.E_ACK_XOR_PACKAGE_ID, 
								ackXorPackageID, 0, MsgBuilder.XOR_PACKAGE_ID_LEN, ackCmStatus);//注意：必须走明文且impl层
						}
					}else{
//						LogManager.errToLog("check idx : " + dataCheckLen + ", real : " + realCheckAnd + "" + realCheckMinus + ", expected : " + bs[dataCheckLen] + "" + bs[dataCheckLen + 1]);
						//fail on check
						LogManager.errToLog("fail on check integrity of package data, force close current connection!");
						coreSocketSession.context.doExtBiz(IContext.BIZ_DATA_CHECK_ERROR, null);
						throw checkException;
					}
				}

//				if(ctrlTag == MsgBuilder.E_PACKAGE_SPLIT_TCP){
//					LogManager.log("[workshop] skip E_PACKAGE_SPLIT_TCP");
//					recBytesCacher.cycle(bs);
//					continue;
//				}
				
				if(isNeverReceivedAfterNewConn){
					isNeverReceivedAfterNewConn = false;
				}
				
//				LogManager.log("Receive data len:" + dataLen);
				if(ctrlTag == MsgBuilder.E_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL){
					coreSocketSession.context.doExtBiz(IContext.BIZ_UPDATE_ONE_TIME_KEYS_IN_CHANNEL, bs);
					recBytesCacher.cycle(bs);
					continue;
				}else if(ctrlTag == MsgBuilder.E_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL){
					coreSocketSession.context.doExtBiz(IContext.BIZ_REPLY_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL, bs);
					recBytesCacher.cycle(bs);
					continue;
				}else if(ctrlTag == MsgBuilder.E_TAG_ROOT){//服务器客户端都走E_TAG_ROOT捷径 isClient && 
					//由于大数据可能导致过载，所以此处直接处理。
					coreSocketSession.context.rootTagListener.action(bs, coreSocketSession);
					recBytesCacher.cycle(bs);
					continue;
				}else{
					final EventBack eb = ebCacher.getFreeEB();
					eb.setBSAndDatalen(coreSocketSession, null, bs, dataLen);
					coreSocketSession.eventCenterDriver.addWatcher(eb);
					
		            if(ctrlTag == MsgBuilder.E_SYN_XOR_PACKAGE_ID){//注意：暂停以供更新的HCConnection先行得到receive，并等待本HCConnection完全进入关闭状态
		            	synchronized (LOCK) {
		            		try {
		            			L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] wait 3000 E_SYN_XOR_PACKAGE_ID in ReceiveServer : " + threadID);
								LOCK.wait(ThreadPriorityManager.NET_MAX_RENEWAL_CONN_MS);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
		        			L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] resume from wait 3000 E_SYN_XOR_PACKAGE_ID in ReceiveServer : " + threadID);
		            	}
		            }
				}
//				LogManager.log("Finished Receive Biz Action");
            }catch (final Exception e) {
            	if(isInWorkshop){
            		LogManager.errToLog("Receive Server [" + dataInputStream.hashCode() + "]:" + threadID + " exception : " + e.toString());
            	}
            	
				//因为重连手机端,可能导致bs重分配，而导致错误
            	recBytesCacher.cycle(bs);

            	if(isShutdown){
            		continue;
            	}
            	
            	if(hcConnection.isInitialCloseReceiveForJ2ME){
            		//比如：需要返回重新登录
            		LogManager.log("close is initial closed, ready to connection.");
            		continue;
            	}
            	
            	if(System.currentTimeMillis() - receiveUpdateMS < 100){
//            		if(L.isInWorkshop){
//            			LogManager.log("[workshop] receive is changing new socket. continue");
//            		}
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
            		LogManager.log("ReceiveServer Exception near deploy time");
            		
            		
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
            		
            		L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] exception on receiver : " + threadID + ", inputStream : " + dataInputStream.hashCode());
            		final boolean out = hcConnection.connectionRebuilder.notifyBuildNewConnection(false, cmStatus);//注意：成功后，本receive将复用
            		if(out){
            			L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] successful build new connection in ReceiveServer : " + threadID);
            			continue;
            		}
            		L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] fail to build new connection in ReceiveServer : " + threadID);
            	}
            	
            	LogManager.errToLog("Receive Exception:[" + e.getMessage() + "], maybe skip receive.");
            	
            	try{
            		hcConnection.sipContext.deploySocket(coreSocketSession.hcConnection, null);
            	}catch (final Exception ex) {
				}
            	
				if(overflowException == e){
            		//请求关闭
            		LogManager.log("Unvalid data len, stop application");
            		if(hcConnection.isUsingUDPAtMobile() == false){
            			SIPManager.notifyLineOff(coreSocketSession, true, false);
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
						SIPManager.notifyLineOff(coreSocketSession, false, false);
					}
            	}
			}
        }//while
    	
    	L.V = L.WShop ? false : LogManager.log("Receiver shutdown " + threadID);
	}
	
	private boolean isShutdown = false;
	
	public void shutDown() {
		L.V = L.WShop ? false : LogManager.log("notify shutdown ReceiveServer.");
    	isShutdown = true;
    	synchronized (LOCK) {
			LOCK.notify();
		}
	}

	long receiveUpdateMS;
	boolean isNeverReceivedAfterNewConn;
	
	void setUdpServerSocket(final Object tcpOrUDPsocket, final boolean isCloseOld) {
		DataInputStream oldIS = this.dataInputStream;
		L.V = L.WShop ? false : LogManager.log("ReceiveServer threadName : " + threadID + " setDataInputStream : " + tcpOrUDPsocket.hashCode());
		this.dataInputStream = (DataInputStream)tcpOrUDPsocket;
		if(dataInputStream != null){
			receiveUpdateMS = System.currentTimeMillis();
			isNeverReceivedAfterNewConn = true;
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