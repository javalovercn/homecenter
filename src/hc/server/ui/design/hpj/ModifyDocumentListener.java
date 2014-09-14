package hc.server.ui.design.hpj;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class ModifyDocumentListener implements DocumentListener{
	public abstract void modify();
	
	@Override
	public void insertUpdate(DocumentEvent e) {
		modify();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		modify();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		modify();
	}
}
