package hc.server.ui.design.hpj;

import hc.util.ResourceUtil;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class VerTextPanel extends JPanel{
	VerTextField verTextField = new VerTextField();
	final String nodeDesc;
	
	public VerTextPanel(final String nodeDesc, final boolean withBorder, final boolean withSpaceDesc) {
		this.nodeDesc = nodeDesc;
		
		final JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		final JLabel verLabel = new JLabel("Version : ");
		final JLabel tipLabel = new JLabel("<html>" +
				"1. version of the current " + nodeDesc + ".<BR>" +
				"2. valid char are 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 and dot.</html>");
//		tipLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		idPanel.add(verLabel);
		verTextField.setColumns(20);
		idPanel.add(verTextField);
		
		setLayout(new GridLayout(2, 1));
		add(idPanel);
		if(withSpaceDesc){
			add(ResourceUtil.addSpaceBeforePanel(tipLabel));
		}else{
			add(tipLabel);
		}
		
		if(withBorder){
			setBorder(new TitledBorder("Version"));
		}
	}
}
