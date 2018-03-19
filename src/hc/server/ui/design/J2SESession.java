package hc.server.ui.design;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.DelayWatcher;
import hc.core.GlobalConditionWatcher;
import hc.core.HCConditionWatcher;
import hc.core.HCConnection;
import hc.core.HCMessage;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IEventHCListener;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.SessionManager;
import hc.core.cache.PendStore;
import hc.core.data.DataInputEvent;
import hc.core.data.DataSelectTxt;
import hc.core.data.XorPackageData;
import hc.core.util.BooleanValue;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.MobileAgent;
import hc.core.util.ReturnableRunnable;
import hc.core.util.Stack;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPriorityManager;
import hc.core.util.UIUtil;
import hc.server.J2SEServerURLAction;
import hc.server.KeepaliveManager;
import hc.server.ScreenServer;
import hc.server.data.screen.PNGCapturer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.msb.Robot;
import hc.server.msb.RobotEvent;
import hc.server.msb.RobotListener;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.ClientDesc;
import hc.server.ui.ClientFontSize;
import hc.server.ui.ClientSession;
import hc.server.ui.ICanvas;
import hc.server.ui.IMletCanvas;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.MenuItem;
import hc.server.ui.ProjectContext;
import hc.server.ui.ResParameter;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SessionMobiMenu;
import hc.server.util.SystemEventListener;
import hc.server.util.VoiceParameter;
import hc.util.BaseResponsor;
import hc.util.ResourceUtil;
import hc.util.UpdateOneTimeRunnable;

/**
 * 该实例在ScreenServer中，被用于对象锁。
 *
 */
public final class J2SESession extends CoreSession {
	private static long sessionIDCounter = 1;
	private Vector<String> alertOnKeys;
	public ClientSession clientSession;
	public final ClientFontSize clientFontSize = new ClientFontSize();
	public boolean isTranedMletBody;
	public final UIEventInput uiEventInput = new UIEventInput();
	public JSEventCenterDriver jsEventProcessor;
	public final BooleanValue memberIDSetStatus = new BooleanValue();
	public boolean isNeedRemoveCacheLater;

	public final boolean isPreLineOff() {
		return hcConnection.isStartLineOffProcess;
	}

	public J2SESession() {
		synchronized (J2SESession.class) {
			sessionID = sessionIDCounter++;
		}
		keepaliveManager = new KeepaliveManager(this);
		urlAction = new J2SEServerURLAction();
	}

	/**
	 * 
	 * @param currentMemberID
	 * @param isSendMessageToOthers
	 *            true means if same then send message to all same session.
	 * @return
	 */
	public final boolean checkSameMemberIDInSys(final String currentMemberID, final boolean isSendMessageToOthers) {
		boolean isFindSame = false;
		final CoreSession[] sessions = SessionManager.getAllSocketSessions();
		for (int i = 0; i < sessions.length; i++) {
			final CoreSession coreSS = sessions[i];
			if (coreSS == this) {
				continue;
			}

			final J2SESession j2seCoreSS = (J2SESession) coreSS;
			final String memID = UserThreadResourceUtil.getMobileMemberID(j2seCoreSS);
			if (currentMemberID.equals(memID)) {
				isFindSame = true;
				if (isSendMessageToOthers) {
					sendMemberIDUsingByOther(j2seCoreSS, currentMemberID);
				}
			}
		}

		if (isFindSame) {
			sendMemberIDUsingByOther(this, currentMemberID);
		}
		return isFindSame;
	}

	private final void sendMemberIDUsingByOther(final J2SESession j2seCoreSS, final String currentMemberID) {
		String msg = ResourceUtil.get(j2seCoreSS, 9273);// 9273=Member ID
														// [{memID}] is being
														// used by other client.
		msg = StringUtil.replace(msg, "{memID}", currentMemberID);
		final J2SESession[] j2seCoreSSArray = { j2seCoreSS };
		ServerUIAPIAgent.sendMessageViaCoreSSInUserOrSys(j2seCoreSSArray, ResourceUtil.getWarnI18N(j2seCoreSS), msg,
				ProjectContext.MESSAGE_WARN, null, 0);
	}

	@Override
	public final boolean isExchangeStatus() {
		if (hcConnection.isStartLineOffProcess) {
			return false;
		}

		final IContext ctx = context;
		return ctx != null && ctx.getStatus() == ContextManager.STATUS_SERVER_SELF;
	}

	public final void notifyCanvasMenuResponse() {
		L.V = L.WShop ? false : LogManager.log("[CanvasMenu] notify menu response.");
		HCURLUtil.sendCmd(this, HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_NOTIFY_CANVAS_MENU_DONE, IConstant.TRUE);
	}

	/**
	 * true means need to send to client.
	 * 
	 * @param projAlertKey
	 * @return
	 */
	public final boolean alertOn(final String projAlertKey) {
		synchronized (this) {
			if (alertOnKeys == null) {
				alertOnKeys = new Vector<String>(2);
			}
			final boolean isNotAlert = (alertOnKeys.size() == 0);
			if (alertOnKeys.contains(projAlertKey) == false) {
				alertOnKeys.add(projAlertKey);
			}
			return isNotAlert;
		}
	}

	/**
	 * true means need to send to client.
	 * 
	 * @param projAlertKey
	 * @return
	 */
	public final boolean alertOff(final String projAlertKey) {
		synchronized (this) {
			if (alertOnKeys == null) {
				return false;
			}
			return alertOnKeys.remove(projAlertKey) && alertOnKeys.size() == 0;// 注意：如果没有该key，则不进行send
		}
	}

	final ArrayList<Object> waitLock = new ArrayList<Object>(2);

	public final boolean addWaitLock(final Object lock) {
		synchronized (waitLock) {
			if (isReleased) {
				return false;
			}

			waitLock.add(lock);
		}
		return true;
	}

	public final void removeWaitLock(final Object lock) {
		synchronized (waitLock) {
			waitLock.remove(lock);
		}
	}

	@Override
	public final void synXorPackageID(final byte[] bs) {
		L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] receive SYN_XOR_PACKAGE_ID");

		final XorPackageData xpd = XorPackageData.buildEmptyPackageData();
		// try{
		// Thread.sleep(ThreadPriorityManager.NET_RESPONSE_DELAY_MS);//等待就绪
		// }catch (Exception e) {
		// }

		xpd.setBytes(bs);

		final J2SESession oldCoreSS = (J2SESession) SessionManager.getCoreSessionByConnectionID(xpd.getConnectionPackageID());
		// final J2SESession oldCoreSS =
		// (J2SESession)ContextManager.getThreadPool().runAndWait(new
		// ReturnableRunnable() {
		// @Override
		// public Object run() throws Throwable {
		// return
		// SessionManager.getCoreSessionByConnectionID(xpd.getConnectionPackageID());
		// }
		// }, threadToken);

		if (oldCoreSS != null && UserThreadResourceUtil.isInServing(oldCoreSS.context)) {
			final HCConnection oldServConn = oldCoreSS.getHCConnection();

			L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] find old server HcConnection, ready to swap connection.");
			final DataInputStream dropInputStream = swapSocket(oldServConn, hcConnection, true);
			try {
				L.V = L.WShop ? false : LogManager.log("drop old DataInputStream in ReceiveServer.");
				dropInputStream.close();// 废弃的dis可能占用ReceiveServer的线程，所以要强制close
			} catch (final Throwable e) {
			}

			oldCoreSS.keepaliveManager.keepalive.resetTimerCount();
			oldCoreSS.keepaliveManager.sendAlive(oldCoreSS.context.getStatus());

			oldCoreSS.notifyContinue();
		} else {
			L.V = L.WShop ? false : LogManager.log("the old session is NOT serving (maybe released), stop swap connection!!!");
		}

		GlobalConditionWatcher.addWatcher(new DelayWatcher(ThreadPriorityManager.REBUILD_SWAP_SOCK_MIN_MS - 2000) {
			@Override
			public void doBiz() {
				L.V = L.WShop ? false : LogManager.log("[ConnectionRebuilder] close connection after success renewal.");
				J2SESession.this.notifyLineOff(false, false);
			}
		});
	}

	Map<String, Vector<String>> sessionScheduler;

	public final void addScheduler(final String projectID, final String domain) {
		synchronized (J2SESession.class) {
			if (sessionScheduler == null) {
				sessionScheduler = new HashMap<String, Vector<String>>(2);
			}
			Vector<String> vector = sessionScheduler.get(projectID);
			if (vector == null) {
				vector = new Vector<String>(2);
				sessionScheduler.put(projectID, vector);
			}
			vector.add(domain);
		}
	}

	public static final double NO_PERMISSION_LOC = -1;

	public Location location = new Location();

	public final void setLocation(final Location location) {
		this.location = location;
	}

	final void shutdowScheduler(final ProjectContext ctx) {
		if (sessionScheduler != null) {
			final Vector<String> vector;
			synchronized (J2SESession.class) {
				vector = sessionScheduler.remove(ctx.getProjectID());
			}
			if (vector == null) {
				return;
			} else {
				ServerUIAPIAgent.shutdownSchedulers(ctx, vector);
			}
		}
	}

	public final long getSessionID() {
		return sessionID;
	}

	public final int[] base64PngData = new int[UIUtil.ICON_MAX * UIUtil.ICON_MAX];
	public int refreshMillSecond = 3000;
	public int mask;
	private long sessionID;
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
	public final HashMap<Integer, ResParameter> questionOrDialogMap = new HashMap<Integer, ResParameter>();
	public int questionOrDialogID = 1;
	public Stack mletHistoryUrl;
	public boolean isEventMobileLoginDone = false;
	public final Vector<SystemEventListener> sessionLevelEventListeners = new Vector<SystemEventListener>();
	private final HashMap<Robot, ArrayList<RobotListener>> sessionLevelRobotListeners = new HashMap<Robot, ArrayList<RobotListener>>(2);
	public final SessionEventManager sessionEventManager = new SessionEventManager();
	public boolean isTransedCertToMobile = false;
	public static final J2SESession NULL_J2SESESSION_FOR_PROJECT = null;
	public Object[] mobileValuesForCSS = null;
	private final HashMap<String, SessionMobiMenu> menuItemsMap = new HashMap<String, SessionMobiMenu>(8);

	public final synchronized boolean lockIdelSession() {
		if (isIdelSession == false) {
			return false;
		} else {
			J2SESessionManager.startNewIdleSession();

			isIdelSession = false;
			L.V = L.WShop ? false : LogManager.log("a session is leave idel : " + hashCode());
			return true;
		}
	}

	public final SessionMobiMenu getMenu(final String projectID) {
		return menuItemsMap.get(projectID);
	}

	public final Vector<MenuItem> getDisplayMenuItems(final String projectID) {
		return getMenu(projectID).getFlushMenuItems();
	}

	public final MenuItem searchMenuItem(final String projectID, final String urlLower, final String aliasUrlLower) {
		return getMenu(projectID).searchMenuItem(urlLower, aliasUrlLower);
	}

	public final void setSessionMenu(final String projectID, final SessionMobiMenu menu) {
		menuItemsMap.put(projectID, menu);
	}

	public final void startUpdateOneTimeKeysProcess() {
		if (hcConnection.isBuildedUPDChannel && hcConnection.isDoneUDPChannelCheck) {
			// LogManager.log("is using UDP, skip
			// startUpdateOneTimeKeysProcess");
		} else if (isOnRelay()) {
			final UpdateOneTimeRunnable updateOneTimeKeysRunnable = new UpdateOneTimeRunnable(this, hcConnection);
			ContextManager.getThreadPool().run(updateOneTimeKeysRunnable);
			hcConnection.updateOneTimeKeysRunnable = updateOneTimeKeysRunnable;
			if (L.isInWorkshop) {
				LogManager.log("success startUpdateOneTimeKeysProcess!");
			}
		}
	}

	public final void notifyMobileLogout() {
		ServerUIUtil.notifyCacheSoftUIDLogout();

		if (clientSession != null) {
			ServerUIAPIAgent.notifyClientSessionWaitObjectShutdown(clientSession);
		}

		final UpdateOneTimeRunnable updateOneTimeKeysRunnable = (UpdateOneTimeRunnable) hcConnection.updateOneTimeKeysRunnable;

		if (updateOneTimeKeysRunnable != null) {
			hcConnection.isStopRunning = true;
			hcConnection.notifyOneTimeReceiveNotifyLock();
		}

		if (L.isInWorkshop) {
			LogManager.log("successful stop UpdateOneTimeRunnable!");
		}
	}

	/**
	 * 没有返回null
	 * 
	 * @param cmd
	 * @return
	 */
	public final MenuItem searchMenuItemByVoiceCommand(final VoiceParameter cmd) {
		final BaseResponsor resp = ServerUIUtil.getResponsor();
		String currProjID = null;
		if (resp != null) {
			currProjID = ((MobiUIResponsor) resp).getCurrProjectID(this);
		}
		if (currProjID != null) {// 当前工程优先
			final SessionMobiMenu menu = getMenu(currProjID);
			if (menu != null) {
				final MenuItem item = menu.searchMenuItemByVoiceCommand(cmd, true);
				if (item != null) {
					return item;
				}
			}
		}

		final Iterator<String> projs = menuItemsMap.keySet().iterator();
		try {
			while (projs.hasNext()) {
				final String projID = projs.next();
				if (projID.equals(currProjID)) {
					continue;
				}
				final SessionMobiMenu menu = getMenu(projID);
				final MenuItem item = menu.searchMenuItemByVoiceCommand(cmd, false);
				if (item != null) {
					return item;
				}
			}
		} catch (final NoSuchElementException e) {
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	public final void addRobotListener(final Robot robot, final RobotListener listener) {
		synchronized (sessionLevelRobotListeners) {
			ArrayList<RobotListener> list = sessionLevelRobotListeners.get(robot);
			if (list == null) {
				list = new ArrayList<RobotListener>(2);
				sessionLevelRobotListeners.put(robot, list);
			}
			list.add(listener);
		}
	}

	public final boolean removeRobotListener(final Robot robot, final RobotListener listener) {
		synchronized (sessionLevelRobotListeners) {
			final ArrayList<RobotListener> list = sessionLevelRobotListeners.get(robot);
			if (list == null) {
				return false;
			} else {
				return list.remove(listener);
			}
		}
	}

	public final void dispatchRobotEventSynchronized(final ProjResponser resp, final Robot robot, final RobotEvent event) {
		final ArrayList<RobotListener> list = sessionLevelRobotListeners.get(robot);
		int size = 0;
		if (list == null || (size = list.size()) == 0) {
			return;
		}

		final int p_size = size;
		final SessionContext mobileSession = resp.getMobileSession(this);
		if (mobileSession != null) {
			mobileSession.recycleRes.threadPool.runAndWait(new ReturnableRunnable() {// 因event回收，所以wait
				@Override
				public Object run() throws Throwable {
					try {
						for (int i = 0; i < p_size; i++) {
							final RobotListener robotListener = list.get(i);
							robotListener.action(event);
						}
					} catch (final IndexOutOfBoundsException outOfBound) {
						// 越界或不存在或已删除
					} catch (final Throwable e) {
						ExceptionReporter.printStackTrace(e);
					}
					return null;
				}
			});
		}
	}

	private boolean isReleased;

	@Override
	public void release() {
		synchronized (this) {
			if (isReleased) {
				return;
			} else {
				isReleased = true;
			}
		}

		super.release();

		ResParameter[] values;
		synchronized (questionOrDialogMap) {
			final Collection<ResParameter> set = questionOrDialogMap.values();
			final int size = set.size();
			values = new ResParameter[size];
			values = set.toArray(values);
			// questionOrDialogMap.clear();
		}

		for (int i = 0; i < values.length; i++) {
			final ResParameter resPara = values[i];
			try {
				ServerUIAPIAgent.exitDialogMlet(resPara, false, true);
			} catch (final Throwable e) {
				e.printStackTrace();
			}
			resPara.quesLock.notifyWaitStop(this, false, true);
			LogManager.log(resPara.toString() + " is released by line off!");
		}

		synchronized (waitLock) {
			final int size = waitLock.size();
			for (int i = size - 1; i >= 0; i--) {
				final Object lock = waitLock.get(i);
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		}

		if (L.isInWorkshop) {
			LogManager.log("release questionOrDialogMap.");
		}

		keepaliveManager = null;

	}

	public final void initScreenEvent() {
		eventCenter.addListener(new IEventHCListener() {
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_UP;
			}

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession) coreSS).cap.moveUp(pixle);
				return true;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_LEFT;
			}

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession) coreSS).cap.moveRight((-1) * pixle);
				return true;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_DOWN;
			}

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession) coreSS).cap.moveUp((-1) * pixle);
				return true;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_MOVE_RIGHT;
			}

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				final int pixle = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession) coreSS).cap.moveRight(pixle);
				return true;
			}
		});

		// 输入事件
		eventCenter.addListener(new IEventHCListener() {
			final int offset = DataInputEvent.screen_id_index;
			final DataInputEvent e = new DataInputEvent();

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				e.setBytes(bs);
				final int screenIDLen = e.getScreenIDLen();
				final J2SESession j2seSession = (J2SESession) coreSS;
				Object screenCap = j2seSession.currScreen;
				if (ScreenServer.isMatchScreen(screenCap, bs, offset, screenIDLen) == false) {
					screenCap = ScreenServer.searchDialog(j2seSession, bs, offset, screenIDLen);
					if (screenCap == null) {
						// 从其它form中搜寻
						screenCap = ScreenServer.searchScreen(j2seSession, bs, offset, screenIDLen);
						if (screenCap == null) {
							L.V = L.WShop ? false : LogManager.errForShop("Not found screen ID : " + new String(bs, offset, screenIDLen));
							LogManager.warning("target may be closed, skip event input.");
							return true;
						}
					}
				}

				((PNGCapturer) screenCap).actionInput(e);
				// LogManager.errToLog("unable to action input for : " +
				// screenCap.getClass().getName());
				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_INPUT_EVENT;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			final int screenIDIndex = MsgBuilder.INDEX_MSG_DATA + 1;

			private boolean actionJSInput(final Object canvas, final byte[] bs, final int screenIDlen) {
				final IMletCanvas iMletCanvas = (IMletCanvas) canvas;
				if (iMletCanvas.isSameScreenID(bs, screenIDIndex, screenIDlen)) {
					final int totalMsgLen = HCMessage.getMsgLen(bs);
					iMletCanvas.actionJSInput(bs, screenIDIndex + screenIDlen, totalMsgLen - 1 - screenIDlen);
					return true;
				}
				return false;
			}

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				final int screenIDlen = ByteUtil.oneByteToInteger(bs, MsgBuilder.INDEX_MSG_DATA);

				final J2SESession j2seCoreSS = (J2SESession) coreSS;
				final ICanvas screenCap = j2seCoreSS.currScreen;

				if (screenCap != null && (screenCap instanceof IMletCanvas) && actionJSInput(screenCap, bs, screenIDlen)) {
					return true;
				} else {
					final IMletCanvas dialogCap = ScreenServer.searchDialog(j2seCoreSS, bs, screenIDIndex, screenIDlen);
					if (dialogCap != null && actionJSInput(dialogCap, bs, screenIDlen)) {
						return true;
					}

					// 其它form中搜寻
					final Enumeration e = j2seCoreSS.mobiScreenMap.elements();
					try {
						while (e.hasMoreElements()) {
							final Object ele = e.nextElement();
							if (ele instanceof IMletCanvas) {
								if (actionJSInput(ele, bs, screenIDlen)) {
									return true;
								}
							}
						}
					} catch (final NoSuchElementException ex) {
					}
				}
				L.V = L.WShop ? false : LogManager.errForShop("Not found screen ID : " + new String(bs, screenIDIndex, screenIDlen));
				LogManager.warning("form/dialog/screen may be closed, skip JavaScript event input.");
				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_JS_EVENT_TO_SERVER;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			final DataSelectTxt txt = new DataSelectTxt();

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				txt.setBytes(bs);

				((J2SESession) coreSS).cap.dragAndDrop(txt.getStartX(), txt.getStartY(), txt.getEndX(), txt.getEndY());
				return true;
			}

			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_SELECT_TXT;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_COLOR_MODE;
			}

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				// Donate会变量此值，所以不宜做属性。
				final int mode = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);

				((J2SESession) coreSS).updateColorBit(mode);
				return true;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_REFRESH_MILLSECOND;
			}

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				// Donate会变量此值，所以不宜做属性。

				final int millSecond = (int) ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_MSG_DATA);

				((J2SESession) coreSS).updateRefreshMS(millSecond);
				return true;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			@Override
			public final byte getEventTag() {
				return MsgBuilder.E_SCREEN_ZOOM;
			}

			@Override
			public final boolean action(final byte[] bs, final CoreSession coreSS, final HCConnection hcConnection) {
				final int zoomMulti = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
				((J2SESession) coreSS).cap.zoom(zoomMulti);
				return true;
			}
		});

	}

	@Override
	protected void delayToSetNull() {
		GlobalConditionWatcher.addWatcher(new DelayWatcher(1000 * ResourceUtil.getIntervalSecondsForNextStartup()) {
			@Override
			public void doBiz() {
				setNull();
			}
		});
	}

	public final void updateRefreshMS(int millSecond) {
		if (millSecond == MobileAgent.INT_UN_KNOW) {
			return;
		}

		final int msOnRelay = Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_MS_On_Relay));
		if (isOnRelay()) {
			if (millSecond < msOnRelay) {
				millSecond = msOnRelay;
			}
		} else {
			final short mode = context.getConnectionModeStatus();
			if (mode == ContextManager.MODE_CONNECTION_HOME_WIRELESS) {
				millSecond = 100;
			} else if (mode == ContextManager.MODE_CONNECTION_PUBLIC_UPNP_DIRECT) {
				millSecond = Math.min(millSecond, 1000);
			} else if (mode == ContextManager.MODE_CONNECTION_PUBLIC_DIRECT) {
				millSecond = Math.min(millSecond, 1000);
			}
		}

		LogManager.log("Client change refresh MillSecond to:" + millSecond);
		refreshMillSecond = millSecond;
	}

	public final void updateColorBit(int mode) {
		if (mode == MobileAgent.INT_UN_KNOW) {
			return;
		}

		final int colorOnRelay = Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_Color_On_Relay));
		if (isOnRelay()) {
			if ((IConstant.COLOR_STAR_TOP - mode) > colorOnRelay) {
				mode = (IConstant.COLOR_STAR_TOP - colorOnRelay);
			}
		} else {
			final short connMode = context.getConnectionModeStatus();
			if (connMode == ContextManager.MODE_CONNECTION_HOME_WIRELESS) {
				// 取最大值
				mode = IConstant.COLOR_64_BIT;
			} else if (connMode == ContextManager.MODE_CONNECTION_PUBLIC_UPNP_DIRECT) {
				mode = Math.min(mode, IConstant.COLOR_16_BIT);
			} else if (connMode == ContextManager.MODE_CONNECTION_PUBLIC_DIRECT) {
				mode = Math.min(mode, IConstant.COLOR_32_BIT);
			}

		}

		LogManager.log("Client change colorMode to level : " + (IConstant.COLOR_STAR_TOP - mode) + " (after limited)");

		mask = UIUtil.getMaskFromBit(mode);
	}

	@Override
	public final HCConditionWatcher getJSEventProcessor() {
		if (jsEventProcessor == null) {
			synchronized (J2SESession.class) {
				if (jsEventProcessor == null) {
					jsEventProcessor = new JSEventCenterDriver();
					if (isReleased) {
						jsEventProcessor.notifyShutdown();
					}
				}
			}
		}
		return jsEventProcessor;
	}
}
