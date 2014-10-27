package hc.server;

import hc.core.ContextManager;
import hc.core.EventCenter;
import hc.core.IConstant;
import hc.core.IEventHCListener;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.data.DataClientAgent;
import hc.core.data.DataInputEvent;
import hc.core.data.DataSelectTxt;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.core.util.Stack;
import hc.server.data.screen.PNGCapturer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.ui.ICanvas;

public class ScreenServer {
	public static ScreenCapturer cap;
	private static ICanvas currScreen;
	private static final Stack MOBI_SCREEN_MAP = new Stack();
	
	public static void pushScreen(ICanvas screen){
		if(currScreen != null){
			try{
				currScreen.onPause();
			}catch (Throwable e) {
				
			}
			MOBI_SCREEN_MAP.push(currScreen);
		}
		currScreen = screen;
		
		try{
			currScreen.onStart();
		}catch (Throwable e) {
			
		}
	}
	
	public static ICanvas getCurrScreen(){
		return currScreen;
	}
	
	public static boolean isCurrScreenType(final Class isClass){
		return isClass.isInstance(currScreen);
	}
	
	/**
	 * 内部执行关闭当前窗口资源exit。
	 * @return
	 */
	public static boolean popScreen(){
		if(currScreen != null){
			try{
				L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "back / exit");
				currScreen.onExit();
			}catch (Throwable e) {
				e.printStackTrace();
			}
		}
		if(MOBI_SCREEN_MAP.size() == 0){
			currScreen = null;
			return false;
		}else{
			currScreen = (ICanvas)MOBI_SCREEN_MAP.pop();
			
			try{
				currScreen.onResume();
			}catch (Throwable e) {
				e.printStackTrace();
			}
			return true;
		}
	}
	
	/**
	 * 增加同步锁，因为退出时，可能多线程，会导致被调用多次。从而确保只调用一次
	 * @return
	 */
	public static synchronized boolean emptyScreen(){
		boolean hasScreen = popScreen();
		while(popScreen()){
		}
		isServing = false;
		return hasScreen;
	}
	
	static{
		EventCenter.addListener(new IEventHCListener(){
			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_UP;
			}
			public boolean action(final byte[] bs) {
				int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.moveUp(pixle);
				return true;
			}});
		
		EventCenter.addListener(new IEventHCListener(){
			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_LEFT;
			}
			public boolean action(final byte[] bs) {
				int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.moveRight((-1)*pixle);
				return true;
			}});
		
		EventCenter.addListener(new IEventHCListener(){
			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_DOWN;
			}
			public boolean action(final byte[] bs) {
				int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.moveUp((-1) * pixle);
				return true;
			}});
		
		EventCenter.addListener(new IEventHCListener(){
			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_RIGHT;
			}
			public boolean action(final byte[] bs) {
				int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.moveRight(pixle);
				return true;
			}});
		
		//输入事件
		EventCenter.addListener(new IEventHCListener(){
			final DataInputEvent e = new DataInputEvent();
			public boolean action(final byte[] bs) {
				e.setBytes(bs);
				if(currScreen == null || (currScreen instanceof PNGCapturer) == false){
					LogManager.errToLog("Error object, skip event input.");
					return true;
				}else{
					return ((PNGCapturer)currScreen).actionInput(e);
				}
			}

			public byte getEventTag() {
				return MsgBuilder.E_INPUT_EVENT;
			}});
		
		EventCenter.addListener(new IEventHCListener(){

			public boolean action(final byte[] bs) {
				DataClientAgent rect = new DataClientAgent();
				rect.setBytes(bs);
				
				ScreenServer.cap.refreshRectange(rect.getWidth(), rect.getHeight());
				return true;
			}

			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_REFRESH_RECTANGLE;
			}});
		
		EventCenter.addListener(new IEventHCListener(){
			final DataSelectTxt txt = new DataSelectTxt();
			public boolean action(final byte[] bs) {
				txt.setBytes(bs);
				
				cap.dragAndDrop(txt.getStartX(), txt.getStartY(), txt.getEndX(), txt.getEndY());
				return true;
			}

			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_SELECT_TXT;
			}});
		

		EventCenter.addListener(new IEventHCListener(){
			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_COLOR_MODE;
			}
			public boolean action(final byte[] bs) {
				//Donate会变量此值，所以不宜做属性。
				final int colorOnRelay = Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_Color_On_Relay));
				int mode = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				
				if(SIPManager.isOnRelay()){
					if((IConstant.COLOR_STAR_TOP - mode) > colorOnRelay){
						mode = (IConstant.COLOR_STAR_TOP - colorOnRelay);
					}
				}else{
					short connMode = ContextManager.getConnectionModeStatus();
					if(connMode == ContextManager.MODE_CONNECTION_HOME_WIRELESS){
						//取最大值
						mode = IConstant.COLOR_64_BIT;
					}else if(connMode == ContextManager.MODE_CONNECTION_PUBLIC_UPNP_DIRECT){
						mode = Math.min(mode, IConstant.COLOR_16_BIT);
					}else if(connMode == ContextManager.MODE_CONNECTION_PUBLIC_DIRECT){
						mode = Math.min(mode, IConstant.COLOR_32_BIT);
					}
					
				}

				hc.core.L.V=hc.core.L.O?false:LogManager.log("Client change colorMode to level : " + (IConstant.COLOR_STAR_TOP - mode) + " (after limited)");
				PNGCapturer.setColorBit(mode);
				return true;
			}});
		
		
		EventCenter.addListener(new IEventHCListener(){
			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_REFRESH_MILLSECOND;
			}
			public boolean action(final byte[] bs) {
				//Donate会变量此值，所以不宜做属性。
				final int msOnRelay = Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_MS_On_Relay));
				
				int millSecond = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_MSG_DATA);
				
				if(SIPManager.isOnRelay()){
					if(millSecond < msOnRelay){
						millSecond = msOnRelay;
					}
				}else{
					short mode = ContextManager.getConnectionModeStatus();
					if(mode == ContextManager.MODE_CONNECTION_HOME_WIRELESS){
						millSecond = 100;
					}else if(mode == ContextManager.MODE_CONNECTION_PUBLIC_UPNP_DIRECT){
						millSecond = Math.min(millSecond, 1000);
					}else if(mode == ContextManager.MODE_CONNECTION_PUBLIC_DIRECT){
						millSecond = Math.min(millSecond, 1000);
					}
				}
				
				hc.core.L.V=hc.core.L.O?false:LogManager.log("Client change refresh MillSecond to:" + millSecond);
				PNGCapturer.setRefreshMillSecond(millSecond);
				return true;
			}});

		EventCenter.addListener(new IEventHCListener(){
			public byte getEventTag() {
				return MsgBuilder.E_SCREEN_ZOOM;
			}
			public boolean action(final byte[] bs) {
				int zoomMulti = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.zoom(zoomMulti);
				return true;
			}});
		
		
	}
	
	private static boolean isServing = false;
	private static final Boolean LOCK = new Boolean(false);
		
	public static boolean isServing(){
		return isServing;
	}
	
	/**
	 * 如果成功请求，则返回true；否则如被其它用户先占，则返回false。
	 * @return
	 */
	public static boolean askForService(){
		synchronized (LOCK) {
			if (isServing) {
				return false;
			}
			isServing = true;
			return true;
		}
	}
	
}
