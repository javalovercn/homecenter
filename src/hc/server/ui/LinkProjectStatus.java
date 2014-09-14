package hc.server.ui;

import hc.App;
import hc.core.IContext;
import hc.core.util.Stack;
import hc.server.JRubyInstaller;
import hc.server.LinkMenuManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;


public class LinkProjectStatus {
	public static final int MANAGER_IDLE = 1;
	public static final int MANAGER_UPGRADE_DOWNLOADING = 2;
	public static final int MANAGER_DESIGN = 3;
	public static final int MANAGER_IMPORT = 4;
	public static final int MANAGER_JRUBY_INSTALL = 5;

	private static int manager_status = MANAGER_IDLE;
	
	private static Stack stack = new Stack();
	
	private static void setStatus(final int status){
		stack.push(new Integer(manager_status));
		manager_status = status;
//		L.V = L.O ? false : LogManager.log("curr status : " + manager_status);
	}
	
	public static void exitStatus(){
		manager_status = (Integer)stack.pop();
//		L.V = L.O ? false : LogManager.log("after exit status : " + manager_status);
	}
	
	public static boolean isIdle(){
		return manager_status == MANAGER_IDLE;
	}
	
	/**
	 * 检查当前状态是否可以进行发布，Link-in Project的添加或维护
	 * return true表示可以进行后续操作
	 */
	public static synchronized boolean tryEnterStatus(JFrame parent, final int toStatus){
		if(isIdle() == false){
			if(manager_status != toStatus){
				if(toStatus == MANAGER_UPGRADE_DOWNLOADING){
					//无需提示的优先
					return false;
				}
				
				if(manager_status == MANAGER_UPGRADE_DOWNLOADING){
					showNotify(parent, "system is downloading and upgrading project(s), please wait for a moment.", IContext.ERROR);
					return false;
				}else if(manager_status == MANAGER_IMPORT){
					JLabel label = new JLabel("<html>Window [" + (String)ResourceUtil.get(9059) + "] is open and maintenance now!" +
							"<BR>click '" + (String) ResourceUtil.get(IContext.OK) + "' to close it.</html>", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING);
					JPanel panel = new JPanel(new BorderLayout());
					panel.add(label, BorderLayout.CENTER);
					
					App.showCenterPanel(panel, 0, 0, "close window?", true, null, null, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							LinkMenuManager.closeLinkPanel();
							LinkMenuManager.startDesigner();
						}
					}, null, null, false, false, null, false, false);//isNewFrame=true时，会在MacOSX下发生漂移
					return false;
				}else if(manager_status == MANAGER_DESIGN){
					if(toStatus == MANAGER_IMPORT && parent != null){
						//仅放行从设计器调用工程选择器
					}else{
						JLabel label = new JLabel("<html>[" + (String)ResourceUtil.get(9034) + "] open and designing now!" +
								"<BR>click '" + (String) ResourceUtil.get(IContext.OK) + "' to close it.</html>", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING);
						JPanel panel = new JPanel(new BorderLayout());
						panel.add(label, BorderLayout.CENTER);
						
						App.showCenterPanel(panel, 0, 0, "close window?", true, null, null, new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								if(LinkMenuManager.notifyCloseDesigner()){
									LinkMenuManager.showLinkPanel(null);
								}
							}
						}, null, null, false, false, null, false, false);//isNewFrame=true时，会在MacOSX下发生漂移
						return false;
					}
				}else if(manager_status == MANAGER_JRUBY_INSTALL){
					JRubyInstaller.needNotify();
					showNotify(parent, "<html>please wait for a moment, system is downloading JRuby engine." +
//							"<br>if we have finished, a notify window will display." +
							"</html>", 
							IContext.INFO);
					return false;
				}else{
					return false;
				}
			}else{
				if(toStatus == MANAGER_UPGRADE_DOWNLOADING){
					return false;
				}else{
//					showNotify(parent, "current window is opened!", IContext.ERROR);
					//可视窗口已打开，无需更新状态，由后续逻辑将相应窗口toFront
					return true;
				}
			}
		}
		setStatus(toStatus);
		return true;
	}

	private static void showNotify(JFrame parent, String msg, int type) {
		JLabel label = new JLabel(msg, App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING);
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(label, BorderLayout.CENTER);
		
		App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(type), 
				false, null, null, null, null, parent, true, true, null, false, false);
	}

}
