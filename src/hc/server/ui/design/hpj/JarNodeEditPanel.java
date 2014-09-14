package hc.server.ui.design.hpj;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class JarNodeEditPanel extends NameEditPanel{
	final VerTextPanel verPanel = new VerTextPanel("jar");
	
	
	public JarNodeEditPanel(){
		super();
		
		final VerTextField verTextField = verPanel.verTextField;
		verTextField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				((HPShareJar)item).ver = verTextField.getText();
				notifyModified();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				modify();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				modify();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				modify();
			}
		});
		verTextField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				((HPShareJar)item).ver = verTextField.getText();
				tree.updateUI();
				item.getContext().modified.setModified(true);
			}
		});
		
		JPanel center = new JPanel();
		center.setLayout(new BorderLayout());
		center.add(verPanel, BorderLayout.NORTH);
		
		add(center, BorderLayout.CENTER);		
	}

	@Override
	public void extendInit(){
		verPanel.verTextField.setText(((HPShareJar)item).ver);
	}
}
