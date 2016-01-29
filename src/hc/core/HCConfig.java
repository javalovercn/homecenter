package hc.core;

import hc.core.util.StringUtil;

import java.util.Vector;

public class HCConfig {
	public static final String CFG_SPLIT = StringUtil.split;

	Vector v;
	
	public HCConfig(String msg){
		v = StringUtil.split(msg, CFG_SPLIT);
	}

	public static String toTransString(String[] item){
		return StringUtil.toSerialBySplit(item);
	}
	
	public int getIntProperty(short propertyID) {
		return getIntProperty(v, propertyID);
	}

	public static int getIntProperty(Vector vector, short propertyID) {
		if(vector == null){
			return 0;
		}
		final String obj = getProperty(vector, propertyID);
		if(obj == null){
			return 0;
		}
		return Integer.parseInt(obj);
	}
	
	public boolean isTrue(short propertyID) {
		return isTrue(v, propertyID);
	}

	public static boolean isTrue(Vector vector, short propertyID) {
		if(vector == null){
			return false;
		}
		String obj = getProperty(vector, propertyID);
		if(obj == null){
			return false;
		}else{
			return obj.equals(IConstant.TRUE);
		}
	}

	public String getProperty(short propertyID) {
		return getProperty(v, propertyID);
	}

	public static String getProperty(Vector vector, short propertyID) {
		if(vector == null){
			return null;
		}
		if(vector.size() < propertyID){
			return null;
		}
		return (String)vector.elementAt(propertyID);
	}
	
	public void setPropertyNull(short propertyID){
		setProperty(propertyID, null);
	}

	public void setProperty(short propertyID, Object value){
		if(v == null){
			return;
		}
		if(v.size() <= propertyID){
			v.insertElementAt(value, propertyID);
		}else{
			v.setElementAt(value, propertyID);
		}
	}
	
	public String toTransString(){
		StringBuffer sb = new StringBuffer("");
		final int size = v.size();
		for (int i = 0; i < size; i++) {
			sb.append((String)v.elementAt(i));
			if(i != size - 1){
				sb.append(CFG_SPLIT);
			}
		}
		return sb.toString();
	}

}
