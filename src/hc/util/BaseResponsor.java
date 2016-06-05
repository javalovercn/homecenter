package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.IHCURLAction;
import hc.server.ui.ProjectContext;
import hc.server.util.ServerCUtil;

import java.awt.Frame;

public abstract class BaseResponsor implements IBiz, IHCURLAction{
	public static final String[] SCRIPT_EVENT_LIST = {
		ProjectContext.EVENT_SYS_PROJ_STARTUP,
		ProjectContext.EVENT_SYS_MOBILE_LOGIN, 
		ProjectContext.EVENT_SYS_MOBILE_LOGOUT,
		ProjectContext.EVENT_SYS_PROJ_SHUTDOWN};

	UpdateOneTimeRunnable updateOneTimeKeysRunnable;
	
	private final void startUpdateOneTimeKeysProcess(){
		final IContext contextInstance = ContextManager.getContextInstance();
		if(contextInstance.isBuildedUPDChannel && contextInstance.isDoneUDPChannelCheck){
//			L.V = L.O ? false : LogManager.log("is using UDP, skip startUpdateOneTimeKeysProcess");
		}else if(SIPManager.isOnRelay()){
			updateOneTimeKeysRunnable = new UpdateOneTimeRunnable();
			ContextManager.getThreadPool().run(updateOneTimeKeysRunnable);
//			System.out.println("---------startUpdateOneTimeKeysProcess---------------- ");
		}
	}
	
	public final void activeNewOneTimeKeys(){
		final IContext contextInstance = ContextManager.getContextInstance();
		contextInstance.isReceivedOneTimeInSecuChannalFromMobile = true;
		if(updateOneTimeKeysRunnable != null){
			CUtil.setOneTimeCertKey(updateOneTimeKeysRunnable.oneTime);
		}
	}
	
	protected final void notifyMobileLogin(){
		createClientSession();
		startUpdateOneTimeKeysProcess();
	}

	protected final void notifyMobileLogout(final boolean isStopStatus){
		if(updateOneTimeKeysRunnable != null){
			updateOneTimeKeysRunnable.isStopRunning = true;
		}
	}
	
	public void stop(){
	}
	
	public abstract void enableLog(final boolean enable);
	
	public abstract BaseResponsor checkAndReady(final Frame owner) throws Exception;
	
	/**
	 * @param contextName
	 */
	public abstract void enterContext(String contextName);
	
	public abstract Object onEvent(Object event);
	
	public abstract Object getObject(int funcID, Object para);
	
	public abstract void addProjectContext(ProjectContext pc);
	
	public abstract void createClientSession();
	
	public abstract void releaseClientSession();
}


class UpdateOneTimeRunnable implements Runnable{
	private static final int SLEEP_INTERNAL_MS = 50;
	public boolean isStopRunning = false;
	byte[] oneTime = new byte[CCoreUtil.CERT_KEY_LEN];
	
	@Override
	public void run() {
		long waitMSTotal;
		final int isLineOff = 1000 * 4;
		final IContext contextInstance = ContextManager.getContextInstance();
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
//				if(ClientDesc.getAgent().isBackground()){
//					L.V = L.O ? false : LogManager.log("skip trans one time for iOS in background mode.");
//					continue;
//				}
//			}

			final Object outStreamLock = contextInstance.getOutputStreamLockObject();
			
			contextInstance.isReceivedOneTimeInSecuChannalFromMobile = false;
			
			CCoreUtil.generateRandomKey(App.getStartMS(), oneTime, 0, CCoreUtil.CERT_KEY_LEN);
//			L.V = L.O ? false : LogManager.log("OneTime:" + CUtil.toHexString(CUtil.OneTimeCertKey));
			
//			L.V = L.O ? false : LogManager.log("transport new one time certification key to client");
			synchronized (outStreamLock) {
				ServerCUtil.transCertKey(oneTime, MsgBuilder.E_TRANS_ONE_TIME_CERT_KEY_IN_SECU_CHANNEL, true);
				
				//waitMSTotal清零在while
				while(contextInstance.isReceivedOneTimeInSecuChannalFromMobile == false){
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
			RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_ServerReq_STR);
			SIPManager.notifyRelineon(false);
		}
		
//		System.out.println("--------done UpdateOneTimeRunnable--------------");
	}

}