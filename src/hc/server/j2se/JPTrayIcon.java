package hc.server.j2se;

import hc.App;
import hc.ITrayIcon;
import hc.PlatformTrayIcon;
import hc.WindowTrayIcon;
import hc.core.ContextManager;
import hc.server.util.HCJFrame;
import hc.util.ResourceUtil;
import hc.util.UILang;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.TrayIcon.MessageType;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class JPTrayIcon implements PlatformTrayIcon{
	private final ThreadGroup threadGroupToken = App.getThreadPoolToken();
	private HashMap<String, String> tip = new HashMap<String, String>();
	
	final ITrayIcon trayIcon;
	
	public static final String NAT_DESC = "Nat_Desc";
	public static final String PUBLIC_IP = "Pub_IP";
//	public static final String PUBLIC_PORT = "Pub_Port";
	
	private JPopupMenu menu;
	private final HCJFrame dialog = new HCJFrame();
	final Runnable toDisvisibleRun = new Runnable() {
		@Override
		public void run() {
			dialog.setVisible(false);						
		}
	};
	private final PopupMenuListener popupListener;
	private final MouseListener mouseListener;
	private String toolTip;
	
	{
		dialog.setUndecorated(true);
		dialog.setAlwaysOnTop(true);
	}

	public void exit() {
		dialog.dispose();
	}
	
	public void putTip(String key, String value){
		tip.put(key, value);
		
		//重算ToolTip
		String toolTip = "";
		
		Iterator<String> it = tip.keySet().iterator();
		while(it.hasNext()){
			if(toolTip.length() > 0){
				toolTip += "\n";
			}
			toolTip += tip.get(it.next());
		}
		
		trayIcon.setToolTip(toolTip);
	}
	
	public String getToolTip(){
		return this.toolTip;
	}
	
	public void setToolTip(String tooltip){
		this.toolTip = tooltip;
		trayIcon.setToolTip(tooltip);
	}
	
	public void remove(){
		trayIcon.removeTray();
	}
	
	public void setImage(Image image){
		trayIcon.setImage(image);
	}
	
	public Image getImage(){
		return trayIcon.getImage();
	}
	
	private ITrayIcon buildTrayIcon(Image image){
//		if(ResourceUtil.isWindowsOS()){
//			return new WindowTrayIcon();
//		}else{
//			return new LinuxTrayIcon(image);
			
//			if(ResourceUtil.isLinuxRelease("fedora")){
//				return new LinuxTrayIcon(image);
////			}else if(ResourceUtil.isLinuxRelease("centos")){
////					return new WindowTrayIcon();
////			}else if(ResourceUtil.isLinuxRelease("ubuntu")){
////				return new WindowTrayIcon();
//			}else if(ResourceUtil.isMacOSX()){10.9由于提示消息不正常
//				return new LinuxTrayIcon(image);
//			}else{
//				return new WindowTrayIcon();
//			}
//		}
		return new WindowTrayIcon();
	}

	public JPTrayIcon(final Image image, String toolTip, final JPopupMenu menu) {
		trayIcon = buildTrayIcon(image);
		
		//要置于setImage之前
		trayIcon.setImageAutoSize(true);
		
		trayIcon.setImage(image);
		this.toolTip = toolTip;
		trayIcon.setToolTip(toolTip);
		dialog.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowLostFocus(WindowEvent e) {
//				L.V = L.O ? false : LogManager.log("Dialog Lose Focus");
				App.invokeLaterUI(toDisvisibleRun);
			}
			
			@Override
			public void windowGainedFocus(WindowEvent e) {
			}
		});
		popupListener = new TrayPopupListener(dialog);
		mouseListener = new TrayMouseAdapter();
		this.setJPopupMenu(menu);
		
		setToolTip("HomeCenter");
//		
		trayIcon.showTray();
//		Locale l = Locale.getDefault();
//		menu.applyComponentOrientation(ComponentOrientation.getOrientation(l));
	}
	
	public void displayMessage(final String caption, final String text, final MessageType messageType){
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				App.invokeAndWaitUI(new Runnable() {
					@Override
					public void run() {
						trayIcon.displayMessage(caption, text, messageType);
					}
				});
			}
		},threadGroupToken);
	}
	
	public void setDefaultActionListener(ActionListener listen){
		trayIcon.setDefaultActionListener(listen);
	}

	/**
	 * 
	 * @return
	 */
	public final JPopupMenu getJPopupMenu() {
		return menu;
	}

	/**
	 * 
	 * @param popmenu
	 */
	private final void setJPopupMenu(final JPopupMenu popmenu) {
		popmenu.applyComponentOrientation(ComponentOrientation.getOrientation(UILang.getUsedLocale()));
		
		if (this.menu != null) {
			this.menu.removePopupMenuListener(popupListener);
			trayIcon.removeTrayMouseListener(mouseListener);
		}
		if (popmenu != null) {
			this.menu = popmenu;
			this.menu.addPopupMenuListener(popupListener);
			trayIcon.addTrayMouseListener(mouseListener);
		}
	}

	private final class TrayMouseAdapter extends MouseAdapter {

		private void showJPopupMenu(final MouseEvent evt) {
			App.invokeLaterUI(new Runnable() {
				@Override
				public void run() {
					final Dimension screenSize = ResourceUtil.getScreenSize();
					int loc_x = evt.getXOnScreen();
					int loc_y = evt.getYOnScreen();
					
					dialog.setLocation(evt.getX(), evt.getY()
								- menu.getPreferredSize().height);
					dialog.setVisible(true);
					menu.show(dialog.getContentPane(), 0, 0);
					// popup works only for focused windows

					if (menu.getWidth() == 0) {
						menu.setVisible(true);
						menu.setVisible(false);
					}

					int w = menu.getWidth();
					int h = menu.getHeight();

					if (loc_x + w > screenSize.width) {
						loc_x = loc_x - w;
					}

					if(loc_y + h > screenSize.height){
						loc_y = loc_y - h;
					}
					menu.setLocation(loc_x, loc_y);
					menu.setVisible(true);
					menu.setInvoker(dialog.getContentPane());

					dialog.toFront();
				}
			});
		}

		public void mousePressed(final MouseEvent evt) {
//			showJPopupMenu(evt);
		}

		public void mouseReleased(final MouseEvent evt) {
			if (menu != null) {
				if(evt.isPopupTrigger() ||
						(evt.getButton() == MouseEvent.BUTTON3 && evt.getClickCount() == 1)){
					showJPopupMenu(evt);
				}
			}
		}

		public void mouseClicked(final MouseEvent evt) {
//			showJPopupMenu(evt);
		}
	}

	private class TrayPopupListener implements PopupMenuListener {
		Window dialog;

		TrayPopupListener(Window dialog) {
			this.dialog = dialog;
		}

		@Override
		public void popupMenuWillBecomeVisible(final PopupMenuEvent evt) {
			// not used
		}

		@Override
		public void popupMenuWillBecomeInvisible(final PopupMenuEvent evt) {
//			L.V = L.O ? false : LogManager.log("popupMenuWillBecomeInvisible");
			//必须的，该逻辑是有用的。
			dialog.setVisible(false);
		}

		@Override
		public void popupMenuCanceled(final PopupMenuEvent evt) {
//			L.V = L.O ? false : LogManager.log("popupMenuCanceled");
			//必须的，该逻辑是有用的。
			dialog.setVisible(false);
		}
	}
}
