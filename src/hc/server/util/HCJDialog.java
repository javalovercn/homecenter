package hc.server.util;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.server.DisposeListener;
import hc.util.ResourceUtil;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.JDialog;

public class HCJDialog extends JDialog {
	private DisposeListener listener;
	
	public HCJDialog(){
		this((Frame)null, false);
	}
	
	public HCJDialog(final Frame owner) {
        this(owner, false);
	}
	
	public HCJDialog(final Frame owner, final boolean modal){
		this(owner, "", modal);
	}
	
	public HCJDialog(final Frame owner, final String title) {
        this(owner, title, false);
	}
	
	public HCJDialog(final Window owner, final String title) {
        this(owner, title, Dialog.ModalityType.MODELESS);
	}
	
	public HCJDialog(final Window owner, final String title, final Dialog.ModalityType modalityType) {
		super(owner, title, modalityType);
		init(title);
	}

	public HCJDialog(final Frame owner, final String title, final boolean modal) {
		super(owner, title, modal);
		init(title);
	}
	
	private final void init(final String title) {
		CCoreUtil.checkAccess();
		
		this.setTitle(title);
		setIconImage(App.SYS_LOGO);
	}
	
	@Override
	public final void setTitle(String title) {
		final String product = ResourceUtil.getProductName();
		title = (title.indexOf(product) >= 0)?title:title + " - " + product;
		super.setTitle(title);
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if(listener != null){
			listener.dispose();
		}
	}
	
	public final void setDisposeListener(final DisposeListener dListener){
		if(listener != null){
			final DisposeListener oldListener = listener;
			listener = new DisposeListener() {
				@Override
				public void dispose() {
					oldListener.dispose();
					dListener.dispose();
				}
			};
		}else{
			listener = dListener;
		}
	}
}
