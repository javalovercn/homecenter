package hc.server.msb;

import hc.App;
import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.util.LangUtil;
import hc.core.util.MobileAgent;
import hc.core.util.ReturnableRunnable;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
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
				public Object run() throws Throwable {
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

	public static int getDeviceHeightFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
		return coreSS.clientDesc.getClientHeight();
		}else{
			return 0;
		}
	}

	public static int getDeviceDPIFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
			return coreSS.clientDesc.getDPI();
		}else{
			return SimuMobile.PROJ_LEVEL_MOBILE_DPI;
		}
	}
	
	private final static boolean isIOS(final J2SESession coreSS){
		return coreSS.clientDesc.isIOS();
	}

	public final static MobileAgent getMobileAgent(final J2SESession coreSS){
		return coreSS.clientDesc.getAgent();
	}

	public final static String getMobileSoftUID(final J2SESession coreSS){
		return getMobileAgent(coreSS).getSoftUID();
	}
	
	public final static String getMobileMemberID(final J2SESession coreSS){
		return getMobileAgent(coreSS).getMemberID();
	}

	public static int getDeviceWidthFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
			return coreSS.clientDesc.getClientWidth();
		}else{
			return 0;
		}
	}

	public static int getMletDPIFrom(final J2SESession coreSS) {
		if (isInServing(coreSS.context)) {
			if(isIOS(coreSS)){
				return (int)(coreSS.clientDesc.getDPI() / Float.parseFloat(coreSS.clientDesc.getClientScale()));
			}else{
				return coreSS.clientDesc.getDPI();
			}
		}else{
			return SimuMobile.PROJ_LEVEL_MOBILE_DPI;
		}
	}
	
	public static int getMletWidthFrom(final J2SESession coreSS, final boolean isForHtmlMlet) {
		if (isInServing(coreSS.context)) {
			if(isForHtmlMlet && isIOS(coreSS)){
				return coreSS.clientDesc.getIOSDrawWidth();
			}else{
				return coreSS.clientDesc.getClientWidth();
			}
		}else{
			return 0;
		}
	}
	
	public static int getMletWidthFrom(final J2SESession coreSS) {
		return getMletWidthFrom(coreSS, true);
	}

	public static int getMletHeightFrom(final J2SESession coreSS, final boolean isForHtmlMlet) {
		if (isInServing(coreSS.context)) {
			if(isForHtmlMlet && isIOS(coreSS)){
				return coreSS.clientDesc.getIOSDrawHeight();
			}else{
				return coreSS.clientDesc.getClientHeight();
			}
		}else{
			return 0;
		}	
	}
	
	public static int getMletHeightFrom(final J2SESession coreSS) {
		return getMletHeightFrom(coreSS, true);
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
		if(coreSS == null){
			return LangUtil.EN_US;
		}
		
		if (isInServing(coreSS.context)) {
			return coreSS.clientDesc.getClientLang();
		}else{
			return SimuMobile.PROJ_LEVEL_MOBILE_LOCALE;
		}
	}

	public static J2SESession getCoreSSFromCtx(final ProjectContext ctx) {
		return ServerUIAPIAgent.getProjResponserMaybeNull(ctx).getSessionContextFromCurrThread().j2seSocketSession;
	}
}
