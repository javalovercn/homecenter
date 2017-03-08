package hc.core;

import hc.core.util.LogManager;
import hc.core.util.RootBuilder;
import hc.core.util.ThreadPriorityManager;

public class ConnectionRebuilder {
	final Object receiveNotifier = new Object();
	final Object sendNotifier = new Object();
	boolean isSuccNewConn;
	boolean isStartBuildingNewAtMobileSide;
	boolean isEnterBuildWaitNewConnection;
	long lastEnterBuildMS;
	final long waitMSForNewConn = Integer.parseInt(RootConfig.getInstance().
			getProperty(RootConfig.p_KeepAliveMS));
	
	public boolean isReleased;
	
	private final void delayReset(){
		isSuccNewConn = false;
		isStartBuildingNewAtMobileSide = false;
	}
	
	public final void waitBeforeSend(){
		synchronized (sendNotifier) {
			if(isEnterBuildWaitNewConnection && isReleased == false){
				final long startWaitMS = System.currentTimeMillis();
				try {
					L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] wait before send...");
					sendNotifier.wait(waitMSForNewConn);
				} catch (InterruptedException e) {
				}
				L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] timeout or continue from wait before send, WaitMS : " + (System.currentTimeMillis() - startWaitMS));
			}
		}
	}
	
	public final boolean notifyBuildNewConnection(final boolean isFromSender, final short cmStatus){
		isEnterBuildWaitNewConnection = true;

		if(isFromSender){
			L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] ready buildNewConnection from Sender.");
		}else{
			L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] ready buildNewConnection from Receiver.");
		}
		final boolean isServer = IConstant.serverSide;
		if(isServer){
			if(cmStatus == ContextManager.STATUS_SERVER_SELF){
				return waitForNew(isFromSender);
			}else{
				return false;
			}
		}else{
			if(cmStatus == ContextManager.STATUS_CLIENT_SELF){
				synchronized (this) {
					if(isStartBuildingNewAtMobileSide == false){
						isStartBuildingNewAtMobileSide = true;
						
						ContextManager.getThreadPool().run(new Runnable() {
							public void run() {
								startBuildNew();
							}
						});//由于是客户端发起，所以无需token
					}
				}
				return waitForNew(isFromSender);
			}else{
				return false;
			}
		}
	}
	
	private final void startBuildNew(){
		final long startMS = System.currentTimeMillis();
		final Object out = buildConn();
		if(out != null && out == IConstant.TRUE){
			return;
		}
		
		final long waitMS = (startMS + waitMSForNewConn / 2) - System.currentTimeMillis();
		if(waitMS > 0){
			try{
				Thread.sleep(waitMS);
			}catch (Exception e) {
			}
		}
		buildConn();//二次尝试
	}

	private final Object buildConn() {
		return RootBuilder.getInstance().doBiz(RootBuilder.ROOT_BUILD_NEW_CONNECTION, null);
	}

	private final boolean waitForNew(boolean isFromSender) {
		lastEnterBuildMS = System.currentTimeMillis();
		
		if(isFromSender){
			L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] skip wait for new in sender");
			return false;
		}else{
			synchronized (receiveNotifier) {
				if(isSuccNewConn){
					return isSuccNewConn;
				}
				
				final long startWaitMS = System.currentTimeMillis();
				try {
					L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] wait for new in receiver...");
					receiveNotifier.wait(waitMSForNewConn);
				} catch (InterruptedException e) {
				}
				L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] timeout or continue from wait for new in receiver, WaitMS : " + (System.currentTimeMillis() - startWaitMS));
			}
		}
		
		return isSuccNewConn;
	}
	
	public final void notifyContinue(){
		L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] notifyContinue.");
		
		isEnterBuildWaitNewConnection = false;
		isSuccNewConn = true;
		
		synchronized (receiveNotifier) {
			receiveNotifier.notifyAll();
		}
		
		synchronized (sendNotifier) {
			sendNotifier.notifyAll();//发送端延后，否则导致receive不完整
		}
		L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] notifyAll sendNotifier.");
		
		ContextManager.getThreadPool().run(new Runnable() {
			public void run() {
				try{
					Thread.sleep(ThreadPriorityManager.UI_DELAY_MOMENT);
				}catch (Exception e) {
				}
				
				delayReset();
			}
		});
	}
	
}
