package hc;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

public class WindowTrayIcon implements ITrayIcon {
	TrayIcon tray;
	boolean autosize = false;
	
	public WindowTrayIcon() {
	}
	
	@Override
	public void setToolTip(String tooltip) {
		tray.setToolTip(tooltip);
	}

	@Override
	public void setImage(Image image) {
		if(tray == null){
			tray = new TrayIcon(image, "");
		}else{
			tray.setImage(image);
		}
		tray.setImageAutoSize(autosize);
	}

	@Override
	public void setImageAutoSize(boolean autosize) {
		this.autosize = autosize;
	}

	@Override
	public void removeTrayMouseListener(MouseListener listener) {
		tray.removeMouseListener(listener);
	}

	@Override
	public void addTrayMouseListener(MouseListener listener) {
		tray.addMouseListener(listener);
	}

	@Override
	public Image getImage() {
		return tray.getImage();
	}
	
	@Override
	public void removeTray(){
		SystemTray stray = SystemTray.getSystemTray();
		stray.remove(tray);
	}

	@Override
	public void showTray() {
		try {
			SystemTray.getSystemTray().add(tray);// 在系统托盘区中增加图标
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	@Override
	public void setDefaultActionListener(ActionListener listen) {
		tray.addActionListener(listen);
	}

	@Override
	public void displayMessage(String caption, String text,
			MessageType messageType) {
		tray.displayMessage(caption, text, messageType);
	}
}
