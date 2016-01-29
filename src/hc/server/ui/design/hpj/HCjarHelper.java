package hc.server.ui.design.hpj;

import hc.core.IConstant;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

public class HCjarHelper {
	public static String getRobotNameAtIdx(final Map<String, Object> map, final int idx){
		return getMSBXXXNameAtIdx(map, HCjar.ROBOT_ITEM_HEADER, idx);
	}
	
	public static final boolean isTrue(final Map map, final String key, final boolean defaultValue){
		String value = (String)map.get(key);
		return (value==null)?defaultValue:(value.equals(IConstant.TRUE));
	}
	
	public static final void setBoolean(final Map map, final String key, boolean value){
		map.put(key, value?IConstant.TRUE:IConstant.FALSE);
	}
	
	/**
	 * remove all item where key start with header.
	 * @param map
	 * @param header
	 */
	public static final void removeHeaderStartWith(final Map<String, Object> map, final String header){
		final Iterator<Map.Entry<String, Object>> keys = map.entrySet().iterator();
		while(keys.hasNext()){
			Map.Entry<String, Object> entry = keys.next();
			if(entry.getKey().startsWith(header, 0)){
				keys.remove();
			}
		}
	}
	
	public static String getRobotListenerAtIdx(final Map<String, Object> map, final int idx){
		return getMSBXXXListenerAtIdx(map, HCjar.ROBOT_ITEM_HEADER, idx);
	}

	private static String getMSBXXXNameAtIdx(final Map<String, Object> map,
			final String keyName, final int idx) {
		return (String)map.get(keyName + idx + "." + HCjar.PROC_NAME);
	}
	
	private static String getMSBXXXListenerAtIdx(final Map<String, Object> map,
			final String keyName, final int idx) {
		return (String)map.get(keyName + idx + "." + HCjar.PROC_LISTENER);
	}
	
	public static String getDeviceNameAtIdx(final Map<String, Object> map, final int idx){
		return getMSBXXXNameAtIdx(map, HCjar.DEVICE_ITEM_HEADER, idx);
	}

	public static String getDeviceListenerAtIdx(final Map<String, Object> map, final int idx){
		return getMSBXXXListenerAtIdx(map, HCjar.DEVICE_ITEM_HEADER, idx);
	}

	public static String getConverterNameAtIdx(final Map<String, Object> map, final int idx){
		return getMSBXXXNameAtIdx(map, HCjar.CONVERTER_ITEM_HEADER, idx);
	}

	public static String getConverterListenerAtIdx(final Map<String, Object> map, final int idx){
		return getMSBXXXListenerAtIdx(map, HCjar.CONVERTER_ITEM_HEADER, idx);
	}

	/**
	 * 返回{name, src}
	 * @param projID
	 * @return
	 */
	public static Vector<String>[] getRobotsSrc(Map<String, Object> map){
		Vector<String> names = new Vector<String>();
		Vector<String> src = new Vector<String>();
		
		int itemCount = getRobotNum(map);
		
		for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
			names.add(getRobotNameAtIdx(map, itemIdx));
			src.add(getRobotListenerAtIdx(map, itemIdx));
		}
		
		Vector<String>[] out = new Vector[2];
		out[0] = names;
		out[1] = src;
		return out;
	}
	
	/**
	 * return {Vector names, Vector source}
	 * @param map
	 * @return
	 */
	public static Vector<String>[] getDevicesSrc(final Map<String, Object> map){
		Vector<String> names = new Vector<String>();
		Vector<String> src = new Vector<String>();
		
		int itemCount = getDeviceNum(map);
		
		for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
			names.add(getDeviceNameAtIdx(map, itemIdx));
			src.add(getDeviceListenerAtIdx(map, itemIdx));
		}
		
		Vector<String>[] out = new Vector[2];
		out[0] = names;
		out[1] = src;
		return out;
	}
	
	/**
	 * return {Vector names, Vector source}
	 * @param map
	 * @return
	 */
	public static Vector<String>[] getConvertersSrc(final Map<String, Object> map){
		Vector<String> names = new Vector<String>();
		Vector<String> src = new Vector<String>();
		
		int itemCount = getConverterNum(map);
		
		for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
			names.add(getConverterNameAtIdx(map, itemIdx));
			src.add(getConverterListenerAtIdx(map, itemIdx));
		}
		
		Vector<String>[] out = new Vector[2];
		out[0] = names;
		out[1] = src;
		return out;
	}

	public static int getRobotNum(final Map<String, Object> map) {
		return getMSBXXXNum(map, HCjar.ROBOT_NUM);
	}

	public static int getDeviceNum(final Map<String, Object> map) {
		return getMSBXXXNum(map, HCjar.DEVICE_NUM);
	}

	public static int getConverterNum(final Map<String, Object> map) {
		return getMSBXXXNum(map, HCjar.CONVERTER_NUM);
	}

	private static int getMSBXXXNum(final Map<String, Object> map, final String keyName) {
		Object num = map.get(keyName);
		int itemCount = 0;
		if(num != null){
			itemCount = Integer.parseInt((String)num);
		}
		return itemCount;
	}
}
