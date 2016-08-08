package hc.util;

import hc.core.HCConfig;
import hc.core.util.StringUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * for Set, please see PropertiesSet
 *
 */
public class PropertiesMap extends HashMap<String, String>{
	private final String itemsPrefix;
	
	public static final String SPLIT = HCConfig.CFG_SPLIT;
	public static final String EQUAL = "=";
	
	public PropertiesMap(final String itemType) {
		this.itemsPrefix = itemType;//不使用扩展字，因为工程删除时，需要使用键值。注意：此处与PropertiesSet的区别
		
		final String v = PropertiesManager.getValue(itemsPrefix, "");
		final Vector<String> lists = StringUtil.split(v, SPLIT);
		final int size = lists.size();
		for (int i = 0; i < size; i++) {
			final String item = lists.elementAt(i);
			final int splitIdx = item.indexOf(EQUAL);
			final String key = item.substring(0, splitIdx);
			final String value = item.substring(splitIdx + 1);
			put(key, value);
		}
	}
	
	public final synchronized void save(){
		String serialStr;
		
		if(size() == 0){
			serialStr = "";
		}else{
			final StringBuilder v = new StringBuilder();
			final Iterator<String> it = this.keySet().iterator();
			boolean isFirst = true;
			while(it.hasNext()){
				if(isFirst){
					isFirst = false;
				}else{
					v.append(SPLIT);
				}
				final String key = it.next();
				v.append(key + EQUAL + get(key));
			}
			
			serialStr = v.toString();
		}
		
		PropertiesManager.setValue(itemsPrefix, serialStr);
		
		PropertiesManager.saveFile();
	}
}