package hc.server.msb;

import java.util.Vector;

public class WorkingDeviceList {
	public static final WorkingDeviceList ALL_DEVICES = new WorkingDeviceList();

	final Vector<String> deviceNameList;

	public WorkingDeviceList() {
		deviceNameList = new Vector<String>(0);
	}

	public WorkingDeviceList(final Vector<String> list) {
		deviceNameList = list;
	}

	public final boolean contain(final String deviceName) {
		return deviceNameList.contains(deviceName);
	}
}
