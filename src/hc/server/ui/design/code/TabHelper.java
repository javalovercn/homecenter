package hc.server.ui.design.code;

import java.awt.Color;
import java.awt.event.KeyEvent;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;

import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.Stack;
import hc.server.ui.design.UpgradeManager;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.server.ui.design.hpj.ScriptModelManager;

public class TabHelper {
	private static final Highlighter.HighlightPainter CODE_LIGHTER = new DefaultHighlighter.DefaultHighlightPainter(Color.decode("#B2D7FC"));
	public static Object currFocusHighlight;
	static int parameterIdx;
	static JTextPane scriptPanel;
	static int currFocusHighlightStartIdx, currFocusHighlightEndIdx, inputShiftOffset;
	
	public static void initScriptPanel(final JTextPane sPanel, final ScriptEditPanel sep, final String listenerScript){
		String scripts = (listenerScript == null || listenerScript.length() == 0) ? ScriptModelManager.ENCODING_UTF_8 : listenerScript;
		scripts = UpgradeManager.preProcessScript(scripts);
		sep.setInitText(scripts);
		final int initLoc = (ScriptModelManager.ENCODING_UTF_8.equals(scripts)?ScriptModelManager.ENCODING_UTF_8.length():0);
		
		sep.rebuildASTNode();
		
		scriptPanel = sPanel;
		scriptPanel.setCaretPosition(initLoc);
		
		sep.initColor(false, 0);
	}
	
	public final static Stack tabBlockStack = new Stack(2);
	public static TabBlock currentTabBlock;
	
	private final static TabBlock innerClassTabBlock = new TabBlock(0, null, 0);
	
	public static void setInnerClassTabBlock(){
		CCoreUtil.checkAccess();
		currentTabBlock = innerClassTabBlock;
	}
	
	public static void setCurrentTabBlock(final int startIdx, final char[] methods, final int parameterNum){
		final TabBlock newBlock = new TabBlock(startIdx, methods, parameterNum);
		if(currentTabBlock != null){
			currentTabBlock.lastParameterIdxBeforeStack = parameterIdx;
			tabBlockStack.push(currentTabBlock);
		}
		
		currentTabBlock = newBlock;
		parameterIdx = 0;
		inputShiftOffset = 0;
		
		focusParameter();
	}
	
	public static boolean isInTabBlock(){
		return currentTabBlock != null;
	}

	private static void focusParameter() {
		calculateIdx();
		
		clearHighlight();
		
		try {
			currFocusHighlight = scriptPanel.getHighlighter().addHighlight(currFocusHighlightStartIdx, currFocusHighlightEndIdx, CODE_LIGHTER);
		} catch (final BadLocationException e) {
//			ExceptionReporter.printStackTrace(e);
		}
		
		scriptPanel.setSelectionStart(currFocusHighlightStartIdx);
		scriptPanel.setSelectionEnd(currFocusHighlightEndIdx);
//		scriptPanel.setSelectionEnd(currFocusHighlightEndIdx);
//		scriptPanel.setCaretPosition(currFocusHighlightEndIdx);
	}

	private static void calculateIdx() {
		for(int i = 0; i<= parameterIdx; i++){
			if(i == 0){
				currFocusHighlightStartIdx = currentTabBlock.startIdx + currentTabBlock.parameterBeginOffsetIdx[i];
			}else{
				currFocusHighlightStartIdx = currFocusHighlightEndIdx + currentTabBlock.parameterBeginOffsetIdx[i];
			}
			currFocusHighlightEndIdx = currFocusHighlightStartIdx + currentTabBlock.parameterEndOffsetIdx[i];
			if(L.isInWorkshop){
				System.out.println("parameterNum : " + i + ", highlightStartIdx : " + currFocusHighlightStartIdx + ", highlightEndIdx : " + currFocusHighlightEndIdx);
			}
		}
	}
	
	public static boolean pushShiftTabKey(){
		if(currentTabBlock != null){
			if(parameterIdx == 0){
				parameterIdx = currentTabBlock.parameterBeginOffsetIdx.length - 1;
			}else{
				parameterIdx--;
			}
			focusParameter();
			return true;
		}
		return false;
	}
	
	public static void clearAll(){
		clearHighlight();
		
		if(currentTabBlock != null){
			tabBlockStack.removeAllElements();
			currentTabBlock = null;
		}
	}
	
	public static boolean pushTabOrEnterKey(){
		if(currentTabBlock != null){
			if(currentTabBlock == innerClassTabBlock){
				try{
					final int post = scriptPanel.getCaretPosition();
					final Document doc = scriptPanel.getDocument();
					final int lineNO = ScriptEditPanel.getLineOfOffset(doc, post) + 1;
					final char[] lineChar = ScriptEditPanel.getLineText(doc, lineNO).toCharArray();
					for (int i = 0; i < lineChar.length; i++) {
						if(lineChar[i] == '\n'){
							scriptPanel.setCaretPosition(ScriptEditPanel.getLineStartOffset(doc, lineNO) + i);
						}
					}
				}catch (final Throwable e) {
					e.printStackTrace();
				}
				clearAll();
				return true;
			}
			
			if(parameterIdx == currentTabBlock.parameterBeginOffsetIdx.length - 1){
				//最后一个参数
				final TabBlock popBlock = (TabBlock)tabBlockStack.pop();
				if(popBlock != null){
					currentTabBlock = popBlock;
					parameterIdx = currentTabBlock.lastParameterIdxBeforeStack;
					currentTabBlock.parameterEndOffsetIdx[parameterIdx] += inputShiftOffset;
					inputShiftOffset = 0;
					pushTabOrEnterKey();
					return true;
				}
				calculateIdx();//重新更新currFocusHighlightEndIdx标位
				clearHighlight();
				
				//跳到方法尾
				currentTabBlock = null;
				scriptPanel.setCaretPosition(currFocusHighlightEndIdx + 1);//+1=)
				return true;
			}else{
				parameterIdx++;
				focusParameter();
				return true;
			}
		}
		return false;
	}

	private static void clearHighlight() {
		if(currFocusHighlight != null){
			scriptPanel.getHighlighter().removeHighlight(currFocusHighlight);
			currFocusHighlight = null;
		}
	}
	
	public static void notifyInputBlock(final int appendNum){
		if(currentTabBlock != null){
			currentTabBlock.parameterEndOffsetIdx[parameterIdx] += appendNum;
			currFocusHighlightEndIdx += appendNum;
			inputShiftOffset += appendNum;
		}
	}
	
	public static void notifyInputKey(final boolean isBackspace, final KeyEvent event, final char inputChar, final int selectionOrPasteLen){
		if(currentTabBlock != null){
			if(L.isInWorkshop){
				System.out.println("---------notifyInputKey char : " + inputChar);
			}
			final boolean isDelete = inputChar==KeyEvent.VK_DELETE;
			if(isBackspace){
			}else if(event != null && (inputChar == '\t' || event.isActionKey())){
				if(L.isInWorkshop){
					System.out.println("-----isActionKey : " + event.isActionKey() + ", isTab : " + (inputChar == '\t'));
				}
				return;
			}
			if(event != null){
				final int keyCode = event.getKeyCode();
				if(keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT){
					return;
				}
			}
			
			if(selectionOrPasteLen != 0){
//					scriptPanel.getDocument().remove(scriptPanel.getSelectionStart(), selectionLen - (isBackspace?1:0));//已完成，无需再执行
				inputShiftOffset -= selectionOrPasteLen;
				currFocusHighlightEndIdx -= selectionOrPasteLen;
				currentTabBlock.parameterEndOffsetIdx[parameterIdx] -= selectionOrPasteLen;
			}
			if(isBackspace || isDelete){
				if(selectionOrPasteLen == 0){
					currentTabBlock.parameterEndOffsetIdx[parameterIdx]--;
					currFocusHighlightEndIdx--;
					inputShiftOffset--;
				}
			}else{
				currentTabBlock.parameterEndOffsetIdx[parameterIdx]++;
				currFocusHighlightEndIdx++;
				inputShiftOffset++;
			}
			SwingUtilities.invokeLater(refreshLightParameter);
		}
	}
	
	static final Runnable refreshLightParameter = new Runnable() {
		@Override
		public void run() {
			clearHighlight();
			try {
				if(L.isInWorkshop){
					System.out.println("lighter startIdx : " + currFocusHighlightStartIdx + ", endIdx : " + currFocusHighlightEndIdx);
				}
				if(currFocusHighlightEndIdx > currFocusHighlightStartIdx){
					currFocusHighlight = scriptPanel.getHighlighter().addHighlight(currFocusHighlightStartIdx, currFocusHighlightEndIdx, CODE_LIGHTER);
				}
			} catch (final BadLocationException e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
	};

	public static boolean pushEscKey(){
		clearAll();
		return true;
//		if(currentTabBlock != null){
//			final TabBlock popBlock = (TabBlock)tabBlockStack.pop();
//			if(popBlock != null){
//				currentTabBlock = popBlock;
//				parameterIdx = currentTabBlock.lastParameterIdxBeforeStack;
//				focusParameter();
//			}else{
//				currentTabBlock = null;
//				clearHighlight();
//			}
//			return true;
//		}
//		return false;
	}
}
