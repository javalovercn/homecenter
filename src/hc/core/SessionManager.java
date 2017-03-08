package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;

import java.util.Vector;

public abstract class SessionManager {
	private static CoreSession preparedSocketSession;
	protected final static Vector sessionListThreadSafe = new Vector(2);
	
	public static final void addToList(final CoreSession coreSS){
		CCoreUtil.checkAccess();
		
		preparedSocketSession = coreSS;
		sessionListThreadSafe.addElement(coreSS);
	}
	
	public static boolean checkAtLeastOneMeet(final int status){
		synchronized (sessionListThreadSafe) {
			final int size = sessionListThreadSafe.size();//不能加锁，会增加应用层互锁
			for (int i = 0; i < size; i++) {
				final CoreSession coreSS = (CoreSession)sessionListThreadSafe.elementAt(i);
				if(coreSS.context != null && coreSS.context.cmStatus == status){
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean checkAtLeastOneNotMeet(final int status){
		synchronized (sessionListThreadSafe) {
			final int size = sessionListThreadSafe.size();//不能加锁，会增加应用层互锁

			for (int i = 0; i < size; i++) {
				final CoreSession coreSS = (CoreSession)sessionListThreadSafe.elementAt(i);
				if(coreSS.context != null && coreSS.context.cmStatus != status){
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 
	 * @return 不返回null
	 */
	public static final CoreSession[] getAllSocketSessions(){
		CCoreUtil.checkAccess();
		
		synchronized (sessionListThreadSafe) {
			final int size = sessionListThreadSafe.size();
			CoreSession[] out = new CoreSession[size];
			int copyIdx = 0;
			
			for (; copyIdx < size; copyIdx++) {
				out[copyIdx] = (CoreSession)sessionListThreadSafe.elementAt(copyIdx);
			}
			
			return out;
		}
	}
	
	public static final CoreSession getCoreSessionByConnectionID(final long hcConnectionID){
		CCoreUtil.checkAccess();
		
		synchronized (sessionListThreadSafe) {
			final int size = sessionListThreadSafe.size();
			
			for (int copyIdx = 0; copyIdx < size; copyIdx++) {
				CoreSession coreSS = (CoreSession)sessionListThreadSafe.elementAt(copyIdx);
				if(coreSS.hcConnection.connectionID == hcConnectionID){
					return coreSS;
				}
			}
		}
		
		return null;
	}

	public static CoreSession getPreparedSocketSession(){
		CCoreUtil.checkAccess();
		return preparedSocketSession;
	}
	
	/**
	 * 当用户下线时，必须释放相应的资源
	 * @param coreSocketSession
	 */
	public final static void release(final CoreSession coreSocketSession){
		if(coreSocketSession != null){
			if(sessionListThreadSafe.removeElement(coreSocketSession)){
				coreSocketSession.release();
			}
		}
	}
	
	public final static void shutdown(){
		if(sessionListThreadSafe.size() == 1){
			if(L.isInWorkshop){
				LogManager.log("ready to shutdown server!");
			}
		}else{
			if(L.isInWorkshop){
				LogManager.log("there is more than one sessions on server, skip shutdown server!");
			}
			return;
		}
		
		ExceptionReporter.shutdown();
	}

	public static boolean isReadyToLineOnAndOnlyOneSession() {
		synchronized (sessionListThreadSafe) {
			final int size = sessionListThreadSafe.size();
			if(size != 1){
				return false;
			}
		
			CoreSession coreSS = (CoreSession)sessionListThreadSafe.elementAt(0);
			return coreSS.context.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON;
		}
	}
}
