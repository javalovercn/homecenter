package hc.server.msb;

public final class RobotEvent {
	Robot source;
	String propertyName;
	Object oldValue;
	Object newValue;
	
	/**
	 * If there are multiple {@link Robot}(s) in current project, it is used to distinguish between different sources.
	 * @return the event source of {@link Robot}.
	 */
	public final Robot getSource(){
		return source;
	}
	
	/**
	 * 
	 * @return the property name of event.
	 */
	public final String getPropertyName(){
		return propertyName;
	}
	
	/**
	 * 
	 * @return the old value of property of current event. Maybe null.
	 */
	public final Object getOldValue(){
		return oldValue;
	}
	
	/**
	 * 
	 * @return the new value of property of current event.
	 */
	public final Object getNewValue(){
		return newValue;
	}
	
	final void clear(){
		source = null;
		propertyName = null;
		oldValue = null;
		newValue = null;
	}
}
