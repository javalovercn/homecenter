package hc.server.ui.design;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public abstract class DesignAbstractAction extends AbstractAction{
	@Override
	public final void actionPerformed(final ActionEvent e) {
		onToolbarActionForCommon();
		actionPerformedExt(e);
	}
	
	public abstract void onToolbarActionForCommon();
	
	public abstract void actionPerformedExt(final ActionEvent e);
}
