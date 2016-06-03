package hc.server;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.LogManager;
import hc.server.ui.ClientDesc;
import hc.server.ui.ProjectContext;

public class IOSBackgroundManager {
	private static HCTimer closeIOSLongConnection;
	
	private final static HCTimer buildCloseIOSLongConnection(){
		final int iosMaxBGMinutes = ClientDesc.getAgent().getIOSMaxBGMinutes();
		return new HCTimer("iOSLongConnection", 1000 * 60 * iosMaxBGMinutes, true) {
			@Override
			public void doBiz() {
				L.V = L.O ? false : LogManager.log("force close connection when iOS keep in background for max minutes!");
				RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_ServerReq_STR);
				SIPManager.notifyRelineon(false);
				setEnable(false);
			}
		};
	}

	private final static void clearIOSLongConnectionTimer() {
		if(closeIOSLongConnection != null){
			L.V = L.O ? false : LogManager.log("remove iOS long connection watch timer.");
			HCTimer.remove(closeIOSLongConnection);
		}
	}
	
	private static boolean isKeepaliveEnableOld;
	
	private static void flipIOSBackgroundXX(final boolean isBackground){
		L.V = L.O ? false : LogManager.log("client iOS background : [" + isBackground + "]");
		
		if(isBackground){
			if(closeIOSLongConnection != null){
				clearIOSLongConnectionTimer();
			}
			closeIOSLongConnection = buildCloseIOSLongConnection();
		}else{
			clearIOSLongConnectionTimer();
		}

		if(isBackground){
			LogManager.warning("iOS will do nothing when in background!!!");
			
			isKeepaliveEnableOld = KeepaliveManager.keepalive.isEnable();
			if(isKeepaliveEnableOld){
				L.V = L.O ? false : LogManager.log("disable keepalive when iOS in background!");
				KeepaliveManager.keepalive.setEnable(false);
			}
		}else{
			if(isKeepaliveEnableOld){
				L.V = L.O ? false : LogManager.log("enable keepalive when iOS resume from background!");
				KeepaliveManager.resetSendData();
				KeepaliveManager.keepalive.resetTimerCount();
				KeepaliveManager.keepalive.setEnable(true);
			}
		}
	}
	
	public static boolean isIOSForBackgroundCond() {
		return ClientDesc.getAgent().getOS().equals(ProjectContext.OS_IOS);
	}
}
