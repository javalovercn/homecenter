package hc.server.ui.design.hpj;

import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import hc.core.util.ExceptionReporter;

public class HCUndoManager extends UndoManager {
	public static final int MAX_LIMIT = Short.MAX_VALUE;
	private static final Pattern idxPattern = Pattern.compile("indexOfNextAdd: (\\d+)");
	
	final HCTextPane textPane;
	
	public HCUndoManager(final HCTextPane textPane) {
		this.textPane = textPane;
	}
	
	@Override
	protected void trimForLimit() {
		final int limit = getLimit();
        if (limit >= 0) {
            final int size = edits.size();

            if (size > limit) {
                trimEdits(0, size - limit);
            }
        }
    }
	
	public final int getIndexOfNextAdd() {
		final String str = super.toString();
		final Matcher m = idxPattern.matcher(str);
		if(m.find()) {
			return Integer.parseInt(m.group(1));
		}else {
			return -1;
		}
	}
	
    @Override
	public void redo() throws CannotRedoException {
        super.redo();
        
        final int indexOfNextAdd = getIndexOfNextAdd();
        if(indexOfNextAdd == -1) {
        	ExceptionReporter.printStackTrace(new Throwable("editToBeRedone getIndexOfNextAdd : -1"));
        }
        
        try {
	        final UndoableEdit edit = edits.elementAt(indexOfNextAdd - 1);
	        if(edit instanceof HCUndoableEdit) {
	        	final HCUndoableEdit hcEdit = (HCUndoableEdit)edit;
				textPane.setCaretPosition(hcEdit.position);
	        }
        }catch (final Throwable e) {
		}
    }
	
	@Override
	protected UndoableEdit editToBeRedone() {
        final int indexOfNextAdd = getIndexOfNextAdd();
        if(indexOfNextAdd == -1) {
        	ExceptionReporter.printStackTrace(new Throwable("editToBeRedone getIndexOfNextAdd : -1"));
        }
        
		int i = indexOfNextAdd + 1;
        
        final int count = edits.size();
        if(i == count) {
        	return edits.elementAt(i - 1);
        }else {
	        while (i < count) {
	            final UndoableEdit edit = edits.elementAt(i++);
	            if (i == count) {
	                return edit;
	            }else if(edit.isSignificant()) {
	            	return edits.elementAt(i - 2);
	            }
	        }
	
	        return null;
        }
    }
	
	public static boolean isCtrlKey(final int keycode) {
		return keycode == KeyEvent.VK_BACK_SPACE 
				|| keycode == KeyEvent.VK_DELETE
				|| keycode == KeyEvent.VK_TAB
				|| keycode == KeyEvent.VK_ENTER;
	}
	
	public static boolean isDelOrBackspaceKey(final int keycode) {
		return keycode == KeyEvent.VK_BACK_SPACE 
				|| keycode == KeyEvent.VK_DELETE;
	}
}
