package hc.server.ui;

import hc.App;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.IContext;
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
import hc.server.MultiUsingManager;
import hc.server.ScreenServer;
import hc.server.TrayMenuUtil;
import hc.server.html5.syn.DifferTodo;
import hc.server.html5.syn.MletHtmlCanvas;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.msb.Workbench;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.JarMainMenu;
import hc.server.ui.design.ProjResponser;
import hc.server.util.SystemEventListener;
import hc.util.I18NStoreableHashMapWithModifyFlag;
import hc.util.ResourceUtil;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JToggleButton;

public class ServerUIAPIAgent {
	public final static String CONVERT_NAME_PROP = Workbench.SYS_RESERVED_KEYS_START + "convert_name_prop";
	public final static String DEVICE_NAME_PROP = Workbench.SYS_RESERVED_KEYS_START + "device_name_prop";
	public final static String ROBOT_NAME_PROP = Workbench.SYS_RESERVED_KEYS_START + "robot_name_prop";
	
	public final static String CLIENT_SESSION_ATTRIBUTE_IS_TRANSED_MLET_BODY = Workbench.SYS_RESERVED_KEYS_START + "mlet_html_body";
	
	final static Object threadToken = App.getThreadPoolToken();
	
	private final static Vector sessionListSnapThreadSafe = J2SESessionManager.getSessionList();
	
	static final CoreSession[] getAllSocketSessionsNoCheck(){
		synchronized (sessionListSnapThreadSafe) {
			final int size = sessionListSnapThreadSafe.size();
			final CoreSession[] out = new CoreSession[size];
			int copyIdx = 0;
			
			for (; copyIdx < size; copyIdx++) {
				out[copyIdx] = (CoreSession)sessionListSnapThreadSafe.elementAt(copyIdx);
			}
			
			return out;
		}
	}
	
	public final static MenuItem buildMobiMenuItem(final String name, final int type, final String image, final String url, 
			final I18NStoreableHashMapWithModifyFlag i18nName, final String listener, final String extendMap){
		return new MenuItem(name, type, image, url, i18nName, listener, extendMap);
	}

	public final static String getMobiMenuItem_Name(final MenuItem item){
		return item.itemName;
	}
	
	public final static int getMobiMenuItem_Type(final MenuItem item){
		return item.itemType;
	}
	
	public final static String getMobiMenuItem_Image(final JarMainMenu jarMenu, final J2SESession coreSS,
			final MenuItem item, final int targetMobileIconSize){
		String imgData = item.itemImage;
		
		if(imgData.startsWith(UIUtil.SYS_ICON_PREFIX)){
		}else{
			//仅传送适合目标手机尺寸的图标
			BufferedImage oriImage = item.cacheOriImage;
			if(oriImage == null){
				oriImage = ServerUIUtil.base64ToBufferedImage(imgData);
				item.cacheOriImage = oriImage;
			}
			if(oriImage != null){
				if(UIUtil.isIntBeiShu(targetMobileIconSize, oriImage.getWidth())){
					imgData = jarMenu.getBitmapBase64ForMobile(coreSS, oriImage, imgData);
				}else{
					//服务器端进行图片缩放
					final BufferedImage newImg = ResourceUtil.resizeImage(oriImage, targetMobileIconSize, targetMobileIconSize);
					//使用适应尺寸的base64图标
					imgData = jarMenu.getBitmapBase64ForMobile(coreSS, newImg, null);
				}
			}else{
				imgData = jarMenu.getBitmapBase64ForMobile(coreSS, oriImage, imgData);
			}
		}
		
		return imgData;
	}
	
	public final static String getMobiMenuItem_URL(final MenuItem item){
		return item.itemURL;
	}
	
	public final static String getMobiMenuItem_URLLower(final MenuItem item){
		return item.getItemURLLower();
	}
	
	public final static I18NStoreableHashMapWithModifyFlag getMobiMenuItem_I18nName(final MenuItem item){
		return item.i18nName;
	}
	
	public final static String getMobiMenuItem_Listener(final MenuItem item){
		return item.itemListener;
	}
	
	public final static String getMobiMenuItem_extendMap(final MenuItem item){
		return item.extendMap;
	}
	
	public static final void notifyStatusChangedForMlet(final Mlet mlet, final int status){
		mlet.notifyStatusChanged(status);
	}
	
	static final String getMobileLocaleFromAllSessionsNoCheck(){
		String mobileLocale = null;
		
		synchronized (sessionListSnapThreadSafe) {
			final int size = sessionListSnapThreadSafe.size();
			
			for (int i = 0; i < size; i++) {
				final J2SESession session = (J2SESession)sessionListSnapThreadSafe.elementAt(i);
				if(UserThreadResourceUtil.isInServing(session.context)){
					final String curMobileLocale = UserThreadResourceUtil.getMobileLocaleFrom(session);
					if(mobileLocale == null){
						mobileLocale = curMobileLocale;
					}else if(mobileLocale.equals(curMobileLocale) == false){
						mobileLocale = null;//取消
						break;
					}
				}
			}
		}
		
		if(mobileLocale != null){
			return mobileLocale;
		}
		
		return SimuMobile.MOBILE_DEFAULT_LOCALE;
	}
	
	/**
	 * 
	 * @return 如果没有联机的，则返回null
	 */
	public static final J2SESession[] getAllOnlineSocketSessions(){
		CCoreUtil.checkAccess();
		
		return getAllOnlineSocketSessionsNoCheck();
	}
	
	/**
	 * 
	 * @return 如果没有联机的，则返回null
	 */
	static final J2SESession[] getAllOnlineSocketSessionsNoCheck(){
		Vector<J2SESession> vector = null;
		
		synchronized (sessionListSnapThreadSafe) {
			final int size = sessionListSnapThreadSafe.size();
			for (int i = 0; i < size; i++) {
				final J2SESession coreSS = (J2SESession)sessionListSnapThreadSafe.elementAt(i);
				if(UserThreadResourceUtil.isInServing(coreSS.context)){
					if(vector == null){
						vector = new Vector();
					}
					vector.addElement(coreSS);
				}
			}
		}
			
		if(vector == null){
			return null;
		}else{
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
	
	public static int buildQuestionID(){
		synchronized (questionCreateLock) {
			questionID++;
			if(questionID == Integer.MAX_VALUE){
				questionID = 1;
			}
			return questionID;
		}
	}
	
	public static QuestionParameter buildQuestionID(final J2SESession coreSS, 
			final ProjectContext ctx, final QuestionGlobalLock quesLock, final int questionID, final String quesDesc,
			final Runnable yesRunnable, final Runnable noRunnable, final Runnable cancelRunnable){
		final QuestionParameter qp = new QuestionParameter(quesLock);
		
		qp.questionDesc = quesDesc;
		
		qp.ctx = ctx;
		qp.yesRunnable = yesRunnable;
		qp.noRunnable = noRunnable;
		qp.cancelRunnable = cancelRunnable;
		
		coreSS.questionMap.put(questionID, qp);
		
		return qp;
	}
	
	private static Boolean isLoggerOn;
	
	static final boolean isLoggerOn(){
		if(isLoggerOn == null){
			isLoggerOn = (Boolean)runAndWaitInSysThread(new ReturnableRunnable() {
				@Override
				public Object run() {
					return ResourceUtil.isLoggerOn();
				}
			});
		}
		return isLoggerOn;
	}
	
	public final static void setProjectContext(final Mlet mlet, final ProjectContext ctx){
		mlet.__context = ctx;
	}
	
	public static void runInProjContext(final ProjectContext ctx, final Runnable run){
		ctx.projectPool.run(run);
	}
	
	public static Object runAndWaitInProjContext(final ProjectContext ctx, final ReturnableRunnable run){
		return ctx.projectPool.runAndWait(run);
	}
	
	public static void runInSessionThreadPool(final J2SESession coreSS, final ProjResponser resp, final Runnable run){
		resp.getMobileSession(coreSS).sessionPool.run(run);
	}
	
	public static void runAndWaitInSessionThreadPool(final J2SESession coreSS, final ProjResponser resp, final Runnable run){
		resp.getMobileSession(coreSS).sessionPool.runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				run.run();
				return null;
			}
		});
	}
	
	public static void printInProjectLevelWarn(final String instance, final String method){
		LogManager.warning(instance + "." + method + "() runs in project level.");
	}
	
	public static void printInProjectLevelWarn(final String method){
		LogManager.warning("projectContext." + method + "() runs in project level, perhaps in MSB Processor.");
	}
	
	public static void execQuestionResult(final J2SESession coreSS, final String ques_id, final String result){
		try{
			final int int_id = Integer.parseInt(ques_id);
			final QuestionParameter qp = removeQuestionFromMap(coreSS, int_id);
			if(qp != null){//有可能被撤消
				final QuestionGlobalLock quesLock = qp.quesLock;
				if(quesLock != null){
					synchronized (quesLock) {
						if(quesLock.isProcessed){
							final String invalid = (String)ResourceUtil.get(9237) + qp.questionDesc;
							final J2SESession[] coreSSS = {coreSS};
							ServerUIAPIAgent.sendMovingMsg(coreSSS, invalid);
							
							return;
						}
						quesLock.isProcessed = true;
					}
					
					//撤消其它
					quesLock.cancelOthers(int_id, coreSS);
				}
				
				final ProjResponser resp = qp.ctx.__projResponserMaybeNull;
				if("yes".equals(result)){
					if(qp.yesRunnable != null){
						ServerUIAPIAgent.runInSessionThreadPool(coreSS, resp, qp.yesRunnable);
					}
				}else if("no".equals(result)){
					if(qp.noRunnable != null){
						ServerUIAPIAgent.runInSessionThreadPool(coreSS, resp, qp.noRunnable);
					}
				}else if("cancel".equals(result)){
					if(qp.cancelRunnable != null){
						ServerUIAPIAgent.runInSessionThreadPool(coreSS, resp, qp.cancelRunnable);
					}
				}
			}
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	protected static QuestionParameter removeQuestionFromMap(final J2SESession coreSS,
			final int int_id) {
		synchronized (coreSS.questionMap) {
			return coreSS.questionMap.remove(int_id);
		}
	}
	
	public final static void setHCSysProperties(final ProjectContext ctx, final String key, final String value){
		ctx.__setPropertySuper(key, value);
	}
	
	public final static String getHCSysProperties(final ProjectContext ctx, final String key){
		return ctx.__getPropertySuper(key);
	}
	
	public final static void loadStyles(final HTMLMlet mlet) {
		final Vector<String> stylesToDeliver = mlet.stylesToDeliver;
		
		if(stylesToDeliver != null){
			final int count = stylesToDeliver.size();
			for (int i = 0; i < count; i++) {
				final String styles = stylesToDeliver.elementAt(i);
				mlet.loadCSS(styles);
			}
			stylesToDeliver.clear();
		}
	}
	
	public final static void loadJS(final HTMLMlet mlet) {
		final Vector<String> jsToDeliver = mlet.jsToDeliver;
		
		if(jsToDeliver != null){
			final int count = jsToDeliver.size();
			for (int i = 0; i < count; i++) {
				final String JS = jsToDeliver.elementAt(i);
				mlet.loadJS(JS);
			}
			jsToDeliver.clear();
		}
	}
	
	public final static void flushCSS(final HTMLMlet mlet, final DifferTodo diffTodo) {//in user thread
		if(mlet.styleItemToDeliver != null){
			final int count = mlet.styleItemToDeliver.size();
			for (int i = 0; i < count; i++) {
				final StyleItem item = mlet.styleItemToDeliver.elementAt(i);
				if(item.forType == StyleItem.FOR_DIV){
					mlet.setCSSForDiv(item.component, item.className, item.styles);//in user thread
				}else if(item.forType == StyleItem.FOR_JCOMPONENT){
					mlet.setCSS(item.component, item.className, item.styles);//in user thread
				}else if(item.forType == StyleItem.FOR_JTOGGLEBUTTON){
					mlet.setCSSForToggle((JToggleButton)item.component, item.className, item.styles);//in user thread
				}
			}
			mlet.styleItemToDeliver.clear();
		}
		
		//由于与MletSnapCanvas共用，所以增加null检查
		if(diffTodo != null){
			diffTodo.notifyInitDone();
		}
	}
	
	public final static boolean isOnTopHistory(final J2SESession coreSS, final ProjectContext ctx, final String urlLower){
		synchronized(coreSS){
			if(coreSS.mletHistoryUrl != null){
				final int size = coreSS.mletHistoryUrl.size();
				if(size > 0){
					final Object targetURL = coreSS.mletHistoryUrl.elementAt(size - 1);
					final boolean isTop = targetURL.equals(urlLower) || targetURL.equals(HCURL.buildMletAliasURL(urlLower));
					if(L.isInWorkshop){
						L.V = L.O ? false : LogManager.log("===>" + urlLower + " is on top : " + isTop);
					}
					return isTop;
				}
			}
			return false;
		}
	}
	
	public final static boolean pushMletURLToHistory(final J2SESession coreSS, final ProjectContext ctx, final String urlLower) {
		synchronized(coreSS){
			if(coreSS.mletHistoryUrl == null){
				coreSS.mletHistoryUrl = new Stack();
			}
			
			final Stack mletHistoryUrl = coreSS.mletHistoryUrl;
			
			final int size = mletHistoryUrl.size();
			if(size > 0){
				final String mletAliasURL = HCURL.buildMletAliasURL(urlLower);
				
				int idx = mletHistoryUrl.search(urlLower);
				if(idx >= 0 || ((idx = mletHistoryUrl.search(mletAliasURL)) >= 0)){
					mletHistoryUrl.removeAt(idx);//从队列中删除
					mletHistoryUrl.push(urlLower);//置于顶
					
					ScreenServer.pushToTopForMlet(coreSS, urlLower);
					if(L.isInWorkshop){
						L.V = L.O ? false : LogManager.log("===>sucessful bring to Top : " + urlLower);
					}
					return false;
				}
			}
	
	//		System.out.println("----------pushMletURLToHistory : " + url);
			mletHistoryUrl.push(urlLower);//注意：有可能对HTMLMlet的form://xx，但实际手机效果为Mlet。
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("===>push into history url : " + urlLower);
			}
			return true;
		}
	}
	
	public final static void removeMletURLHistory(final J2SESession coreSS, final String targetURL){
		final int removeIdx;
		final Stack history = coreSS.mletHistoryUrl;
		synchronized(coreSS){
			removeIdx = history.search(targetURL.toLowerCase());
			if(removeIdx >= 0){
				history.removeAt(removeIdx);
			}
		}
		if(L.isInWorkshop){
			if(removeIdx >= 0){
				L.V = L.O ? false : LogManager.log("===>pop out from history url : " + targetURL);
			}
		}
	}
	
	public static final void set__projResponserMaybeNull(final ProjectContext ctx, final ProjResponser resp){
		ctx.__projResponserMaybeNull = resp;
	}
	
	public static ProjResponser getProjResponserMaybeNull(final ProjectContext ctx){
		return ctx.__projResponserMaybeNull;
	}
	
	public static final void setDiffTodo(final HTMLMlet mlet, final DifferTodo diff){
		mlet.diffTodo = diff;
	}
	
	/**
	 * 
	 * @param coreSS 工程级请求时，为null
	 * @param ctx
	 * @return
	 */
	public static final Enumeration<SystemEventListener> getSystemEventListener(final J2SESession coreSS, final ProjectContext ctx){
		//返回一个Vector新实例的
		if(coreSS != null){
			final Vector<SystemEventListener> total = new Vector<SystemEventListener>(ctx.projectLevelEventListeners.size() + coreSS.sessionLevelEventListeners.size());
			total.addAll(ctx.projectLevelEventListeners);
			total.addAll(coreSS.sessionLevelEventListeners);
			return total.elements();
		}else{
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
	public final static void __sendTextOfCtrlButton(final CoreSession coreSS, final int keyValue, final String text){
		if(UserThreadResourceUtil.isInServing(coreSS.context)){
			final String[] keys = {"key", "text"};
			final String[] values = {String.valueOf(keyValue), text};
			HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_CTRL_BTN_TXT, keys, values);
		}
	}

	public final static void setSuperProp(final ProjectContext ctx, final String propName, final String value){
		ctx.__setPropertySuper(propName, value);
	}

	public final static void removeSuperProp(final ProjectContext ctx, final String propName){
		ctx.__removePropertySuper(propName);
	}

	public final static String getSuperProp(final ProjectContext ctx, final String propName){
		return ctx.__getPropertySuper(propName);
	}

	public final static void setSuperAttribute(final ProjectContext ctx, final String attributeName, final Object value){
		ctx.__setAttributeSuper(attributeName, value);
	}

	public final static void removeSuperAttribute(final ProjectContext ctx, final String attributeName){
		ctx.__removeAttributeSuper(attributeName);
	}

	public final static Object getSuperAttribute(final ProjectContext ctx, final String attributeName){
		return ctx.__getAttributeSuper(attributeName);
	}

	public final static void removeSuperClientSessionAttribute(final J2SESession coreSS, final ProjResponser pr, final String attributeName){
		pr.getMobileSession(coreSS).getClientSession().removeAttribute(attributeName);
	}

	public final static Object getClientSessionAttribute(final J2SESession coreSS, final ProjResponser pr, final String attributeName){
		return pr.getMobileSession(coreSS).getClientSession().getAttribute(attributeName);
	}

	public final static void setClientSessionAttribute(final J2SESession coreSS, final ProjResponser pr, final String attributeName, final Object value){
		pr.getMobileSession(coreSS).getClientSession().setAttribute(attributeName, value);
	}
	
	public static String getProcessorNameFromCtx(final ProjectContext ctx, String name, final String prop) {
		if(name != null && name.length() > 0){
		}else{
			name = getSuperProp(ctx, prop);
			removeSuperProp(ctx, prop);
		}
		if(name == null){
			name = "";
		}
		return name;
	}


	static final Object runAndWaitInSysThread(final ReturnableRunnable returnRun){
		return ContextManager.getThreadPool().runAndWait(returnRun, threadToken);
	}


	static final void runInSysThread(final Runnable run){
		ContextManager.getThreadPool().run(run, threadToken);
	}

	public static final String CURRENT_THREAD_IS_IN_PROJECT_LEVEL = "current thread is in project level, perhaps in MSB (Robot, Converter, Device).";
	public static final String ERR_CURRENT_THREAD_IS_IN_PROJECT_LEVEL = "it is NOT allowed that current thread is in project level.";

	public static void sendMessageViaCoreSS(final J2SESession[] coreSS,
			String caption, String text, final int type,
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
		final String url = HCURL.CMD_PROTOCAL + HCURL.HTTP_SPLITTER + HCURL.DATA_CMD_MSG
				+ "?caption=" + StringUtil.replace(caption, "&", "\\&")
				+ "&text=" + StringUtil.replace(text, "&", "\\&")
				+ "&timeOut=" + timeOut + "&type=" + String.valueOf(type)
				+ (imageData);//注意：必须在外部进行转换
	
		//如果同时发出两个msg，则可能不同步，所以以下要wait
		runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
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
			public Object run() {
				TrayMenuUtil.displayMessage(ResourceUtil.getInfoI18N(), msg, IContext.INFO, null, 0);		
				return null;
			}
		});
	}


	public static void sendMovingMsg(final J2SESession[] coreSS, final String msg) {
		if(coreSS == null){
			return;
		}
		
		runAndWaitInSysThread(new ReturnableRunnable() {
			@Override
			public Object run() {
				for (int i = 0; i < coreSS.length; i++) {
					HCURLUtil.sendCmd(coreSS[i], HCURL.DATA_CMD_MOVING_MSG, "value", msg);
				}
				return null;
			}
		});
	}

	public static void openMlet(final J2SESession coreSS, final ProjectContext context, final Mlet toMlet, final String targetOfMlet,
				final boolean isAutoReleaseCurrentMlet, final Mlet fromMlet) {
			final int httpSplitterIdx = targetOfMlet.indexOf(HCURL.HTTP_SPLITTER);
			final boolean isWithHttpSplitter = httpSplitterIdx > 0;
			final String targetURL = isWithHttpSplitter?targetOfMlet:HCURL.buildStandardURL(HCURL.FORM_PROTOCAL, targetOfMlet);
			
			//优先检查bringMletToTop
			if(ProjResponser.bringMletToTop(coreSS, context, targetURL.toLowerCase())){
				return;
			}
			
			if(fromMlet != null){//可能为null，比如从addHar
				runAndWaitInSessionThreadPool(coreSS, getProjResponserMaybeNull(context), new Runnable() {
					@Override
					public void run() {
						fromMlet.setAutoReleaseAfterGo(isAutoReleaseCurrentMlet);
					}
				});
			}
			
			toMlet.__target = targetURL;//后置式，注意：不是新建时获得。
			final String elementID = isWithHttpSplitter?targetOfMlet.substring(httpSplitterIdx + HCURL.HTTP_SPLITTER.length()):targetOfMlet;
			openMlet(coreSS, elementID, "title-" + elementID, context, toMlet);
			return;
		}

	/**
		 * 
		 * @param coreSS 
		 * @param elementID 标识当前对象的唯一串，长度不限。同时被应用于缓存。注：是设计器中url.elementID段
		 * @param title
		 * @param context
		 * @param mlet
		 */
		private static void openMlet(final J2SESession coreSS, final String elementID,
				final String title, final ProjectContext context, final Mlet mlet) {
			if(L.isInWorkshop){
				L.V = L.O ? false : LogManager.log("openMlet elementID : " + elementID);
			}
			
			boolean isHTMLMlet = (mlet instanceof HTMLMlet);
			final IMletCanvas mcanvas;
			if(isHTMLMlet == false || ProjResponser.isMletMobileEnv(coreSS) || ProjResponser.getMletComponentCount(coreSS, context, mlet) == 0){
				if(isHTMLMlet){
					L.V = L.O ? false : LogManager.log("force HTMLMlet to Mlet, because there is no component in it or for J2ME mobile.");
					isHTMLMlet = false;
				}
				ProjResponser.sendReceiver(coreSS, HCURL.DATA_RECEIVER_MLET, elementID);
				mcanvas = new MletSnapCanvas(coreSS, UserThreadResourceUtil.getMobileWidthFrom(coreSS), UserThreadResourceUtil.getMobileHeightFrom(coreSS));
			}else{
				ProjResponser.sendMletBodyOnlyOneTime(coreSS, context);
				ProjResponser.sendReceiver(coreSS, HCURL.DATA_RECEIVER_HTMLMLET, elementID);
				mcanvas = new MletHtmlCanvas(coreSS, UserThreadResourceUtil.getMobileWidthFrom(coreSS), UserThreadResourceUtil.getMobileHeightFrom(coreSS));
			}
			
			mcanvas.setScreenIDAndTitle(elementID, title);//注意：要在setMlet之前，因为后者可能用到本参数
			
	//		try{
	//			Thread.sleep(ThreadPriorityManager.UI_WAIT_MS * 3);//如果手机性能较差，可能会导致手机端对象正在初始中，而后续数据已送达。
	//		}catch (final Throwable e) {
	//		}
	
			mcanvas.setMlet(coreSS, mlet, context);
			runAndWaitInSessionThreadPool(coreSS, getProjResponserMaybeNull(context), new Runnable() {
				@Override
				public void run() {
					mcanvas.init();//in user thread
				}
			});
			
			ScreenServer.pushScreen(coreSS, (ICanvas)mcanvas);
			MultiUsingManager.enter(coreSS, context.getProjectID(), mlet.__target);
			
			if(isHTMLMlet){
				L.V = L.O ? false : LogManager.log(" onStart HTMLMlet form : [" + title + "]");
			}else{
				L.V = L.O ? false : LogManager.log(" onStart Mlet form : [" + title + "]");
			}
		}

}
