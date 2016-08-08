package hc.util;

import hc.core.HCConfig;
import hc.core.util.StringUtil;

import java.util.Vector;

/**
 * for map, see PropertiesMap
 *
 */
public class PropertiesSet {
	private final String itemsPrefix;
	private final Vector<String> lists;
	
	private final String SPLIT = HCConfig.CFG_SPLIT;
	
	public PropertiesSet(final String itemPrefix) {
		this.itemsPrefix = itemPrefix + "Lists";
		
		final String v = PropertiesManager.getValue(itemsPrefix, "");
		lists = StringUtil.split(v, SPLIT);
	}
	
	public final void refill(final Object[] props){
		lists.removeAllElements();
		
		for (int i = 0; i < props.length; i++) {
			appendItemIfNotContains(props[i].toString());
		}
	}
	
	public final int size(){
		return lists.size();
	}
	
	/**
	 * 如果已存在，则返回，否则添加到最后。
	 * @param itemName
	 */
	public final void appendItemIfNotContains(final String itemName){
		if(lists.contains(itemName)){
			return;
		}
		lists.add(itemName);
	}
	
	public final int indexOf(final String itemName){
		return lists.indexOf(itemName);
	}
	
	public final boolean contains(final String itemName){
		return lists.contains(itemName);
	}
	
	public final void save(){
		final StringBuilder v = new StringBuilder();
		boolean isAppended = false;
		for (int i = 0; i < lists.size(); i++) {
			if(isAppended){
				v.append(SPLIT);
			}else{
				isAppended = true;
			}
			v.append(lists.elementAt(i));
		}
		
		PropertiesManager.setValue(itemsPrefix, v.toString());
		
		PropertiesManager.saveFile();
	}
	
	public final String getItem(final int idx){
		return lists.elementAt(idx);
	}
	
	public final void updateItem(final int idx, final String value){
		lists.setElementAt(value, idx);
	}
	
	public final void delItem(final int idx){
		lists.remove(idx);
	}

	public final void delItem(final String itemName){
		lists.remove(itemName);
	}
}
