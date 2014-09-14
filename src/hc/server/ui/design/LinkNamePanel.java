package hc.server.ui.design;


import hc.server.ui.ClientDesc;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class LinkNamePanel  extends JPanel {
	final JTextField linkNameField = new JTextField(20);
	final JTextField projRemarkField = new JTextField(20);
	public final String CANCLE = "-1";
	
	public LinkNamePanel(final String linkName, final String desc) {
		if(linkName != null){
			linkNameField.setText(linkName);
		}
		if(desc != null){
			projRemarkField.setText(desc);
		}
		
		setLayout(new GridLayout(2, 1, ClientDesc.hgap, ClientDesc.vgap));
		{
			JPanel panel = new JPanel(new BorderLayout());
			
			final JLabel nameLabel = new JLabel("link name :");
			final Font oldfont = nameLabel.getFont();
			nameLabel.setFont(new Font(oldfont.getFontName(), Font.BOLD, oldfont.getSize()));
			
			panel.add(nameLabel, BorderLayout.LINE_START);
			panel.add(linkNameField, BorderLayout.CENTER);
			
			JPanel compose = new JPanel(new GridLayout(2, 1));
			compose.add(panel);
			compose.add(new JLabel("it is folder name of the entrance to the project"));
			
			compose.setBorder(new TitledBorder("link name"));
			add(compose);
		}
		{
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JLabel("Comment :"), BorderLayout.LINE_START);
			panel.add(projRemarkField, BorderLayout.CENTER);
			
			JPanel compose = new JPanel(new GridLayout(2, 1));
			compose.add(panel);
			compose.add(new JLabel("comment information of the project"));
			
			compose.setBorder(new TitledBorder("project comment"));
			add(compose);
		}
	}
}