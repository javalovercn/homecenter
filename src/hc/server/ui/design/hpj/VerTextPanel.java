package hc.server.ui.design.hpj;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class VerTextPanel extends JPanel{
	VerTextField verTextField = new VerTextField();
	final String nodeDesc;
	
	public VerTextPanel(final String nodeDesc) {
		this.nodeDesc = nodeDesc;
		
		JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		final JLabel verLabel = new JLabel("ver : ");
		final JLabel tipLabel = new JLabel("<html>input version of the current " + nodeDesc + "." +
				"<BR>valid char : 0-9, .</html>");
//		tipLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		idPanel.add(verLabel);
		verTextField.setColumns(20);
		idPanel.add(verTextField);
		
		setLayout(new GridLayout(2, 1));
		add(idPanel);
		add(tipLabel);
		
		setBorder(new TitledBorder("Version"));
	}
}
