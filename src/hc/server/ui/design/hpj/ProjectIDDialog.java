package hc.server.ui.design.hpj;

import hc.App;
import hc.server.ui.design.Designer;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;

public class ProjectIDDialog {
	public static final void showInputProjectID(final Designer designer,
			final DefaultMutableTreeNode root) {
		final JTextField idField = new JTextField();
		final JPanel idPanel = new JPanel();

		idPanel.setLayout(new GridBagLayout());
		final JLabel idLabel = new JLabel("ID : ");
		final JLabel tipLabel = ProjectIDDialog.buildIDTipLabel();
		{
			final GridBagConstraints c = new GridBagConstraints();
			c.weightx = 0;
			c.weighty = 0;
			c.fill = GridBagConstraints.NONE;
			idPanel.add(idLabel, c);
		}
		ProjectIDDialog.buildIDFieldKeyListener(idField);
		{
			final GridBagConstraints c = new GridBagConstraints();
			c.weightx = 1;
			c.weighty = 0;
			c.gridx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			idPanel.add(idField, c);
		}

		final JPanel compose = new JPanel(new GridLayout(2, 1));
		compose.add(idPanel);
		// tipLabel.setBorder(new TitledBorder((String)ResourceUtil.get(9095)));
		compose.add(tipLabel);

		compose.setBorder(new TitledBorder("Project ID"));

		final ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final String text = idField.getText();
				if (text.trim().length() > 0) {
					((HPProject) root.getUserObject()).id = text;
				}
			}
		};
		App.showCenterPanelMain(compose, 0, 0, "Project ID", false, null, null, listener, null,
				designer, true, false, null, false, false);
	}

	public static void buildIDFieldKeyListener(final JTextField idField) {
		idField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
				final char keyCh = e.getKeyChar();
				if ((keyCh >= '0' && keyCh <= '9') || (keyCh >= 'a' && keyCh <= 'z')
						|| (keyCh >= 'A' && keyCh <= 'Z') || keyCh == '_' || keyCh == '.') {
				} else {
					e.setKeyChar('\0');
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
			}

			@Override
			public void keyPressed(final KeyEvent e) {
			}
		});
	}

	public static JLabel buildIDTipLabel() {
		return new JLabel(
				"<html>it is used to identify this project from other when install and upgrade."
						+ "<BR>'root' is system reserved ID."
						+ "<BR>valid char : A-Z, a-z, 0-9, _ and .</html>");
	}

}
