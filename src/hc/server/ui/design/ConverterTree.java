package hc.server.ui.design;

import java.io.Serializable;
import java.util.Vector;

public class ConverterTree implements Serializable {

	private static final long serialVersionUID = 1L;

	Vector<BindDeviceNode> projectList = new Vector<BindDeviceNode>();
	Vector<BindDeviceNode> converterList = new Vector<BindDeviceNode>();
	
	public final void addProjectNode(final BindDeviceNode bdn) {
		projectList.add(bdn);
	}
	
	public final void addConverterNode(final BindDeviceNode bdn) {
		converterList.add(bdn);
	}
}
