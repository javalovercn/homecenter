package hc.server.ui.design.hpj;

import hc.App;
import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.ThreadPriorityManager;
import hc.res.ImageSrc;
import hc.server.ui.ClientDesc;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.Designer;
import hc.server.ui.design.code.CodeHelper;
import hc.server.ui.design.code.CodeWindow;
import hc.server.ui.design.code.TabHelper;
import hc.server.util.CSSUtil;
import hc.server.util.IDArrayGroup;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
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
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
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
import javax.swing.tree.MutableTreeNode;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class CSSNodeEditPanel extends NameEditPanel {
	public final CSSJumpRunnable jumpRunnable;
	
	private static final SimpleAttributeSet CLASS_LIGHTER = ScriptEditPanel.build(Color.decode("#088A29"), false);//dark green
	private static final SimpleAttributeSet ITEM_LIGHTER = ScriptEditPanel.build(Color.decode("#0431B4"), false);
	private static final SimpleAttributeSet SPLITTER_LIGHTER = ScriptEditPanel.build(Color.BLACK, false);
	private static final SimpleAttributeSet VALUE_LIGHTER = ScriptEditPanel.build(Color.decode("#B18904"), false);
	private static final SimpleAttributeSet VARIABLE_LIGHTER = ScriptEditPanel.build(Color.decode("#DF0101"), false);
	
	private static final SimpleAttributeSet JUMP_CLASS_DEF_LIGHTER = ScriptEditPanel.buildBackground(new Color(165, 199, 234));
	
	private static final Pattern class_pattern = Pattern.compile("(\\s*?(.*?)\\s*?)\\{");
	private static final Pattern item_pattern = Pattern.compile("(\\b*?([a-zA-Z_0-9-]+)\\b*?\\s*?):");
	private static final Pattern css_rem_pattern = Pattern.compile("(/\\*(.*?)\\*/)", Pattern.MULTILINE|Pattern.DOTALL);
	private static final Pattern var_pattern = Pattern.compile("(\\$(.*?)\\$)");
	private static final Pattern spliter_pattern = Pattern.compile("([\\{\\};:])");
	
	private final void initSytleBlock(final String text, final int offset, final boolean isReplace) {
		final StyledDocument document = (StyledDocument)cssEditPane.getDocument();
		document.setCharacterAttributes(offset, text.length(), VALUE_LIGHTER, true);
		
		buildSytleHighlight(cssEditPane, class_pattern, CLASS_LIGHTER, offset, text, isReplace);
		buildSytleHighlight(cssEditPane, item_pattern, ITEM_LIGHTER, offset, text, isReplace);//要置于字符串之前，因为字符串中可能含有数字
		buildSytleHighlight(cssEditPane, var_pattern, VARIABLE_LIGHTER, offset, text, isReplace);
		buildSytleHighlight(cssEditPane, css_rem_pattern, ScriptEditPanel.REM_LIGHTER, offset, text, isReplace);//字符串中含有#{}，所以要置于STR_LIGHTER之前
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
			
			buildSytleHighlight(cssEditPane, css_rem_pattern, ScriptEditPanel.REM_LIGHTER, 0, allText, true);
		}
	};

	private final void colorStyle(){
		SwingUtilities.invokeLater(colorRunnable);
	}
	
	private final JPanel cssPanel = new JPanel();
	private final JLabel tipCssLabel = new JLabel("<html>" +
			"the above styles will be shared to all HTMLMlet/Dialog(s) in current project." +
			"<BR><BR>" +
			"if you want special styles for a HTMLMlet/Dialog, please invoke <STRONG>loadCSS</STRONG>." +
			"<BR><BR>to get CSS (2.2) properties or variables, please press shortcut keys for word completion." +
			"</html>");
	
	CSSUndoableEditListener cssUndoListener;
	UndoManager cssUndoManager;
	final HCTextPane cssEditPane = new HCTextPane(new CSSSelectWordAction()){
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
						final int line = ScriptEditPanel.getLineOfOffset(cssDocument, position);
						final int startOff = ScriptEditPanel.getLineStartOffset(cssDocument, line);
						final int endOff = ScriptEditPanel.getLineEndOffset(cssDocument, line);
						final int length = endOff - startOff;
						initSytleBlock(cssDocument.getText(startOff, length), startOff, true);
						
						buildSytleHighlight(cssEditPane, css_rem_pattern, ScriptEditPanel.REM_LIGHTER, 0, cssEditPane.getText(), true);
					} catch (final BadLocationException e) {
					}
				}
			});
		}
	};
	private final JScrollPane cssScrollPane;
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
		int line = ScriptEditPanel.getLineOfOffset(cssDocument, InsertPosition);
		sb.append("\n");
		
		//复制上行的缩进到新行中
		boolean inputSelfBackEnd = false;
		try {
			final int positionLine = line;
			while(line > 0){
				final int startIdx = ScriptEditPanel.getLineStartOffset(cssDocument, line);

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
					final int startUpRowIdx = ScriptEditPanel.getLineStartOffset(cssDocument, positionLine - 1);
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
						final String nextLine = cssDocument.getText(InsertPosition + 1, ScriptEditPanel.getLineEndOffset(cssDocument, positionLine + 2) - (InsertPosition + 1));
						
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
	
	final JButton formatBtn = new JButton(ScriptEditPanel.FORMAT_BUTTON_TEXT);
	final JButton commentBtn = new JButton("Comment/Uncomment");
	
	public CSSNodeEditPanel(){
		super();
	
//		Disable Word wrap in JTextPane		
		final JPanel NoWrapPanel = new JPanel(new BorderLayout());
		NoWrapPanel.add(cssEditPane, BorderLayout.CENTER);
		cssScrollPane = new JScrollPane(NoWrapPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final LineNumber lineNumber = new LineNumber(cssEditPane);
		cssScrollPane.setRowHeaderView(lineNumber);
		cssScrollPane.getVerticalScrollBar().setUnitIncrement(ServerUIUtil.SCROLLPANE_VERTICAL_UNIT_PIXEL);
		
		jumpRunnable = new CSSJumpRunnable() {
			final SimpleAttributeSet defaultBackground = ScriptEditPanel.buildBackground(cssEditPane.getBackground());
			final Runnable jumpLoc = new Runnable() {
				@Override
				public void run() {
					cssEditPane.setCaretPosition(getOffSet());
					try {
						final Rectangle modelToView = cssEditPane.modelToView(getOffSet());
						modelToView.y += cssScrollPane.getHeight();
						cssEditPane.scrollRectToVisible(modelToView);
					} catch (final Throwable e) {
						e.printStackTrace();
					}
				}
			};
			
			final Runnable lightRun = new Runnable() {
				@Override
				public void run() {
					try {
						SwingUtilities.invokeAndWait(jumpLoc);
					} catch (final Throwable e) {
						e.printStackTrace();
					}
					
					try{
						for (int i = 0; i < 5; i++) {
							cssEditPane.getStyledDocument().setCharacterAttributes(getOffSet(), getLen(), JUMP_CLASS_DEF_LIGHTER, false);
							Thread.sleep(500);
							cssEditPane.getStyledDocument().setCharacterAttributes(getOffSet(), getLen(), defaultBackground, false);
							Thread.sleep(500);
						}
					}catch (final Exception e) {
					}
				}
			};
			
			@Override
			public void run() {
				ContextManager.getThreadPool().run(lightRun);
			}
			
			private final int getOffSet(){
				return offset;
			}
			
			private final int getLen(){
				return len;
			}
		};
	
		commentBtn.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + /)<BR><BR>comment/uncomment the selected rows.</html>");
		commentBtn.setIcon(Designer.loadImg("comment_16.png"));
		final Action commentAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(CSSUtil.comment(cssEditPane)){
							colorRunnable.run();
							cssEditPane.requestFocus();
						}
					}
				});
			}
		};
		ResourceUtil.buildAcceleratorKeyOnAction(commentAction, KeyEvent.VK_SLASH);
		commentBtn.addActionListener(commentAction);
		commentBtn.getActionMap().put("commentCSSAction", commentAction);
		commentBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
		        (KeyStroke) commentAction.getValue(Action.ACCELERATOR_KEY), "commentCSSAction");
		
		formatBtn.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + F)<BR><BR>format the CSS.</html>");
		formatBtn.setIcon(Designer.loadImg(ImageSrc.FORMAT_16_PNG));
		
		final Action formatAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						cssEditPane.setText(CSSUtil.format(cssEditPane.getText()));
						colorRunnable.run();
						cssEditPane.requestFocus();
					}
				});
			}
		};
		
		ResourceUtil.buildAcceleratorKeyOnAction(formatAction, KeyEvent.VK_F);
		formatBtn.addActionListener(formatAction);
		formatBtn.getActionMap().put("formatCSSAction", formatAction);
		formatBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
		        (KeyStroke) formatAction.getValue(Action.ACCELERATOR_KEY), "formatCSSAction");
		
		cssDocument = cssEditPane.getDocument();
		
		tipCssLabel.setBorder(new TitledBorder("Tip :"));
		cssPanel.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
		
		if(IDArrayGroup.checkAndAdd(IDArrayGroup.MSG_CSS_NOTE)){
			cssPanel.add(tipCssLabel, BorderLayout.NORTH);
		}else{
			cssPanel.add(tipCssLabel, BorderLayout.SOUTH);
		}
		
		{
			final JPanel editToolPane = new JPanel(new BorderLayout());
			
			final JPanel tmpPanel = new JPanel(new BorderLayout());
			tmpPanel.setBorder(new TitledBorder("Styles Edit Area :"));
			tmpPanel.add(cssScrollPane, BorderLayout.CENTER);
			
			final JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
			toolPanel.setBorder(new TitledBorder(""));
			toolPanel.add(formatBtn);
			toolPanel.add(commentBtn);
			editToolPane.add(toolPanel, BorderLayout.NORTH);
			editToolPane.add(tmpPanel, BorderLayout.CENTER);
			
			cssPanel.add(editToolPane, BorderLayout.CENTER);
		}
		
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
						codeHelper.inputForCSSInCSSEditor(0, cssEditPane, cssDocument, caretPointer, fontHeight, caretPosition);
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
						codeHelper.inputForCSSInCSSEditor(0, cssEditPane, cssDocument, caretPointer, fontHeight, caretPosition);
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
				final CodeWindow window = designer.codeHelper.window;
				if(window.isWillOrAlreadyToFront){
					window.hide(true);
				}
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
		
    	String text = designer.getProjCSS();
		if(text.indexOf('\r') >= 0){
			text = text.replace("\r", "");
		}
		cssEditPane.setText(text);
    	colorStyle();
    	
	}
	
	@Override
	public void loadAfterShow(final Runnable run){
		cssUndoListener.isForbidRecordUndoEdit = false;
		cssUndoManager.discardAllEdits();
		cssEditPane.setCaretPosition(0);
		cssEditPane.requestFocus();
		
		super.loadAfterShow(run);
	}
	
	@Override
	public void notifyLostEditPanelFocus(){
		cssUndoListener.isForbidRecordUndoEdit = true;
		super.notifyLostEditPanelFocus();
		
		final String sameFullName = designer.updateCssClassOfProjectLevel(cssDocument);
		if(sameFullName != null){
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					App.showMessageDialog(designer, sameFullName, ResourceUtil.getErrorI18N(), JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
				}
			});
		}
	}
	
	@Override
	public JComponent getMainPanel(){
		return cssPanel;
	}

}

class CSSUndoableEditListener implements UndoableEditListener{
	static final int UNDO_MODEL_TYPE = 1;
	static final int UNDO_MODEL_PASTE = 2;
	static final int UNDO_MODEL_CUT = 3;
	static final int UNDO_MODEL_ENTER = 4;
	
	boolean isForbidRecordUndoEdit = true;
	final UndoManager manager;
	final CSSNodeEditPanel panel;
	private int undoModel = 0;
	
	final void setUndoModel(final int model){
		undoModel = model;
	}
	
	CSSUndoableEditListener(final CSSNodeEditPanel panel, final UndoManager manager){
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
	final CSSNodeEditPanel panel;
	final boolean isModi;
	final long saveToken;
	DocumentEvent.EventType type;
	
	CSSUndoableEdit(final CSSNodeEditPanel panel, final UndoableEdit base, final int undoModel){
		this.panel = panel;
		
		this.cssPane = panel.cssEditPane;
		this.caret = cssPane.getCaret();
		beforeCaretPos = caret.getDot();
		this.base = base;
		this.undoModel = undoModel;
		isModi = this.panel.isModified();
		saveToken = this.panel.getSaveToken();
		
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
		if(saveToken == panel.getSaveToken()){
			if(isModi == false){
				panel.notifyModified(false);
			}
		}else{
			if(panel.isModified() == false){
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
		if(saveToken == panel.getSaveToken()){
			if(isModi == false){
				panel.notifyModified(true);
			}
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