package hc.server.ui.design.hpj;

import java.awt.Color;

import javax.swing.Action;
import javax.swing.JTextPane;

import hc.server.ui.design.SearchDialog;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

public abstract class HCTextPane extends JTextPane {
	public long selectedWordsMS;
	public boolean hasSelectedWords;
	public SearchDialog searchDialog;

	public HCTextPane() {
		this(new SelectWordAction());
	}

	public HCTextPane(final Action action) {
		super();

		getActionMap().put("select-word", action);
	}

	@Override
	public final Color getBackground() {
		if (settedBG != null) {
			return settedBG;
		} else if (PropertiesManager.getValue(PropertiesManager.C_SKIN, "").equals(ResourceUtil.LF_NIMBUS)) {
			return Color.WHITE;// to fix : getBackground() == Color.GRAY
		} else {
			return super.getBackground();
		}
	}

	private Color settedBG;

	@Override
	public final void setBackground(final Color c) {
		settedBG = c;
		super.setBackground(c);
	}

	public abstract void refreshCurrLineAfterKey(final int line);
	
	public abstract void notifyUpdateScript();
}
