package hc.server.util;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import hc.App;
import hc.core.util.CCoreUtil;
import hc.server.DisposeListener;

import javax.swing.JDialog;

public class HCJDialog extends JDialog {
	private DisposeListener listener;
	
	public HCJDialog(){
		this((Frame)null, false);
	}
	
	public HCJDialog(Frame owner) {
        this(owner, false);
	}
	
	public HCJDialog(Frame owner, boolean modal){
		this(owner, "", modal);
	}
	
	public HCJDialog(Frame owner, String title) {
        this(owner, title, false);
	}
	
	public HCJDialog(Window owner, String title) {
        this(owner, title, Dialog.ModalityType.MODELESS);
	}
	
	public HCJDialog(Window owner, String title, Dialog.ModalityType modalityType) {
		super(owner, title, modalityType);
		init(title);
	}

	public HCJDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
		init(title);
	}
	
	private final void init(String title) {
		CCoreUtil.checkAccess();
		
		this.setTitle(title);
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
