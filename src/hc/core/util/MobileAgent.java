package hc.core.util;

import hc.core.ConfigManager;
import hc.core.IConstant;

public class MobileAgent {
	final VectorMap vectorMap = new VectorMap(10);
	
	private final static String TAG_OS = "os";
	private final static String TAG_VER = "osver";
	private final static String TAG_UID = "uid";
	private final static String TAG_HAS_CAMERA = "camera";
	private final static String TAG_CONTROL_WIFI = "ctrlWiFi";
	private final static String TAG_EncryptionStrength = "encryptionStrength";
	private final static String TAG_IS_BACKGROUND = ConfigManager.UI_IS_BACKGROUND;
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
	
	public final String toSerial(){
		final StringBuffer sb = new StringBuffer();
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
		
		return sb.toString();
	}
	
	public final String get(final String key, final String defaultValue){
		return (String)vectorMap.get(key, defaultValue);
	}
	
	public final void set(final String key, final String value){
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
			e.printStackTrace();
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
			e.printStackTrace();
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
