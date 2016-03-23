package hc.server;

import hc.core.util.ExceptionReporter;
import hc.util.PropertiesManager;

import java.util.Vector;

public class ConfigValueGroup {
	final ConfigPane configPane;
	final Vector<ConfigValue> values = new Vector<ConfigValue>();
	boolean isCancelApply = false;

	ConfigValueGroup(final ConfigPane pane){
		this.configPane = pane;
	}
	
	public String getValueForApply(final String key){
		final int size = values.size();
		for (int i = 0; i < size; i++) {
			final ConfigValue cv = values.elementAt(i);
			if(cv.key != null && cv.key.equals(key)){
				if(isCancelApply == false){
					return cv.getNewValue();
				}else{
					return cv.getOldValue();
				}
			}
		}
		return null;
	}
	
	public final void doSaveUI(){
		try{
			applyAll(ConfigPane.OPTION_OK);
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		
		if(configPane.isNeedShutdownAndRestart){
			ConfigPane.rebuildConnection(configPane);
		}
		
		final int size = values.size();
		for (int i = 0; i < size; i++) {
			final ConfigValue cv = values.elementAt(i);
			final String key = cv.key;
			final String newValue = cv.getNewValue();
			if(key != null && newValue != null){
				PropertiesManager.setValue(key, newValue);
			}
		}
		PropertiesManager.saveFile();//注意：本逻辑要置于applyAll之后，因为applyAll有可以修改其它配置项
		
		dispose();
	}
	
	private void dispose(){
		values.removeAllElements();
		isCancelApply = false;
	}
	
	public void applyAll(final int option){
		final int size = values.size();
		for (int i = 0; i < size; i++) {
			values.elementAt(i).applyBiz(option);
		}
	}
	
	public void doCancel(){
		isCancelApply = true;
		
		final int size = values.size();
		for (int i = 0; i < size; i++) {
			final ConfigValue cv = values.elementAt(i);
			final String key = cv.key;
			final String oldValue = cv.getOldValue();
			if(key != null && oldValue != null){
				PropertiesManager.setValue(key, oldValue);
			}
		}
		
		applyAll(ConfigPane.OPTION_CANCEL);
		
		dispose();
	}
}
