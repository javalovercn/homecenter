package hc.server.ui.design.hpj;

import hc.core.util.StringUtil;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import javax.swing.text.TextAction;

public class SelectWordAction extends TextAction {
	public static final SimpleAttributeSet BG_SELECTED_VAR = ScriptEditPanel.buildBackground(Color.decode("#D4D4D4"));
	
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
				
				final int startIdx = clickStartIdx + 1;
				final String selectedText = String.valueOf(lineChar, startIdx, clickEndIdx - startIdx);
				
				target.setCaretPosition(lineStartIdx + startIdx);
				target.moveCaretPosition(lineStartIdx + clickEndIdx);
				
				final Object eventSrc = e.getSource();
				if(selectedText.length() > 0 && eventSrc instanceof HCTextPane){
					final HCTextPane pane = (HCTextPane)eventSrc;
					final StyledDocument styleDoc = pane.getStyledDocument();
					final String text = pane.getText();
					
					final String matchText = StringUtil.replace(selectedText, "$", "\\$");
					final Pattern varPattern = Pattern.compile("\\b" + matchText + "\\b");
					final Matcher m = varPattern.matcher(text);
					while(m.find()){
						final int start = m.start();
						final int end = m.end();
						styleDoc.setCharacterAttributes(start, end - start, BG_SELECTED_VAR, false);
					}
					pane.selectedWordsMS = System.currentTimeMillis();
					pane.hasSelectedWords = true;
				}
			}catch (final Throwable ex) {
			}
		}
	}
	
	private final boolean isAllowChar(final char c){
		if(c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >='A' && c <= 'Z' || c == '_' || c == '$' || c == '@'){//后两项为变量用
			return true;
		}
		return false;
	}

}
