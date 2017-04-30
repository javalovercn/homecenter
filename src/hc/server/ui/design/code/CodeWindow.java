package hc.server.ui.design.code;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
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
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
	
	private static final int scaleWindowSizeByFontSize(final int w_h){
		try{
			return w_h * Integer.parseInt(DefaultManager.getDesignerDocFontSize()) / DefaultManager.DEFAULT_DOC_FONT_SIZE;
		}catch (final Throwable e) {
			return w_h;
		}
	}
	
	public final void setMouseOverAutoTipLoc(final int x, final int y, final int fontHeight) {
		docHelper.isForMouseOverTip = true;
		docHelper.mouseOverX = x;
		docHelper.mouseOverY = y;
		docHelper.mouseOverFontHeight = fontHeight;
	}
	
	private static final int limitThreeOf(final int width){
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int threeOf = screenSize.width / 3;
		if(width > threeOf){
			return threeOf;
		}else{
			return width;
		}
	}
	
	private int loc_x, loc_y, fontHeight;
	private final JList codeList = new JList();//JRE 1.6 not gerneric
	Class codeBelongClass;
	private final ArrayList<CodeItem> classData = new ArrayList<CodeItem>();
	private final JFrame classFrame = new JFrame();
	private final DocLayoutLimit layoutLimit = new DocLayoutLimit();
	private final JScrollPane classPanel = new JScrollPane(codeList);
	private final DocTipTimer autoPopTip = new DocTipTimer("", 500, false);
	public final DocHelper docHelper;
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
			classFrame.validate();
			ClassUtil.revalidate(classFrame);
			classFrame.pack();
			classFrame.setLocation(loc_x, loc_y);
			if(codeList.getModel().getSize() > 0){
				codeList.setSelectedIndex(0);
			}
		}
	};
	
	final KeyListener keyListener = new KeyListener() {
		@Override
		public void keyTyped(final KeyEvent e) {
		}
		
		@Override
		public void keyReleased(final KeyEvent e) {
		}
		
		@Override
		public void keyPressed(final KeyEvent e) {
			if(classFrame.isVisible() == false){//仅doc时，不显示codeList
				hide(true);
				return;
			}
			
			final int keyCode = e.getKeyCode();
			
			if(keyCode == KeyEvent.VK_ESCAPE){
				hide(true);
				return;
			}
			
			synchronized (ScriptEditPanel.scriptEventLock) {
			if(classFrame.isVisible()){
			if(keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN 
					|| keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT){
				if(keyCode == KeyEvent.VK_UP){
					if(codeList.getSelectedIndex() == 0){
						SwingUtilities.invokeLater(new Runnable() {//App.invokeLaterUI不正常
							@Override
							public void run() {
								codeList.setSelectedIndex(codeList.getModel().getSize() - 1);
								final JScrollBar verticalScrollBar = classPanel.getVerticalScrollBar();
								verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount());
							}
						});
					}else{
						dispatchEvent(e, keyCode);
					}
				}else if(keyCode == KeyEvent.VK_DOWN){
					if(codeList.getSelectedIndex() == codeList.getModel().getSize() - 1){
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								codeList.setSelectedIndex(0);
								classPanel.getVerticalScrollBar().setValue(0);
							}
						});
					}else{
						dispatchEvent(e, keyCode);
					}
				}
				return;
			}
			
			if(keyCode == KeyEvent.VK_ENTER){
				final int selectedIndex = codeList.getSelectedIndex();
				actionOnItem(selectedIndex);
			}else if(keyCode == KeyEvent.VK_BACK_SPACE){
				if(preCodeCharsLen > 0){
					preCodeCharsLen--;
					preCodeLower = String.valueOf(preCodeChars, 0, preCodeCharsLen);
					SwingUtilities.invokeLater(refilterRunnable);
					
					try{
						document.remove(--movingScriptIdx, 1);
						textPane.setCaretPosition(movingScriptIdx);
//						textPane.updateUI();//会导致重新获得焦点时为posi:0
						textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, movingScriptIdx));
						TabHelper.notifyInputKey(true, e, e.getKeyChar(), 0);
					}catch (final Exception ex) {
					}
				}
			}else{
				keyPressedAfterDot(e);
			}
			}
			}
		}
	};
	
	public CodeWindow(final CodeHelper codeHelper){
		this.codeHelper = codeHelper;
		docHelper = new DocHelper(codeHelper, this);
		
		codeList.setCellRenderer(new ListCellRenderer() {//JRE 1.6 not gerneric
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
				if(type == CodeItem.TYPE_RESOURCES){
					renderer.setIcon(CodeRes.item5);
				}else if(type == CodeItem.TYPE_IMPORT){
					renderer.setIcon(CodeRes.item4);
				}else if(type == CodeItem.TYPE_FIELD){
					renderer.setIcon(CodeRes.item3);
				}else if(type == CodeItem.TYPE_METHOD){
					renderer.setIcon(CodeRes.item2);
				}else if(type == CodeItem.TYPE_CLASS){
					renderer.setIcon(CodeRes.item1);
				}
				renderer.setForeground(Color.BLACK);
				renderer.setText((String)value);
				return renderer;
			}
		});
		
		classFrame.setVisible(false);
//		classFrame.setAlwaysOnTop(true);
		classFrame.setUndecorated(true);
		
		classFrame.add(classPanel);
		classFrame.setPreferredSize(new Dimension(MAX_WIDTH, MAX_HEIGHT));
		classFrame.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(final WindowEvent e) {
			}
			
			@Override
			public void windowIconified(final WindowEvent e) {
			}
			
			@Override
			public void windowDeiconified(final WindowEvent e) {
			}
			
			@Override
			public void windowDeactivated(final WindowEvent e) {
				if(docHelper.isShowing()){
					codeList.requestFocusInWindow();
					return;
				}
				
				hide(true);
			}
			
			@Override
			public void windowClosing(final WindowEvent e) {
			}
			
			@Override
			public void windowClosed(final WindowEvent e) {
			}
			
			@Override
			public void windowActivated(final WindowEvent e) {
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
				codeHelper.notifyUsingByCode(false);
			}
			
			@Override
			public void mouseEntered(final MouseEvent e) {
				codeHelper.notifyUsingByCode(true);
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
				if(idx >= 0){
					final CodeItem item = classData.get(idx);
					if(docHelper.acceptType(item.type)){
						startAutoPopTip(item, null);
					}else{
						docHelper.setInvisible();
					}
				}
			}
		});
		
		codeList.addKeyListener(keyListener);
		
	}
	
	public final void hide(final boolean lostFocus){
		if(L.isInWorkshop){
			ClassUtil.printCurrentThreadStack("[CodeTip] CodeWindow.hide(" + lostFocus + ")");
		}
		codeHelper.mouseExitHideDocForMouseMovTimer.setEnable(false);
		synchronized (classFrame) {
			if(classFrame.isVisible() || docHelper.isShowing()){
				isWillOrAlreadyToFront = false;
				synchronized (autoPopTip) {
					autoPopTip.setEnable(false);
					docHelper.setInvisible();
				}
				if(L.isInWorkshop){
					LogManager.log("[CodeTip] docHelper setInvisible.");
				}
				classFrame.setVisible(false);
				if(L.isInWorkshop){
					LogManager.log("[CodeTip] classFrame setVisible(false)");
				}
//				if(lostFocus == false){
//					textPane.requestFocusInWindow();
//				}
			}	
		}
	}
	
	public final void release(){
		if(classFrame != null){
			classFrame.dispose();
			docHelper.release();
			HCTimer.remove(autoPopTip);
		}
	}
	
	/**
	 * 如果没有选中的，则返回null
	 * @return
	 */
	public final CodeItem getSelectedLine(){
		final int selected = codeList.getSelectedIndex();
		if(selected == -1){
			return null;
		}else{
			return classData.get(selected);
		}
	}
	
	private HCTextPane textPane;
	Document document;
	ScriptEditPanel sep;
	private ArrayList<CodeItem> fullList;
	private String preCodeLower;
	private final char[] preCodeChars = new char[2048];
	private int preCodeCharsLen;
	private int movingScriptIdx, oriScriptIdx;
	
	private final void refilter(){
		fillPreCode(fullList, classData, preCodeLower);
		
		codeList.setModel(new AbstractListModel() {//JRE 1.6 not gerneric
            @Override
			public int getSize() { return classData.size(); }
            @Override
			public String getElementAt(final int i) { return classData.get(i).codeDisplay; }
        });
		
		if(classData.size() == 0){
			codeList.clearSelection();
			docHelper.setInvisible();
		}else{
			codeList.setSelectedIndex(0);
		}
		
		classPanel.getVerticalScrollBar().setValue(0);
		
		codeList.invalidate();
		classFrame.validate();
		ClassUtil.revalidate(classFrame);
		classFrame.pack();//有可能backspace，出现更长内容，而需要pack
	}

	public final void fillPreCode(final ArrayList<CodeItem> src, final ArrayList<CodeItem> target, 
			final String preCodeLower) {
		target.clear();
		final int size = src.size();
		final int preLen = preCodeLower.length();
		for (int i = 0; i < size; i++) {
			final CodeItem codeItem = src.get(i);
			
			if(preCodeType == CodeHelper.PRE_TYPE_OVERRIDE_METHOD){
				if(codeItem.isOerrideable() == false){
					continue;
				}
			}
			
//			if(CodeHelper.isDisplayOverrideMethodAndDoc == false){
//				if(codeItem.overrideMethodLevel < 0){//过滤子类的同名方法
//					continue;
//				}
//			}
			
			if(preLen == 0 
					|| (codeItem.isFullPackageAndClassName && codeItem.type == CodeItem.TYPE_CLASS && codeItem.codeLowMatch.indexOf(preCodeLower) >= 0)
					|| codeItem.codeLowMatch.startsWith(preCodeLower) 
					){
				addItemExcludeOverride(codeItem, target);
			}
		}
		
		Collections.sort(target);
	}
	
	public static void fillForAutoTip(final ArrayList<CodeItem> src, final ArrayList<CodeItem> target, final String fieldOrMethod) {
		target.clear();
		final int size = src.size();
		for (int i = 0; i < size; i++) {
			final CodeItem codeItem = src.get(i);
//			if(codeItem.isDefed){//不过滤，比如operate(p1, p2)
//				continue;
//			}
			if(codeItem.fmClass == CodeHelper.IterNodeClass){//定义方法不作doc
				continue;
			}
//			if(CodeHelper.isDisplayOverrideMethodAndDoc == false){
//				if(codeItem.overrideMethodLevel < 0){//过滤子类的同名方法
//					continue;
//				}
//			}
			if(codeItem.fieldOrMethodOrClassName.equals(fieldOrMethod)){
				addItemExcludeOverride(codeItem, target);
			}
		}
		Collections.sort(target);
	}
	
	private static void addItemExcludeOverride(final CodeItem codeitem, final ArrayList<CodeItem> target){
		if(codeitem.type == CodeItem.TYPE_METHOD){
			final int size = target.size();
			for (int i = 0; i < size; i++) {
				final CodeItem item = target.get(i);
				if(item.isOverrideItem(codeitem)){
					if(item.overrideMethodLevel > codeitem.overrideMethodLevel){
						return;
					}else{
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
			final int x, final int y, final ArrayList<CodeItem> list, 
			final String preCode, final int scriptIdx, final int fontHeight){
		this.preCodeType = preCodeType;
		
		isWillOrAlreadyToFront = true;
		docHelper.isForMouseOverTip = false;
		
		fullList = list;
		this.codeBelongClass = codeClass;
		classData.clear();
		
		this.preCodeLower = preCode.toLowerCase();
		final char[] preCharsLower = this.preCodeLower.toCharArray();
		preCodeCharsLen = preCharsLower.length;
		this.movingScriptIdx = scriptIdx;
		this.oriScriptIdx = scriptIdx - preCodeCharsLen;
		
		System.arraycopy(preCharsLower, 0, preCodeChars, 0, preCodeCharsLen);
		
		this.sep = sep;
		textPane = eventFromComponent;
		document = textPane.getDocument();
		
		loc_x = x;
		loc_y = y;
		this.fontHeight = fontHeight;
		
//		if(classFrame.isVisible() == false){
		//由于已处于visible时，强制toFront
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

			if(loc_x + frameSize.width > screenSize.width){
				showX = loc_x - frameSize.width;//右边没有空间，左展式
				isIgnoreLayoutLimit = true;
			}
			if(loc_y + fontHeight + frameSize.height > screenSize.height){
				showY = loc_y - frameSize.height;//底部没有空间，上展式
				if(isIgnoreLayoutLimit == false){
					layoutLimit.isNotDownLayout = true;
				}
			}else{
				showY = loc_y + fontHeight;//下展式
				if(isIgnoreLayoutLimit == false){
					layoutLimit.isNotUpLayout = true;
				}
			}
			
			classFrame.setLocation(showX, showY);
			classFrame.setVisible(true);
			codeList.requestFocusInWindow();
			
			if(L.isInWorkshop){
				LogManager.log("[CodeTip] codeList requestFocusInWindow.");
			}
		}
	};

	public final void actionOnItem(final int selectedIndex) {
		hide(false);

		if(selectedIndex >= 0){
			final CodeItem item = classData.get(selectedIndex);
			if(item != null){
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if(item.anonymousClass != null){
							insertAnonymouseClass(item);
						}else if(item.isInnerClass){
							insertInnerClass();
						}else{
							insertMethod(item);
						}
					}
					
					private final String removeParameters(final String insertedCode){
						return insertedCode.substring(0, insertedCode.indexOf("("));
					}
					
					private final char[] getShuJin(final Document doc, int line) throws BadLocationException {
						//获得缩进串
						String lineStr;
						while(true){
							lineStr = ScriptEditPanel.getLineText(doc, line);
							if(lineStr.length() > 0){
								break;
							}
							line--;
						}
						return lineStr.toCharArray();
					}

					public final void insertInnerClass() {
						try {
							final int charIdxRemovedTab = getShujinTabIdx();
							
							document.remove(oriScriptIdx, preCodeCharsLen);
							
							final StringBuilder sb = StringBuilderCacher.getFree();
							sb.append(CodeHelper.JRUBY_NEW);
							sb.append("(BaseClass) {");
							sb.append('\n');
							
							sb.append(shujin, 0, charIdxRemovedTab);
							sb.append('\t');
//							final int newLocIdx = sb.length();
							sb.append('\n');
							
							sb.append(shujin, 0, charIdxRemovedTab);
							sb.append("}.new");
							
							document.insertString(oriScriptIdx, sb.toString(), ScriptEditPanel.DEFAULT_LIGHTER);
							StringBuilderCacher.cycle(sb);
							
							final int position = oriScriptIdx + CodeHelper.JRUBY_NEW.length() + 1;
							textPane.setSelectionStart(position);
							textPane.setSelectionEnd(position + 9);//BaseClass.length == 9
							
							TabHelper.setInnerClassTabBlock();
						} catch (final BadLocationException e) {
							ExceptionReporter.printStackTrace(e);
						}
					}
					
					char[] shujin;
					
					public final void insertAnonymouseClass(final CodeItem item) {
						try {
							final int charIdxRemovedTab = getShujinTabIdx();
							
							document.remove(oriScriptIdx, preCodeCharsLen);
							final String insertedCode = removeParameters(item.code);
							
							final StringBuilder sb = StringBuilderCacher.getFree();
							sb.append(insertedCode);
							if(item.anonymousClass == Runnable.class){
								sb.append(" {");
							}else{
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
							
//							textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, oriScriptIdx));
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
							if(shujin[charIdxRemovedTab] == ' ' || shujin[charIdxRemovedTab] == '\t'){
							}else{
								break;
							}
						}
						return charIdxRemovedTab;
					}
					
					public final void insertMethod(final CodeItem item) {
						try {
							document.remove(oriScriptIdx, preCodeCharsLen);
							final String insertedCode = item.code;
							AttributeSet attSet = null;
							if(item.isCSSProperty || item.isCSSClass){
								attSet = ScriptEditPanel.STR_LIGHTER;
							}
							document.insertString(oriScriptIdx, insertedCode, attSet);//注：attSet不为null可减少闪烁，即使后有refreshCurrLineAfterKey
							final int position = oriScriptIdx + insertedCode.length();
							
							textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, oriScriptIdx));
							TabHelper.notifyInputBlock(insertedCode.length() - preCodeCharsLen);
							
							final char[] insertedCodeChars = insertedCode.toCharArray();
							final int parameterNum = TabBlock.countParameterNum(insertedCodeChars);
							if(parameterNum > 0){
								TabHelper.setCurrentTabBlock(oriScriptIdx, insertedCodeChars, parameterNum);
//								setCurrentTabBlock方法内有setCaretPosition，故不用
//								textPane.setCaretPosition(position);
							}else{
								textPane.setCaretPosition(position);
							}
						} catch (final BadLocationException e) {
//							ExceptionReporter.printStackTrace(e);
						}
					}
				});
			}
		}
	}

	private void dispatchEvent(final KeyEvent e, final int keyCode) {
		if(e.getSource() != codeList){
			codeList.dispatchEvent(new KeyEvent(codeList, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, e.getKeyChar()));
//			codeList.dispatchEvent(new KeyEvent(codeList, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, keyCode, e.getKeyChar()));
		}
	}

	public final void startAutoPopTip(final CodeItem item, final HCTextPane jtaPaneMaybeNull) {
		if(L.isInWorkshop){
			ClassUtil.printCurrentThreadStack("---------------------------------startAutoPopTip---------------------------------");
		}
		synchronized (autoPopTip) {
			autoPopTip.resetTimerCount();
			autoPopTip.docHelper = docHelper;
			if(jtaPaneMaybeNull != null){
				textPane = jtaPaneMaybeNull;
			}
			autoPopTip.classFrame = classFrame;
			autoPopTip.item = item;
			autoPopTip.fmClass = item.fmClass;
			autoPopTip.fieldOrMethodName = item.codeForDoc;//注意：构造方法已转为new(),而非simpleClassName()
			autoPopTip.type = item.type;
			autoPopTip.layoutLimit = layoutLimit;
			autoPopTip.setEnable(true);
		}
	}

	public final void keyPressedAfterDot(final KeyEvent e) {
		final char keyChar = e.getKeyChar();
		if(keyChar == '￿'){//Shift in Mac
			return;
		}
		preCodeChars[preCodeCharsLen++] = keyChar;
		preCodeLower = String.valueOf(preCodeChars, 0, preCodeCharsLen).toLowerCase();
		
		SwingUtilities.invokeLater(refilterRunnable);
		
		try{
			document.insertString(movingScriptIdx++, String.valueOf(keyChar), null);
			textPane.setCaretPosition(movingScriptIdx);
//					textPane.updateUI();//会导致重新获得焦点时为posi:0
			
			textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, movingScriptIdx));
			TabHelper.notifyInputKey(false, e, keyChar, 0);
		}catch (final Exception ex) {
		}
	}
}

class DocTipTimer extends HCTimer{
	DocHelper docHelper;
	JFrame classFrame;
	CodeItem item;
	String fieldOrMethodName;
	String fmClass;
	int type;
	DocLayoutLimit layoutLimit;
	
	public DocTipTimer(final String name, final int ms, final boolean enable){
		super(name, ms, enable);
	}

	@Override
	public final void doBiz() {
		synchronized (this) {//与hide互斥
			if(isEnable()){//重新检查条件，必须的
				if(type == CodeItem.TYPE_CLASS){
					final Class c = ResourceUtil.loadClass(fieldOrMethodName, L.isInWorkshop);
					if(c != null){
						CodeHelper.buildForClass(docHelper.codeHelper, null, c);
					}
				}
				docHelper.popDocTipWindow(item, classFrame, fmClass, fieldOrMethodName, type, layoutLimit);
				setEnable(false);
			}
		}
	}
}
