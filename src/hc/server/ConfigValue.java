package hc.server;


public abstract class ConfigValue {
	final ConfigValueGroup group;
	
	String key;
	private String oldValue;
	
	public ConfigValue(String configKey, String old, ConfigValueGroup group){
		this.group = group;
		this.key = configKey;
		this.oldValue = old;
		this.group.values.add(this);
	}
	
	public boolean isChanged(){
		return !(getNewValue().equals(oldValue));
	}
	
	public String getOldValue(){
		return oldValue;
	}
	
	public abstract String getNewValue();
	
	/**
	 * 应用新值的实现逻辑。
	 * <BR>注意：取新值只能通过group.getValueForApply，而不能调用getNewValue。禁止本方法内调用getNewValue
	 * @param isCancel
	 */
	public abstract void applyBiz(boolean isCancel);
}
