package hc.server.ui.design;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

public class BDNRobotList implements Serializable{

	private static final long serialVersionUID = 1L;
	
	public String projectID;
	public Vector<String> robotIDList = new Vector<String>();
	
	public BDNRobotList(final String projectID, final ArrayList<String> list) {
		this.projectID = projectID;
		final int size = list.size();
		
		for (int i = 0; i < size; i++) {
			robotIDList.add(list.get(i));
		}
	}

}
