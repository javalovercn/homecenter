package hc.server.ui;

import java.awt.event.ActionListener;

public interface ClosableWindow {
	public void notifyClose();
	
	public void setCloseAction(ActionListener al);
}
