package hc.server.ui.design.hpj;

import hc.App;
import hc.server.HCActionListener;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class JarNodeEditPanel extends NameEditPanel{
	final VerTextPanel verPanel = new VerTextPanel("jar", true, false);
	
	
	public JarNodeEditPanel(){
		super();
		
		final VerTextField verTextField = verPanel.verTextField;
		verTextField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				((HCShareFileResource)currItem).ver = verTextField.getText();
				notifyModified(true);
			}
			@Override
			public void removeUpdate(final DocumentEvent e) {
				modify();
			}
			
			@Override
			public void insertUpdate(final DocumentEvent e) {
				modify();
			}
			
			@Override
			public void changedUpdate(final DocumentEvent e) {
				modify();
			}
		});
		verTextField.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				((HCShareFileResource)currItem).ver = verTextField.getText();
				App.invokeLaterUI(updateTreeRunnable);
				currItem.getContext().modified.setModified(true);
			}
		}, threadPoolToken));
		
		final JPanel center = new JPanel();
		center.setLayout(new BorderLayout());
		center.add(verPanel, BorderLayout.NORTH);
		
		add(center, BorderLayout.CENTER);		
	}

	@Override
	public void extendInit(){
		verPanel.verTextField.setText(((HCShareFileResource)currItem).ver);
	}
}
