package hc.server.ui.design;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

public class BDNTree implements Serializable{

	private static final long serialVersionUID = 1L;

	final Vector<String> projectList = new Vector<String>();
	final Vector<BDNRobotList> bdnRobotList = new Vector<BDNRobotList>();
	final Vector<BDNTreeNode> treeNodeList = new Vector<BDNTreeNode>();
	
	public final void addProjectAndRobotList(final String projectID, final ArrayList<String> robotList, final BDNTreeNode node) {
		projectList.add(projectID);
		
		bdnRobotList.add(new BDNRobotList(projectID, robotList));
		
		treeNodeList.add(node);
	}
}
