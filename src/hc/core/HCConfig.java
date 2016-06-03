package hc.core;

import hc.core.util.StringUtil;

import java.util.Vector;

/**
 * 将数组值转为对应索引位来进行访问，故结构不许删除或变动。
 */
public class HCConfig {
	public static final String CFG_SPLIT = StringUtil.SPLIT_LEVEL_2_JING;

	Vector v;
	
	public HCConfig(String msg){
		refresh(msg);
	}
	
	public final void refresh(final String msg){
		v = StringUtil.split(msg, CFG_SPLIT);
	}

	public static String toTransString(String[] item){
		return StringUtil.toSerialBySplit(item);
	}
	
	/**
	 * 如果没有，返回0
	 * @param propertyID
	 * @return
	 */
	public final int getIntProperty(final short propertyID) {
		return getIntProperty(v, propertyID);
	}

	public static int getIntProperty(final Vector vector, final short propertyID) {
		if(vector == null){
			return 0;
		}
		final String obj = getProperty(vector, propertyID);
		if(obj == null){
			return 0;
		}
		return Integer.parseInt(obj);
	}
	
	public final boolean isTrue(final short propertyID) {
		return isTrue(v, propertyID);
	}

	private static boolean isTrue(final Vector vector, final short propertyID) {
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

	public final String getProperty(short propertyID) {
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
	
	public final void setPropertyNull(final short propertyID){
		setProperty(propertyID, null);
	}

	public final void setProperty(final short propertyID, final Object value){
		if(v == null){
			return;
		}
		if(v.size() <= propertyID){
			v.insertElementAt(value, propertyID);
		}else{
			v.setElementAt(value, propertyID);
		}
	}
	
	public final String toTransString(){
		final StringBuffer sb = new StringBuffer("");
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
