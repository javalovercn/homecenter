package hc.server.ui.design.hpj;

import hc.App;
import hc.core.ContextManager;
import hc.core.IContext;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.HCActionListener;
import hc.server.data.StoreDirManager;
import hc.server.ui.HCByteArrayOutputStream;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.Designer;
import hc.server.ui.design.code.CodeHelper;
import hc.server.ui.design.code.TabHelper;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.util.ContextSecurityManager;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.IDArrayGroup;
import hc.util.ResourceUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
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
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
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
	//要置于createRunTestDir之后
	public static HCJRubyEngine runTestEngine;
	
	public static synchronized final HCJRubyEngine getRunTestEngine(){
		if(runTestEngine == null){
			ScriptEditPanel.rebuildJRubyEngine();
		}
		return runTestEngine;
	}
	
	private static final SimpleAttributeSet STR_LIGHTER = build(Color.decode("#4EA539"), false);
	public static final SimpleAttributeSet REM_LIGHTER = build(Color.decode("#3AC2EB"), false);
	private static final SimpleAttributeSet MAP_LIGHTER = build(Color.BLACK, true);
	private static final SimpleAttributeSet KEYWORDS_LIGHTER = build(Color.BLUE, true);
	private static final SimpleAttributeSet NUM_LIGHTER = build(Color.RED, false);
	private static final SimpleAttributeSet DEFAULT_LIGHTER = build(Color.BLACK, false);
	private static final SimpleAttributeSet VAR_LIGHTER = build(Color.decode("#f19e37"), false);
	
	private static final Pattern str_pattern = Pattern.compile("\".*?(?<!\\\\)\"");
	private static final Pattern keywords_pattern = Pattern.compile("\\b(BEGIN|END|__ENCODING__|__END__|__FILE__|__LINE__|alias|" +
			"and|begin|break|case|class|def|defined?|do|else|elsif|end|" +
			"ensure|extend|false|for|if|in|import|include|module|next|nil|not|or|raise|redo|require|rescue|retry|return|" +
			"self|super|then|true|undef|unless|until|when|while|yield)\\b", Pattern.MULTILINE);
//	private static final String[] Indentation = {"begin", "case ", "class ", "def ", "else", 
//		"elsif ", "for ", "if ", "module ", "when ", "while ", "rescue "};
	private static final String[] WithEndIndentation = {"begin", "case ", "class ", "def ", "for ", "if ", "module ", "when "};//由于while[ ]后有do，所以在此移去
	private static final String[] doIndentation = {" do", " do\n"};
	
	//addActionListener{|exception| or do |exception|。|exception|段可能有，可能没有。如果测试，参见TestEndIndentKuoHao
	private static final Pattern WithEndIndentationKuoHao = Pattern.compile("(\\{|\\s+do)\\s*?(\\|\\s*?\\w+?\\s*?\\|)?\\s*?(?<!\\})\\s*?(#.*)?$");
	
	private static final char[] elsifChar = {'e', 'l', 's', 'i', 'f', ' '};
	private static final char[] elseChar = {'e', 'l', 's', 'e'};
	private static final char[][] backIndent = {
		elseChar,
		elsifChar,
		{'r', 'e', 's', 'c', 'u', 'e'},//可以没有异常类型段，所以去掉空格
		{'e', 'n', 's', 'u', 'r', 'e'}
	};
	private static final String[] backIndentStr = {"end", "else", "elsif ", "rescue", "ensure", "}"};//带end，是供全局format之用
	private static final String[] nextIndentStr = {"else", "elsif ", "rescue", "ensure"};//带end，是供全局format之用
	
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
	
	private static final Pattern num_pattern = Pattern.compile("\\b\\d+\\b", Pattern.MULTILINE);
	private static final Pattern hc_map_pattern = Pattern.compile("\\$_hcmap\\b");
	private static final Pattern rem_pattern = Pattern.compile("#.*(?=\n)?");
	private static final Pattern var_pattern = Pattern.compile("@\\w+");

	HPNode currItem;
	final JScrollPane scrollpane;
	boolean isModifySourceForRebuildAST = false;
	boolean isErrorOnBuildScript;
	final JLabel errRunInfo = new JLabel("");
	final JButton testBtn = new JButton("Test Script");
	final JButton formatBtn = new JButton("Format");
	final HCByteArrayOutputStream iconBsArrayos = ServerUIUtil.buildForMaxIcon();
	final JTextField nameField = new JTextField();
	final JRubyErrorHCTimer errorTimer = new JRubyErrorHCTimer("JRubyErrorHCTimer", 1000, false);
	boolean isInited = false;
	final boolean isJ2SEServer = ResourceUtil.isJ2SELimitFunction();

	abstract Map<String, String> buildMapScriptParameter();
	
	final public void updateScriptInInitProcess() {
		final boolean result = designer.codeHelper.updateScriptASTNode(this, jtaScript.getText(), true);
//		if(result == false){
//			SwingUtilities.invokeLater(new Runnable() {
//				@Override
//				public void run() {
//					try{
//						format(jtaScript.getDocument());//init中调用，导致系统变慢，并buildMap可能检查错误。
//						
//						doTest(false, true);
//					}catch (final Throwable e) {
//					}
//				}
//			});
//		}
	}
	
	final void doTest(final boolean isRun, final boolean isCompileOnly) {
		final Map<String, String> map = buildMapScriptParameter();
		if(runTestEngine != null && runTestEngine.getEvalException() != null){
			//清空旧错误
			jtaScript.getHighlighter().removeAllHighlights();
		}
		
		if(designer.tryBuildTestJRuby() == false){
			return;
		}
		
		final ProjectContext context = ContextSecurityManager.getConfig(
				(ThreadGroup)HCLimitSecurityManager.getTempLimitThreadPool().getThreadGroup()).getProjectContext();
		
		final String script = jtaScript.getText();
		ScriptException evalException = null;
		try{
			RubyExector.parse(script, null, runTestEngine, false);
			evalException = runTestEngine.getEvalException();
			if(evalException == null && (isRun || isCompileOnly == false)){
				RubyExector.run(script, null, map, runTestEngine, context);
				RubyExector.removeCache(script, runTestEngine);
				evalException = runTestEngine.getEvalException();
			}
		}catch (final Throwable e) {
			evalException = runTestEngine.getEvalException();
		}
		if(evalException != null){
//			final Object runnable = Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation");
//			 if (runnable != null && runnable instanceof Runnable){
//				 ((Runnable)runnable).run();
//			 }
			 
			Toolkit.getDefaultToolkit().beep();
			
			errRunInfo.setText(evalException.getMessage());
			errRunInfo.setBackground(testBtn.getBackground());
			isErrorOnBuildScript = true;
			
			final int line = evalException.getLineNumber();
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

	static {
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				try{
					Thread.sleep(1000);
				}catch (final Exception e) {
				}
				RubyExector.initActive(getRunTestEngine());//提前预热
			}
		});
	}
	public ScriptEditPanel() {
		errRunInfo.setForeground(Color.RED);
		errRunInfo.setOpaque(true);

		errorTimer.setErrorLable(errRunInfo, testBtn);
		
		final String runTip = "<STRONG>Note : </STRONG> even if a green bar is displayed in bottom, <BR>" +
				"defects may be in the scripts that are not covered.";
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
							
							IDArrayGroup.showMsg(IDArrayGroup.MSG_JRUBY_RUN_NO_COVER, App.SYS_WARN_ICON, (String)ResourceUtil.get(IContext.INFO), 
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
							format(jtaScript.getDocument());
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
				"<BR>" +
				"<BR>" + runTip +
				"</html>");
		testBtn.setIcon(Designer.loadImg("test_16.png"));
		
		formatBtn.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + F) <BR><BR>format the JRuby script.</html>");
		formatBtn.setIcon(Designer.loadImg("format_16.png"));
		
		//		jtaScript.setTabSize(2);
		
		//以下代码设置Tab跳过指定的空格
		final FontMetrics fm = jtaScript.getFontMetrics(jtaScript.getFont());
		jtaScript.setForeground(jtaScript.getBackground());
		final int cw = fm.stringWidth("    ");
		final float f = cw;
		final TabStop[] tabs = new TabStop[10]; // this sucks
		for(int i = 0; i < tabs.length; i++){
			tabs[i] = new TabStop(f * (i + 1), TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
		}
		final TabSet tabset = new TabSet(tabs);
		final StyleContext sc = StyleContext.getDefaultStyleContext();
		final AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabset);
		jtaScript.setParagraphAttributes(aset, false);	
		
		jtaScript.addCaretListener(new CaretListener() {
			int lastLineNo = 0;
			final Document doc = jtaScript.getDocument();
			@Override
			public void caretUpdate(final CaretEvent e) {
				try{
					final int currLineNo = getLineOfOffset(doc, e.getDot());
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
							jtaScript.getDocument().insertString(posi, "\t", null);
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
				final Document doc = jtaScript.getDocument();
				@Override
				public void actionPerformed(final ActionEvent e) {
					if(TabHelper.pushTabOrEnterKey()){
					}else{
						try{
							actionOnEnterKey(doc);
						}catch (final Exception ex) {
						}
					}
				}
			};
			final KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
			jtaScript.getInputMap().put(enterKey, enterAction);
		}
		
		jtaScript.addKeyListener(new KeyListener() {
			final Document doc = jtaScript.getDocument();
			final boolean isMacOS = ResourceUtil.isMacOSX();
			
			@Override
			public void keyTyped(final KeyEvent event) {
				
				final char inputChar = event.getKeyChar();
				final int modifiers = event.getModifiers();
				
//				final int keycode = e.getKeyCode();
//				System.out.println("keyCode : " + keycode + ", inputChar : " + inputChar + ", modifyMask : " + modifiers);
				final CodeHelper codeHelper = designer.codeHelper;
//				System.out.println("codeHelp wordcode : " + codeHelper.wordCompletionCode + ", char : " + codeHelper.wordCompletionChar + 
//						", modify : " + codeHelper.wordCompletionModifyCode + ", modifyMask : " + codeHelper.wordCompletionModifyMaskCode);
				
				//有输出字符时的触发提示代码。在Mac环境下Ctrl+.时，无输出字符，modifiers==ctrl_mask且inputChar!=0；在Mac环境下option+/时，出现字符÷且modifier==0
				if(isMacOS && (inputChar != 0 && inputChar == codeHelper.wordCompletionChar
						&& ((codeHelper.wordCompletionModifyCode == KeyEvent.VK_ALT && modifiers == 0) 
									|| (codeHelper.wordCompletionModifyMaskCode == modifiers)))){//注意：请同步到MletNodeEditPanel
					try {
						codeHelper.input(ScriptEditPanel.this, jtaScript, doc, fontHeight, true, 
								jtaScript.getCaret().getMagicCaretPosition(), jtaScript.getCaretPosition());
					} catch (final Exception e) {
//						if(L.isInWorkshop){
							ExceptionReporter.printStackTrace(e);
//						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								doTest(false, false);
							}
						});
					}
					consumeEvent(event);
					return;
				}
				
				isModifySourceForRebuildAST = true;

				try{
			        position = jtaScript.getCaretPosition();
					final int line = getLineOfOffset(doc, position);
					if(inputChar == KeyEvent.VK_ENTER){
						return;
						//end if VK_ENTER
					}else{
//						final int keyCode = e.getKeyCode();
						
						refreshCurrLineAfterKey(line, doc, jtaScript, ScriptEditPanel.this);
						
						TabHelper.notifyInputKey(inputChar == KeyEvent.VK_BACK_SPACE, event, inputChar);
						
						final boolean isDot = inputChar == '.';
						final boolean isPathSplit = inputChar == '/';
						if(isDot || isPathSplit || inputChar == ':'){//自动弹出代码提示条件
					        final int lineStartIdx = ScriptEditPanel.getLineStartOffset(doc, line);
					        final int lineIdx = position - lineStartIdx;
					        final char[] lineChars = doc.getText(lineStartIdx, lineIdx).toCharArray();
					        final int lineCharsLen = lineChars.length;
					        
					        if(isHttpResource(lineChars, lineCharsLen - 1)){
					        	return;//对于http型的资源，不弹出本地库资源代码提示
					        }
					        
					        //处于""之中
					        int countYinHao = 0;
					        for (int i = 0; i < lineCharsLen; i++) {
								if(lineChars[i] == '\"' && i > 0 && lineChars[i - 1] != '\\'){
									countYinHao++;
								}
							}
					        if(countYinHao % 2 == 1){//countYinHao > 0 && 
					        	if(isPathSplit){
					        		popUpAuto(codeHelper);
					        		return;
					        	}
					        	
					        	if(L.isInWorkshop){
					        		L.V = L.O ? false : LogManager.log("input dot (.) is in yinhao, skip auto popup codetip.");
					        	}
					        	return;
					        }
					        
					        if(isPathSplit){
					        	return;
					        }
					        
					        //处于#之后
					        for (int i = 0; i < lineCharsLen; i++) {
					        	if(lineChars[i] == '#'){
					        		return;
					        	}
					        }

					        if(isDot){
						        //a = 100.2 + 10.情形
						        boolean isVarOrMethodCase = false;
						        for (int i = lineCharsLen - 1; i >= 0; i--) {
						        	final char oneChar = lineChars[i];
									if(oneChar >= '1' && oneChar <= '0'){
										continue;
									}else if((oneChar >= 'a' && oneChar <= 'z') || (oneChar >='A' && oneChar <= 'Z') || oneChar == '_' || oneChar == ')'){// )表示方法
										isVarOrMethodCase = true;
									}
									break;
								}
						        
						        if(isVarOrMethodCase){
						        }else{
						        	if(L.isInWorkshop){
						        		L.V = L.O ? false : LogManager.log("input dot (.) is not for variable, skip auto popup codetip.");
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
						}
					}
				}catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}//end try
			}

			private void popUpAuto(final CodeHelper codeHelper) {
				//自动弹出代码提示
				final Point magicCaretPosition = jtaScript.getCaret().getMagicCaretPosition();
				final int caretPosition = jtaScript.getCaretPosition() + 1;//+1是因为当前正在输入的需要计入
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						try{
							codeHelper.input(ScriptEditPanel.this, jtaScript, doc, fontHeight, false, 
									magicCaretPosition, caretPosition);
						}catch (final Exception e) {
						}
					}
				});
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
//				final int keycode = e.getKeyCode();
//				switch (keycode) {
//				case KeyEvent.VK_ALT:
//				case KeyEvent.VK_SHIFT:
//				case KeyEvent.VK_CAPS_LOCK:
//				case KeyEvent.VK_CONTROL:
//				case KeyEvent.VK_ESCAPE:
//					return;
//				}
//				
//				if(Designer.isDirectKeycode(keycode) == false && Designer.isCopyKeyEvent(e, isJ2SEServer) == false){
//					App.invokeLaterUI(run);
//				}
			}
			
			final int fontHeight = jtaScript.getFontMetrics(jtaScript.getFont()).getHeight();
			
			@Override
		    public void keyPressed(final KeyEvent event) {
	            final int keycode = event.getKeyCode();
//				switch (keycode) {
//				case KeyEvent.VK_ALT:
//				case KeyEvent.VK_SHIFT:
//				case KeyEvent.VK_CAPS_LOCK:
//				case KeyEvent.VK_CONTROL:
//				case KeyEvent.VK_ESCAPE:
//					return;
//				}
	            final int modifiers = event.getModifiers();
	            final CodeHelper codeHelper = designer.codeHelper;
				final int wordCompletionModifyMaskCode = codeHelper.wordCompletionModifyMaskCode;
				//无输入字符时的触发提示代码
				if(keycode == codeHelper.wordCompletionCode && (modifiers & wordCompletionModifyMaskCode) == wordCompletionModifyMaskCode){
					//注意：请同步到MletNodeEditPanel
					try {
						codeHelper.input(ScriptEditPanel.this, jtaScript, doc, fontHeight, true, 
								jtaScript.getCaret().getMagicCaretPosition(), jtaScript.getCaretPosition());
					} catch (final Throwable e) {
						ExceptionReporter.printStackTrace(e);
					}
					consumeEvent(event);
					return;
				}
	            
	            if (modifiers == KeyEvent.CTRL_MASK) {
	                if (keycode == KeyEvent.VK_Z) {
	                	//ctrl + z
	                    undo();
	                }else if(keycode == KeyEvent.VK_Y) {
	                	//ctrl + y
	                    redo();
	                }
	            }else if(keycode == KeyEvent.VK_Z){
					if(isMacOS){
		                if (modifiers == KeyEvent.META_MASK) {
		                	//cmd+z
		                    undo();
		                }else if(modifiers == (KeyEvent.META_MASK | KeyEvent.SHIFT_MASK)) {
		                	redo();
		                }
	            	}
	            }
	            
	            if(keycode == KeyEvent.VK_ESCAPE){
					TabHelper.pushEscKey();
					return;
				}
	            
		    }
		});

		final LineNumber lineNumber = new LineNumber(jtaScript);
		scrollpane = new JScrollPane(jtaScript, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollpane.setRowHeaderView(lineNumber);

		nameField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				currItem.name = nameField.getText();
				App.invokeLaterUI(updateTreeRunnable);
				notifyModified();
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
		nameField.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				currItem.name = nameField.getText();
				App.invokeLaterUI(updateTreeRunnable);
				notifyModified();
			}
		}, threadPoolToken));

	}
	
	private final boolean isELSIFIndentKeyWords(final char[] chars, final int startIdx, final char[] isIndentChar){
		int m = startIdx;
		boolean inputSelfBackEnd = true;
		for (int k = 0; k < isIndentChar.length && m < chars.length; k++, m++) {
			if(isIndentChar[k] != chars[m]){
				inputSelfBackEnd = false;
				break;
			}
			if(inputSelfBackEnd){
				return true;
			}
		}
		return false;
	}
		
	public abstract void updateScript(String script);
	
	public void notifyModified(){
		if(isInited){
			currItem.getContext().modified.setModified(true);
		}
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
	
	static int getLineEndOffset(final Document doc, final int line) throws BadLocationException {
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
		
		jtaScript.setText("");
		errRunInfo.setText("");

		currItem = (HPNode)currNode.getUserObject();
		nameField.setText(currItem.name);
	}
	boolean undoInProcess = false;
	PositionUndoManager undoManager;
	final JTextPane jtaScript = new JTextPane(){
		final Document document = getDocument();
		
		@Override
		public void paste(){
			try{
				synchronized (modifyAndColorAll) {
					undoManager.recordPosition(true);
					super.paste();
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						format(document);
						App.invokeLaterUI(modifyAndColorAll);
					}
				});
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		@Override
		public void cut(){
			synchronized (modifyAndColorAll) {
				undoManager.recordPosition(true);
				super.cut();
				App.invokeLaterUI(modifyAndColorAll);
			}
		}
	};
	
	private static final String ONE_TAB_STR = String.valueOf('\t');
	
	/**
	 * 格式化代码全文
	 * @param document
	 */
	private final void format(final Document document){
//		System.out.println("format content : \n" + jtaScript.getText());
		int startLine = 0;
		int lineWillDen = 0;
		try{
			for( ; true ; startLine++){
				final int startPosition = getLineStartOffset(document, startLine);
				final int endPosition = getLineEndOffset(document, startLine);
				final String line = document.getText(startPosition, endPosition - startPosition);
//				System.out.println("" + startLine + ". " + line);
				final char[] lineChars = line.toCharArray();
				int currOldIndent = 0;
				for (int i = 0; i < lineChars.length; i++) {
					final boolean isSpace = (lineChars[i] == ' ');
					if(isSpace || lineChars[i] == '\t'){
						if(isSpace){
							//删除空格，改为Tab
							final int charIdx = startPosition + i;
							document.remove(charIdx, 1);
							document.insertString(charIdx, ONE_TAB_STR, null);
						}
						currOldIndent++;
					}else{
						break;
					}
				}
				
				final String strTrim = ((currOldIndent==0)?line:new String(lineChars, currOldIndent, lineChars.length - currOldIndent));
				if(strTrim.length() == 0 || (strTrim.length() == 1 && strTrim.charAt(0) == '\n')){//#开始的，也可能需要缩进
					continue;
//					System.out.println("empty line at line : " + startLine);
				}else{
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
//						System.out.println("lineWillDen : " + lineWillDen + ", currOldIndent : " + currOldIndent);
						if(lineWillDen > currOldIndent){
							final int step = lineWillDen - currOldIndent;
							String tab = "";
							for (int i = 0; i < step; i++) {
								tab += "\t";
							}
							document.insertString(operateOffLine, tab, null);
//							System.out.println("insert from " + operateOffLine + tab + "(tab:"+tab.length()+"), strTrim:" + strTrim);
//							System.out.println("after insert : " + document.getText(operateOffLine, 20));
						}else{
							final int step = currOldIndent - lineWillDen;
							document.remove(operateOffLine - step, step);
//							System.out.println("remove from " + (operateOffLine - step) + "(step:"+step+")");
//							System.out.println("after remove : " + document.getText((operateOffLine - step), 20));
						}
					}
				}
				
				initBlock(strTrim, startPosition + lineWillDen);//粘贴多行后，需要进行format和color
				
				{
					//是否下行需要缩进, if , while 
					boolean isDone = false;
					{
						final int size = WithEndIndentation.length;
						for (int i = 0; i < size; i++) {
							if(strTrim.startsWith(WithEndIndentation[i], 0)){
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
	
	int position;
	boolean newline = false;
	final Runnable modifyAndColorAll = new Runnable() {
		@Override
		public void run() {
			try{
				synchronized (modifyAndColorAll) {
					initColor(false, true);
					notifyModified();
					final String scripts = jtaScript.getText();
					updateScript(scripts);		
				}
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
	};
	
	final void initColor(final boolean isLoadColor, final boolean useOldPosition){
		if(isLoadColor){//等待其它事件完成
			try{
				Thread.sleep(50);
			}catch (final Exception e) {
			}
		}
		undoInProcess = false;
		final Document document = jtaScript.getDocument();
		if(isLoadColor){
			if(undoManager != null){
				document.removeUndoableEditListener(undoManager);
				undoManager.die();
			}
			undoManager = new PositionUndoManager(jtaScript){
				@Override
				public void undoableEditHappened(final UndoableEditEvent e) {
					synchronized (modifyAndColorAll) {
						if(undoInProcess){
							this.addEdit(e.getEdit());
						}				
					}
				}
			};
			undoManager.setLimit(20);
			document.addUndoableEditListener(undoManager);
		}
		try{
			int colorOffset = 0, colorLen = -1;
			if(useOldPosition && position != 0){
				final int lineNo = getLineOfOffset(document, position);
				if(newline){
					colorOffset = getLineStartOffset(document, lineNo - 1);
				}else{
					colorOffset = getLineStartOffset(document, lineNo);
				}
				colorLen = getLineEndOffset(document, lineNo) - colorOffset;
			}else{
			}
			position = 0;
			newline = false;
			
			final String text = jtaScript.getText(colorOffset, (colorLen==-1)?jtaScript.getText().length():colorLen);
//			System.out.println("change Line : " + text + "(endWithReturn : "+(text.charAt(text.length()-1)=='\n')+")");
			if(colorLen==-1 && text.indexOf("\r") >= 0){
				jtaScript.setText(text.replace("\r", ""));
				colorLen = jtaScript.getText().length();
			}
			
			final int finalColorOffSet = colorOffset;
			initBlock(text, finalColorOffSet);
//			System.out.println("context : \n" + jtaScript.getText());
		}catch (final Exception e) {
			e.printStackTrace();//注意：不javax.swing.text.BadLocationException
//			ExceptionReporter.printStackTrace(e);
		}
		undoInProcess = true;
//		buildHighlight(jtaScript, first_rem_pattern, REM_LIGHTER);
	}

	public final void initBlock(final String text, final int offset) {
		final StyledDocument document = (StyledDocument)jtaScript.getDocument();
		document.setCharacterAttributes(offset, text.length(), DEFAULT_LIGHTER, true);
		
		buildHighlight(jtaScript, hc_map_pattern, MAP_LIGHTER, offset, text);
		buildHighlight(jtaScript, num_pattern, NUM_LIGHTER, offset, text);//要置于字符串之前，因为字符串中可能含有数字
		buildHighlight(jtaScript, keywords_pattern, KEYWORDS_LIGHTER, offset, text);
		buildHighlight(jtaScript, var_pattern, VAR_LIGHTER, offset, text);
		buildHighlight(jtaScript, str_pattern, STR_LIGHTER, offset, text);//?<!\\\"
		buildHighlight(jtaScript, rem_pattern, REM_LIGHTER, offset, text);//字符串中含有#{}，所以要置于STR_LIGHTER之前
	}
	
	private final void buildHighlight(final JTextPane jta, final Pattern pattern, final SimpleAttributeSet attributes, final int offset, final String text) {
		final StyledDocument document = (StyledDocument)jta.getDocument();
		final Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			final int start = matcher.start() + offset;
			final int end = matcher.end() + offset;
			document.setCharacterAttributes(start, end - start, attributes, false);
		}
	}

	public void undo() {
		synchronized (modifyAndColorAll) {
			if (undoManager.canUndo()){
				undoManager.undo();
				undoInProcess = false;
				jtaScript.setText(jtaScript.getText());
				try{
					jtaScript.setCaretPosition(undoManager.getPostion());
				}catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
				App.invokeLaterUI(modifyAndColorAll);
			}
		}
	}

	public void redo() {
		synchronized (modifyAndColorAll) {
			if (undoManager.canRedo()){
				undoManager.redo();
				undoInProcess = false;
				jtaScript.setText(jtaScript.getText());
				try{
					jtaScript.setCaretPosition(undoManager.getPostion());
				}catch (final Exception e) {
				}
				App.invokeLaterUI(modifyAndColorAll);
			}
		}
	}

	private final void actionOnEnterKey(final Document doc) throws Exception{
		position = jtaScript.getCaretPosition();
		int line = getLineOfOffset(doc, position);
		doc.insertString(position++, "\n", null);
		
		synchronized (modifyAndColorAll) {
			//复制上行的缩进到新行中
			boolean inputSelfBackEnd = false;
			newline = true;
			try {
				final int positionLine = line;
				while(line > 0){
					final int startIdx = getLineStartOffset(doc, line);

					//获得缩进串
					final String lineStr = doc.getText(startIdx, position - startIdx - 1);//-1去掉\n
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
					if(trim_str.length() == 0 || trim_str.startsWith("#")){
						//复制上行的缩进
						doc.insertString(position, String.valueOf(chars, 0, charIdxRemovedTab), null);
						jtaScript.setCaretPosition(position + charIdxRemovedTab);
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
							final int startUpRowIdx = getLineStartOffset(doc, positionLine - 2);
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
									if(isELSIFIndentKeyWords(upRowChars, charIdxUpRowRemovedTab, elsifChar)){
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
							if(trim_str.startsWith(WithEndIndentation[j], 0)){
								chars[charNewIdxRemovedTab++] = '\t';
								withEndInd = 1;
								break;
							}
						}
					}
					if(withEndInd == 0){
						final int size = doIndentation.length;
						for (int j = 0; j < size; j++) {
							if(trim_str.endsWith(doIndentation[j])){
								chars[charNewIdxRemovedTab++] = '\t';
								withEndInd = 1;
								break;
							}
						}
					}
					if(withEndInd == 0){
						withEndInd = WithEndIndentationKuoHao.matcher(trim_str).find()?2:0;
						if(withEndInd > 0){
							chars[charNewIdxRemovedTab++] = '\t';
						}
					}
					boolean isNextIndentAlready = false;
					if(withEndInd > 0){
						//检查下行是否已缩进，
						try{
							final String nextLine = doc.getText(position + 1, getLineEndOffset(doc, positionLine + 2) - (position + 1));
							
							final char[] nextLineChars = nextLine.toCharArray();
							
							int charIdxNextRemovedTab = 0;
							for (; charIdxNextRemovedTab < nextLineChars.length; charIdxNextRemovedTab++) {
								if(nextLineChars[charIdxNextRemovedTab] == ' ' || nextLineChars[charIdxNextRemovedTab] == '\t'){
									
								}else{
									break;
								}
							}
							final boolean isElse = isELSIFIndentKeyWords(nextLineChars, charIdxNextRemovedTab, elsifChar)
									|| isELSIFIndentKeyWords(nextLineChars, charIdxNextRemovedTab, elseChar);
							if((charIdxNextRemovedTab + (isElse?1:0)) > charIdxRemovedTab){
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
					if(charNewIdxRemovedTab != 0 || withEndInd > 0){
						int newPosition;
						doc.insertString(position, String.valueOf(chars, 0, charNewIdxRemovedTab), null);
						position += charNewIdxRemovedTab;
						if(nextRowIndent && (inputSelfBackEnd == false)){
							doc.insertString(position, "\t", null);
							position++;
						}
						newPosition = position;
						if(withEndInd > 0 && (isNextIndentAlready == false) && (nextRowIndent == false)){
							doc.insertString(position++, "\n", null);
							doc.insertString(position, String.valueOf(chars, 0, charNewIdxRemovedTab - 1), null);//下一行，减少一个缩位
							position+= (charNewIdxRemovedTab - 1);
							if(withEndInd == 1){
								doc.insertString(position, "end", null);
								initBlock("end", position);
							}else{
								doc.insertString(position, "}", null);
								initBlock("}", position);
							}
						}
						jtaScript.setCaretPosition(newPosition);
						break;
					}else{
						//取上一行
//									line--;
						break;
					}
					}catch (final Throwable e) {
						ExceptionReporter.printStackTrace(e);
					}finally{
						if(inputSelfBackEnd && charNewIdxRemovedTab > 0){
							doc.remove(startIdx, 1);//end之前的字符去掉一个缩位
						}
					}
				}
			} catch (final Throwable e) {
				if(e instanceof BadLocationException){
				}else{
					ExceptionReporter.printStackTrace(e);
				}
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyAndColorAll.run();
				
				designer.codeHelper.updateScriptASTNode(ScriptEditPanel.this, jtaScript.getText(), isModifySourceForRebuildAST);
				isModifySourceForRebuildAST = false;
			}
		});
	}

	public static final void consumeEvent(final KeyEvent e) {
		e.setKeyChar('\0');
		e.consume();
	}

	private final Runnable doTestRunnable =  new Runnable(){
		@Override
		public void run(){
			doTest(false, false);
		}
	};

	public static void refreshCurrLineAfterKey(final int line, final Document doc, final JTextPane jtaScript, final ScriptEditPanel sep) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				try{
				final int lineStartIdx = getLineStartOffset(doc, line);
				final int lineEndIdx = getLineEndOffset(doc, line);
				final String text = doc.getText(lineStartIdx, lineEndIdx - lineStartIdx);
				sep.initBlock(text, lineStartIdx);
				sep.notifyModified();
				final String scripts = jtaScript.getText();
				sep.updateScript(scripts);	
				}catch (final Exception e) {
				}
			}
		});
	}

	public static SimpleAttributeSet build(final Color c, final boolean bold){
		 final SimpleAttributeSet attributes = new SimpleAttributeSet();
		 StyleConstants.setForeground(attributes, c);
		 StyleConstants.setBold(attributes, bold);
//		 StyleConstants.setFontSize(attributes, fontSize);
//		 StyleConstants.setFontFamily(attrSet, "黑体");
		 return attributes;
	}

	public static synchronized void rebuildJRubyEngine() {
		terminateJRubyEngine();
		runTestEngine = new HCJRubyEngine(StoreDirManager.RUN_TEST_DIR.getAbsolutePath(), ResourceUtil.buildProjClassLoader(StoreDirManager.RUN_TEST_DIR, "hc.testDir"), true);
	}

	public static synchronized void terminateJRubyEngine() {
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
	public LineNumber(final JComponent component) {
		if (component == null) {
			setFont(DEFAULT_FONT);
		} else {
			setFont(component.getFont());
		}
		setPreferredSize(99);
	}

	public void setPreferredSize(final int row) {
		final int width = fontMetrics.stringWidth(String.valueOf(row));
		if (currentRowWidth < width) {
			currentRowWidth = width;
			setPreferredSize(new Dimension(width, HEIGHT));
		}
	}

	@Override
	public void setFont(final Font font) {
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

	public void setLineHeight(final int lineHeight) {
		if (lineHeight > 0)
			this.lineHeight = lineHeight;
	}

	public int getStartOffset() {
		return 2;
	}

	@Override
	public void paintComponent(final Graphics g) {
		final int lineHeight = getLineHeight();
		final int startOffset = getStartOffset();
		final Rectangle drawHere = g.getClipBounds();
		g.setColor(getBackground());//使用缺省背景色
//		g.setColor(Color.YELLOW);
		g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);
		// Determine the number of lines to draw in the foreground.
		g.setColor(getForeground());
		final int startLineNumber = (drawHere.y / lineHeight) + 1;
		final int endLineNumber = startLineNumber + (drawHere.height / lineHeight);
		int start = (drawHere.y / lineHeight) * lineHeight + lineHeight
				- startOffset;
		// System.out.println( startLineNumber + " : " + endLineNumber + " : " +
		// start );
		for (int i = startLineNumber; i <= endLineNumber; i++) {
			final String lineNumber = String.valueOf(i);
			final int width = fontMetrics.stringWidth(lineNumber);
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
	
	PositionUndoManager(final JTextPane jta){
		super();
		jtaScript = jta;
	}
	
	@Override
	public synchronized void setLimit(final int l) {
		post = new int[l];
		super.setLimit(l);
	}
	
	public int getPostion(){
		return post[currIdx];
	}
	
	@Override
	public synchronized boolean addEdit(final UndoableEdit anEdit) {
		if(skipNext == false){
			recordPosition(false);
		}
		return super.addEdit(anEdit);
	}

	public void recordPosition(final boolean skipNext) {
		try{
			post[currIdx++] = jtaScript.getCaretPosition() + (skipNext?0:-1);
			if(currIdx == post.length){
				currIdx = 0;
			}
			this.skipNext = skipNext;
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	boolean skipNext = false;
	
	@Override
	public synchronized void undo() throws CannotUndoException {
		currIdx--;
		if(currIdx < 0){
			currIdx = post.length - 1;
		}
		super.undo();
	}
	
	@Override
	public synchronized void redo() throws CannotUndoException {
		currIdx++;
		if(currIdx == post.length){
			currIdx = 0;
		}
		super.redo();
	}
}
