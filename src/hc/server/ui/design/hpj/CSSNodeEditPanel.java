package hc.server.ui.design.hpj;

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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.tree.MutableTreeNode;

import hc.App;
import hc.core.ContextManager;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.res.ImageSrc;
import hc.server.ui.ClientDesc;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.Designer;
import hc.server.ui.design.code.CodeDeclare;
import hc.server.ui.design.code.CodeHelper;
import hc.server.ui.design.code.CodeWindow;
import hc.server.util.CSSUtil;
import hc.server.util.IDArrayGroup;
import hc.util.ResourceUtil;

public class CSSNodeEditPanel extends NameEditPanel {

	private static final SimpleAttributeSet CLASS_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#088A29"), false);// dark green
	private static final SimpleAttributeSet ITEM_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#0431B4"), false);
	private static final SimpleAttributeSet SPLITTER_LIGHTER = ResourceUtil.buildAttrSet(Color.BLACK, false);
	private static final SimpleAttributeSet VALUE_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#B18904"), false);
	private static final SimpleAttributeSet VARIABLE_LIGHTER = ResourceUtil.buildAttrSet(Color.decode("#DF0101"), false);

	private static final SimpleAttributeSet JUMP_CLASS_DEF_LIGHTER = ScriptEditPanel.buildBackground(new Color(165, 199, 234));

	private static final Pattern class_pattern = Pattern.compile("(\\s*?(.*?)\\s*?)\\{");
	private static final Pattern item_pattern = Pattern.compile("(\\b*?([a-zA-Z_0-9-]+)\\b*?\\s*?):(?!/)");// 排除如url(http://domain/)
	private static final Pattern css_rem_pattern = Pattern.compile("(/\\*(.*?)\\*/)", Pattern.MULTILINE | Pattern.DOTALL);
	private static final Pattern var_pattern = Pattern.compile("(\\$(.*?)\\$)");
	private static final Pattern spliter_pattern = Pattern.compile("([\\{\\};:])");

	private final void initSytleBlock(final String text, final int offset, final boolean isReplace) {
		cssUndoListener.enableSkipAddUndoableEditForColor(true);
		
		try {
			final StyledDocument document = (StyledDocument) cssEditPane.getDocument();
			document.setCharacterAttributes(offset, text.length(), VALUE_LIGHTER, true);
	
			buildSytleHighlight(cssEditPane, class_pattern, CLASS_LIGHTER, offset, text, isReplace);
			buildSytleHighlight(cssEditPane, item_pattern, ITEM_LIGHTER, offset, text, isReplace);// 要置于字符串之前，因为字符串中可能含有数字
			buildSytleHighlight(cssEditPane, var_pattern, VARIABLE_LIGHTER, offset, text, isReplace);
			buildSytleHighlightForSpliter(cssEditPane, spliter_pattern, SPLITTER_LIGHTER, offset, text, isReplace);
			
			buildSytleHighlight(cssEditPane, css_rem_pattern, ScriptEditPanel.REM_LIGHTER, offset, text, true);//要置于最后
		}finally {
			cssUndoListener.enableSkipAddUndoableEditForColor(false);
		}
	}

	private static final void buildSytleHighlightForSpliter(final JTextPane pane, final Pattern pattern,
			final SimpleAttributeSet attributes, final int offset, final String text, final boolean isReplace) {
		final StyledDocument document = (StyledDocument) pane.getDocument();
		final Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			final int start = matcher.start(1) + offset;
			final int end = matcher.end(1) + offset;

			if (getChar(document, start) == ':') {
				if (getChar(document, start + 1) == '/') {// 排除如url(http://domain/)
					continue;
				}
			}
			document.setCharacterAttributes(start, end - start, attributes, isReplace);
		}
	}

	private static char getChar(final Document doc, final int idx) {
		try {
			final String chars = doc.getText(idx, 1);
			return chars.charAt(0);
		} catch (final Throwable e) {
		}
		return 0;
	}

	private static final void buildSytleHighlight(final JTextPane pane, final Pattern pattern, final SimpleAttributeSet attributes,
			final int offset, final String text, final boolean isReplace) {
		final StyledDocument document = (StyledDocument) pane.getDocument();
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
		}
	};

	private final void colorStyle(final boolean isFromInit) {
		SwingUtilities.invokeLater(colorRunnable);
		
		if(isFromInit) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					cssEditPane.getDocument().addUndoableEditListener(cssUndoListener);//注意：必须处于colorRunnable之后
				}
			});
		}
	}

	private final Runnable buildCommentUncommentAction() {
		return new Runnable() {
			@Override
			public void run() {
				cssUndoListener.setSignificant();
				if (CSSUtil.comment(cssEditPane)) {
					colorRunnable.run();
					cssEditPane.requestFocus();
				}
			}
		};
	}

	private final JPanel cssPanel = new JPanel();
	private final JLabel tipCssLabel = new JLabel("<html>"
			+ "1. all above will be loaded automatically to all HTMLMlet/Dialog(s) of current project by server.<BR>"
			+ "2. to load special styles for HTMLMlet/Dialog, please invoke <STRONG>loadCSS</STRONG> method.<BR>"
			+ "3. for CSS (2.2) properties or variables of client, please press shortcut keys for word completion.<BR>" 
			+ "4. if there is a <code>url()</code> in CSS, it is required to add domain of it to socket/connect permission or disable limit socket/connect."
			+ "</html>");

	HCUndoableEditListener cssUndoListener;
	HCUndoManager cssUndoManager;
	final HCTextPane cssEditPane = new HCTextPane(new CSSSelectWordAction()) {
		@Override
		public void paste() {
			try {
				cssUndoListener.setSignificant();
				super.paste();
				colorStyle(false);
			} catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}

		@Override
		public void cut() {
			cssUndoListener.setSignificant();
			super.cut();
			colorStyle(false);
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
					} catch (final BadLocationException e) {
					}
				}
			});
		}
		
		@Override
		public void notifyUpdateScript() {
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

	@Override
	public final void notifyLostWindowFocus() {
		designer.codeHelper.window.hide();
	}
	
	private final void actionOnEnterKey() throws Exception {
		final StringBuilder sb = new StringBuilder(24);
		boolean isDoneEnter = false;
		boolean isMovSelfBack = false;

		final int InsertPosition = cssEditPane.getCaretPosition();
		int line = ScriptEditPanel.getLineOfOffset(cssDocument, InsertPosition);
		sb.append("\n");

		// 复制上行的缩进到新行中
		boolean inputSelfBackEnd = false;
		try {
			final int positionLine = line;
			while (line >= 0) {// 第一行等于0
				final int startIdx = ScriptEditPanel.getLineStartOffset(cssDocument, line);

				// 获得缩进串
				final String lineStr = cssDocument.getText(startIdx, InsertPosition - startIdx);// -1去掉\n
				final char[] chars = lineStr.toCharArray();

				if (chars.length == 0) {
					line--;
					continue;
				}
				int charIdxRemovedTab = 0;
				for (; charIdxRemovedTab < chars.length; charIdxRemovedTab++) {
					if (chars[charIdxRemovedTab] == ' ' || chars[charIdxRemovedTab] == '\t') {

					} else {
						break;
					}
				}
				final String trim_str = new String(chars, charIdxRemovedTab, chars.length - charIdxRemovedTab);
				inputSelfBackEnd = false;
				boolean nextRowIndent = false;
				for (int k = chars.length - 1; k >= 0; k--) {
					final char c = chars[k];
					if ('}' == c) {
						// 查找前面是否有闭合
						boolean isFindStarter = false;
						for (int m = k - 1; m >= 0; m--) {
							if ('{' == chars[m]) {
								isFindStarter = true;
								break;
							}
						}
						inputSelfBackEnd = !isFindStarter;
						break;
					}
				}
				if (inputSelfBackEnd) {
					// nextRowIndent = inputSelfBackEnd;//yyh
					// 检查上行是否当前行已缩进。即在已有代码elsif xxx后进行回车，当前是否需要缩进
					final int startUpRowIdx = ScriptEditPanel.getLineStartOffset(cssDocument, positionLine - 1);
					try {
						final String upRowStr = cssDocument.getText(startUpRowIdx, startIdx - 1 - startUpRowIdx);
						final char[] upRowChars = upRowStr.toCharArray();
						int charIdxUpRowRemovedTab = 0;
						for (; charIdxUpRowRemovedTab < upRowChars.length; charIdxUpRowRemovedTab++) {
							if (upRowChars[charIdxUpRowRemovedTab] == ' ' || upRowChars[charIdxUpRowRemovedTab] == '\t') {

							} else {
								break;
							}
						}
						if (charIdxUpRowRemovedTab > charIdxRemovedTab) {
							inputSelfBackEnd = false;// 取消自缩进
						} else if (charIdxUpRowRemovedTab == charIdxRemovedTab) {
							// if(isELSIFIndentKeyWords(upRowChars,
							// charIdxUpRowRemovedTab, elsifChar)){
							// inputSelfBackEnd = false;//取消自缩进
							// }
						}
					} catch (final Exception ex) {
					}
				}
				int charNewIdxRemovedTab = charIdxRemovedTab;
				boolean withEndInd = false;
				{
					final int starter = trim_str.indexOf('{', 0);
					if (starter >= 0 && trim_str.indexOf('}', 0) < 0) {
						nextRowIndent = true;
						chars[charNewIdxRemovedTab++] = '\t';
						withEndInd = true;
					}
				}
				boolean isNextIndentAlready = false;
				boolean hasNextHuaKuoHao = false;
				if (withEndInd) {
					// 检查下行是否已缩进，
					try {
						final String nextLine = cssDocument.getText(InsertPosition + 1,
								ScriptEditPanel.getLineEndOffset(cssDocument, positionLine + 2) - (InsertPosition + 1));
						if (nextLine.startsWith("}", 0)) {
							hasNextHuaKuoHao = true;
						}
						final char[] nextLineChars = nextLine.toCharArray();

						int charIdxNextRemovedTab = 0;
						for (; charIdxNextRemovedTab < nextLineChars.length; charIdxNextRemovedTab++) {
							if (nextLineChars[charIdxNextRemovedTab] == ' ' || nextLineChars[charIdxNextRemovedTab] == '\t') {

							} else {
								break;
							}
						}
						if (charIdxNextRemovedTab > charIdxRemovedTab) {
							isNextIndentAlready = true;
						}
					} catch (final Exception e) {
						if (e instanceof BadLocationException) {
						} else {
							ExceptionReporter.printStackTrace(e);
						}
					}
				}

				try {
					final int newPosition;
					// if(charNewIdxRemovedTab > 0 && (inputSelfBackEnd ==
					// false)){//不能}自缩进
					// sb.append(String.valueOf(chars, 0,
					// charNewIdxRemovedTab));
					// }
					if (nextRowIndent && (inputSelfBackEnd == false)) {
						sb.append("\t");
					}
					if (charNewIdxRemovedTab != 0 || withEndInd) {
						newPosition = InsertPosition + sb.length();
						if (hasNextHuaKuoHao == false && withEndInd && (isNextIndentAlready == false)) {// &&
																										// (nextRowIndent
																										// == false)
							sb.append("\n");
							sb.append(String.valueOf(chars, 0, charNewIdxRemovedTab - 1));// 下一行，减少一个缩位

							try {
								final String end = cssDocument.getText(InsertPosition, 1);
								if (end.startsWith("}", 0)) {
									hasNextHuaKuoHao = true;
								}
							} catch (final Throwable e) {
							}

							if (hasNextHuaKuoHao == false) {
								final String strEnd = "}";
								sb.append(strEnd);
							}
							final String txt = sb.toString();
							appendEnterText(txt, newPosition);
							isDoneEnter = true;
							colorStyle(false);
						} else if (inputSelfBackEnd == false) {
							// 复制上行的缩进
							sb.append(String.valueOf(chars, 0, charIdxRemovedTab));
							final String txt = sb.toString();
							final int newPos = InsertPosition + sb.length();
							//// cssDocument.insertString(InsertPosition, txt,
							//// null);
							// final int newPos = InsertPosition + sb.length();
							appendEnterText(txt, newPos);
							isDoneEnter = true;
							return;
						}
						break;
					} else {
						break;
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				} finally {
					if (inputSelfBackEnd && charNewIdxRemovedTab > 0) {
						cssDocument.remove(startIdx, 1);// }之前的字符去掉一个缩位
						isMovSelfBack = true;
					}
				}
			}
			if (isDoneEnter == false) {
				final String txt = (isMovSelfBack ? "\n" : sb.toString());
				// try {
				// cssDocument.insertString(InsertPosition, txt, null);
				// } catch (final BadLocationException e) {
				// }
				final int newPos = InsertPosition + txt.length() + (isMovSelfBack ? -1 : 0);
				appendEnterText(txt, newPos);
			}
		} catch (final Throwable e) {
			if (e instanceof BadLocationException) {
			} else {
				ExceptionReporter.printStackTrace(e);
			}
		}
	}

	final JButton formatBtn = new JButton(ScriptEditPanel.FORMAT_BUTTON_TEXT);
	final JButton commentBtn = ScriptEditPanel.buildCommentUncommentButton(buildCommentUncommentAction());

	public CSSNodeEditPanel() {
		super();

		// Disable Word wrap in JTextPane
		final JPanel NoWrapPanel = new JPanel(new BorderLayout());
		NoWrapPanel.add(cssEditPane, BorderLayout.CENTER);
		cssScrollPane = new JScrollPane(NoWrapPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final LineNumber lineNumber = new LineNumber(cssEditPane);
		cssScrollPane.setRowHeaderView(lineNumber);
		cssScrollPane.getVerticalScrollBar().setUnitIncrement(ServerUIUtil.SCROLLPANE_VERTICAL_UNIT_PIXEL);

		cssEditPane.addCaretListener(new CaretListener() {
			@Override
			public void caretUpdate(final CaretEvent e) {
			}
		});
		
		jumpRunnable = new EditorJumpRunnable() {
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

					try {
						for (int i = 0; i < 5; i++) {
							cssEditPane.getStyledDocument().setCharacterAttributes(getOffSet(), getLen(), JUMP_CLASS_DEF_LIGHTER, false);
							Thread.sleep(500);
							cssEditPane.getStyledDocument().setCharacterAttributes(getOffSet(), getLen(), defaultBackground, false);
							Thread.sleep(500);
						}
					} catch (final Exception e) {
					}
				}
			};

			@Override
			public void run() {
				ContextManager.getThreadPool().run(lightRun);
			}

			private final int getOffSet() {
				return offset;
			}

			private final int getLen() {
				return len;
			}
		};

		formatBtn.setToolTipText("<html>(" + ResourceUtil.getAbstractCtrlKeyText() + " + F)<BR><BR>format the CSS.</html>");
		formatBtn.setIcon(Designer.loadImg(ImageSrc.FORMAT_16_PNG));

		final Action formatAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						cssUndoListener.setSignificant();
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
		formatBtn.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) formatAction.getValue(Action.ACCELERATOR_KEY),
				"formatCSSAction");

		cssDocument = cssEditPane.getDocument();

		tipCssLabel.setBorder(new TitledBorder("Description :"));
		cssPanel.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));

		if (IDArrayGroup.checkAndAdd(IDArrayGroup.MSG_CSS_NOTE)) {
			cssPanel.add(tipCssLabel, BorderLayout.NORTH);
		} else {
			cssPanel.add(tipCssLabel, BorderLayout.SOUTH);
		}

		{
			final JPanel editToolPane = new JPanel(new BorderLayout());

			final JPanel tmpPanel = new JPanel(new BorderLayout());
			tmpPanel.setBorder(new TitledBorder("Styles Edit Area :"));
			tmpPanel.add(cssScrollPane, BorderLayout.CENTER);

			final JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
			// toolPanel.setBorder(new TitledBorder(""));
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
						actionOnEnterKey();
					} catch (final Exception e1) {
					}
				}
			};
			final KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
			cssEditPane.getInputMap().put(enterKey, enterAction);
		}

		// 以下代码设置Tab跳过指定的空格
		{
			final FontMetrics fm = cssEditPane.getFontMetrics(cssEditPane.getFont());
			cssEditPane.setForeground(cssEditPane.getBackground());
			final int cw = fm.stringWidth("    ");
			final float f = cw;
			final TabStop[] tabs = new TabStop[10]; // this sucks
			for (int i = 0; i < tabs.length; i++) {
				tabs[i] = new TabStop(f * (i + 1), TabStop.ALIGN_LEFT, TabStop.LEAD_NONE);
			}
			final TabSet tabset = new TabSet(tabs);
			final StyleContext sc = StyleContext.getDefaultStyleContext();
			final AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.TabSet, tabset);
			cssEditPane.setParagraphAttributes(aset, false);
		}

		cssEditPane.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(final MouseEvent e) {
			}

			@Override
			public void mousePressed(final MouseEvent e) {
			}

			@Override
			public void mouseExited(final MouseEvent e) {
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
			}

			@Override
			public void mouseClicked(final MouseEvent e) {
				designer.codeHelper.window.hide();
			}
		});

		cssEditPane.addKeyListener(new KeyListener() {
			final boolean isMacOS = ResourceUtil.isMacOSX();
			final int fontHeight = cssEditPane.getFontMetrics(cssEditPane.getFont()).getHeight();

			@Override
			public void keyTyped(final KeyEvent event) {
				if (isEventConsumed) {
					ScriptEditPanel.consumeEvent(event);// otherwise display shortcut key.
					return;
				}

				final char inputChar = event.getKeyChar();
				if (HCUndoManager.isCtrlKey(inputChar)) {
				}else {
					//注意：此段代码不能与isCtrlKey合并，
//					cssUndoListener.setSignificant();//比如在selection时，键入'e'，注意：必须在此处进行setSignificant和removeSelection
					TabHelper.removeSelection(null, cssEditPane, selectionLen);
				}
				
				if (inputChar == KeyEvent.VK_ENTER) {
					// 具体转由actionOnEnterKey()
					ScriptEditPanel.consumeEvent(event);//注意：移去此段代码，会导致undo不正常
					return;
				}
				
				final int modifiers = event.getModifiers();

				if (codeHelper == null) {
					codeHelper = designer.codeHelper;
				}

				if (isMacOS && (inputChar != 0 && inputChar == codeHelper.wordCompletionChar
						&& ((codeHelper.wordCompletionModifyCode == KeyEvent.VK_ALT && modifiers == 0)
								|| (codeHelper.wordCompletionModifyMaskCode == modifiers)))) {// 注意：请同步从ScriptEditPanel
					try {
						final int caretPosition = cssEditPane.getCaretPosition();
						final Rectangle caretRect = cssEditPane.modelToView(caretPosition);
						final Point caretPointer = new Point(caretRect.x, caretRect.y);
						codeHelper.inputForCSSInCSSEditor(CodeDeclare.buildNewInstance(), cssEditPane, cssDocument, caretPointer, fontHeight, caretPosition);
					} catch (final Exception e) {
						if (L.isInWorkshop) {
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
			CodeHelper codeHelper;
			int selectionLen;
			
			@Override
			public void keyPressed(final KeyEvent event) {
				final int keycode = event.getKeyCode();
				final int modifiers = event.getModifiers();
				if (codeHelper == null) {
					codeHelper = designer.codeHelper;
				}
				cssUndoListener.setSignificant();
				final int wordCompletionModifyMaskCode = codeHelper.wordCompletionModifyMaskCode;
				// 无输入字符时的触发提示代码
				isEventConsumed = false;
				if (keycode == codeHelper.wordCompletionCode
						&& (modifiers & wordCompletionModifyMaskCode) == wordCompletionModifyMaskCode) {
					// 注意：请同步从ScriptEditPanel
					try {
						final int caretPosition = cssEditPane.getCaretPosition();
						final Rectangle caretRect = cssEditPane.modelToView(caretPosition);
						final Point caretPointer = new Point(caretRect.x, caretRect.y);
						codeHelper.inputForCSSInCSSEditor(CodeDeclare.buildNewInstance(), cssEditPane, cssDocument, caretPointer, fontHeight, caretPosition);
					} catch (final Exception e) {
						if (L.isInWorkshop) {
							ExceptionReporter.printStackTrace(e);
						}
					}
					ScriptEditPanel.consumeEvent(event);
					isEventConsumed = true;
					return;
				}

				if (codeHelper.window.isVisible()) {
					codeHelper.window.keyPressed(event);
					ScriptEditPanel.consumeEvent(event);
					isEventConsumed = true;
					return;
				}

				if (modifiers == KeyEvent.CTRL_MASK) {
					if (keycode == KeyEvent.VK_Z) {
						// ctrl + z
						cssundo();
						return;
					} else if (keycode == KeyEvent.VK_Y) {
						// ctrl + y
						cssredo();
						return;
					}
				} else if (keycode == KeyEvent.VK_Z) {
					if (isMacOS) {
						if (modifiers == KeyEvent.META_MASK) {
							// cmd+z
							cssundo();
							return;
						} else if (modifiers == (KeyEvent.META_MASK | KeyEvent.SHIFT_MASK)) {
							cssredo();
							return;
						}
					}
				}

				if (keycode == KeyEvent.VK_ESCAPE) {
					TabHelper.pushEscKey();
					isEventConsumed = true;
					return;
				}
				
				final int selectionStart = cssEditPane.getSelectionStart();
				final int selectionEnd = cssEditPane.getSelectionEnd();
				if (selectionEnd > selectionStart) {
					selectionLen = selectionEnd - selectionStart;
				} else {
					selectionLen = 0;
				}
				
				if (HCUndoManager.isCtrlKey(keycode)) {
//					ScriptEditPanel.consumeEvent(event);//注意：此处不能consumeEvent
//					isEventConsumed = true;
					final boolean isRemSelection = TabHelper.removeSelection(null, cssEditPane, selectionLen);
					
					if(isRemSelection && HCUndoManager.isDelOrBackspaceKey(keycode)) {
						ScriptEditPanel.consumeEvent(event);//注意：DelOrBackspaceKey一定要在此处consumeEvent
						isEventConsumed = true;
					}
					return;//不能return，因为后续需要removeSelection
				}
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
				if (window.isWillOrAlreadyToFront) {
					window.hide();
				}
			}
		});
	}

	private final void cssundo() {
		if (cssUndoManager.canUndo()) {
			cssUndoManager.undo();
			//不需要更新修改到tree节点，
		}
	}

	private final void cssredo() {
		if (cssUndoManager.canRedo()) {
			cssUndoManager.redo();
			//不需要更新修改到tree节点，
		}
	}

	@Override
	public void init(final MutableTreeNode data, final JTree tree) {
		super.init(data, tree);

		if (cssUndoManager != null) {
			cssUndoManager.discardAllEdits();
		}else {
			cssUndoManager = new HCUndoManager(cssEditPane);
			cssUndoManager.setLimit(HCUndoManager.MAX_LIMIT);
			cssUndoListener = new HCUndoableEditListener(this, cssUndoManager, cssEditPane);
		}

		cssEditPane.getDocument().removeUndoableEditListener(cssUndoListener);
		
		String text = designer.getProjCSS();
		if (text.indexOf('\r') >= 0) {
			text = text.replace("\r", "");
		}
		cssEditPane.setText(text);
		colorStyle(true);
	}

	@Override
	public void loadAfterShow(final Runnable run) {
		cssUndoManager.discardAllEdits();
		cssEditPane.setCaretPosition(0);
		cssEditPane.requestFocus();

		super.loadAfterShow(run);
	}

	@Override
	public void notifyLostEditPanelFocus() {
		super.notifyLostEditPanelFocus();

		final String sameFullName = designer.updateCssClassOfProjectLevel(cssDocument);
		if (sameFullName != null) {
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					App.showMessageDialog(designer, sameFullName, ResourceUtil.getErrorI18N(), JOptionPane.ERROR_MESSAGE,
							App.getSysIcon(App.SYS_ERROR_ICON));
				}
			});
		}
	}

	@Override
	public JComponent getMainPanel() {
		return cssPanel;
	}

}