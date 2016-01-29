package hc.core.util;

import hc.core.IConstant;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class StoreableHashMap extends Hashtable {

	private final String SPLIT = ";;;";
	private final String SPLIT_CHAR = ";";
	private final String FAN_SPLIT = "&#-1x";
	private final String EQUAL = "=";
	//用于value是数组型的
	protected final String ARRAY_SPLIT = "^";
	protected final String DOUBLE_ARRAY_SPLIT = ARRAY_SPLIT + ARRAY_SPLIT;

	public StoreableHashMap() {
		super();
	}

	public StoreableHashMap(int initialCapacity) {
		super(initialCapacity);
	}

	protected String getValueDefault(String key, String defaultValue) {
		String v = (String)get(key);
		return (v == null)?defaultValue:v;
	}

	public String toSerial() {
		if(size() == 0){
			return "";
		}
		
		StringBuffer sb = new StringBuffer();
		Enumeration en = this.keys();
		boolean isAppended = false;
		while(en.hasMoreElements()){
			if(isAppended){
				sb.append(SPLIT);
			}else{
				isAppended = true;
			}
			String key = (String)en.nextElement();
			String v = (String)get(key);
			sb.append(key);
			sb.append(EQUAL);
			final int splitIdx = v.indexOf(SPLIT_CHAR);
			if(splitIdx >= 0){
				v = replace(v, SPLIT_CHAR, FAN_SPLIT, 0);
			}
			sb.append(v);
		}
		return sb.toString();
	}
	
	private String replace(final String src, final String replaceFrom, final String replaceWith, int startIdx){
		final StringBuffer sb = new StringBuffer();
		int oldStartIdx = startIdx;
		startIdx = src.indexOf(replaceFrom, oldStartIdx);
		final int relaceLen = replaceFrom.length();
		while(startIdx >= 0){
			if(oldStartIdx < startIdx){
				sb.append(src.substring(oldStartIdx, startIdx));
			}
			sb.append(replaceWith);
			startIdx += relaceLen;
			oldStartIdx = startIdx;
			startIdx = src.indexOf(replaceFrom, oldStartIdx);
		}
		final int src_length = src.length();
		if(oldStartIdx < src_length){
			sb.append(src.substring(oldStartIdx, src_length));
		}
		return sb.toString();
	}

	public void restore(String serial) {
		if(serial == null || serial.length() == 0){
			return;
		}
		
		Vector lists = StringUtil.split(serial, SPLIT);
		final int size = lists.size();
		for (int i = 0; i < size; i++) {
			String item = (String)lists.elementAt(i);
			final int splitIdx = item.indexOf(EQUAL);
			String key = item.substring(0, splitIdx);
			String value = item.substring(splitIdx + 1);
			if(value.indexOf(FAN_SPLIT) >= 0){
				value = replace(value, FAN_SPLIT, SPLIT_CHAR, 0);
			}
			put(key, value);
		}
	}

	protected boolean isTrue(String key) {
		String v = (String)get(key);
		if(v == null){
			return false;
		}else if(v.equals(IConstant.TRUE)){
			return true;
		}else{
			return false;
		}
	}
	
}