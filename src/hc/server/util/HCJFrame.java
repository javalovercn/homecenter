package hc.server.util;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.server.DisposeListener;
import hc.util.ResourceUtil;

import javax.swing.JFrame;

public class HCJFrame extends JFrame {
	private DisposeListener listener;
	private final boolean withoutHC;
	
	public HCJFrame(){
		this("", false);
	}
	
	public HCJFrame(final String title, final boolean withoutHC){
		super(title);
		this.withoutHC = withoutHC;
		
		setTitle(title);
		CCoreUtil.checkAccess();
		setIconImage(App.SYS_LOGO);
	}
	
	public HCJFrame(final String title){
		this(title, false);
	}
	
	@Override
	public final void setTitle(String title) {
		if(withoutHC == false){
			final String product = ResourceUtil.getProductName();
			title = (title.indexOf(product) >= 0)?title:title + " - " + product;
		}
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
