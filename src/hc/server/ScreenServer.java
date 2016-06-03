package hc.server;

import hc.core.EventCenter;
import hc.core.HCMessage;
import hc.core.IConstant;
import hc.core.IEventHCListener;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.data.DataClientAgent;
import hc.core.data.DataInputEvent;
import hc.core.data.DataSelectTxt;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLCacher;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.Stack;
import hc.server.data.screen.PNGCapturer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.html5.syn.MletHtmlCanvas;
import hc.server.ui.ICanvas;
import hc.server.ui.IMletCanvas;
import hc.server.ui.Mlet;
import hc.server.ui.MletSnapCanvas;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;

import java.util.Enumeration;

public class ScreenServer {
	public static ScreenCapturer cap;
	private static ICanvas currScreen;
	private static final Stack MOBI_SCREEN_MAP = new Stack();
	
	public static synchronized void pushToTopForMlet(final String url){
		final HCURL hcurl = HCURLUtil.extract(url);
		final String elementID = hcurl.elementID;
		final byte[] elementIDBS = ByteUtil.getBytes(elementID, IConstant.UTF_8);
		HCURLCacher.getInstance().cycle(hcurl);
		
		final int size = MOBI_SCREEN_MAP.size();
		for (int i = 0; i < size; i++) {
			final ICanvas canvas = (ICanvas)MOBI_SCREEN_MAP.elementAt(i);
			
			if(canvas instanceof IMletCanvas){
				final IMletCanvas mhtml = (IMletCanvas)canvas;
				if(mhtml.isSameScreenID(elementIDBS, 0, elementIDBS.length)){
					//因为仅做画面次序调整，所以要先行，
					final String[] para = {HCURL.DATA_PARA_SHIFT_SCREEN_TO_TOP_FROM_IDX, HCURL.DATA_PARA_SHIFT_SCREEN_TO_TOP_SIZE};
					final String[] values = {String.valueOf(i), String.valueOf(size)};
					HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, para, values);

					//重新关闭或启动逻辑，所以后行，
					MOBI_SCREEN_MAP.removeAt(i);

					pauseAndPushOldScreen();

					currScreen = (ICanvas)mhtml;
					
					resumeCurrent();
//					L.V = L.O ? false : LogManager.log("shift screen [" + url + "] to top. index : " + i + ", size : " + size);
					return;
				}
			}
		}
	}
	
	public static synchronized void pushScreen(final ICanvas screen){
		pauseAndPushOldScreen();
		
		currScreen = screen;
		
		try{
			currScreen.onStart();
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	private static void removeRemoteDispalyByIdx(){
		final int i = MOBI_SCREEN_MAP.size();
		final String[] para = {HCURL.DATA_PARA_REMOVE_SCREEN_BY_IDX};
		final String[] values = {String.valueOf(i + 1)};//mobile端是入stack后显示。
		HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, para, values);
	}

	private static void pauseAndPushOldScreen() {
		if(currScreen != null){
			if(currScreen instanceof MletHtmlCanvas || currScreen instanceof MletSnapCanvas){
				
				//进入isAutoReleaseAfterGo
				
				if(currScreen instanceof MletHtmlCanvas){
					final MletHtmlCanvas mletHtmlCanvas = (MletHtmlCanvas)currScreen;
					if(MletHtmlCanvas.isAutoReleaseAfterGo(mletHtmlCanvas.projectContext, mletHtmlCanvas.mlet)){
						removeRemoteDispalyByIdx();
						mletHtmlCanvas.onExit(true);
						return;
					}
				}else if(currScreen instanceof MletSnapCanvas){
					final MletSnapCanvas mletSnapCanvas = (MletSnapCanvas)currScreen;
					if(MletHtmlCanvas.isAutoReleaseAfterGo(mletSnapCanvas.projectContext, mletSnapCanvas.mlet)){
						removeRemoteDispalyByIdx();
						mletSnapCanvas.onExit(true);
						return;
					}
				}
			}
			
			//进入pause，并且压入stack
			try{
				currScreen.onPause();
			}catch (final Throwable e) {
			}
			MOBI_SCREEN_MAP.push(currScreen);
		}
	}
	
	public static ICanvas getCurrScreen(){
		return currScreen;
	}
	
	public static Mlet getCurrMlet(){
		if(currScreen instanceof IMletCanvas){
			return ((IMletCanvas)currScreen).getMlet();
		}
		return null;
	}
	
	public static boolean isCurrScreenType(final Class isClass){
		return isClass.isInstance(currScreen);
	}
	
	/**
	 * 内部执行关闭当前窗口资源exit。
	 * @return
	 */
	public static synchronized boolean popScreen(){
		if(currScreen != null){
			try{
				L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "back / exit");
				currScreen.onExit();
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		if(MOBI_SCREEN_MAP.size() == 0){
			currScreen = null;
			return false;
		}else{
			currScreen = (ICanvas)MOBI_SCREEN_MAP.pop();
			
			resumeCurrent();
			return true;
		}
	}

	private static void resumeCurrent() {
		try{
			currScreen.onResume();
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	/**
	 * 增加同步锁，因为退出时，可能多线程，会导致被调用多次。从而确保只调用一次
	 * @return
	 */
	public static synchronized boolean emptyScreen(){
		final boolean hasScreen = popScreen();
		while(popScreen()){
		}
		isServing = false;
		return hasScreen;
	}
	
	static{
		EventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_UP;
			}
			@Override
			public final boolean action(final byte[] bs) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.moveUp(pixle);
				return true;
			}});
		
		EventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_LEFT;
			}
			@Override
			public final boolean action(final byte[] bs) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.moveRight((-1)*pixle);
				return true;
			}});
		
		EventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_DOWN;
			}
			@Override
			public final boolean action(final byte[] bs) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.moveUp((-1) * pixle);
				return true;
			}});
		
		EventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_RIGHT;
			}
			@Override
			public final boolean action(final byte[] bs) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.moveRight(pixle);
				return true;
			}});
		
		//输入事件
		EventCenter.addListener(new IEventHCListener(){
			final DataInputEvent e = new DataInputEvent();
			@Override
			public final boolean action(final byte[] bs) {
				e.setBytes(bs);
				final ICanvas screenCap = currScreen;
				if(screenCap == null || (screenCap instanceof PNGCapturer) == false){
					LogManager.errToLog("Error object, skip event input.");
					return true;
				}else{
					return ((PNGCapturer)screenCap).actionInput(e);
				}
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_INPUT_EVENT;
			}});

		EventCenter.addListener(new IEventHCListener(){
			final int screenIDIndex = MsgBuilder.INDEX_MSG_DATA + 1;
			
			private boolean actionJSInput(final Object canvas, final byte[] bs, final int screenIDlen){
				final IMletCanvas iMletCanvas = (IMletCanvas)canvas;
				if(iMletCanvas.isSameScreenID(bs, screenIDIndex, screenIDlen)){
					final int totalMsgLen = HCMessage.getMsgLen(bs);
					iMletCanvas.actionJSInput(bs, screenIDIndex + screenIDlen, totalMsgLen - 1 - screenIDlen);
					return true;
				}
				return false;
			}
			
			@Override
			public final boolean action(final byte[] bs) {
				final ICanvas screenCap = currScreen;
				if(screenCap == null || (screenCap instanceof IMletCanvas) == false){
					LogManager.errToLog("screen or form may be closed, skip javascript event input.");
					return true;
				}else{
					final int screenIDlen = ByteUtil.oneByteToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
					if(actionJSInput(screenCap, bs, screenIDlen)){
						return true;
					}else{
						final Enumeration e = MOBI_SCREEN_MAP.elements();
						while(e.hasMoreElements()){
							final Object ele = e.nextElement();
							if(ele instanceof IMletCanvas){
								if(actionJSInput(ele, bs, screenIDlen)){
									return true;
								}								
							}
						}
					}
					
					return true;
				}
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_JS_EVENT_TO_SERVER;
			}});
		
		EventCenter.addListener(new IEventHCListener(){

			@Override
			public final boolean action(final byte[] bs) {
				final DataClientAgent rect = new DataClientAgent();
				rect.setBytes(bs);
				
				ScreenServer.cap.refreshRectange(rect.getWidth(), rect.getHeight());
				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_REFRESH_RECTANGLE;
			}});
		
		EventCenter.addListener(new IEventHCListener(){
			final DataSelectTxt txt = new DataSelectTxt();
			@Override
			public final boolean action(final byte[] bs) {
				txt.setBytes(bs);
				
				cap.dragAndDrop(txt.getStartX(), txt.getStartY(), txt.getEndX(), txt.getEndY());
				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_SELECT_TXT;
			}});
		

		EventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_COLOR_MODE;
			}
			@Override
			public final boolean action(final byte[] bs) {
				//Donate会变量此值，所以不宜做属性。
				final int mode = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				
				PNGCapturer.updateColorBit(mode);
				return true;
			}});
		
		
		EventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_REFRESH_MILLSECOND;
			}
			@Override
			public final boolean action(final byte[] bs) {
				//Donate会变量此值，所以不宜做属性。
				
				final int millSecond = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_MSG_DATA);
				
				PNGCapturer.updateRefreshMS(millSecond);
				return true;
			}});

		EventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_ZOOM;
			}
			@Override
			public final boolean action(final byte[] bs) {
				final int zoomMulti = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				cap.zoom(zoomMulti);
				return true;
			}});
		
		
	}
	
	private static boolean isServing = false;
	private static final Object LOCK = new Object();
		
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

	public static final int J2SE_STANDARD_DPI = 96;

	public static void onStartForMlet(final ProjectContext projectContext, final Mlet mlet) {
		projectContext.run(new Runnable() {
			@Override
			public void run() {
				mlet.notifyStatusChanged(Mlet.STATUS_RUNNING);//in user thread
				mlet.onStart();
			}
		});
	}

	public static void onExitForMlet(final ProjectContext projectContext, final Mlet mlet, final boolean isAutoReleaseAfterGo) {
		ServerUIAPIAgent.popMletURLHistory(projectContext);
		
		if(isAutoReleaseAfterGo){
			L.V = L.O ? false : LogManager.log("last Mlet/HTMLMlet is auto released after go to other Mlet/HTMLMlet.");
		}
		
		projectContext.run(new Runnable() {
			@Override
			public void run() {
				mlet.notifyStatusChanged(Mlet.STATUS_EXIT);//in user thread
				mlet.onExit();
			}
		});
	}

	public static void onPauseForMlet(final ProjectContext projectContext, final Mlet mlet) {
		projectContext.run(new Runnable() {
			@Override
			public void run() {
				mlet.notifyStatusChanged(Mlet.STATUS_PAUSE);//in user thread
				mlet.onPause();
			}
		});
	}

	public static void onResumeForMlet(final ProjectContext projectContext, final Mlet mlet) {
		projectContext.run(new Runnable() {
			@Override
			public void run() {
				mlet.notifyStatusChanged(Mlet.STATUS_RUNNING);//in user thread
				mlet.onResume();
			}
		});
	}
	
}
