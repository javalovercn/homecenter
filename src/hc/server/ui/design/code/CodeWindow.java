package hc.server.ui.design.code;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.RepeatManager;
import hc.server.DefaultManager;
import hc.server.ui.design.hpj.HCTextPane;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.util.ClassUtil;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class CodeWindow {
	public static final int MAX_HEIGHT = scaleWindowSizeByFontSize(230);
	public static final int MAX_WIDTH = limitThreeOf(scaleWindowSizeByFontSize(500));

	private static final int scaleWindowSizeByFontSize(final int w_h) {
		try {
			return w_h * Integer.parseInt(DefaultManager.getDesignerDocFontSize()) / DefaultManager.DEFAULT_DOC_FONT_SIZE;
		} catch (final Throwable e) {
			return w_h;
		}
	}

	public final void setMouseOverAutoTipLoc(final int x, final int y, final int fontHeight) {
		docHelper.isForMouseOverTip = true;
		docHelper.mouseOverX = x;
		docHelper.mouseOverY = y;
		docHelper.mouseOverFontHeight = fontHeight;
	}

	private static final int limitThreeOf(final int width) {
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int threeOf = screenSize.width / 3;
		if (width > threeOf) {
			return threeOf;
		} else {
			return width;
		}
	}

	private int loc_x, loc_y, fontHeight;
	private final JList codeList = new JList();// JRE 1.6 not gerneric
	Class codeBelongClass;
	private final ArrayList<CodeItem> classData = new ArrayList<CodeItem>();
	private final JFrame classFrame = new JFrame();
	private final DocLayoutLimit layoutLimit = new DocLayoutLimit();
	private final JScrollPane scrollPanel = new JScrollPane(codeList);
	private final DocTipTimer autoDocPopTip = new DocTipTimer("", 350, false);
	public final DocHelper docHelper;
	public final CodeInvokeCounter codeInvokeCounter = new CodeInvokeCounter();
	public final CSSHelper cssHelper = new CSSHelper();
	private final CodeHelper codeHelper;

	final Runnable refilterRunnable = new Runnable() {
		@Override
		public void run() {
			refilter();
		}
	};

	final Runnable repainRunnable = new Runnable() {
		@Override
		public void run() {
			classFrame.setLocation(loc_x, loc_y);
			if (codeList.getModel().getSize() > 0) {
				codeList.setSelectedIndex(0);
			}
		}
	};

	public final boolean isVisible() {
		return classFrame.isVisible() || docHelper.isShowing();
	}

	public final void keyPressed(final KeyEvent e) {
		if (classFrame.isVisible() == false) {// 仅doc时，不显示codeList
			hide();
			return;
		}

		final int keyCode = e.getKeyCode();

		if (keyCode == KeyEvent.VK_ESCAPE) {
			hide();
			autoDocPopTip.setEnable(false);
			return;
		}

		synchronized (ScriptEditPanel.scriptEventLock) {
			if (classFrame.isVisible()) {
				if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LEFT
						|| keyCode == KeyEvent.VK_RIGHT) {
					if (keyCode == KeyEvent.VK_UP) {
						if (codeList.getSelectedIndex() == 0) {
							SwingUtilities.invokeLater(new Runnable() {// App.invokeLaterUI不正常
								@Override
								public void run() {
									codeList.setSelectedIndex(codeList.getModel().getSize() - 1);
									final JScrollBar verticalScrollBar = scrollPanel.getVerticalScrollBar();
									verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount());
								}
							});
						} else {
							dispatchEvent(e, keyCode);
						}
					} else if (keyCode == KeyEvent.VK_DOWN) {
						if (codeList.getSelectedIndex() == codeList.getModel().getSize() - 1) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									codeList.setSelectedIndex(0);
									scrollPanel.getVerticalScrollBar().setValue(0);
								}
							});
						} else {
							dispatchEvent(e, keyCode);
						}
					} else if (keyCode == KeyEvent.VK_LEFT) {
						if (preCodeCharsLen > 0) {
							preCodeCharsLen--;
							textPane.setCaretPosition(--movingScriptIdx);
							refill(e);
						}
					} else if (keyCode == KeyEvent.VK_RIGHT) {
						try {
							final char nextChar = textPane.getDocument().getText(movingScriptIdx, 1).toLowerCase().charAt(0);
							if ((nextChar >= 'a' && nextChar <= 'z') || (nextChar >= 'A' && nextChar <= 'Z')
									|| (nextChar >= '0' && nextChar <= '9') || nextChar == '_') {
								preCodeChars[preCodeCharsLen++] = nextChar;
								textPane.setCaretPosition(++movingScriptIdx);
								refill(e);
							}
						} catch (final Exception ex) {
						}
					}
					return;
				}
				if (scriptEditPanel != null) {
					scriptEditPanel.isModifySourceForRebuildAST = true;
				}
				if (keyCode == KeyEvent.VK_ENTER) {
					final int selectedIndex = codeList.getSelectedIndex();
					actionOnItem(selectedIndex);
				} else if (keyCode == KeyEvent.VK_BACK_SPACE) {
					noCodeListManager.reset();
					if (preCodeCharsLen > 0) {
						preCodeCharsLen--;
						preCodeLower = String.valueOf(preCodeChars, 0, preCodeCharsLen);
						SwingUtilities.invokeLater(refilterRunnable);

						back(e);
					} else {
						hide();
						back(e);
					}
				} else {
					keyPressedAfterDot(e);
				}
			}
		}
	}

	private final void back(final KeyEvent e) {
		try {
			document.remove(--movingScriptIdx, 1);
			textPane.setCaretPosition(movingScriptIdx);
			// textPane.updateUI();//会导致重新获得焦点时为posi:0
			textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, movingScriptIdx));
			TabHelper.notifyInputKey(true, e, e.getKeyChar(), 0);
		} catch (final Exception ex) {
		}
	}

	private final void refill(final KeyEvent e) {
		preCodeLower = String.valueOf(preCodeChars, 0, preCodeCharsLen);
		SwingUtilities.invokeLater(refilterRunnable);

		TabHelper.notifyInputKey(true, e, e.getKeyChar(), 0);
	}

	public CodeWindow(final CodeHelper codeHelper) {
		this.codeHelper = codeHelper;
		docHelper = new DocHelper(codeHelper);

		codeList.setCellRenderer(new ListCellRenderer() {// JRE 1.6 not gerneric
			protected final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
			final Color defaultBackground = defaultRenderer.getBackground();

			@Override
			public final Component getListCellRendererComponent(final JList list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (isSelected) {
					renderer.setBackground(Color.LIGHT_GRAY);
				} else {
					renderer.setBackground(defaultBackground);
				}
				final int type = classData.get(index).type;
				if (type == CodeItem.TYPE_RESOURCES || type == CodeItem.TYPE_CSS_VAR || type == CodeItem.TYPE_CSS
						|| type == CodeItem.TYPE_VARIABLE) {
					renderer.setIcon(CodeRes.item5);
				} else if (type == CodeItem.TYPE_IMPORT) {
					renderer.setIcon(CodeRes.item4);
				} else if (type == CodeItem.TYPE_FIELD) {
					renderer.setIcon(CodeRes.item3);
				} else if (type == CodeItem.TYPE_METHOD) {
					renderer.setIcon(CodeRes.item2);
				} else if (type == CodeItem.TYPE_CLASS) {
					renderer.setIcon(CodeRes.item1);
				}
				renderer.setForeground(Color.BLACK);
				renderer.setText((String) value);
				return renderer;
			}
		});

		classFrame.setVisible(false);
		classFrame.setAlwaysOnTop(true);
		classFrame.setFocusableWindowState(false);
		classFrame.setUndecorated(true);

		classFrame.getContentPane().add(scrollPanel);
		classFrame.setPreferredSize(new Dimension(MAX_WIDTH, MAX_HEIGHT));
		classFrame.pack();

		CodeHelper.buildListenerForScroll(scrollPanel, codeHelper);

		codeList.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(final MouseEvent e) {
				codeHelper.flipTipKeepOn();
			}

			@Override
			public void mouseDragged(final MouseEvent e) {
			}
		});

		codeList.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
			}

			@Override
			public void mousePressed(final MouseEvent e) {
			}

			@Override
			public void mouseExited(final MouseEvent e) {
				// System.out.println("========================>mouseExited");
				// codeHelper.flipTipStop();
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
				// System.out.println("========================>mouseEntered");
				// codeHelper.flipTipKeepOn();
			}

			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					final int selectedIndex = codeList.getSelectedIndex();
					actionOnItem(selectedIndex);
				}
			}
		});

		codeList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				final int idx = codeList.getSelectedIndex();
				if (idx >= 0) {
					final CodeItem item = classData.get(idx);
					if (docHelper.acceptType(item.type)) {
						startAutoPopTip(item, null, scriptEditPanel);
					} else {
						docHelper.setInvisible();
					}
				}
			}
		});

	}

	public final void hide() {
		if (L.isInWorkshop) {
			ClassUtil.printCurrentThreadStack("[CodeTip] CodeWindow.hide()");
		}
		noCodeListManager.reset();
		codeHelper.mouseExitHideDocForMouseMovTimer.setEnable(false);
		synchronized (classFrame) {
			if (classFrame.isVisible() || docHelper.isShowing()) {
				isWillOrAlreadyToFront = false;
				synchronized (autoDocPopTip) {
					docHelper.setInvisible();
				}
				if (L.isInWorkshop) {
					LogManager.log("[CodeTip] docHelper setInvisible.");
				}
				classFrame.setVisible(false);
				if (L.isInWorkshop) {
					LogManager.log("[CodeTip] classFrame setVisible(false)");
				}
			}
		}
	}

	public final void release() {
		if (classFrame != null) {
			classFrame.dispose();
			docHelper.release();
			HCTimer.remove(autoDocPopTip);
		}
	}

	/**
	 * 如果没有选中的，则返回null
	 * 
	 * @return
	 */
	public final CodeItem getSelectedLine() {
		final int selected = codeList.getSelectedIndex();
		if (selected == -1) {
			return null;
		} else {
			return classData.get(selected);
		}
	}

	HCTextPane textPane;
	Document document;
	ScriptEditPanel scriptEditPanel;
	private final ArrayList<CodeItem> fullList = new ArrayList<CodeItem>();
	private String preCodeLower;
	private final char[] preCodeChars = new char[2048];
	private int preCodeCharsLen;
	private int movingScriptIdx, oriScriptIdx;
	private final RepeatManager noCodeListManager = new RepeatManager() {
		@Override
		public boolean repeatAction() {
			final boolean isReset = (System.currentTimeMillis() - getLastMS() >= 500);
			if (isReset) {
				hide();
			}
			return isReset;
		}
	};

	private final void refilter() {
		fillPreCode(fullList, classData, preCodeLower);

		codeList.setModel(new AbstractListModel() {// JRE 1.6 not gerneric
			@Override
			public int getSize() {
				return classData.size();
			}

			@Override
			public String getElementAt(final int i) {
				return classData.get(i).codeDisplay;
			}
		});

		if (classData.size() == 0) {
			noCodeListManager.occur();
			codeList.clearSelection();
			docHelper.setInvisible();
		} else {
			codeList.setSelectedIndex(0);
		}

		scrollPanel.getVerticalScrollBar().setValue(0);
		scrollPanel.getHorizontalScrollBar().setValue(0);

	}

	public final void fillPreCode(final ArrayList<CodeItem> src, final ArrayList<CodeItem> target, final String preCodeLower) {
		target.clear();
		final int size = src.size();
		final int preLen = preCodeLower.length();
		for (int i = 0; i < size; i++) {
			final CodeItem codeItem = src.get(i);

			if (preCodeType == CodeHelper.PRE_TYPE_OVERRIDE_METHOD) {
				if (codeItem.isOverrideable() == false) {
					continue;
				}
			}

			// if(CodeHelper.isDisplayOverrideMethodAndDoc == false){
			// if(codeItem.overrideMethodLevel < 0){//过滤子类的同名方法
			// continue;
			// }
			// }
			// L.V = L.WShop ? false : LogManager.log("skip item : " +
			// codeItem.codeLowMatch + ", code : " + codeItem.code + ",
			// fieldOrMethod : " + codeItem.fieldOrMethodOrClassName);

			if (preLen == 0 || (codeItem.isFullPackageAndClassName && codeItem.type == CodeItem.TYPE_CLASS
					&& codeItem.codeLowMatch.indexOf(preCodeLower) >= 0) || codeItem.codeLowMatch.startsWith(preCodeLower)) {
				addItemExcludeOverride(codeItem, target);
			}
		}

		Collections.sort(target);
	}

	public static void fillForAutoTip(final ArrayList<CodeItem> src, final ArrayList<CodeItem> target, final boolean isBeginWith,
			final String fieldOrMethodWithoutGanTanHaoOrWenHao) {
		target.clear();
		final int size = src.size();
		for (int i = 0; i < size; i++) {
			final CodeItem codeItem = src.get(i);
			// if(codeItem.isDefed){//不过滤，比如operate(p1, p2)
			// continue;
			// }
			if (codeItem.fmClass == CodeHelper.IterNodeClass) {// 定义方法不作doc
				continue;
			}
			// if(CodeHelper.isDisplayOverrideMethodAndDoc == false){
			// if(codeItem.overrideMethodLevel < 0){//过滤子类的同名方法
			// continue;
			// }
			// }
			if ((isBeginWith && codeItem.fieldOrMethodOrClassName.startsWith(fieldOrMethodWithoutGanTanHaoOrWenHao, 0))// 考虑ruby.method!
					// |
					// method?
					// |
					// method三型，所以改为startsWith
					|| codeItem.fieldOrMethodOrClassName.equals(fieldOrMethodWithoutGanTanHaoOrWenHao)) {
				addItemExcludeOverride(codeItem, target);
			}
		}
		Collections.sort(target);
	}

	private static void addItemExcludeOverride(final CodeItem codeitem, final ArrayList<CodeItem> target) {
		if (codeitem.type == CodeItem.TYPE_METHOD) {
			final int size = target.size();
			for (int i = 0; i < size; i++) {
				final CodeItem item = target.get(i);
				if (item.isOverrideItem(codeitem)) {
					if (item.overrideMethodLevel > codeitem.overrideMethodLevel) {
						return;
					} else {
						target.remove(i);
						target.add(codeitem);
						return;
					}
				}
			}
		}

		target.add(codeitem);
	}

	final Rectangle rect = new Rectangle(0, 0, 1, 1);

	public boolean isWillOrAlreadyToFront;
	public int preCodeType;

	public final void toFront(final int preCodeType, final Class codeClass, final ScriptEditPanel sep, final HCTextPane eventFromComponent,
			final int x, final int y, final ArrayList<CodeItem> list, final String preCode, final int scriptIdx, final int fontHeight) {
		noCodeListManager.reset();
		this.preCodeType = preCodeType;

		isWillOrAlreadyToFront = true;
		docHelper.isForMouseOverTip = false;

		fullList.clear();
		fullList.addAll(list);
		this.codeBelongClass = codeClass;
		classData.clear();

		this.preCodeLower = preCode.toLowerCase();
		final char[] preCharsLower = this.preCodeLower.toCharArray();
		preCodeCharsLen = preCharsLower.length;
		this.movingScriptIdx = scriptIdx;
		this.oriScriptIdx = scriptIdx - preCodeCharsLen;

		System.arraycopy(preCharsLower, 0, preCodeChars, 0, preCodeCharsLen);

		this.scriptEditPanel = sep;
		textPane = eventFromComponent;
		document = textPane.getDocument();

		loc_x = x;
		loc_y = y;
		this.fontHeight = fontHeight;

		// if(classFrame.isVisible() == false){
		// 由于已处于visible时，强制toFront
		SwingUtilities.invokeLater(toVisibleRunnable);
	}

	final Dimension frameSize = new Dimension();
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

	final Runnable toVisibleRunnable = new Runnable() {

		@Override
		public void run() {
			refilter();

			classFrame.getSize(frameSize);

			int showX = loc_x;
			int showY;

			layoutLimit.isNotDownLayout = false;
			layoutLimit.isNotUpLayout = false;
			boolean isIgnoreLayoutLimit = false;

			if (loc_x + frameSize.width > screenSize.width) {
				showX = loc_x - frameSize.width;// 右边没有空间，左展式
				isIgnoreLayoutLimit = true;
			}
			if (loc_y + fontHeight + frameSize.height > screenSize.height) {
				showY = loc_y - frameSize.height;// 底部没有空间，上展式
				if (isIgnoreLayoutLimit == false) {
					layoutLimit.isNotDownLayout = true;
				}
			} else {
				showY = loc_y + fontHeight;// 下展式
				if (isIgnoreLayoutLimit == false) {
					layoutLimit.isNotUpLayout = true;
				}
			}

			classFrame.setLocation(showX, showY);
			classFrame.setVisible(true);
			if (scriptEditPanel != null) {
				scriptEditPanel.autoCodeTip.setEnable(false);
			}
		}
	};

	char[] shujin;

	public final void actionOnItem(final int selectedIndex) {
		hide();

		if (selectedIndex >= 0) {
			final CodeItem item = classData.get(selectedIndex);
			if (item != null) {
				// SwingUtilities.invokeLater(new Runnable() {
				// @Override
				// public void run() {
				if (item.anonymousClass != null) {
					insertAnonymouseClass(item);
				} else if (item.isInnerClass) {
					insertInnerClass();
				} else {
					insertMethod(item);
				}
				// }
				// });
			}
		}
	}

	private final String removeParameters(final String insertedCode) {
		return insertedCode.substring(0, insertedCode.indexOf("("));
	}

	private final char[] getShuJin(final Document doc, int line) throws BadLocationException {
		// 获得缩进串
		String lineStr;
		while (true) {
			lineStr = ScriptEditPanel.getLineText(doc, line);
			if (lineStr.length() > 0) {
				break;
			}
			line--;
		}
		return lineStr.toCharArray();
	}

	private final void insertInnerClass() {
		try {
			final int charIdxRemovedTab = getShujinTabIdx();

			document.remove(oriScriptIdx, preCodeCharsLen);

			final StringBuilder sb = StringBuilderCacher.getFree();
			sb.append(CodeHelper.JRUBY_NEW);
			sb.append("(BaseClass) {");
			sb.append('\n');

			sb.append(shujin, 0, charIdxRemovedTab);
			sb.append('\t');
			// final int newLocIdx = sb.length();
			sb.append('\n');

			sb.append(shujin, 0, charIdxRemovedTab);
			sb.append("}.new");

			document.insertString(oriScriptIdx, sb.toString(), ScriptEditPanel.DEFAULT_LIGHTER);
			StringBuilderCacher.cycle(sb);

			final int position = oriScriptIdx + CodeHelper.JRUBY_NEW.length() + 1;
			textPane.setSelectionStart(position);
			textPane.setSelectionEnd(position + 9);// BaseClass.length == 9

			TabHelper.setInnerClassTabBlock();
		} catch (final BadLocationException e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	private final void insertAnonymouseClass(final CodeItem item) {
		try {
			final int charIdxRemovedTab = getShujinTabIdx();

			document.remove(oriScriptIdx, preCodeCharsLen);
			final String insertedCode = removeParameters(item.code);

			final StringBuilder sb = StringBuilderCacher.getFree();
			sb.append(insertedCode);
			if (item.anonymousClass == Runnable.class) {
				sb.append(" {");
			} else {
				sb.append(" {|e|");
			}
			sb.append('\n');

			sb.append(shujin, 0, charIdxRemovedTab);
			sb.append('\t');
			final int newLocIdx = sb.length();
			sb.append('\n');

			sb.append(shujin, 0, charIdxRemovedTab);
			sb.append('}');

			document.insertString(oriScriptIdx, sb.toString(), ScriptEditPanel.DEFAULT_LIGHTER);
			StringBuilderCacher.cycle(sb);

			// textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document,
			// oriScriptIdx));
			final int position = oriScriptIdx + newLocIdx;
			textPane.setCaretPosition(position);
		} catch (final BadLocationException e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	private final int getShujinTabIdx() throws BadLocationException {
		final int line = ScriptEditPanel.getLineOfOffset(document, oriScriptIdx);
		shujin = getShuJin(document, line);
		int charIdxRemovedTab = 0;
		for (; charIdxRemovedTab < shujin.length; charIdxRemovedTab++) {
			if (shujin[charIdxRemovedTab] == ' ' || shujin[charIdxRemovedTab] == '\t') {
			} else {
				break;
			}
		}
		return charIdxRemovedTab;
	}

	private final void insertMethod(final CodeItem item) {
		try {
			final int itemType = item.type;
			if (codeInvokeCounter.isRecordableItemType(itemType)) {
				codeInvokeCounter.addOne(item);
			}

			document.remove(oriScriptIdx, preCodeCharsLen);
			final String insertedCode = item.code;
			AttributeSet attSet = null;
			if (item.isCSSProperty || item.isCSSClass) {
				attSet = ScriptEditPanel.STR_LIGHTER;
			}
			document.insertString(oriScriptIdx, insertedCode, attSet);// 注：attSet不为null可减少闪烁，即使后有refreshCurrLineAfterKey
			final int position = oriScriptIdx + insertedCode.length();

			textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, oriScriptIdx));
			TabHelper.notifyInputBlock(insertedCode.length() - preCodeCharsLen);

			final char[] insertedCodeChars = insertedCode.toCharArray();
			final int parameterNum = TabBlock.countParameterNum(insertedCodeChars);
			if (parameterNum > 0) {
				TabHelper.setCurrentTabBlock(oriScriptIdx, insertedCodeChars, parameterNum);
				// setCurrentTabBlock方法内有setCaretPosition，故不用
				// textPane.setCaretPosition(position);
			} else {
				textPane.setCaretPosition(position);
			}
		} catch (final BadLocationException e) {
			// ExceptionReporter.printStackTrace(e);
		}
	}

	private final void dispatchEvent(final KeyEvent e, final int keyCode) {
		if (e.getSource() != codeList) {
			codeList.dispatchEvent(new KeyEvent(codeList, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, e.getKeyChar()));
			// codeList.dispatchEvent(new KeyEvent(codeList, KeyEvent.KEY_TYPED,
			// System.currentTimeMillis(), 0, keyCode, e.getKeyChar()));
		}
	}

	public final void startAutoPopTip(final CodeItem item, final HCTextPane jtaPaneMaybeNull, final ScriptEditPanel scriptEditPanel) {
		if (L.isInWorkshop) {
			ClassUtil.printCurrentThreadStack("---------------------------------startAutoPopTip---------------------------------");
		}
		synchronized (autoDocPopTip) {
			autoDocPopTip.resetTimerCount();
			autoDocPopTip.docHelper = docHelper;
			if (jtaPaneMaybeNull != null) {
				textPane = jtaPaneMaybeNull;
			}
			this.scriptEditPanel = scriptEditPanel;
			autoDocPopTip.classFrame = classFrame;
			autoDocPopTip.item = item;
			autoDocPopTip.fmClass = item.fmClass;
			autoDocPopTip.fieldOrMethodName = item.codeForDoc;// 注意：构造方法已转为new(),而非simpleClassName()
			autoDocPopTip.type = item.type;
			autoDocPopTip.layoutLimit = layoutLimit;
			autoDocPopTip.setEnable(true);
		}
	}

	public final void keyPressedAfterDot(final KeyEvent e) {
		final char keyChar = e.getKeyChar();
		if (keyChar == '￿') {// Shift in Mac
			return;
		}
		preCodeChars[preCodeCharsLen++] = keyChar;
		preCodeLower = String.valueOf(preCodeChars, 0, preCodeCharsLen).toLowerCase();

		SwingUtilities.invokeLater(refilterRunnable);

		try {
			document.insertString(movingScriptIdx++, String.valueOf(keyChar), null);
			textPane.setCaretPosition(movingScriptIdx);
			// textPane.updateUI();//会导致重新获得焦点时为posi:0

			textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, movingScriptIdx));
			TabHelper.notifyInputKey(false, e, keyChar, 0);
		} catch (final Exception ex) {
		}
	}

}

class DocTipTimer extends HCTimer {
	DocHelper docHelper;
	JFrame classFrame;
	CodeItem item;
	String fieldOrMethodName;
	String fmClass;
	int type;
	DocLayoutLimit layoutLimit;

	public DocTipTimer(final String name, final int ms, final boolean enable) {
		super(name, ms, enable);
	}

	@Override
	public final void doBiz() {
		synchronized (this) {// 与hide互斥
			if (isEnable()) {// 重新检查条件，必须的
				if (type == CodeItem.TYPE_CLASS) {
					final Class c = ResourceUtil.loadClass(fieldOrMethodName, L.isInWorkshop);
					if (c != null) {
						CodeHelper.buildForClass(docHelper.codeHelper, c);
					}
				}
				docHelper.popDocTipWindow(item, classFrame, fmClass, fieldOrMethodName, type, layoutLimit);
				setEnable(false);
				docHelper.codeHelper.window.scriptEditPanel.autoCodeTip.setEnable(false);
			}
		}
	}
}
