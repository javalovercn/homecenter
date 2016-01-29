package hc.util;

import hc.core.HCConfig;
import hc.core.util.StringUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class PropertiesMap extends HashMap<String, String>{
	private final String itemsPrefix;
	
	private final String SPLIT = HCConfig.CFG_SPLIT;
	private final String EQUAL = "=";
	
	public PropertiesMap(String itemType) {
		this.itemsPrefix = itemType;//不使用扩展字，因为工程删除时，需要使用键值。注意：此处与PropertiesSet的区别
		
		String v = PropertiesManager.getValue(itemsPrefix, "");
		Vector<String> lists = StringUtil.split(v, SPLIT);
		final int size = lists.size();
		for (int i = 0; i < size; i++) {
			String item = (String)lists.elementAt(i);
			final int splitIdx = item.indexOf(EQUAL);
			String key = item.substring(0, splitIdx);
			String value = item.substring(splitIdx + 1);
			put(key, value);
		}
	}
	
	public final synchronized void save(){
		if(size() == 0){
			return;
		}
		
		StringBuilder v = new StringBuilder();
		Iterator<String> it = this.keySet().iterator();
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
		
		PropertiesManager.setValue(itemsPrefix, v.toString());
		
		PropertiesManager.saveFile();
	}
}