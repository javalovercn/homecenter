package hc.server.ui.design.hpj;

import hc.App;
import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringBufferCacher;
import hc.core.util.ThreadPriorityManager;
import hc.res.ImageSrc;
import hc.server.CallContext;
import hc.server.HCActionListener;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.Robot;
import hc.server.ui.CtrlResponse;
import hc.server.ui.HCByteArrayOutputStream;
import hc.server.ui.HTMLMlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SimuMobile;
import hc.server.ui.design.Designer;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.SearchDialog;
import hc.server.ui.design.SearchResult;
import hc.server.ui.design.code.CSSIdx;
import hc.server.ui.design.code.CodeHelper;
import hc.server.ui.design.code.CodeItem;
import hc.server.ui.design.code.CodeWindow;
import hc.server.ui.design.code.TabHelper;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.engine.RubyExector;
import hc.server.util.ContextSecurityManager;
import hc.server.util.DownlistButton;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.IDArrayGroup;
import hc.server.util.ListAction;
import hc.util.ClassUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.text.View;
import javax.swing.tree.MutableTreeNode;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public abstract class ScriptEditPanel extends NodeEditPanel {
	
	public static final String FORMAT_BUTTON_TEXT = "Format";
	
	public static final JButton buildCommentUncommentButton(final Runnable actionRun){
		final JButton commentBtn = new JButton("Comment");//Uncomment
		commentBtn.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + /)<BR><BR>comment/uncomment the selected rows.</html>");
		commentBtn.setIcon(Designer.loadImg("comment_16.png"));
		
		final Action commentAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				SwingUtilities.invokeLater(actionRun);
			}
		};
		ResourceUtil.buildAcceleratorKeyOnAction(commentAction, KeyEvent.VK_SLASH);
		commentBtn.addActionListener(commentAction);
		commentBtn.getActionMap().put("commentCSSAction", commentAction);
		commentBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
		        (KeyStroke) commentAction.getValue(Action.ACCELERATOR_KEY), "commentCSSAction");
		return commentBtn;
	}

	static Highlighter.HighlightPainter ERROR_CODE_LINE_LIGHTER = new DefaultHighlighter.
			DefaultHighlightPainter(Color.RED);
	
	public static final SimpleAttributeSet STR_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#4EA539"), false);
	public static final SimpleAttributeSet REM_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#3AC2EB"), false);
	private static final SimpleAttributeSet MAP_LIGHTER = ResourceUtil.buildAttrSet(Color.BLACK, true);
	static final SimpleAttributeSet KEYWORDS_LIGHTER = ResourceUtil.buildAttrSet(ResourceUtil.toDarker(Color.BLUE, 0.8F), false);
	private static final SimpleAttributeSet NUM_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#887BE0"), true);
	public static final SimpleAttributeSet DEFAULT_LIGHTER = ResourceUtil.buildAttrSet(Color.BLACK, false);
	private static final SimpleAttributeSet VAR_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#f19e37"), false);
	private static final SimpleAttributeSet REGEXP_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#ffa07a"), false);
	
	private static final SimpleAttributeSet UNDERLINE_LIGHTER = buildUnderline(true);
	private static final SimpleAttributeSet UNDERLINE_REMOVE_LIGHTER = buildUnderline(false);
	
	public static final UnderlineHighlightPainter SYNTAX_ERROR_PAINTER = new UnderlineHighlightPainter(Color.red);
	
	public final static Object scriptEventLock = new Object();
	
	static final Pattern keywords_pattern = Pattern.compile("\\b(BEGIN|END|__ENCODING__|__END__|__FILE__|__LINE__|alias|" +
			"and|attr_accessor|attr_reader|attr_writer|begin|break|case|class |def|defined?|do|else|elsif|end|" +
			"ensure|extend|false|for|if|in|import|include|module|next|nil|not|or|private|protected|public|raise|redo|require|rescue|retry|return|" +
			"self|super|then|true|undef|unless|until|when|while|yield)\\b", Pattern.MULTILINE);//class+space解决ctx.class.get()
//	private static final String[] Indentation = {"begin", "case ", "class ", "def ", "else", 
//		"elsif ", "for ", "if ", "module ", "when ", "while ", "rescue "};
	private static final String[] WithEndIndentation = {"begin", "case", "class ", "def ", "for ", "if ", "module ", "while ", "until ", "unless "};//将when移出
	private static final int WithEndIndentationTrimEqual = 1;//begin, case, 必须 equals
	private static final String DO_WORD = "do";
	private static final String[] doIndentation = {" " + DO_WORD, " " + DO_WORD + "\n"};
	
	//addActionListener{|exception, index| or do |exception|。|exception|段可能有，可能没有。如果测试，参见TestEndIndentKuoHao
	private static final Pattern WithEndIndentationKuoHao = Pattern.compile("(\\{|\\s+" + DO_WORD + ")\\s*?(\\|\\s*?\\w+?\\s*?(,\\s*?\\w+\\s*?)*\\|)?\\s*?(?<!\\})\\s*?(#.*)?$");
	
	private static final char[] caseChar = {'c', 'a', 's', 'e'};//注意：不能带空格
	private static final char[] whenChar = {'w', 'h', 'e', 'n', ' '};
	private static final char[] ifChar = {'i', 'f', ' '};
	private static final char[] elsifChar = {'e', 'l', 's', 'i', 'f', ' '};
	private static final char[] elseChar = {'e', 'l', 's', 'e'};
	private static final char[] endChar = {'e', 'n', 'd'};
	private static final char[][] backIndent = {
		elseChar,
		elsifChar,
		whenChar,
		{'r', 'e', 's', 'c', 'u', 'e'},//可以没有异常类型段，所以去掉空格
		{'e', 'n', 's', 'u', 'r', 'e'}
	};
	private static final String[] backIndentStr = {"end", "else", "when ", "elsif ", "rescue", "ensure", "}"};//带end，是供全局format之用
	private static final String[] nextIndentStr = {"else", "elsif ", "when ", "rescue", "ensure", "{"};//带end，是供全局format之用
	
	private static final char STRING_PRE_CHAR = '"';
	
	//不提示弹出如"http://aa"的资源
	private static final char P_CHAR = 'p';
	private static final char[] httpPrefix = {STRING_PRE_CHAR, 'h', 't', 't', P_CHAR};
	private static final int httpPreLen = httpPrefix.length;
	private static final int httpLastIdx = httpPreLen - 1;
	
	private static final boolean isHttpResource(final char[] lineChars, final int currIdx){
		for (int i = currIdx; i >= 0; i--) {
			final char oneChar = lineChars[i];
			if(oneChar == STRING_PRE_CHAR){
				return false;
			}
			
			if(oneChar == P_CHAR){
				if(i - httpLastIdx >= 0){
					boolean isMatch = true;
					for (int j = 1; j < httpPreLen; j++) {
						if(lineChars[i - j] != httpPrefix[httpLastIdx - j]){
							isMatch = false;
							break;
						}
					}
					if(isMatch){
						return true;
					}
				}
			}
		}
		
		return false;
	}
	//不考虑负数，因为a-12不好处理
	private static final Pattern num_pattern = Pattern.compile("\\b(((0|0b|0o|0d)?((\\d+(\\.)?\\d*|\\d*(\\.)?\\d+)|([0-9_]{4,}))(([eE]([-+])?)?\\d+)?)|(0x[0-9abcdefABCDEF]+))\\b", Pattern.MULTILINE);
	private static final Pattern hc_map_pattern = Pattern.compile("\\$_hcmap\\b");
	private static final Pattern rem_str_regexp_pattern = Pattern.compile("(#.*(?=\n)?)|(\".*?(?<!\\\\)\")|('.*?(?<!\\\\)')|(/.*?(?<!\\\\)/)");
	private static final Pattern var_pattern = Pattern.compile("@{1,2}\\w+|\\$\\w+");//支持@@a的class instance和@a
	
	final ConsoleTextPane consoleTextPane = new ConsoleTextPane();
	final ConsoleWriter consoleWriter = new ConsoleWriter(consoleTextPane);
	
	private boolean isShowJRubyTestConsole = false;
	
	public final void showConsole(final boolean isShowConsole){
		if(isShowConsole == isShowJRubyTestConsole){
			return;
		}
		isShowJRubyTestConsole = isShowConsole;
		
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				ResourceUtil.removeFromParent(consolePanel);
				ResourceUtil.removeFromParent(scrollpane);

				if(isShowJRubyTestConsole){
					final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollpane, consolePanel);
					final int divLocation = PropertiesManager.getIntValue(PropertiesManager.p_JRubyTestConsoleDividerLocation);
					if(divLocation == 0){
						splitPane.setDividerLocation(0.8);
					}else{
						splitPane.setDividerLocation(divLocation);
					}
					splitPane.addPropertyChangeListener(new java.beans.PropertyChangeListener() {  
			            @Override
						public void propertyChange(final java.beans.PropertyChangeEvent evt) {  
			                if (evt.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {  
			                    PropertiesManager.setValue(PropertiesManager.p_JRubyTestConsoleDividerLocation, 	String.valueOf(splitPane.getDividerLocation()));
			                    PropertiesManager.saveFile();
			                }  
			            }  
			        });
					editorPane.removeAll();
					editorPane.add(splitPane, BorderLayout.CENTER);
				}else{
					editorPane.removeAll();
					addEditorOnly();
				}
				
				ClassUtil.revalidate(editorPane);
				
				PropertiesManager.setValue(PropertiesManager.p_isShowJRubyTestConsole, isShowJRubyTestConsole);
				PropertiesManager.saveFile();
			}
		}, token);
	}

	private final void addEditorOnly() {
		editorPane.add(scrollpane, BorderLayout.CENTER);
	}
	
	final JPanel editorPane = new JPanel(new BorderLayout());
	private final JScrollPane scrollpane;//归属于editorPane
	final ConsolePane consolePanel;
	public boolean isModifySourceForRebuildAST = false;
	boolean isErrorOnBuildScript;
	String currentEditVar;
	boolean isKeyTypedForChangeVar;
	final JLabel errRunInfo = new JLabel("");
	final JButton testBtn = new JButton("Run Script");
	final JButton formatBtn = new JButton(FORMAT_BUTTON_TEXT);
	final Runnable commentUncommentAction = new Runnable() {
		@Override
		public void run() {
			final int startIdx = jtaScript.getSelectionStart();
			final int endIdx = jtaScript.getSelectionEnd();
			
			if(startIdx == 0 && endIdx == 0){
				return;
			}
			
			try{
				final int beginLineNo = getLineOfOffset(jtaDocment, Math.min(startIdx, endIdx));
				final int endLineNo = getLineOfOffset(jtaDocment, Math.max(startIdx, endIdx));
				
				final char[] firstLineChar = getLineText(jtaDocment, beginLineNo).toCharArray();
				boolean isComment = false;
				int tabNum = 0;
				for (int i = 0; i < firstLineChar.length; i++) {
					final char c = firstLineChar[i];
					if(c == '\t'){
						tabNum++;
						continue;
					}else if(c == '#'){
						isComment = true;
					}else{
						break;
					}
				}
				
				final int startOff = getLineStartOffset(jtaDocment, beginLineNo);
				final int endOff = getLineEndOffset(jtaDocment, endLineNo) - 1;//去掉最后一个换行
				
				final char[] blockArea = jtaDocment.getText(startOff, endOff - startOff).toCharArray();
				final int size = blockArea.length;
				final Vector<Integer> newLineIdx = new Vector<Integer>(32);
				for (int j = size - 1; j >= 0; j--) {
					if(blockArea[j] == '\n'){
						newLineIdx.add(j);
					}
				}
				
				final int lineNum = newLineIdx.size();
				int lastEndIdx = size;
				for (int k = 0; k < lineNum; k++) {
					final int lastLineIdx = newLineIdx.get(k);
					if(isComment){
						uncomment(blockArea, lastLineIdx + 1, lastEndIdx, startOff);
					}else{
						comment(blockArea, lastLineIdx + 1, tabNum, lastEndIdx, startOff);
					}
					lastEndIdx = lastLineIdx;
				}
				
				//首行特殊处理
				if(isComment){
					uncomment(blockArea, 0, lastEndIdx, startOff);
				}else{
					comment(blockArea, 0, tabNum, lastEndIdx, startOff);
				}
				
				final int newStartOff = getLineStartOffset(jtaDocment, beginLineNo);
				final int newEndOff = getLineEndOffset(jtaDocment, endLineNo);
				
				initBlock(jtaDocment.getText(newStartOff, newEndOff - newStartOff), newStartOff, false, false);
				jtaScript.requestFocus();
				doAfterModifyBlock(false);
			}catch (final Exception e) {
				e.printStackTrace();
			}
		}
		
		final void comment(final char[] block, final int blockStartIdx, int tabNum, final int lastEndIdx, final int pStartIdx) throws BadLocationException {
			final StringBuffer sb = StringBufferCacher.getFree();
			for (int i = blockStartIdx; i < blockStartIdx + tabNum && i < lastEndIdx; i++) {
				if(block[i] != '\t'){
					sb.append('\t');
				}
			}
			if(sb.length() == 0){
				for (int i = blockStartIdx + tabNum; i < lastEndIdx; i++) {
					if(block[i] == '\t'){
						tabNum++;
					}else{
						break;
					}
				}
			}
			sb.append('#');
			jtaDocment.insertString(pStartIdx + blockStartIdx + tabNum, sb.toString(), null);
			StringBufferCacher.cycle(sb);
		}
		
		final void uncomment(final char[] block, final int blockStartIdx, final int lastEndIdx, final int pStartIdx) throws BadLocationException {
			for (int i = blockStartIdx; i < lastEndIdx; i++) {
				final char c = block[i];
				if(c == '#'){
					jtaDocment.remove(pStartIdx + i, 1);
					return;
				}else if(c != '\t'){
					return;
				}
			}
		}
	};
	
	private final boolean isSelectLineEnd(){
		final int startOff = jtaScript.getSelectionStart();
		try{
			final int lineNo = getLineOfOffset(jtaDocment, startOff);
			final boolean isLineEnd = getLineEndOffset(jtaDocment, lineNo) == startOff + 1;
			return isLineEnd;
		}catch (final Exception e) {
		}
		return false;
	}
	
	final JButton commentBtn = buildCommentUncommentButton(commentUncommentAction);
	final DownlistButton scriptBtn = new DownlistButton("To String"){
		final char TAB = '\t';
		
		@Override
		public void listActionPerformed(final ListAction act) {
			final SurroundListAction action = (SurroundListAction)act;
			
			final String selectedText = jtaScript.getSelectedText();
			if(selectedText == null){
				showSelectCodesFirst();
				return;
			}
			
			final boolean isLineEnd = isSelectLineEnd();
			
			final StringBuilder sb = StringBuilderCacher.getFree();
			
			if(isLineEnd){
				sb.append('\n');//不能在行尾且有内容，进行追加，要先加一个换行
			}
			
			final char[] selectedChars = selectedText.toCharArray();
			int firstTabNum = 0;
			for (int j = isLineEnd?1:0; j < selectedChars.length; j++) {
				if(selectedChars[j] != TAB){
					break;
				}else{
					sb.append(TAB);
					firstTabNum++;
				}
			}

			final String name = action.getName();
			final boolean isBEGIN_END = SurroundListAction.BLOCK_BEGIN.equals(name) || SurroundListAction.BLOCK_END.equals(name);
			sb.append(name);
			if(action.extendInfo != null){
				sb.append(action.extendInfo);
			}
			if(isBEGIN_END){
				sb.append(" {");
			}
			sb.append('\n');
			
			sb.append(TAB);
			
			boolean isEndWithReturn = false;
			for (int i = isLineEnd?1:0; i < selectedChars.length; i++) {
				final char nextChar = selectedChars[i];
				sb.append(nextChar);
				if(nextChar == '\n'){
					sb.append(TAB);
					isEndWithReturn = true;
				}else{
					isEndWithReturn = false;
				}
			}
			if(isEndWithReturn){
				sb.deleteCharAt(sb.length() - 1);//删除最后一个
			}else{
				sb.append('\n');
			}
			for (int k = 0; k < firstTabNum; k++) {
				sb.append(TAB);
			}
			if(isBEGIN_END){
				sb.append('}');
			}else{
				sb.append("end");
			}
			if(isEndWithReturn){
				sb.append('\n');
			}
			
			final String scripts = sb.toString();
			StringBuilderCacher.cycle(sb);
			
			jtaScript.replaceSelection(scripts);
			
			doAfterModifyBlock(true);
		}
	};
	int modifierKeysPressed = 0;
	final int abstractCtrlKeyMash = ResourceUtil.getAbstractCtrlKeyMask();
	
	final HCByteArrayOutputStream iconBsArrayos = ServerUIUtil.buildForMaxIcon();
	final JTextField nameField = new JTextField(){
		@Override
		public void paste(){
			super.paste();
			notifyModifyName();
		}
	};
	final JRubyErrorHCTimer errorTimer = new JRubyErrorHCTimer("JRubyErrorHCTimer", 1000, false);
	final boolean isJ2SEServer = ResourceUtil.isJ2SELimitFunction();

	abstract Map<String, String> buildMapScriptParameter();
	
	final public void rebuildASTNode() {
		isKeyTypedForChangeVar = false;
		designer.codeHelper.updateScriptASTNode(this, jtaScript.getText(), true);
	}
	
	final CallContext callCtxNeverCycle = CallContext.getFree();
	final Runnable rebuildTestEngineRunnable = new Runnable() {
		@Override
		public void run() {
			final HCJRubyEngine hcje = SimuMobile.rebuildJRubyEngine();//import逻辑已加载，必须重建实例
			RubyExector.initActive(hcje);
		}
	};
	
	final void doTest(final boolean isRun, final boolean isCompileOnly) {
		final Map<String, String> map = buildMapScriptParameter();
		HCJRubyEngine runTestEngine = SimuMobile.getRunTestEngine();
		if(runTestEngine != null && callCtxNeverCycle.isError){
			//清空旧错误
			jtaScript.getHighlighter().removeAllHighlights();
		}
		
		if(designer.tryBuildTestJRuby() == false){
			return;
		}
		
		SimuMobile.changeCTP(consoleTextPane);
		runTestEngine = SimuMobile.getRunTestEngine();//有可能上行重建
		
		final ProjectContext context = ContextSecurityManager.getConfig(
				(ThreadGroup)HCLimitSecurityManager.getTempLimitRecycleRes().threadPool.getThreadGroup()).getProjectContext();
//		if(isRun){
//			ServerUIAPIAgent.setTestSimuClientSession(context, designer.testSimuClientSession);
//		}
		
		final String script = jtaScript.getText();
		final HPNode node = (HPNode)currNode.getUserObject();

		final String targetURL;
		if(node instanceof HPMenuItem){
			targetURL = ((HPMenuItem)node).url;
		}else{
			targetURL = CallContext.UN_KNOWN_TARGET;
		}
		
		consolePanel.ctp.clearText();
		
		try{
			{
				callCtxNeverCycle.reset();
				callCtxNeverCycle.targetURL = targetURL;
				RubyExector.parse(callCtxNeverCycle, script, null, runTestEngine, false);
			}
			if(callCtxNeverCycle.isError == false && (isRun || isCompileOnly == false)){
				callCtxNeverCycle.reset();
				callCtxNeverCycle.targetURL = targetURL;
				RubyExector.runAndWaitInProjectOrSessionPool(J2SESession.NULL_J2SESESSION_FOR_PROJECT, callCtxNeverCycle, script, null, map, runTestEngine, context);
			}
		}catch (final Throwable e) {
		}finally{
			ContextManager.getThreadPool().run(rebuildTestEngineRunnable);
		}
		if(callCtxNeverCycle.isError){
//			final Object runnable = Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation");
//			 if (runnable != null && runnable instanceof Runnable){
//				 ((Runnable)runnable).run();
//			 }
			 
			Toolkit.getDefaultToolkit().beep();
			
			errRunInfo.setText(callCtxNeverCycle.getMessage());
			errRunInfo.setBackground(testBtn.getBackground());
			isErrorOnBuildScript = true;
			
			final int line = callCtxNeverCycle.getLineNumber();
			final char[] chars = script.toCharArray();
			
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
				} catch (final BadLocationException e1) {
				}
				jtaScript.setCaretPosition(startIdx);
			}
		}else{
			isErrorOnBuildScript = false;
			if(isRun){
				errRunInfo.setBackground(Color.GREEN);
				errRunInfo.setText(" ");
				
				errorTimer.resetTimerCount();
				errorTimer.setEnable(true);
			}else{
				errorTimer.doBiz();
			}
		}
	}

	private final void setEditorDefaultCurosr() {
		if(isHandCursor){
			isHandCursor = false;
			jtaScript.setCursor(defaultCursor);
		}
	}
	
	private final ThreadGroup token;
	
	public ScriptEditPanel() {
		token = App.getThreadPoolToken();
		consoleTextPane.setScriptEditPanel(this);
		consolePanel = new ConsolePane(consoleTextPane);
		
		jtaDocment = (AbstractDocument)jtaScript.getDocument();
		jtaStyledDocment = (StyledDocument)jtaDocment;
		
//		jtaDocment.putProperty(TextAttribute.RUN_DIRECTION,TextAttribute.RUN_DIRECTION_LTR);
		
		normalBackground = buildBackground(jtaScript.getBackground());
		
		errRunInfo.setForeground(Color.RED);
		errRunInfo.setOpaque(true);

		errorTimer.setErrorLable(errRunInfo, testBtn);
		
		final String runTip = "evaluate the script and execute it." +
				"<BR>To apply modification and access from mobile, please click [<STRONG>" + Designer.ACTIVE + "</STRONG>]." +
				"<BR><BR>" +
				"<STRONG>Note : </STRONG><BR>" +
				"1. there is a JRuby engine instance for each project, and one for designer,<BR>" +
				"2. even if a green bar is displayed in bottom, defects may be in the scripts that are not covered,<BR>" +
				"3. although script runs in simulator, but it is a real run.";
		{
			final Action testAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run(){
//							开启下行，会导致format获得焦点
//							App.invokeAndWaitUI(new Runnable() {
//								@Override
//								public void run() {
//									testBtn.setEnabled(false);
//								}
//							});
							
							IDArrayGroup.showMsg(IDArrayGroup.MSG_JRUBY_RUN_NO_COVER, App.SYS_WARN_ICON, ResourceUtil.getInfoI18N(), 
									"<html>" + runTip + "</html>");
							
							try{
								doTest(true, false);
							}catch (final Exception e) {
								ExceptionReporter.printStackTrace(e);
							}
							
//							App.invokeLaterUI(new Runnable() {
//								@Override
//								public void run() {
//									testBtn.setEnabled(true);
//								}
//							});
						}
					}, threadPoolToken);
				}
			};
			ResourceUtil.buildAcceleratorKeyOnAction(testAction, KeyEvent.VK_T);//同时支持Windows下的Ctrl+T和Mac下的Command+T
			testBtn.addActionListener(testAction);
			testBtn.getActionMap().put("testAction", testAction);
			testBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) testAction.getValue(Action.ACCELERATOR_KEY), "testAction");
			
			final Action formatAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							format(jtaDocment);
//							initColor(false);
						}
					});
				}
			};
			ResourceUtil.buildAcceleratorKeyOnAction(formatAction, KeyEvent.VK_F);//执行太快，感觉没反应
			formatBtn.addActionListener(formatAction);
			formatBtn.getActionMap().put("formatAction", formatAction);
			formatBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) formatAction.getValue(Action.ACCELERATOR_KEY), "formatAction");
		}
		testBtn.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + T)" +
				"<BR>" + runTip + 
				"<BR><BR>JRuby compat version : <STRONG>" + HCJRubyEngine.JRUBY_PARSE_VERSION + "</STRONG>" +
				"</html>");
		testBtn.setIcon(Designer.loadImg("test_16.png"));
		
		formatBtn.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + F) <BR>format the JRuby script.</html>");
		formatBtn.setIcon(Designer.loadImg(ImageSrc.FORMAT_16_PNG));
		
		{
//			surround.add(new SurroundListAction("do"));
//			surround.add(new SurroundListAction("for"));
//			for ‹ name ›+ in expression ‹ do › body
//			end
			scriptBtn.addListAction(new SurroundListAction("if", " true"));
//			if boolean-expression ‹ then › body
//			‹ elsif boolean-expression ‹ then › body ›*
//			‹ else body ›
//			end
			scriptBtn.addListAction(new SurroundListAction("loop", " do"));
			scriptBtn.addListAction(new SurroundListAction("while", " true"));
//			while boolean-expression ‹ do › body
//			end
//			This executes body zero or more times as long as boolean-expression is true.
			scriptBtn.addListAction(new SurroundListAction("until", " false"));
//			until boolean-expression ‹ do › body
//			end
//			This executes body zero or more times as long as boolean-expression is false.
			scriptBtn.addListAction(new SurroundListAction("unless", " false"));
//			unless boolean-expression ‹ then › body
//			‹ else body ›
//			end
			scriptBtn.addListAction(new SurroundListAction(SurroundListAction.BLOCK_BEGIN));
			scriptBtn.addListAction(new SurroundListAction(SurroundListAction.BLOCK_END));
		}
		scriptBtn.setDefaultAction(new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final String selectedText = jtaScript.getSelectedText();
				if(selectedText == null){
					showSelectCodesFirst();
					return;
				}
				
				final StringBuilder sb = StringBuilderCacher.getFree();
				
				final boolean isLineEnd = isSelectLineEnd();
				if(isLineEnd){
					sb.append('\n');
					try{
						final int lineNo = getLineOfOffset(jtaDocment, jtaScript.getSelectionStart() + 1);//下一行，不能使用当前行的缩进
						final int startOff = getLineStartOffset(jtaDocment, lineNo);
						final int endOff = getLineEndOffset(jtaDocment, lineNo);
						final char[] chars = jtaDocment.getText(startOff, endOff - startOff).toCharArray();
						final int size = chars.length;
						for (int i = 0; i < size; i++) {
							if(chars[i] == '\t'){
								sb.append('\t');
							}else{
								break;
							}
						}
					}catch (final Exception ex) {
					}
				}
				
				final char[] selectedChars;
				if(isLineEnd){
					//移去第一个换行 
					final char[] tmpChars = selectedText.toCharArray();
					selectedChars = new char[tmpChars.length - 1];
					for (int i = 1; i < tmpChars.length; i++) {
						selectedChars[i - 1] = tmpChars[i];
					}
				}else{
					selectedChars = selectedText.toCharArray();
				}
				
//				sb.append("#encoding:utf-8\n");
				sb.append("sb = java.lang.StringBuilder::new(" + (selectedChars.length * 2) + ")\n");
				int startIdx = 0;
				for (int i = 0; i < selectedChars.length; i++) {
					final char nextChar = selectedChars[i];
					if(nextChar == '\n'){
						appendLine(sb, selectedChars, startIdx, i - startIdx);
						startIdx = i + 1;
					}
				}
				appendLine(sb, selectedChars, startIdx, selectedChars.length - startIdx);
				sb.deleteCharAt(sb.length() - 1);//删除最后一个回车
				
//				sb.append("Java::hc.server.ui.ProjectContext::getProjectContext().eval(sb.toString())\n");
				final String scripts = sb.toString();
				StringBuilderCacher.cycle(sb);
				
				jtaScript.replaceSelection(scripts);
				
				doAfterModifyBlock(true);
//				ResourceUtil.sendToClipboard(scripts);
//				JOptionPane.showMessageDialog(designer, "successful generate string sources to clipboard!", ResourceUtil.getInfoI18N(), JOptionPane.INFORMATION_MESSAGE, App.getSysIcon(App.SYS_INFO_ICON));
			}
			
			private final void appendLine(final StringBuilder sb, final char[] chars, final int startIdx, final int len){
				sb.append("sb.append(\"");
				final int endIdx = startIdx + len;
				for (int i = startIdx; i < endIdx; i++) {
					final char currChar = chars[i];
					if(currChar == '#' && (i + 1 < endIdx) && chars[i + 1] == '{'){
						final int startKIdx = i;
						int k = i + 2;
						for (; k < endIdx; k++) {
							final char c = chars[k];
							if(c == '"'){
								while((k + 1) < endIdx && chars[k + 1] == '"' && chars[k] != '\\'){
									k++;
								}
								continue;
							}else if(c == '\''){
								while((k + 1) < endIdx && chars[k + 1] == '\'' && chars[k] != '\\'){
									k++;
								}
								continue;
							}else if(c == '}'){
								break;
							}
						}
						i = k;
						sb.append(chars, startKIdx, k + 1 - startKIdx);
						continue;
					}
					
					if(currChar == '\\'){
						sb.append('\\');
						sb.append('\\');
					}else if(currChar == '\"'){
						sb.append("\\\"");
					}else{
						sb.append(currChar);
					}
				}
				sb.append("\\n\")\n");
			}
		});
		scriptBtn.setToolTipText("<html>" +
				"translate scripts to a string for ProjectContext.<STRONG>eval</STRONG>." +
				"<BR><BR>Do as following steps :" +
				"<BR>1. select scripts in <STRONG>" + JRubyNodeEditPanel.JRUBY_SCRIPT + "</STRONG> panel," +
				"<BR>2. click this button,</html>");
		scriptBtn.setIcon(Designer.loadImg("script_16.png"));
		
		//		jtaScript.setTabSize(2);
		
		//以下代码设置Tab跳过指定的空格
		final FontMetrics fm = jtaScript.getFontMetrics(jtaScript.getFont());
		jtaScript.setForeground(jtaScript.getBackground());
		final int cw = fm.stringWidth("    ");
		final float f = cw;
		final TabStop[] tabs = new TabStop[10];
		for(int i = 0; i < tabs.length; i++){
			tabs[i] = new TabStop(f * (i + 1), TabStop.ALIGN_LEFT, TabStop.LEAD_UNDERLINE);
		}
		final TabSet tabset = new TabSet(tabs);
		final SimpleAttributeSet attributes = new SimpleAttributeSet();
		StyleConstants.setTabSet(attributes, tabset);
//		final StyleContext sc = StyleContext.getDefaultStyleContext();
//		final AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabset);
//		jtaScript.setParagraphAttributes(aset, false);	
		jtaStyledDocment.setParagraphAttributes(0, 0, attributes, false);
		fontHeight = jtaScript.getFontMetrics(jtaScript.getFont()).getHeight();
		autoCodeTip = new MouseMovingTipTimer(this, jtaScript, jtaDocment, fontHeight);
		
		jtaScript.addCaretListener(new CaretListener() {
			int lastLineNo = 0;
			@Override
			public void caretUpdate(final CaretEvent e) {
				clearSelectionBG();
				try{
					final int scriptIdx = e.getDot();
					final int currLineNo = getLineOfOffset(jtaDocment, scriptIdx);
					if(isModifySourceForRebuildAST){
//						System.out.println("caret newLineNo : " + currLineNo + ", oldLineNo : " + lastLineNo + ", new Idx :  " + e.getDot());
						if(currLineNo != lastLineNo){
							TabHelper.clearAll();
							designer.codeHelper.updateScriptASTNode(ScriptEditPanel.this, jtaScript.getText(), isModifySourceForRebuildAST);
							isModifySourceForRebuildAST = false;
//							if(isErrorOnBuildScript || isSucc == false){
//								SwingUtilities.invokeLater(doTestRunnable);
//							}
						}
					}
					lastLineNo = currLineNo;
					
					if(jtaScript.searchDialog != null){
						final int startIdx = Math.min(e.getDot(), e.getMark());
						final int endIdx = Math.max(e.getDot(), e.getMark());
//						System.out.println("========> startIdx : " + startIdx + ", endIdx : " + endIdx);
						jtaScript.searchDialog.splitHighlightWhenSelection(startIdx, endIdx, jtaScript.getHighlighter());
					}

//			        final int editLineStartIdx = getLineStartOffset(jtaDocment, currLineNo);
//			        final int lineIdx = scriptIdx - editLineStartIdx;
//			        final int editLineEndIdx = getLineEndOffset(jtaDocment, currLineNo);
//			        final char[] lineChars = jtaDocment.getText(editLineStartIdx, editLineEndIdx - editLineStartIdx).toCharArray();
//			        
//			        final String oldCurrentEditVar = currentEditVar;
//					//当前编辑的@变量
//			        currentEditVar = InstanceVariableManager.getCurrentEditingVar(lineChars, lineIdx);
//			        
//			        if(isKeyTypedForChangeVar 
//			        		&& oldCurrentEditVar != null && currentEditVar != null 
//			        		&& oldCurrentEditVar.equals(currentEditVar) == false){
//			        	isKeyTypedForChangeVar = false;//reset
//			        	
//			        	InstanceVariableManager.replaceVariable(oldCurrentEditVar, currentEditVar, jtaDocment, 
//			        			designer.codeHelper.root, scriptIdx);
//			        }
				}catch (final Exception ex) {
				}
			}
		});
		
		{
			final AbstractAction tabAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent event) {
			    	if(TabHelper.pushTabOrEnterKey()){
			    	}else{
			    		final int posi = jtaScript.getCaretPosition();
			    		try {
							jtaDocment.insertString(posi, "\t", null);
							jtaScript.setCaretPosition(posi + 1);
						} catch (final BadLocationException e) {
							ExceptionReporter.printStackTrace(e);
						}
			    	}
			    }
			};
			final KeyStroke tabKey = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
			jtaScript.getInputMap().put(tabKey, tabAction);
		}
		{
			final AbstractAction shiftTabAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent e) {
			    	TabHelper.pushShiftTabKey();
			    }
			};
			final KeyStroke shiftTabKey = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK);
			jtaScript.getInputMap().put(shiftTabKey, shiftTabAction);
		}
		{
			final AbstractAction enterAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					if(TabHelper.pushTabOrEnterKey()){
					}else{
						try{
							isModifySourceForRebuildAST = false;//disable caretUpdate event
							scriptUndoListener.setUndoModel(ScriptUndoableEditListener.UNDO_MODEL_ENTER);
							
							actionOnEnterKey(jtaDocment);
						}catch (final Exception ex) {
						}
					}
				}
			};
			final KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
			jtaScript.getInputMap().put(enterKey, enterAction);
		}
		
		jtaScript.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(final FocusEvent e) {
				designer.codeHelper.window.hide();
			}
			
			@Override
			public void focusGained(final FocusEvent e) {
				final CodeWindow window = designer.codeHelper.window;
				if(window.isWillOrAlreadyToFront){
					window.hide();
				}
			}
		});
		
		jtaScript.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				autoCodeTip.setEnable(false);
			}
			
			@Override
			public void mousePressed(final MouseEvent e) {
				autoCodeTip.setEnable(false);
			}
			
			@Override
			public void mouseExited(final MouseEvent e) {
				autoCodeTip.setEnable(false);
//				designer.codeHelper.hideAfterMouse(true);//手工输入代码提示时，会触发此事件
			}
			
			@Override
			public void mouseEntered(final MouseEvent e) {
//				if(designer.codeHelper.window.isVisible() == false){
//				autoCodeTip.resetTimerCount();
//				autoCodeTip.setEnable(true);//只要鼠标移动，就启动，不管code/doc窗口已开启或关闭
//				}
			}
			
			final CSSFocusAction action = new CSSFocusAction() {
				@Override
				public void action(final int startIdx, final int len) {
					CSSClassIndex firstMatcher = null;
					Vector<CSSClassIndex> vector = null;
					
					try {
						final String className = jtaDocment.getText(startIdx, len);
						final Vector<CodeItem> cssClassesOfProjectLevel = designer.cssClassesOfProjectLevel;
						final int size = cssClassesOfProjectLevel.size();
						for (int i = 0; i < size; i++) {
							final CodeItem item = cssClassesOfProjectLevel.elementAt(i);
							final Object element = item.userObject;
							final CSSClassIndex cssIdx;
							boolean isVector = false;
							if(element instanceof CSSClassIndex){
								cssIdx = (CSSClassIndex)element;
							}else{
								isVector = true;
								cssIdx = ((Vector<CSSClassIndex>)element).elementAt(0);
							}
							if(cssIdx.className.equals(className)){
								if(isVector){
									vector = ((Vector<CSSClassIndex>)element);
								}else{
									firstMatcher = cssIdx;
								}
								break;
							}
						}
						
						if(vector == null){
							if(firstMatcher == null){
								App.showMessageDialog(designer, "<html>Not found defined CSS : <strong>" + className + "</strong></html>", 
										ResourceUtil.getErrorI18N(), JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
								return;
							}else{
								jumpCSSDefine(firstMatcher);
							}
						}else{
							//选择一个进行jump
							final int listSize = vector.size();
							final String[] listDesc = new String[listSize];
							for (int i = 0; i < listSize; i++) {
								listDesc[i] = vector.elementAt(i).fullName;
							}
							final JLabel lable = new JLabel("choose one CSS to open :");
							final JList list = new JList(listDesc);//java 6不支持<String>
							list.setSelectedIndex(0);
							list.setCellRenderer(new CSSListCellRenderer());
							list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
							final JScrollPane listScrollPane = new JScrollPane(list);
							final JPanel panel = new JPanel(new BorderLayout());
							panel.add(lable, BorderLayout.NORTH);
							panel.add(listScrollPane, BorderLayout.CENTER);
							
							final Vector<CSSClassIndex> vectorPara = vector;
							final ActionListener listener = new ActionListener() {
								@Override
								public void actionPerformed(final ActionEvent e) {
									final int selected = list.getSelectedIndex();
									if(selected >= 0){
										jumpCSSDefine(vectorPara.elementAt(selected));
									}
								}
							};
							ContextManager.getThreadPool().run(new Runnable() {
								@Override
								public void run() {
									App.showCenterPanelMain(panel, 300, 210, ResourceUtil.getInfoI18N(), true, null, null, listener, null, designer, true, false, null, false, false);
								}
							});
						}
					} catch (final Throwable e) {
						e.printStackTrace();
					}
				}
			};
			
			@Override
			public void mouseClicked(final MouseEvent e) {
				autoCodeTip.setEnable(false);
				lastMouseClickX = e.getX();
				lastMouseClickY = e.getY();
				designer.codeHelper.window.hide();
				if(focusCSSClass(e, action) != -1){
					clearCSSClassFocus();
					return;
				}
			}

			private final void jumpCSSDefine(final CSSClassIndex firstMatcherPara) {
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						designer.jumpCSSDefine(firstMatcherPara.startIdx, firstMatcherPara.fullName.length());
					}
				});
			}
		});
		
		jtaScript.addMouseMotionListener(new MouseMotionListener() {
			final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
			
			final CSSFocusAction action = new CSSFocusAction() {
				@Override
				public void action(final int startIdx, final int len) {
					lastFocusUnderlineCSSStartIdx = startIdx;
					lastFocusUnderlineCSSLen = len;
					jtaStyledDoc.setCharacterAttributes(lastFocusUnderlineCSSStartIdx, lastFocusUnderlineCSSLen, UNDERLINE_LIGHTER, false);
					jtaScript.setCursor(handCursor);
					isHandCursor = true;
				}
			};
			
			final StyledDocument jtaStyledDoc = jtaScript.getStyledDocument();
			int moveX, moveY;
			
			@Override
			public void mouseMoved(final MouseEvent e) {
				modifierKeysPressed = e.getModifiers();
				
				if(modifierKeysPressed == 0){
					setEditorDefaultCurosr();
//					if(designer.codeHelper.window.isWillOrAlreadyToFront){
//						return;
//					}
					
					if(designer.codeHelper.window.isVisible() && Math.abs(e.getX() - moveX) < 5 && Math.abs(e.getY() - moveY) < 5){//消除CodeList显示时，鼠标偶动
						moveX = e.getX();
						moveY = e.getY();
						return;
					}
					moveX = e.getX();
					moveY = e.getY();
					synchronized (ScriptEditPanel.scriptEventLock) {
						if(designer.codeHelper.window.isVisible()){
							if(Math.abs(moveX - autoCodeTip.lastShowX) >= fontHeight * 3 || Math.abs(moveY - autoCodeTip.lastShowY) >= fontHeight){
								designer.codeHelper.lastMousePreCode = "";
								final MouseExitHideDocForMouseMovTimer timer = designer.codeHelper.mouseExitHideDocForMouseMovTimer;
								if(timer.isEnable() == false){
									timer.resetTimerCount();
									timer.setEnable(true);//有可能现tip时，移动
								}
							}
						}
//					designer.codeHelper.hideAfterMouse(true);//有可能移到DocTip内
						if(Math.abs(moveX - lastMouseClickX) >= fontHeight * 2 || Math.abs(moveY - lastMouseClickY) >= fontHeight){
							lastMouseClickX = -100;
							lastMouseClickY = -100;
							autoCodeTip.resetTimerCount();
							autoCodeTip.setEnable(true);
							autoCodeTip.setLocation(moveX, moveY);
						}
					}
				}else{
					final int lastFocusCSSStartIdxBack = lastFocusUnderlineCSSStartIdx;
					final int lastFocusCSSLenBak = lastFocusUnderlineCSSLen;
					
					final int startIdx = focusCSSClass(e, action);
					if(startIdx == -1){
						setEditorDefaultCurosr();
					}
					if(lastFocusCSSStartIdxBack != startIdx && lastFocusCSSStartIdxBack != -1){
						jtaStyledDoc.setCharacterAttributes(lastFocusCSSStartIdxBack, lastFocusCSSLenBak, UNDERLINE_REMOVE_LIGHTER, false);
					}
				}
			}
			
			@Override
			public void mouseDragged(final MouseEvent e) {
				autoCodeTip.setEnable(false);
			}
		});
		
		jtaScript.addKeyListener(new KeyListener() {
			final boolean isMacOS = ResourceUtil.isMacOSX();
			long lastTypedCharMS;
			long currTypedCharMS;
			
			@Override
			public void keyTyped(final KeyEvent event) {
				lastTypedCharMS = currTypedCharMS;
				currTypedCharMS = System.currentTimeMillis();
				
				if(isEventConsumed){
					consumeEventLocal(event);//otherwise display shortcut key in JRE 6.
					return;
				}
				synchronized (scriptEventLock) {
				final CodeHelper codeHelper = designer.codeHelper;
				if(codeHelper.window.isWillOrAlreadyToFront){
					codeHelper.window.keyPressedAfterDot(event);
					consumeEventLocal(event);
					return;
				}
				final char inputChar = event.getKeyChar();
				final int modifiers = event.getModifiers();
//				final int keycode = event.getKeyCode();
//				System.out.println("keyCode : " + keycode + ", inputChar : " + inputChar + ", modifyMask : " + modifiers);
//				System.out.println("codeHelp wordcode : " + codeHelper.wordCompletionCode + ", char : " + codeHelper.wordCompletionChar + 
//						", modify : " + codeHelper.wordCompletionModifyCode + ", modifyMask : " + codeHelper.wordCompletionModifyMaskCode);
				
				//有输出字符时的触发提示代码。在Mac环境下Ctrl+.时，无输出字符，modifiers==ctrl_mask且inputChar!=0；在Mac环境下option+/时，出现字符÷且modifier==0
				if(isMacOS && (inputChar != 0 && inputChar == codeHelper.wordCompletionChar
						&& ((codeHelper.wordCompletionModifyCode == KeyEvent.VK_ALT && modifiers == 0) 
									|| (codeHelper.wordCompletionModifyMaskCode == modifiers)))){//注意：请同步到MletNodeEditPanel
					try {
						final int caretPosition = jtaScript.getCaretPosition();
						codeHelper.input(ScriptEditPanel.this, jtaScript, jtaDocment, fontHeight, true, caretPosition, true);
					} catch (final Exception e) {
//						if(L.isInWorkshop){
							ExceptionReporter.printStackTrace(e);
//						}
//						SwingUtilities.invokeLater(new Runnable() {//不能执行，因为编辑器可能存在bug，会导致触发
//							@Override
//							public void run() {
//								doTest(false, false);
//							}
//						});
					}
					consumeEventLocal(event);
					return;
				}
				
				if(inputChar == KeyEvent.VK_ENTER){
					//具体转由actionOnEnterKey()
					consumeEventLocal(event);
					return;
				}
				
				isModifySourceForRebuildAST = true;//enable key and caretUpdate newline
				
				if(inputChar == KeyEvent.VK_BACK_SPACE || inputChar == KeyEvent.VK_DELETE){
					scriptUndoListener.setUndoModel(ScriptUndoableEditListener.UNDO_MODEL_BACK_OR_DEL);
				}else{
					scriptUndoListener.setUndoModel(ScriptUndoableEditListener.UNDO_MODEL_TYPE);
				}
				
				try{
					final int position = jtaScript.getCaretPosition();
					final int line = getLineOfOffset(jtaDocment, position);
					
					jtaScript.refreshCurrLineAfterKey(line);
					
					TabHelper.notifyInputKey(inputChar == KeyEvent.VK_BACK_SPACE, event, inputChar, selectionLen);
					
					final boolean isDot = inputChar == '.';
					final boolean isPathSplit = inputChar == '/';
			        
					final int lineStartIdx = ScriptEditPanel.getLineStartOffset(jtaDocment, line);
			        final int lineIdx = position - lineStartIdx;

			        if(isDot || isPathSplit || inputChar == ':'){//自动弹出代码提示条件
				        final char[] lineChars = jtaDocment.getText(lineStartIdx, lineIdx).toCharArray();
				        final int lineCharsLen = lineChars.length;
				        
				        if(isHttpResource(lineChars, lineCharsLen - 1) || CodeHelper.searchGoGoExternalURL(lineChars)){
				        	return;//对于http型的资源，不弹出本地库资源代码提示
				        }
				        
						int partSplitIdx = 0;
				        {
							//puts " #{word} "
							char isInStr = 0;
							for (int i = lineCharsLen - 1; i >= 0; i--) {
								final char checkSplitChar = lineChars[i];
								if((checkSplitChar == '"' || checkSplitChar == '\'') && (i == 0 || ((i - 1) >= 0 && lineChars[i - 1] != '\\'))){//array[var.indexOf("Hello")]
									if(isInStr != 0){
										if(isInStr == checkSplitChar){
											isInStr = 0;
										}else{
											continue;
										}
									}else{
										isInStr = checkSplitChar;
									}
								}
								if(isInStr != 0){
									continue;
								}
								if(i > 1 && checkSplitChar == '{' && lineChars[i - 1] == '#'){
									partSplitIdx = i + 1;
									break;
								}
							}
						}
				        
				        //处于""之中
				        char isInStr = 0;
				        for (int i = partSplitIdx; i < lineCharsLen; i++) {
				        	final char checkSplitChar = lineChars[i];
							if((checkSplitChar == '\"' || checkSplitChar == '\'') && (i == 0 || i > 0 && lineChars[i - 1] != '\\')){
								if(isInStr != 0){
									if(isInStr == checkSplitChar){
										isInStr = 0;
									}else{
										continue;
									}
								}else{
									isInStr = checkSplitChar;
								}
							}
						}
				        if(isInStr != 0){
				        	if(isPathSplit){
				        		popUpAuto(codeHelper);
				        		return;
				        	}
				        	
				        	if(L.isInWorkshop){
				        		LogManager.log("input dot (.) is in yinhao, skip auto popup codetip.");
				        	}
				        	return;
				        }
				        
				        if(isPathSplit){
				        	return;
				        }
				        
				        //处于#之后
				        for (int i = partSplitIdx; i < lineCharsLen; i++) {
				        	if(lineChars[i] == '#' && ((i + 1 == lineCharsLen) || (i + 1 < lineCharsLen) && lineChars[i + 1] != '{')){//但不含"Hello #{} world"
				        		return;
				        	}
				        }

				        if(isDot){
					        //a = 100.2 + 10.情形
					        boolean isVarOrMethodCase = false;
					        for (int i = lineCharsLen - 1; i >= partSplitIdx; i--) {
					        	final char oneChar = lineChars[i];
								if(oneChar >= '0' && oneChar <= '9'){
									continue;
								}else if((oneChar >= 'a' && oneChar <= 'z') || (oneChar >='A' && oneChar <= 'Z') 
										|| oneChar == '_' || oneChar == ')' || oneChar == ']'){// )表示方法；[]数组
									isVarOrMethodCase = true;
								}
								break;
							}
					        
					        if(isVarOrMethodCase){
					        }else{
//					        	if(L.isInWorkshop){
//					        		LogManager.log("input dot (.) is not for variable, skip auto popup codetip.");
//					        	}
					        	if(isNumberFirst(lineChars) || (lastTypedCharMS != 0 && (currTypedCharMS - lastTypedCharMS) > 500)){//稍有等待，则
						        	popUpAuto(codeHelper);
					        	}
					        	return;
					        }
				        }
				        
				        if(inputChar == ':'){
				        	//abc = Java::情形
				        	if(lineCharsLen > 0 && lineChars[lineCharsLen - 1] == ':'){
				        	}else{
				        		return;
				        	}
				        }
				        
				        popUpAuto(codeHelper);
				        return;
					}//end 自动弹出代码提示条件
			        
			        isKeyTypedForChangeVar = true;
				}catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}//end try
				}
			}
			
			private final boolean isNumberFirst(final char[] lineChar){
				boolean isFirstSpace = true;
				for (int i = 0; i < lineChar.length; i++) {
					final char oneChar = lineChar[i];
					if(oneChar == '\t' || oneChar == ' '){
						if(isFirstSpace == false){
							return false;
						}
					}else if(oneChar >= '0' && oneChar <= '9'){
						isFirstSpace = false;
					}else{
						return false;
					}
				}
				return true;
			}

			private final void popUpAuto(final CodeHelper codeHelper) {
				//自动弹出代码提示
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						synchronized (scriptEventLock) {
						try{
							final int caretPosition = jtaScript.getCaretPosition();
							codeHelper.input(ScriptEditPanel.this, jtaScript, jtaDocment, fontHeight, false, caretPosition, false);
						}catch (final Exception e) {
							if(L.isInWorkshop){
								e.printStackTrace();
							}
						}
						}
					}
				});
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
				modifierKeysPressed = 0;
				clearCSSClassFocus();
			}
			
			boolean isEventConsumed;
			int selectionLen;
			
			@Override
		    public void keyPressed(final KeyEvent event) {
				modifierKeysPressed = event.getModifiers();
				
				autoCodeTip.setEnable(false);
				autoCodeTip.getCodeHelper().mouseExitHideDocForMouseMovTimer.setEnable(false);
				
				final int selectionStart = jtaScript.getSelectionStart();
				final int selectionEnd = jtaScript.getSelectionEnd();
				if(selectionEnd > selectionStart){
					selectionLen = selectionEnd - selectionStart;
				}else{
					selectionLen = 0;
				}
				
				synchronized (scriptEventLock) {
	            final int keycode = event.getKeyCode();
//				switch (keycode) {
//				case KeyEvent.VK_ALT:
//				case KeyEvent.VK_SHIFT:
//				case KeyEvent.VK_CAPS_LOCK:
//				case KeyEvent.VK_CONTROL:
//				case KeyEvent.VK_ESCAPE:
//					return;
//				}
	            final CodeHelper codeHelper = designer.codeHelper;
				final int wordCompletionModifyMaskCode = codeHelper.wordCompletionModifyMaskCode;
				//无输入字符时的触发提示代码
				isEventConsumed = false;
				if(keycode == codeHelper.wordCompletionCode && (modifierKeysPressed & wordCompletionModifyMaskCode) == wordCompletionModifyMaskCode){
					//注意：请同步到MletNodeEditPanel
					try {
						final int caretPosition = jtaScript.getCaretPosition();
						codeHelper.input(ScriptEditPanel.this, jtaScript, jtaDocment, fontHeight, true, caretPosition, true);
					} catch (final Throwable e) {
						ExceptionReporter.printStackTrace(e);
					}
					consumeEventLocal(event);
					isEventConsumed = true;
					return;
				}
				
				if(codeHelper.window.isVisible()){
					codeHelper.window.keyPressed(event);
					consumeEventLocal(event);
					isEventConsumed = true;
					return;
				}
	            
	            if (modifierKeysPressed == KeyEvent.CTRL_MASK) {
	                if (keycode == KeyEvent.VK_Z) {
	                	//ctrl + z
	                    undo();
	                }else if(keycode == KeyEvent.VK_Y) {
	                	//ctrl + y
	                    redo();
	                }
	            }else if(keycode == KeyEvent.VK_Z){
					if(isMacOS){
		                if (modifierKeysPressed == KeyEvent.META_MASK) {
		                	//cmd+z
		                    undo();
		                }else if(modifierKeysPressed == (KeyEvent.META_MASK | KeyEvent.SHIFT_MASK)) {
		                	redo();
		                }
	            	}
	            }
	            
	            if(keycode == KeyEvent.VK_ESCAPE){
					TabHelper.pushEscKey();
					consumeEventLocal(event);//清空可能占用的modifierKeysPressed
					isEventConsumed = true;
					return;
				}
	            
				if(keycode == KeyEvent.VK_BACK_SPACE || keycode == KeyEvent.VK_DELETE){
					scriptUndoListener.setUndoModel(ScriptUndoableEditListener.UNDO_MODEL_BACK_OR_DEL);
				}else{
					scriptUndoListener.setUndoModel(ScriptUndoableEditListener.UNDO_MODEL_TYPE);
				}
		    }
			}
		});

		final LineNumber lineNumber = new LineNumber(jtaScript);
//		Disable Word wrap in JTextPane		
		final JPanel NoWrapPanel = new JPanel(new BorderLayout());
		NoWrapPanel.add(jtaScript, BorderLayout.CENTER);
		scrollpane = new JScrollPane(NoWrapPanel, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollpane.setRowHeaderView(lineNumber);
		scrollpane.getVerticalScrollBar().setUnitIncrement(ServerUIUtil.SCROLLPANE_VERTICAL_UNIT_PIXEL);
		
		scrollpane.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(final MouseWheelEvent e) {
				designer.codeHelper.flipTipStop();
				autoCodeTip.setEnable(false);				
			}
		});
		
		nameField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				final String newClassName = nameField.getText();
				currItem.name = newClassName;
				App.invokeLaterUI(updateTreeRunnable);
				notifyModified(true);
			}
			@Override
			public void removeUpdate(final DocumentEvent e) {
				modify();
			}
			
			@Override
			public void insertUpdate(final DocumentEvent e) {
				modify();
			}
			
			@Override
			public void changedUpdate(final DocumentEvent e) {
				modify();
			}
		});
		nameField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {//输入字符在途
					@Override
					public void run() {
						notifyModifyName();
					}
				});
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
			}
			
			@Override
			public void keyPressed(final KeyEvent e) {
			}
		});
		
		jumpRunnable = new EditorJumpRunnable() {
			@Override
			public void run() {
				final SearchDialog searchDialog = (SearchDialog)userObj;
				jtaScript.searchDialog = searchDialog;
				searchDialog.hcTextPane = jtaScript;
				
				final Vector<SearchResult> list = searchDialog.sameNodeList;
				if(list.get(0).highLight == null){
					final Highlighter highlighter = jtaScript.getHighlighter();
					highlighter.removeAllHighlights();
					
					final int size = list.size();
					for (int i = 0; i < size; i++) {
						final SearchResult sr = list.get(i);
						try {
							sr.highLight = searchDialog.addSearchHighlight(highlighter, sr.offset, sr.offset + sr.length);
							sr.highlighter = highlighter;
						} catch (final Throwable e) {
							e.printStackTrace();
						}
					}
				}else{
					final int size = list.size();
					for (int i = 0; i < size; i++) {
						final SearchResult sr = list.get(i);
						if(sr.offset == offset){
							offset = sr.highLight.getStartOffset();
							break;
						}
					}
				}
				
				final int endIdx = offset + len;
//				jtaScript.setCaretPosition(endIdx);
				jtaScript.setSelectionStart(offset);
				jtaScript.setSelectionEnd(endIdx);
				try {
					final Rectangle modelToView = jtaScript.modelToView(offset);
					modelToView.y += scrollpane.getHeight();
					jtaScript.scrollRectToVisible(modelToView);
				} catch (final Throwable e) {
					e.printStackTrace();
				}			
			}
		};
		addEditorOnly();
		showConsole(PropertiesManager.isTrue(PropertiesManager.p_isShowJRubyTestConsole, false));
	}
	
	protected final void replaceClassName(final String newClassName, final JTextField inputField){
		final String currNodeSuperClassName = findCurrNodeSuperClassName();
		if(currNodeSuperClassName != null && newClassName.length() > 0){
			final char firstChar = newClassName.charAt(0);
			if(firstChar >= 'A' && firstChar <= 'Z'){
				final boolean isReplaced = ScriptModelManager.replaceClassNameForScripts(jtaDocment, jtaScript.getText(), currNodeSuperClassName, newClassName);
				if(isReplaced){
					SwingUtilities.invokeLater(submitModifyRunnable);
				}
			}else{
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						final JPanel panel = new JPanel(new BorderLayout());
						panel.add(new JLabel("it must begin with an uppercase letter.", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
						final ActionListener listener = new HCActionListener(new Runnable() {
							@Override
							public void run() {
								inputField.requestFocus();
							}
						}, App.getThreadPoolToken());
						App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getErrorI18N(), false, null, null, listener, null, designer, false, false, inputField, false, false);
					}
				});
			}
		}
	}
	
	private final String findCurrNodeSuperClassName(){
		final int type = currItem.type;
		
		if(type == HPNode.TYPE_MENU_ITEM_FORM || type == HPNode.TYPE_MENU_ITEM_SCREEN){
			return HTMLMlet.class.getName();
		}else if(type == HPNode.MASK_MSB_ROBOT){
			return Robot.class.getName();
		}else if(type == HPNode.MASK_MSB_CONVERTER){
			return Converter.class.getName();
		}else if(type == HPNode.MASK_MSB_DEVICE){
			return Device.class.getName();
		}else if(type == HPNode.TYPE_MENU_ITEM_CONTROLLER){
			return CtrlResponse.class.getName();
		}
		
		return null;
	}
	
	private final boolean isELSIFIndentKeyWords(final char[] chars, final int startIdx, final char[] isIndentChar){//xx
		return CodeHelper.matchChars(chars, startIdx, isIndentChar);
//		int m = startIdx;
//		boolean inputSelfBackEnd = true;
//		for (int k = 0; k < isIndentChar.length && m < chars.length; k++, m++) {
//			if(isIndentChar[k] != chars[m]){
//				inputSelfBackEnd = false;
//				break;
//			}
//			if(inputSelfBackEnd){
//				return true;
//			}
//		}
//		return false;
	}
		
	public abstract void updateScript(String script);
	
	public static int getLineOfOffsetWithoutException(final Document doc, final int offset) {
		try{
			return getLineOfOffset(doc, offset);
		}catch (final Exception e) {
		}
		return -1;
	}
	
	public static int getLineOfOffset(final Document doc, final int offset) throws BadLocationException {
	    if (offset < 0) {
	        throw new BadLocationException("Can't translate offset to line", -1);
	    } else if (offset > doc.getLength()) {
	        throw new BadLocationException("Can't translate offset to line", doc.getLength() + 1);
	    } else {
	        return doc.getDefaultRootElement().getElementIndex(offset);
	    }
	}
	
	/**
	 * 获得指定行的文本
	 * @param doc
	 * @param line
	 * @return
	 * @throws BadLocationException
	 */
	public static final String getLineText(final Document doc, final int line) throws BadLocationException {
		final Element map = doc.getDefaultRootElement();
	    if (line < 0) {
	        throw new BadLocationException("Negative line", -1);
	    } else if (line >= map.getElementCount()) {
	        throw new BadLocationException("No such line", doc.getLength() + 1);
	    } else {
	        final Element lineElem = map.getElement(line);
	        final int lineStartOff = lineElem.getStartOffset();
	        final int lineEndOff = lineElem.getEndOffset();
	        return doc.getText(lineStartOff, lineEndOff - lineStartOff);
	    }
	}

	public static int getLineStartOffset(final Document doc, final int line) throws BadLocationException {
	    final Element map = doc.getDefaultRootElement();
	    if (line < 0) {
	        throw new BadLocationException("Negative line", -1);
	    } else if (line >= map.getElementCount()) {
	        throw new BadLocationException("No such line", doc.getLength() + 1);
	    } else {
	        final Element lineElem = map.getElement(line);
	        return lineElem.getStartOffset();
	    }
	}
	
	public static int getLineEndOffset(final Document doc, final int line) throws BadLocationException {
	    final Element map = doc.getDefaultRootElement();
	    if (line < 0) {
	        throw new BadLocationException("Negative line", -1);
	    } else if (line >= map.getElementCount()) {
	        throw new BadLocationException("No such line", doc.getLength() + 1);
	    } else {
	        final Element lineElem = map.getElement(line);
	        return lineElem.getEndOffset();
	    }
	}
	
	@Override
	public void init(final MutableTreeNode data, final JTree tree) {
		super.init(data, tree);
		
		isInited = false;
		
		designer.codeHelper.resetASTRoot();
		
		if(scriptUndoManager != null){
			scriptUndoManager.discardAllEdits();
			scriptUndoListener.isForbidRecordUndoEdit = true;
		}else{
			scriptUndoManager = new UndoManager();
			scriptUndoManager.setLimit(100);
			scriptUndoListener = new ScriptUndoableEditListener(this, scriptUndoManager);
			
			jtaDocment.addUndoableEditListener(scriptUndoListener);
		}

		jtaScript.setText("");
		errRunInfo.setText("");

		currItem = (HPNode)currNode.getUserObject();
		nameField.setText(currItem.name);
		
	}
	
	private ScriptUndoableEditListener scriptUndoListener;
	private UndoManager scriptUndoManager;
	public final AbstractDocument jtaDocment;
	public final StyledDocument jtaStyledDocment;
	final int fontHeight;
	int lastMouseClickX = -100, lastMouseClickY = -100;
	public final MouseMovingTipTimer autoCodeTip;
	int lastFocusUnderlineCSSStartIdx = -1;
	int lastFocusUnderlineCSSLen = 0;
	final Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	boolean isHandCursor;
	final SimpleAttributeSet normalBackground;
	
	public final HCTextPane jtaScript = new HCTextPane(){
		@Override
		public void paste(){
			try{
				isModifySourceForRebuildAST = false;
				scriptUndoListener.setUndoModel(ScriptUndoableEditListener.UNDO_MODEL_PASTE);
				final int oldCaretPos = jtaScript.getCaretPosition();
				final int oldLineNo = getLineOfOffsetWithoutException(jtaDocment, oldCaretPos);
				
				super.paste();
				
				final int newCaretPos = jtaScript.getCaretPosition();
				final int newLineNo = getLineOfOffset(jtaDocment, newCaretPos);
				
				if(oldLineNo != newLineNo){
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							format(jtaDocment, oldLineNo==0?0:(oldLineNo - 1), newLineNo);//可能插入新缩进
							rebuildASTNode();
							updateScript(jtaScript.getText());
//							final int newTotalLen = getDocument().getLength();
//							final int newNewLinePos = oldCaretPos  + (newTotalLen - totalLen);//重新计算新loc
//							setCaretPosition(newNewLinePos);
//							final int shiftPos = newNewLinePos - oldCaretPos;
//							if(adjustNum != 0){
//								jtaScript.setCaretPosition(jtaScript.getCaretPosition() + adjustNum);
//							}
							final int shiftPos = jtaScript.getCaretPosition() - oldCaretPos;
							TabHelper.notifyInputKey(false, null, (char)0, - shiftPos + 1);
						}
					});
				}else{
					ContextManager.getThreadPool().run(new Runnable() {//有可能发生在同一行
						@Override
						public void run() {
							rebuildASTNode();
						}
					}, threadPoolToken);
					updateScript(jtaScript.getText());
					refreshCurrLineAfterKey(newLineNo);
					final int shiftPos = newCaretPos - oldCaretPos;
					TabHelper.notifyInputKey(false, null, (char)0, - shiftPos + 1);
				}
				
			}catch (final Throwable e) {
//				ExceptionReporter.printStackTrace(e);
			}
			
		}
		@Override
		public void cut(){
//			synchronized (modifyAndColorAll) {
				isModifySourceForRebuildAST = false;
				scriptUndoListener.setUndoModel(ScriptUndoableEditListener.UNDO_MODEL_CUT);
				
				super.cut();
				
				try{
					final int newLineNo = getLineOfOffset(jtaDocment, jtaScript.getCaretPosition());
					ContextManager.getThreadPool().run(new Runnable() {//有可能发生在同一行
						@Override
						public void run() {
							rebuildASTNode();
						}
					}, threadPoolToken);
					updateScript(jtaScript.getText());
					refreshCurrLineAfterKey(newLineNo);
				}catch (final Throwable e) {
				}
//			}
		}
		@Override
		public void refreshCurrLineAfterKey(final int line) {
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					try{
						final int lineStartIdx = getLineStartOffset(jtaDocment, line);
						final int lineEndIdx = getLineEndOffset(jtaDocment, line);
						final String text = jtaDocment.getText(lineStartIdx, lineEndIdx - lineStartIdx);
//						closeDocumentListener();
						initBlock(text, lineStartIdx, false, false);
//						openDocumentListener();
//						final int caretPos = jtaScript.getCaretPosition();
//						jtaScript.updateUI();
//						jtaScript.setCaretPosition(caretPos);
						final String scripts = jtaScript.getText();
						updateScript(scripts);//更新待保存内容
					}catch (final Exception e) {
					}
				}
			});
		}
	};
	
	private static final String ONE_TAB_STR = String.valueOf('\t');
	
	final void format(final Document document){
		format(jtaDocment, 0, Integer.MAX_VALUE);
	}
	
	/**
	 * 格式化代码全文
	 * @param document
	 */
	final void format(final Document document, final int startLineIdx, final int endLineIdx){
//		System.out.println("format content : \n" + jtaScript.getText());
		int startLine = 0;
		int lineWillDen = 0;
		try{
			for( ; startLine <= endLineIdx ; startLine++){
				final int startPosition = getLineStartOffset(document, startLine);
				final int endPosition = getLineEndOffset(document, startLine);
				final String line = document.getText(startPosition, endPosition - startPosition);
//				System.out.println("" + startLine + ". " + line);
				final char[] lineChars = line.toCharArray();
				int currOldIndent = 0;
				boolean isInStr = false;
				boolean isSpaceBegin = true;
				char lastChar = 0;
				int strTrimIdx = 0;
				final int lineCharsLen = lineChars.length;
				for (; strTrimIdx < lineCharsLen; strTrimIdx++) {
					final char c = lineChars[strTrimIdx];
					if(c == '\n'){
						break;
					}
					final boolean isSpace = (c == ' ');
					if(isSpaceBegin && (isSpace || c == '\t')){
						if(isSpace){
							//删除空格，改为Tab
							final int charIdx = startPosition + strTrimIdx;
							document.remove(charIdx, 1);
							document.insertString(charIdx, ONE_TAB_STR, null);
						}
						currOldIndent++;
					}else{
						isSpaceBegin = false;
						if(isInStr == false && c == '#'){
							break;
						}else if(c == '\"' && lastChar != '\\'){
							isInStr = !isInStr;
						}
					}
					lastChar = c;
				}
				
				final int strLen = strTrimIdx - currOldIndent;
				
				//#开始的，也可能需要缩进
//				if(strLen == 0){
//					continue;
//				}
				final String lineColorText = new String(lineChars, currOldIndent, lineChars.length - currOldIndent);
				final String strTrim = ((currOldIndent==0&&strTrimIdx==lineCharsLen)?line:new String(lineChars, currOldIndent, strLen));
				{
					//是否反向缩进,else, elsif...
					boolean isDone = false;
					for (int i = 0; i < backIndentStr.length; i++) {
						if(strTrim.startsWith(backIndentStr[i], 0)){
							lineWillDen--;
							isDone = true;
							break;
						}
					}
					if(isDone == false){
						for (int i = 0; i < nextIndentStr.length; i++) {
							if(strTrim.startsWith(nextIndentStr[i], 0)){
								lineWillDen--;
								isDone = true;
								break;
							}
						}
					}
				}
				
				if(lineWillDen != currOldIndent){
					final int operateOffLine = startPosition + currOldIndent;
//					System.out.println("lineWillDen : " + lineWillDen + ", currOldIndent : " + currOldIndent);
					if(lineWillDen > currOldIndent){
						final int step = lineWillDen - currOldIndent;
						final StringBuilder sb = StringBuilderCacher.getFree();
						for (int i = 0; i < step; i++) {
							sb.append('\t');
						}
						final String tab = sb.toString();
						StringBuilderCacher.cycle(sb);
						document.insertString(operateOffLine, tab, null);
//						System.out.println("insert from " + operateOffLine + tab + "(tab:"+tab.length()+"), strTrim:" + strTrim);
//						System.out.println("after insert : " + document.getText(operateOffLine, 20));
					}else{
						final int step = currOldIndent - lineWillDen;
						document.remove(operateOffLine - step, step);
//						System.out.println("remove from " + (operateOffLine - step) + "(step:"+step+")");
//						System.out.println("after remove : " + document.getText((operateOffLine - step), 20));
					}
				}
				
				initBlock(lineColorText, startPosition + lineWillDen, false, false);//粘贴多行后，需要进行format和color
				
				{
					//是否下行需要缩进, if , while 
					boolean isDone = false;
					{
						final int size = WithEndIndentation.length;
						for (int i = 0; i < size; i++) {
							if(trimEqualWithIndentation(strTrim, i)){
								lineWillDen++;
								isDone = true;
								break;
							}
						}
					}
					if(isDone == false){
						final int size = doIndentation.length;
						for (int i = 0; i < size; i++) {
							if(strTrim.endsWith(doIndentation[i])){
								lineWillDen++;
								isDone = true;
								break;
							}
						}
					}
					if(isDone == false){
						isDone = WithEndIndentationKuoHao.matcher(strTrim).find();
						if(isDone){
							lineWillDen++;
						}
					}
					if(isDone == false){
						for (int i = 0; i < nextIndentStr.length; i++) {
							if(strTrim.startsWith(nextIndentStr[i], 0)){
								lineWillDen++;
								isDone = true;
								break;
							}
						}
					}
				}
			}
		}catch (final Exception e) {
			if(e instanceof BadLocationException){
			}else{
				ExceptionReporter.printStackTrace(e);
			}
		}
	}
	
	final void doAfterModifyBlock(final boolean isFormatNeed) {
		rebuildASTNode();
		if(isFormatNeed){
			format(jtaScript.getDocument());
		}
		updateScript(jtaScript.getText());
	}

	boolean newline = false;
//	final Runnable colorAll = new Runnable() {
//		int position = 0;
//		@Override
//		public void run() {
//			try{
//				initColor(false, true, position);
//				position = jtaScript.getCaretPosition();
//				final String scripts = jtaScript.getText();
//				updateScript(scripts);
//			}catch (final Throwable e) {
//				ExceptionReporter.printStackTrace(e);
//			}
//		}
//	};
	
	public final void initColor(final boolean useOldPosition, final int position){
		try{
			int colorOffset = 0, colorLen = -1;
			if(useOldPosition && position != 0){
				final int lineNo = getLineOfOffset(jtaDocment, position);
//				if(newline){
//					colorOffset = getLineStartOffset(jtaDocment, lineNo - 1);
//				}else{
					colorOffset = getLineStartOffset(jtaDocment, lineNo);
//				}
				colorLen = getLineEndOffset(jtaDocment, lineNo) - colorOffset;
			}else{
			}
			newline = false;
			String text = null;
			if(colorOffset == 0 && colorLen == -1){
				text = jtaScript.getText();
			}else{
				try{
					text = jtaScript.getText(colorOffset, (colorLen==-1)?jtaScript.getText().length():colorLen);
				}catch (final Exception e) {
					text = jtaScript.getText();
					colorOffset = 0;
				}
			}
//			System.out.println("change Line : " + text + "(endWithReturn : "+(text.charAt(text.length()-1)=='\n')+")");
			if(colorLen==-1 && text.indexOf('\r') >= 0){
				text = text.replace("\r", "");
				jtaScript.setText(text);
			}
			
			final String p_str = text;
			final int p_idx = colorOffset;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					initBlock(p_str, p_idx, false, true);
				}
			});
			
//			System.out.println("context : \n" + jtaScript.getText());
		}catch (final Exception e) {
			e.printStackTrace();//注意：不javax.swing.text.BadLocationException
//			ExceptionReporter.printStackTrace(e);
		}
//		buildHighlight(jtaScript, first_rem_pattern, REM_LIGHTER);
	}
	
	@Override
	public void loadAfterShow(final Runnable run){
		scriptUndoListener.isForbidRecordUndoEdit = false;
		scriptUndoManager.discardAllEdits();
		
		ContextManager.getThreadPool().run(focusJtaScript);
		
		super.loadAfterShow(run);
	}
	
	final Runnable focusJtaScript = new Runnable() {
		@Override
		public void run() {
			jtaScript.requestFocus();
//			jtaScript.setCaretPosition(0);
		}
	};
	
	public final void initBlock(final String text, final int offset, final boolean isReplace, final boolean isDelayStringRem) {
		if(text.length() == 0){
			return;
		}
		
		jtaStyledDocment.setCharacterAttributes(offset, text.length(), DEFAULT_LIGHTER, true);
		
		buildHighlight(hc_map_pattern, MAP_LIGHTER, offset, text, isReplace);
		buildHighlight(num_pattern, NUM_LIGHTER, offset, text, isReplace);//要置于字符串之前，因为字符串中可能含有数字
		buildHighlight(keywords_pattern, KEYWORDS_LIGHTER, offset, text, isReplace);
		buildHighlight(var_pattern, VAR_LIGHTER, offset, text, isReplace);
		buildHighlightForStringAndRem(offset, text, isReplace, isDelayStringRem);
	}
	
	private final void buildHighlight(final Pattern pattern, final SimpleAttributeSet attributes, final int offset, final String text, final boolean isReplace) {
		final Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			final int start = matcher.start() + offset;
			final int end = matcher.end() + offset;
			jtaStyledDocment.setCharacterAttributes(start, end - start, attributes, isReplace);
		}
	}

	private final void buildHighlightForStringAndRem(final int offset, final String text, final boolean isReplace, final boolean isDelayStringRem) {
		if(isDelayStringRem){
			SwingUtilities.invokeLater(new Runnable() {//由于多语言绘制耗时，所以delay
				@Override
				public void run() {
					colorRemAndStr(offset, text, isReplace);
				}
			});
		}else{
			colorRemAndStr(offset, text, isReplace);
		}
	}

	public final void undo() {
//		synchronized (modifyAndColorAll) {
		if (scriptUndoManager.canUndo()){
			isModifySourceForRebuildAST = false;
			scriptUndoManager.undo();
//			ContextManager.getThreadPool().run(new Runnable() {
//				@Override
//				public void run() {
////					rebuildASTNode();
//					App.invokeLaterUI(colorAll);
//				}
//			}, threadPoolToken);
		}
//		}
	}

	public final void redo() {
//		synchronized (modifyAndColorAll) {
			if (scriptUndoManager.canRedo()){
				isModifySourceForRebuildAST = false;
				scriptUndoManager.redo();
//				ContextManager.getThreadPool().run(new Runnable() {
//					@Override
//					public void run() {
//						rebuildASTNode();
//						App.invokeLaterUI(colorAll);
//					}
//				}, threadPoolToken);
			}
//		}
	}

	public final void setInitText(final String listener) {
		setUndoText(listener, 0);
	}
	
	private static boolean trimEqualWithIndentation(final String trim_str, final int withEndIdx){
		final String item = WithEndIndentation[withEndIdx];
		if(withEndIdx<=WithEndIndentationTrimEqual){
			return trim_str.equals(item) || trim_str.trim().equals(item);
		}else{
			return trim_str.startsWith(item, 0);
		}
	}
	
	private final char[] noChar = new char[1];

	private final void actionOnEnterKey(final Document doc) throws Exception{
		final StringBuilder sb = new StringBuilder(24);
		boolean isDoneEnter = false;
		boolean isMovSelfBack = false;
		
		final int InsertPosition = jtaScript.getCaretPosition();
		final int startLineNo = getLineOfOffset(doc, InsertPosition);
		int line = startLineNo;
		sb.append("\n");
		
		//复制上行的缩进到新行中
		boolean inputSelfBackEnd = false;
		newline = true;
		try {
			final int positionLine = line;
			while(line >= 0){
				final int startIdx = getLineStartOffset(doc, line);

				//获得缩进串
				final String lineStr = doc.getText(startIdx, InsertPosition - startIdx);//-1去掉\n
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
				
				int strTrimIdx = 0;
				{//将#后，置为space
					boolean isInStr = false;
					char lastChar = 0;
					final int lineCharsLen = chars.length;
					for (; strTrimIdx < lineCharsLen; strTrimIdx++) {
						final char c = chars[strTrimIdx];
						if(isInStr == false && c == '#'){
							break;
						}else if(c == '\"' && lastChar != '\\'){
							isInStr = !isInStr;
						}
						lastChar = c;
					}
					
//					for (; strTrimIdx < lineCharsLen; strTrimIdx++) {//将#后，置为space
//						chars[strTrimIdx] = ' ';
//					}
				}
				
				final String trim_str = new String(chars, charIdxRemovedTab, strTrimIdx - charIdxRemovedTab);
				if(trim_str.length() == 0 || trim_str.startsWith("#")){
					//复制上行的缩进
					sb.append(String.valueOf(chars, 0, charIdxRemovedTab));
					final String txt = sb.toString();
					final int newPos = InsertPosition + sb.length();
					setUndoText(txt, newPos);
					isDoneEnter = true;
					return;
				}
				inputSelfBackEnd = false;
				boolean nextRowIndent = false;
				for (int j = 0; j < backIndent.length; j++) {
					final char[] oneBackStr = backIndent[j];
					int l = charIdxRemovedTab;
					inputSelfBackEnd = true;
					for (int k = 0; k < oneBackStr.length && l < chars.length; k++, l++) {
						if(oneBackStr[k] != chars[l]){
							inputSelfBackEnd = false;
							break;
						}
					}
					if(inputSelfBackEnd){
						nextRowIndent = inputSelfBackEnd;
						//检查上行是否当前行已缩进。即在已有代码elsif xxx后进行回车，当前是否需要缩进
						final int startUpRowIdx = getLineStartOffset(doc, positionLine - 1);
						try{
							final String upRowStr = doc.getText(startUpRowIdx, startIdx - 1 - startUpRowIdx);
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
								if(isELSIFIndentKeyWords(upRowChars, charIdxUpRowRemovedTab, elsifChar)
										|| isELSIFIndentKeyWords(upRowChars, charIdxUpRowRemovedTab, ifChar)
										|| isELSIFIndentKeyWords(upRowChars, charIdxUpRowRemovedTab, caseChar)//when上行为case
										|| isELSIFIndentKeyWords(upRowChars, charIdxUpRowRemovedTab, whenChar)//else上行为when
									){
									inputSelfBackEnd = false;//取消自缩进
								}
							}
						}catch (final Exception ex) {
						}
						break;
					}
				}
				int charNewIdxRemovedTab = charIdxRemovedTab;
				int withEndInd = 0;
				{
					final int size = WithEndIndentation.length;
					for (int j = 0; j < size; j++) {
						if(trimEqualWithIndentation(trim_str, j)){
							chars[charNewIdxRemovedTab++] = '\t';
							withEndInd = END_TYPE1;
							break;
						}
					}
				}
				if(withEndInd == 0){
					final int size = doIndentation.length;
					for (int j = 0; j < size; j++) {
						if(trim_str.endsWith(doIndentation[j])){
							chars[charNewIdxRemovedTab++] = '\t';
							withEndInd = END_TYPE1;
							break;
						}
					}
				}
				
				if(withEndInd == 0){
					final Matcher matcher = WithEndIndentationKuoHao.matcher(trim_str);
					withEndInd = matcher.find()?(matcher.group(1).indexOf(DO_WORD)>=0?END_TYPE1:END_TYPE2):0;
					if(withEndInd > 0){
						chars[charNewIdxRemovedTab++] = '\t';
					}
				}

				boolean isNextIndentAlready = false;
				boolean hasEnd = false;
				boolean hasNewLineBeforeEnd = false;
				boolean hasEndInOldNextRowButNeedNewLine = false;//def search(field, genre: nil, duration: 120) V p [field, genre, duration ]\nend
				char[] afterNewEnterChars = noChar;
				boolean hasEndInOldNextRowButNeedNewLineCase2 = false;
				if(withEndInd > 0){
					//检查下行是否已缩进，
					try{
						int startOffOldNextLine = 0;
						try{
							afterNewEnterChars = doc.getText(InsertPosition, getLineEndOffset(doc, positionLine) - InsertPosition).toCharArray();
							startOffOldNextLine = getLineStartOffset(doc, positionLine + 1);
							final String oldNextLineBeforeEnter = doc.getText(startOffOldNextLine, getLineEndOffset(doc, positionLine + 1) - startOffOldNextLine);
							final char[] oldNextLineCharsBeforeEnter = oldNextLineBeforeEnter.toCharArray();
							hasEndInOldNextRowButNeedNewLine = isELSIFIndentKeyWords(afterNewEnterChars, 0, endChar) || afterNewEnterChars[0] == '}';
							if(hasEndInOldNextRowButNeedNewLine == false){
								hasEndInOldNextRowButNeedNewLine = isELSIFIndentKeyWords(oldNextLineCharsBeforeEnter, charIdxRemovedTab, endChar) || oldNextLineCharsBeforeEnter[charIdxRemovedTab] == '}';
								hasEndInOldNextRowButNeedNewLineCase2 = (hasEndInOldNextRowButNeedNewLine & true);
							}
						}catch (final Throwable e) {
						}
						
						final String nextLineAfterEnter = doc.getText(startOffOldNextLine, getLineEndOffset(doc, positionLine + 1) - startOffOldNextLine);
						
						final char[] nextLineChars = nextLineAfterEnter.toCharArray();
						hasNewLineBeforeEnd = (nextLineChars[0] == '\n');
						hasEnd = isELSIFIndentKeyWords(nextLineChars, charIdxRemovedTab + (hasNewLineBeforeEnd?1:0), endChar) || nextLineChars[charIdxRemovedTab+ (hasNewLineBeforeEnd?1:0)] == '}';
						int charIdxNextRemovedTab = hasNewLineBeforeEnd?1:0;
						for (; charIdxNextRemovedTab < nextLineChars.length; charIdxNextRemovedTab++) {
							if(nextLineChars[charIdxNextRemovedTab] == ' ' || nextLineChars[charIdxNextRemovedTab] == '\t'){
								
							}else{
								break;
							}
						}
						final boolean isElse = isELSIFIndentKeyWords(nextLineChars, charIdxNextRemovedTab, elsifChar)
								|| isELSIFIndentKeyWords(nextLineChars, charIdxNextRemovedTab, elseChar)
								|| isELSIFIndentKeyWords(nextLineChars, charIdxNextRemovedTab, whenChar);//case下行为when
						if((charIdxNextRemovedTab + (isElse?1:0)) > charIdxRemovedTab + (hasNewLineBeforeEnd?1:0)){
							isNextIndentAlready = true;
						}
					}catch (final Throwable e) {
					}finally{
						if(hasEnd == false){
							hasNewLineBeforeEnd = false;
						}
					}
				}
				
				try{
				final int newPosition;
				if(charNewIdxRemovedTab > 0){
					sb.append(String.valueOf(chars, 0, charNewIdxRemovedTab));
				}
				if(nextRowIndent && (inputSelfBackEnd == false)){
					sb.append("\t");
				}
				if(charNewIdxRemovedTab != 0 || withEndInd > 0){
					newPosition = InsertPosition + sb.length();
					if(withEndInd > 0 && (isNextIndentAlready == false) && (nextRowIndent == false)){
						final String strEnd = ((withEndInd == END_TYPE1)?"end":"}");
						if(hasEndInOldNextRowButNeedNewLine == false || hasNewLineBeforeEnd == false){
							final StringBuilder tailsb = new StringBuilder();
							if(hasNewLineBeforeEnd == false && hasEndInOldNextRowButNeedNewLineCase2 == false){
								tailsb.append("\n");
								tailsb.append(String.valueOf(chars, 0, charNewIdxRemovedTab - 1));//下一行，减少一个缩位
							}
							if(hasEnd == false && hasEndInOldNextRowButNeedNewLine == false){
								tailsb.append(strEnd);
								if(afterNewEnterChars.length > noChar.length){
									try {
										jtaDocment.insertString(InsertPosition - 1 + afterNewEnterChars.length, tailsb.toString(), KEYWORDS_LIGHTER);
									} catch (final BadLocationException e) {
									}
								}else{
									sb.append(tailsb.toString());
								}
							}else{
								sb.append(tailsb.toString());
							}
						}
						
						final String txt = sb.toString();
						setUndoText(txt, newPosition);
						isDoneEnter = true;
						initBlock(strEnd, InsertPosition + sb.length() - strEnd.length(), false, false);
					}
					break;
				}else{
					break;
				}
				}catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}finally{
					if(inputSelfBackEnd && charNewIdxRemovedTab > 0){
						doc.remove(startIdx, 1);//end之前的字符去掉一个缩位
						isMovSelfBack = true;
					}
				}
			}
			if(isDoneEnter == false){
				final String txt = sb.toString();
				final int newPos = InsertPosition + sb.length() + (isMovSelfBack?-1:0);
				setUndoText(txt, newPos);
			}
		} catch (final Throwable e) {
			if(e instanceof BadLocationException){
			}else{
				ExceptionReporter.printStackTrace(e);
			}
		}finally{
			//注释后的部分被回车，分配到下一行，需要进行行color刷新
			try{
				final int nextLineNo = startLineNo + 1;
				final int nextStartIdx = getLineStartOffset(jtaDocment, nextLineNo);
				final int currLineLen = getLineEndOffset(jtaDocment, nextLineNo) - nextStartIdx;
				if(currLineLen > 1){//==1 means \n only
					final String currLineText = jtaDocment.getText(nextStartIdx, currLineLen);
					initBlock(currLineText, nextStartIdx, false, false);
				}
			}catch (final Throwable e) {
			}
		}
		SwingUtilities.invokeLater(submitModifyRunnable);
	}

	private final void setUndoText(final String txt, final int newPos) {
		final int oldPos = jtaScript.getCaretPosition();
		try {
			jtaDocment.insertString(oldPos, txt, null);
		} catch (final BadLocationException e) {
		}
//		ResourceUtil.sendToClipboard(txt);
//		jtaScript.paste();
		isModifySourceForRebuildAST = true;//enable fireCaretUpdate， if (){} or def abc {}
		jtaScript.setCaretPosition(newPos);
	}
	
	protected final boolean isTypeFromTargetInput(final int type) {
		return type == HPNode.TYPE_MENU_ITEM_SCREEN || //早期mlet型的screen
				type == HPNode.TYPE_MENU_ITEM_FORM 
				|| type == HPNode.TYPE_MENU_ITEM_CONTROLLER;
	}

	private final void notifyModifyName() {
		final int type = currItem.type;
		
		if(isTypeFromTargetInput(type)){
			return;//在target框上
		}
		
		replaceClassName(nameField.getText(), nameField);
	}

	private final int focusCSSClass(final MouseEvent e, final CSSFocusAction action) {
		if(modifierKeysPressed == abstractCtrlKeyMash){//进入css class def
			final Point eventPoint = new Point(e.getX(), e.getY());
			final int scriptIdx = jtaScript.viewToModel(eventPoint);
			if(scriptIdx < 0){
				return -1;
			}	

			try{
				final int line = ScriptEditPanel.getLineOfOffset(jtaDocment, scriptIdx);
		        final int editLineStartIdx = ScriptEditPanel.getLineStartOffset(jtaDocment, line);
		        final int lineIdx = scriptIdx - editLineStartIdx;
		        final char[] lineChars = jtaDocment.getText(editLineStartIdx, ScriptEditPanel.getLineEndOffset(jtaDocment, line) - editLineStartIdx).toCharArray();
		        final CSSIdx cssIdx = CodeHelper.getCSSIdx(lineChars, lineIdx, lineIdx);
		        if(cssIdx != null && cssIdx.classIdx != -1){
		        	int i = cssIdx.classIdx;
		        	for (; i < lineChars.length; i++) {
						final char nextChar = lineChars[i];
						if(nextChar >= 'a' && nextChar <= 'z'
								|| nextChar >= 'A' && nextChar <= 'Z'
								|| nextChar >= '0' && nextChar <= '9'
								|| nextChar == '_'){
						}else{
							break;
						}
					}
		        	final String className = String.valueOf(lineChars, cssIdx.classIdx, i - cssIdx.classIdx);
		        	final int startIdx = editLineStartIdx + cssIdx.classIdx;
					action.action(startIdx, i - cssIdx.classIdx);
		        	return startIdx;
		        }
			}catch (final Throwable ex) {
				ex.printStackTrace();
			}
		}
		return -1;
	}
	
	private final void clearCSSClassFocus() {
		if(lastFocusUnderlineCSSStartIdx != -1){
			jtaScript.getStyledDocument().setCharacterAttributes(lastFocusUnderlineCSSStartIdx, lastFocusUnderlineCSSLen, UNDERLINE_REMOVE_LIGHTER, false);
			lastFocusUnderlineCSSStartIdx = -1;
			setEditorDefaultCurosr();
		}
	}
	
	private final int getSelectionLen() {
		final int selectStartIdx = jtaScript.getSelectionStart();
		final int selectEndIdx = jtaScript.getSelectionEnd();
		if(selectEndIdx > selectStartIdx){
			return selectEndIdx - selectStartIdx;
		}else{
			return 0;
		}
	}
	
	private final Runnable submitModifyRunnable = new Runnable() {
		@Override
		public void run() {
//			colorAll.run();
			updateScript(jtaScript.getText());
			rebuildASTNode();
		}
	};
	
	private static final int END_TYPE1 = 1;
	private static final int END_TYPE2 = 2;

	public static final void consumeEvent(final KeyEvent e) {
		e.setKeyChar('\0');
		e.consume();
	}
	
	private final void consumeEventLocal(final KeyEvent e) {
		consumeEvent(e);
		modifierKeysPressed = 0;
	}

	final Runnable clearSelectionBGDelay = new Runnable() {//否则导致paste，只清除selection，而没insert新内容
		@Override
		public void run() {
			final StyledDocument styledDocument = jtaScript.getStyledDocument();
			styledDocument.setCharacterAttributes(0, styledDocument.getLength(), normalBackground, false);
			jtaScript.selectedWordsMS = System.currentTimeMillis();
			jtaScript.hasSelectedWords = false;
		}
	};

	private final void clearSelectionBG() {
		if(jtaScript.hasSelectedWords && (System.currentTimeMillis() - jtaScript.selectedWordsMS) > 500){
			SwingUtilities.invokeLater(clearSelectionBGDelay);
		}
	}
	
	private static final Pattern codeInStringPattern = Pattern.compile("#\\{(.*?)\\}");
	
	private final void colorRemAndStr(final int offset, final String text, final boolean isReplace) {
		final Matcher matcher = rem_str_regexp_pattern.matcher(text);
		while (matcher.find()) {
			boolean isStr = false;
			final int shiftStartIdx = matcher.start();
			final int shiftEndIdx = matcher.end();
			final int start = shiftStartIdx + offset;
			final int end = shiftEndIdx + offset;
			final String matcherStr = matcher.group();
			final char firstChar = matcherStr.charAt(0);
			final SimpleAttributeSet lighter;
			if(firstChar == '#'){
				lighter = REM_LIGHTER;
			}else if(firstChar == '"' || firstChar == '\''){
				lighter = STR_LIGHTER;
				isStr = true;
			}else if(firstChar == '/'){
				lighter = REGEXP_LIGHTER;
			}else{
				lighter = DEFAULT_LIGHTER;
			}
			jtaStyledDocment.setCharacterAttributes(start, end - start, lighter, isReplace);
			if(isStr){
				final int indexOf = text.indexOf("#{", shiftStartIdx);
				if(indexOf >= 0 && indexOf < shiftEndIdx){
					colorCodeInStr(offset, text, isReplace, shiftStartIdx, shiftEndIdx);
				}
			}
		}
	}

	private final void colorCodeInStr(final int offset, final String text, final boolean isReplace,
			final int shiftStartIdx, final int shiftEndIdx) {
		final String substring = text.substring(shiftStartIdx, shiftEndIdx);
		final Matcher inCode = codeInStringPattern.matcher(substring);
		while(inCode.find()){
			final int mStartIdx = inCode.start(1);
			final int mEndIdx = inCode.end(1);
			final String code = substring.substring(mStartIdx, mEndIdx);
			initBlock(code, offset + shiftStartIdx + mStartIdx, isReplace, false);
		}
	}

	private final void showSelectCodesFirst() {
		JOptionPane.showMessageDialog(designer, "please select source codes first!", ResourceUtil.getErrorI18N(), JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
	}

	private static SimpleAttributeSet buildUnderline(final boolean isUnderLine){
		 final SimpleAttributeSet attributes = new SimpleAttributeSet();
		 StyleConstants.setUnderline(attributes, isUnderLine);
		 return attributes;
	}
	
	public static SimpleAttributeSet buildBackground(final Color color){
		 final SimpleAttributeSet attributes = new SimpleAttributeSet();
		 StyleConstants.setBackground(attributes, color);
		 return attributes;
	}
	
}

class UnderlineHighlightPainter extends LayeredHighlighter.LayerPainter {
	public UnderlineHighlightPainter(final Color c) {
		color = c;
	}

	@Override
	public void paint(final Graphics g, final int offs0, final int offs1, final Shape bounds, final JTextComponent c) {
	}

	@Override
	public Shape paintLayer(final Graphics g, final int offs0, final int offs1,
			final Shape bounds, final JTextComponent c, final View view) {
		g.setColor(color == null ? c.getSelectionColor() : color);

		Rectangle alloc = null;
		if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
			if (bounds instanceof Rectangle) {
				alloc = (Rectangle) bounds;
			} else {
				alloc = bounds.getBounds();
			}
		} else {
			try {
				final Shape shape = view.modelToView(offs0,
						Position.Bias.Forward, offs1, Position.Bias.Backward,
						bounds);
				alloc = (shape instanceof Rectangle) ? (Rectangle) shape
						: shape.getBounds();
			} catch (final BadLocationException e) {
				return null;
			}
		}

		final FontMetrics fm = c.getFontMetrics(c.getFont());
		final int baseline = alloc.y + alloc.height - fm.getDescent() + 1;
		
		final int endX = alloc.x + alloc.width;
		for (int i = alloc.x; i + 2 < endX; ) {
			final int x2 = i + 2;
			g.drawLine(i, baseline, x2, baseline);
			final int y1 = baseline + 1;
			g.drawLine(i, y1, x2, y1);
			i += 4;
		}

		return alloc;
	}

	protected Color color;
}

class ScriptUndoableEditListener implements UndoableEditListener{
	static final int UNDO_MODEL_TYPE = 1;
	static final int UNDO_MODEL_PASTE = 2;
	static final int UNDO_MODEL_CUT = 3;
	static final int UNDO_MODEL_ENTER = 4;
	static final int UNDO_MODEL_BACK_OR_DEL = 5;

	boolean isForbidRecordUndoEdit = true;
	private int undoModel = UNDO_MODEL_BACK_OR_DEL;
	final UndoManager manager;
	final ScriptEditPanel panel;
	
	final void setUndoModel(final int model){
		undoModel = model;
	}
	
	ScriptUndoableEditListener(final ScriptEditPanel panel, final UndoManager manager){
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
			manager.addEdit(new HCUndoableEdit(panel, edit, undoModel));
		}
	}
}

class HCUndoableEdit implements UndoableEdit{
	final int beforeCaretPos;
	int afterCaretPos;
	final UndoableEdit base;
	final HCTextPane jta;
	final int undoModel;
	final Caret caret;
	int selectionLen;
	final ScriptEditPanel panel;
	final boolean isModi;
	final long saveToken;
	DocumentEvent.EventType type;
	
	HCUndoableEdit(final ScriptEditPanel panel, final UndoableEdit base, final int undoModel){
		this.panel = panel;
		
		this.jta = panel.jtaScript;
		this.caret = jta.getCaret();
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
				if(undoModel == ScriptUndoableEditListener.UNDO_MODEL_TYPE
						|| undoModel == ScriptUndoableEditListener.UNDO_MODEL_BACK_OR_DEL){
					selectionLen = 1;
				}else{
					selectionLen = dde.getLength();
				}
			}else if(type == DocumentEvent.EventType.INSERT){
				if(undoModel == ScriptUndoableEditListener.UNDO_MODEL_TYPE
						|| undoModel == ScriptUndoableEditListener.UNDO_MODEL_BACK_OR_DEL){
				}else{
					selectionLen = dde.getLength();
				}
			}
		}
		
		if(undoModel == ScriptUndoableEditListener.UNDO_MODEL_ENTER){
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
		final Document document = jta.getDocument();

		final int oldLineNo = ScriptEditPanel.getLineOfOffsetWithoutException(document, jta.getCaretPosition());
		
		base.undo();
		
		if(beforeCaretPos > 0){
			if(undoModel == ScriptUndoableEditListener.UNDO_MODEL_PASTE){
				if(type == DocumentEvent.EventType.INSERT){
					caret.setDot(beforeCaretPos - selectionLen);
				}else{
					caret.setDot(beforeCaretPos);
				}
			}else if(undoModel == ScriptUndoableEditListener.UNDO_MODEL_ENTER){
				caret.setDot(beforeCaretPos - selectionLen);
			}
		}
		
		try{
			final int newLineNo = ScriptEditPanel.getLineOfOffset(document, jta.getCaretPosition());
			if(oldLineNo != newLineNo){
				if(undoModel == ScriptUndoableEditListener.UNDO_MODEL_ENTER){
				}else{
					panel.rebuildASTNode();
					panel.format(document);
				}
				panel.updateScript(jta.getText());
			}else{
				jta.refreshCurrLineAfterKey(newLineNo);
			}
		}catch (final Exception e) {
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
		final Document document = jta.getDocument();
		
		final int oldLineNo = ScriptEditPanel.getLineOfOffsetWithoutException(document, jta.getCaretPosition());
		
		base.redo();
		
		if(afterCaretPos > 0){
			if(undoModel == ScriptUndoableEditListener.UNDO_MODEL_ENTER){
				
				final Document doc = jta.getDocument();
				try{
					final int lineNo = ScriptEditPanel.getLineOfOffset(doc, afterCaretPos + 1);
					final int startOff = ScriptEditPanel.getLineStartOffset(doc, lineNo);
					final int endOff = ScriptEditPanel.getLineEndOffset(doc, lineNo);
					final String strEnd = doc.getText(startOff, endOff - startOff);//redo可能产生end
					
//					panel.initBlock(strEnd, startOff, true);
					final Matcher matcher = ScriptEditPanel.keywords_pattern.matcher(strEnd);
					if (matcher.find()) {
						final int start = matcher.start() + startOff - 1;
						final int end = matcher.end() + startOff;
						panel.jtaStyledDocment.setCharacterAttributes(start, end - start, ScriptEditPanel.KEYWORDS_LIGHTER, true);
					}else{
						panel.jtaStyledDocment.setCharacterAttributes(startOff, endOff - startOff, ScriptEditPanel.DEFAULT_LIGHTER, true);
					}
				}catch (final Exception e) {
				}
				caret.setDot(afterCaretPos);
			}
		}
		
		try{
			final int newLineNo = ScriptEditPanel.getLineOfOffset(document, jta.getCaretPosition());
			if(oldLineNo != newLineNo){
				panel.doAfterModifyBlock(true);
			}else{
				jta.refreshCurrLineAfterKey(newLineNo);
			}
		}catch (final Exception e) {
		}
		
		if(saveToken == panel.getSaveToken()){
			if(isModi == false){
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

class CSSListCellRenderer extends JLabel implements ListCellRenderer {
	final Color listBackColor = ServerUIUtil.LIGHT_BLUE_BG;
	
    @Override
    public Component getListCellRendererComponent(final JList list, final Object value,
            final int index, final boolean isSelected, final boolean cellHasFocus) {
    	setText(value.toString());
    	
    	if (isSelected) {
    	    setForeground(list.getSelectionForeground());
    	    setBackground(list.getSelectionBackground());
    	}else{
	    	if(index % 2 == 1){
	    		setBackground(listBackColor);
	    	}else{
	    		setBackground(list.getBackground());
	    	}
	    	setForeground(list.getForeground());
    	}
    	setOpaque(true);
        return this;
    }
}
