package hc.server.ui.design.code;

import hc.core.HCTimer;
import hc.core.util.ExceptionReporter;
import hc.server.DefaultManager;
import hc.server.ui.design.Designer;
import hc.server.ui.design.hpj.HCTextPane;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.util.ClassUtil;
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
	private final DocHelper docHelper;
	
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
			final int keyCode = e.getKeyCode();
			
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
			
			if(keyCode == KeyEvent.VK_ESCAPE){
				hide(false);
				return;
			}
			
			if(keyCode == KeyEvent.VK_ENTER){
				final int selectedIndex = codeList.getSelectedIndex();
				actionOnItem(selectedIndex);
			}else if(keyCode == KeyEvent.VK_BACK_SPACE){
				if(preCodeCharsLen > 0){
					preCodeCharsLen--;
					preCode = String.valueOf(preCodeChars, 0, preCodeCharsLen);
					SwingUtilities.invokeLater(refilterRunnable);
					
					try{
						document.remove(--movingScriptIdx, 1);
						textPane.updateUI();
						textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, movingScriptIdx));
						TabHelper.notifyInputKey(true, e, e.getKeyChar());
					}catch (final Exception ex) {
					}
				}
			}else{
				final char keyChar = e.getKeyChar();
				if(keyChar == '￿'){//Shift in Mac
					return;
				}
				preCodeChars[preCodeCharsLen++] = keyChar;
				preCode = String.valueOf(preCodeChars, 0, preCodeCharsLen);
				
				SwingUtilities.invokeLater(refilterRunnable);
				
				try{
					document.insertString(movingScriptIdx++, String.valueOf(keyChar), null);
					textPane.updateUI();
					
					textPane.refreshCurrLineAfterKey(ScriptEditPanel.getLineOfOffset(document, movingScriptIdx));
					TabHelper.notifyInputKey(false, e, keyChar);
				}catch (final Exception ex) {
				}
			}
		}
	};
	
	public CodeWindow(){
		docHelper = new DocHelper(this);
		
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
			}
			
			@Override
			public void mouseEntered(final MouseEvent e) {
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
						synchronized (autoPopTip) {
							autoPopTip.resetTimerCount();
							autoPopTip.docHelper = docHelper;
							autoPopTip.classFrame = classFrame;
							autoPopTip.fmClass = item.fmClass;
							autoPopTip.fieldOrMethodName = item.code;//注意：构造方法已转为new(),而非simpleClassName()
							autoPopTip.type = item.type;
							autoPopTip.layoutLimit = layoutLimit;
							autoPopTip.setEnable(true);
						}
					}else{
						docHelper.setInvisible();
					}
				}
			}
		});
		
		codeList.addKeyListener(keyListener);
		
	}
	
	public final void hide(final boolean lostFocus){
		synchronized (classFrame) {
			if(classFrame.isVisible()){
				synchronized (autoPopTip) {
					autoPopTip.setEnable(false);
					docHelper.setInvisible();
				}
				classFrame.setVisible(false);
				if(lostFocus == false){
					Designer.getInstance().requestFocus();
					textPane.requestFocusInWindow();
					textPane.setCaretPosition(movingScriptIdx);
				}
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
	private String preCode;
	private final char[] preCodeChars = new char[2048];
	private int preCodeCharsLen;
	private int movingScriptIdx, oriScriptIdx;
	
	private final void refilter(){
		classData.clear();
		final int size = fullList.size();
		final int preLen = preCode.length();
		for (int i = 0; i < size; i++) {
			final CodeItem codeItem = fullList.get(i);
			if(preLen == 0 
					|| (codeItem.isFullPackageAndClassName && codeItem.type == CodeItem.TYPE_CLASS && codeItem.codeLowMatch.indexOf(preCode) >= 0)
					|| codeItem.codeLowMatch.startsWith(preCode) 
					){
				classData.add(codeItem);
			}
		}
		
		codeList.setModel(new AbstractListModel() {//JRE 1.6 not gerneric
            @Override
			public int getSize() { return classData.size(); }
            @Override
			public String getElementAt(final int i) { return classData.get(i).codeDisplay; }
        });
		
		if(classData.size() == 0){
			codeList.clearSelection();
		}else{
			codeList.setSelectedIndex(0);
		}
		
		classPanel.getVerticalScrollBar().setValue(0);
		
		codeList.invalidate();
		classFrame.validate();
		ClassUtil.revalidate(classFrame);
		classFrame.pack();//有可能backspace，出现更长内容，而需要pack
	}
	
	final Rectangle rect = new Rectangle(0, 0, 1, 1);
	
	public final void toFront(final Class codeClass, final ScriptEditPanel sep, final HCTextPane eventFromComponent, final int x, final int y, final ArrayList<CodeItem> list, 
			final String preCode, final int scriptIdx, final int fontHeight){
		fullList = list;
		this.codeBelongClass = codeClass;
		classData.clear();
		
		final char[] preChars = preCode.toCharArray();
		this.preCode = preCode;
		preCodeCharsLen = preChars.length;
		this.movingScriptIdx = scriptIdx;
		this.oriScriptIdx = scriptIdx - preCodeCharsLen;
		
		System.arraycopy(preChars, 0, preCodeChars, 0, preCodeCharsLen);
		
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

	final Runnable toVisibleRunnable = new Runnable() {
		final Dimension frameSize = new Dimension();
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
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
						if(item.anonymousClass == null){
							insertMethod(item);
						}else{
							insertAnonymouseClass(item);
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

					public final void insertAnonymouseClass(final CodeItem item) {
						try {
							final int line = ScriptEditPanel.getLineOfOffset(document, oriScriptIdx);
							final char[] shujin = getShuJin(document, line);
							int charIdxRemovedTab = 0;
							for (; charIdxRemovedTab < shujin.length; charIdxRemovedTab++) {
								if(shujin[charIdxRemovedTab] == ' ' || shujin[charIdxRemovedTab] == '\t'){
								}else{
									break;
								}
							}
							
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
					
					public final void insertMethod(final CodeItem item) {
						try {
							document.remove(oriScriptIdx, preCodeCharsLen);
							final String insertedCode = item.code;
							document.insertString(oriScriptIdx, insertedCode, null);
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
							ExceptionReporter.printStackTrace(e);
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
}

class DocTipTimer extends HCTimer{
	DocHelper docHelper;
	JFrame classFrame;
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
					try {
						final Class c = Class.forName(fieldOrMethodName);
						DocHelper.processDoc(c, false);
					} catch (final ClassNotFoundException e) {
					}
				}
				docHelper.popDocTipWindow(classFrame, fmClass, fieldOrMethodName, type, layoutLimit);
				setEnable(false);
			}
		}
	}
}
