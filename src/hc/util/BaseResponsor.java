package hc.util;

import hc.core.L;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.J2SESession;

import javax.swing.JFrame;

public abstract class BaseResponsor implements IBiz, IHCURLAction{
	public static final String[] SCRIPT_EVENT_LIST = {
		ProjectContext.EVENT_SYS_PROJ_STARTUP,
		ProjectContext.EVENT_SYS_MOBILE_LOGIN, 
		ProjectContext.EVENT_SYS_MOBILE_LOGOUT,
		ProjectContext.EVENT_SYS_PROJ_SHUTDOWN};

	protected final void notifyMobileLogin(final J2SESession coreSS){
		createClientSession(coreSS);
		if(coreSS == null){
			if(L.isInWorkshop){
				LogManager.errToLog("fail to startUpdateOneTimeKeysProcess!");
			}
		}else{
			coreSS.startUpdateOneTimeKeysProcess();
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
	public abstract BaseResponsor checkAndReady(final JFrame owner) throws Exception;
	
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


