package hc.server.msb;

/**
 * @see Device#getDeviceCompatibleDescription()
 * @see Robot#getDeviceCompatibleDescription(String)
 * @see Converter#getUpDeviceCompatibleDescription()
 * @see Converter#getDownDeviceCompatibleDescription()
 */
public abstract class DeviceCompatibleDescription {
	/**
	 * @return the description of {@link Device}
	 * @see #getCompatibleStringList()
	 * @see #getVersion()
	 * @since 7.0
	 */
	public abstract String getDescription();
	
	String[] compItems;
	
	final String[] getCompatibleItem(){
		if(compItems == null){
			compItems = DeviceMatchManager.getMatchItemFromDesc(getCompatibleStringList());
		}
		return compItems;
	}
	
	/**
	 * for example, two lists:
	 * <BR>1. "<STRONG>Sun_AirCond_Type1 , Sun_AirCond_Type2</STRONG>"
	 * <BR>2. "<STRONG>\"Sun AirCond Type1\" , sun-aircond-type2</STRONG>"
	 * <BR><BR><STRONG>Important : </STRONG>
	 * <BR>1. each item of device model is used for device auto match when binding, so you do not need to remember the mutual compatibility of equipment.
	 * <BR>2. "Sun_AirCond_Type1", "Sun-AirCond-Type1" and "SUN AirCond Type1" are same models for auto-bind, case-insensitive.
	 * @return the compatible description list of real type device(s) that are supported by this {@link Device}
	 * @see Robot#getDeviceCompatibleDescription(String)
	 * @see Converter#getUpDeviceCompatibleDescription()
	 * @see Converter#getDownDeviceCompatibleDescription()
	 * @see Device#getDeviceCompatibleDescription()
	 * @since 7.0
	 */
	public abstract String getCompatibleStringList();
	
	/**
	 * @return the version of {@link Device}
	 * @see #getDescription()
	 * @see #getCompatibleStringList()
	 * @since 7.0
	 */
	public abstract String getVersion();
}
