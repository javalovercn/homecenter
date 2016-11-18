package hc.core.util;

import hc.core.ConfigManager;
import hc.core.IConstant;

public class MobileAgent {
	public static final String UN_KNOW = "-1";
	public static final int INT_UN_KNOW = Integer.parseInt(UN_KNOW);
	
	final VectorMap vectorMap = new VectorMap(10);
	
	public static final String DEFAULT_SCALE = "1.0";
	
	public static final String TAG_HIDE_PREFIX = "hide_";
	
	private final static String TAG_OS = "os";
	private final static String TAG_VER = "osver";
	private final static String TAG_UID = "uid";
	private final static String TAG_HAS_CAMERA = TAG_HIDE_PREFIX + "camera";
	private final static String TAG_CONTROL_WIFI = TAG_HIDE_PREFIX + "ctrlWiFi";
	private final static String TAG_EncryptionStrength = "encryptionStrength";
	private final static String TAG_IS_BACKGROUND = ConfigManager.UI_IS_BACKGROUND;
	private final static String TAG_COLOR_BIT = "colorBit";
	private final static String TAG_REFRESH_MS = "refreshMS";
	private final static String TAG_IOS_MAX_BG_MINUTES = "iOSBGMinute";
	private final static String TAG_MENU_TRUE_COLOR = "MenuTrueColor";
	private final static String TAG_SCALE = TAG_HIDE_PREFIX + "Scale";//iOS screen scale
	
	private final static String SPLIT = ";";
	private final static String EQUAL = "=";
	
	public static final MobileAgent toObject(final String serial){
		final MobileAgent ma = new MobileAgent();
		
		final String[] items = StringUtil.splitToArray(serial, SPLIT);
		for (int i = 0; i < items.length; i++) {
			final String[] kv = StringUtil.splitToArray(items[i], EQUAL);   
			ma.set(kv[0], kv[1]);
		}
		
		return ma;
	}
	
	public final int size(){
		return vectorMap.size();
	}
	
	/**
	 * 将指定idx的key-value放入数组kv中，并返回。
	 * @param idx
	 * @param kv
	 * @return
	 */
	public final Object[] get(final int idx, final Object[] kv){
		return vectorMap.get(idx, kv);
	}
	
	public final String toSerial(){
		final StringBuffer sb = StringBufferCacher.getFree();
		final int size = vectorMap.size();
		
		for (int i = 0; i < size; i++) {
			if(sb.length() > 0){
				sb.append(SPLIT);
			}
			
			final KeyValue keyValue = (KeyValue)vectorMap.elementAt(i);
			sb.append(keyValue.key);
			sb.append(EQUAL);
			sb.append(keyValue.value);
		}
		
		final String out = sb.toString();
		StringBufferCacher.cycle(sb);
		return out;
	}
	
	public final String get(final String key, final String defaultValue){
		return (String)vectorMap.get(key, defaultValue);
	}
	
	public final void set(final String key, final String value){
//		L.V = L.O ? false : LogManager.log("client mobile [" + key + ":" + value + "]");
		vectorMap.set(key, value);
	}
	
	public final boolean isBackground(){
		String out = get(TAG_IS_BACKGROUND, IConstant.FALSE);
		return out.equals(IConstant.TRUE);
	}
	
	public final String getOS(){
		return get(TAG_OS, "");
	}
	
	public final void setOS(final String osName){
		set(TAG_OS, osName);
	}
	
	public final String getScale(){
		return get(TAG_SCALE, DEFAULT_SCALE);
	}
	
	public final void setScale(final String scale){
		set(TAG_SCALE, scale);
	}
	
	public final int getColorBit(){
		return Integer.parseInt(get(TAG_COLOR_BIT, UN_KNOW));
	}
	
	public final void setColorBit(final int colorBit){
		set(TAG_COLOR_BIT, String.valueOf(colorBit));
	}
	
	public final int getIOSMaxBGMinutes(){
		return Integer.parseInt(get(TAG_IOS_MAX_BG_MINUTES, UN_KNOW));
	}
	
	public final void setIOSMaxBGMinutes(final int minutes){
		set(TAG_IOS_MAX_BG_MINUTES, String.valueOf(minutes));
	}
	
	public final int getRefreshMS(){
		return Integer.parseInt(get(TAG_REFRESH_MS, UN_KNOW));
	}
	
	public final void setRefreshMS(final int ms){
		set(TAG_REFRESH_MS, String.valueOf(ms));
	}
	
	public final boolean isMenuTrueColor(){
		final String color = get(TAG_MENU_TRUE_COLOR, IConstant.FALSE);
		return color.equals(IConstant.TRUE);
	}
	
	public final void setMenuTrueColor(final boolean trueColor){
		set(TAG_MENU_TRUE_COLOR, trueColor?IConstant.TRUE:IConstant.FALSE);
	}
	
	public final int getEncryptionStrength(){
		return Integer.parseInt(get(TAG_EncryptionStrength, "0"));//缺省加密强度
	}
	
	public final String getEncryptionStrengthDesc(){
		final int es = getEncryptionStrength();
		return CCoreUtil.ENCRYPTION_STRENGTH_DESC[es];
	}
	
	public final void setEncryptionStrength(final int es){
		set(TAG_EncryptionStrength, String.valueOf(es));
	}
	
	public final boolean hasCamera(){
		try{
			return Integer.parseInt(get(TAG_HAS_CAMERA, "0")) > 0;
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			return false;
		}
	}
	
	public final void setHasCamera(final String hasCamera){
		set(TAG_HAS_CAMERA, hasCamera);
	}
	
	public final String getVer(){
		return get(TAG_VER, "0");
	}
	
	public final void setCtrlWiFi(final String ctrlWiFi){
		set(TAG_CONTROL_WIFI, ctrlWiFi);
	}
	
	public final boolean ctrlWiFi(){
		try{
			return Integer.parseInt(get(TAG_CONTROL_WIFI, "0")) > 0;
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			return false;
		}
	}
	public final void setVer(final String ver){
		set(TAG_VER, ver);
	}
	
	public final String getUID(){
		return get(TAG_UID, "");
	}
	
	public final void setUID(final String uuid){
		set(TAG_UID, uuid);
	}
}
