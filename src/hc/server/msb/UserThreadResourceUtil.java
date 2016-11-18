package hc.server.msb;

import hc.App;
import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.util.MobileAgent;
import hc.core.util.ReturnableRunnable;
import hc.server.ui.ProjectContext;
import hc.server.ui.SimuMobile;
import hc.server.ui.design.J2SESession;
import hc.util.ResourceUtil;

/**
 * 通过用户级线程来访问系统级资源
 */
public class UserThreadResourceUtil {
	private static final ThreadGroup threadToken = App.getThreadPoolToken();
	
	public static void doNothing(){
	}
	
	private static Boolean isLoggerOn;
	
	static final boolean isLoggerOn(){
		if(isLoggerOn == null){
			isLoggerOn = (Boolean)runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					return ResourceUtil.isLoggerOn();
				}
			});
		}
		return isLoggerOn;
	}
	
	static final Object runAndWaitInSysThread(final ReturnableRunnable returnRun){
		return ContextManager.getThreadPool().runAndWait(returnRun, threadToken);
	}

	public final static boolean isInServing(final IContext ic) {
		//Dont add checkAccess()
		return ic != null && ic.cmStatus == ContextManager.STATUS_SERVER_SELF;
	}

	public static int getMobileHeightFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
		return coreSS.clientDesc.getClientHeight();
		}else{
			return 0;
		}
	}

	public static int getMobileDPIFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
			return coreSS.clientDesc.getDPI();
		}else{
			return SimuMobile.PROJ_LEVEL_MOBILE_DPI;
		}
	}

	public final static MobileAgent getMobileAgent(final J2SESession coreSS){
		return coreSS.clientDesc.getAgent();
	}

	public final static String getMobileSoftUID(final J2SESession coreSS){
		return getMobileAgent(coreSS).getUID();
	}

	public static int getMobileWidthFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
			return coreSS.clientDesc.getClientWidth();
		}else{
			return 0;
		}
	}

	public static String getMobileOSVerFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
			return getMobileAgent(coreSS).getVer();
		}else{
			return SimuMobile.PROJ_LEVEL_MOBILE_OS_VER;
		}
	}

	public static String getMobileOSFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
			final String[] allOS = {ProjectContext.OS_ANDROID, ProjectContext.OS_IOS, ProjectContext.OS_J2ME};
			
			final String oldOS = getMobileAgent(coreSS).getOS();
			for (int i = 0; i < allOS.length; i++) {
				final String oneOS = allOS[i];
				if(oneOS.equals(oldOS)){
					return oneOS;
				}
			}
			return oldOS;
		}else{
			return SimuMobile.PROJ_LEVEL_MOBILE_OS;
		}
	}

	public static String getMobileLocaleFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
			return coreSS.clientDesc.getClientLang();
		}else{
			return SimuMobile.PROJ_LEVEL_MOBILE_LOCALE;
		}
	}
}
