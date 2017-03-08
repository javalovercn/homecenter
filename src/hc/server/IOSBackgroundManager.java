package hc.server;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.LogManager;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.J2SESession;

public class IOSBackgroundManager {
	
	private final static HCTimer buildCloseIOSLongConnection(final J2SESession coreSS){
		final int iosMaxBGMinutes = UserThreadResourceUtil.getMobileAgent(coreSS).getIOSMaxBGMinutes();
		return new HCTimer("iOSLongConnection", 1000 * 60 * iosMaxBGMinutes, true) {
			@Override
			public void doBiz() {
				LogManager.log("force close connection when iOS keep in background for max minutes!");
				J2SESessionManager.stopSession(coreSS, true, true, false);
				setEnable(false);
			}
		};
	}

	private final static void clearIOSLongConnectionTimer(final J2SESession coreSS) {
		if(coreSS.closeIOSLongConnection != null){
			LogManager.log("remove iOS long connection watch timer.");
			HCTimer.remove(coreSS.closeIOSLongConnection);
		}
	}
	
	private static boolean isKeepaliveEnableOld;
	
	private static void flipIOSBackgroundXX(final J2SESession coreSS, final boolean isBackground){
		LogManager.log("client iOS background : [" + isBackground + "]");
		
		if(isBackground){
			if(coreSS.closeIOSLongConnection != null){
				clearIOSLongConnectionTimer(coreSS);
			}
			coreSS.closeIOSLongConnection = buildCloseIOSLongConnection(coreSS);
		}else{
			clearIOSLongConnectionTimer(coreSS);
		}

		if(isBackground){
			LogManager.warning("iOS will do nothing when in background!!!");
			
			isKeepaliveEnableOld = coreSS.keepaliveManager.keepalive.isEnable();
			if(isKeepaliveEnableOld){
				LogManager.log("disable keepalive when iOS in background!");
				coreSS.keepaliveManager.keepalive.setEnable(false);
			}
		}else{
			if(isKeepaliveEnableOld){
				LogManager.log("enable keepalive when iOS resume from background!");
				coreSS.keepaliveManager.resetSendData();
				coreSS.keepaliveManager.keepalive.resetTimerCount();
				coreSS.keepaliveManager.keepalive.setEnable(true);
			}
		}
	}
	
	public static boolean isIOSForBackgroundCond(final J2SESession coreSS) {
		return UserThreadResourceUtil.getMobileAgent(coreSS).getOS().equals(ProjectContext.OS_IOS);
	}
}
