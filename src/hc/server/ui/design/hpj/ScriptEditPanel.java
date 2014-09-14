package hc.server.ui.design.hpj;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.ui.HCByteArrayOutputStream;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.Designer;
import hc.server.ui.design.UpgradeManager;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.util.ResourceUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.tree.MutableTreeNode;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public abstract class ScriptEditPanel extends NodeEditPanel {
	static Highlighter.HighlightPainter ERROR_CODE_LINE_LIGHTER = new DefaultHighlighter.
			DefaultHighlightPainter(Color.RED);
	static {
		UpgradeManager.createRunTestDir();
	}
	//要置于createRunTestDir之后
	public static HCJRubyEngine runTestEngine;
	
	private static final SimpleAttributeSet STR_LIGHTER = build(Color.decode("#4EA539"), false);
	private static final SimpleAttributeSet REM_LIGHTER = build(Color.decode("#3AC2EB"), false);
	private static final SimpleAttributeSet MAP_LIGHTER = build(Color.BLACK, true);
	private static final SimpleAttributeSet KEYWORDS_LIGHTER = build(Color.BLUE, true);
	private static final SimpleAttributeSet NUM_LIGHTER = build(Color.RED, false);
	private static final SimpleAttributeSet DEFAULT_LIGHTER = build(Color.BLACK, false);
	
	private static final Pattern str_pattern = Pattern.compile("\".*?(?<!\\\\)\"");
	private static final Pattern keywords_pattern = Pattern.compile("\\b(BEGIN|END|__ENCODING__|__END__|__FILE__|__LINE__|alias|" +
			"and|begin|break|case|class|def|defined?|do|else|elsif|end|" +
			"ensure|false|for|if|in|import|module|next|nil|not|or|raise|redo|require|rescue|retry|return|" +
			"self|super|then|true|undef|unless|until|when|while|yield)\\b", Pattern.MULTILINE);
	private static final String[] Indentation = {"begin", "case ", "class ", "def ", "else", 
		"elsif ", "for ", "if ", "module ", "when ", "while ", "rescue "};
	private static final Pattern num_pattern = Pattern.compile("\\b\\d+\\b", Pattern.MULTILINE);
	private static final Pattern hc_map_pattern = Pattern.compile("\\$_hcmap\\b");
	private static final Pattern rem_pattern = Pattern.compile("#.*(?=\n)?");

	HPNode currItem;
	final JScrollPane scrollpane;
	final JLabel errRunInfo = new JLabel("");
	final JButton testBtn = new JButton("Test Script");
	final HCByteArrayOutputStream iconByteArrayos = new HCByteArrayOutputStream();
	final JTextField nameField = new JTextField();
	final JRubyErrorHCTimer errorTimer = new JRubyErrorHCTimer("JRubyErrorHCTimer", 1000, false);
	boolean isInited = false;
	
	abstract Map<String, String> buildMapScriptParameter();
	
	public ScriptEditPanel() {
		errRunInfo.setForeground(Color.RED);
		errRunInfo.setOpaque(true);

		errorTimer.setErrorLable(errRunInfo, testBtn);
		
		{

			final Action testAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Map<String, String> map = buildMapScriptParameter();
					if(runTestEngine != null && runTestEngine.getEvalException() != null){
						//清空旧错误
						jtaScript.getHighlighter().removeAllHighlights();
					}
					
					if(Designer.getInstance().tryBuildTestJRuby() == false){
						return;
					}
					
					ProjectContext context = new ProjectContext(Designer.getInstance().getCurrProjID(), Designer.getInstance().getCurrProjVer());
					context.__tmp_target = "";
					RubyExector.run(jtaScript.getText(), map, runTestEngine, context);
					final ScriptException evalException = runTestEngine.getEvalException();
					if(evalException != null){
//						final Object runnable = Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation");
//						 if (runnable != null && runnable instanceof Runnable){
//							 ((Runnable)runnable).run();
//						 }
						 
						Toolkit.getDefaultToolkit().beep();
						
						errRunInfo.setText(evalException.getMessage());
						errRunInfo.setBackground(testBtn.getBackground());
						final int line = evalException.getLineNumber();
						char[] chars = jtaScript.getText().toCharArray();
						
						int currRow = 1;
						int startIdx = -1, endIdx = -1;
						if(line == 1){
							startIdx = 0;
						}
						for (int i = 0; i < chars.length; i++) {
							if(chars[i] == '\n'){
								if(++currRow == line && (startIdx == -1)){
									startIdx = i + 1;
								}else if(startIdx >=0 && endIdx == -1){
									endIdx = i;
									break;
								}
								
							}
						}
						if(startIdx >= 0){
							if(endIdx < 0){
								endIdx = chars.length;
							}
							try {
								jtaScript.getHighlighter().addHighlight(startIdx, endIdx, ERROR_CODE_LINE_LIGHTER);
							} catch (BadLocationException e1) {
							}
							jtaScript.setCaretPosition(startIdx);
						}
					}else{
						errRunInfo.setBackground(Color.GREEN);
						errRunInfo.setText(" ");
						
						errorTimer.resetTimerCount();
						errorTimer.setEnable(true);
					}
				}
			};
			ResourceUtil.buildAcceleratorKeyOnAction(testAction, KeyEvent.VK_T);//同时支持Windows下的Ctrl+S和Mac下的Command+S
			testBtn.addActionListener(testAction);
			testBtn.getActionMap().put("testAction", testAction);
			testBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) testAction.getValue(Action.ACCELERATOR_KEY), "testAction");
		}
		testBtn.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + T)" +
				"<BR>" +
				"<BR>if no error in script, then nothing display;" +
				"<BR>if error, a red waring message is displayed in bottom.</html>");
		testBtn.setIcon(Designer.loadImg("test_16.png"));

		//		jtaScript.setTabSize(2);
		
		//以下代码设置Tab跳过指定的空格
		FontMetrics fm = jtaScript.getFontMetrics(jtaScript.getFont());
		int cw = fm.stringWidth("    ");
		float f = (float)cw;
		TabStop[] tabs = new TabStop[10]; // this sucks
		for(int i = 0; i < tabs.length; i++){
			tabs[i] = new TabStop(f * (i + 1), TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
		}
		TabSet tabset = new TabSet(tabs);
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabset);
		jtaScript.setParagraphAttributes(aset, false);	
		
		jtaScript.addKeyListener(new KeyListener() {
			final Document doc = jtaScript.getDocument();
			int endCompIdx = 0;
			final int[] end = {'e', 'n', 'd'};
			@Override
			public void keyTyped(KeyEvent e) {
//				if(e.isControlDown() || e.isAltDown()){
//					return;
//				}
		        position = jtaScript.getCaretPosition();
				synchronized (run) {
					//复制上行的缩进到新行中
					final char inputChar = e.getKeyChar();
					if(inputChar != KeyEvent.VK_ENTER){
						if(endCompIdx + 1 <= end.length && inputChar == end[endCompIdx]){
							endCompIdx++;
						}else{
							endCompIdx = 0;
						}
					}
					if(inputChar == KeyEvent.VK_ENTER){
						boolean inputEnd = (endCompIdx == end.length);
						endCompIdx = 0;
						newline = true;
						try {
							int line = getLineOfOffset(doc, position);
							while(line > 0){
								int startIdx = getLineStartOffset(doc, line - 1);
		
								//获得缩进串
								final String lineStr = doc.getText(startIdx, position - startIdx);
								final char[] chars = lineStr.toCharArray();
								int i = 0;
								for (; i < chars.length; i++) {
									if(chars[i] == ' ' || chars[i] == '\t'){
										
									}else{
										break;
									}
								}
								final String trim_str = lineStr.trim();
								for (int j = 0; j < Indentation.length; j++) {
									if(trim_str.startsWith(Indentation[j])){
										chars[i++] = '\t';
										break;
									}
								}
								boolean isbackIndent = false;
								if(trim_str.equals("end")){
									if(i > 0){
										isbackIndent = true;
									}
								}else{
									if(inputEnd){//可能是end收尾的变量
										inputEnd = false;
									}
								}

								try{
								if(i != 0 || isbackIndent){
									if(inputEnd){
										doc.insertString(position, String.valueOf(chars, 0, i - 1), null);//下一行，减少一个缩位
									}else{
										doc.insertString(position, String.valueOf(chars, 0, i), null);
									}
									break;
								}else{
									//取上一行
//									line--;
									break;
								}
								}finally{
									if(inputEnd && i > 0){
										doc.remove(startIdx, 1);//end之前的字符去掉一个缩位
									}
								}
							}
						} catch (BadLocationException e1) {
							e1.printStackTrace();
						}
					}
					
					//更新代码样式
					SwingUtilities.invokeLater(run);
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
		    public void keyPressed(KeyEvent e) {
		        if (e.getSource() == jtaScript) {
		            final int keycode = e.getKeyCode();
		            final int modi = e.getModifiers();
		            if (modi == KeyEvent.CTRL_MASK) {
		                if (keycode == KeyEvent.VK_Z) {
		                	//ctrl + z
		                    undo();
		                }else if(keycode == KeyEvent.VK_Y) {
		                	//ctrl + y
		                    redo();
		                }
		            }else if(keycode == KeyEvent.VK_Z){
		            	if(ResourceUtil.isMacOSX()){
			                if (modi == KeyEvent.META_MASK) {
			                	//cmd+z
			                    undo();
			                }else if(modi == (KeyEvent.META_MASK | KeyEvent.SHIFT_MASK)) {
			                	redo();
			                }
		            	}
		            }
		        }
		    }
		});

		LineNumber lineNumber = new LineNumber(jtaScript);
		scrollpane = new JScrollPane(jtaScript, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollpane.setRowHeaderView(lineNumber);

		nameField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				currItem.name = nameField.getText();
				tree.updateUI();
				notifyModified();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				modify();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				modify();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				modify();
			}
		});
		nameField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				currItem.name = nameField.getText();
				tree.updateUI();
				notifyModified();
			}
		});

	}
	
	public abstract void updateScript(String script);
	
	public void notifyModified(){
		if(isInited){
			currItem.getContext().modified.setModified(true);
		}
	}
	
	static int getLineOfOffset(final Document doc, final int offset) throws BadLocationException {
	    if (offset < 0) {
	        throw new BadLocationException("Can't translate offset to line", -1);
	    } else if (offset > doc.getLength()) {
	        throw new BadLocationException("Can't translate offset to line", doc.getLength() + 1);
	    } else {
	        return doc.getDefaultRootElement().getElementIndex(offset);
	    }
	}

	static int getLineStartOffset(final Document doc, final int line) throws BadLocationException {
	    final Element map = doc.getDefaultRootElement();
	    if (line < 0) {
	        throw new BadLocationException("Negative line", -1);
	    } else if (line >= map.getElementCount()) {
	        throw new BadLocationException("No such line", doc.getLength() + 1);
	    } else {
	        Element lineElem = map.getElement(line);
	        return lineElem.getStartOffset();
	    }
	}
	
	static int getLineEndOffset(final Document doc, final int line) throws BadLocationException {
	    final Element map = doc.getDefaultRootElement();
	    if (line < 0) {
	        throw new BadLocationException("Negative line", -1);
	    } else if (line >= map.getElementCount()) {
	        throw new BadLocationException("No such line", doc.getLength() + 1);
	    } else {
	        Element lineElem = map.getElement(line);
	        return lineElem.getEndOffset();
	    }
	}
	
	public void init(MutableTreeNode data, JTree tree) {
		super.init(data, tree);
		
		isInited = false;
		
		jtaScript.setText("");
		errRunInfo.setText("");

		currItem = (HPNode)currNode.getUserObject();
		nameField.setText(currItem.name);
	}
	boolean undoInProcess = false;
	PositionUndoManager undoManager;
	final JTextPane jtaScript = new JTextPane(){
		@Override
		public void paste(){
			synchronized (run) {
				undoManager.recordPosition(true);
				super.paste();
				SwingUtilities.invokeLater(run);
			}
		}
		@Override
		public void cut(){
			synchronized (run) {
				undoManager.recordPosition(true);
				super.cut();
				SwingUtilities.invokeLater(run);	
			}
		}
	};
	int position;
	boolean newline = false;
	final Runnable run = new Runnable() {
		@Override
		public void run() {
			synchronized (run) {
				initColor(false);
				notifyModified();
				updateScript(jtaScript.getText());		
			}
		}
	};
	void initColor(final boolean isLoadColor){
		undoInProcess = false;
		if(isLoadColor){
			if(undoManager != null){
				jtaScript.getDocument().removeUndoableEditListener(undoManager);
				undoManager.die();
			}
			undoManager = new PositionUndoManager(jtaScript){
				public void undoableEditHappened(UndoableEditEvent e) {
					synchronized (run) {
						if(undoInProcess){
							this.addEdit(e.getEdit());
						}				
					}
				}
			};
			undoManager.setLimit(20);
			jtaScript.getDocument().addUndoableEditListener(undoManager);
		}
		try{
			int colorOffset = 0, colorLen = -1;
			if(position != 0){
				int lineNo = getLineOfOffset(jtaScript.getDocument(), position);
				if(newline){
					colorOffset = getLineStartOffset(jtaScript.getDocument(), lineNo - 1);
				}else{
					colorOffset = getLineStartOffset(jtaScript.getDocument(), lineNo);
				}
				colorLen = getLineEndOffset(jtaScript.getDocument(), lineNo) - colorOffset;
				position = 0;
			}else{
			}
			newline = false;
			
			final String text = jtaScript.getText(colorOffset, (colorLen==-1)?jtaScript.getText().length():colorLen);
			if(colorLen==-1 && text.indexOf("\r") >= 0){
				jtaScript.setText(text.replace("\r", ""));
				colorLen = jtaScript.getText().length();
			}
			
			initBlock(text, colorOffset);
		}catch (Exception e) {
		}
		undoInProcess = true;
//		buildHighlight(jtaScript, first_rem_pattern, REM_LIGHTER);
	}

	public void initBlock(final String text, int offset) {
		final StyledDocument document = (StyledDocument)jtaScript.getDocument();
		document.setCharacterAttributes(offset, text.length(), DEFAULT_LIGHTER, true);
		
		buildHighlight(jtaScript, hc_map_pattern, MAP_LIGHTER, offset, text);
		buildHighlight(jtaScript, num_pattern, NUM_LIGHTER, offset, text);//要置于字符串之前，因为字符串中可能含有数字
		buildHighlight(jtaScript, keywords_pattern, KEYWORDS_LIGHTER, offset, text);
		buildHighlight(jtaScript, rem_pattern, REM_LIGHTER, offset, text);//字符串中含有#{}，所以要置于STR_LIGHTER之前
		buildHighlight(jtaScript, str_pattern, STR_LIGHTER, offset, text);//?<!\\\"
	}
	
	private void buildHighlight(JTextPane jta, Pattern pattern, SimpleAttributeSet attributes, int offset, String text) {
		final StyledDocument document = (StyledDocument)jta.getDocument();
		final Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			int start = matcher.start() + offset;
			int end = matcher.end() + offset;
			document.setCharacterAttributes(start, end - start, attributes, false);
		}
	}

	public void undo() {
		synchronized (run) {
			if (undoManager.canUndo()){
				undoManager.undo();
				undoInProcess = false;
				jtaScript.setText(jtaScript.getText());
				try{
					jtaScript.setCaretPosition(undoManager.getPostion());
				}catch (Exception e) {
					e.printStackTrace();
				}
				SwingUtilities.invokeLater(run);
			}
		}
	}

	public void redo() {
		synchronized (run) {
			if (undoManager.canRedo()){
				undoManager.redo();
				undoInProcess = false;
				jtaScript.setText(jtaScript.getText());
				try{
					jtaScript.setCaretPosition(undoManager.getPostion());
				}catch (Exception e) {
				}
				SwingUtilities.invokeLater(run);
			}
		}
	}

	private static SimpleAttributeSet build(Color c, boolean bold){
		 SimpleAttributeSet attributes = new SimpleAttributeSet();
		 StyleConstants.setForeground(attributes, c);
		 StyleConstants.setBold(attributes, bold);
//		 StyleConstants.setFontSize(attributes, fontSize);
//		 StyleConstants.setFontFamily(attrSet, "黑体");
		 return attributes;
	}

	public static void rebuildJRubyEngine() {
		terminateJRubyEngine();
		runTestEngine = new HCJRubyEngine(UpgradeManager.RUN_TEST_DIR.getAbsolutePath());
	}

	public static void terminateJRubyEngine() {
		if(runTestEngine != null){
			runTestEngine.terminate();
			runTestEngine = null;
		}
	}
	

}
class LineNumber extends JComponent {
	private final static Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN,
			12);
	private final static int HEIGHT = Integer.MAX_VALUE - 1000000;
	// Set right/left margin
	private int lineHeight;
	private int fontLineHeight;
	private int currentRowWidth;
	private FontMetrics fontMetrics;

	/**
	 * Convenience constructor for Text Components
	 */
	public LineNumber(JComponent component) {
		if (component == null) {
			setFont(DEFAULT_FONT);
		} else {
			setFont(component.getFont());
		}
		setPreferredSize(99);
	}

	public void setPreferredSize(int row) {
		int width = fontMetrics.stringWidth(String.valueOf(row));
		if (currentRowWidth < width) {
			currentRowWidth = width;
			setPreferredSize(new Dimension(width, HEIGHT));
		}
	}

	public void setFont(Font font) {
		super.setFont(font);
		fontMetrics = getFontMetrics(getFont());
		fontLineHeight = fontMetrics.getHeight();
	}

	public int getLineHeight() {
		if (lineHeight == 0)
			return fontLineHeight;
		else
			return lineHeight;
	}

	public void setLineHeight(int lineHeight) {
		if (lineHeight > 0)
			this.lineHeight = lineHeight;
	}

	public int getStartOffset() {
		return 2;
	}

	public void paintComponent(Graphics g) {
		int lineHeight = getLineHeight();
		int startOffset = getStartOffset();
		Rectangle drawHere = g.getClipBounds();
		// System.out.println( drawHere );
		// Paint the background
		g.setColor(getBackground());
		g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);
		// Determine the number of lines to draw in the foreground.
		g.setColor(getForeground());
		int startLineNumber = (drawHere.y / lineHeight) + 1;
		int endLineNumber = startLineNumber + (drawHere.height / lineHeight);
		int start = (drawHere.y / lineHeight) * lineHeight + lineHeight
				- startOffset;
		// System.out.println( startLineNumber + " : " + endLineNumber + " : " +
		// start );
		for (int i = startLineNumber; i <= endLineNumber; i++) {
			String lineNumber = String.valueOf(i);
			int width = fontMetrics.stringWidth(lineNumber);
			g.drawString(lineNumber, currentRowWidth - width, start);
			start += lineHeight;
		}
		setPreferredSize(endLineNumber);
	}
}

class PositionUndoManager extends UndoManager{
	int[] post;
	int currIdx;
	JTextPane jtaScript;
	
	PositionUndoManager(JTextPane jta){
		super();
		jtaScript = jta;
	}
	
	public synchronized void setLimit(int l) {
		post = new int[l];
		super.setLimit(l);
	}
	
	public int getPostion(){
		return post[currIdx];
	}
	
	public synchronized boolean addEdit(UndoableEdit anEdit) {
		if(skipNext == false){
			recordPosition(false);
		}
		return super.addEdit(anEdit);
	}

	public void recordPosition(boolean skipNext) {
		try{
			post[currIdx++] = jtaScript.getCaretPosition() + (skipNext?0:-1);
			if(currIdx == post.length){
				currIdx = 0;
			}
			this.skipNext = skipNext;
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	boolean skipNext = false;
	
	public synchronized void undo() throws CannotUndoException {
		currIdx--;
		if(currIdx < 0){
			currIdx = post.length - 1;
		}
		super.undo();
	}
	
	public synchronized void redo() throws CannotUndoException {
		currIdx++;
		if(currIdx == post.length){
			currIdx = 0;
		}
		super.redo();
	}
}
