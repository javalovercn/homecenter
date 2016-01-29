package hc.server.msb;

import hc.server.ui.design.LinkProjectStore;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NameMapper {
	//因为可能运行时，加载工程，所以必须线程安全
	public final ConcurrentHashMap<String, HashMap<String, HashMap<String, Vector<String>>>> searchBindIDFromDevice = new ConcurrentHashMap<String, HashMap<String,HashMap<String,Vector<String>>>>();
	public final ConcurrentHashMap<String, RealDeviceInfo> bind2RealDeviceBindInfo = new ConcurrentHashMap<String, RealDeviceInfo>();
	public final ConcurrentHashMap<String, ConverterInfo> bind2ConverterBindInfo = new ConcurrentHashMap<String, ConverterInfo>();
	public final ConcurrentHashMap<String, RobotReferBindInfo> bind2ReferID = new ConcurrentHashMap<String, RobotReferBindInfo>();
	
	public abstract void appendBindToNameSet(final LinkProjectStore lps);
}
