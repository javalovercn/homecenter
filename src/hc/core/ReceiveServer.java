package hc.core;

import hc.core.sip.SIPManager;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.EventBack;
import hc.core.util.EventBackCacher;
import hc.core.util.LogManager;

import java.io.DataInputStream;

public class ReceiveServer implements Runnable{
	DataInputStream dataInputStream;
	public static final ByteArrayCacher recBytesCacher = ByteUtil.byteArrayCacher;
	
    public Thread thread;
	public ReceiveServer(){
		thread = new Thread(this);//"Receive Server"
		thread.setPriority(Thread.MAX_PRIORITY);
        //J2ME不支持setName
		//thread.setName("Receive Server");
    }
	
	public void start(){
		if(thread != null){
			thread.start();
		}
	}
    
	private final Boolean LOCK = new Boolean(false);
	
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
	
	public void run(){
		final int initSize = 2048;
		final int initMaxDataLen = initSize - MsgBuilder.MIN_LEN_MSG;
		byte[] bs = null;
		final EventBackCacher ebCacher = EventBackCacher.getInstance();
		final int MAX_DATA_LEN = 1024 * 1024;
		final Exception maxDataLenException = new Exception("Max DataLen, maybe clientReset Error");
		long lastReconnAfterResetMS = System.currentTimeMillis();
		final int WAIT_MODI_STATUS_MS = HCTimer.HC_INTERNAL_MS + 100;
		
    	while (!isShutdown) {
			if(dataInputStream == null){
	    		synchronized (LOCK) {
	    			if(dataInputStream == null){
	    				try {
							LOCK.wait();
						} catch (Exception e) {
//							e.printStackTrace();
						}
						continue;
	    			}
				}
			}
            try {
            	bs = recBytesCacher.getFree(initSize);
            	
            	dataInputStream.readFully(bs, 0, MsgBuilder.MIN_LEN_MSG);
//				L.V = L.O ? false : LogManager.log("Receive head len:" + len);
				
//				final byte ctrlTag = bs[MsgBuilder.INDEX_CTRL_TAG];
				
//				System.out.println("Receive One packet [" + bs[MsgBuilder.INDEX_CTRL_TAG] + "][" + bs[MsgBuilder.INDEX_CTRL_SUB_TAG] + "]");
				
				final int temp0 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
				final int temp1 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
				final int temp2 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;
				final int dataLen = ((temp0 << 16) + (temp1 << 8) + temp2);

//				L.V = L.O ? false : LogManager.log("Receive head data len:" + dataLen);
				
				//有可能因ClientReset而导致收到不完整包，产生虚假大数据
				if(dataLen > MAX_DATA_LEN){
					throw maxDataLenException;
				}
				
				
				if(initMaxDataLen < dataLen){
					byte[] temp = recBytesCacher.getFree(dataLen + MsgBuilder.MIN_LEN_MSG);
					System.arraycopy(bs, 0, temp, 0, MsgBuilder.MIN_LEN_MSG);
					recBytesCacher.cycle(bs);
					bs = temp;
				}
				
				//以下段不用用 bs 变量，因为上行已发生变更可能。
				if(dataLen > 0){
					dataInputStream.readFully(bs, MsgBuilder.MIN_LEN_MSG, dataLen);
				}

//				L.V = L.O ? false : LogManager.log("Receive data len:" + dataLen);
				EventBack eb = ebCacher.getFree();
				eb.setBSAndDatalen(null, bs, dataLen);
				ConditionWatcher.addWatcher(eb);
				
//				L.V = L.O ? false : LogManager.log("Finished Receive Biz Action");
            }catch (Exception e) {
				//因为重连手机端,可能导致bs重分配，而导致错误
            	recBytesCacher.cycle(bs);

            	if(ContextManager.cmStatus == ContextManager.STATUS_EXIT){
            		try{
            			Thread.sleep(100);
            		}catch (Exception ex) {
						
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
            		}catch (Exception ex) {
						
					}
            		continue;
            	}
            	
            	L.V = L.O ? false : LogManager.log("Receive Exception:[" + e.getMessage() + "], maybe skip receive.");

    			SIPManager.getSIPContext().deploySocket(null, null, null);
    			
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
    				}catch (Exception ee) {
    					
    				}
            		if(isShutdown == false && (System.currentTimeMillis() - lastReconnAfterResetMS > 2000)//1000稍小，改为2000   
            				&& SIPManager.isOnRelay() && 
            				(    (ContextManager.cmStatus == ContextManager.STATUS_CLIENT_SELF) 
            					||  (ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF))){
						
						//手机端，因网络不稳定导致意外reset，进入重连模式
            			L.V = L.O ? false : LogManager.log("Network reset error, reconnect...");
            			ContextManager.getContextInstance().doExtBiz(IContext.BIZ_MOVING_SCREEN_TIP, 
            					ContextManager.getContextInstance().doExtBiz(IContext.BIZ_I18N_KEY, "m21"));
            			
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
            				}catch (Exception eee) {
								
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
            			}catch (Throwable ex) {
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
	}

	public void setUdpServerSocket(DataInputStream s) {
		this.dataInputStream = s;
		if(s != null){
//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Changed Receive Socket:" + s.hashCode());
			synchronized (LOCK) {
				LOCK.notify();
			}
		}
	}
}