package hc.util;

import java.awt.Frame;

import javax.swing.JDialog;

public class HCDialog extends JDialog {
	public HCDialog(final Frame owner, final String title, final boolean modal) {
		super(owner, title, modal);
	}
}
