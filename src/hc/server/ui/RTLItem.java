package hc.server.ui;

import javax.swing.JComponent;

public class RTLItem extends TodoItem{
	public boolean isRTL;
	
	public RTLItem(final int forType, final JComponent component, final boolean isRTL){
		super(forType, component);
		
		this.isRTL = isRTL;
	}
}
