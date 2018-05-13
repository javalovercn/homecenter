package hc.server.ui.design;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

public abstract class DesignButton extends JButton {
	public DesignButton(final String text, final Icon icon) {
		super(text, icon);
	}
	
	@Override
	public final void addActionListener(final ActionListener listener) {
		super.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				onToolbarActionForCommon();
				listener.actionPerformed(e);
			}
		});
	}
	
	public abstract void onToolbarActionForCommon();
}
