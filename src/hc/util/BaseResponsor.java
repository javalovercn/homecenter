package hc.util;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.cache.CacheManager;
import hc.core.util.CCoreUtil;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.J2SESession;

import java.util.Vector;

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
	
	private final Vector<String> cacheSoftUID = new Vector<String>(CacheManager.MAX_CACHE_SOFTUID_NUM + 1);
	
	public final void addCacheSoftUID(final String uid){
		CCoreUtil.checkAccess();
		
		L.V = L.WShop ? false : LogManager.log("addCacheSoftUID : " + uid);
		synchronized(cacheSoftUID){
			if(cacheSoftUID.contains(uid) == false){
				cacheSoftUID.add(uid);
			}
		}
	}
	
	public final void notifyCacheSoftUIDLogout(){
		L.V = L.WShop ? false : LogManager.log("notifyCacheSoftUIDLogout");
		
		new HCTimer("clearCacheSoftUID", HCTimer.ONE_MINUTE * 2, true) {
			@Override
			public void doBiz() {
				HCTimer.remove(this);
				final J2SESession[] sessions = J2SESessionManager.getAllOnlineSocketSessions();
				if(sessions == null || sessions.length == 0){
					synchronized(cacheSoftUID){
						if(cacheSoftUID.size() >= CacheManager.MAX_CACHE_SOFTUID_NUM){
							L.V = L.WShop ? false : LogManager.log("clear cacahe for reaching max num softuid.");
							cacheSoftUID.clear();
							CacheManager.clearBuffer();
						}
					}
				}
			}
		};
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


