package hc.server.ui.design;

import hc.server.msb.Device;
import hc.server.msb.DeviceCompatibleDescription;

public class DeviceAndExt {
	public final Device device;
	public DeviceCompatibleDescription deviceCompatibleDescription;
	
	public DeviceAndExt(final Device device) {
		this.device = device;
	}
}
