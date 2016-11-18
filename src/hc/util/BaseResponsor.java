package hc.util;

import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.L;
import hc.core.sip.SIPManager;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.J2SESession;

import java.awt.Frame;

public abstract class BaseResponsor implements IBiz, IHCURLAction{
	public static final String[] SCRIPT_EVENT_LIST = {
		ProjectContext.EVENT_SYS_PROJ_STARTUP,
		ProjectContext.EVENT_SYS_MOBILE_LOGIN, 
		ProjectContext.EVENT_SYS_MOBILE_LOGOUT,
		ProjectContext.EVENT_SYS_PROJ_SHUTDOWN};

	private final void startUpdateOneTimeKeysProcess(final J2SESession coreSS){
		final IContext ctx = coreSS.context;
		if(ctx.isBuildedUPDChannel && ctx.isDoneUDPChannelCheck){
//			L.V = L.O ? false : LogManager.log("is using UDP, skip startUpdateOneTimeKeysProcess");
		}else if(SIPManager.isOnRelay(coreSS)){
			final UpdateOneTimeRunnable updateOneTimeKeysRunnable = new UpdateOneTimeRunnable(coreSS);
			ContextManager.getThreadPool().run(updateOneTimeKeysRunnable);
			if(coreSS != null){
				coreSS.updateOneTimeKeysRunnable = updateOneTimeKeysRunnable;
				if(L.isInWorkshop){
					LogManager.log("success startUpdateOneTimeKeysProcess!");
				}
			}else{
				if(L.isInWorkshop){
					LogManager.errToLog("fail to startUpdateOneTimeKeysProcess!");
				}
			}
		}
	}
	
	public final void activeNewOneTimeKeys(final J2SESession coreSS){
		if(coreSS != null){
			coreSS.isReceivedOneTimeInSecuChannalFromMobile = true;
			if(coreSS.updateOneTimeKeysRunnable != null){
				coreSS.setOneTimeCertKey(coreSS.updateOneTimeKeysRunnable.oneTime);
			}
			if(L.isInWorkshop){
				LogManager.log("success activeNewOneTimeKeys");
			}
		}else{
			if(L.isInWorkshop){
				LogManager.errToLog("fail to activeNewOneTimeKeys");
			}
		}
	}
	
	protected final void notifyMobileLogin(final J2SESession coreSS){
		createClientSession(coreSS);
		startUpdateOneTimeKeysProcess(coreSS);
	}

	protected final void notifyMobileLogout(final J2SESession coreSS){
		if(coreSS != null){
			final UpdateOneTimeRunnable updateOneTimeKeysRunnable = coreSS.updateOneTimeKeysRunnable;
			
			if(updateOneTimeKeysRunnable != null){
				updateOneTimeKeysRunnable.isStopRunning = true;
			}
			
			if(L.isInWorkshop){
				LogManager.log("successful stop UpdateOneTimeRunnable!");
			}
		}else{
			if(L.isInWorkshop){
				LogManager.errToLog("fail to stop UpdateOneTimeRunnable!");
			}
		}
	}
	
	public void stop(){
	}
	
	public abstract void enableLog(final boolean enable);
	
	/**
	 * 
	 * @param owner
	 * @return 如果检测不过，返回null；否则返回自己
	 * @throws Exception
	 */
	public abstract BaseResponsor checkAndReady(final Frame owner) throws Exception;
	
	/**
	 * @param socketSession
	 * @param contextName
	 */
	public abstract void enterContext(final J2SESession socketSession, String contextName);
	
	public abstract Object onEvent(final J2SESession socketSession, final String event);
	
	public abstract Object getObject(int funcID, Object para);
	
	public abstract void createClientSession(J2SESession ss);
	
	public abstract void releaseClientSession(J2SESession coreSS);
}


