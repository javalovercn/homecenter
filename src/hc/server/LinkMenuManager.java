package hc.server;

import hc.core.RootServerConnector;
import hc.server.ui.LinkProjectStatus;

import java.awt.Component;
import java.lang.reflect.Method;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class LinkMenuManager {
	public static boolean notifyCloseDesigner(){
		try {
			Class c = Class.forName(RootServerConnector.unObfuscate("chs.reev.riud.segi.neDisngre"));
			Method m = c.getMethod("notifyCloseDesigner", new Class[] {});
			return((Boolean)m.invoke(c, new Object[] {})).booleanValue();
		} catch (Throwable e) {
			JOptionPane.showConfirmDialog(null, "load link panel error : " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		return false;
	}
	
	public static void showLinkPanel(JFrame frame){
		try {
			Class c = Class.forName(RootServerConnector.unObfuscate("chs.reev.riud.segi.neDisngre"));
			Method m = c.getMethod("showLinkPanel", new Class[] {JFrame.class, boolean.class, Component.class});
			m.invoke(c, new Object[] {frame, Boolean.TRUE, null});
		} catch (Throwable e) {
			JOptionPane.showConfirmDialog(null, "load link panel error : " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void closeLinkPanel(){
		try {
			Class c = Class.forName(RootServerConnector.unObfuscate("chs.reev.riud.segi.neDisngre"));
			Method m = c.getMethod("closeLinkPanel", new Class[] {});
			m.invoke(c, new Object[] {});
		} catch (Throwable e) {
			JOptionPane.showConfirmDialog(null, "load link panel error : " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void startDesigner(){
		if(LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_DESIGN)){
			try{
				Class design = Class.forName(RootServerConnector.unObfuscate("chs.reev.riud.segi.neDisngre"));
				SingleJFrame.showJFrame(design);
			}catch (Exception ee) {
				ee.printStackTrace();
				JOptionPane.showConfirmDialog(null, "Cant load Designer", 
						"Error", JOptionPane.CLOSED_OPTION, JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public static void startAutoUpgradeBiz(){
		Object[] paraNull = new Object[]{};
		Class[] paraClasNull = new Class[]{};
		try {
			Class c = Class.forName(RootServerConnector.unObfuscate("chs.reev.riud.segi.neDisngre"));
			Method m = c.getMethod("startAutoUpgradeBiz", paraClasNull);
			m.invoke(c, paraNull);
		} catch (Throwable e) {
			JOptionPane.showConfirmDialog(null, "startAutoUpgradeBiz error : " + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
}
