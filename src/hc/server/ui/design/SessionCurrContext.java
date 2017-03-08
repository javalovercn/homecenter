package hc.server.ui.design;

import hc.core.L;
import hc.core.util.BinTree;
import hc.core.util.LogManager;

public class SessionCurrContext {
	J2SESession[] sessions;
	String[] currContext;
	int size;
	
	final BinTree binTree = SessionThread.binTree;
	
	public SessionCurrContext(){
		final int size = 2;
		
		sessions = new J2SESession[size];
		currContext = new String[size];
	}
	
	public final void removeSession(J2SESession session){
		if(session == null){
			if(L.isInWorkshop){
				LogManager.errToLog("removeSession J2SESocketSession is null, try get it from thread.");
			}
			
			session = getCurrentSessionFromThread();
			
			if(session == null && L.isInWorkshop){
				LogManager.errToLog("fail to get J2SESocketSession from thread to removeSession.");
			}
		}
		
		synchronized (this) {
			int matchIdx = -1;
			for (int i = 0; i < size; i++) {
				if(sessions[i] == session){
					matchIdx = i;
					break;
				}
			}
			
			if(matchIdx >= 0){
				int i = matchIdx + 1;
				for (; i < size; i++) {
					final int preStepIdx = i - 1;
					sessions[preStepIdx] = sessions[i];
					currContext[preStepIdx] = currContext[i];
				}
				
				final int preStepIdx = i - 1;
				sessions[preStepIdx] = null;
				currContext[preStepIdx] = null;
				
				size--;
			}
		}
	}
	
	private final J2SESession getCurrentSessionFromThread(){
		return (J2SESession)binTree.get(Thread.currentThread().getId());
	}
	
	public final void setCurrContext(J2SESession session, final String cc){
		if(session == null){
			if(L.isInWorkshop){
				LogManager.errToLog("fail to get J2SESocketSession, try get it from thread.");
			}
			session = getCurrentSessionFromThread();
		}
		
		if(session != null){
			for (int i = 0; i < size; i++) {
				if(sessions[i] == session){
					currContext[i] = cc;
					if(L.isInWorkshop){
						LogManager.log("successful set current projectID in MobiUIResponsor.");
					}
					return;
				}
			}
		}
		
		if(L.isInWorkshop){
			LogManager.errToLog("fail to set current context of project");
		}
	}
	
//	public final String getCurrContext(){
//		final J2SESocketSession session = getCurrentSessionFromThread();
//		return getCurrContext(session);
//	}

	public final String getCurrContext(final J2SESession session) {
		if(session != null){
			for (int i = 0; i < size; i++) {
				if(sessions[i] == session){
					return currContext[i];
				}
			}
		}
		
		if(L.isInWorkshop){
			LogManager.errToLog("fail to find current projectID in MobiUIResponsor.");
		}
		return null;
	}
	
	public final void appendCurrContext(final J2SESession coreSS, final String projID){
		if(coreSS != null){
			synchronized(this){
				if(size == sessions.length){
					final int newSize = size * 2;
					final J2SESession[] newSessions = new J2SESession[newSize];
					final String[] newCurrContext = new String[newSize];
					
					for (int i = 0; i < size; i++) {
						newSessions[i] = sessions[i];
						newCurrContext[i] = currContext[i];
					}
					
					sessions = newSessions;
					currContext = newCurrContext;
				}
				
				sessions[size] = coreSS;
				currContext[size] = projID;
				
				size++;
			}
		}
	}
}
