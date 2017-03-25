package hc.server.util;

import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JButton;

public class DownlistButton extends JButton {
	private final Vector<String> aliveServer = new Vector<String>();

	private static final String downArrow = " ▼";//◥◤▼↓
	
	public DownlistButton(final String text, final Icon icon){
		super(text, icon);
	}
	
	public final void reset(){
		removeDownArrow();
		synchronized (aliveServer) {
			aliveServer.removeAllElements();
		}
	}

	public final synchronized void removeDownArrow(){
		final String btnText = getText();
		final int downIdx = btnText.indexOf(downArrow);
		if(downIdx > 0){
			setText(btnText.substring(0, downIdx));
		}
	}
	
	public final synchronized void addDownArrow(){
		final String btnText = getText();
		final int downIdx = btnText.indexOf(downArrow);
		if(downIdx < 0){
			setText(btnText + downArrow);
		}
	}
	
	public final void addList(final Vector<String> list){
		synchronized (aliveServer) {
			aliveServer.removeAllElements();
			aliveServer.addAll(list);
		}
	}
	
	public final Vector<String> getList(){
		synchronized (aliveServer) {
			final Vector<String> out = new Vector<String>();
			out.addAll(aliveServer);
			return out;
		}
	}
}
