package hc.server.msb;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import hc.server.ui.design.LinkProjectStore;

public abstract class NameMapper {
	// 注意：如果增加，请clear在reloadMap。因为可能运行时，加载工程(appendBindToNameSet)，所以必须ConcurrentHashMap
	public ConcurrentMap<String, HashMap<String, HashMap<String, Vector<String>>>> searchBindIDFromDevice = new ConcurrentHashMap<String, HashMap<String, HashMap<String, Vector<String>>>>();
	public ConcurrentMap<String, RealDeviceInfo> bind2RealDeviceBindInfo = new ConcurrentHashMap<String, RealDeviceInfo>();
	public ConcurrentMap<String, ConverterInfo> bind2ConverterBindInfo = new ConcurrentHashMap<String, ConverterInfo>();
	public ConcurrentMap<String, RobotReferBindInfo> bind2ReferID = new ConcurrentHashMap<String, RobotReferBindInfo>();

	public abstract void appendBindToNameSet(final LinkProjectStore lps);

	public abstract WorkingDeviceList getWorkingDeviceList(final String projectID);

	public abstract void reloadMap();
}
