package hc.server.msb;

/**
 * a description about compatible of a device.
 * 
 * @see Robot#getDeviceCompatibleDescription(String)
 * @see Device#getDeviceCompatibleDescription()
 * @see Converter#getUpDeviceCompatibleDescription()
 * @see Converter#getDownDeviceCompatibleDescription()
 */
public abstract class DeviceCompatibleDescription {
	/**
	 * return the description about <code>Device</code>
	 * 
	 * @return
	 * @see #getCompatibleStringList()
	 * @see #getVersion()
	 * @since 7.0
	 */
	public abstract String getDescription();

	String[] compItems;

	final String[] getCompatibleItem() {
		if (compItems == null) {
			compItems = DeviceMatchManager.getMatchItemFromDesc(getCompatibleStringList());
		}
		return compItems;
	}

	/**
	 * for example, return "\"Sun AirCond Type1\", sun-aircond-type2". <BR>
	 * <BR>
	 * <STRONG>Important : </STRONG> <BR>
	 * 1. "Sun_AirCond_Type1", "Sun-AirCond-Type1" and "SUN AirCond Type1" are treated as equivalent
	 * for auto-bind.
	 * 
	 * @return the compatible type/model list of this <code>Device</code>
	 * @see Robot#getDeviceCompatibleDescription(String)
	 * @see Device#getDeviceCompatibleDescription()
	 * @see Converter#getUpDeviceCompatibleDescription()
	 * @see Converter#getDownDeviceCompatibleDescription()
	 * @since 7.0
	 */
	public abstract String getCompatibleStringList();

	/**
	 * return the version about the <code>Device</code>
	 * 
	 * @return
	 * @see #getDescription()
	 * @see #getCompatibleStringList()
	 * @since 7.0
	 */
	public abstract String getVersion();
}
