package hc.util;

import hc.core.IContext;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.util.CCoreUtil;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.design.J2SESession;
import hc.server.util.ServerCUtil;

public class UpdateOneTimeRunnable implements Runnable{
	private static final int SLEEP_INTERNAL_MS = 50;
	public boolean isStopRunning = false;
	byte[] oneTime = new byte[CCoreUtil.CERT_KEY_LEN];
	final J2SESession j2seCoreSS;
	
	public UpdateOneTimeRunnable(final J2SESession ss){
		this.j2seCoreSS = ss;
	}
	
	@Override
	public void run() {
		long waitMSTotal;
		final int isLineOff = 1000 * 4;
		final IContext contextInstance = j2seCoreSS.context;
		int updateMinMinutes = RootConfig.getInstance().getIntProperty(RootConfig.p_UpdateOneTimeMinMinutes);
		
		if(updateMinMinutes <= 0 || updateMinMinutes > 20){
			updateMinMinutes = 20;
		}
		
		while(true){
			//等待收到应答
			waitMSTotal = 0;

			try{
				Thread.sleep(1000 * 60 * updateMinMinutes);
			}catch (final Exception e) {
			}
			if(isStopRunning){
				break;
			}
			
//			if(IOSBackgroundManager.isIOSForBackgroundCond()){
//				if(.ServerUIAPIAgent.getMobileAgent(.isBackground()){
//					L.V = L.O ? false : LogManager.log("skip trans one time for iOS in background mode.");
//					continue;
//				}
//			}

			final Object outStreamLock = contextInstance.getOutputStreamLockObject();
			if(outStreamLock == null){
				continue;
			}
			
			
			j2seCoreSS.isReceivedOneTimeInSecuChannalFromMobile = false;
			
			CCoreUtil.generateRandomKey(ResourceUtil.getStartMS(), oneTime, 0, CCoreUtil.CERT_KEY_LEN);
//			L.V = L.O ? false : LogManager.log("OneTime:" + CUtil.toHexString(CUtil.OneTimeCertKey));
			
//			L.V = L.O ? false : LogManager.log("transport new one time certification key to client");
			synchronized (outStreamLock) {
				ServerCUtil.transCertKey(j2seCoreSS.context, oneTime, MsgBuilder.E_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL, true);
				
				//waitMSTotal清零在while
				while(j2seCoreSS.isReceivedOneTimeInSecuChannalFromMobile == false){
					try{
						Thread.sleep(SLEEP_INTERNAL_MS);
						waitMSTotal += SLEEP_INTERNAL_MS;
					}catch (final Exception e) {
					}
					
					if(waitMSTotal > isLineOff){
						//进行断线处理
						isStopRunning = true;
					}
					
					if(isStopRunning){
						break;
					}
				}
				
			}//end synchronized
			
			if(isStopRunning){
				break;
			}
		}//end while(true)
			
		if(waitMSTotal > isLineOff){
//			L.V = L.O ? false : LogManager.log("timeout for ReceivedOneTimeInSecuChannalFromMobile");
			J2SESessionManager.stopSession(j2seCoreSS, true, true, false);
		}
		
//		System.out.println("--------done UpdateOneTimeRunnable--------------");
	}
}
