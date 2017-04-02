package hc.server.msb;

/**
 * A semantic event which indicates that a Robot-defined event occurred.
 */
public final class RobotEvent {
	RobotWrapper source;
	String propertyName;
	Object oldValue;
	Object newValue;
	
	/**
	 * return the event source of {@link Robot}.
	 * @return 
	 */
	public final Robot getSource(){
		return source;
	}
	
	/**
	 * return the property name of event.
	 * @return 
	 */
	public final String getPropertyName(){
		return propertyName;
	}
	
	/**
	 * return the old value of property. Maybe null.
	 * @return 
	 */
	public final Object getOldValue(){
		return oldValue;
	}
	
	/**
	 * return the new value of property.
	 * @return 
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
