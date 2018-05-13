package hc.server.ui.design.hpj;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

public class HCUndoableEdit implements UndoableEdit {
	final UndoableEdit base;
	final NodeEditPanel panel;
	final boolean isModi;
	final long saveToken;
	final int position;
	boolean isSignificant;
	
	@Override
	public final String toString() {
		return "isSignificant : " + isSignificant;
	}

	HCUndoableEdit(final NodeEditPanel panel, final UndoableEdit base, final int position) {
		this.panel = panel;
		this.base = base;
		isModi = this.panel.isModified();
		saveToken = this.panel.getSaveToken();
		this.position = position;
		this.isSignificant = false;
		
		if (isModi == false) {
			this.panel.notifyModified(true);// 由于REMOVE和INSERT都是isModi==false,所以强制后续的INSERT为ture
		}
	}

	@Override
	public void undo() throws CannotUndoException {
		base.undo();

		if (saveToken == panel.getSaveToken()) {
			if (isModi == false) {
				panel.notifyModified(false);
			}
		} else {
			if (panel.isModified() == false) {
				panel.notifyModified(true);
			}
		}
	}

	@Override
	public boolean canUndo() {
		return base.canUndo();
	}

	@Override
	public void redo() throws CannotRedoException {
		base.redo();
		
		if (saveToken == panel.getSaveToken()) {
			if (isModi == false) {
				panel.notifyModified(true);
			}
		}
	}

	@Override
	public boolean canRedo() {
		return base.canRedo();
	}

	@Override
	public void die() {
		base.die();
	}

	@Override
	public boolean addEdit(final UndoableEdit anEdit) {
		return base.addEdit(anEdit);
	}

	@Override
	public boolean replaceEdit(final UndoableEdit anEdit) {
		return base.replaceEdit(anEdit);
	}

	@Override
	public boolean isSignificant() {
		return isSignificant;
	}

	@Override
	public String getPresentationName() {
		return base.getPresentationName();
	}

	@Override
	public String getUndoPresentationName() {
		return base.getUndoPresentationName();
	}

	@Override
	public String getRedoPresentationName() {
		return base.getRedoPresentationName();
	}

}