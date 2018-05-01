package hc.server.ui.design.code;

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
import java.util.HashSet;

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

import hc.App;
import hc.core.DelayWatcher;
import hc.core.GlobalConditionWatcher;
import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.BooleanValue;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.DefaultManager;
import hc.server.ui.design.engine.RubyExector;
import hc.server.ui.design.hpj.HCTextPane;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.server.ui.design.hpj.ScriptModelManager;
import hc.util.ClassUtil;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

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
	public final JFrame classFrame = new JFrame();
	public final DocLayoutLimit layoutLimit = new DocLayoutLimit();
	private final JScrollPane scrollPanel = new JScrollPane(codeList);
	private final int delayDocTipMS = 300;
	public final DocTipTimer autoDocPopTip = new DocTipTimer("autoDocPopTip", delayDocTipMS, false);
	private final HCTimer noCodeListDelayCloseDocTipTimer = new HCTimer("NoCodeDelayDismissDocTip", delayDocTipMS + HCTimer.HC_INTERNAL_MS, false) {
		@Override
		public void doBiz() {
			docHelper.setInvisible();//不能关闭CodeList，因为用户可能只是输错方法名
			setEnable(false);
		}
	};

	public final DocHelper docHelper;
	private static final ClassImporter j2seHcClassImporter = ClassImporter.buildJ2SEHCImporter();
	
	public final CodeInvokeCounter codeInvokeCounter = new CodeInvokeCounter();
	public final CSSHelper cssHelper = new CSSHelper();
	final CodeHelper codeHelper;

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
			return;
		}

		synchronized (ScriptEditPanel.scriptEventLock) {
			if (classFrame.isVisible()) {
				if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LEFT
						|| keyCode == KeyEvent.VK_RIGHT) {
					final boolean isEnableLeftRight = false;
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
						if (isEnableLeftRight && preCodeLowerCharsLen > 0) {//停用，因为后面一个字符仍在。比如："@|"时，左移一，变为"|@"
							preCodeLowerCharsLen--;
							textPane.setCaretPosition(--movingScriptIdx);
							refill(e);
						}
					} else if (isEnableLeftRight && keyCode == KeyEvent.VK_RIGHT) {//停用，同上
						try {
							final char nextChar = textPane.getDocument().getText(movingScriptIdx, 1).toLowerCase().charAt(0);
							if ((nextChar >= 'a' && nextChar <= 'z') || (nextChar >= 'A' && nextChar <= 'Z')
									|| (nextChar >= '0' && nextChar <= '9') || nextChar == '_') {
								preCodeLowerChars[preCodeLowerCharsLen++] = nextChar;
								textPane.setCaretPosition(++movingScriptIdx);
								refill(e);
							}
						} catch (final Exception ex) {
						}
					}
					//end VK_LEFT, VK_RIGHT, VK_UP, VK_DOWN
					return;
				}
				if (scriptEditPanel != null) {
					scriptEditPanel.isModifySourceForRebuildAST = true;
				}
				if (keyCode == KeyEvent.VK_ENTER) {
					final int selectedIndex = codeList.getSelectedIndex();
					actionOnItem(selectedIndex);
				} else if (keyCode == KeyEvent.VK_BACK_SPACE) {
					noCodeListDelayCloseDocTipTimer.isEnable();
					if (preCodeLowerCharsLen > 0) {
						preCodeLowerCharsLen--;
						preCodeLower = String.valueOf(preCodeLowerChars, 0, preCodeLowerCharsLen);
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
		preCodeLower = String.valueOf(preCodeLowerChars, 0, preCodeLowerCharsLen);
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
				} else if (type == CodeItem.TYPE_CLASS || type == CodeItem.TYPE_CLASS_IMPORT) {
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
						startAutoPopTip(item, null);
					} else {
						docHelper.setInvisible();
					}
				}
			}
		});

	}

	public final void hide() {
		noCodeListDelayCloseDocTipTimer.setEnable(false);
		codeHelper.mouseExitHideDocForMouseMovTimer.setEnable(false);
		synchronized (classFrame) {
			boolean isCodeWindowVisible, isDocWinVisible = false;
			if ((isCodeWindowVisible = classFrame.isVisible()) || (isDocWinVisible = docHelper.isShowing())) {
				if (L.isInWorkshop) {
					ClassUtil.printCurrentThreadStack("[CodeTip] CodeWindow.hide(), isCodeWindowVisible : " + isCodeWindowVisible + ", isDocWinVisible : " + isDocWinVisible);
				}
				isWillOrAlreadyToFront = false;
				synchronized (autoDocPopTip) {
					docHelper.setInvisible();
					autoDocPopTip.setEnable(false);
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
			noCodeListDelayCloseDocTipTimer.remove();
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
	public ScriptEditPanel scriptEditPanel;
	private final ArrayList<CodeItem> fullList = new ArrayList<CodeItem>();
	private String preCodeLower;
	private final char[] preCodeLowerChars = new char[2048];
	private int preCodeLowerCharsLen;
	private int movingScriptIdx, oriScriptIdx;

	private final void refilter() {
		fillPreCode(fullList, classData);

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
			autoDocPopTip.setEnable(false);
			noCodeListDelayCloseDocTipTimer.setEnable(true);
			codeList.clearSelection();
		} else {
			codeList.setSelectedIndex(0);
			L.V = L.WShop ? false : LogManager.log("[Code] refilter : " + classData.get(0).codeDisplay);
		}

		scrollPanel.getVerticalScrollBar().setValue(0);
		scrollPanel.getHorizontalScrollBar().setValue(0);

	}

	public final void fillPreCode(final ArrayList<CodeItem> src, final ArrayList<CodeItem> target) {
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

			if (preLen == 0 
					|| (codeItem.isFullPackageAndClassName && codeItem.type == CodeItem.TYPE_CLASS && codeItem.matchPreCode(preCodeLower)) 
					|| codeItem.similarity(preCodeLower, preCodeLowerChars, preCodeLowerCharsLen) > 0) {
				addItemExcludeOverride(codeItem, target);
			}
		}
		
		if(preCodeType == CodeHelper.PRE_TYPE_BEFORE_INSTANCE) {
			if(preCodeLowerCharsLen >= ClassImporter.MIN_PRE_LEN) {
				final BooleanValue isImportClassesDone = codeHelper.isImportClassesDone;
				if(isImportClassesDone.value == false) {
					synchronized (isImportClassesDone) {
						if(isImportClassesDone.value == false) {
							try {
								isImportClassesDone.wait();
							} catch (final InterruptedException e) {
							}
						}
					}
				}
				final HashSet<String> importClasses = codeHelper.importClasses;
				j2seHcClassImporter.appendClassImport(this, preCodeLower, preCodeLowerChars, preCodeLowerCharsLen, target, importClasses);
				codeHelper.nodeLibClassesAndResMap.appendClassImport(this, preCodeLower, preCodeLowerChars, preCodeLowerCharsLen, target, importClasses);
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
			if ((isBeginWith && codeItem.fieldOrMethodOrFullClassName.startsWith(fieldOrMethodWithoutGanTanHaoOrWenHao, 0))// 考虑ruby.method!
					// |
					// method?
					// |
					// method三型，所以改为startsWith
					|| codeItem.fieldOrMethodOrFullClassName.equals(fieldOrMethodWithoutGanTanHaoOrWenHao)) {
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

	public final void toFront(final int preCodeType, final Class codeClass, final HCTextPane eventFromComponent,
			final int x, final int y, final ArrayList<CodeItem> list, final String preCode, final int scriptIdx, final int fontHeight) {
		noCodeListDelayCloseDocTipTimer.isEnable();
		this.preCodeType = preCodeType;

		isWillOrAlreadyToFront = true;
		docHelper.isForMouseOverTip = false;

		fullList.clear();
		fullList.addAll(list);
		this.codeBelongClass = codeClass;
		classData.clear();

		this.preCodeLower = preCode.toLowerCase();
		final char[] preCharsLower = this.preCodeLower.toCharArray();
		preCodeLowerCharsLen = preCharsLower.length;
		this.movingScriptIdx = scriptIdx;
		this.oriScriptIdx = scriptIdx - preCodeLowerCharsLen;

		System.arraycopy(preCharsLower, 0, preCodeLowerChars, 0, preCodeLowerCharsLen);

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
				L.V = L.WShop ? false : LogManager.log("[Code] toVisibleRunnable.");
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
				if (item.type == CodeItem.TYPE_CLASS_IMPORT) {
					insertClassImport(item);
				}else if (item.anonymousClass != null) {
					insertAnonymouseClass(item);
				} else if (item.isInnerClass) {
					insertInnerClass();
				} else {
					insertMethod(item);
				}
				textPane.notifyUpdateScript();
				// }
				// });
			}
		}
	}
	
	private final void insertClassImport(final CodeItem item) {
		try {
			final int itemType = item.type;
			if (codeInvokeCounter.isRecordableItemType(itemType)) {
				codeInvokeCounter.addOne(item);
			}

			document.remove(oriScriptIdx, preCodeLowerCharsLen);
			final String insertedCode = item.code;
			document.insertString(oriScriptIdx, insertedCode, ScriptEditPanel.DEFAULT_LIGHTER);// 注：attSet不为null可减少闪烁，即使后有refreshCurrLineAfterKey
			final int position = oriScriptIdx + insertedCode.length();

//			textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, oriScriptIdx));
			TabHelper.notifyInputBlock(insertedCode.length() - preCodeLowerCharsLen);

			final char[] insertedCodeChars = insertedCode.toCharArray();
			final int parameterNum = TabBlock.countParameterNum(insertedCodeChars);
			if (parameterNum > 0) {
				TabHelper.setCurrentTabBlock(oriScriptIdx, insertedCodeChars, parameterNum);
				// setCurrentTabBlock方法内有setCaretPosition，故不用
				// textPane.setCaretPosition(position);
			} else {
				textPane.setCaretPosition(position);
			}
			
			final String fullClassName = item.codeForDoc;
			final HashSet<String> importClasses = codeHelper.importClasses;
			importClasses.add(fullClassName);
			{
				final BooleanValue isImportClassesDone = codeHelper.isImportClassesDone;
				isImportClassesDone.value = false;
				
				GlobalConditionWatcher.addWatcher(new DelayWatcher(DelayWatcher.NO_DELAY) {//由于线程优先级最低，所以NO_DELAY
					@Override
					public void doBiz() {
						loadCodeItem(fullClassName);

						synchronized (isImportClassesDone) {
							isImportClassesDone.value = true;
							isImportClassesDone.notify();
						}
					}

					private final void loadCodeItem(final String claz) {
						final ReturnType rt = CodeHelper.findClassByName(claz, false);
						if(rt != null) {
							codeHelper.initClass(claz, rt.getRawClass());
						}
					}
				});
			}
			
			int firstRequireIdx = -1;
			int afterEncodeUTF8Idx = -1;

			//找到import xx的position
			final String scripts = textPane.getText();
			int insertIdx = 0;
			boolean hasException = false;
			
			try {
				while(true) {
					insertIdx = scripts.indexOf(RubyExector.IMPORT, insertIdx);
					if(insertIdx < 0) {
						break;
					}else {
						if(insertIdx > oriScriptIdx) {
							break;
						}
						if(scripts.charAt(insertIdx - 1) == '\n') {
							insertImportClassCommands(insertIdx, fullClassName);
							return;
						}
					}
				}//end while
				
				//require 'xx.jar' 
				insertIdx = 0;
				int lastRequireIdx = -1;
				while(true) {
					insertIdx = scripts.indexOf(CodeHelper.PRE_REQUIRE, insertIdx);
					if(insertIdx < 0) {
						break;
					}
					if(firstRequireIdx < 0) {
						firstRequireIdx = insertIdx;
					}
					if(scripts.charAt(insertIdx - 1) == '\n' && scripts.charAt(insertIdx + CodeHelper.PRE_REQUIRE.length()) == ' ') {
						if(insertIdx > oriScriptIdx) {
							break;
						}
						lastRequireIdx = insertIdx;
					}
					insertIdx++;
				}
				
				if(lastRequireIdx >= 0) {
					insertIdx = lastRequireIdx;
					while(scripts.charAt(insertIdx++) != '\n') {
					}
					
					document.insertString(insertIdx++, "\n", ScriptEditPanel.DEFAULT_LIGHTER);
					insertImportClassCommands(insertIdx, fullClassName);
					return;
				}
				
				//#encoding:utf-8
				insertIdx = scripts.indexOf(ScriptModelManager.ENCODING_UTF_8, 0);
				if(insertIdx >= 0) {
					insertIdx += ScriptModelManager.ENCODING_UTF_8.length();
					afterEncodeUTF8Idx = insertIdx;
					insertImportClassCommands(insertIdx, fullClassName);
					return;
				}
				
				hasException = true;
				App.showMessageDialog(null, "fail to insert [" + RubyExector.IMPORT + fullClassName + "].", ResourceUtil.getErrorI18N(), 
						App.ERROR_MESSAGE);
			}catch (final Exception e) {
				hasException = true;
				throw e;
			}finally {
				if(hasException == false && item.thirdLibNameMaybeNull != null) {
					//检查是否已require该lib，准备添加"require 'lib.jar'"
					if(RubyExector.hasInsertedRequireLib(scripts, item.thirdLibNameMaybeNull, oriScriptIdx) == false) {
						if(firstRequireIdx > 0) {
							insertRequireLibCommands(firstRequireIdx, item.thirdLibNameMaybeNull);
						}else {
							if(afterEncodeUTF8Idx > 0) {
								insertRequireLibCommands(afterEncodeUTF8Idx, item.thirdLibNameMaybeNull);
							}else {
								afterEncodeUTF8Idx = scripts.indexOf(ScriptModelManager.ENCODING_UTF_8, 0);
								if(afterEncodeUTF8Idx >= 0) {
									afterEncodeUTF8Idx += ScriptModelManager.ENCODING_UTF_8.length();
									insertRequireLibCommands(afterEncodeUTF8Idx, item.thirdLibNameMaybeNull);
								}else {
									App.showMessageDialog(null, "fail to insert [" + CodeHelper.PRE_REQUIRE + " '" + item.thirdLibNameMaybeNull + "'].", ResourceUtil.getErrorI18N(), 
											App.ERROR_MESSAGE);
								}
							}
						}
					}
				}
			}
		} catch (final Exception e) {
			// ExceptionReporter.printStackTrace(e);
		}
	}

	private final void insertRequireLibCommands(int insertIdx, final String libName) throws BadLocationException {
		document.insertString(insertIdx, CodeHelper.PRE_REQUIRE, ScriptEditPanel.KEYWORDS_LIGHTER);
		insertIdx += CodeHelper.PRE_REQUIRE.length();
		document.insertString(insertIdx, " '" + libName + "'\n", ScriptEditPanel.DEFAULT_LIGHTER);
	}
	
	private final void insertImportClassCommands(int insertIdx, String fullClassName) throws BadLocationException {
		if(fullClassName.startsWith(CodeHelper.JAVA_PACKAGE_CLASS_PREFIX, 0)) {
			fullClassName += "\n";
		}else {
			fullClassName = RubyExector.JAVA_MAO_MAO + fullClassName + "\n";//可能hc.或用户级
		}
		
		document.insertString(insertIdx, RubyExector.IMPORT, ScriptEditPanel.KEYWORDS_LIGHTER);
		insertIdx += RubyExector.IMPORT.length();
		document.insertString(insertIdx, fullClassName, ScriptEditPanel.DEFAULT_LIGHTER);
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

			document.remove(oriScriptIdx, preCodeLowerCharsLen);

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

			document.remove(oriScriptIdx, preCodeLowerCharsLen);
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

			document.remove(oriScriptIdx, preCodeLowerCharsLen);
			final String insertedCode = item.code;
			AttributeSet attSet = null;
			if (item.isCSSProperty || item.isCSSClass) {
				attSet = ScriptEditPanel.STR_LIGHTER;
			}
			document.insertString(oriScriptIdx, insertedCode, attSet);// 注：attSet不为null可减少闪烁，即使后有refreshCurrLineAfterKey
			final int position = oriScriptIdx + insertedCode.length();

			textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, oriScriptIdx));
			TabHelper.notifyInputBlock(insertedCode.length() - preCodeLowerCharsLen);

			final char[] insertedCodeChars = insertedCode.toCharArray();
			final int parameterNum = TabBlock.countParameterNum(insertedCodeChars);
			if (parameterNum > 0) {
				TabHelper.setCurrentTabBlock(oriScriptIdx, insertedCodeChars, parameterNum);
				// setCurrentTabBlock方法内有setCaretPosition，故不用
				// textPane.setCaretPosition(position);
			} else {
				textPane.setCaretPosition(position);
			}
		} catch (final Exception e) {
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

	public final void startAutoPopTip(final CodeItem item, final HCTextPane jtaPaneMaybeNull) {
		if (L.isInWorkshop) {
			ClassUtil.printCurrentThreadStack("---------------------------------startAutoPopTip---------------------------------");
		}
		synchronized (autoDocPopTip) {
			autoDocPopTip.resetTimerCount();
			autoDocPopTip.docHelper = docHelper;
			if (jtaPaneMaybeNull != null) {
				textPane = jtaPaneMaybeNull;
			}
			autoDocPopTip.classFrame = classFrame;
			autoDocPopTip.item = item;
			autoDocPopTip.layoutLimit = layoutLimit;
			autoDocPopTip.setEnable(true);
		}
	}

	public final void keyPressedAfterDot(final KeyEvent e) {
		final char keyChar = e.getKeyChar();
		if (keyChar == '￿') {// Shift in Mac
			return;
		}
		preCodeLowerChars[preCodeLowerCharsLen++] = keyChar;
		preCodeLower = String.valueOf(preCodeLowerChars, 0, preCodeLowerCharsLen).toLowerCase();

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
