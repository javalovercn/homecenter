package hc.server.ui.design.hpj;

import javax.swing.Action;
import javax.swing.JTextPane;

public abstract class HCTextPane extends JTextPane {
	public HCTextPane(){
		super();
		
		final Action action = new SelectWordAction();
        getActionMap().put("select-word", action);
	}
	
	public abstract void refreshCurrLineAfterKey(final int line);
}
