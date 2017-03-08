package hc.util;

import hc.core.MsgBuilder;
import hc.core.util.CCoreUtil;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.design.J2SESession;
import hc.server.util.ServerCUtil;

public class UpdateOneTimeRunnable implements Runnable{
	final J2SESession j2seCoreSS;
	
	public UpdateOneTimeRunnable(final J2SESession ss){
		this.j2seCoreSS = ss;
	}
	
	@Override
	public void run() {
		final int isLineOff = 1000 * 4;
		
		if(j2seCoreSS.hcConnection.updateMinMinutes <= 0 || j2seCoreSS.hcConnection.updateMinMinutes > 20){
			j2seCoreSS.hcConnection.updateMinMinutes = 20;
		}
		
		while(true){
			try{
				Thread.sleep(1000 * 60 * j2seCoreSS.hcConnection.updateMinMinutes);
			}catch (final Exception e) {
			}
			if(j2seCoreSS.hcConnection.isStopRunning){
				break;
			}
			
//			if(IOSBackgroundManager.isIOSForBackgroundCond()){
//				if(.ServerUIAPIAgent.getMobileAgent(.isBackground()){
//					LogManager.log("skip trans one time for iOS in background mode.");
//					continue;
//				}
//			}

			final Object outStreamLock = j2seCoreSS.hcConnection.getOutputStream();
			if(outStreamLock == null){
				continue;
			}
			
			CCoreUtil.generateRandomKey(ResourceUtil.getStartMS(), j2seCoreSS.hcConnection.oneTime, 0, CCoreUtil.CERT_KEY_LEN);
//			LogManager.log("OneTime:" + CUtil.toHexString(CUtil.OneTimeCertKey));
			
//			LogManager.log("transport new one time certification key to client");
			synchronized (j2seCoreSS.context.sendLock) {
				j2seCoreSS.hcConnection.isReceivedOneTimeInSecuChannalFromMobile = false;

				ServerCUtil.transCertKey(j2seCoreSS, j2seCoreSS.hcConnection.oneTime, MsgBuilder.E_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL, true);
				
				synchronized (j2seCoreSS.hcConnection.oneTimeReceiveNotifyLock) {
					try {
						j2seCoreSS.hcConnection.oneTimeReceiveNotifyLock.wait(isLineOff);
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				if(j2seCoreSS.hcConnection.isReceivedOneTimeInSecuChannalFromMobile == false){
					j2seCoreSS.hcConnection.isStopRunning = true;
				}
				
			}//end synchronized
			
			if(j2seCoreSS.hcConnection.isStopRunning){
				break;
			}
		}//end while(true)
			
		if(j2seCoreSS.hcConnection.isReceivedOneTimeInSecuChannalFromMobile == false){
//			LogManager.log("timeout for ReceivedOneTimeInSecuChannalFromMobile");
			J2SESessionManager.stopSession(j2seCoreSS, true, true, false);
		}
		
//		System.out.println("--------done UpdateOneTimeRunnable--------------");
	}
}
