package hc.server.ui.design.code;

import hc.core.L;
import hc.core.util.Stack;

import java.awt.Color;
import java.awt.event.KeyEvent;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class TabHelper {
	private static final Highlighter.HighlightPainter CODE_LIGHTER = new DefaultHighlighter.DefaultHighlightPainter(Color.decode("#B2D7FC"));
	public static Object currFocusHighlight;
	static int parameterIdx;
	static JTextPane scriptPanel;
	static boolean isFocusFullParameter;
	static int currFocusHighlightStartIdx, currFocusHighlightEndIdx, inputShiftOffset;
	
	public static void setScriptPanel(final JTextPane sPanel){
		scriptPanel = sPanel;
	}
	
	public final static Stack tabBlockStack = new Stack(2);
	public static TabBlock currentTabBlock;
	
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

	private static void focusParameter() {
		calculateIdx();
		
		clearHighlight();
		
		try {
			currFocusHighlight = scriptPanel.getHighlighter().addHighlight(currFocusHighlightStartIdx, currFocusHighlightEndIdx, CODE_LIGHTER);
		} catch (final BadLocationException e) {
			e.printStackTrace();
		}
		
		scriptPanel.setSelectionStart(currFocusHighlightStartIdx);
		scriptPanel.setSelectionEnd(currFocusHighlightEndIdx);
		scriptPanel.setCaretPosition(currFocusHighlightStartIdx);
		isFocusFullParameter = true;
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
		if(currentTabBlock != null){
			clearHighlight();
			tabBlockStack.removeAllElements();
			currentTabBlock = null;
		}
	}
	
	public static boolean pushTabOrEnterKey(){
		if(currentTabBlock != null){
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
	
	public static void notifyInputKey(final boolean isBackspace, final KeyEvent event, final char inputChar){
		if(currentTabBlock != null){
			if(L.isInWorkshop){
				System.out.println("---------notifyInputKey char : " + inputChar);
			}
			if(isBackspace){
			}else if(inputChar == '\t' || event.isActionKey()){
				if(L.isInWorkshop){
					System.out.println("-----isActionKey : " + event.isActionKey() + ", isTab : " + (inputChar == '\t'));
				}
				return;
			}
			final int keyCode = event.getKeyCode();
			if(keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT){
				return;
			}
		
			if(isFocusFullParameter){
				isFocusFullParameter = false;
				try {
					final int removeLen = currFocusHighlightEndIdx - currFocusHighlightStartIdx;
					scriptPanel.getDocument().remove(currFocusHighlightStartIdx, removeLen);
					inputShiftOffset -= removeLen;
					currFocusHighlightEndIdx = currFocusHighlightStartIdx;
					currentTabBlock.parameterEndOffsetIdx[parameterIdx] = 0;
				} catch (final BadLocationException e) {
					e.printStackTrace();
				}
			}
			if(isBackspace){
				currentTabBlock.parameterEndOffsetIdx[parameterIdx]--;
				currFocusHighlightEndIdx--;
				inputShiftOffset--;
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
//				if(L.isInWorkshop){
//					System.out.println("lighter startIdx : " + currFocusHighlightStartIdx + ", endIdx : " + currFocusHighlightEndIdx);
//				}
				currFocusHighlight = scriptPanel.getHighlighter().addHighlight(currFocusHighlightStartIdx, currFocusHighlightEndIdx, CODE_LIGHTER);
			} catch (final BadLocationException e) {
				e.printStackTrace();
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
