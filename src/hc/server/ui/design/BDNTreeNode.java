package hc.server.ui.design;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import hc.server.msb.DeviceBindInfo;

public class BDNTreeNode implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public BindDeviceNode projectNode;
	public Vector<DeviceBindInfo[]> refList = new Vector<DeviceBindInfo[]>();
	public Vector<BindDeviceNode> bdnForRobot = new Vector<BindDeviceNode>();
	public Vector<BindDeviceNode[]> devBelowRobotList = new Vector<BindDeviceNode[]>();
	
	public final void addRefList(final ArrayList<DeviceBindInfo> list) {
		final int size = list.size();
		final DeviceBindInfo[] array = new DeviceBindInfo[size];
		
		for (int i = 0; i < size; i++) {
			array[i] = list.get(i);
		}
		
		refList.add(array);
	}
	
	public final void addBDNForRobot(final BindDeviceNode bdn) {
		bdnForRobot.add(bdn);
	}
	
	public final void addDevBelowRobot(final ArrayList<BindDeviceNode> devList) {
		final int size = devList.size();
		final BindDeviceNode[] array = new BindDeviceNode[size];
		
		for (int i = 0; i < size; i++) {
			array[i] = devList.get(i);
		}
		
		devBelowRobotList.add(array);
	}
}
