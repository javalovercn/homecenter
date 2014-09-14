package hc.util;

import hc.core.HCConfig;
import hc.core.util.StringUtil;

import java.util.Vector;

public class PropertiesSet {
	private final String itemsPrefix;
	private final Vector<String> lists;
	
	private final String SPLIT = HCConfig.CFG_SPLIT;
	
	public PropertiesSet(String itemType) {
		this.itemsPrefix = itemType + "Lists";
		
		String v = PropertiesManager.getValue(itemsPrefix, "");
		lists = StringUtil.split(v, SPLIT);
	}
	
	public void refill(Object[] props){
		lists.removeAllElements();
		
		for (int i = 0; i < props.length; i++) {
			lists.add(props[i].toString());
		}
	}
	
	public int size(){
		return lists.size();
	}
	
	public void appendItem(String itemName){
		lists.add(itemName);
	}
	
	public boolean contains(String itemName){
		return lists.contains(itemName);
	}
	
	public void save(){
		StringBuilder v = new StringBuilder();
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
	
	public String getItem(int idx){
		return lists.elementAt(idx);
	}
	
	public void updateItem(int idx, String value){
		lists.setElementAt(value, idx);
	}
	
	public void delItem(String itemName){
		lists.remove(itemName);
	}
}
