package hc.core;


import java.util.Hashtable;

/**
 * 本类为其它环境，如android系统进行参数通信之用
 *
 */
public class ConfigManager {
	/**
	 * 考虑并发问题，请锁定本类
	 */
	public final static Hashtable table = new Hashtable();
	
	public final static void put(Object p, Object value){
		table.put(p, value);
	}
	
	public final static Object get(Object p, Object defalutValue){
		final Object v = table.get(p);
		return (v == null?defalutValue:v);
	}

	public static final String ANTI_ALIAS = "hc.font.antiAlias";
	public static final String UI_BUILDER = "hc.ui.builder";
	public static final String RESOURCES = "hc.resources";
	public static final String EXCHANGE_SCREEN_FORWARD = "hc.ui.exchangeScreenForward";
	public static final String TEXT_FIELD_PLACEHOLDER = "hc.ui.placeholder";
	public static final String DEVICE_DPI = "hc.ui.device.dpi";//以String存储，各实现平台
	public static final String NOTIFICATION = "hc.notification";
	public static final String STATUS_MOBI_OS = "hc.status.mobi.os";
	public static final String SET_MOBI_OS_TAG = "hc.set.mobi.os";
	
	public static int getOSType(){
		final Object v = table.get(STATUS_MOBI_OS);
		if(v == null || (v instanceof Integer) == false){
			return OS_J2ME;
		}
		return ((Integer)v).intValue();
	}
	
	public static final int OS_ANDROID = 1;
	public static final int OS_IPHONE = 2;
	public static final int OS_J2ME = 3;
	
	public static final int FLAG_NOTIFICATION_SOUND = 1;
	public static final int FLAG_NOTIFICATION_VIBRATE = 2;
	
	public static final int CHECK_PERMISSION_IMPL_SPECIAL_DONE = 19760728;//供ios和android实现之用

	public static void backward() {
		put(EXCHANGE_SCREEN_FORWARD, IConstant.FALSE);
	}
}
