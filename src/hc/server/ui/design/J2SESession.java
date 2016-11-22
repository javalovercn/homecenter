package hc.server.ui.design;

import hc.core.CoreSession;
import hc.core.HCMessage;
import hc.core.HCTimer;
import hc.core.IEventHCListener;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.cache.PendStore;
import hc.core.data.DataClientAgent;
import hc.core.data.DataInputEvent;
import hc.core.data.DataSelectTxt;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.Stack;
import hc.core.util.UIUtil;
import hc.server.J2SEServerURLAction;
import hc.server.KeepaliveManager;
import hc.server.data.screen.PNGCapturer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.msb.Robot;
import hc.server.msb.RobotEvent;
import hc.server.msb.RobotListener;
import hc.server.ui.ClientDesc;
import hc.server.ui.ClientSession;
import hc.server.ui.ICanvas;
import hc.server.ui.IMletCanvas;
import hc.server.ui.MenuItem;
import hc.server.ui.QuestionParameter;
import hc.server.ui.SessionMobiMenu;
import hc.server.util.SystemEventListener;
import hc.util.UpdateOneTimeRunnable;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Vector;

/**
 * 该实例在ScreenServer中，被用于对象锁。
 *
 */
public class J2SESession extends CoreSession{
	public J2SESession(){
		keepaliveManager = new KeepaliveManager(this);
		urlAction = new J2SEServerURLAction();
	}
	
	public ClientSession clientSession;
	public UpdateOneTimeRunnable updateOneTimeKeysRunnable;
	public boolean isReceivedOneTimeInSecuChannalFromMobile = false;
	public final int[] base64PngData = new int[UIUtil.ICON_MAX * UIUtil.ICON_MAX];
	public int refreshMillSecond = 3000;
	public int mask;
	public ScreenCapturer cap;
	public ICanvas currScreen;
	public HCTimer closeIOSLongConnection;
	public final Vector<PendStore> pendStoreVector = new Vector(32);
	public final ClientDesc clientDesc = new ClientDesc();
	public final Stack mobiScreenMap = new Stack();
	public boolean isServingHomeScreen = false;
	public KeepaliveManager keepaliveManager;
	public boolean isIdelSession = true;
	public boolean isWillCheckServer;
	public final HashMap<Integer, QuestionParameter> questionMap = new HashMap<Integer, QuestionParameter>();
	public int questionID = 1;
	public Stack mletHistoryUrl;
	public boolean isEventMobileLoginDone = false;
	public final Vector<SystemEventListener> sessionLevelEventListeners = new Vector<SystemEventListener>();
	private final HashMap<Robot, ArrayList<RobotListener>> sessionLevelRobotListeners = new HashMap<Robot, ArrayList<RobotListener>>(2);
	public final SessionEventManager sessionEventManager = new SessionEventManager();
	public boolean isTransedCertToMobile = false;
	public static final J2SESession NULL_J2SESESSION_FOR_PROJECT = null;
	public Object[] mobileValuesForCSS = null;
	private final HashMap<String, SessionMobiMenu> menuItemsMap = new HashMap<String, SessionMobiMenu>(8);
	
	public final SessionMobiMenu getMenu(final String projectID){
		return menuItemsMap.get(projectID);
	}
	
	public final Vector<MenuItem> getDisplayMenuItems(final String projectID){
		return getMenu(projectID).getFlushMenuItems();
	}
	
	public final MenuItem searchMenuItem(final String projectID, final String urlLower, final String aliasUrlLower){
		return getMenu(projectID).searchMenuItem(urlLower, aliasUrlLower);
	}
	
	public final void setSessionMenu(final String projectID, final SessionMobiMenu menu){
		menuItemsMap.put(projectID, menu);
	}

	public final void addRobotListener(final Robot robot, final RobotListener listener){
		synchronized (sessionLevelRobotListeners) {
			ArrayList<RobotListener> list = sessionLevelRobotListeners.get(robot);
			if(list == null){
				list = new ArrayList<RobotListener>(2);
				sessionLevelRobotListeners.put(robot, list);
			}
			list.add(listener);
		}
	}
	
	public final boolean removeRobotListener(final Robot robot, final RobotListener listener){
		synchronized (sessionLevelRobotListeners) {
			final ArrayList<RobotListener> list = sessionLevelRobotListeners.get(robot);
			if(list == null){
				return false;
			}else{
				return list.remove(listener);
			}
		}
	}
	
	public final void dispatchRobotEventSynchronized(final ProjResponser resp, final Robot robot, final RobotEvent event){
		final ArrayList<RobotListener> list = sessionLevelRobotListeners.get(robot);
		int size = 0;
		if(list == null || (size = list.size()) == 0){
			return;
		}
		
		final int p_size = size;
		resp.getMobileSession(this).recycleRes.threadPool.runAndWait(new ReturnableRunnable() {//因event回收，所以wait
			@Override
			public Object run() {
				try{
					for (int i = 0; i < p_size; i++) {
						final RobotListener robotListener = list.get(i);
						robotListener.action(event);
					}
				}catch (final IndexOutOfBoundsException outOfBound) {
					//越界或不存在或已删除
				}catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
				return null;
			}
		});
	}
	
	@Override
	public void release(){
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("release J2SESocketSession.");
		}
		
		super.release();
		
		keepaliveManager = null;
	}

	public final void initScreenEvent(){
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_UP;
			}
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession)coreSS).cap.moveUp(pixle);
				return true;
			}});
		
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_LEFT;
			}
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession)coreSS).cap.moveRight((-1)*pixle);
				return true;
			}});
		
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_DOWN;
			}
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession)coreSS).cap.moveUp((-1) * pixle);
				return true;
			}});
		
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_RIGHT;
			}
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession)coreSS).cap.moveRight(pixle);
				return true;
			}});
		
		//输入事件
		eventCenter.addListener(new IEventHCListener(){
			final DataInputEvent e = new DataInputEvent();
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				e.setBytes(bs);
				final ICanvas screenCap = ((J2SESession)coreSS).currScreen;
				if(screenCap == null || (screenCap instanceof PNGCapturer) == false){
					LogManager.errToLog("Error object, skip event input.");
				}else{
					((PNGCapturer)screenCap).actionInput(e);
				}
				return true;
			}
	
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_INPUT_EVENT;
			}});
	
		eventCenter.addListener(new IEventHCListener(){
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
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final J2SESession j2seCoreSS = (J2SESession)coreSS;
				final ICanvas screenCap = j2seCoreSS.currScreen;
				if(screenCap == null || (screenCap instanceof IMletCanvas) == false){
					LogManager.errToLog("screen or form may be closed, skip javascript event input.");
					return true;
				}else{
					final int screenIDlen = ByteUtil.oneByteToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
					if(actionJSInput(screenCap, bs, screenIDlen)){
						return true;
					}else{
						final Enumeration e = j2seCoreSS.mobiScreenMap.elements();
						try{
							while(e.hasMoreElements()){
								final Object ele = e.nextElement();
								if(ele instanceof IMletCanvas){
									if(actionJSInput(ele, bs, screenIDlen)){
										return true;
									}								
								}
							}
						}catch (final NoSuchElementException ex) {
						}
					}
					
					return true;
				}
			}
	
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_JS_EVENT_TO_SERVER;
			}});
		
		eventCenter.addListener(new IEventHCListener(){
	
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final DataClientAgent rect = new DataClientAgent();
				rect.setBytes(bs);
				
				((J2SESession)coreSS).cap.refreshRectange(rect.getWidth(), rect.getHeight());
				return true;
			}
	
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_REFRESH_RECTANGLE;
			}});
		
		eventCenter.addListener(new IEventHCListener(){
			final DataSelectTxt txt = new DataSelectTxt();
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				txt.setBytes(bs);
				
				((J2SESession)coreSS).cap.dragAndDrop(txt.getStartX(), txt.getStartY(), txt.getEndX(), txt.getEndY());
				return true;
			}
	
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_SELECT_TXT;
			}});
		
	
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_COLOR_MODE;
			}
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				//Donate会变量此值，所以不宜做属性。
				final int mode = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				
				PNGCapturer.updateColorBit(((J2SESession)coreSS), mode);
				return true;
			}});
		
		
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_REFRESH_MILLSECOND;
			}
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				//Donate会变量此值，所以不宜做属性。
				
				final int millSecond = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_MSG_DATA);
				
				PNGCapturer.updateRefreshMS(((J2SESession)coreSS), millSecond);
				return true;
			}});
	
		eventCenter.addListener(new IEventHCListener(){
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_ZOOM;
			}
			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final int zoomMulti = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession)coreSS).cap.zoom(zoomMulti);
				return true;
			}});
		
		
	}
}
