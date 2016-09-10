package hc.server.ui.design.hpj;

import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.ThreadPriorityManager;
import hc.server.ui.ClientDesc;
import hc.server.ui.design.code.CodeHelper;
import hc.server.ui.design.code.TabHelper;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class MletNodeEditPanel extends DefaultMenuItemNodeEditPanel {
	private static final SimpleAttributeSet CLASS_LIGHTER = build(Color.decode("#088A29"), false);//dark green
	private static final SimpleAttributeSet ITEM_LIGHTER = build(Color.decode("#0431B4"), false);
	private static final SimpleAttributeSet SPLITTER_LIGHTER = build(Color.BLACK, false);
	private static final SimpleAttributeSet VALUE_LIGHTER = build(Color.decode("#B18904"), false);
	private static final SimpleAttributeSet VARIABLE_LIGHTER = build(Color.decode("#DF0101"), false);
	
	private static final Pattern class_pattern = Pattern.compile("(\\s*?(.*?)\\s*?)\\{");
	private static final Pattern item_pattern = Pattern.compile("(\\b*?([a-zA-Z_0-9-]+)\\b*?\\s*?):");
	private static final Pattern rem_pattern = Pattern.compile("(/\\*(.*?)\\*/)", Pattern.MULTILINE|Pattern.DOTALL);
	private static final Pattern var_pattern = Pattern.compile("(\\$(.*?)\\$)");
	private static final Pattern spliter_pattern = Pattern.compile("([\\{\\};:])");
	
	private final void initSytleBlock(final String text, final int offset, final boolean isReplace) {
		final StyledDocument document = (StyledDocument)cssEditPane.getDocument();
		document.setCharacterAttributes(offset, text.length(), VALUE_LIGHTER, true);
		
		buildSytleHighlight(cssEditPane, class_pattern, CLASS_LIGHTER, offset, text, isReplace);
		buildSytleHighlight(cssEditPane, item_pattern, ITEM_LIGHTER, offset, text, isReplace);//要置于字符串之前，因为字符串中可能含有数字
		buildSytleHighlight(cssEditPane, var_pattern, VARIABLE_LIGHTER, offset, text, isReplace);
		buildSytleHighlight(cssEditPane, rem_pattern, REM_LIGHTER, offset, text, isReplace);//字符串中含有#{}，所以要置于STR_LIGHTER之前
		buildSytleHighlight(cssEditPane, spliter_pattern, SPLITTER_LIGHTER, offset, text, isReplace);//字符串中含有#{}，所以要置于STR_LIGHTER之前
	}
	
	private static final void buildSytleHighlight(final JTextPane pane, final Pattern pattern, final SimpleAttributeSet attributes, final int offset, final String text, final boolean isReplace) {
		final StyledDocument document = (StyledDocument)pane.getDocument();
		final Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			final int start = matcher.start(1) + offset;
			final int end = matcher.end(1) + offset;
			document.setCharacterAttributes(start, end - start, attributes, isReplace);
		}
	}
	
	private final Runnable colorRunnable = new Runnable() {
		@Override
		public void run() {
			final String allText = cssEditPane.getText();
			initSytleBlock(allText, 0, false);
			
			buildSytleHighlight(cssEditPane, rem_pattern, REM_LIGHTER, 0, allText, true);
		}
	};

	private final void colorStyle(){
		SwingUtilities.invokeLater(colorRunnable);
	}
	
	private static final String MLET = "Mlet";
	private final JTabbedPane mainPanel = new JTabbedPane();
	private final JPanel cssPanel = new JPanel();
	private final JLabel tipCssLabel = new JLabel("<html>" +
			"The following styles will be shared for all HTMLMlet(s) in this project." +
			"<BR>if you need special styles for a HTMLMlet, invoke <STRONG>HTMLMlet.loadCSS</STRONG>." +
			"<BR>to set styles for a JComponent, please invoke <STRONG>HTMLMlet.setCSS</STRONG>." +
			"<BR><BR>for variables, input shortcut keys for word completion." +
			"</html>");
	CSSUndoableEditListener cssUndoListener;
	UndoManager cssUndoManager;
	final HCTextPane cssEditPane = new HCTextPane(){
		@Override
		public void paste(){
			try{
				cssUndoListener.setUndoModel(CSSUndoableEditListener.UNDO_MODEL_PASTE);
				super.paste();
				colorStyle();
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		@Override
		public void cut(){
			cssUndoListener.setUndoModel(CSSUndoableEditListener.UNDO_MODEL_CUT);
			super.cut();
			colorStyle();
		}
		@Override
		public void refreshCurrLineAfterKey(final int line) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					final int position = cssEditPane.getCaretPosition();
					try {
						final int line = getLineOfOffset(cssDocument, position);
						final int startOff = getLineStartOffset(cssDocument, line);
						final int endOff = getLineEndOffset(cssDocument, line);
						final int length = endOff - startOff;
						initSytleBlock(cssDocument.getText(startOff, length), startOff, true);
						
						buildSytleHighlight(cssEditPane, rem_pattern, REM_LIGHTER, 0, cssEditPane.getText(), true);
					} catch (final BadLocationException e) {
					}
				}
			});
		}
	};
	private final JScrollPane cssScrollPane = new JScrollPane(cssEditPane);
	private final Document cssDocument;
	
	private final void appendEnterText(final String txt, final int newPos) {
		final int oldPos = cssEditPane.getCaretPosition();
		try {
			cssDocument.insertString(oldPos, txt, null);
		} catch (final BadLocationException e) {
		}
		cssEditPane.setCaretPosition(newPos);
	}
	
	private final void actionOnEnterKey() throws Exception{
		final StringBuilder sb = new StringBuilder(24);
		boolean isDoneEnter = false;
		boolean isMovSelfBack = false;
		
		final int InsertPosition = cssEditPane.getCaretPosition();
		int line = getLineOfOffset(cssDocument, InsertPosition);
		sb.append("\n");
		
		//复制上行的缩进到新行中
		boolean inputSelfBackEnd = false;
		try {
			final int positionLine = line;
			while(line > 0){
				final int startIdx = getLineStartOffset(cssDocument, line);

				//获得缩进串
				final String lineStr = cssDocument.getText(startIdx, InsertPosition - startIdx);//-1去掉\n
				final char[] chars = lineStr.toCharArray();
				
				if(chars.length == 0){
					line--;
					continue;
				}
				int charIdxRemovedTab = 0;
				for (; charIdxRemovedTab < chars.length; charIdxRemovedTab++) {
					if(chars[charIdxRemovedTab] == ' ' || chars[charIdxRemovedTab] == '\t'){
						
					}else{
						break;
					}
				}
				final String trim_str = new String(chars, charIdxRemovedTab, chars.length - charIdxRemovedTab);
				inputSelfBackEnd = false;
				boolean nextRowIndent = false;
				for (int k = chars.length - 1; k >= 0 ; k--) {
					final char c = chars[k];
					if('}' == c){
						//查找前面是否有闭合
						boolean isFindStarter = false;
						for (int m = k - 1; m >= 0 ; m--) {
							if('{' == chars[m]){
								isFindStarter = true;
								break;
							}
						}
						inputSelfBackEnd = !isFindStarter;
						break;
					}
				}
				if(inputSelfBackEnd){
//					nextRowIndent = inputSelfBackEnd;//yyh
					//检查上行是否当前行已缩进。即在已有代码elsif xxx后进行回车，当前是否需要缩进
					final int startUpRowIdx = getLineStartOffset(cssDocument, positionLine - 1);
					try{
						final String upRowStr = cssDocument.getText(startUpRowIdx, startIdx - 1 - startUpRowIdx);
						final char[] upRowChars = upRowStr.toCharArray();
						int charIdxUpRowRemovedTab = 0;
						for (; charIdxUpRowRemovedTab < upRowChars.length; charIdxUpRowRemovedTab++) {
							if(upRowChars[charIdxUpRowRemovedTab] == ' ' || upRowChars[charIdxUpRowRemovedTab] == '\t'){
								
							}else{
								break;
							}
						}
						if(charIdxUpRowRemovedTab > charIdxRemovedTab){
							inputSelfBackEnd = false;//取消自缩进
						}else if(charIdxUpRowRemovedTab == charIdxRemovedTab){
//							if(isELSIFIndentKeyWords(upRowChars, charIdxUpRowRemovedTab, elsifChar)){
//								inputSelfBackEnd = false;//取消自缩进
//							}
						}
					}catch (final Exception ex) {
					}
				}
				int charNewIdxRemovedTab = charIdxRemovedTab;
				boolean withEndInd = false;
				{
					final int starter = trim_str.indexOf('{', 0);
					if(starter >= 0  && trim_str.indexOf('}', 0) < 0){
						nextRowIndent = true;
						chars[charNewIdxRemovedTab++] = '\t';
						withEndInd = true;
					}
				}
				boolean isNextIndentAlready = false;
				if(withEndInd){
					//检查下行是否已缩进，
					try{
						final String nextLine = cssDocument.getText(InsertPosition + 1, getLineEndOffset(cssDocument, positionLine + 2) - (InsertPosition + 1));
						
						final char[] nextLineChars = nextLine.toCharArray();
						
						int charIdxNextRemovedTab = 0;
						for (; charIdxNextRemovedTab < nextLineChars.length; charIdxNextRemovedTab++) {
							if(nextLineChars[charIdxNextRemovedTab] == ' ' || nextLineChars[charIdxNextRemovedTab] == '\t'){
								
							}else{
								break;
							}
						}
						if(charIdxNextRemovedTab > charIdxRemovedTab){
							isNextIndentAlready = true;
						}
					}catch (final Exception e) {
						if(e instanceof BadLocationException){
						}else{
							ExceptionReporter.printStackTrace(e);
						}
					}
				}
				
				try{
				final int newPosition;
//				if(charNewIdxRemovedTab > 0 && (inputSelfBackEnd == false)){//不能}自缩进
//					sb.append(String.valueOf(chars, 0, charNewIdxRemovedTab));
//				}
				if(nextRowIndent && (inputSelfBackEnd == false)){
					sb.append("\t");
				}
				if(charNewIdxRemovedTab != 0 || withEndInd){
					newPosition = InsertPosition + sb.length();
					if(withEndInd && (isNextIndentAlready == false)){//&& (nextRowIndent == false)
						sb.append("\n");
						sb.append(String.valueOf(chars, 0, charNewIdxRemovedTab - 1));//下一行，减少一个缩位
						
						final String strEnd = "}";
						sb.append(strEnd);
						final String txt = sb.toString();
						appendEnterText(txt, newPosition);
						isDoneEnter = true;
						colorStyle();
					}else if(inputSelfBackEnd == false){
						//复制上行的缩进
						sb.append(String.valueOf(chars, 0, charIdxRemovedTab));
						final String txt = sb.toString();
						final int newPos = InsertPosition + sb.length();
////						cssDocument.insertString(InsertPosition, txt, null);
//						final int newPos = InsertPosition + sb.length();
						appendEnterText(txt, newPos);
						isDoneEnter = true;
						return;
					}
					break;
				}else{
					break;
				}
				}catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}finally{
					if(inputSelfBackEnd && charNewIdxRemovedTab > 0){
						cssDocument.remove(startIdx, 1);//}之前的字符去掉一个缩位
						isMovSelfBack = true;
					}
				}
			}
			if(isDoneEnter == false){
				final String txt = (isMovSelfBack?"\n":sb.toString());
//				try {
//					cssDocument.insertString(InsertPosition, txt, null);
//				} catch (final BadLocationException e) {
//				}
				final int newPos = InsertPosition + txt.length() + (isMovSelfBack?-1:0);
				appendEnterText(txt, newPos);
			}
		} catch (final Throwable e) {
			if(e instanceof BadLocationException){
			}else{
				ExceptionReporter.printStackTrace(e);
			}
		}
	}
	
	public MletNodeEditPanel(){
		super();
		
		cssDocument = cssEditPane.getDocument();
		
		tipCssLabel.setBorder(new TitledBorder("Note :"));
		cssPanel.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
		cssPanel.add(tipCssLabel, BorderLayout.NORTH);
		{
			final JPanel tmpPanel = new JPanel(new BorderLayout());
			tmpPanel.setBorder(new TitledBorder("Styles Edit Area :"));
			tmpPanel.add(cssScrollPane, BorderLayout.CENTER);
			cssPanel.add(tmpPanel, BorderLayout.CENTER);
		}
		
		mainPanel.addTab(MLET, this);
		
		mainPanel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e){
			    final int selectedIndex = mainPanel.getSelectedIndex();
			    if(selectedIndex==1){
			    	cssEditPane.setText(designer.getProjCSS());
			    	colorStyle();
			    	
					cssUndoListener.isForbidRecordUndoEdit = false;
					cssUndoManager.discardAllEdits();
					cssEditPane.setCaretPosition(0);
			    }else{
			    	cssUndoListener.isForbidRecordUndoEdit = true;
			    }
		}});
		
		{
			final AbstractAction enterAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					try {
						cssUndoListener.setUndoModel(CSSUndoableEditListener.UNDO_MODEL_ENTER);
						actionOnEnterKey();
					} catch (final Exception e1) {
					}
				}
			};
			final KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
			cssEditPane.getInputMap().put(enterKey, enterAction);
		}
		
		//以下代码设置Tab跳过指定的空格
		{
			final FontMetrics fm = cssEditPane.getFontMetrics(cssEditPane.getFont());
			cssEditPane.setForeground(cssEditPane.getBackground());
			final int cw = fm.stringWidth("    ");
			final float f = cw;
			final TabStop[] tabs = new TabStop[10]; // this sucks
			for(int i = 0; i < tabs.length; i++){
				tabs[i] = new TabStop(f * (i + 1), TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
			}
			final TabSet tabset = new TabSet(tabs);
			final StyleContext sc = StyleContext.getDefaultStyleContext();
			final AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabset);
			cssEditPane.setParagraphAttributes(aset, false);
		}
		
		cssEditPane.addKeyListener(new KeyListener() {
			final boolean isMacOS = ResourceUtil.isMacOSX();
			final int fontHeight = cssEditPane.getFontMetrics(cssEditPane.getFont()).getHeight();
			
			@Override
			public void keyTyped(final KeyEvent event) {
				if(isEventConsumed){
					ScriptEditPanel.consumeEvent(event);//otherwise display shortcut key.
					return;
				}
				
				final char inputChar = event.getKeyChar();
				final int modifiers = event.getModifiers();

				final CodeHelper codeHelper = designer.codeHelper;
				
				if(isMacOS && (inputChar != 0 && inputChar == codeHelper.wordCompletionChar
						&& ((codeHelper.wordCompletionModifyCode == KeyEvent.VK_ALT && modifiers == 0) 
									|| (codeHelper.wordCompletionModifyMaskCode == modifiers)))){//注意：请同步从ScriptEditPanel
					try {
						final int caretPosition = cssEditPane.getCaretPosition();
						final Rectangle caretRect=cssEditPane.modelToView(caretPosition);
						final Point caretPointer = new Point(caretRect.x, caretRect.y);
						codeHelper.inputVariableForCSS(cssEditPane, caretPointer, fontHeight, caretPosition);
					} catch (final Exception e) {
						if(L.isInWorkshop){
							ExceptionReporter.printStackTrace(e);
						}
					}
					ScriptEditPanel.consumeEvent(event);
					return;
				}
				
				cssEditPane.refreshCurrLineAfterKey(0);
				
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
			}
			
			boolean isEventConsumed;
			
			@Override
			public void keyPressed(final KeyEvent event) {
				final int keycode = event.getKeyCode();
	            final int modifiers = event.getModifiers();
	            final CodeHelper codeHelper = designer.codeHelper;
				final int wordCompletionModifyMaskCode = codeHelper.wordCompletionModifyMaskCode;
				//无输入字符时的触发提示代码
				isEventConsumed = false;
				if(keycode == codeHelper.wordCompletionCode && (modifiers & wordCompletionModifyMaskCode) == wordCompletionModifyMaskCode){
					//注意：请同步从ScriptEditPanel
					try {
						final int caretPosition = cssEditPane.getCaretPosition();
						final Rectangle caretRect=cssEditPane.modelToView(caretPosition);
						final Point caretPointer = new Point(caretRect.x, caretRect.y);
						codeHelper.inputVariableForCSS(cssEditPane, caretPointer, fontHeight, caretPosition);
					} catch (final Exception e) {
						if(L.isInWorkshop){
							ExceptionReporter.printStackTrace(e);
						}
					}
					ScriptEditPanel.consumeEvent(event);
					isEventConsumed = true;
					return;
				}
				
				if (modifiers == KeyEvent.CTRL_MASK) {
	                if (keycode == KeyEvent.VK_Z) {
	                	//ctrl + z
	                    cssundo();
	                }else if(keycode == KeyEvent.VK_Y) {
	                	//ctrl + y
	                    cssredo();
	                }
	            }else if(keycode == KeyEvent.VK_Z){
					if(isMacOS){
		                if (modifiers == KeyEvent.META_MASK) {
		                	//cmd+z
		                    cssundo();
		                }else if(modifiers == (KeyEvent.META_MASK | KeyEvent.SHIFT_MASK)) {
		                	cssredo();
		                }
	            	}
	            }
				
				if(keycode == KeyEvent.VK_ESCAPE){
					TabHelper.pushEscKey();
					isEventConsumed = true;
					return;
				}
				
				cssUndoListener.setUndoModel(CSSUndoableEditListener.UNDO_MODEL_TYPE);
			}
		});
		
		cssEditPane.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(final FocusEvent e) {
				designer.setProjCSS(cssEditPane.getText());
			}
			@Override
			public void focusGained(final FocusEvent e) {
			}
		});
	}
	
	private final void cssundo() {
		if (cssUndoManager.canUndo()){
			cssUndoManager.undo();
			colorStyle();
		}
	}

	private final void cssredo() {
		if (cssUndoManager.canRedo()){
			cssUndoManager.redo();
			colorStyle();
		}
	}
	
	@Override
	public void init(final MutableTreeNode data, final JTree tree) {
		super.init(data, tree);
		
		if(cssUndoManager == null){
			cssUndoManager = new UndoManager();
			cssUndoManager.setLimit(100);
			cssUndoListener = new CSSUndoableEditListener(this, cssUndoManager);
			
			cssEditPane.getDocument().addUndoableEditListener(cssUndoListener);
		}
		
		if(NodeEditPanelManager.meetHTMLMletLimit((HPNode)((DefaultMutableTreeNode)data).getUserObject())){
			if(mainPanel.getTabCount() < 2){
				mainPanel.addTab("CSS Styles", cssPanel);
			}
			mainPanel.setTitleAt(0, "Mlet/HTMLMlet");
		}else{
			//兼容以前的screen://myMlet
			if(mainPanel.getTabCount() == 2){
				mainPanel.remove(1);
			}
			mainPanel.setTitleAt(0, MLET);
		}

		mainPanel.setSelectedIndex(0);
	}
	
	@Override
	public JComponent getMainPanel(){
		return mainPanel;
	}

}

class CSSUndoableEditListener implements UndoableEditListener{
	static final int UNDO_MODEL_TYPE = 1;
	static final int UNDO_MODEL_PASTE = 2;
	static final int UNDO_MODEL_CUT = 3;
	static final int UNDO_MODEL_ENTER = 4;
	
	boolean isForbidRecordUndoEdit = true;
	final UndoManager manager;
	final MletNodeEditPanel panel;
	private int undoModel = 0;
	
	final void setUndoModel(final int model){
		undoModel = model;
	}
	
	CSSUndoableEditListener(final MletNodeEditPanel panel, final UndoManager manager){
		this.manager = manager;
		this.panel = panel;
	}
	
	@Override
	public void undoableEditHappened(final UndoableEditEvent e) {
		if(isForbidRecordUndoEdit == false){
			final UndoableEdit edit = e.getEdit();
			if(edit instanceof AbstractDocument.DefaultDocumentEvent){
				final AbstractDocument.DefaultDocumentEvent dde = (AbstractDocument.DefaultDocumentEvent)edit;
				if(dde.getType() == DocumentEvent.EventType.CHANGE){
					return;
				}
			}
			manager.addEdit(new CSSUndoableEdit(panel, edit, undoModel));
		}
	}
}

class CSSUndoableEdit implements UndoableEdit{
	final int beforeCaretPos;
	int afterCaretPos;
	final UndoableEdit base;
	final JTextPane cssPane;
	final int undoModel;
	final Caret caret;
	int selectionLen;
	final MletNodeEditPanel panel;
	final boolean isModi;
	DocumentEvent.EventType type;
	
	CSSUndoableEdit(final MletNodeEditPanel panel, final UndoableEdit base, final int undoModel){
		this.panel = panel;
		
		this.cssPane = panel.cssEditPane;
		this.caret = cssPane.getCaret();
		beforeCaretPos = caret.getDot();
		this.base = base;
		this.undoModel = undoModel;
		isModi = this.panel.isModified();
		
		if(isModi == false){
			this.panel.notifyModified(true);//由于REMOVE和INSERT都是isModi==false,所以强制后续的INSERT为ture
		}
		
		if(base instanceof DefaultDocumentEvent){
			final DefaultDocumentEvent dde = (DefaultDocumentEvent)base;
			type = dde.getType();
			if(type == DocumentEvent.EventType.REMOVE){
				if(undoModel == CSSUndoableEditListener.UNDO_MODEL_TYPE){
					selectionLen = 1;
				}else{
					selectionLen = dde.getLength();
				}
			}else if(type == DocumentEvent.EventType.INSERT){
				if(undoModel == CSSUndoableEditListener.UNDO_MODEL_TYPE){
				}else{
					selectionLen = dde.getLength();
				}
			}
		}
		
		if(undoModel == CSSUndoableEditListener.UNDO_MODEL_ENTER){
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					try{
						Thread.sleep(ThreadPriorityManager.UI_WAIT_FOR_EVENTQUEUE);
					}catch (final Exception e) {
					}
					afterCaretPos = caret.getDot();
//					System.out.println("afterCaretPos : " + afterCaretPos);
				}
			});
		}
	}
	
	@Override
	public void undo() throws CannotUndoException {
		base.undo();
		if(beforeCaretPos > 0){
			if(undoModel == CSSUndoableEditListener.UNDO_MODEL_PASTE){
				if(type == DocumentEvent.EventType.INSERT){
					caret.setDot(beforeCaretPos - selectionLen);
				}else{
					caret.setDot(beforeCaretPos);
				}
			}
		}
		if(isModi == false){
			panel.notifyModified(false);
		}
	}

	@Override
	public boolean canUndo() {
		return base.canUndo();
	}

	@Override
	public void redo() throws CannotRedoException {
		base.redo();
		if(isModi == false){
			panel.notifyModified(true);
		}
		
		if(afterCaretPos > 0){
			if(undoModel == ScriptUndoableEditListener.UNDO_MODEL_ENTER){
				caret.setDot(afterCaretPos);
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
		return base.isSignificant();
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