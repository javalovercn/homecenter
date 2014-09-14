package hc.server.data.screen;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootServerConnector;
import hc.core.data.DataInputEvent;
import hc.core.sip.SIPManager;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.ThumbnailHelper;
import hc.core.util.TimeWatcher;
import hc.server.AbstractDelayBiz;
import hc.server.DelayServer;
import hc.server.data.DAOKeyComper;
import hc.server.data.KeyComperPanel;
import hc.server.ui.SingleMessageNotify;
import hc.server.util.IDArrayGroup;
import hc.util.ResourceUtil;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.peer.RobotPeer;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Vector;

import sun.awt.ComponentFactory;

public class ScreenCapturer extends PNGCapturer{
	final private Rectangle moveNewRect = new Rectangle();

	private final int screenWidth, screenHeigh;
	public static Object robotPeer;
	public static Robot robot;
	public KeyComper mobiUserIcons = null;
	private final TimeWatcher timeWatcher;
	private boolean clearCacheThumbnail = false;
	
	public void clearCacheThumbnail(){
		clearCacheThumbnail = true;
	}
	
	static {
		try {
			robot = new Robot();
			robot.setAutoDelay(50);
		    robot.setAutoWaitForIdle(true);
			robotPeer = ((ComponentFactory) Toolkit.getDefaultToolkit())
					.createRobot(robot, GraphicsEnvironment
							.getLocalGraphicsEnvironment()
							.getDefaultScreenDevice());
		} catch (Throwable e) {
			//以上不能catch Exception
			LogManager.err("Not Found : java.awt.peer.RobotPeer(Sun API), use java.awt.Robot");
		}
	}
	
	/**
	 * 初始化扩展自定义按钮
	 */
	public void initExtMobiIcon(){
		DAOKeyComper.getInstance(this);
	}

	public ScreenCapturer(final int clientWidth, final int clientHeight, final int pcWidth, final int pcHeight) {
		super(clientWidth, clientHeight, true, 0);
	
		timeWatcher = new TimeWatcher(60 * 1000) {//超过一分钟
			@Override
			public void doBiz() {
				//检查缓存是否全黑
				randomCheckLock();
			}
		};
		
		final Thread t = new Thread(){
			//发送远屏缩略图
			public void run(){
				int[] outSize = ThumbnailHelper.calcThumbnail(clientWidth, clientHeight, pcWidth, pcHeight);
				final int thumbnailWidth = outSize[0];
				final int thumbnailHeight = outSize[1];
				
				final Rectangle sc = new Rectangle(0, 0, pcWidth, pcHeight);
				final int thumbnailSize = thumbnailWidth * thumbnailHeight;
				final int[] thumbnailRGBData = new int[thumbnailSize];
				final int[] screenRGBData = new int[thumbnailRGBData.length];
				
//				final byte[] thumbnail_bs = StringUtil.getBytes(HCURL.THUMBNAIL_SCREEN);
				
				//变化最小系数，当变化大于此值时，重新传送缩略图
				final int modiNum = 10;
				final int minModiNum = thumbnailSize / modiNum;
				
				try{
					while(!isShutDown){
						Thread.sleep(5000);
						
						BufferedImage screen;
						
						//加锁，以防并用对象robot
						synchronized (LOCK) {
							screen = robot.createScreenCapture(sc);
						}
						
						screen = ResourceUtil.resizeImage(screen, thumbnailWidth, thumbnailHeight);
						screen.getRGB(0, 0, thumbnailWidth, thumbnailHeight, screenRGBData, 0, thumbnailWidth);
						
						//降低色彩度
						for (int idxRGB = 0; idxRGB < thumbnailSize; idxRGB++) {
							screenRGBData[idxRGB] &= mask;
						}
						
						if(clearCacheThumbnail){
							for (int i = 0; i < thumbnailRGBData.length; i++) {
								thumbnailRGBData[i] = 0;
							}
							clearCacheThumbnail = false;
						}
						
//						L.V = L.O ? false : LogManager.log("send thumbnail.");
						int modiCount = 0;
						for (int idxRGB = 0; idxRGB < thumbnailSize; idxRGB++) {
							if(screenRGBData[idxRGB] != thumbnailRGBData[idxRGB]){
								if(++modiCount >= minModiNum){
									System.arraycopy(screenRGBData, 0, thumbnailRGBData, 0, thumbnailSize);
									
									sendBlock(0, 0, thumbnailRGBData, thumbnailSize, thumbnailWidth, thumbnailHeight, false, MsgBuilder.E_IMAGE_PNG_THUMBNAIL);
									break;
								}
							}
						}
					}
				}catch (Exception e) {
				}
			}
		};
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
		
		//初始化扩展自定义按钮
		initExtMobiIcon();
		
		screenWidth = pcWidth;
		screenHeigh = pcHeight;
		
		{
			KeyComperPanel.sendExtMouse();
		}

		setCaptureID(HCURL.REMOTE_HOME_SCREEN);
		
		hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + "Screen [" + HCURL.REMOTE_HOME_SCREEN + "] start");
	
		{
			CLIENT_WIDTH = clientWidth/2;//初始时要拆半
			CLIENT_HEIGHT = clientHeight/2;//初始时要拆半
			
			initMobileArea();
			
			rematchRect();
		}
	}

	/**
	 * 更新capRect和movNewRect的x,y,width, height
	 */
	private void rematchRect() {
		capRect.x = locX;
		capRect.y = locY;
		matchRect(capRect);
		
		moveNewRect.x = locX;
		moveNewRect.y = locY;
		matchRect(moveNewRect);
	}
	
	private boolean isZoomIn = false;
	
	private int convertZoomUnzoomXY(int xy){
		return isZoomIn?xy/2:xy*2;
	}
	
	private void initMobileArea() {
//		L.V = L.O ? false : LogManager.log("is ZoomIn : " + isZoomIn);
		
		CLIENT_WIDTH = convertZoomUnzoomXY(CLIENT_WIDTH);
		CLIENT_HEIGHT = convertZoomUnzoomXY(CLIENT_HEIGHT);
		
		locX = 0;
		int by = screenHeigh - CLIENT_HEIGHT;
		if(by < 0){
			BOTTOM_Y = 0;
		}else{
			BOTTOM_Y = by;
		}
		int rx = screenWidth - CLIENT_WIDTH;
		if(rx < 0){
			MAX_RIGHT_X = 0;
		}else{
			MAX_RIGHT_X = rx;
		}
		
		locY = BOTTOM_Y;
	}
	
	@Override
	public void run(){
		sleepBeforeRun();

		notifyScreenMode();

		cycleCapture();
	}
	
	public static boolean isDirectServerMode(){
		if(SIPManager.isOnRelay() == false){
			short connMode = ContextManager.getConnectionModeStatus();
			if(connMode == ContextManager.MODE_CONNECTION_HOME_WIRELESS){
				return true;
			}
		}
		return false;
	}
	
	private void notifyScreenMode(){
		if(isDirectServerMode()){
			if(IDArrayGroup.checkAndAdd(IDArrayGroup.MSG_NOTIFIED_SERVER_DIRECT_MODE) == false){
			}else{
				L.V = L.O ? false : LogManager.log("Detect Server is in direct mode, notify mobile.");
				ContextManager.getContextInstance().send(null, MsgBuilder.E_TAG_ROOT, 
						MsgBuilder.DATA_ROOT_SERVER_IN_DIRECT_MODE);
			}
		}
		
		randomCheckLock();
	}

	private void randomCheckLock() {
		final Random r = new Random(System.currentTimeMillis());
		final Rectangle testMinBlock = new Rectangle(r.nextInt(screenWidth - MIN_BLOCK_CAP), 
				r.nextInt(screenHeigh - MIN_BLOCK_CAP), MIN_BLOCK_CAP, MIN_BLOCK_CAP);
		if(isSameColorBlock(testMinBlock, rgb)){
			isSameFullScreen();
		}
	}

	private void isSameFullScreen() {
		//产生一个模拟事件，以解除如屏保而导致的可自解锁型
		L.V = L.O ? false : LogManager.log("Screen maybe in lock or screensave, create ctrl push event to active screen.");
		
//		Point mousepoint = MouseInfo.getPointerInfo().getLocation();  
//		robot.mouseMove(0, 0);//会导致弹出移动，最小化菜单，故关闭
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyRelease(KeyEvent.VK_CONTROL);
//		robot.mouseMove(mousepoint.x, mousepoint.y);
		
		DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null) {
			@Override
			public void doBiz() {
				try{
					Thread.sleep(500);
				}catch (Exception e) {
				}
				isSameFullScreenDelay();
			}
		});
	}
	private void isSameFullScreenDelay() {
		final Rectangle testMinBlock = new Rectangle();
		//进行全屏测试
		testMinBlock.x = 0;
		testMinBlock.y = 0;
		testMinBlock.width = screenWidth;
		testMinBlock.height = screenHeigh;
		
		try{
			final int[] fullScreenRGB = new int[screenWidth * screenHeigh];
			
			if(isSameColorBlock(testMinBlock, fullScreenRGB)){
				LogManager.errToLog("Screen is in lock, no image is sended to mobile.");
//				LogManager.errToLog("  screen is locked after pressed Win + L or screen save is triggered,");
				LogManager.errToLog("  mobile displays black if server in lock mode in some older OS");
				LogManager.errToLog("  please disable screen lock.");
				ContextManager.getContextInstance().send(null, MsgBuilder.E_TAG_ROOT, 
						MsgBuilder.DATA_ROOT_OS_IN_LOCK);
						
				SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_SCR_LOCKING, 
						"In some older OS and JRE, mobile displays black if screen is locked!<BR><BR>Please disable screen locking or screen saver.", 
						"Screen is LOCK!!!", SingleMessageNotify.NEVER_AUTO_CLOSE, App.getSysIcon(App.SYS_INFO_ICON));
				
				RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_LockScreen_STR);
			}
		}catch (Throwable e) {
			LogManager.errToLog("Fail check screen lock mode.");
			e.printStackTrace();
		}
	}

	private boolean isSameColorBlock(final Rectangle testMinBlock, final int[] colorRGB) {
		final int len = grabImage(testMinBlock, colorRGB);
		return isLockScreenColorBlock(colorRGB, len);
	}

	private boolean isLockScreenColorBlock(final int[] colorRGB, final int len) {
		final int firstColor = colorRGB[0];

		boolean isSame = true;
		for (int i = 1; i < len; i++) {
			if(colorRGB[i] != firstColor){
				isSame = false;
				break;
			}
		}
		return isSame;
	}

	public boolean actionInput(DataInputEvent e){
		final int type = e.getType();
		final int pointX = e.getX();
		final int pointY = e.getY();
		if(type == DataInputEvent.TYPE_MOUSE_MOVE){
			robot.mouseMove(pointX, pointY);
			L.V = L.O ? false : LogManager.log(OP_STR + "Mouse move to (" + pointX + ", " + pointY + ")");
		}else if(type == DataInputEvent.TYPE_MOUSE_LEFT_CLICK){
			hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + "Mouse left click at (" + pointX + ", " + pointY + ")");
			robot.mouseMove(pointX, pointY);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}else if(type == DataInputEvent.TYPE_MOUSE_RIGHT_CLICK){
			hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + "Mouse right click at (" + pointX + ", " + pointY + ")");
			robot.mouseMove(pointX, pointY);
			robot.mousePress(InputEvent.BUTTON3_MASK);
			robot.mouseRelease(InputEvent.BUTTON3_MASK);			
		}else if(type == DataInputEvent.TYPE_MOUSE_DOUBLE_CLICK){
			hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + "Mouse double click at (" + pointX + ", " + pointY + ")");
			
			robot.mouseMove(pointX, pointY);
			
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);			
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);			
		}else if(type == DataInputEvent.TYPE_TRANS_TEXT){
			try {
				String s = e.getTextDataAsString();
				if(inputKeyStr(s) == false){
					hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + "Client paste txt:[" + s + "]!");
					sendToClipboard(s);
					ctrlSomeKey(KeyEvent.VK_V);
				}else{
					hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + "Client input txt:[" + s + "]!");
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}else if(type == DataInputEvent.TYPE_BACKSPACE){
			backspace();
			L.V = L.O ? false : LogManager.log(OP_STR + "Press BACKSPACE");
		}else if(type == DataInputEvent.TYPE_TAG_TRANS_TEXT_2_MOBI){
			copytxtToMobi();
		}else if(type == DataInputEvent.TYPE_TAG_INPUT_KEYBOARD_FROM_MOBI){
			L.V = L.O ? false : LogManager.log(OP_STR + "Press a mobile keyboard char");
			type(robot, (char)pointX);
		}else{
			//自定义扩展MouseIcon
			int idx = type - DataInputEvent.TYPE_TAG_TRANS_TEXT_2_MOBI;
			
			if(mobiUserIcons != null){
				Vector<Integer> vInt = mobiUserIcons.getKeys(idx);
				if(vInt == null){
					L.V = L.O ? false : LogManager.log("Unknow ext icon idx:" + type);
				}else{
					KeyComper.keyAction(robot, vInt);
				}
			}
		}
		
//		mttRefreshAfterInput();
		return true;
	}

	public static void type(Robot r, char character) {
        switch (character) {
        case '\b': doType(r, KeyEvent.VK_BACK_SPACE); break;
        case 'a': doType(r, KeyEvent.VK_A); break;
        case 'b': doType(r, KeyEvent.VK_B); break;
        case 'c': doType(r, KeyEvent.VK_C); break;
        case 'd': doType(r, KeyEvent.VK_D); break;
        case 'e': doType(r, KeyEvent.VK_E); break;
        case 'f': doType(r, KeyEvent.VK_F); break;
        case 'g': doType(r, KeyEvent.VK_G); break;
        case 'h': doType(r, KeyEvent.VK_H); break;
        case 'i': doType(r, KeyEvent.VK_I); break;
        case 'j': doType(r, KeyEvent.VK_J); break;
        case 'k': doType(r, KeyEvent.VK_K); break;
        case 'l': doType(r, KeyEvent.VK_L); break;
        case 'm': doType(r, KeyEvent.VK_M); break;
        case 'n': doType(r, KeyEvent.VK_N); break;
        case 'o': doType(r, KeyEvent.VK_O); break;
        case 'p': doType(r, KeyEvent.VK_P); break;
        case 'q': doType(r, KeyEvent.VK_Q); break;
        case 'r': doType(r, KeyEvent.VK_R); break;
        case 's': doType(r, KeyEvent.VK_S); break;
        case 't': doType(r, KeyEvent.VK_T); break;
        case 'u': doType(r, KeyEvent.VK_U); break;
        case 'v': doType(r, KeyEvent.VK_V); break;
        case 'w': doType(r, KeyEvent.VK_W); break;
        case 'x': doType(r, KeyEvent.VK_X); break;
        case 'y': doType(r, KeyEvent.VK_Y); break;
        case 'z': doType(r, KeyEvent.VK_Z); break;
        case 'A': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_A); break;
        case 'B': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_B); break;
        case 'C': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_C); break;
        case 'D': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_D); break;
        case 'E': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_E); break;
        case 'F': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_F); break;
        case 'G': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_G); break;
        case 'H': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_H); break;
        case 'I': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_I); break;
        case 'J': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_J); break;
        case 'K': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_K); break;
        case 'L': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_L); break;
        case 'M': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_M); break;
        case 'N': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_N); break;
        case 'O': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_O); break;
        case 'P': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_P); break;
        case 'Q': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_Q); break;
        case 'R': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_R); break;
        case 'S': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_S); break;
        case 'T': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_T); break;
        case 'U': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_U); break;
        case 'V': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_V); break;
        case 'W': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_W); break;
        case 'X': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_X); break;
        case 'Y': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_Y); break;
        case 'Z': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_Z); break;
        case '`': doType(r, KeyEvent.VK_BACK_QUOTE); break;
        case '0': doType(r, KeyEvent.VK_0); break;
        case '1': doType(r, KeyEvent.VK_1); break;
        case '2': doType(r, KeyEvent.VK_2); break;
        case '3': doType(r, KeyEvent.VK_3); break;
        case '4': doType(r, KeyEvent.VK_4); break;
        case '5': doType(r, KeyEvent.VK_5); break;
        case '6': doType(r, KeyEvent.VK_6); break;
        case '7': doType(r, KeyEvent.VK_7); break;
        case '8': doType(r, KeyEvent.VK_8); break;
        case '9': doType(r, KeyEvent.VK_9); break;
        case '-': doType(r, KeyEvent.VK_MINUS); break;
        case '=': doType(r, KeyEvent.VK_EQUALS); break;
        case '~': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_QUOTE); break;
        case '!': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_1); break;
        case '@': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_2); break;
        case '#': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_3); break;
        case '$': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_4); break;
        case '%': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_5); break;
        case '^': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_6); break;
        case '&': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_7); break;
        case '*': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_8); break;
        case '(': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_9); break;
        case ')': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_0); break;
        case '_': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_MINUS); break;
        case '+': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_EQUALS); break;
        case '\t': doType(r, KeyEvent.VK_TAB); break;
        case '\n': doType(r, KeyEvent.VK_ENTER); break;
        case '[': doType(r, KeyEvent.VK_OPEN_BRACKET); break;
        case ']': doType(r, KeyEvent.VK_CLOSE_BRACKET); break;
        case '\\': doType(r, KeyEvent.VK_BACK_SLASH); break;
        case '{': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_OPEN_BRACKET); break;
        case '}': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_CLOSE_BRACKET); break;
        case '|': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_BACK_SLASH); break;
        case ';': doType(r, KeyEvent.VK_SEMICOLON); break;
        case ':': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_SEMICOLON); break;
        case '\'': doType(r, KeyEvent.VK_QUOTE); break;
        case '"': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_QUOTE); break;
        case ',': doType(r, KeyEvent.VK_COMMA); break;
        case '<': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_COMMA); break;
        case '.': doType(r, KeyEvent.VK_PERIOD); break;
        case '>': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_PERIOD); break;
        case '/': doType(r, KeyEvent.VK_SLASH); break;
        case '?': doType(r, KeyEvent.VK_SHIFT, KeyEvent.VK_SLASH); break;
        case ' ': doType(r, KeyEvent.VK_SPACE); break;
        default:
                throw new IllegalArgumentException("Cannot type character " + character);
        }
    }
	
	private static void doType(Robot r, int keyCode) {
        r.keyPress(keyCode);
        r.keyRelease(keyCode);
    }
	
	private static void doType(Robot r, int keyCode1, int keyCode2) {
        r.keyPress(keyCode1);

        r.keyPress(keyCode2);
        r.keyRelease(keyCode2);

        r.keyRelease(keyCode1);
    }
	
	private boolean inputKeyStr(String txt){
		char[] chars = txt.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			final char testChar = chars[i];
			boolean isFinded = false;
			for (int j = 0; j < KeyComperPanel.DIRECT_KEYS.length; j++) {
				if(testChar == KeyComperPanel.DIRECT_KEYS[j]){
					isFinded = true;
					break;
				}
			}
			if(isFinded == false){
				//含有不适合键盘输入的字符
				return false;
			}
		}
		
		//全部符合键盘输入
		for (int i = 0; i < chars.length; i++) {
			type(robot, chars[i]);
		}
		return true;
	}
	
	private void ctrlSomeKey(int key) {
		final int abstractCtrlKeyCode = ResourceUtil.getAbstractCtrlKeyCode();
		robot.keyPress(abstractCtrlKeyCode);
		robot.keyPress(key);
		robot.keyRelease(key);
		robot.keyRelease(abstractCtrlKeyCode);
	}
	
	public static void sendToClipboard(String to_url) {
		Clipboard clipbd = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringSelection clipString = new StringSelection(to_url);
		clipbd.setContents(clipString, clipString);
	}
	
	private static String getTxtFromClipboard(){
		Clipboard clipbd = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable clipT = clipbd.getContents(null);
		String d = null;
		if (clipT != null) {
			// 检查内容是否是文本类型
			if (clipT.isDataFlavorSupported(DataFlavor.stringFlavor)){
				try {
					d = (String) clipT.getTransferData(DataFlavor.stringFlavor);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if(d == null){
			return "";
		}else{
			return d.trim();
		}
	}
	
	public void backspace(){
		robot.keyPress(KeyEvent.VK_BACK_SPACE);
		robot.keyRelease(KeyEvent.VK_BACK_SPACE);
	}
	
	public void copytxtToMobi(){
		ctrlSomeKey(KeyEvent.VK_C);
		
		String txt = getTxtFromClipboard();
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("User ready copyTxtToMobi:" + txt);
		if(txt.length() > 0){
    		DataInputEvent e = new DataInputEvent();
			try {
	    		byte[] txt_bs = txt.getBytes(IConstant.UTF_8);
	    		
	    		final byte[] txtToMobiBS = new byte[DataInputEvent.text_index + DataInputEvent.MAX_MOBI_UI_TXT_LEN];
	    		e.setBytes(txtToMobiBS);
	    		e.setType(DataInputEvent.TYPE_TRANS_TEXT);

	    		e.setTextData(txt_bs, 0, txt_bs.length);
	    		ContextManager.getContextInstance().send(
	    				MsgBuilder.E_INPUT_EVENT, txtToMobiBS, e.getLength());
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
		}
		hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + "copyTxtToMobi:" + ((txt.length()==0)?"null":txt));
	}
	
	public void dragAndDrop(int startX, int startY, int endX, int endY){
		robot.mouseMove(startX, startY);
		robot.mousePress(InputEvent.BUTTON1_MASK);
		
		robot.mouseMove(endX, endY);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
		
//		ctrlSomeKey(KeyEvent.VK_C);
		
		hc.core.L.V=hc.core.L.O?false:LogManager.log(OP_STR + "drag from ["+startX + ", " + startY+"] to [" + endX + ", " + endY + "]");
	}
	
	public void zoom(final int type){
		synchronized (LOCK) {
			int oldLocX = locX, oldLocY = locY;
			
			int mobileW = 0, mobileH = 0;
			if(type == MsgBuilder.DATA_ZOOM_IN){
				mobileW = CLIENT_WIDTH;
				mobileH = CLIENT_HEIGHT;
				isZoomIn = true;
				initMobileArea();
			}else if(type == MsgBuilder.DATA_ZOOM_OUT){
				isZoomIn = false;
				initMobileArea();
				mobileW = CLIENT_WIDTH;
				mobileH = CLIENT_HEIGHT;
			}
			
			final int[] newLocXY = ThumbnailHelper.calNewLocXY(isZoomIn, oldLocX, oldLocY, screenWidth, screenHeigh, mobileW, mobileH);
			
			locX = newLocXY[0];
			locY = newLocXY[1];
			
//			L.V = L.O ? false : LogManager.log("oldLocX : " + oldLocX + ", oldLocY : " + oldLocY + ", locX : " + locX + ", locY : " + locY);
			
			rematchRect();
			
			initClientSnap();
		}
		
		try{
			sendPNG(capRect, CLIENT_WIDTH, false);
		}catch (Exception e) {
			//考虑数据溢出，故忽略
		}
	}

	public void moveUp(final int pixle){
		if(pixle > 0){
			L.V = L.O ? false : LogManager.log(OP_STR + "moveUp : " + pixle);
		}else{
			L.V = L.O ? false : LogManager.log(OP_STR + "moveDown : " + (-pixle));
		}
		
		moveNewRect.x = capRect.x;
		moveNewRect.y = capRect.y;

		final int newLocY = locY - pixle;
		if(newLocY < 0){
			locY = 0;
		}else if(newLocY > BOTTOM_Y){
			locY = BOTTOM_Y;
		}else{
			locY = newLocY;
		}
		
		synchronized (LOCK) {
			if(pixle > 0){
				moveNewRect.height = pixle;
				moveNewRect.y = locY;
				
				//************************
				//************************
				//************************
				//------------------------
				final int len = (CLIENT_HEIGHT - pixle) * CLIENT_WIDTH;
				int fromRowIdx = len - 1;
				for (int idxEnd = CLIENT_HEIGHT * CLIENT_WIDTH - 1; fromRowIdx >= 0;) {
					clientSnap[idxEnd--] = clientSnap[fromRowIdx--];
				}
				
				for (int i = 0, lenM = pixle * CLIENT_WIDTH; i < lenM; ) {
					clientSnap[i++] = DEFAULT_BACK_COLOR;
				}
			}else{
				moveNewRect.height = pixle * (-1);
				moveNewRect.y = locY + CLIENT_HEIGHT + pixle;
				
				//------------------------
				//************************
				//************************
				//************************
				final int len = (CLIENT_HEIGHT + pixle) * CLIENT_WIDTH;
				int fromRowIdx = (-pixle) * CLIENT_WIDTH;
				for (int i = 0; i < len;) {
					clientSnap[i++] = clientSnap[fromRowIdx++];
				}
				
				final int toIdx = (CLIENT_HEIGHT + pixle) * CLIENT_WIDTH;
				for (int i = toIdx, end = CLIENT_HEIGHT * CLIENT_WIDTH; i < end; ) {
					clientSnap[i++] = DEFAULT_BACK_COLOR;
				}
			}
	
			capRect.y = locY;
			
			moveNewRect.width = CLIENT_WIDTH;
	//		将新出生的空白块，发出
			sendPNG(cutBlackBorder(moveNewRect), CLIENT_WIDTH, false);
		}
		timeWatcher.watchTrigger();		
	}
	
	public void moveRight(final int pixle){		
		if(pixle > 0){
			L.V = L.O ? false : LogManager.log(OP_STR + "moveRight : " + pixle);
		}else{
			L.V = L.O ? false : LogManager.log(OP_STR + "moveLeft : " + (-pixle));
		}
		
		moveNewRect.x = capRect.x;
		moveNewRect.y = capRect.y;
		
		final int newLocX = locX + pixle;
		if(newLocX < 0){
			locX = 0;
		}else if(newLocX > MAX_RIGHT_X){
			locX = MAX_RIGHT_X;
		}else{
			locX = newLocX;
		}

		synchronized (LOCK) {		
			if(pixle > 0){
				moveNewRect.width = pixle;
				moveNewRect.x = locX + CLIENT_WIDTH - pixle;

				for (int row = 0, RowLen = CLIENT_HEIGHT; row < RowLen; row++) {
					final int idxR = row * CLIENT_WIDTH;
					int idxM = idxR + pixle;
					//--********************
					for (int x = idxR, end = idxR + CLIENT_WIDTH - pixle; x >= 0 && x < end;) {
//						if(row == 0){
//							L.V = L.O ? false : LogManager.log("mov " + idxM + " to " + x);
//						}
						clientSnap[x++] = clientSnap[idxM++];
					}
					for (int end = idxR + CLIENT_WIDTH, i = end - pixle; i >= 0 && i < end; i++) {
//						if(row == 0){
//							L.V = L.O ? false : LogManager.log("reset color idx " + i);
//						}
						clientSnap[i] = DEFAULT_BACK_COLOR;
					}
				}
			}else{
				moveNewRect.width = pixle * (-1);
				moveNewRect.x = locX;

				for (int row = 0, RowLen = CLIENT_HEIGHT; row < RowLen; row++) {
					final int idxR = row * CLIENT_WIDTH;
					int idxM = idxR + CLIENT_WIDTH + pixle - 1;
					//********************--
					for (int x = idxR + CLIENT_WIDTH - 1; idxM >= idxR;) {
						clientSnap[x--] = clientSnap[idxM--];
					}
					for (int i = idxR, len = idxR-pixle; i >= 0 && i < len; i++) {
						clientSnap[i] = DEFAULT_BACK_COLOR;
					}
				}
			}
			
			capRect.x = locX;
	
			moveNewRect.height = CLIENT_HEIGHT;
			//将新出生的空白块，发出
			sendPNG(cutBlackBorder(moveNewRect), CLIENT_WIDTH, false);
		}
		timeWatcher.watchTrigger();
	}
	
	private int BOTTOM_Y, MAX_RIGHT_X;
	private int CLIENT_WIDTH, CLIENT_HEIGHT;
	
	public void refreshRectange(final int x, final int y){	
		capRect.x = x;
		capRect.y = y;
		
		locX = x;
		locY = y;
		
		matchRect(moveNewRect);
		
		for (int i = 0; i < clientSnap.length; i++) {
			clientSnap[i] = -1;//不用DEFAULT_BACK_COLOR，而用-1，是强制更新全部
		}
		
		sendPNG(cutBlackBorder(capRect), CLIENT_WIDTH, false);
	}
	
	/**
	 * 去掉超界的区块，因为会产生黑块
	 * @param r
	 * @return
	 */
	private Rectangle cutBlackBorder(final Rectangle r){
		final int realW = r.x + r.width;
		if(realW > screenWidth){
			r.width -= realW - screenWidth;
		}
		
		final int realH = r.y + r.height;
		if(realH > screenHeigh){
			r.height -= realH - screenHeigh;
		}
		
		return r;
	}

	private void matchRect(final Rectangle rectangle) {
		rectangle.width = ((CLIENT_WIDTH < screenWidth)?CLIENT_WIDTH:screenWidth);
		rectangle.height = ((CLIENT_HEIGHT < screenHeigh)?CLIENT_HEIGHT:screenHeigh);
	}
	
	/**
	 * 本方法未来将由自动定时刷新对比来替换
	 */
//	private void mttRefreshAfterInput(){
//		robot.delay(200);
//		sendPNG(capRect, capRect.x, capRect.y, true);
//	}	

	@Override
	public int grabImage(final Rectangle bc, final int[] rgb){
		int[] out;
		
		//与缩略图可能存在并发问题，所以加锁
		synchronized (LOCK){
			out  = (robotPeer!=null)?
				((RobotPeer)robotPeer).getRGBPixels(bc):
				robot.createScreenCapture(bc).getRGB(
						0, 0, bc.width, bc.height, null, 0, bc.width);
		}
		final int length = bc.width * bc.height;
		System.arraycopy(out, 0, rgb, 0, length);
		return length;
	}

	public int getScreenWidth() {
		return screenWidth;
	}

	public int getScreenHeigh() {
		return screenHeigh;
	}

	@Override
	public void onExit(){
		super.onExit();
	}

	@Override
	public void onStart() {
		super.start();
	}

	@Override
	public void onPause() {
		enableStopCap(true);
	}

	@Override
	public void onResume() {
		enableStopCap(false);
	}
}
