package hc.server.data.screen;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.MsgBuilder;
import hc.core.RootServerConnector;
import hc.core.data.DataInputEvent;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.core.util.ThumbnailHelper;
import hc.core.util.TimeWatcher;
import hc.server.MultiUsingManager;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.server.data.DAOKeyComper;
import hc.server.data.KeyComperPanel;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.SingleMessageNotify;
import hc.server.ui.design.J2SESession;
import hc.server.util.IDArrayGroup;
import hc.util.ResourceUtil;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.peer.RobotPeer;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Vector;

public class ScreenCapturer extends PNGCapturer{
	final private ThreadGroup threadPoolToken = App.getThreadPoolToken();
	final private Rectangle moveNewRect = new Rectangle();

	private final int screenWidth, screenHeigh;
	public static Object robotPeer;
	public static Robot robot;
	public KeyComper mobiUserIcons;
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
			robotPeer = PlatformManager.getService().createRobotPeer(robot);
		} catch (final Throwable e) {
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

	public ScreenCapturer(final J2SESession coreSS, final int clientWidth, final int clientHeight, final int pcWidth, final int pcHeight) {
		super(coreSS, pcWidth, pcHeight, true, 0);//全屏式
	
		timeWatcher = new TimeWatcher(60 * 1000) {//超过一分钟
			@Override
			public final void doBiz() {
				//检查缓存是否全黑
				randomCheckLock();
			}
		};
		
		final Thread t = new Thread(){
			//发送远屏缩略图
			@Override
			public void run(){
				final int[] outSize = ThumbnailHelper.calcThumbnail(clientWidth, clientHeight, pcWidth, pcHeight);
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
					Thread.sleep(5000);
					while(!isShutDown){
						BufferedImage screen;
						
						//加锁，以防并用对象robot
						synchronized (LOCK) {
							screen = robot.createScreenCapture(sc);
						}
						
						screen = ResourceUtil.resizeImage(screen, thumbnailWidth, thumbnailHeight);
						screen.getRGB(0, 0, thumbnailWidth, thumbnailHeight, screenRGBData, 0, thumbnailWidth);
						
						//降低色彩度
						for (int idxRGB = 0; idxRGB < thumbnailSize; idxRGB++) {
							final int c = screenRGBData[idxRGB];
							if(c != 0xFF000000){//纯黑保持不变
								screenRGBData[idxRGB] = c | coreSS.mask;
							}
						}
						
						if(clearCacheThumbnail){
							for (int i = 0; i < thumbnailRGBData.length; i++) {
								thumbnailRGBData[i] = 0;
							}
							clearCacheThumbnail = false;
						}
						
//						LogManager.log("send thumbnail.");
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
						Thread.sleep(5000);
					}
				}catch (final Exception e) {
				}
			}
		};
		t.setPriority(ThreadPriorityManager.CAPTURER_PRIORITY);
		t.start();
		
		//初始化扩展自定义按钮
		initExtMobiIcon();
		
		screenWidth = pcWidth;
		screenHeigh = pcHeight;
		
		{
			KeyComperPanel.sendExtMouse(coreSS);
		}

		setCaptureID(HCURL.REMOTE_HOME_SCREEN, HCURL.REMOTE_HOME_SCREEN);
		
		LogManager.log(ILog.OP_STR + "Screen [" + HCURL.REMOTE_HOME_SCREEN + "] start");
	
		{
			client_width_zoomornot = clientWidth;
			client_height_zoomornot = clientHeight;
			
			final_mobi_width = clientWidth;
			final_mobi_height = clientHeight;
			
			final int[] out = ThumbnailHelper.calNewLocXY(2, 1, 0, 0, pcWidth, pcHeight, clientWidth, clientHeight);
			

			client_width_zoomornot = out[2];
			client_height_zoomornot = out[3];
			
			MAX_RIGHT_X = out[4];
			BOTTOM_Y = out[5];
//			
//			int by = screenHeigh - client_height_zoomornot;
//			if(by < 0){
//				BOTTOM_Y = 0;
//			}else{
//				BOTTOM_Y = by;
//			}
//			int rx = screenWidth - client_width_zoomornot;
//			if(rx < 0){
//				 = 0;
//			initMobileArea();

			locX = 0;
			locY = BOTTOM_Y;

			rematchRect(out[6], out[7]);
		}
	}

	/**
	 * 更新capRect和movNewRect的x,y,width, height
	 */
	private void rematchRect(final int addOnePixelW, final int addOnePixelH) {
		capRect.x = locX;
		capRect.y = locY;
		matchRect(capRect, addOnePixelW, addOnePixelH);
		
		moveNewRect.x = locX;
		moveNewRect.y = locY;
		matchRect(moveNewRect, addOnePixelW, addOnePixelH);
	}
	
	private int zoomOldMultiples = 1;
	
//	private void initMobileArea() {
////		LogManager.log("is ZoomIn : " + isZoomIn);
//		
//		client_width_zoomornot = convertZoomUnzoomXY(final_mobi_width);xx
//		client_height_zoomornot = convertZoomUnzoomXY(final_mobi_height);
//		
//		int by = screenHeigh - client_height_zoomornot;
//		if(by < 0){
//			BOTTOM_Y = 0;
//		}else{
//			BOTTOM_Y = by;
//		}
//		int rx = screenWidth - client_width_zoomornot;
//		if(rx < 0){
//			MAX_RIGHT_X = 0;
//		}else{
//			MAX_RIGHT_X = rx;
//		}
//		LogManager.log("BOTTOM_Y : " + BOTTOM_Y + ", MAX_RIGHT_X : " + MAX_RIGHT_X);
//	}
	
	@Override
	public void run(){
		sleepBeforeRun();

		notifyScreenMode();

		cycleCapture();
	}
	
	private final boolean isDirectServerMode(){
		if(coreSS.isOnRelay() == false){
			final short connMode = coreSS.context.getConnectionModeStatus();
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
				LogManager.log("Detect Server is in direct mode, notify mobile.");
				coreSS.context.send(null, MsgBuilder.E_TAG_ROOT, 
						MsgBuilder.DATA_ROOT_SERVER_IN_DIRECT_MODE);
			}
		}
		
		randomCheckLock();
	}

	private void randomCheckLock() {
		if(ResourceUtil.isAndroidServerPlatform()){
			if(PlatformManager.getService().isLockScreen()){
				doBizAfterIsLockScreen();
			}
		}else{
			randomCheckLockJ2SE();
		}
	}
	
	private void randomCheckLockJ2SE() {
		final Random r = new Random(System.currentTimeMillis());
		final int minW = screenWidth - MIN_BLOCK_CAP;
		final int minH = screenHeigh - MIN_BLOCK_CAP;
		final Rectangle testMinBlock = new Rectangle(((minW==0)?0:r.nextInt(minW)), ((minH==0)?0:r.nextInt(minH)), 
				MIN_BLOCK_CAP, MIN_BLOCK_CAP);
		if(isSameColorBlock(testMinBlock, rgb)){
			isSameFullScreen();
		}
	}

	private void isSameFullScreen() {
		//产生一个模拟事件，以解除如屏保而导致的可自解锁型
		LogManager.log("Screen maybe in lock or screensave, create ctrl-key push event to active screen.");
		
//		Point mousepoint = MouseInfo.getPointerInfo().getLocation();  
//		robot.mouseMove(0, 0);//会导致弹出移动，最小化菜单，故关闭
		synchronized (robot) {
			robot.keyPress(KeyEvent.VK_CONTROL);
			robot.keyRelease(KeyEvent.VK_CONTROL);
		}
//		robot.mouseMove(mousepoint.x, mousepoint.y);
		
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				try{
					Thread.sleep(500);
				}catch (final Exception e) {
				}
				
				if(isSameFullScreenDelay()){
					doBizAfterIsLockScreen();
				}
			}
		}, threadPoolToken);
	}
	private boolean isSameFullScreenDelay() {
		final Rectangle testMinBlock = new Rectangle();
		//进行全屏测试
		testMinBlock.x = 0;
		testMinBlock.y = 0;
		testMinBlock.width = screenWidth;
		testMinBlock.height = screenHeigh;
		
		try{
			final int[] fullScreenRGB = new int[screenWidth * screenHeigh];
			
			if(isSameColorBlock(testMinBlock, fullScreenRGB)){
				return true;
			}
		}catch (final Throwable e) {
			LogManager.errToLog("Fail check screen lock mode.");
			ExceptionReporter.printStackTrace(e);
		}
		
		return false;
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

	@Override
	public void actionInput(final DataInputEvent event){
		final int type = event.getType();
		final int pointX = event.getX();
		final int pointY = event.getY();
		if(type == DataInputEvent.TYPE_MOUSE_MOVE){
			robot.mouseMove(pointX, pointY);
			LogManager.log(ILog.OP_STR + "Mouse move to (" + pointX + ", " + pointY + ")");
		}else if(type == DataInputEvent.TYPE_MOUSE_LEFT_CLICK){
			LogManager.log(ILog.OP_STR + "Mouse left click at (" + pointX + ", " + pointY + ")");
			synchronized (robot) {
				robot.mouseMove(pointX, pointY);
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
			}
		}else if(type == DataInputEvent.TYPE_MOUSE_RIGHT_CLICK){
			LogManager.log(ILog.OP_STR + "Mouse right click at (" + pointX + ", " + pointY + ")");
			synchronized (robot) {
				robot.mouseMove(pointX, pointY);
				robot.mousePress(InputEvent.BUTTON3_MASK);
				robot.mouseRelease(InputEvent.BUTTON3_MASK);			
			}
		}else if(type == DataInputEvent.TYPE_MOUSE_DOUBLE_CLICK){
			LogManager.log(ILog.OP_STR + "Mouse double click at (" + pointX + ", " + pointY + ")");
			synchronized (robot) {
				robot.mouseMove(pointX, pointY);
				
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);			
				robot.mousePress(InputEvent.BUTTON1_MASK);
				robot.mouseRelease(InputEvent.BUTTON1_MASK);		
			}
		}else if(type == DataInputEvent.TYPE_TRANS_TEXT){
			try {
				final String s = event.getTextDataAsString();
				if(inputKeyStr(s) == false){
					LogManager.log(ILog.OP_STR + "Client paste txt:[" + s + "]!");
					final Clipboard clipboard = ResourceUtil.getClipboard();
					synchronized (clipboard) {
						Transferable oldTrans = null;
						try{
							oldTrans = clipboard.getContents(null);
						}catch (final Throwable e) {
						}
						
						ResourceUtil.sendToClipboard(s);
						ctrlSomeKey(KeyEvent.VK_V);
						
						try{
							Thread.sleep(ThreadPriorityManager.UI_WAIT_FOR_EVENTQUEUE);//Android maybe need sleep
						}catch (final Exception e) {
						}
						if(oldTrans != null){
							clipboard.setContents(oldTrans, null);
						}
					}
				}else{
					LogManager.log(ILog.OP_STR + "Client input txt:[" + s + "]!");
				}
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}else if(type == DataInputEvent.TYPE_BACKSPACE){
			backspace();
			LogManager.log(ILog.OP_STR + "Press BACKSPACE");
		}else if(type == DataInputEvent.TYPE_TAG_TRANS_TEXT_2_MOBI){
			copytxtToMobi();
		}else if(type == DataInputEvent.TYPE_TAG_INPUT_KEYBOARD_FROM_MOBI){
			LogManager.log(ILog.OP_STR + "Press a mobile keyboard char");
			type(robot, (char)pointX);
		}else{
			//自定义扩展MouseIcon
			final int idx = type - DataInputEvent.TYPE_TAG_TRANS_TEXT_2_MOBI;
			
			if(mobiUserIcons != null){
				final String[] keysDesc = new String[1];
				final Vector<Integer> vInt = mobiUserIcons.getKeys(idx, keysDesc);
				if(vInt == null){
					LogManager.log("Unknow ext icon idx:" + type);
				}else{
					KeyComper.keyAction(coreSS, robot, vInt, keysDesc[0]);
				}
			}
		}
		
//		mttRefreshAfterInput();
	}

	public static void type(final Robot r, final char character) {
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
	
	private static void doType(final Robot r, final int keyCode) {
		synchronized (r) {
	        r.keyPress(keyCode);
	        r.keyRelease(keyCode);
		}
    }
	
	private static void doType(final Robot r, final int keyCode1, final int keyCode2) {
		synchronized (r) {
	        r.keyPress(keyCode1);
	
	        r.keyPress(keyCode2);
	        r.keyRelease(keyCode2);
	
	        r.keyRelease(keyCode1);
		}
    }
	
	private boolean inputKeyStr(final String txt){
		final char[] chars = txt.toCharArray();
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
	
	private void ctrlSomeKey(final int key) {
		if(ResourceUtil.isAndroidServerPlatform()){
			if(key == KeyEvent.VK_V){
				PlatformManager.getService().doExtBiz(PlatformService.BIZ_CTRL_V, null);
				return;
			}
		}
		final int abstractCtrlKeyCode = ResourceUtil.getAbstractCtrlKeyCode();
		synchronized (robot) {
			robot.keyPress(abstractCtrlKeyCode);
			robot.keyPress(key);
			robot.keyRelease(key);
			robot.keyRelease(abstractCtrlKeyCode);
		}
	}
	
	public void backspace(){
		synchronized (robot) {
			robot.keyPress(KeyEvent.VK_BACK_SPACE);
			robot.keyRelease(KeyEvent.VK_BACK_SPACE);
		}
	}
	
	public void copytxtToMobi(){
//		final Clipboard clipboard = ResourceUtil.getClipboard();
//		synchronized (clipboard) {
//			Transferable oldTrans = null;
//			try{
//				oldTrans = clipboard.getContents(null);
//				Thread.sleep(1000);
//			}catch (final Throwable e) {
//			}
			
			ctrlSomeKey(KeyEvent.VK_C);
			try{
				Thread.sleep(200);//实测不需要也行
			}catch (final Exception e) {
			}
			final String txt = ResourceUtil.getTxtFromClipboard();
	//		LogManager.log("User ready copyTxtToMobi:" + txt);
			if(txt.length() > 0){
				try {
		    		final DataInputEvent e = new DataInputEvent();
		    		final byte[] txt_bs = txt.getBytes(IConstant.UTF_8);
		    		
		    		final byte[] txtToMobiBS = new byte[MsgBuilder.UDP_BYTE_SIZE];
		    		e.setBytes(txtToMobiBS);
		    		e.setScreenID(screenIDForCapture, 0, screenIDForCapture.length);
		    		e.setType(DataInputEvent.TYPE_TRANS_TEXT);
	
		    		e.setTextData(txt_bs, 0, txt_bs.length);
		    		coreSS.context.send(
		    				MsgBuilder.E_INPUT_EVENT, txtToMobiBS, e.getLength());
				} catch (final UnsupportedEncodingException e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
			
			LogManager.log(ILog.OP_STR + "copyTxtToMobi:" + ((txt.length()==0)?"null":txt));

//			if(oldTrans != null){
//				clipboard.setContents(oldTrans, null);//只有第一次正确，故关闭
//			}
//		}
	}
	
	public void dragAndDrop(final int startX, final int startY, final int endX, final int endY){
		synchronized (robot) {
			robot.mouseMove(startX, startY);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			
			robot.mouseMove(endX, endY);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
		}
//		ctrlSomeKey(KeyEvent.VK_C);
		
		LogManager.log(ILog.OP_STR + "drag from ["+startX + ", " + startY+"] to [" + endX + ", " + endY + "]");
	}
	
	/**
	 * 由于采用全屏技术，所以不考虑zoom
	 * @param zoomMultiPara
	 */
	@Deprecated
	public void zoom(final int zoomMultiPara){
		synchronized (LOCK) {
//			LogManager.log("receive zoom cmd : " + zoomMultiPara + ", oldZoomMulti : " + zoomOldMultiples);
			int[] outInts = null;;
			if(zoomOldMultiples != 1){
				outInts = zoomUnzoom(zoomOldMultiples, 1);
			}
			if(zoomMultiPara != 1){
				outInts = zoomUnzoom(1, zoomMultiPara);
			}
			zoomOldMultiples = zoomMultiPara;
			
//			LogManager.log("oldLocX : " + oldLocX + ", oldLocY : " + oldLocY + ", locX : " + locX + ", locY : " + locY);
			
			if(outInts != null){
				rematchRect(outInts[6], outInts[7]);
			}else{
				rematchRect(0, 0);
			}
			initClientSnap();
		
			try{
				sendPNG(capRect, client_width_zoomornot, false);
			}catch (final Exception e) {
				//考虑数据溢出，故忽略
			}
		}
	}

	private int[] zoomUnzoom(final int fromZoom, final int toZoom) {
		final int oldLocX = locX, oldLocY = locY;
		
		if(toZoom != 1){
//			mobileW = client_width_zoomornot;
//			mobileH = client_height_zoomornot;
//				isZoomIn = true;
//			initMobileArea();
		}else{
//				isZoomIn = false;
//			initMobileArea();
//			mobileW = client_width_zoomornot;
//			mobileH = client_height_zoomornot;
		}
//		initMobileArea();
		final int[] newLocXY = ThumbnailHelper.calNewLocXY(fromZoom, toZoom, oldLocX, oldLocY, screenWidth, screenHeigh, final_mobi_width, final_mobi_height);
		
		locX = newLocXY[0];
		locY = newLocXY[1];
		
		client_width_zoomornot = newLocXY[2];
		client_height_zoomornot = newLocXY[3];
		
		MAX_RIGHT_X = newLocXY[4];
		BOTTOM_Y = newLocXY[5];
		
		return newLocXY;
	}

	public void moveUp(final int pixle){
		if(pixle > 0){
			LogManager.log(ILog.OP_STR + "moveUp : " + pixle);
		}else{
			LogManager.log(ILog.OP_STR + "moveDown : " + (-pixle));
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
				final int len = (client_height_zoomornot - pixle) * client_width_zoomornot;
				int fromRowIdx = len - 1;
				for (int idxEnd = client_height_zoomornot * client_width_zoomornot - 1; fromRowIdx >= 0;) {
					clientSnap[idxEnd--] = clientSnap[fromRowIdx--];
				}
				
				for (int i = 0, lenM = pixle * client_width_zoomornot; i < lenM; ) {
					clientSnap[i++] = DEFAULT_BACK_COLOR;
				}
			}else{
				moveNewRect.height = pixle * (-1);
				moveNewRect.y = locY + client_height_zoomornot + pixle;
				
				//------------------------
				//************************
				//************************
				//************************
				final int len = (client_height_zoomornot + pixle) * client_width_zoomornot;
				int fromRowIdx = (-pixle) * client_width_zoomornot;
				for (int i = 0; i < len;) {
					clientSnap[i++] = clientSnap[fromRowIdx++];
				}
				
				final int toIdx = (client_height_zoomornot + pixle) * client_width_zoomornot;
				for (int i = toIdx, end = client_height_zoomornot * client_width_zoomornot; i < end; ) {
					clientSnap[i++] = DEFAULT_BACK_COLOR;
				}
			}
	
			capRect.y = locY;
			
			moveNewRect.width = client_width_zoomornot;
	//		将新出生的空白块，发出
			sendPNG(cutBlackBorder(moveNewRect), client_width_zoomornot, false);
		}
		timeWatcher.watchTrigger();		
	}
	
	public void moveRight(final int pixle){		
		if(pixle > 0){
			LogManager.log(ILog.OP_STR + "moveRight : " + pixle);
		}else{
			LogManager.log(ILog.OP_STR + "moveLeft : " + (-pixle));
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
				moveNewRect.x = locX + client_width_zoomornot - pixle;

				for (int row = 0, RowLen = client_height_zoomornot; row < RowLen; row++) {
					final int idxR = row * client_width_zoomornot;
					int idxM = idxR + pixle;
					//--********************
					for (int x = idxR, end = idxR + client_width_zoomornot - pixle; x >= 0 && x < end;) {
//						if(row == 0){
//							LogManager.log("mov " + idxM + " to " + x);
//						}
						clientSnap[x++] = clientSnap[idxM++];
					}
					for (int end = idxR + client_width_zoomornot, i = end - pixle; i >= 0 && i < end; i++) {
//						if(row == 0){
//							LogManager.log("reset color idx " + i);
//						}
						clientSnap[i] = DEFAULT_BACK_COLOR;
					}
				}
			}else{
				moveNewRect.width = pixle * (-1);
				moveNewRect.x = locX;

				for (int row = 0, RowLen = client_height_zoomornot; row < RowLen; row++) {
					final int idxR = row * client_width_zoomornot;
					int idxM = idxR + client_width_zoomornot + pixle - 1;
					//********************--
					for (int x = idxR + client_width_zoomornot - 1; idxM >= idxR;) {
						clientSnap[x--] = clientSnap[idxM--];
					}
					for (int i = idxR, len = idxR-pixle; i >= 0 && i < len; i++) {
						clientSnap[i] = DEFAULT_BACK_COLOR;
					}
				}
			}
			
			capRect.x = locX;
	
			moveNewRect.height = client_height_zoomornot;
			//将新出生的空白块，发出
			sendPNG(cutBlackBorder(moveNewRect), client_width_zoomornot, false);
		}
		timeWatcher.watchTrigger();
	}
	
	private int BOTTOM_Y, MAX_RIGHT_X;
	private int client_width_zoomornot, client_height_zoomornot;
	private final int final_mobi_width, final_mobi_height;
	
	/**
	 * 仅在初始发送时，以增强用户体验，而被调用
	 * @param x
	 * @param y
	 */
	public void refreshRectange(final int x, final int y){	
		final int mobileWidth = UserThreadResourceUtil.getMobileWidthFrom(coreSS);
		final int mobileHeight = UserThreadResourceUtil.getMobileHeightFrom(coreSS);
		
		synchronized (LOCK) {
//			locX = 0;
//			locY = 0;
			
			final Rectangle firstRect = new Rectangle(x, y, 
					Math.min(mobileWidth, screenWidth), Math.min(mobileHeight, screenHeigh));
//			LogManager.log("First Screen x : " + x + ", y : " + y + ", w : " + firstRect.width + ", h : " + firstRect.height);
			sendPNG(firstRect, firstRect.width, false);
		}
		
		final int[] screenSize = ResourceUtil.getSimuScreenSize();
		
		if(screenSize[0] > mobileWidth || screenSize[1] > mobileHeight){
			//发送全部
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					final int[] screenSize = ResourceUtil.getSimuScreenSize();
					
					synchronized (LOCK) {
						locX = 0;
						locY = 0;
						
						capRect.x = 0;
						capRect.y = 0;
						capRect.width = screenSize[0];
						capRect.height = screenSize[1];
						
//						System.out.println("new capRect  w : " + capRect.width + ", h : " + capRect.height);
					}
				}
			}, App.getThreadPoolToken());
		}
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

	private void matchRect(final Rectangle rectangle, final int addOnePixelW, final int addOnePixelH) {
		rectangle.width = (((client_width_zoomornot < screenWidth)?client_width_zoomornot:screenWidth)) + addOnePixelW;
		rectangle.height = (((client_height_zoomornot < screenHeigh)?client_height_zoomornot:screenHeigh)) + addOnePixelH;
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
		final String screenID = ServerUIAPIAgent.buildScreenID(MultiUsingManager.NULL_PROJECT_ID, HCURL.URL_HOME_SCREEN);
		MultiUsingManager.exit(coreSS, screenID);
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

	private void doBizAfterIsLockScreen() {
		LogManager.errToLog("Screen is in lock, no image is sended to mobile.");
//					LogManager.errToLog("  screen is locked after pressed Win + L or screen save is triggered,");
		LogManager.errToLog("  mobile displays black if server is in lock mode in some older OS");
		LogManager.errToLog("  please disable screen lock or screen saver.");
		coreSS.context.send(null, MsgBuilder.E_TAG_ROOT, 
				MsgBuilder.DATA_ROOT_OS_IN_LOCK);
				
		SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_SCR_LOCKING, 
				(String)ResourceUtil.get(9088), 
				(String)ResourceUtil.get(9087), SingleMessageNotify.NEVER_AUTO_CLOSE, App.getSysIcon(App.SYS_ERROR_ICON));
		
		RootServerConnector.notifyLineOffType(coreSS, RootServerConnector.LOFF_LockScreen_STR);
	}
}
