package hc.server.util;

import hc.App;
import hc.util.PropertiesManager;

import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class IDArrayGroup {
	final String groupName;
	final ArrayList<String> members = new ArrayList<String>();
	private static final String SPLIT_STR = "#";
	
	public static final String MSG_ID_DESIGNER = "S1";
	public static final String MSG_ID_LOCK_SCREEN = "S2";
	public static final String MSG_NOTIFIED_SERVER_DIRECT_MODE = "S3";
	
	public static final void showMsg(final String msgId, String message, String title){
		if(checkAndAdd(msgId)){
    		
    		JPanel panel = new JPanel();
    		panel.add(new JLabel(message, App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.CENTER));
    		App.showCenterPanel(panel, 0, 0, title, false, null, null, null, null, null, true, false, null, false, false);
    	}
	}
	
	public IDArrayGroup(String groupName){
		this.groupName = groupName;
		
		String memStrs = PropertiesManager.getValue(groupName);
		try{
			if(memStrs != null && memStrs.length() > 0){
				String[] ids = memStrs.split(SPLIT_STR);
				for (int i = 0; i < ids.length; i++) {
					final String item = ids[i];
					if(isIn(item) == false) {
						members.add(item);
					}
				}
			}
		}catch (Exception e) {
		}
	}
	
	public boolean isIn(String id){
		return members.contains(id);
	}
	
	public void add(final String id){
		synchronized (IDArrayGroup.class) {
			if(isIn(id) == false) {
				members.add(id);
			}else{
				return;
			}
			
			StringBuffer sb = new StringBuffer();
			final int size = members.size();
			for (int i = 0; i < size; i++) {
				if(i != 0){
					sb.append(SPLIT_STR);
				}
				sb.append(members.get(i));
			}
			
			PropertiesManager.setValue(groupName, sb.toString());
			PropertiesManager.saveFile();
		}
	}

	public static boolean checkAndAdd(String msgID) {
		IDArrayGroup ia = new IDArrayGroup(PropertiesManager.p_ReadedMsgID);
		if(ia.isIn(msgID)){
			return false;
		}
		
		ia.add(msgID);
		return true;
	}
}
