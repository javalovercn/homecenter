package hc.server.data;

import hc.util.ResourceUtil;

import javax.swing.JPanel;

public class DataEditorPanel extends JPanel {
	public void notifySave(){
		ResourceUtil.notifySave();
	}
	
	public void notifyCancle(){
		ResourceUtil.notifyCancel();
	}
}
