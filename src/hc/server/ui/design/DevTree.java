package hc.server.ui.design;

import java.io.Serializable;
import java.util.Vector;

public class DevTree implements Serializable {

	private static final long serialVersionUID = 1L;

	Vector<BindDeviceNode> projectList = new Vector<BindDeviceNode>();
	Vector<BindDeviceNode> devList = new Vector<BindDeviceNode>();
	Vector<BindDeviceNode> realDevList = new Vector<BindDeviceNode>();
	
	public final void addProjectNode(final BindDeviceNode bdn) {
		projectList.add(bdn);
	}
	
	public final void addDevNode(final BindDeviceNode bdn) {
		devList.add(bdn);
	}
	
	public final void addRealDevNode(final BindDeviceNode bdn) {
		realDevList.add(bdn);
	}
}
