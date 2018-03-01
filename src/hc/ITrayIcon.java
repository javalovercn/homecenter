package hc;

import java.awt.Image;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

public interface ITrayIcon {
	public void setToolTip(String tooltip);

	public void setImage(Image image);

	public Image getImage();

	public void setImageAutoSize(boolean autosize);

	public void removeTrayMouseListener(MouseListener listener);

	public void addTrayMouseListener(MouseListener listener);

	public void removeTray();

	public void showTray();

	public void setDefaultActionListener(ActionListener listen);

	public void displayMessage(String caption, String text, MessageType messageType);
}
