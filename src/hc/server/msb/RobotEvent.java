package hc.server.msb;

/**
 * A semantic event which indicates that a Robot-defined event occurred.
 */
public final class RobotEvent {
	RobotWrapper sourceWrapper;
	String propertyName;
	Object oldValue;
	Object newValue;
	
	RobotEvent(){//Not visible to JavaDoc
	}

	/**
	 * return the event source wrapper of {@link Robot}.
	 * @return 
	 */
	public final Robot getSource(){
		return sourceWrapper;
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
		sourceWrapper = null;
		propertyName = null;
		oldValue = null;
		newValue = null;
	}
}
