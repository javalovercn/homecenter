package hc.server.ui.design.hpj;

import javax.swing.JTextPane;

public abstract class HCTextPane extends JTextPane {
	public abstract void refreshCurrLineAfterKey(final int line);
}
