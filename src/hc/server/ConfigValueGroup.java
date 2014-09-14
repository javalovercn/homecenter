package hc.server;

import hc.util.PropertiesManager;

import java.util.Vector;

public class ConfigValueGroup {
	final Vector<ConfigValue> values = new Vector<ConfigValue>();
	boolean isCancelApply = false;

	public String getValueForApply(String key){
		int size = values.size();
		for (int i = 0; i < size; i++) {
			ConfigValue cv = values.elementAt(i);
			if(cv.key.equals(key)){
				if(isCancelApply == false){
					return cv.getNewValue();
				}else{
					return cv.getOldValue();
				}
			}
		}
		return null;
	}
	
	public void doSave(){
		applyAll();
		
		int size = values.size();
		for (int i = 0; i < size; i++) {
			ConfigValue cv = values.elementAt(i);
			final String key = cv.key;
			final String newValue = cv.getNewValue();
			if(key != null && newValue != null){
				PropertiesManager.setValue(key, newValue);
			}
		}
		PropertiesManager.saveFile();
		
		dispose();
	}
	
	private void dispose(){
		values.removeAllElements();
		isCancelApply = false;
	}
	
	public void applyAll(){
		int size = values.size();
		for (int i = 0; i < size; i++) {
			values.elementAt(i).applyBiz(isCancelApply);
		}
	}
	
	public void doCancel(){
		isCancelApply = true;
		applyAll();
		dispose();
	}
}
