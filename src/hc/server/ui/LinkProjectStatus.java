package hc.server.ui;

import hc.App;
import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.LogManager;
import hc.core.util.Stack;
import hc.core.util.StringUtil;
import hc.server.HCActionListener;
import hc.server.JRubyInstaller;
import hc.server.LinkMenuManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;

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
	public static final int MANAGER_ADD_HAR_VIA_MOBILE = 6;

	private static int manager_status = MANAGER_IDLE;
	
	private static final Stack stack = new Stack();
	
	public static int getStatus(){
		return manager_status;
	}
	
	private static void setStatus(final int status){
		stack.push(new Integer(manager_status));
		manager_status = status;
		LogManager.log("set project lock status : " + manager_status);
	}
	
	public static synchronized void exitStatus(){
		final int oldStatus = manager_status;
		final Object pop = stack.pop();
		if(pop != null){
			manager_status = (Integer)pop;
			LogManager.log("return project lock status : " + manager_status + ", from : " + oldStatus);
		}
	}
	
	public static synchronized boolean isIdle(){
		return manager_status == MANAGER_IDLE;
	}
	
	public final static void resetWantDesignOrLinkProjectsNotify(){
		isWantDesingOrLinkProjects = false;
	}
	
	public final static boolean isWantDesignOrLinkProjectsNotify(){
		return isWantDesingOrLinkProjects;
	}
	
	private static boolean isWantDesingOrLinkProjects;
	
	/**
	 * 检查当前状态是否可以进行发布，Link-in Project的添加或维护
	 * return true表示可以进行后续操作
	 */
	public static synchronized boolean tryEnterStatus(final JFrame parent, final int toStatus){
		if(isIdle() == false){
			if(manager_status != toStatus){
				if(toStatus == MANAGER_UPGRADE_DOWNLOADING){
					//无需提示的优先
					return false;
				}
				
				if(manager_status == MANAGER_UPGRADE_DOWNLOADING){
					isWantDesingOrLinkProjects = true;
					showNotify(parent, (String)ResourceUtil.get(9161), IContext.INFO);
					return false;
				}else if(manager_status == MANAGER_IMPORT){
					
					final String replaced = isOpend((String)ResourceUtil.get(9059));
	
					final JLabel label = new JLabel("<html>" + replaced + "</html>", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING);
					final JPanel panel = new JPanel(new BorderLayout());
					panel.add(label, BorderLayout.CENTER);
					
					App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(9086), true, null, null, new HCActionListener(new Runnable() {
						@Override
						public void run() {
							LinkMenuManager.closeLinkPanel();
							try{
								Thread.sleep(300);
							}catch (final Exception e) {
							}
							LinkMenuManager.startDesigner(true);
						}
					}, App.getThreadPoolToken()), null, null, false, false, null, false, false);//isNewFrame=true时，会在MacOSX下发生漂移
					return false;
				}else if(manager_status == MANAGER_DESIGN){
					if(toStatus == MANAGER_IMPORT && parent != null){
						//仅放行从设计器调用工程选择器
					}else{
						final String replaced = isOpend((String)ResourceUtil.get(9034));
						
						final JLabel label = new JLabel("<html>" + replaced + "</html>", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING);
						final JPanel panel = new JPanel(new BorderLayout());
						panel.add(label, BorderLayout.CENTER);
						
						App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(9086), true, null, null, new HCActionListener(new Runnable() {
							@Override
							public void run() {
								if(LinkMenuManager.notifyCloseDesigner()){
									try{
										Thread.sleep(300);
									}catch (final Exception e) {
									}
									LinkMenuManager.showLinkPanel(null);
								}
							}
						}, App.getThreadPoolToken()), null, null, false, false, null, false, false);//isNewFrame=true时，会在MacOSX下发生漂移
						return false;
					}
				}else if(manager_status == MANAGER_JRUBY_INSTALL){
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							JRubyInstaller.showProgressWindow(parent);
						}
					});
					return false;
				}else if(manager_status == MANAGER_ADD_HAR_VIA_MOBILE){
					showNotify(parent, "adding HAR from mobile now, please wait for a moment.", IContext.ERROR);
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

	private static String isOpend(final String winName) {
		final String isOpend = (String)ResourceUtil.get(9085);
				
		String replaced = StringUtil.replace(isOpend, "{win_name}", winName);
		replaced = StringUtil.replace(replaced, "{ok}", (String) ResourceUtil.get(IContext.OK));
		return replaced;
	}

	private static void showNotify(final JFrame parent, final String msg, final int type) {
		final JLabel label = new JLabel(msg, App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING);
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(label, BorderLayout.CENTER);
		
		App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(type), 
				false, null, null, null, null, parent, true, true, null, false, false);
	}

}
