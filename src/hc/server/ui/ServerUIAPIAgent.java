package hc.server.ui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Permission;
import java.util.Enumeration;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JToggleButton;

import hc.App;
import hc.core.BaseWatcher;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.Stack;
import hc.core.util.StringUtil;
import hc.core.util.UIUtil;
import hc.server.CallContext;
import hc.server.MultiUsingManager;
import hc.server.ScreenServer;
import hc.server.TrayMenuUtil;
import hc.server.html5.syn.DifferTodo;
import hc.server.html5.syn.MletHtmlCanvas;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.JarMainMenu;
import hc.server.ui.design.MobiUIResponsor;
import hc.server.ui.design.ProjResponser;
import hc.server.ui.design.SessionContext;
import hc.server.ui.design.SystemDialog;
import hc.server.ui.design.engine.ScriptValue;
import hc.server.util.Assistant;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.ServerAPIAgent;
import hc.server.util.SystemEventListener;
import hc.util.I18NStoreableHashMapWithModifyFlag;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;
import hc.util.ThreadConfig;

public class ServerUIAPIAgent {
	public static final String QUESTION_CANCEL = "cancel";

	public final static String KEY_IS_INSTALL_FROM_CLIENT = CCoreUtil.SYS_RESERVED_KEYS_START + "_isInstallFromClient";
	public final static String KEY_HAS_ERROR_PS_SCRIPT = CCoreUtil.SYS_RESERVED_KEYS_START + "_hasErrorPSScript";// SYS_PROJ_STARTUP

	// 以下存放于hc_config.properties，而非user/project.properties
	public final static String CONVERT_NAME_PROP = CCoreUtil.SYS_RESERVED_KEYS_START + "convert_name_prop";
	public final static String DEVICE_NAME_PROP = CCoreUtil.SYS_RESERVED_KEYS_START + "device_name_prop";
	public final static String ROBOT_NAME_PROP = CCoreUtil.SYS_RESERVED_KEYS_START + "robot_name_prop";
	public final static String PROJ_DB_PASSWORD = CCoreUtil.SYS_RESERVED_KEYS_START + "db_pwd";

	// 以下存放于user/project.properties，而非hc_config.properties
	public final static String PROJ_CRON_DB_COMPACT_MS = CCoreUtil.SYS_RESERVED_KEYS_START + "cron_db_compact_ms_";
	public final static String PROJ_USER_DB_COMPACT_MS = CCoreUtil.SYS_RESERVED_KEYS_START + "user_db_compact_ms_";

	final static Object threadToken = App.getThreadPoolToken();

	static {
		ServerAPIAgent.init();
	}

	public static void notifyClientSessionWaitObjectShutdown(final ClientSession clientSession) {
		clientSession.notifyClientSessionWaitObjectShutdown();
	}

	public static ProjDesignConf getProjDesignConf(final ProjectContext ctx) {
		return ctx.projDesignConfig;
	}

	public final static ScriptCSSSizeHeight getScriptCSSSizeHeight(final HTMLMlet htmlMlet) {
		return htmlMlet.sizeHeightForXML;
	}

	public static ProjResponser getCurrentProjResponser(final J2SESession coreSS) {
		final MobiUIResponsor responsor = (MobiUIResponsor) ServerUIUtil.getResponsor();
		return responsor.getCurrentProjResponser(coreSS);// 必须使用用户级实例，比如clientSession
	}

	public static ClientSession buildClientSession(final J2SESession j2seCoreSS, final boolean hasLocationOfMobile) {
		return new ClientSession(j2seCoreSS, hasLocationOfMobile);
	}

	public static void notifyClientSessionQRResult(final ClientSession clientSession, final String result) {
		clientSession.notifyQRCode(result);
	}

	public static void removeScheduler(final ProjectContext ctx, final String domain) {
		ctx.removeScheduler(domain);
	}

	public static void shutdownSchedulers(final ProjectContext ctx, final Vector<String> list) {
		ctx.shutdownSchedulers(list);
	}

	public final static MobiMenu getBelongMobiMenu(final MenuItem item) {
		return item.belongToMenu;
	}

	public static ProjectContext getProjectContextFromMlet(final Mlet mlet) {
		return mlet.__context;
	}

	private final static Vector sessionListSnapThreadSafe = J2SESessionManager.getSessionList();

	public static boolean isEnableApplyOrientationWhenRTL(final Mlet mlet) {
		return mlet.enableApplyOrientationWhenRTL;
	}

	public static ProjectContext getProjectContextFromCtrl(final CtrlResponse ctrl) {
		return ctrl.context;
	}

	static final CoreSession[] getAllSocketSessionsNoCheck() {
		synchronized (sessionListSnapThreadSafe) {
			final int size = sessionListSnapThreadSafe.size();
			final CoreSession[] out = new CoreSession[size];
			int copyIdx = 0;

			for (; copyIdx < size; copyIdx++) {
				out[copyIdx] = (CoreSession) sessionListSnapThreadSafe.elementAt(copyIdx);
			}

			return out;
		}
	}

	public final static MenuItem buildMobiMenuItem(final String name, final int type, final String image, final String url,
			final I18NStoreableHashMapWithModifyFlag i18nName, final String listener, final String extendMap) {
		return new MenuItem(name, type, image, url, i18nName, listener, extendMap);
	}

	public final static void sendDialog(final DialogParameter dialogParameter, final J2SESession coreSS, final Dialog dialog_p,
			final Runnable buildProc, final ProjectContext ctx, final DialogGlobalLock dialogLock) {
		final String elementID = HCURL.DIALOG_PRE + dialogLock.dialogID;

		final CallContext runCtx = CallContext.getFree();
		final String targetURL = HCURL.buildStandardURL(HCURL.FORM_PROTOCAL, elementID);
		runCtx.targetURL = targetURL;
		final Mlet dialogMlet = (Mlet) runAndWaitInSessionThreadPool(coreSS, getProjResponserMaybeNull(ctx), new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				try {
					final Dialog dialog;
					if (dialog_p != null) {
						dialog = dialog_p;
						dialog.dialogCanvas.__target = runCtx.targetURL;
					} else {
						ThreadConfig.setThreadTargetURL(runCtx);
						buildProc.run();
						dialog = (Dialog) ThreadConfig.getValue(ThreadConfig.BUILD_DIALOG_INSTANCE, true);
						// LogManager.log("dialog sub component : " +
						// dialog.getComponentCount());
						if (dialog == null) {
							throw new IllegalArgumentException(
									"fail to build Dialog instance from ProjectContext.sendDialogByBulding(Runnable)");
						}
					}
					// if(dialog.getComponentCount() == 0){
					// LogManager.warning("there is no components in
					// Dialog, it seems that NOT invoke super in
					// initialize method.");
					// }

					if (dialog.dialogCanvas instanceof DialogHTMLMlet) {
						final DialogHTMLMlet dm = (DialogHTMLMlet) dialog.dialogCanvas;
						dm.setDialogGlobalLock(dialogLock);
						dm.addDialog(dialog);
					} else {
						final DialogMlet dm = (DialogMlet) dialog.dialogCanvas;// 如果是j2ME，则返回DialogMlet
						dm.setDialogGlobalLock(dialogLock);
						dm.addDialog(dialog);
					}

					dialog.dialogParameter = dialogParameter;
					return dialog.dialogCanvas;
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}

				return null;
			}
		});
		CallContext.cycle(runCtx);
		if (dialogMlet == null) {
			return;
		}

		final String screenID = ServerUIAPIAgent.buildScreenID(ctx.getProjectID(), targetURL);

		boolean isCancelable = true;
		if (dialogMlet instanceof DialogHTMLMlet) {
			isCancelable = ((DialogHTMLMlet) dialogMlet).dialog.isCancelable;
		}
		if (isCancelable == false) {
			dialogMlet.setCancelableToNo();
		}
		openMletImpl(coreSS, screenID, "title-" + screenID, ctx, dialogMlet);
	}

	public final static String getMobiMenuItem_Name(final MenuItem item) {
		return item.itemName;
	}

	public final static int getMobiMenuItem_Type(final MenuItem item) {
		return item.itemType;
	}

	public final static String getMobiMenuItem_Image(final JarMainMenu jarMenu, final J2SESession coreSS, final MenuItem item,
			final int targetMobileIconSize) {
		String imgData = item.itemImage;

		if (imgData.startsWith(UIUtil.SYS_ICON_PREFIX)) {
		} else {
			// 仅传送适合目标手机尺寸的图标
			BufferedImage oriImage = item.cacheOriImage;
			if (oriImage == null) {
				oriImage = ServerUIUtil.base64ToBufferedImage(imgData);
				item.cacheOriImage = oriImage;
			}
			if (oriImage != null) {
				if (UIUtil.isIntBeiShu(targetMobileIconSize, oriImage.getWidth())) {
					imgData = jarMenu.getBitmapBase64ForMobile(coreSS, oriImage, imgData);
				} else {
					// 服务器端进行图片缩放
					final BufferedImage newImg = ResourceUtil.resizeImage(oriImage, targetMobileIconSize, targetMobileIconSize);
					// 使用适应尺寸的base64图标
					imgData = jarMenu.getBitmapBase64ForMobile(coreSS, newImg, null);
				}
			} else {
				imgData = jarMenu.getBitmapBase64ForMobile(coreSS, oriImage, imgData);
			}
		}

		return imgData;
	}

	public final static String getMobiMenuItem_URL(final MenuItem item) {
		return item.itemURL;
	}

	public final static String getMobiMenuItem_URLLower(final MenuItem item) {
		return item.getItemURLLower();
	}

	public final static I18NStoreableHashMapWithModifyFlag getMobiMenuItem_I18nName(final MenuItem item) {
		return item.i18nName;
	}

	public final static ScriptValue getMobiMenuItem_Listener(final MenuItem item) {
		return item.itemListenerSV;
	}

	public final static String getMobiMenuItem_extendMap(final MenuItem item) {
		return item.extendMap;
	}

	public static final void notifyStatusChangedForMlet(final Mlet mlet, final int status) {
		mlet.notifyStatusChanged(status);
	}

	static final String getMobileLocaleFromAllSessionsNoCheck() {
		String mobileLocale = null;

		synchronized (sessionListSnapThreadSafe) {
			final int size = sessionListSnapThreadSafe.size();

			for (int i = 0; i < size; i++) {
				final J2SESession session = (J2SESession) sessionListSnapThreadSafe.elementAt(i);
				if (UserThreadResourceUtil.isInServing(session.context)) {
					final String curMobileLocale = UserThreadResourceUtil.getMobileLocaleFrom(session);
					if (mobileLocale == null) {
						mobileLocale = curMobileLocale;
					} else if (mobileLocale.equals(curMobileLocale) == false) {
						mobileLocale = null;// 取消
						break;
					}
				}
			}
		}

		if (mobileLocale != null) {
			return mobileLocale;
		}

		return SimuMobile.MOBILE_DEFAULT_LOCALE;
	}

	/**
	 * 
	 * @return 如果没有联机的，则返回null
	 */
	public static final J2SESession[] getAllOnlineSocketSessions() {
		CCoreUtil.checkAccess();

		return getAllOnlineSocketSessionsNoCheck();
	}

	static final boolean isClientLineOn() {
		synchronized (sessionListSnapThreadSafe) {
			final int size = sessionListSnapThreadSafe.size();
			for (int i = 0; i < size; i++) {
				final J2SESession coreSS = (J2SESession) sessionListSnapThreadSafe.elementAt(i);
				if (UserThreadResourceUtil.isInServing(coreSS.context)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @return 如果没有联机的，则返回null
	 */
	static final J2SESession[] getAllOnlineSocketSessionsNoCheck() {
		Vector<J2SESession> vector = null;

		synchronized (sessionListSnapThreadSafe) {
			final int size = sessionListSnapThreadSafe.size();
			for (int i = 0; i < size; i++) {
				final J2SESession coreSS = (J2SESession) sessionListSnapThreadSafe.elementAt(i);
				if (UserThreadResourceUtil.isInServing(coreSS.context)) {
					if (vector == null) {
						vector = new Vector();
					}
					vector.addElement(coreSS);
				}
			}
		}

		if (vector == null) {
			return null;
		} else {
			final int size = vector.size();
			final J2SESession[] out = new J2SESession[size];
			for (int i = 0; i < size; i++) {
				out[i] = vector.elementAt(i);
			}
			return out;
		}
	}

	private static int questionID;
	private static Object questionCreateLock = new Object();

	public static int buildQuestionID() {
		return buildResID();
	}

	public static int buildDialogID() {
		return buildResID();
	}

	private static int buildResID() {
		synchronized (questionCreateLock) {
			if (++questionID == Integer.MAX_VALUE) {
				questionID = 1;// 手机端Alert的questionID==0
			}
			return questionID;
		}
	}

	public static DialogParameter buildDialogParameter(final J2SESession coreSS, final ProjectContext ctx,
			final DialogGlobalLock dialogLock, final int dialogID) {
		final DialogParameter dp = new DialogParameter(coreSS, dialogLock);

		register(coreSS, ctx, dialogID, dp);

		return dp;
	}

	public static QuestionParameter buildQuestionParameter(final J2SESession coreSS, final ProjectContext ctx,
			final QuestionGlobalLock quesLock, final int questionID, final String quesDesc, final Runnable yesRunnable,
			final Runnable noRunnable, final Runnable cancelRunnable) {
		final QuestionParameter qp = new QuestionParameter(quesLock, questionID);

		qp.questionDesc = quesDesc;

		qp.yesRunnable = yesRunnable;
		qp.noRunnable = noRunnable;
		qp.cancelRunnable = cancelRunnable;

		register(coreSS, ctx, questionID, qp);

		return qp;
	}

	private static void register(final J2SESession coreSS, final ProjectContext ctx, final int resID, final ResParameter qp) {
		qp.ctx = ctx;
		synchronized (coreSS.questionOrDialogMap) {
			coreSS.questionOrDialogMap.put(resID, qp);
		}
	}

	static final boolean isLoggerOn() {//不能 cache
		return (Boolean) runAndWaitInSysThread(new ReturnableRunnable() {//注意：不能cache，因为用户可能option
			@Override
			public Object run() throws Throwable {
				return ResourceUtil.isLoggerOn();
			}
		});
	};

	public final static void setProjectContext(final Mlet mlet, final ProjectContext ctx) {
		mlet.__context = ctx;
	}

	/**
	 * 需要占用project优先位，但是专为Session任务，确保Session任务完成，才能进行相关后续project任务。
	 * 
	 * @param coreSS
	 * @param resp
	 * @param runnable
	 */
	public static void addSequenceWatcherInProjContextForSessionFirst(final J2SESession coreSS, final ProjResponser resp,
			final ReturnableRunnable runnable) {
		addSequenceWatcherInProjContext(resp.context, new BaseWatcher() {
			@Override
			public boolean watch() {
				ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, resp, runnable);
				return true;
			}
		});
	}

	public static void addSequenceWatcherInProjContext(final ProjectContext ctx, final BaseWatcher watcher) {
		ctx.recycleRes.sequenceWatcher.addWatcher(watcher);
	}

	/**
	 * 停用，被addSequenceWatcherInProjContextForSessionFirst代替
	 */
	public static void execInSequenceForSession(final J2SESession coreSS, final ProjResponser resp, final ReturnableRunnable runnable) {
		final SessionContext mobileSession = resp.getMobileSession(coreSS);
		if (mobileSession != null) {
			mobileSession.recycleRes.sequenceWatcher.addWatcher(new BaseWatcher() {
				@Override
				public boolean watch() {
					ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, resp, runnable);
					return true;
				}
			});
		}
	}

	public static void runInProjContext(final ProjectContext ctx, final Runnable run) {
		ctx.recycleRes.threadPool.run(run);
	}
	
	public static void setIDEConsoleWriterForProjectContext(final ProjectContext ctx) {
		ctx.ideConsoleWriter = SimuMobile.getConsoleWriter();
	}

	public static Object runAndWaitInProjContext(final ProjectContext ctx, final ReturnableRunnable run) {
		return ctx.recycleRes.threadPool.runAndWait(run, null);
	}

	public static void runInSessionThreadPool(final J2SESession coreSS, final ProjResponser resp, final Runnable run) {
		final SessionContext mobileSession = resp.getMobileSession(coreSS);
		if (mobileSession != null) {// && coreSS.isExchangeStatus()
			mobileSession.recycleRes.threadPool.run(run);
		}
	}

	public static Assistant getVoiceAssistant(final ProjectContext ctx) {
		return ctx.assistant;
	}

	public static Object runAndWaitInSessionThreadPool(final J2SESession coreSS, final ProjResponser resp, final ReturnableRunnable run) {
		final SessionContext mobileSession = resp.getMobileSession(coreSS);
		if (mobileSession != null) {
			return mobileSession.recycleRes.threadPool.runAndWait(run);
		} else {
			L.V = L.WShop ? false : LogManager.log("mobileSession is null!");
			return null;
		}
	}

	public static void printInProjectLevelWarn(final String instance, final String method) {
		LogManager.warning(instance + "." + method + "() runs in project level.");
	}

	public static void printInProjectLevelWarn(final String method) {
		LogManager.warning("projectContext." + method + "() runs in project level, perhaps in MSB Processor.");
	}

	public static void execQuestionResult(final J2SESession coreSS, final String ques_id, final String result) {
		try {
			final int int_id = Integer.parseInt(ques_id);
			final QuestionParameter qp = (QuestionParameter) removeQuestionDialogFromMap(coreSS, int_id, false, false);
			if (qp != null) {// 有可能被撤消
				execQuestionResult(coreSS, qp, int_id, result);
			}
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static boolean execQuestionResult(final J2SESession coreSS, final QuestionParameter qp, final int int_id, final String result) {
		final QuestionGlobalLock quesLock = qp.getGlobalLock();
		if (quesLock.isForMultiple) {
			if (quesLock.isProcessed(coreSS, int_id, ResourceUtil.get(coreSS, 9237) + qp.questionDesc)) {
				return false;
			}
		}

		coreSS.uiEventInput.setQuestionEvent();

		final ProjResponser resp = qp.ctx.__projResponserMaybeNull;
		if ("yes".equals(result)) {
			return processQuestionRunnable(false, coreSS, resp, qp.yesRunnable, quesLock);
		} else if ("no".equals(result)) {
			return processQuestionRunnable(false, coreSS, resp, qp.noRunnable, quesLock);
		} else if (QUESTION_CANCEL.equals(result)) {
			return processQuestionRunnable(true, coreSS, resp, qp.cancelRunnable, quesLock);
		}

		return false;
	}

	private static boolean processQuestionRunnable(final boolean isFromCancel, final J2SESession coreSS, final ProjResponser resp,
			final Runnable run, final QuestionGlobalLock quesLock) {
		if (run != null) {
			final Runnable implRun = new Runnable() {
				@Override
				public void run() {
					try {
						run.run();
					} finally {
						quesLock.notifyWaitStop(coreSS, isFromCancel, false);
					}
				}
			};
			ServerUIAPIAgent.runInSessionThreadPool(coreSS, resp, implRun);
			return true;
		} else {
			quesLock.notifyWaitStop(coreSS, isFromCancel, false);// 解除锁定
			return false;
		}
	}

	public static ResParameter removeQuestionDialogFromMap(final J2SESession coreSS, final int int_id, final boolean isFromCancel,
			final boolean isFromLineOff) {
		final ResParameter out;

		synchronized (coreSS.questionOrDialogMap) {
			out = coreSS.questionOrDialogMap.remove(int_id);
		}

		exitDialogMlet(out, isFromCancel, isFromLineOff);
		return out;
	}

	public static void exitDialogMlet(final ResParameter out, final boolean isFromCancel, final boolean isFromLineOff) {
		if (out == null || out instanceof DialogParameter == false) {
			return;
		}

		final DialogParameter para = (DialogParameter) out;
		final IMletCanvas mletCanvas = para.getGlobalLock().mletCanvas;

		if (isFromCancel || isFromLineOff) {
			para.isDismissedByBack = true;
			final Mlet mlet = mletCanvas.getMlet();
			if (mlet != null && mlet instanceof DialogHTMLMlet) {
				final Dialog dialog = ((DialogHTMLMlet) mlet).dialog;
				final ProjResponser resp = ServerUIAPIAgent.getProjResponserMaybeNull(out.ctx);
				if (dialog != null && resp != null) {
					if (dialog instanceof SystemDialog) {
						((SystemDialog) dialog).setCanceled();
					}

					L.V = L.WShop ? false : LogManager.log("Dialog.dismiss() is invoked by cancel/back/lineoff.");
					ServerUIAPIAgent.runInSessionThreadPool(para.coreSS, resp, new Runnable() {
						@Override
						public void run() {
							try {
								dialog.dismiss();// 注意：cancel or line off
							} catch (final Throwable e) {
								e.printStackTrace();
							}

							if (dialog instanceof BlockSystemDialog) {
								((BlockSystemDialog) dialog).notifyBlockUserThread();
							}

							ServerUIAPIAgent.runInSysThread(new Runnable() {
								@Override
								public void run() {
									releaseExit(isFromCancel, para, mletCanvas, isFromLineOff);
								}
							});
						}
					});
					return;
				}
			}
		}

		releaseExit(isFromCancel, para, mletCanvas, isFromLineOff);// 注意：此操作有可能在上段的back的会话中调用
	}

	private static void releaseExit(final boolean isFromCancel, final DialogParameter para, final IMletCanvas mletCanvas,
			final boolean isFromLineOff) {
		mletCanvas.onExit(false);

		para.getGlobalLock().notifyWaitStop(para.coreSS, isFromCancel, isFromLineOff);
	}

	public final static void loadStyles(final HTMLMlet mlet) {
		final Vector<CacheString> stylesToDeliver = mlet.sizeHeightForXML.stylesToDeliver;

		if (stylesToDeliver != null) {
			final int count = stylesToDeliver.size();
			for (int i = 0; i < count; i++) {
				final CacheString cs = stylesToDeliver.elementAt(i);
				mlet.sizeHeightForXML.loadStylesImpl(mlet, cs.jsOrStyles, cs.isCacheEnabled);
			}
			stylesToDeliver.clear();
		}
	}

	public final static void flushJSForScriptPanel(final HTMLMlet mlet) {
		final ScriptCSSSizeHeight sizeHeightForXML = mlet.sizeHeightForXML;
		{
			final Vector<CacheString> scriptToDeliver = sizeHeightForXML.scriptToDeliver;

			if (scriptToDeliver != null) {
				final int count = scriptToDeliver.size();
				for (int i = 0; i < count; i++) {
					final CacheString cs = scriptToDeliver.elementAt(i);
					sizeHeightForXML.loadScriptImplForScriptPanel(mlet, cs.jsOrStyles, cs.isCacheEnabled);
				}
				scriptToDeliver.clear();
			}
		}

		final Vector<String> jsNoCacheToDeliver = sizeHeightForXML.jsNoCacheToDeliver;
		if (jsNoCacheToDeliver != null) {
			final int count = jsNoCacheToDeliver.size();
			for (int i = 0; i < count; i++) {
				final String JS = jsNoCacheToDeliver.elementAt(i);
				sizeHeightForXML.diffTodo.executeJSWithoutCacheForScriptPanel(JS);
			}
			jsNoCacheToDeliver.clear();
		}
	}

	public final static void flushCSS(final HTMLMlet mlet, final DifferTodo diffTodo) {// in
																						// user
																						// thread
		if (mlet.sizeHeightForXML.styleItemToDeliver != null) {
			final int count = mlet.sizeHeightForXML.styleItemToDeliver.size();
			for (int i = 0; i < count; i++) {
				final TodoItem item = mlet.sizeHeightForXML.styleItemToDeliver.elementAt(i);
				if (item.forType == TodoItem.FOR_DIV) {
					final StyleItem styleItem = (StyleItem) item;
					mlet.setCSSForDiv(item.component, styleItem.className, styleItem.styles);// in
																								// user
																								// thread
				} else if (item.forType == TodoItem.FOR_JCOMPONENT) {
					final StyleItem styleItem = (StyleItem) item;
					mlet.setCSS(item.component, styleItem.className, styleItem.styles);// in
																						// user
																						// thread
				} else if (item.forType == TodoItem.FOR_JTOGGLEBUTTON) {
					final StyleItem styleItem = (StyleItem) item;
					mlet.setCSSForToggle((JToggleButton) item.component, styleItem.className, styleItem.styles);// in user thread
				} else if (item.forType == TodoItem.FOR_RTL) {
					mlet.setRTL(item.component, ((RTLItem) item).isRTL);
				}
			}
			mlet.sizeHeightForXML.styleItemToDeliver.clear();
		}

		// 由于与MletSnapCanvas共用，所以增加null检查
		if (diffTodo != null) {
			diffTodo.notifyInitDone();
		}
	}

	public final static boolean isOnTopHistory(final J2SESession coreSS, final ProjectContext ctx, final String screenIDLower,
			final String targetURLLower) {
		synchronized (coreSS) {
			if (coreSS.mletHistoryUrl != null) {
				final int size = coreSS.mletHistoryUrl.size();
				if (size > 0) {
					final Object targetURL = coreSS.mletHistoryUrl.elementAt(size - 1);
					final boolean isTop = targetURL.equals(screenIDLower) || targetURL
							.equals(ServerUIAPIAgent.buildScreenID(ctx.getProjectID(), HCURL.buildMletAliasURL(targetURLLower)));
					if (L.isInWorkshop) {
						LogManager.log("===>" + screenIDLower + " is on top : " + isTop);
					}
					return isTop;
				}
			}
			return false;
		}
	}

	public final static boolean pushMletURLToHistory(final J2SESession coreSS, final ProjectContext ctx, final String screenIDLower,
			final String targetURLLower) {
		synchronized (coreSS) {
			if (coreSS.mletHistoryUrl == null) {
				coreSS.mletHistoryUrl = new Stack();
			}

			final Stack mletHistoryUrl = coreSS.mletHistoryUrl;

			final int size = mletHistoryUrl.size();
			if (size > 0) {
				final String mletAliasURL = ServerUIAPIAgent.buildScreenID(ctx.getProjectID(), HCURL.buildMletAliasURL(targetURLLower))
						.toLowerCase();

				int idx = mletHistoryUrl.search(screenIDLower);
				if (idx >= 0 || ((idx = mletHistoryUrl.search(mletAliasURL)) >= 0)) {
					mletHistoryUrl.removeAt(idx);// 从队列中删除
					mletHistoryUrl.push(screenIDLower);// 置于顶

					ScreenServer.pushToTopForMlet(coreSS, screenIDLower);
					if (L.isInWorkshop) {
						LogManager.log("===>sucessful bring to Top : " + screenIDLower);
					}
					return false;
				}
			}

			// System.out.println("----------pushMletURLToHistory : " + url);
			mletHistoryUrl.push(screenIDLower);// 注意：有可能对HTMLMlet的form://xx，但实际手机效果为Mlet。
			if (L.isInWorkshop) {
				LogManager.log("===>push into history url : " + screenIDLower);
			}
			return true;
		}
	}

	public final static void removeMletURLHistory(final J2SESession coreSS, final String projectID, final String targetURL) {
		final int removeIdx;
		final Stack history = coreSS.mletHistoryUrl;
		if (history != null) {
			final String screenIDLower = buildScreenID(projectID, targetURL).toLowerCase();
			synchronized (coreSS) {
				removeIdx = history.search(screenIDLower);
				if (removeIdx >= 0) {
					history.removeAt(removeIdx);
				}
			}
			if (L.isInWorkshop) {
				if (removeIdx >= 0) {
					LogManager.log("===>pop out from history url : " + targetURL);
				}
			}
		}
	}

	public static final void notifyShutdown(final ProjectContext ctx) {
		ctx.shutdown();
	}

	public static final void set__projResponserMaybeNull(final ProjectContext ctx, final ProjResponser resp) {
		ctx.__projResponserMaybeNull = resp;
	}

	public static ProjResponser getProjResponserMaybeNull(final ProjectContext ctx) {
		return ctx.__projResponserMaybeNull;
	}

	public static final void setDiffTodo(final HTMLMlet mlet, final DifferTodo diff) {
		mlet.sizeHeightForXML.diffTodo = diff;
	}

	/**
	 * 
	 * @param coreSS
	 *            工程级请求时，为null
	 * @param ctx
	 * @return
	 */
	public static final Enumeration<SystemEventListener> getSystemEventListener(final J2SESession coreSS, final ProjectContext ctx) {
		// 返回一个Vector新实例的
		if (coreSS != null) {
			final Vector<SystemEventListener> total = new Vector<SystemEventListener>(
					ctx.projectLevelEventListeners.size() + coreSS.sessionLevelEventListeners.size());
			total.addAll(ctx.projectLevelEventListeners);
			total.addAll(coreSS.sessionLevelEventListeners);
			return total.elements();
		} else {
			return ctx.projectLevelEventListeners.elements();
		}
	}

	/**
	 * @param coreSS
	 * @param keyValue
	 * @param text
	 * @deprecated
	 */
	@Deprecated
	public final static void __sendTextOfCtrlButton(final CoreSession coreSS, final int keyValue, final String text) {
		if (UserThreadResourceUtil.isInServing(coreSS.context)) {
			final String[] keys = { "key", "text" };
			final String[] values = { String.valueOf(keyValue), text };
			HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_CTRL_BTN_TXT, keys, values);
		}
	}

	public final static void saveHCSysProperties(final ProjectContext ctx) {
		ctx.__saveSysPropertiesOnHC();
	}

	public final static void setHCSysProperties(final ProjectContext ctx, final String propName, final String value) {
		ctx.__setPropertySuperOnHC(propName, value);
	}

	public final static void setSysPropertiesOnProj(final ProjectContext ctx, final String propName, final String value) {
		ctx.__setPropertySuperOnProj(propName, value);
	}

	public final static void removeHCSysProperties(final ProjectContext ctx, final String propName) {
		ctx.__removePropertySuperOnHC(propName);
	}

	public static final String CRATE_DB_PASSWORD = null;

	public final static String getDBPassword(final ProjectContext ctx) {
		return ctx.dbPassword;
	}

	public final static String getHCSysProperties(final ProjectContext ctx, final String propName) {
		return ctx.__getPropertySuperOnHC(propName);
	}

	public final static String getSysPropertiesOnProj(final ProjectContext ctx, final String propName) {
		return ctx.__getPropertySuperOnProj(propName);
	}

	public final static void setSysAttribute(final ProjResponser pr, final String attributeName, final Object value) {
		pr.__setSysAtt(attributeName, value);
	}

	public final static void removeSysAttribute(final ProjResponser pr, final String attributeName) {
		pr.__removeSysAtt(attributeName);
	}

	public final static Object getSysAttribute(final ProjResponser pr, final String attributeName, final boolean isClearAfterGet) {
		return pr.__getSysAtt(attributeName, isClearAfterGet);
	}

	public final static void removeClientSessionAttributeForSys(final J2SESession coreSS, final ProjResponser pr,
			final String attributeName) {
		final SessionContext mobileSession = pr.getMobileSession(coreSS);
		if (mobileSession != null) {
			mobileSession.getClientSessionForSys().removeAttribute(attributeName);
		}
	}

	public final static Object getClientSessionAttributeForSys(final J2SESession coreSS, final ProjResponser pr,
			final String attributeName) {
		final SessionContext mobileSession = pr.getMobileSession(coreSS);
		if (mobileSession != null) {
			return mobileSession.getClientSessionForSys().getAttribute(attributeName);
		} else {
			return null;
		}
	}

	public final static void setClientSessionAttributeForSys(final J2SESession coreSS, final ProjResponser pr, final String attributeName,
			final Object value) {
		final SessionContext mobileSession = pr.getMobileSession(coreSS);
		if (mobileSession != null) {
			mobileSession.getClientSessionForSys().setAttribute(attributeName, value);
		}
	}

	public static String getProcessorNameFromCtx(final ProjectContext ctx, String name, final String prop) {
		if (name != null && name.length() > 0) {
		} else {
			name = getHCSysProperties(ctx, prop);
			removeHCSysProperties(ctx, prop);
		}
		if (name == null) {
			name = "";
		}
		return name;
	}

	static final Object runAndWaitInSysThread(final ReturnableRunnable returnRun) {
		return ContextManager.getThreadPool().runAndWait(returnRun, threadToken);
	}

	static final void runInSysThread(final Runnable run) {
		ContextManager.getThreadPool().run(run, threadToken);
	}

	public static final String CURRENT_THREAD_IS_IN_PROJECT_LEVEL = "current thread is in project level, perhaps in MSB (Robot, Converter, Device).";
	public static final String ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL = "it is NOT allowed that current thread is in project level.";

	public static void sendMessageViaCoreSSInUserOrSys(final J2SESession[] coreSS, String caption, String text, final int type,
			final BufferedImage image, final int timeOut) {
		if (caption == null) {
			caption = "information";
		}
		if (text == null) {
			text = "";
		}
		String imageData = "";
		if (image != null) {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			try {
				ImageIO.write(image, "png", byteArrayOutputStream);
				final byte[] out = byteArrayOutputStream.toByteArray();
				imageData = "&image=" + ByteUtil.encodeBase64(out);
				byteArrayOutputStream.close();
			} catch (final IOException e) {
				ExceptionReporter.printStackTrace(e);
				return;
			}
		}
		final String url = HCURL.CMD_PROTOCAL + HCURL.HTTP_SPLITTER + HCURL.DATA_CMD_MSG + "?caption="
				+ StringUtil.replace(caption, "&", "\\&") + "&text=" + StringUtil.replace(text, "&", "\\&") + "&timeOut=" + timeOut
				+ "&type=" + String.valueOf(type) + (imageData);// 注意：必须在外部进行转换

		// 如果同时发出两个msg，则可能不同步，所以以下要wait
		runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				for (int i = 0; i < coreSS.length; i++) {
					coreSS[i].context.send(MsgBuilder.E_GOTO_URL, url);
				}
				return null;
			}
		});
	}

	static void tipOnTray(final String msg) {
		runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				TrayMenuUtil.displayMessage(ResourceUtil.getInfoI18N(), msg, IConstant.INFO, null, 0);
				return null;
			}
		});
	}

	public static void sendMovingMsg(final J2SESession[] coreSS, final String msg) {
		if (coreSS == null) {
			return;
		}

		runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				for (int i = 0; i < coreSS.length; i++) {
					final J2SESession oneCoreSS = coreSS[i];
					sendOneMovingMsg(oneCoreSS, msg);
				}
				return null;
			}
		});
	}

	public static void sendVoice(final J2SESession[] coreSS, final String voice) {
		if (coreSS == null) {
			return;
		}

		runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				for (int i = 0; i < coreSS.length; i++) {
					final J2SESession oneCoreSS = coreSS[i];
					sendVoice(oneCoreSS, voice);
				}
				return null;
			}
		});
	}

	public static void setMletTarget(final Mlet mlet, final String targetOfMlet) {
		mlet.__target = buildTargetForElement(targetOfMlet);// 后置式，注意：不是新建时获得。
	}

	private static String buildTargetForElement(final String targetOfMlet) {
		final int httpSplitterIdx = targetOfMlet.indexOf(HCURL.HTTP_SPLITTER);
		final boolean isWithHttpSplitter = httpSplitterIdx > 0;
		return isWithHttpSplitter ? targetOfMlet : HCURL.buildStandardURL(HCURL.FORM_PROTOCAL, targetOfMlet);
	}

	public static void openMlet(final J2SESession coreSS, final ProjectContext context, final Mlet toMlet, final String targetOfMlet,
			final boolean isAutoReleaseCurrentMlet, final Mlet fromMletMaybeNull) {
		final int httpSplitterIdx = targetOfMlet.indexOf(HCURL.HTTP_SPLITTER);
		final boolean isWithHttpSplitter = httpSplitterIdx > 0;
		final String targetURL = isWithHttpSplitter ? targetOfMlet : HCURL.buildStandardURL(HCURL.FORM_PROTOCAL, targetOfMlet);
		final String screenID = ServerUIAPIAgent.buildScreenID(context.getProjectID(), targetURL);

		// 优先检查bringMletToTop
		if (ProjResponser.bringMletToTop(coreSS, context, screenID.toLowerCase(), targetURL.toLowerCase())) {
			return;
		}

		if (fromMletMaybeNull != null && fromMletMaybeNull.isAutoReleaseAfterGo != isAutoReleaseCurrentMlet) {// 可能为null，比如从addHar
			runAndWaitInSessionThreadPool(coreSS, getProjResponserMaybeNull(context), new ReturnableRunnable() {
				@Override
				public Object run() throws Throwable {
					fromMletMaybeNull.setAutoReleaseAfterGo(isAutoReleaseCurrentMlet);
					return null;
				}
			});
		}

		// final String elementID =
		// isWithHttpSplitter?targetOfMlet.substring(httpSplitterIdx +
		// HCURL.HTTP_SPLITTER.length()):targetOfMlet;
		openMletImpl(coreSS, screenID, "title-" + screenID, context, toMlet);
		return;
	}

	public static String buildScreenID(final String projectID, final String targetURL) {
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append('@');
		sb.append(projectID);
		sb.append('@');
		sb.append(targetURL);
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		return out;
	}

	/**
	 * 
	 * @param coreSS
	 * @param screenID
	 * @param title
	 * @param context
	 * @param mlet
	 */
	public static void openMletImpl(final J2SESession coreSS, final String screenID, final String title, final ProjectContext context,
			final Mlet mlet) {
		if (L.isInWorkshop) {
			if (mlet.getTarget() == null) {
				throw new Error("target of Mlet is null");
			}
			LogManager.log("openMlet elementID : " + screenID + ", targetInMlet : " + mlet.getTarget());
		}

		boolean isHTMLMlet = (mlet instanceof HTMLMlet);
		final IMletCanvas mcanvas;
		if (isHTMLMlet == false || ProjResponser.isMletMobileEnv(coreSS)
				|| ProjResponser.getMletComponentCount(coreSS, context, mlet) == 0) {
			if (isHTMLMlet) {
				LogManager.log("force HTMLMlet to Mlet, because there is no component in it or for J2ME mobile.");
				isHTMLMlet = false;
			}
			ProjResponser.sendReceiver(coreSS, HCURL.DATA_RECEIVER_MLET, screenID, mlet.isCancelable);
			mcanvas = new MletSnapCanvas(coreSS, UserThreadResourceUtil.getMletWidthFrom(coreSS, false),
					UserThreadResourceUtil.getMletHeightFrom(coreSS, false));
		} else {
			ProjResponser.sendMletBodyOnlyOneTime(coreSS);
			ProjResponser.sendReceiver(coreSS, HCURL.DATA_RECEIVER_HTMLMLET, screenID, mlet.isCancelable);
			mcanvas = new MletHtmlCanvas(coreSS, UserThreadResourceUtil.getMletWidthFrom(coreSS),
					UserThreadResourceUtil.getMletHeightFrom(coreSS));
		}

		mcanvas.setScreenIDAndTitle(screenID, title);// 注意：要在setMlet之前，因为后者可能用到本参数

		// try{
		// Thread.sleep(ThreadPriorityManager.UI_WAIT_MS *
		// 3);//如果手机性能较差，可能会导致手机端对象正在初始中，而后续数据已送达。
		// }catch (final Throwable e) {
		// }

		mcanvas.setMlet(coreSS, mlet, context);
		runAndWaitInSessionThreadPool(coreSS, getProjResponserMaybeNull(context), new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				mcanvas.init();// in user thread
				return null;
			}
		});

		ScreenServer.pushScreen(coreSS, (ICanvas) mcanvas);
		final boolean isForm = MultiUsingManager.enter(coreSS, screenID, mlet.getTarget());

		if (isForm) {
			if (isHTMLMlet) {
				LogManager.log(" onStart HTMLMlet form : [" + title + "]");
			} else {
				LogManager.log(" onStart Mlet form : [" + title + "]");
			}
		}
	}

	static final String[] FAST_NOT_RESP_URL = { HCURL.URL_CMD_EXIT, HCURL.URL_CMD_CONFIG };

	private static boolean isCmdExitOrConfig(final String lowcase) {
		for (int i = 0; i < FAST_NOT_RESP_URL.length; i++) {
			if (FAST_NOT_RESP_URL[i].equals(lowcase)) {
				return true;
			}
		}

		return false;
	}

	public static void goInServer(final J2SESession coreSS, final ProjectContext ctx, final String url) {
		HCURL.checkSuperCmd(url);

		if (L.isInWorkshop) {
			LogManager.log("====>go : " + url);
		}
		try {
			// 不需要转码，可直接支持"screen://我的Mlet"
			// final String encodeURL = URLEncoder.encode(url, IConstant.UTF_8);
			runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() throws Throwable {
					goInSysThread(coreSS, ctx, url);
					return null;
				}
			});
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static void goExternalURL(final J2SESession coreSS, final ProjectContext ctx, final String url, final boolean isUseExtBrowser) {
		if (url.endsWith(StringUtil.JAD_EXT)) {
			throw new Error("external URL can NOT end with : " + StringUtil.JAD_EXT);
		}

		if (url.startsWith(StringUtil.URL_EXTERNAL_PREFIX) == false) {
			throw new Error("external URL must start with : " + StringUtil.URL_EXTERNAL_PREFIX);
		}

		// 检查权限
		if (HCLimitSecurityManager.isSecurityManagerOn()) {
			final Object[] exception = new Object[1];
			try {
				final HttpURLConnection urlConn = new HttpURLConnection(new URL(url)) {
					@Override
					public void connect() throws IOException {
					}

					@Override
					public boolean usingProxy() {
						return false;
					}

					@Override
					public void disconnect() {
					}
				};
				final Permission perm = urlConn.getPermission();
				runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						final HCLimitSecurityManager manager = HCLimitSecurityManager.getHCSecurityManager();
						runAndWaitInSessionThreadPool(coreSS, getProjResponserMaybeNull(ctx), new ReturnableRunnable() {
							@Override
							public Object run() throws Throwable {
								try {
									ThreadConfig.putValue(ThreadConfig.AUTO_PUSH_EXCEPTION, false);// 关闭push exception
									manager.checkPermission(perm);
								} catch (final Throwable e) {
									exception[0] = e;
								} finally {
									ThreadConfig.putValue(ThreadConfig.AUTO_PUSH_EXCEPTION, true);
								}
								return null;
							}
						});
						return null;
					}
				});
			} catch (final Throwable e) {
				throw new Error("invalid external URL : " + url);
			}
			if (exception[0] != null) {
				throw new SecurityException(exception[0].toString());
			}
		}

		if (isUseExtBrowser || ProjResponser.isMletMobileEnv(coreSS)) {
			runInSysThread(new Runnable() {
				@Override
				public void run() {
					HCURLUtil.sendEClass(coreSS, HCURLUtil.CLASS_GO_EXTERNAL_URL, url);
				}
			});
		} else {
			runInSysThread(new Runnable() {
				@Override
				public void run() {
					HCURLUtil.sendEClass(coreSS, HCURLUtil.CLASS_GO_EXTERNAL_URL, HCURLUtil.INNER_MODE + url);
				}
			});
		}
	}

	public static void goMlet(final J2SESession coreSS, final ProjectContext ctx, final Mlet fromMletMaybeNull, final Mlet toMlet,
			final String targetOfMlet, final boolean isAutoReleaseCurrentMlet) throws Error {
		if (toMlet == null) {
			throw new Error("Mlet is null when goMlet.");
		}

		if (targetOfMlet == null) {
			throw new Error("target of Mlet is null when goMlet.");
		}

		runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				setMletTarget(toMlet, targetOfMlet);
				openMlet(coreSS, ctx, toMlet, targetOfMlet, isAutoReleaseCurrentMlet, fromMletMaybeNull);
				return null;
			}
		});
	}

	public static void sendOneMovingMsg(final CoreSession oneCoreSS, final String msg) {
		HCURLUtil.sendCmd(oneCoreSS, HCURL.DATA_CMD_MOVING_MSG, "value", msg);
	}

	public static void sendVoice(final CoreSession oneCoreSS, final String voice) {
		HCURLUtil.sendCmd(oneCoreSS, HCURL.DATA_CMD_VOICE, "value", voice);
	}

	public static void goInSysThread(final J2SESession coreSS, final ProjectContext ctx, final String url) {
		final HCURL hu = HCURLUtil.extract(url);

		final String protocal = hu.protocal;
		boolean isFastRespProtocal = false;
		for (int i = 0; i < HCURL.FAST_RESP_PROTOCAL.length; i++) {
			if (protocal == HCURL.FAST_RESP_PROTOCAL[i]) {
				isFastRespProtocal = true;
				break;
			}
		}
		if (isFastRespProtocal && ((protocal == HCURL.CMD_PROTOCAL && isCmdExitOrConfig(url.toLowerCase())) == false)) {
			final MobiUIResponsor mobiResp = (MobiUIResponsor) ServerUIUtil.getResponsor();
			if (mobiResp != null) {
				final boolean done = mobiResp.findContext(ctx.getProjectID()).doBiz(coreSS, hu, false);// 直接处理，无需手机端转一圈
				L.V = L.WShop ? false : LogManager.log("exec url : " + url + ", result : " + done);
				if (done) {
					HCURLUtil.hcurlCacher.cycle(hu);
					return;
				}
			}
		}

		HCURLUtil.hcurlCacher.cycle(hu);

		L.V = L.WShop ? false : LogManager.log("tranURL to client : " + url);
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_TRANSURL, url);
	}

	public static Object getSysAttrInUserThread(final String key, final boolean isClearAfterGet) {
		return ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				final ProjResponser pr = getProjResponserMaybeNull(ProjectContext.getProjectContext());
				if (pr == null) {
					return null;
				}
				final Object out = getSysAttribute(pr, key, isClearAfterGet);
				return out;
			}
		}, threadToken);
	}

	public static void setProjProp(final ProjectContext ctx, final String propPre, final String key, final boolean value) {
		final String prop = propPre + key;
		setHCSysProperties(ctx, prop, String.valueOf(value));
		ctx.__saveSysPropertiesOnHC();
	}

	public static boolean isProjPropTrue(final ProjectContext ctx, final String propPre, final String key) {
		final String value = getHCSysProperties(ctx, propPre + key);
		if (value == null) {
			return false;
		} else {
			try {
				return Boolean.parseBoolean(value);
			} catch (final Exception e) {
			}
		}
		return false;
	}

}
