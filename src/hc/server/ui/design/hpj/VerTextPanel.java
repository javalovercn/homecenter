package hc.server.ui.design.hpj;

import hc.util.ResourceUtil;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class VerTextPanel extends JPanel {
	VerTextField verTextField = new VerTextField();
	final String nodeDesc;

	public VerTextPanel(final String nodeDesc, final boolean withBorder,
			final boolean withSpaceDesc, final boolean isLableStrong) {
		this.nodeDesc = nodeDesc;

		final JLabel verLabel = new JLabel(
				isLableStrong ? "<html><STRONG>Version</STRONG> : </html>" : "Version : ");
		final JLabel tipLabel = new JLabel("<html>" + "1. version of the current " + nodeDesc
				+ ".<BR>" + "2. valid char are 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 and dot.</html>");
		verTextField.setColumns(20);

		if (withSpaceDesc) {
			final JPanel gridbag = new JPanel(new GridBagLayout());
			final GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.LINE_START;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			gridbag.add(new JLabel("   "), c);

			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			gridbag.add(verTextField, c);

			setLayout(new GridBagLayout());
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			add(verLabel, c);
			c.gridy = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(gridbag, c);
			c.gridy = 2;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			add(ResourceUtil.addSpaceBeforePanel(tipLabel), c);
		} else {
			setLayout(new GridLayout(2, 1));

			final JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
			// idPanel.add(verLabel);
			idPanel.add(verTextField);
			add(idPanel);

			add(tipLabel);
		}

		if (withBorder) {
			setBorder(new TitledBorder("Version"));
		}
	}
}
