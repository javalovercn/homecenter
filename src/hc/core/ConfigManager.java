package hc.core;

import java.util.Hashtable;

/**
 * 本类为其它环境，如android系统进行参数通信之用
 *
 */
public class ConfigManager {
	public static final boolean isTCPOnly = true;
	
	public final static Hashtable table = new Hashtable(32);
	
	public final static void put(final Object p, final Object value){
		table.put(p, value);
	}
	
	public final static void remove(final Object key){
		table.remove(key);
	}
	
	public final static Object get(final Object p, final Object defalutValue){
		final Object v = table.get(p);
		return (v == null?defalutValue:v);
	}

	public static final String INIT_MOBILE_CLIENT = "hc.InitMobileClient";
	public static final String ANTI_ALIAS = "hc.font.antiAlias";
	public static final String UI_BUILDER = "hc.ui.builder";
	public static final String RESOURCES = "hc.resources";
	public static final String EXCHANGE_SCREEN_FORWARD = "hc.ui.exchangeScreenForward";
	public static final String TEXT_FIELD_PLACEHOLDER = "hc.ui.placeholder";
	public static final String DEVICE_DPI = "hc.ui.device.dpi";//以String存储，各实现平台.屏幕密度DPI（120 / 160 / 240）
	public static final String DEVICE_XDPI = "hc.ui.device.xdpi";//每英寸像素数，比如一个像素为240x320，大小为1.5"x2"的屏幕，其xdpi=240/1.5=160，ydpi=320/2=160。
	public static final String DEVICE_YDPI = "hc.ui.device.ydpi";//注意这个值不一定是整数。float
	public static final String DEVICE_DENSITY = "hc.ui.device.density";//float 屏幕密度（0.75 / 1.0 / 1.5）
	public static final String NOTIFICATION = "hc.notification";
	public static final String STATUS_NET_TYPE = "hc.status.net";
	public static final String STATUS_MOBI_OS = "hc.status.mobi.os";
	public static final String STATUS_MOBI_OS_VER = "hc.status.mobi.os.ver";
	public static final String SET_MOBI_OS_TAG = "hc.set.mobi.os";
	public static final String AUTO_LOGIN_ID = "autoLoginID";
	public static final String AUTO_LOGIN_PWD = "autoLoginPWD";
	public static final String IS_DONE_AUTO_LOGIN = "isDoneAutoLogin";
	public static final String UI_CLIPBOARD_PUT = "hc.ui.clipboard.put";
	public static final String UI_CLIPBOARD_GET = "hc.ui.clipboard.get";
	public static final String UI_BUILD_JSVIEW = "hc.ui.buildJSView";
	public static final String UI_SCREEN_ON = "hc.ui.setScreenOn";
	public static final String UI_DISMISS_ALERT = "hc.ui.dismissAlert";
	public static final String UI_IS_BACKGROUND = "hc.ui.isBackground";
	public static final String UI_FORM_SIZE = "hc.ui.formSize";
	public static final String UI_RELEASE_OBJECT = "hc.ui.releaseObject";
	public static final String BUILD_NESTACTION = "hc.buildNestAction";
	public static final String UI_CHECK_BACKGROUND_ENABLE = "hc.ui.checkBackground";
	public static final String UI_SET_EXCEPTION_HANDLER_FOR_THREAD = "hc.ui.setExceptionHandler";
	public static final String UI_ENABLE_SCREEN_ADAPTER = "hc.ui.screenAdapter";
	public static final String UI_JUMP_TO_HOME = "hc.ui.jumpHome";
	public static final String UI_SCALE_OF_SCREEN = "hc.ui.scale";//iOS
	
	/**
	 * 判断客户端是否是后台运行。
	 * @return
	 */
	public static boolean isBackground(){
		final Object v = table.get(UI_IS_BACKGROUND);
		if(v == null || (v instanceof Boolean) == false){
			return false;
		}
		return ((Boolean)v).booleanValue();
	}
	
	public static void setBackground(boolean isBack){
		table.put(UI_IS_BACKGROUND, new Boolean(isBack));
	}
	
	public static int getOSType(){
		final Object v = table.get(STATUS_MOBI_OS);
		if(v == null || (v instanceof Integer) == false){
			return OS_J2ME;
		}
		return ((Integer)v).intValue();
	}
	
	public static String getOSVer(){
		final Object v = table.get(STATUS_MOBI_OS_VER);
		if(v == null || (v instanceof String) == false){
			return "0";
		}
		
		return (String)v;
	}
	
	public static final int OS_ANDROID = 1;
	public static final int OS_IPHONE = 2;
	public static final int OS_J2ME = 3;
	
	public static final String OS_ANDROID_DESC = "Android";
	public static final String OS_IOS_DESC = "iOS";
	public static final String OS_J2ME_DESC = "J2ME";
	
	public static final String WIFI_SECURITY_OPTION_NO_PASSWORD = "";
	public static final String WIFI_SECURITY_OPTION_WEP = "WEP";
	public static final String WIFI_SECURITY_OPTION_WPA_WPA2_PSK = "WPA_WPA2_PSK";
	
	public static final int NET_TYPE_WIFI = 1;
	public static final int NET_TYPE_2G_3G_XG = 2;
	
	public static final Integer getCurrNetType(){
		return (Integer)ConfigManager.get(ConfigManager.STATUS_NET_TYPE, new Integer(ConfigManager.NET_TYPE_2G_3G_XG));
	}
	
	public static final int FLAG_NOTIFICATION_SOUND = 1;
	public static final int FLAG_NOTIFICATION_VIBRATE = 2;
	
	public static final int CHECK_PERMISSION_IMPL_SPECIAL_DONE = 19760728;//供ios和android实现之用

	public static void foreward() {
		put(EXCHANGE_SCREEN_FORWARD, IConstant.TRUE);
	}
	
	public static void backward() {
		put(EXCHANGE_SCREEN_FORWARD, IConstant.FALSE);
	}

	public static String getDPI() {
		return (String) get(DEVICE_DPI, "0");
	}
	
	public static int getDPIInt() {
		return Integer.parseInt(getDPI());
	}

	public static final boolean isEnableExceptionReportForAndroid = true;

	public static final String HC_J2ME_LOAD_NOTIFICATION = "hc.j2me.load.Notification";
}
