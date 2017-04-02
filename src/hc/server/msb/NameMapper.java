package hc.server.msb;

import hc.server.ui.design.LinkProjectStore;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NameMapper {
	//注意：如果增加，请clear在reloadMap。因为可能运行时，加载工程(appendBindToNameSet)，所以必须ConcurrentHashMap
	public ConcurrentHashMap<String, HashMap<String, HashMap<String, Vector<String>>>> searchBindIDFromDevice = new ConcurrentHashMap<String, HashMap<String,HashMap<String,Vector<String>>>>();
	public ConcurrentHashMap<String, RealDeviceInfo> bind2RealDeviceBindInfo = new ConcurrentHashMap<String, RealDeviceInfo>();
	public ConcurrentHashMap<String, ConverterInfo> bind2ConverterBindInfo = new ConcurrentHashMap<String, ConverterInfo>();
	public ConcurrentHashMap<String, RobotReferBindInfo> bind2ReferID = new ConcurrentHashMap<String, RobotReferBindInfo>();
	
	public abstract void appendBindToNameSet(final LinkProjectStore lps);
	
	public abstract WorkingDeviceList getWorkingDeviceList(final String projectID);
	
	public abstract void reloadMap();
}
