package hc;

import java.awt.Image;
import java.awt.TrayIcon.MessageType;

public interface PlatformTrayIcon {
	public void remove();
	
	public void exit();
	
	public void displayMessage(final String caption, final String text, final MessageType messageType);
	
	public void setToolTip(String tooltip);
	
	public Image getImage();
	
	public String getToolTip();
	
	public void setImage(Image image);
}
