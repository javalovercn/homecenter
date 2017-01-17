package hc.server.ui.design.hpj;

import java.awt.event.ActionEvent;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

public class SelectWordAction extends TextAction {
	public SelectWordAction() {
		super("Select Word");
	}
	
	@Override
	public void actionPerformed(final ActionEvent e) {
		final JTextComponent target = getTextComponent(e);

		if (target != null) {
			final int offset = target.getCaretPosition();
			final Document doc = target.getDocument();
			try{
				final int line = ScriptEditPanel.getLineOfOffset(doc, offset);
				final int lineStartIdx = ScriptEditPanel.getLineStartOffset(doc, line);
				final int lineEndIdx = ScriptEditPanel.getLineEndOffset(doc, line);
				final char[] lineChar = doc.getText(lineStartIdx, lineEndIdx - lineStartIdx).toCharArray();
				int clickStartIdx = offset - lineStartIdx;
				int clickEndIdx = clickStartIdx;
				for (; clickStartIdx >= 0; clickStartIdx--) {
					if(isAllowChar(lineChar[clickStartIdx]) == false){
						break;
					}
				}
				for (; clickEndIdx < lineChar.length; clickEndIdx++) {
					if(isAllowChar(lineChar[clickEndIdx]) == false){
						break;
					}
				}
				
				target.setCaretPosition(lineStartIdx + clickStartIdx + 1);
				target.moveCaretPosition(lineStartIdx + clickEndIdx);
			}catch (final Throwable ex) {
			}
		}
	}
	
	private final boolean isAllowChar(final char c){
		if(c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >='A' && c <= 'Z' || c == '_'){
			return true;
		}
		return false;
	}

}
