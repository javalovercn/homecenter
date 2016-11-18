package hc.server.ui.design;

import hc.core.L;
import hc.core.util.LogManager;

public class SessionMobileContext {
	J2SESession[] sessions;
	SessionContext[] mobileContexts;
	int size;
	
	public SessionMobileContext(){
		final int initSize = 2;
		
		sessions = new J2SESession[initSize];
		mobileContexts = new SessionContext[initSize];
	}
	
	/**
	 * 如果没有找到，返回null
	 * @param tg
	 * @return
	 */
	public final SessionContext getSessionContextByThreadGroup(final ThreadGroup tg){
		synchronized (this) {
			for (int i = 0; i < size; i++) {
				final SessionContext sc = mobileContexts[i];
				if(sc.mtg == tg){
					return sc;
				}
			}
		}
		return null;
	}
	
	public final void removeSession(final String projectID, final J2SESession session){
		synchronized (this) {
			int matchIdx = -1;
			for (int i = 0; i < size; i++) {
				if(sessions[i] == session){
					matchIdx = i;
					break;
				}
			}
			
			if(matchIdx >= 0){
				final SessionContext cycleMC = mobileContexts[matchIdx];
				SessionContext.cycle(projectID, cycleMC);
				
				int i = matchIdx + 1;
				for (; i < size; i++) {
					final int preStepIdx = i - 1;
					sessions[preStepIdx] = sessions[i];
					mobileContexts[preStepIdx] = mobileContexts[i];
				}
				
				final int preStepIdx = i - 1;
				sessions[preStepIdx] = null;
				mobileContexts[preStepIdx] = null;
				
				size--;
			}
		}
	}
	
	public final void release(final String projID){
		synchronized(this){
			for (int i = 0; i < size; i++) {
				SessionContext.cycle(projID, mobileContexts[i]);//考虑到可能还有未完线程，不作setNull处理
			}
//			size = 0;//考虑到可能还有未完线程，不为set to 0
		}
	}
	
	public final SessionContext getMobileContext(final J2SESession socket){
		if(socket != null){
			synchronized(this){
				for (int i = 0; i < size; i++) {
					if(sessions[i] == socket){
						return mobileContexts[i];
					}
				}
			}
		}
		
		if(L.isInWorkshop){
			LogManager.errToLog("fail to find current MobileContext of project");
		}
		return null;
	}
	
	public final void appendCurrContext(final J2SESession socket, final SessionContext mc){
		synchronized(this){
			if(size == sessions.length){
				final int newSize = size * 2;
				final J2SESession[] newSessions = new J2SESession[newSize];
				final SessionContext[] newMobileContexts = new SessionContext[newSize];
				
				for (int i = 0; i < size; i++) {
					newSessions[i] = sessions[i];
					newMobileContexts[i] = mobileContexts[i];
				}
				
				sessions = newSessions;
				mobileContexts = newMobileContexts;
			}
			
			sessions[size] = socket;
			mobileContexts[size] = mc;
			
			size++;
		}
	}
}
