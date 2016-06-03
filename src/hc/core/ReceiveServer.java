package hc.core;

import hc.core.sip.SIPManager;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.EventBackCacher;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;

import java.io.DataInputStream;

public class ReceiveServer implements Runnable{
	DataInputStream dataInputStream;
	public static final ByteArrayCacher recBytesCacher = ByteUtil.byteArrayCacher;
    public Thread thread;
	public ReceiveServer(){
		thread = new Thread(this);//"Receive Server"
		thread.setPriority(ThreadPriorityManager.DATA_TRANS_PRIORITY);
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
//		L.V = L.O ? false : LogManager.log("try new read " + len + "...");
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
//	//		    	L.V = L.O ? false : LogManager.log("ReadBuff available:" + readOnceCount);
//	//		    	throw new Exception();
//	//		    }
//			    
//	    		final int bestReadNum = len - n;
////	    		final int realReadedNum = in.read(b, off + n, bestReadNum>readOnceCount?readOnceCount:bestReadNum);
//	    		L.V = L.O ? false : LogManager.log("readOnceCount : " + readOnceCount);
//	    		final int realReadedNum = readFullyShort(in, b, off + n, bestReadNum>readOnceCount?readOnceCount:bestReadNum);
//				n += realReadedNum;
//			    
//	//			L.V = L.O ? false : LogManager.log("receiveFull readOnce:" + realReadedNum + ", readed:" + n + ", targetNum:" + len);
//			}
//		}else{
////			readFullyShort(in, b, off, len);
//			in.readFully(b, off, len);
//		}
//		}else{
//			in.readFully(b, off, len);
//		}
//	}
	
	public static boolean isInitialCloseReceive = false;
	
	public void run(){
		final int initSize = 2048;
		final int initMaxDataLen = initSize - MsgBuilder.MIN_LEN_MSG;
		byte[] bs = null;
		final EventBackCacher ebCacher = EventBackCacher.getInstance();
		final Exception maxDataLenException = new Exception("Max DataLen, maybe clientReset Error");
		long lastReconnAfterResetMS = System.currentTimeMillis();
		final int WAIT_MODI_STATUS_MS = HCTimer.HC_INTERNAL_MS + 100;
		final boolean isInWorkshop = L.isInWorkshop;
		
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
            	bs = recBytesCacher.getFree(initSize);
            	dataInputStream.readFully(bs, 0, MsgBuilder.MIN_LEN_MSG);
//				L.V = L.O ? false : LogManager.log("Receive head len:" + len);
				
				final byte ctrlTag = bs[MsgBuilder.INDEX_CTRL_TAG];
				
				final int temp0 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
				final int temp1 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
				final int temp2 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;
				final int dataLen = ((temp0 << 16) + (temp1 << 8) + temp2);

				if(isInWorkshop){
					L.V = L.O ? false : LogManager.log("[workshop] Receive One packet [" + ctrlTag + "][" + bs[MsgBuilder.INDEX_CTRL_SUB_TAG] + "], data len : " + dataLen);
				}
				
				//有可能因ClientReset而导致收到不完整包，产生虚假大数据
				if(dataLen > MsgBuilder.MAX_LEN_TCP_PACKAGE_BLOCK_BUF){
					throw maxDataLenException;
				}
				
				if(initMaxDataLen < dataLen){
					final byte[] temp = recBytesCacher.getFree(dataLen + MsgBuilder.MIN_LEN_MSG);
					for (int i = 0; i < MsgBuilder.MIN_LEN_MSG; i++) {
						temp[i] = bs[i];
					}
					recBytesCacher.cycle(bs);
					bs = temp;
				}
				
				//以下段不用用 bs 变量，因为上行已发生变更可能。
				if(dataLen > 0){
					dataInputStream.readFully(bs, MsgBuilder.MIN_LEN_MSG, dataLen);
				}

//				if(ctrlTag == MsgBuilder.E_PACKAGE_SPLIT_TCP){
//					L.V = L.O ? false : LogManager.log("[workshop] skip E_PACKAGE_SPLIT_TCP");
//					recBytesCacher.cycle(bs);
//					continue;
//				}
				
//				L.V = L.O ? false : LogManager.log("Receive data len:" + dataLen);
				if(ctrlTag == MsgBuilder.E_TAG_ROOT){//服务器客户端都走E_TAG_ROOT捷径 isClient && 
					//由于大数据可能导致过载，所以此处直接处理。
					ClientInitor.rootTagListener.action(bs);
					recBytesCacher.cycle(bs);
				}else{
					final EventBack eb = ebCacher.getFreeEB();
					eb.setBSAndDatalen(null, bs, dataLen);
					ConditionWatcher.addWatcher(eb);
				}
//				L.V = L.O ? false : LogManager.log("Finished Receive Biz Action");
            }catch (final Exception e) {
				//因为重连手机端,可能导致bs重分配，而导致错误
            	recBytesCacher.cycle(bs);

            	if(isInitialCloseReceive){
            		//比如：需要返回重新登录
            		L.V = L.O ? false : LogManager.log("close is initial closed, ready to connection.");
            		continue;
            	}
            	
            	if(System.currentTimeMillis() - receiveUpdateMS < 100){
            		if(L.isInWorkshop){
            			L.V = L.O ? false : LogManager.log("[workshop] receive is changing new socket. continue");
            		}
            		continue;
            	}
            	
            	final int cmStatus = ContextManager.cmStatus;
            	if(cmStatus == ContextManager.STATUS_EXIT || cmStatus == ContextManager.STATUS_READY_EXIT){
            		try{
            			Thread.sleep(100);
            		}catch (final Exception ex) {
						
					}
            		continue;
            	}
            	if(SIPManager.getSIPContext().isNearDeployTime()){
            		L.V = L.O ? false : LogManager.log("ReceiveServer Exception near deploy time");
            		
            		
//            		if(IConstant.serverSide 
//            				&& (ContextManager.cmStatus == ContextManager.STATUS_NEED_NAT) 
//            				&& (ContextManager.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON)){
//            		}else{
////            			L.V = L.O ? false : LogManager.log("UDP mode , relineon");
//            			SIPManager.notifyRelineon(true);
//            		}
            		
            		//无线内网接入时，会关闭RelaySocket，并将新本地连接替换，所以会出现此情形，中继模式下测试通过
            		try{
            			Thread.sleep(100);
            		}catch (final Exception ex) {
						
					}
            		continue;
            	}
            	
            	LogManager.errToLog("Receive Exception:[" + e.getMessage() + "], maybe skip receive.");
            	
            	try{
            		SIPManager.getSIPContext().deploySocket(null);
            	}catch (final Exception ex) {
				}
            	
				if(maxDataLenException == e){
            		//请求关闭
            		L.V = L.O ? false : LogManager.log("Unvalid data len, stop application");
            		if(ContextManager.getContextInstance().isUsingUDPAtMobile() == false){
            			SIPManager.notifyRelineon(true);
            		}
            	}else{
        			//关闭和sleep要提前到此，因为reset后重连依赖于ContextManager.getStatus，
        			//有可能用户主动下线，则等待相应线程会更新状态后，不需要进行reConnectAfterResetExcep
    				try{
						Thread.sleep(WAIT_MODI_STATUS_MS);
    					//***等待相应线程(如用户主动退出，或切换Relay)会更新状态后，不需要进行reConnectAfterResetExcep***
    				}catch (final Exception ee) {
    					
    				}
            		if(isShutdown == false && (System.currentTimeMillis() - lastReconnAfterResetMS > 2000)//1000稍小，改为2000   
            				&& SIPManager.isOnRelay() && 
            				(    (ContextManager.cmStatus == ContextManager.STATUS_CLIENT_SELF) 
            					||  (ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF))){
						
						//手机端，因网络不稳定导致意外reset，进入重连模式
            			L.V = L.O ? false : LogManager.log("Network reset error, reconnect...");
            			
            			if(IConstant.serverSide == false){
//            				ContextManager.getContextInstance().doExtBiz(IContext.BIZ_MOVING_SCREEN_TIP, 
//            				ContextManager.getContextInstance().doExtBiz(IContext.BIZ_I18N_KEY, "m21"));
	        				ContextManager.getContextInstance().displayMessage(
	        						(String)ContextManager.getContextInstance().doExtBiz(IContext.BIZ_I18N_KEY, String.valueOf(IContext.INFO)), 
	        						(String)ContextManager.getContextInstance().doExtBiz(IContext.BIZ_I18N_KEY, "m21"), 
	        						IContext.INFO, null, 0);
	            			if(ConfigManager.isBackground()){
	            				ContextManager.getContextInstance().doExtBiz(IContext.BIZ_VIBRATE, new Integer(100));
	            			}
            			}
            			
            			boolean succRecon = false;
            			int maxTry = 0;
        				final int sleep_internal_ms = (ContextManager.getContextInstance().isUsingUDPAtMobile())?1000:500;//如果是手机端，则采用较长的间隔时间
        				final int try_num = (ContextManager.getContextInstance().isUsingUDPAtMobile()?99999999:2);//如果是手机端，则进行无限尝试
            			while((isShutdown == false) && 
            					(    (ContextManager.cmStatus == ContextManager.STATUS_CLIENT_SELF) 
            					||  (ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF))
            					 	&& (succRecon = (SIPManager.reConnectAfterResetExcep() != null)) == false){
            				//仅重建TCP，不建UDP
							try{
            					Thread.sleep(sleep_internal_ms);
            				}catch (final Exception eee) {
								
							}
            				if((++maxTry > try_num) || isShutdown){
            					break;
            				}
            			}

            			if(succRecon){
            				lastReconnAfterResetMS = System.currentTimeMillis();
            				L.V = L.O ? false : LogManager.log("Success reconnect after reset error");
            			}else{
            				if(ContextManager.getContextInstance().isUsingUDPAtMobile() == false){
            					SIPManager.notifyRelineon(false);
            				}
            			}

            		}else{          
            			try{
            				dataInputStream.close();
            			}catch (final Throwable ex) {
						}
						dataInputStream = null;
//						if(!isShutdown){
							if(ContextManager.getContextInstance().isUsingUDPAtMobile() == false){
								SIPManager.notifyRelineon(false);
							}
//						}
            		}
            	}
			}
        }//while
    	
    	hc.core.L.V=hc.core.L.O?false:LogManager.log("Receiver shutdown");
	}
	
	private boolean isShutdown = false;
	
	public void shutDown() {
    	isShutdown = true;
    	synchronized (LOCK) {
			LOCK.notify();
		}
	}

	long receiveUpdateMS;
	
	public void setUdpServerSocket(final Object tcpOrUDPsocket) throws Exception{
		this.dataInputStream = (tcpOrUDPsocket==null)?null:SIPManager.getSIPContext().getInputStream(tcpOrUDPsocket);
		if(dataInputStream != null){
			receiveUpdateMS = System.currentTimeMillis();
//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Changed Receive Socket:" + dataInputStream.hashCode());
			synchronized (LOCK) {
				LOCK.notify();
			}
		}
	}
}