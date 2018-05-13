package hc.server.ui.design.hpj;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import hc.core.L;
import hc.core.util.LogManager;
import hc.util.ResourceUtil;

public class HCUndoableEditListener implements UndoableEditListener {
	final UndoManager manager;
	final NodeEditPanel panel;
	final HCTextPane textPane;
	private boolean isSignificant = true;
	boolean isSkipAddUndoableEditForColor = false;

	public final void enableSkipAddUndoableEditForColor(final boolean isEnable) {
		isSkipAddUndoableEditForColor = isEnable;
	}
	
	public final void setSignificant() {
		isSignificant = true;
	}
	
	public HCUndoableEditListener(final NodeEditPanel panel, final UndoManager manager, final HCTextPane textPane) {
		this.manager = manager;
		this.panel = panel;
		this.textPane = textPane;
	}

	@Override
	public void undoableEditHappened(final UndoableEditEvent e) {
//		if(isSkipAddUndoableEditForColor) {
//			return;
//		}
		
		final UndoableEdit edit = e.getEdit();
		final HCUndoableEdit undoableEdit = new HCUndoableEdit(panel, edit, textPane.getCaretPosition());
		undoableEdit.isSignificant = isSignificant;
		isSignificant = false;
		L.V = L.WShop ? false : LogManager.log("[CodeTip] add UndoableEdit, dde.getType() : " + ResourceUtil.getDocumentEventType(edit).getType().toString());
		manager.addEdit(undoableEdit);
	}
}