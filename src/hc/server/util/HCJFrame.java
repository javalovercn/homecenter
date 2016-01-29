package hc.server.util;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.server.DisposeListener;

import javax.swing.JFrame;

public class HCJFrame extends JFrame {
	private DisposeListener listener;
	
	public HCJFrame(){
		this("");
	}
	
	public HCJFrame(String title){
		super(title);
		
		setTitle(title);
		
		CCoreUtil.checkAccess();
		
		setIconImage(App.SYS_LOGO);
	}
	
	public final void setTitle(String title) {
		title = (title.indexOf("HomeCenter") >= 0)?title:title + " - HomeCenter";
		super.setTitle(title);
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if(listener != null){
			listener.dispose();
		}
	}
	
	public final DisposeListener getDisposeListener(){
		return listener;
	}
	
	public final void setDisposeListener(DisposeListener dListener){
		listener = dListener;
	}
}
