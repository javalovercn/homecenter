package hc.server.ui;

import hc.core.IConstant;
import hc.core.L;
import hc.core.data.DataInputEvent;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.server.MultiUsingManager;
import hc.server.ScreenServer;
import hc.server.data.screen.PNGCapturer;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import hc.util.ResourceUtil;

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
import java.awt.event.MouseMotionListener;
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
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

public class MletSnapCanvas extends PNGCapturer implements IMletCanvas{
    
    final int width, height;
    final BufferedImage bufferedImage;
    BufferedImage bufferedImageCombo;
    final Graphics graphcis;
    final Runnable scrollPrintRunnable, listPrintRunnable;
    final int[] imageData;
    int[] imageDataComboBox;
    public JFrame frame, frameCombobox;
    JScrollPane scrollPane;
    public Mlet mlet;
	final static boolean isAndroidServer = ResourceUtil.isAndroidServerPlatform();
	final boolean isJ2SEPanelInset = ResourceUtil.isJ2SELimitFunction();
	public ProjectContext projectContext;
	private ProjResponser projResp;
	
	public MletSnapCanvas(final J2SESession coreSS, final int w, final int h) {
		super(coreSS, w, h, false, getMaskFromBit(IConstant.COLOR_64_BIT));
		
		this.width = w;
		this.height = h;
		
		capRect.setBounds(0, 0, width, height);
		
		imageData = new int[width * height];
		if(isJ2SEPanelInset){
			bufferedImage = new BufferedImage(width + J2SE_JPANEL_INSETS * 2, height + J2SE_JPANEL_INSETS * 2, BufferedImage.TYPE_INT_RGB);
		}else{
			bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
		graphcis = bufferedImage.getGraphics();
		
		scrollPrintRunnable = new Runnable() {
			@Override
			public void run(){
				mlet.print(graphcis);
			}
		};
		listPrintRunnable = new Runnable() {
			@Override
			public void run() {
				listForJComboBox.print(bufferedImageCombo.getGraphics());
			}
		};
	}
	
	@Override
	public void setMlet(final J2SESession coreSS, final Mlet mlet, final ProjectContext projectCtx){
		this.mlet = mlet;
		ServerUIAPIAgent.setProjectContext(mlet, projectCtx);
		projectContext = projectCtx;
		projResp = ServerUIAPIAgent.getProjResponserMaybeNull(projectContext);
		
		frame = new JFrame();//不能入Session会导致block showWindowWithoutWarningBanner
		frameCombobox = new JFrame();
		frameCombobox.getContentPane().setLayout(new BorderLayout());
		
		ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, projResp, new Runnable() {
			@Override
			public void run() {
				scrollPane = new JScrollPane(mlet, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			}
		});
	}
	
	@Override
	public Mlet getMlet(){
		return mlet;
	}
	
	@Override
	public void setScreenIDAndTitle(final String screenID, final String title){
		setCaptureID(screenID, title);
	}
	
	private static boolean isPrintLog = false;
	
	int innerColor1 = 0, innerColor2 = 0, innerColor0 = 0;
	final int shiftStep = 40;

	private final void eraseMletEdge(final int[] data, final int w, final int h){
		if(isPrintLog == false){
			isPrintLog = true;
			L.V = L.O ? false : LogManager.log("tip : in Mlet, there are insets(2, 2, 2, 2) between components and Mlet(JPanel) in edge, Mlet will dark pixels to the edge.");
			L.V = L.O ? false : LogManager.log("important : the insets(2, 2, 2, 2) is ONLY in J2SE server, not in Android server.");
		}
		
		innerColor1 = 0;
		innerColor2 = 0;
		innerColor0 = 0;
		
//		final int innerColor1 = 0xCDCDCD, innerColor2 = 0xEEEEEE;
		
		//第三行copy到第一，二行
		{
			final int endw = w - 2;
			for (int i = 2, k = w + 2, j = w*2 + 2; i < endw;) {
//				data[i++] = data[k++] = data[j++];
				final int colorJ = data[j++];
				
				if(innerColor0 == colorJ){
				}else{
					buildShiftColor(colorJ);
				}
				
				data[i++] = innerColor2;
				data[k++] = innerColor1;
			}
		}
		
		//倒数第三行copy到倒数第一，二行
		{
			int i = w * (h - 1) + 2, k = w * (h - 2) + 2, j = w * (h - 3) + 2;
			final int endW = i + w - 3;
			for (;i < endW;) {
//				data[i++] = data[k++] = data[j++];
				final int colorJ = data[j++];
				
				if(innerColor0 == colorJ){
				}else{
					buildShiftColor(colorJ);
				}
				
				data[i++] = innerColor2;
				data[k++] = innerColor1;
			}
		}
		
		//第三列copy到第一，二列
		{
			int i = w, k = w + 1, j = w + 2;
			final int endh = w * (h - 1);
			for (; i < endh;) {
//				data[i] = data[k] = data[j];
				final int colorJ = data[j];
				
				if(innerColor0 == colorJ){
				}else{
					buildShiftColor(colorJ);
				}
				
				data[i] = innerColor2;
				data[k] = innerColor1;
				j += w;
				k += w;
				i += w;
			}
		}
		
		//倒数第三列copy到倒数第一，二列
		{
			int i = w * 2 - 1, k = w * 2 - 2, j = w * 2 - 3;
			final int endh = w * (h - 1);
			for (; i < endh;) {
	//			data[i] = data[k] = data[j];
				final int colorJ = data[j];
				
				if(innerColor0 == colorJ){
				}else{
					buildShiftColor(colorJ);
				}
				
				data[i] = innerColor2;
				data[k] = innerColor1;
				j += w;
				k += w;
				i += w;
			}
		}
		
		//处理四个角的两点段
		{
			int j = 2;
			buildShiftColor(data[j--]);
			data[j--] = innerColor1;
			data[j] = innerColor2;
			
			j = w - 3;
			buildShiftColor(data[j++]);
			data[j++] = innerColor1;
			data[j] = innerColor2;
			
			j = w * (h - 1) + 2;
			buildShiftColor(data[j--]);
			data[j--] = innerColor1;
			data[j] = innerColor2;
			
			j = w * h - 3;
			buildShiftColor(data[j++]);
			data[j++] = innerColor1;
			data[j] = innerColor2;
		}
	}

	private final void buildShiftColor(final int colorJ) {
		innerColor0 = colorJ;
		
		int r = (colorJ & 0xff0000) >> 16;
		int g = (colorJ & 0xff00) >> 8;
		int b = colorJ & 0xff;
		
		if(r < shiftStep){
			r = 0;
		}else{
			r -= shiftStep;
		}
		if(g < shiftStep){
			g = 0;
		}else{
			g -= shiftStep;
		}
		if(b < shiftStep){
			b = 0;
		}else{
			b -= shiftStep;
		}
		innerColor1 = (r << 16) | (g << 8) | b;
		
		if(r < shiftStep){
			r = 0;
		}else{
			r -= shiftStep;
		}
		if(g < shiftStep){
			g = 0;
		}else{
			g -= shiftStep;
		}
		if(b < shiftStep){
			b = 0;
		}else{
			b -= shiftStep;
		}
		innerColor2 = (r << 16) | (g << 8) | b;
	}
	
	private static final int J2SE_JPANEL_INSETS = 2;
	
	@Override
	public int grabImage(final Rectangle bc, final int[] blockImageData) {
		if(bc.x == 0 && bc.y == 0){
			ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, projResp, scrollPrintRunnable);
			if(isJ2SEPanelInset){
				bufferedImage.getRGB(J2SE_JPANEL_INSETS, J2SE_JPANEL_INSETS, width, height, imageData, 0, width);
//				eraseMletEdge(imageData, width, height);
			}else{
				bufferedImage.getRGB(0, 0, width, height, imageData, 0, width);
			}

			if(isSelectedJComboBox && listForJComboBox != null){
				
				final Dimension d = listForJComboBox.getSize();
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
				ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, projResp, listPrintRunnable);
				
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
				}catch (final Exception e) {
					//忽略超出边界的数据
//					ExceptionReporter.printStackTrace(e);
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
	public final boolean actionInput(final DataInputEvent e) {
		final boolean[] out = new boolean[1];
		
		ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, projResp, new Runnable() {
			@Override
			public void run() {
				out[0] = actionInputInUserThread(e);
			}
		});
		
		return out[0];
	}
	
	/**
	 * 可能重载某些方法，故全inUserThread
	 * @param e
	 * @return
	 */
	private final boolean actionInputInUserThread(final DataInputEvent e) {
		final int eventType = e.getType();
		final int x = e.getX();
		final int y = e.getY();
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("[workbench] inputEvent at x : " + x + ", y : " + y + ", type : " + eventType);
		}
		if(eventType == DataInputEvent.TYPE_TAG_KEY_PRESS_V_SCREEN){
			keyPressed(x, y);//in user thread
			return true;
		}else if(eventType == DataInputEvent.TYPE_TAG_KEY_RELEASE_V_SCREEN){
			keyReleased(x, y);//in user thread
			return true;
		}else if(eventType == DataInputEvent.TYPE_TRANS_TEXT){
			try {
				final String s = e.getTextDataAsString();
				if(currFocusObject instanceof JTextComponent){
					((JTextComponent)currFocusObject).setText(s);
				}else if(currFocusObject instanceof TextComponent){
					((TextComponent)currFocusObject).setText(s);
				}
			}catch (final Exception ex) {
			}
		} else {
			if(eventType == DataInputEvent.TYPE_TAG_POINTER_PRESS_V_SCREEN){
				final Component componentAt = getCtrlComponentAt(null, x, y);
				if(componentAt != null){
					getMletLocation(componentAt, x, y, location);
					pointerPressed(x - location[0], y - location[1], componentAt);
				}
				return true;
			}else if(eventType == DataInputEvent.TYPE_TAG_POINTER_DRAG_V_SCREEN){
				final Component componentAt = getCtrlComponentAt(null, x, y);
				if(componentAt != null){
					getMletLocation(componentAt, x, y, location);
					pointerDragged(x - location[0], y - location[1], componentAt);
				}
				return true;
			}else if(eventType == DataInputEvent.TYPE_TAG_POINTER_RELEASE_V_SCREEN){
				pointerReleased(x, y);
				return true;
			}
		}
		
		return false;
	}
	
	final int[] location = new int[2];
	
	private void getMletLocation(Component parent, final int x, final int y, final int[] loc){
		final Container contain = mlet;//isAndroidServer?mlet:scrolPanel;
		int locX = 0, locY = 0;
		while(parent != contain){
			final Point p = parent.getLocation();
			locX += p.x;
			locY += p.y;
			parent = parent.getParent();
		}
		
		loc[0] = locX;
		loc[1] = locY;
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
	
	@Override
	public void init(){//in user thread
	    frame.setContentPane(scrollPane);
	    
	    if(isAndroidServer){
	    	scrollPane.setPreferredSize(new Dimension(width, height));
	    }else{
	    	mlet.setPreferredSize(new Dimension(width, height));
	    }
		frame.pack();//可能重载某些方法
	}

	private void updateTraveKeys(final int traverKey, final int[] keys) {
		final Set forwardKeys = mlet.getFocusTraversalKeys(
			    traverKey);
		
		final Set newForwardKeys = new HashSet(forwardKeys);
		
		for (int i = 0; i < keys.length; i++) {
			newForwardKeys.add(KeyStroke.getKeyStroke(keys[i], 0));
		}
		
		mlet.setFocusTraversalKeys(traverKey, newForwardKeys);
	}
	
	public final void keyPressed(final int keyStates, final int gameAction){//in user thread
		doKeyAction(keyStates, gameAction, true);//in user thread
	}

	public final void keyReleased(final int keyCode, final int gameAction){
//		doKeyAction(keyCode, gameAction, false);		
//		ctx.run(new Runnable() {
//			@Override
//			public void run() {
//			}
//		});
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
	
	private Component focusNext(final Component c, final boolean isContainIn){//in user thread
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
	
	private Component focusNext(final Component c) {//in user thread
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
					return focusNext(nextFocus, true);//in user thread
				}
			}else{
				return null;
			}
		}
		
	    final Container root = c.getParent();
	    final int idx = root.getComponentZOrder(c);
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

	private Component focusPrevious(final Component c, final boolean isContainIn) {
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
		
	    final Container root = c.getParent();
	    final int idx = root.getComponentZOrder(c);
	    
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

	private void doKeyAction(final int keyStates, final int gameAction, final boolean isPressNotRelease) {//in user thread
		if (gameAction == LEFT || keyStates == KEY_NUM4) {
			focusPrevious(currFocusObject);
		} else if (gameAction == RIGHT || keyStates == KEY_NUM6) {
			focusNext(currFocusObject);//in user thread
		} else if (gameAction == UP || keyStates == KEY_NUM2) {
			focusPrevious(currFocusObject);
		} else if (gameAction == DOWN || keyStates == KEY_NUM8) {
			focusNext(currFocusObject);//in user thread
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
	
	public final void pointerPressed(final int x, final int y, final Component componentAt){
//		L.V = L.O ? false : LogManager.log("Pressed on MCanvas at x:" + x + ", y:" + y);
	    //先执行focusLost
	    if(currFocusObject != null && currFocusObject != componentAt){
	    	loseFocus(currFocusObject);
	    }
	    //再执行focusGained
		focus(componentAt);
		
		final MouseListener[] ml = componentAt.getListeners(MouseListener.class);
		if(ml.length == 0){
			return;
		}
		
		final MouseEvent event = new MouseEvent(componentAt, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
	    		MouseEvent.BUTTON1_MASK, x, y, 1, false);
		
		for (int i = getStartMouseListenerIdx(componentAt); i < ml.length; i++) {
			ml[i].mousePressed(event);
		}
	}
	
	private Component currFocusObject;
	
	private Component getCtrlComponentAt(Component contain, final int x, final int y){
//		System.out.println("getCtrlComponentAt contain class : " + contain.getClass().getName());
		if(contain == null){
			contain = mlet;//isAndroidServer?mlet:scrolPanel;
		}
		
		if(contain instanceof JTabbedPane){
			final JTabbedPane tabPane = (JTabbedPane)contain;
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
			final JTabbedPane tabPane = (JTabbedPane)contain;
			contain = tabPane.getComponentAt(tabPane.getSelectedIndex());
			return getInitFocusComponentAt(contain);
		}
		if(contain instanceof Container && ((Container)contain).getComponentCount() > 0){
			final int size = ((Container)contain).getComponentCount();
			for (int i = 0; i < size; i++) {
				final Component c = getInitFocusComponentAt(((Container)contain).getComponent(i));
				if(c != null){
					return c;
				}
			}
			return null;
		}else if(contain instanceof Component){
			if(contain.hasFocus()){
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
	
	public final void pointerReleased(final int x, final int y){
		doPointerReleased(x, y);
	}
	
	private void doPointerReleased(final int x, final int y){
		if(isSelectedJComboBox){
			final int comboX = x - locComboX, comboY = y - locComboY;
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
	    final Component componentAt = getCtrlComponentAt(null, x, y);//注意：不能是mlet，因为有可能出现Scroll情形
	    if(componentAt == null){
	    	return;
	    }
		
		if(componentAt instanceof JComboBox){
			oriComboBox = (JComboBox)componentAt;
			final JList list = new JList(oriComboBox.getModel());
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
			final Point l = getRelationLocation(frame.getContentPane(), componentAt);
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
			final int[] locs = new int[2];
			getMletLocation(componentAt, x, y, locs);
			
			//然后执行MouseEvent，如果同一个组件注册多种类型事件侦听器，则可能被触发多次类型。
		    final MouseEvent e = new MouseEvent(componentAt, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
		    		MouseEvent.BUTTON1_MASK, x - locs[0], y - locs[1], 1, false);

//		    L.V = L.O ? false : LogManager.log("Released on MCanvas at x:" + e.getX() + ", y:" + e.getY());

		    
		    processClickOnComponent(componentAt, e);
		}
	}
	
	public static void processClickOnComponent(final Component comp, final AWTEvent e){
		//支持JList 
		//先执行Actionlistener
		doActon(comp);
		
	    if(dispatchEvent(comp, e) == false){
//	    	以下代码会产生权限错误，暂停
//	    	frame.getToolkit().getSystemEventQueue().postEvent(e);
	    	comp.dispatchEvent(e);//可能不能执行如MouseEvent
	    }
	}
	
	private Point getRelationLocation(final Component topCorn, final Component sub){
		final Container parent = sub.getParent();
		final Point location = sub.getLocation();
		if(parent == topCorn || sub == topCorn){
			return location;
		}else{
			final Point diff = getRelationLocation(topCorn, parent);
			location.x += diff.x;
			location.y += diff.y;
			return location;
		}
	}

	private void focus(final Component componentAt) {
		if(currFocusObject == componentAt){
			if(isEditableComponent(componentAt)){
				//发送编辑文本指令到手机
				HCURLUtil.sendGoPara(coreSS, HCURL.DATA_PARA_INPUT, "all");//all表示支持全部类型
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

	public static boolean doActon(final Component componentAt) {
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
		}else if(componentAt instanceof JComboBox){
			final ActionEvent actionEvent = new ActionEvent( componentAt, ActionEvent.ACTION_PERFORMED, 
					((JComboBox)componentAt).getActionCommand() );
			if(dispatchEvent(componentAt, actionEvent)){
				return true;
			}
		}else if(componentAt instanceof JTextField){
			final ActionEvent actionEvent = new ActionEvent( componentAt, ActionEvent.ACTION_PERFORMED, 
					((JTextField)componentAt).getText() );
			if(dispatchEvent(componentAt, actionEvent)){
				return true;
			}
		}
		return false;
	}
	
	public static boolean dispatchEvent(final Component component, final AWTEvent event){
		if(event instanceof FocusEvent){
			final FocusListener[] fl = component.getListeners(FocusListener.class);
			if(fl.length == 0){
				return false;
			}
			for (int i = 0; i < fl.length; i++) {
				fl[i].focusGained((FocusEvent)event);
			}
			return true;
		}else if(event instanceof ActionEvent){
			final ActionListener[] al = component.getListeners(ActionListener.class);
			if(al.length == 0){
				return false;
			}
			for (int i = 0; i < al.length; i++) {
				al[i].actionPerformed((ActionEvent)event);
			}
			return true;
		}else if(event instanceof MouseEvent){
			final MouseListener[] ml = component.getListeners(MouseListener.class);
			if(ml.length == 0){
				return false;
			}
			
			//注意：从1开始。因为第0个是AquaButtonListener。它会触发actionListener
			for (int i = getStartMouseListenerIdx(component); i < ml.length; i++) {
				final MouseListener mouseListener = ml[i];
//				mouseListener.mousePressed((MouseEvent)event);//本逻辑移到pointerPressed
				mouseListener.mouseReleased((MouseEvent)event);
				mouseListener.mouseClicked((MouseEvent)event);			
			}

			return true;
		}else if(event instanceof java.awt.event.KeyEvent){
			final KeyListener[] kl = component.getKeyListeners();
			if(kl.length == 0){
				return false;
			}
			for (int i = 0; i < kl.length; i++) {
				kl[i].keyTyped((java.awt.event.KeyEvent)event);
			}
			return true;
		}else{
			
		}
		return false;
	}
	
	private static final int getStartMouseListenerIdx(final Component componentAt){
		if(isAndroidServer){
			return 0;
		}else{
			if(componentAt instanceof AbstractButton){
				return 1;
			}else{
				return 0;
			}
		}
	}
	
	public final void pointerDragged(final int x, final int y, final Component componentAt){
		L.V = L.O ? false : LogManager.log("Dragged on MCanvas at x:" + x + ", y:" + y);
		
		final MouseMotionListener[] ml = componentAt.getListeners(MouseMotionListener.class);
		if(ml.length == 0){
			return;
		}

		final MouseEvent event = new MouseEvent(componentAt, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
	    		MouseEvent.BUTTON1_MASK, x, y, 1, false);

		for (int i = 0; i < ml.length; i++) {
			final MouseMotionListener mouseListener = ml[i];
//					mouseListener.mouseMoved((MouseEvent)event);
			mouseListener.mouseDragged(event);
		}
	}
	
	@Override
	public void onExit() {
		onExit(false);
	}
	
	@Override
	public void onExit(final boolean isAutoReleaseAfterGo){
		ScreenServer.onExitForMlet(coreSS, projectContext, mlet, isAutoReleaseAfterGo);
		MultiUsingManager.exit(coreSS, projectContext.getProjectID(), mlet.getTarget());
		
		frame.dispose();
		frameCombobox.dispose();
		
		super.onExit();
	}

	@Override
	public void onStart() {
		super.start();

		ScreenServer.onStartForMlet(coreSS, projectContext, mlet);
	}

	@Override
	public void onPause() {
		enableStopCap(true);
		ScreenServer.onPauseForMlet(coreSS, projectContext, mlet);
	}

	@Override
	public void onResume() {
		enableStopCap(false);
		ScreenServer.onResumeForMlet(coreSS, projectContext, mlet);
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

	@Override
	public void actionJSInput(final byte[] bs, final int offset, final int len) {
		//无需实现，因为走actionInput(DataInputEvent)
	}

	@Override
	public boolean isSameScreenID(final byte[] bs, final int offset, final int len) {
		if(len == pngCaptureID.length){
			for (int i = 0; i < pngCaptureID.length; i++) {
				if(pngCaptureID[i] != bs[offset + i]){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isSameScreenIDIgnoreCase(final char[] chars, final int offset, final int len){
		if(len == pngCaptureIDChars.length){
			for (int i = 0; i < len; i++) {
				final char c1 = pngCaptureIDChars[i];
				final char c2 = chars[offset + i];
				if(c1 == c2
						|| Character.toUpperCase(c1) == Character.toUpperCase(c2)
						|| Character.toLowerCase(c1) == Character.toLowerCase(c2)){
				}else{
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
}