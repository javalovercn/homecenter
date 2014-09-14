package hc.server.ui;

import hc.core.IConstant;
import hc.core.data.DataInputEvent;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.server.data.screen.PNGCapturer;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

public class MCanvas extends PNGCapturer{
    
    final int width, height;
    final BufferedImage bufferedImage;
    BufferedImage bufferedImageCombo;
    final Graphics graphcis;
    final int[] imageData;
    int[] imageDataComboBox;
    public final JFrame frame, frameCombobox;
    Mlet mlet;
	boolean isPressed = false;
	
    private JScrollPane scrolPanel;
	
	public MCanvas(int width, int height) {
		super(width, height, false, getMaskFromBit(IConstant.COLOR_64_BIT));
		
		this.width = width;
		this.height = height;
		
		capRect.setBounds(0, 0, width, height);
		
		imageData = new int[width * height];
		bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		graphcis = bufferedImage.getGraphics();
		
		frame = new JFrame();
		frameCombobox = new JFrame();
		frameCombobox.getContentPane().setLayout(new BorderLayout());
		isPressed = false;
	}
	
	public void setMlet(Mlet mlet){
		this.mlet = mlet;
	}
	
	public Mlet getMlet(){
		return mlet;
	}
	
	@Override
	public int grabImage(final Rectangle bc, final int[] blockImageData) {
		if(bc.x == 0 && bc.y == 0){
			scrolPanel.paint(graphcis);
			bufferedImage.getRGB(0, 0, width, height, imageData, 0, width);

			final JList list = listForJComboBox;
			if(isSelectedJComboBox && list != null){
				
				final Dimension d = list.getSize();
				final int listWidth = d.width, listHeight = d.height;
				final int listSize = listWidth * listHeight;
				if(imageDataComboBox == null || imageDataComboBox.length < listSize){
					//有可能出现大于全屏的情形
					imageDataComboBox = new int[listSize];
				}
				if((bufferedImageCombo == null) || 
						(bufferedImageCombo.getWidth() != listWidth || bufferedImageCombo.getHeight() != listHeight)){
					bufferedImageCombo = new BufferedImage(listWidth, listHeight, BufferedImage.TYPE_INT_RGB);
				}
				list.paint(bufferedImageCombo.getGraphics());
				
				final int startX = locComboX, startY = locComboY;

				bufferedImageCombo.getRGB(0, 0, listWidth, listHeight, imageDataComboBox, 0, listWidth);

				try{
					final int maxLineDataLen = Math.min(listWidth, width - startX);
					final int endPos = listSize - listWidth;
					for (int destPos = startY*width + startX, srcPos = 0; srcPos < endPos; ) {
//						L.V = L.O ? false : LogManager.log("destPos : " + destPos + ", srcPos : " + srcPos + ", max target : " + imageDataComboBox.length);
						if(destPos >= 0){
							System.arraycopy(imageDataComboBox, srcPos, imageData, destPos, maxLineDataLen);
						}
						srcPos += listWidth;
						destPos += width;
					}
				}catch (Exception e) {
					//忽略超出边界的数据
//					e.printStackTrace();
				}
			}
		}
		
		final int bwidth = bc.width; 
		final int bheight = bc.height;

		final int size = bwidth * bheight;
		
		if(bwidth == width && bheight == height){
			System.arraycopy(imageData, 0, blockImageData, 0, size);
		}else{
			for (int srcPos = bc.y*width + bc.x, destPos = 0; destPos < size; ) {
				System.arraycopy(imageData, srcPos, blockImageData, destPos, bwidth);
				srcPos += width;
				destPos += bwidth;
			}
		}
		
//		System.out.println("Block x : " + bc.x + ", y : " + bc.y + ", w : " + bc.width + ", h : " + bc.height);
		return size;
	}
	
	@Override
	public boolean actionInput(DataInputEvent e) {
		final int eventType = e.getType();
		final int x = e.getX();
		final int y = e.getY();
		if(eventType == DataInputEvent.TYPE_TAG_KEY_PRESS_V_SCREEN){
			keyPressed(x, y);
			return true;
		}else if(eventType == DataInputEvent.TYPE_TAG_KEY_RELEASE_V_SCREEN){
			keyReleased(x, y);
			return true;
		}else if(eventType == DataInputEvent.TYPE_TRANS_TEXT){
			try {
				String s = e.getTextDataAsString();
				if(currFocusObject instanceof JTextComponent){
					((JTextComponent)currFocusObject).setText(s);
				}else if(currFocusObject instanceof TextComponent){
					((TextComponent)currFocusObject).setText(s);
				}
			}catch (Exception ex) {
			}
		} else {
			if(eventType == DataInputEvent.TYPE_TAG_POINTER_PRESS_V_SCREEN){
				pointerPressed(x, y);
				return true;
			}else if(eventType == DataInputEvent.TYPE_TAG_POINTER_DRAG_V_SCREEN){
				pointerDragged(x, y);
				return true;
			}else if(eventType == DataInputEvent.TYPE_TAG_POINTER_RELEASE_V_SCREEN){
				pointerReleased(x, y);
				return true;
			}
		}
		
		return false;
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
	
	public void init(){	
		scrolPanel = new JScrollPane(mlet, 
	    		JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//		scrolPanel.setOpaque(true); //content panes must be opaque
	    frame.setContentPane(scrolPanel);
	    
	    scrolPanel.setPreferredSize(new Dimension(width, height));
	    frame.pack();
	}

	private void updateTraveKeys(int traverKey, int[] keys) {
		Set forwardKeys = mlet.getFocusTraversalKeys(
			    traverKey);
		
		Set newForwardKeys = new HashSet(forwardKeys);
		
		for (int i = 0; i < keys.length; i++) {
			newForwardKeys.add(KeyStroke.getKeyStroke(keys[i], 0));
		}
		
		mlet.setFocusTraversalKeys(traverKey, newForwardKeys);
	}
	
	public void keyPressed(int keyStates, int gameAction){
		doKeyAction(keyStates, gameAction, true);
	}

	public void keyReleased(int keyCode, int gameAction){
//		doKeyAction(keyCode, gameAction, false);		
	}
	
	private boolean isEditableComponent(final Component c){
		return c.isEnabled() && (c instanceof JTextComponent) || (c instanceof TextComponent);
	}
	
	private boolean isFocusableComponent(final Component c){
		return c.isVisible() && (
					(c instanceof Button) 
				|| 	(c instanceof AbstractButton)
				||	(c instanceof JTextComponent)
				||	(c instanceof TextComponent)
				||	(c instanceof JComboBox)
				||	(c instanceof JList)
				||	(c instanceof JTree)
				||	(c instanceof JTable)
				); 
	}
	
	private Component focusNext(final Component c, boolean isContainIn){
		if(c instanceof Container && isContainIn){
			if(((Container)c).getComponentCount() > 0){
				final Component component = ((Container)c).getComponent(0);
				if(isFocusableComponent(component)){
					focus(component);
					return component;
				}else{
					return focusNext(component, true);
				}
			}
		}
		return focusNext(c);
	}
	
	private Component focusNext(final Component c) {		
		if(c instanceof Mlet){
			return null;
		}
		
		if(c == null){
			//获得初始的第一个可焦点对象
			if(mlet.getComponentCount() > 0){
				final Component nextFocus = mlet.getComponent(0);
				if(isFocusableComponent(nextFocus)){
					focus(nextFocus);
					return nextFocus;
				}else{
					return focusNext(nextFocus, true);
				}
			}else{
				return null;
			}
		}
		
	    Container root = c.getParent();
	    int idx = root.getComponentZOrder(c);
	    final int tailIdx = root.getComponentCount() - 1;
	    
	    Component nextFocus = null;
		if(idx == tailIdx){
			return focusNext(root, false);
	    }else if(idx < tailIdx){
	    	nextFocus = root.getComponent(idx + 1);
	    }

		if (nextFocus != null) {
			if(isFocusableComponent(nextFocus)){
				focus(nextFocus);
				return nextFocus;
			}else{
				return focusNext(nextFocus, true);
			}
	    }
	    return nextFocus;
	}

	private Component focusPrevious(final Component c, boolean isContainIn) {
		if(c instanceof Container && isContainIn){
			final int componentCount = ((Container)c).getComponentCount();
			if(componentCount > 0){
				final Component component = ((Container)c).getComponent(componentCount - 1);
				if(isFocusableComponent(component)){
					focus(component);
					return component;
				}else{
					return focusPrevious(component, true);
				}
			}
		}
		return focusPrevious(c);
	}
	private Component focusPrevious(final Component c) {
//		final java.awt.event.KeyEvent event = new java.awt.event.KeyEvent(c, 
//				java.awt.event.KeyEvent.KEY_PRESSED, 
//				System.currentTimeMillis(), java.awt.event.KeyEvent.SHIFT_DOWN_MASK, 
//				java.awt.event.KeyEvent.VK_TAB, (char)java.awt.event.KeyEvent.VK_TAB);
		if(c instanceof Mlet){
			return null;
		}
		
		if(c == null){
			//获得初始的最末可焦点对象
			final int componentCount = mlet.getComponentCount();
			if(componentCount > 0){
				final Component preFocus = mlet.getComponent(componentCount - 1);
				if(isFocusableComponent(preFocus)){
					focus(preFocus);
					return preFocus;
				}else{
					return focusPrevious(preFocus, true);
				}
			}else{
				return null;
			}
		}
		
	    Container root = c.getParent();
	    int idx = root.getComponentZOrder(c);
	    
	    Component preFocus = null;
		if(idx == 0){
			return focusPrevious(root, false);
	    }else{
	    	preFocus = root.getComponent(idx - 1);
	    }

		if (preFocus != null) {
			if(isFocusableComponent(preFocus)){
				focus(preFocus);
				return preFocus;
			}else{
				return focusPrevious(preFocus, true);
			}
	    }
		
		return preFocus;
	}

	private void doKeyAction(int keyStates, int gameAction, boolean isPressNotRelease) {
		if (gameAction == LEFT || keyStates == KEY_NUM4) {
			focusPrevious(currFocusObject);
		} else if (gameAction == RIGHT || keyStates == KEY_NUM6) {
			focusNext(currFocusObject);
		} else if (gameAction == UP || keyStates == KEY_NUM2) {
			focusPrevious(currFocusObject);
		} else if (gameAction == DOWN || keyStates == KEY_NUM8) {
			focusNext(currFocusObject);
		} else if (gameAction == FIRE || keyStates == KEY_NUM5) {
			if(doActon(currFocusObject) == false){
				dispatchEvent(currFocusObject, new java.awt.event.KeyEvent(currFocusObject, 
					java.awt.event.KeyEvent.KEY_PRESSED, 
					System.currentTimeMillis(), 0, java.awt.event.KeyEvent.VK_ENTER, 
					  (char)java.awt.event.KeyEvent.VK_ENTER));
			}
//		    eq.postEvent(new ActionEvent(focusManager.getFocusOwner(), 500, "", 
//		    		System.currentTimeMillis(), 0));
		}
	}
	

	public void pointerPressed(int x, int y){
//		L.V = L.O ? false : LogManager.log("Pressed on MCanvas at x:" + x + ", y:" + y);
		
//	    final Component componentAt = mlet.getComponentAt(x, y);
//	    mlet.dispatchEvent(new MouseEvent(componentAt, MouseEvent.MOUSE_PRESSED, 
//	    		System.currentTimeMillis(), MouseEvent.MOUSE_PRESSED,//BUTTON1_MASK 
//				x, y, 1, false, MouseEvent.BUTTON1));
		
		isPressed = true;
	}
	
	private Component currFocusObject;
	
	private Component getCtrlComponentAt(Component contain, int x, int y){
		if(contain instanceof JTabbedPane){
			JTabbedPane tabPane = (JTabbedPane)contain;
			final int size = tabPane.getTabCount();
			for (int i = 0; i < size; i++) {
				if(tabPane.getBoundsAt(i).contains(x, y)){
					tabPane.setSelectedIndex(i);
					return null;
				}
			}
			contain = tabPane.getComponentAt(tabPane.getSelectedIndex());
			return getCtrlComponentAt(contain, x - contain.getX(), y - contain.getY());
		}
		final Component c = contain.getComponentAt(x, y);
		if(c == null){
			return null;
		}else if(c == contain){
			return c;
		}else if(isFocusableComponent(c)){
				return c;
		}else{
			return getCtrlComponentAt(c, x - c.getX(), y - c.getY());
		}
	}
	
	private Component getInitFocusComponentAt(Component contain){
		if(contain instanceof JTabbedPane){
			JTabbedPane tabPane = (JTabbedPane)contain;
			contain = tabPane.getComponentAt(tabPane.getSelectedIndex());
			return getInitFocusComponentAt(contain);
		}
		if(contain instanceof Container && ((Container)contain).getComponentCount() > 0){
			final int size = ((Container)contain).getComponentCount();
			for (int i = 0; i < size; i++) {
				Component c = getInitFocusComponentAt(((Container)contain).getComponent(i));
				if(c != null){
					return c;
				}
			}
			return null;
		}else if(contain instanceof Component){
			if(((Component)contain).hasFocus()){
				return contain;
			}else{
				return null;
			}
		}else{
			return null;
		}
	}
	
	private boolean isSelectedJComboBox = false;
	private int locComboX, locComboY;
	private JList listForJComboBox;
	private JComboBox oriComboBox;
	
	public void pointerReleased(int x, int y){
		if(isSelectedJComboBox){
			int comboX = x - locComboX, comboY = y - locComboY;
			final Dimension size = listForJComboBox.getSize();
			boolean isFinished = false;
			if((comboX < size.width && comboX >= 0) 
					&& (comboY < size.height && comboY >= 0)){
				final MouseEvent me = new MouseEvent(listForJComboBox, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
			    		MouseEvent.BUTTON1_MASK, comboX, comboY, 1, false);
//				L.V = L.O ? false : LogManager.log("JList x : " + comboX + ", y : " + comboY);
				dispatchEvent(listForJComboBox, me);
				
				//会自动触发ItemListener和ActionListener
				oriComboBox.setSelectedIndex(listForJComboBox.getSelectedIndex());
				isFinished = true;
			}
			
			//hide JCombobox
			frameCombobox.getContentPane().removeAll();
			listForJComboBox = null;
			isSelectedJComboBox = false;
			
			if(isFinished){
				return;
			}
		}
		
//		L.V = L.O ? false : LogManager.log("Released on MCanvas at x:" + x + ", y:" + y);
	    final Component componentAt = getCtrlComponentAt(scrolPanel, x, y);//注意：不能是mlet，因为有可能出现Scroll情形
	    if(componentAt == null){
	    	return;
	    }
	    //先执行focusLost
	    if(currFocusObject != null && currFocusObject != componentAt){
	    	loseFocus(currFocusObject);
	    }
	    //再执行focusGained
		focus(componentAt);
		
		if(componentAt instanceof JComboBox){
			oriComboBox = (JComboBox)componentAt;
			JList list = new JList(oriComboBox.getModel());
//				final Dimension preferredSize = new Dimension(componentAt.getWidth(), 
//					frame.getPreferredSize().height - list.getLocation().y);
//				list.setPreferredSize(preferredSize);
			
			list.setBorder(BorderFactory.createEtchedBorder());
			list.setFixedCellWidth(componentAt.getWidth() - 3);//-3是为了右边侧被顶出或与背景相同，而不能出现应有边框效果
//				list.setVisibleRowCount(10);
			
			frameCombobox.getContentPane().add(list, BorderLayout.CENTER);
			//不能执行下行，没有效果，无显示
//				list.setPreferredSize(preferredSize);
//				list.setMaximumSize(preferredSize);
//				frameCombobox.setMaximumSize(preferredSize);
//				frameCombobox.setMaximizedBounds(new Rectangle(preferredSize));
			frameCombobox.pack();
			Point l = getRelationLocation(frame.getContentPane(), componentAt);
			locComboX = l.x;
			if(l.y > height / 2){
				locComboY = l.y - list.getSize().height;
			}else{
				locComboY = l.y + componentAt.getHeight();
			}
//				L.V = L.O ? false : LogManager.log("shade JComboBox height : " + list.getSize().height);
			listForJComboBox = list;

			isSelectedJComboBox = true;
		}else{
			//支持JList 
			//先执行Actionlistener
			doActon(componentAt);
			//然后执行MouseEvent，如果同一个组件注册多种类型事件侦听器，则可能被触发多次类型。
		    final MouseEvent e = new MouseEvent(componentAt, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
		    		MouseEvent.BUTTON1_MASK, x, y, 1, false);
		    if(dispatchEvent(componentAt, e) == false){
		    	frame.getToolkit().getSystemEventQueue().postEvent(e);
	//	    	frame.dispatchEvent(e);//不能执行如MouseEvent
		    }
		}
	    isPressed = false;
	}
	
	private Point getRelationLocation(Component topCorn, Component sub){
		final Container parent = sub.getParent();
		final Point location = sub.getLocation();
		if(parent == topCorn || sub == topCorn){
			return location;
		}else{
			Point diff = getRelationLocation(topCorn, parent);
			location.x += diff.x;
			location.y += diff.y;
			return location;
		}
	}

	private void focus(final Component componentAt) {
		if(currFocusObject == componentAt){
			if(isEditableComponent(componentAt)){
				//发送编辑文本指令到手机
				HCURLUtil.sendGoPara(HCURL.DATA_PARA_INPUT, "all");//all表示支持全部类型
			}
		}else{
			final FocusEvent fe = new FocusEvent(componentAt, FocusEvent.FOCUS_GAINED);
			componentAt.dispatchEvent(fe);
		    currFocusObject = componentAt;
		}
	}

	private void loseFocus(final Component componentAt) {
		final FocusEvent fe = new FocusEvent(componentAt, FocusEvent.FOCUS_LOST);
		componentAt.dispatchEvent(fe);
	}

	private static boolean doActon(final Component componentAt) {
		if (componentAt instanceof Button) {
			final ActionEvent actionEvent = new ActionEvent( componentAt, ActionEvent.ACTION_PERFORMED, 
					((Button)componentAt).getActionCommand() );
			if(dispatchEvent(componentAt, actionEvent)){
				return true;
			}
		}else if(componentAt instanceof AbstractButton){
			if(componentAt instanceof JToggleButton){
				final JToggleButton togButton = (JToggleButton)componentAt;
				if(togButton instanceof JRadioButton){
					if(!togButton.isSelected()){
						togButton.setSelected(true);
					}
				}else{
					//JCheckBox
					togButton.setSelected(!togButton.isSelected());
				}
			}
			final ActionEvent actionEvent = new ActionEvent( componentAt, ActionEvent.ACTION_PERFORMED, 
					((AbstractButton)componentAt).getActionCommand() );
			if(dispatchEvent(componentAt, actionEvent)){
				return true;
			}
		}
		return false;
	}
	
	public static boolean dispatchEvent(Component component, AWTEvent event){
		if(event instanceof FocusEvent){
			FocusListener[] fl = component.getListeners(FocusListener.class);
			if(fl.length == 0){
				return false;
			}
			fl[fl.length - 1].focusGained((FocusEvent)event);
			return true;
		}else if(event instanceof ActionEvent){
			ActionListener[] al = component.getListeners(ActionListener.class);
			if(al.length == 0){
				return false;
			}
			al[al.length - 1].actionPerformed((ActionEvent)event);
			return true;
		}else if(event instanceof MouseEvent){
			MouseListener[] ml = component.getListeners(MouseListener.class);
			if(ml.length == 0){
				return false;
			}
			final MouseListener mouseListener = ml[ml.length - 1];
			mouseListener.mousePressed((MouseEvent)event);
			mouseListener.mouseReleased((MouseEvent)event);
			mouseListener.mouseClicked((MouseEvent)event);
			return true;
		}else if(event instanceof java.awt.event.KeyEvent){
			KeyListener[] kl = component.getKeyListeners();
			if(kl.length == 0){
				return false;
			}
			kl[kl.length - 1].keyTyped((java.awt.event.KeyEvent)event);
			return true;
		}else{
			
		}
		return false;
	}
	
	public void pointerDragged(int x, int y){
		
	}
	
	@Override
	public void onExit() {
		mlet.onExit();
		frame.dispose();
		frameCombobox.dispose();
		super.onExit();
	}
	
	@Override
	public void onStart() {
		super.start();

		mlet.onStart();
	}

	@Override
	public void onPause() {
		enableStopCap(true);
		mlet.onPause();
	}

	@Override
	public void onResume() {
		enableStopCap(false);
		mlet.onResume();
	}

	public static final int FIRE = 8;
	public static final int RIGHT = 5;
	public static final int LEFT = 2;
	public static final int DOWN = 6;
	public static final int UP = 1;
	
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;

    public static final int KEY_POUND = 35;
    public static final int KEY_STAR = 42;

    public static final int KEY_NUM0 = 48;
    public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;

}