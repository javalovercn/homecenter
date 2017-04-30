package hc.server.ui.design.hpj;

import hc.server.ui.design.code.StyleManager;

import java.awt.event.ActionEvent;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;

public class CSSSelectWordAction extends TextAction {
	
	public CSSSelectWordAction() {
		super("Select Word");
	}
	
	private final PartLoc clickOnNumber(final char[] lineChar, int clickStartIdx){
		int clickEndIdx = clickStartIdx;
		for (; clickStartIdx >= 0; clickStartIdx--) {
			if(isNumChar(lineChar[clickStartIdx]) == false){
				break;
			}
		}
		for (; clickEndIdx < lineChar.length; clickEndIdx++) {
			if(isNumChar(lineChar[clickEndIdx]) == false){
				break;
			}
		}
		if(clickEndIdx > clickStartIdx && clickEndIdx - clickStartIdx >= 2){
			return new PartLoc(clickStartIdx, clickEndIdx);
		}else{
			return null;
		}
	}
	
	private final PartLoc clickOnParameter(final char[] lineChar, int clickStartIdx){
		int clickEndIdx = clickStartIdx;
		boolean hasStartBorder = false, hasEndBorder = false;
		for (; clickStartIdx >= 0; clickStartIdx--) {
			final char ch = lineChar[clickStartIdx];
			if(isParameterChar(ch) == false){
				break;
			}
			if(ch == StyleManager.PARAMETER_BORDER_CHAR){
				clickStartIdx--;
				hasStartBorder = true;
				break;
			}
		}
		for (; clickEndIdx < lineChar.length; clickEndIdx++) {
			final char ch = lineChar[clickEndIdx];
			if(isParameterChar(ch) == false){
				break;
			}
			if(ch == StyleManager.PARAMETER_BORDER_CHAR){
				clickEndIdx++;
				hasEndBorder = true;
				break;
			}
		}
		
		if(hasStartBorder && hasEndBorder){
			return new PartLoc(clickStartIdx, clickEndIdx);
		}else{
			return null;
		}
	}
	
	private final PartLoc clickOnNormal(final char[] lineChar, int clickStartIdx){
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
		return new PartLoc(clickStartIdx, clickEndIdx);
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
				final int clickStartIdx = offset - lineStartIdx;
				
				PartLoc out;
				if((out = clickOnNumber(lineChar, clickStartIdx)) != null){
				}else if((out = clickOnParameter(lineChar, clickStartIdx)) != null){
				}else if((out = clickOnNormal(lineChar, clickStartIdx)) != null){
				}
				
				final int startIdx = out.startIdx + 1;
				
				target.setCaretPosition(lineStartIdx + startIdx);
				target.moveCaretPosition(lineStartIdx + out.endIdx);
				
//				final Object eventSrc = e.getSource();
//				if(selectedText.length() > 0 && eventSrc instanceof HCTextPane){
//					final HCTextPane pane = (HCTextPane)eventSrc;
//					final StyledDocument styleDoc = pane.getStyledDocument();
//					final String text = pane.getText();
//					
//					final String matchText = StringUtil.replace(selectedText, "$", "\\$");
//					final Pattern varPattern = Pattern.compile("\\b" + matchText + "\\b");
//					final Matcher m = varPattern.matcher(text);
//					while(m.find()){
//						final int start = m.start();
//						final int end = m.end();
//						styleDoc.setCharacterAttributes(start, end - start, SelectWordAction.BG_SELECTED_VAR, false);
//					}
//					pane.selectedWordsMS = System.currentTimeMillis();
//					pane.hasSelectedWords = true;
//				}
			}catch (final Throwable ex) {
			}
		}
	}
	
	private final boolean isAllowChar(final char c){
		if(c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >='A' && c <= 'Z' || c == '_' || c == '-' || c == '$'){
			return true;
		}
		return false;
	}
	
	private final boolean isParameterChar(final char c){
		if(c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >='A' && c <= 'Z' || c == '$'){
			return true;
		}
		return false;
	}
	
	private final boolean isNumChar(final char c){
		if(c >= '0' && c <= '9'){
			return true;
		}
		return false;
	}

}

class PartLoc {
	final int startIdx;
	final int endIdx;
	
	PartLoc(final int startIdx, final int endIdx){
		this.startIdx = startIdx;
		this.endIdx = endIdx;
	}
}