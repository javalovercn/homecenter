package hc.server.ui;

import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.L;
import hc.core.RootServerConnector;
import hc.core.SessionManager;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.MutableArray;
import hc.server.J2SEContext;
import hc.server.msb.Robot;
import hc.server.msb.RobotEvent;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.msb.WiFiHelper;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import hc.server.util.StarterParameter;
import hc.util.SecurityDataProtector;

import java.util.Vector;

public class J2SESessionManager extends SessionManager {
	
	private static boolean isRestartDirecet = true;
	private static boolean isShutdown = false;
	
	public static final void notifySessionEvent(final int eventID, final ThreadGroup token){
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				final int size = sessionListThreadSafe.size();
				try{
					for (int i = 0; i < size; i++) {
						final J2SESession coreSS = (J2SESession)sessionListThreadSafe.elementAt(i);
						try{
							coreSS.sessionEventManager.actionListener(eventID);
						}catch (final Throwable e) {
							ExceptionReporter.printStackTrace(e);
						}
					}
				}catch (final ArrayIndexOutOfBoundsException e) {
				}
			}
		}, token);
	}
	
	/**
	 * 
	 * @return 如果没有联机的，则返回null
	 */
	public static final J2SESession[] getAllOnlineSocketSessions(){
		return ServerUIAPIAgent.getAllOnlineSocketSessions();
	}
	
	public static final void notifyReadyShutdown(){
		CCoreUtil.checkAccess();
		
		isShutdown = true;
	}
	
	public static boolean isShutdown(){
		return isShutdown;
	}
	
	public static final void notifyRestartDirect(){
		CCoreUtil.checkAccess();
		
		isRestartDirecet = true;
	}
	
	public static final Vector getSessionList(){
		CCoreUtil.checkAccess();
		
		return sessionListThreadSafe;
	}
	
	public final static void startNewIdleSession(){
		if(SecurityDataProtector.isEnableSecurityData() == false){//Android可能正在升级JRuby
			return;
		}
		
		CCoreUtil.checkAccess();
		
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				StarterParameter.startBeforeSession();
				
				startNewIdleSessionImpl();
			}
		});
	}
	
	private final synchronized static void startNewIdleSessionImpl(){
		if(isRestartDirecet){
			StarterParameter.reconnect();
			StarterParameter.startMulticastServer();
			isRestartDirecet = false;
		}
		
		if(isShutdown){
			return;
		}
		
		synchronized (sessionListThreadSafe) {
			final int size = sessionListThreadSafe.size();
			for (int i = 0; i < size; i++) {
				final J2SESession coreSS = (J2SESession)sessionListThreadSafe.elementAt(i);
				if(coreSS.isIdelSession){
					if(L.isInWorkshop){
						LogManager.log("there is an idle session [" + coreSS.hashCode() + "] on server, skip start new idle.");
					}
					return;
				}
			}
		}
		
		startJ2SESession();
	}
	
	private final static void startJ2SESession(){
		final J2SESession socketSession = new J2SESession();
		if(L.isInWorkshop){
			LogManager.log("creating idle session for client login, session hashCode : " + socketSession.hashCode());
		}else{
			LogManager.log("creating idle session for client login.");
		}
		
		final J2SEContext j2seContext = new J2SEContext(socketSession);
		SessionManager.appendToSessionPool(socketSession);
		
		WiFiHelper.startAPIfExists(j2seContext.coreSS);//依赖context
		j2seContext.run();
		
		socketSession.context.setStatus(ContextManager.STATUS_NEED_NAT);
		socketSession.keepaliveManager.keepalive.setEnable(true);
		socketSession.keepaliveManager.keepalive.doNowAsynchronous();
	}
	
	public final static void stopAllSession(final boolean notifyLineOff, final boolean notifyRelineon, final boolean isForce){
		CCoreUtil.checkAccess();
		
		final CoreSession[] coreSSS = SessionManager.getAllSocketSessions();
		for (int i = 0; i < coreSSS.length; i++) {
			final CoreSession coreSS = coreSSS[i];
			stopSession(coreSS, notifyLineOff, notifyRelineon, isForce);
		}
	}

	public final static void stopSession(final CoreSession coreSS, final boolean notifyLineOff, final boolean notifyRelineon, final boolean isForce){
		if(notifyLineOff){
			try{
				RootServerConnector.notifyLineOffType(coreSS, RootServerConnector.LOFF_ServerReq_STR);
			}catch (final Throwable e) {//Anroid环境下，有可能不连接服务器时，产生异常。需catch。
			}
		}
		if(notifyRelineon){
			coreSS.notifyLineOff(false, isForce);
		}
		try{
			coreSS.eventCenter.stop();//eventCenter可能为null, 停止产生可能新的sequence任务
		}catch (final Exception e) {
		}
	}
	
	public static J2SESession lockIdelSession(){
		CCoreUtil.checkAccess();
		
		final J2SESession coreSS = (J2SESession)SessionManager.getPreparedSocketSession();
		if(coreSS == null || coreSS.lockIdelSession() == false){
			return null;
		}else{
			return coreSS;
		}
	}

	public static void dispatchRobotEventSynchronized(final ProjResponser resp, final Robot robot, final RobotEvent event, final MutableArray mutableArray) {
		final Object[] arraySnap = mutableArray.array;
		final Object[] array = sessionListThreadSafe.toArray(arraySnap);
		
		if(array != arraySnap){
			mutableArray.array = array;
		}
		
		final int size = array.length;
		for (int i = 0; i < size; i++) {
			final J2SESession j2seSession = (J2SESession)array[i];
			if(j2seSession == null){
				break;
			}
			
			if(UserThreadResourceUtil.isInServing(j2seSession.context)){
				j2seSession.dispatchRobotEventSynchronized(resp, robot, event);
			}
		}
	}
}
