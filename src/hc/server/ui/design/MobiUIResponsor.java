package hc.server.ui.design;

import hc.App;
import hc.UIActionListener;
import hc.core.BaseWatcher;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.GlobalConditionWatcher;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.cache.CacheManager;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.server.ScreenServer;
import hc.server.msb.Device;
import hc.server.msb.MSBAgent;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.msb.WorkingDeviceList;
import hc.server.ui.ExceptionCatcherToWindow;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.MenuItem;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.SessionMobiMenu;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.ServerAPIAgent;
import hc.server.util.SystemEventListener;
import hc.server.util.VoiceCommand;
import hc.server.util.ai.AIPersistentManager;
import hc.server.util.ai.LabelManager;
import hc.server.util.ai.LuceneManager;
import hc.server.util.ai.ProjectTargetForAI;
import hc.util.BaseResponsor;
import hc.util.PropertiesManager;
import hc.util.RecycleProjThreadPool;
import hc.util.ResourceUtil;
import hc.util.UILang;

import java.awt.Window;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;

public class MobiUIResponsor extends BaseResponsor {
	public final ExceptionCatcherToWindow ec;
	int totalRobotRefDevices;
	String[] projIDs;
	Map<String, Object>[] maps;
	ProjResponser[] responsors;
	int responserSize; 
	final MSBAgent msbAgent;
	public BindRobotSource bindRobotSource;
	boolean isEventProjStartDone = false;
	long publishLocationMS = HCTimer.ONE_MINUTE * 5;
	boolean hasLocationOfMobile;
	final ThreadGroup threadToken;
	
	public final int getProjResponserSize(){
		return responserSize;
	}
	
	final void setPublishLocationMS(final long newMS){
		if(newMS < 1000){
			LogManager.error("illegal argument publish location ms : " + newMS + ", must bigger than 1000.");
			return;
		}
		
		if(hasLocationOfMobile && newMS < publishLocationMS){
			publishLocationMS = newMS;
			
			//用户级线程请求更新
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					applyLocationUpdatsToAllSessions();
					L.V = L.WShop ? false : LogManager.log("successful setPublishLocationMS : " + newMS);
				}
			}, threadToken);
		}
	}
	
	/**
	 * 更新权限状态
	 * @param hasLoc
	 */
	public final void hasLocationOfMobile(final boolean hasLoc){
		if(hasLocationOfMobile == false && hasLoc){//只要一个工程有权限，即全部会话都开启GPS
			hasLocationOfMobile = true;
			
			applyLocationUpdatsToAllSessions();
		}
	}

	private final void applyLocationUpdatsToAllSessions() {
		final J2SESession[] coreSSS = J2SESessionManager.getAllOnlineSocketSessions();
		if(coreSSS != null && coreSSS.length > 0){
			for (int i = 0; i < coreSSS.length; i++) {
				final J2SESession j2seCoreSS = coreSSS[i];
				changePublishLocationMS(j2seCoreSS);//注意：新添加工程时，触发启动会话的location
			}
		}
	}
	
	/**
	 * 注意：本方法被两次调用，其触发方式不一样 
	 * @param j2seCoreSS
	 */
	private final void changePublishLocationMS(final CoreSession j2seCoreSS){
		if(hasLocationOfMobile){//由多个工程共同决定
			final long minMS = publishLocationMS;//由多个工程决定最小值
			HCURLUtil.sendCmd(j2seCoreSS, HCURL.DATA_CMD_PUBLISH_LOCATION_MS, "value", String.valueOf(minMS));
		}
	}
	
	@Override
	public final void enableLog(final boolean enable){
		msbAgent.enableDebugInfo(enable);
	}
	
	@Override
	public Object getObject(final int funcID, final Object para){
		return null;
	}
	
	public final synchronized ProjectTargetForAI query(final J2SESession coreSS, final String locale, final String voice){
		if(AIPersistentManager.isEnableHCAI()){
			return AIPersistentManager.query(coreSS, locale, voice, projIDs, responserSize);
		}else{
			return null;
		}
	}
	
	//stop synchronized:不占用其它锁
	public final boolean dispatchVoiceCommand(final J2SESession coreSS, final String voiceText){
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser projResp = responsors[i];
			
			final VoiceCommand vc = ServerAPIAgent.buildVoiceCommand(voiceText, coreSS, projResp);
			final boolean isConsumed = projResp.dispatchVoiceCommandAndWait(coreSS, vc);
			if(isConsumed){
				LogManager.log(ILog.OP_STR + "successful process voice command [" + vc.getText() + "] in project [" + projResp.projectID + "].");
				return true;
			}
		}
		
		return false;
	}
	
	public final void dispatchLocation(final J2SESession j2seCoreSS, final Location location){
		j2seCoreSS.setLocation(location);
		
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser projResp = responsors[i];
			if(projResp.hasLocationOfMobile){
				fireSystemEventListenerInSequence(j2seCoreSS, projResp, projResp.context, ProjectContext.EVENT_SYS_MOBILE_LOCATION);
			}
		}
	}
	
	/**
	 * 与appendNewHarProject间，需锁
	 * @return
	 */
	public final synchronized HashMap<ProjResponser, Device[]> getAllDevices(){
		final HashMap<ProjResponser, Device[]> out = new HashMap<ProjResponser, Device[]>();
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser projResp = responsors[i];
			try{
				out.put(projResp, projResp.getDevices(WorkingDeviceList.ALL_DEVICES));
			}catch (final Exception e) {
			}
		}
		
		return out;
	}
	
	public final boolean hasRobotReferenceDevice(){
		try{
			//每次重算，因为可能追加了工程
			totalRobotRefDevices = 0;
			for (int i = 0; i < responserSize; i++) {
				totalRobotRefDevices += bindRobotSource.getTotalReferenceDeviceNumByProject(projIDs[i]);
			}
			return totalRobotRefDevices > 0;
		}catch (final Exception e) {
			return false;
		}
	}
	
	private final void notifyMobileCacheProjList(final J2SESession socketSession){
		if(responserSize > 0){
			final String[] projList = new String[responserSize];
			for (int i = 0; i < responserSize; i++) {
				projList[i] = projIDs[i];
			}
			
			final String serialProjs = StringUtil.toSerialBySplit(projList);
			HCURLUtil.sendCmd(socketSession, HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_NOTIFY_PROJ_LIST, serialProjs);//将最新的工程名列表，通知到手机的cache
//			以下逻辑置于用户下线时操作，以节省用户时间。
//			CacheManager.checkAndDelCacheOverflow(ServerUIAPIAgent.getMobileUID());//可能超载，只限于服务器端
		}
	}
	
	/**
	 * 重新从lps仓库中寻找未添加的工程，不启动，不运行
	 */
	public final synchronized ProjResponser[] appendNewHarProject(final boolean isInstallFromClient){
		final Vector<LinkProjectStore> appendLPS = new Vector<LinkProjectStore>();

		final int oldRespSize = responserSize;
		final int newRespSize = LinkProjectManager.getActiveProjNum();
		if(oldRespSize >= newRespSize){
			final ProjResponser[] projResp = {};
			return projResp;
		}
		
		final Iterator<LinkProjectStore> lpsIt = LinkProjectManager.getLinkProjsIteratorInUserSysThread(true);
		while(lpsIt.hasNext()){
			final LinkProjectStore lps = lpsIt.next();
			if(!lps.isActive()){
				continue;
			}
			final String projID = lps.getProjectID();
			boolean isExists = false;
			for (int i = 0; i < oldRespSize; i++) {
				if(projIDs[i].equals(projID)){
					isExists = true;
					break;
				}
			}
			
			if(isExists == false){
				appendLPS.add(lps);
			}
		}
		
		final String[] newprojIDS = new String[newRespSize];
		final Map[] newmaps = new Map[newRespSize];
		final ProjResponser[] newresponsors = new ProjResponser[newRespSize];
		
		System.arraycopy(projIDs, 0, newprojIDS, 0, oldRespSize);
		System.arraycopy(maps, 0, newmaps, 0, oldRespSize);
		System.arraycopy(responsors, 0, newresponsors, 0, oldRespSize);
		
		projIDs = newprojIDS;
		maps = newmaps;
		responsors = newresponsors;
		
		final int appendSize = appendLPS.size();
		final ProjResponser[] appResp = new ProjResponser[appendSize];
		
		for (int i = 0; i < appendSize; i++) {
			final int nextIdx = oldRespSize + i;
			final LinkProjectStore lps = appendLPS.elementAt(i);
			
			maps[nextIdx] = HCjar.loadHarFromLPS(lps);
			final String projectID = lps.getProjectID();
			projIDs[nextIdx] = projectID;
			buildProjResp(projectID, lps, nextIdx, isInstallFromClient);
			
			appResp[i] = responsors[nextIdx];
		}
		
		setRespSize(newRespSize);
		
		//注意：必须在setRespSize之后，因为initJarMainMenu依赖于前者
		for (int i = 0; i < appendSize; i++) {
			final int nextIdx = oldRespSize + i;
			responsors[nextIdx].initJarMainMenu();
		}
		
		return appResp;
	}

	private final void setRespSize(final int newRespSize) {
		responserSize = newRespSize;
	}
	
	public MobiUIResponsor(final ExceptionCatcherToWindow ec) {
		threadToken = App.getThreadPoolToken();
		UILang.initToken(threadToken);
		LuceneManager.init();
		
		this.ec = ec;
		
		//获得active数量
		responserSize = LinkProjectManager.getActiveProjNum();
		
		msbAgent = new MSBAgent(MSBAgent.buildWorkbench(new IoTNameMapper()));

		final LinkProjectStore[] lpss = new LinkProjectStore[responserSize];
		
		projIDs = new String[responserSize];
		maps = new Map[responserSize];
		responsors = new ProjResponser[responserSize];
		
		final Iterator<LinkProjectStore> lpsIt = LinkProjectManager.getLinkProjsIteratorInUserSysThread(true);
		int count = 0;
		while(lpsIt.hasNext()){
			final LinkProjectStore lps = lpsIt.next();
			if(!lps.isActive()){
				continue;
			}
			final Map<String, Object> map = HCjar.loadHarFromLPS(lps);
			
			//先暂存到Map中，因为后续需要检查判断。
			maps[count] = map;
			lpss[count] = lps;
			projIDs[count] = lps.getProjectID();
			
			count++;
		}
		
		final boolean isInstallFromClient = false;
		
		//先暂存Map，后实例化ProjResponser，以供检查子工程的菜单结点为0时，不显示Folder
		final boolean[] findExitsCache = new boolean[responserSize];
		for (int i = 0; i < responserSize; i++) {
			final String projID = (String)maps[i].get(HCjar.PROJ_ID);
			final boolean isInCache = RecycleProjThreadPool.containsProjID(projID);//重用缓存中的对象
			findExitsCache[i] = isInCache;
			if(isInCache){
				buildProjResp(projID, lpss[i], i, isInstallFromClient);
			}
		}
		for (int i = 0; i < responserSize; i++) {
			if(findExitsCache[i] == false){
				final String projID = (String)maps[i].get(HCjar.PROJ_ID);
				buildProjResp(projID, lpss[i], i, isInstallFromClient);
			}
		}
		
		for (int i = 0; i < responserSize; i++) {
			responsors[i].initJarMainMenu();
		}
		
		//由于JRuby引擎初始化时，不能受限，所以增加下行代码，以完成初始化
//		final HCJRubyEngine hcje = responsors[0].hcje;
//		RubyExector.initActive(hcje);
	}
	
	public final void preLoadJRubyScripts(){
		final int seconds = ResourceUtil.getSecondsForPreloadJRuby();
		if(seconds < 0){
			return;
		}

		GlobalConditionWatcher.addWatcher(new IWatcher() {
			final long startMS = System.currentTimeMillis();
			
			@Override
			public boolean watch() {
				if(System.currentTimeMillis() - startMS > seconds * 1000){
					LogManager.log("pre load and compile JRuby scripts to improve performance.");
					LogManager.log("it maybe occupy some memory and CPU resources to pre-load.");
					try{
						for (int i = 0; i < responserSize; i++) {
							responsors[i].preLoadJRubyScripts();
						}
					}catch (final Throwable e) {
					}
					return true;
				}
				return false;
			}
			
			@Override
			public void setPara(final Object p) {
			}
			
			@Override
			public boolean isCancelable() {
				return false;
			}
			
			@Override
			public void cancel() {
			}
		});
	}
	
	private final void buildProjResp(final String projID, final LinkProjectStore lps, final int i, final boolean isInstallFromClient) {
		final ProjResponser pr = new ProjResponser(projID, maps[i], this, lps);
		responsors[i] = pr;
		if(isInstallFromClient){
			ServerUIAPIAgent.setSysAttribute(pr, ServerUIAPIAgent.KEY_IS_INSTALL_FROM_CLIENT, Boolean.TRUE);
		}
	}
	
	/**
	 * 如果放弃绑定，则返回null；成功则返回实例。
	 */
	@Override
	public final BaseResponsor checkAndReady(final JFrame owner) throws Exception{
		final BindRobotSource bindSource = new BindRobotSource(this);
		if(BindManager.hasProjNotBinded()){
			if(BindManager.findNewUnbind(bindSource) == false){
				return this;
			}else{
				final Boolean[] isDoneBind = {null};
				final ThreadGroup threadPoolToken = App.getThreadPoolToken();
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						DeviceBinderWizard out = null;
						try{
							out = DeviceBinderWizard.getInstance(bindSource, false, owner, bindSource.respo, threadPoolToken);
						}catch (final Throwable e) {
							ExceptionReporter.printStackTrace(e);
							isDoneBind[0] = false;
							LinkProjectManager.reloadLinkProjects();
							return;
						}
						
						final DeviceBinderWizard binder = out;
						final UIActionListener jbOKAction = new UIActionListener() {
							@Override
							public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
								window.dispose();

								isDoneBind[0] = true;
								binder.save();
								bindSource.respo.msbAgent.workbench.reloadMap();
							}
						};
						final UIActionListener cancelAction = new UIActionListener() {
							@Override
							public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
								window.dispose();

								//以下代码，请与上行的DeviceBinderWizard.getInstance保持一致
								isDoneBind[0] = false;
								//恢复原状态
								LinkProjectManager.reloadLinkProjects();
							}
						};
						binder.setButtonAction(jbOKAction, cancelAction);
						binder.show();
					}
				});
				while(isDoneBind[0] == null){
					try{
						Thread.sleep(500);
					}catch (final Exception e) {
						ExceptionReporter.printStackTrace(e);
					}
				}
				if(isDoneBind[0]){
					return this;
				}
			}
		}else{
			return this;
		}
		
		return null;
	}
	
	public final ProjResponser getProjResponser(final String projID){
		for (int i = 0; i < responserSize; i++) {
			if(projIDs[i].equals(projID)){
				return responsors[i];
			}
		}
		return null;
	}
	
	/**
	 * 没有找到，返回null
	 * @param projID
	 * @return
	 */
	public final Map<String, Object> getHarMap(final String projID){
		for (int i = 0; i < responserSize; i++) {
			if(projIDs[i].equals(projID)){
				return maps[i];
			}
		}
		return null;
	}

	@Override
	public void setMap(final HashMap map){
		
	}
	
	@Override
	public synchronized void start(){
		try{
			startupIOT();
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			throw new Error(e.toString());
		}
		
		onEvent(J2SESession.NULL_J2SESESSION_FOR_PROJECT, ProjectContext.EVENT_SYS_PROJ_STARTUP);
	}
	
	@Override
	public void enterContext(final J2SESession socketSession, final String projectID){
		currContext.setCurrContext(socketSession, projectID);
	}

	private static void changeMobileProjectID(final J2SESession socketSession, final String projID) {
		final int recordNum = CacheManager.getRecordNum(projID, UserThreadResourceUtil.getMobileSoftUID(socketSession));
		final String splitter = projID + StringUtil.SPLIT_LEVEL_2_JING + recordNum;
//		System.out.println("CLASS_CHANGE_PROJECT_ID : " + splitter);
		HCURLUtil.sendEClass(socketSession, HCURLUtil.CLASS_CHANGE_PROJECT_ID, splitter);
	}
	
	private void startupIOT() throws Exception{
		if(PropertiesManager.isTrue(PropertiesManager.p_isEnableMSBLog)){
			msbAgent.enableDebugInfo(true);
		}
		
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser pr = responsors[i];
			
			pr.loadProcessors(msbAgent);
		}
		
		msbAgent.startAllProcessor();
	}
	
	private final void shutdownIOT(){
		if(msbAgent != null){//有可能未启动成功，而需要release
			msbAgent.stopAllProcessor();
		}
	}
	
	@Override
	public synchronized void stop(){
		super.stop();
		
		onEvent(J2SESession.NULL_J2SESESSION_FOR_PROJECT, ProjectContext.EVENT_SYS_PROJ_SHUTDOWN);
		
		release();
	}

	public final void release() {
		//由于重启时，需要先关闭旧连接，才建立新连接，所以此处不加线程（异步）
		shutdownIOT();//要置于ProjResponser.stop之前，因为可能使用Scheduler

		for (int i = 0; i < responserSize; i++) {
			responsors[i].waitForFinishAllSequTask();//不能与下段合并
		}
		
		L.V = L.WShop ? false : LogManager.log("waiting for all task done in AI...");
		AIPersistentManager.waitForAllDone();
		L.V = L.WShop ? false : LogManager.log("all task done in AI.");
		
		//terminate
		for (int i = 0; i < responserSize; i++) {
			responsors[i].stop();
		}
		
		bindRobotSource = null;
		
	}
	
	public final synchronized void processProjectNameAndItemNameImpl(final J2SESession coreSS) throws SQLException {
		final String locale = UserThreadResourceUtil.getMobileLocaleFrom(coreSS);
		
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser resp = responsors[i];
			final String projectID = resp.projectID;
			final AIPersistentManager aimgr = AIPersistentManager.getManagerByProjectIDInDelayFromResp(projectID);
			final String title = resp.jarMainMenu.getTitle(coreSS);
			if(aimgr.projTitleSM.hasTitle(locale, title) == false){
				final List<String> keys = LuceneManager.tokenizeString(locale, title);
				final int labelID = aimgr.labelSM.appendData(LabelManager.LABEL_SRC_PROJ, locale, title, keys);
				aimgr.projTitleSM.appendTitleData(labelID, locale, title);
			}
			
			final Vector<MenuItem> menuItems = coreSS.getDisplayMenuItems(projectID);
			final String[] urls = resp.jarMainMenu.getURLs(coreSS, menuItems);
			final String[] itemLabels = resp.jarMainMenu.getIconLabels(coreSS, menuItems);
			
			final int itemSize = urls.length;
			for (int j = 0; j < itemSize; j++) {
				final String itemLabel = itemLabels[j];
				final String itemURL = urls[j];
				if(aimgr.itemTitleSM.hasTitle(itemURL, locale, itemLabel) == false){
					final List<String> keys = LuceneManager.tokenizeString(locale, itemLabel);
					final int labelID = aimgr.labelSM.appendData(LabelManager.LABEL_SRC_ITEM, locale, itemLabel, keys);
					aimgr.itemTitleSM.appendTitleData(labelID, locale, itemLabel, itemURL);
				}
			}
		}
	}
	
	/**
	 * @param j2seCoreSS 有可能为null
	 */
	@Override
	public synchronized Object onEvent(final J2SESession j2seCoreSS, final String event) {
		if(ProjResponser.isScriptEventToAllProjects(event)){//否则仅对session的事件
			//logout或shutdown属于后入先出型，应最后执行ROOT
			boolean isReturnBack = false;

			//处理可能没有mobile_login，而导致调用mobile_logout事件
			final String rootProjID = findRootContextID();
			if(event == ProjectContext.EVENT_SYS_MOBILE_LOGIN){//注意：请与fireSystemEventListenerOnAppendProject保持同步
				j2seCoreSS.isEventMobileLoginDone = true;
				
				notifyMobileCacheProjList(j2seCoreSS);
				
				//必须先于rootProjID，专用于HTMLMlet初始脚本cache，JSViewManager.setHTMLBody
				changeMobileProjectID(j2seCoreSS, CacheManager.ELE_PROJ_ID_HTML_PROJ);

				currContext.appendCurrContext(j2seCoreSS, rootProjID);
				changeMobileProjectID(j2seCoreSS, rootProjID);
				
				notifyMobileLogin(j2seCoreSS);
				changePublishLocationMS(j2seCoreSS);//注意：新会话上线时，将启动会话的location
			}else if(event == ProjectContext.EVENT_SYS_MOBILE_LOGOUT){
				if(j2seCoreSS != null){
					j2seCoreSS.notifyMobileLogout();
				}
				
				isReturnBack = true;
				
				currContext.setCurrContext(j2seCoreSS, rootProjID);
//				currContext = findRootContextID();
				if(j2seCoreSS.isEventMobileLoginDone == false){
					return null;
				}
				j2seCoreSS.isEventMobileLoginDone = false;
			}else if(event == ProjectContext.EVENT_SYS_PROJ_STARTUP){//注意：请与fireSystemEventListenerOnAppendProject保持同步
				isEventProjStartDone = true;
				
			}else if(event == ProjectContext.EVENT_SYS_PROJ_SHUTDOWN){
				isReturnBack = true;
				
				if(isEventProjStartDone == false){
					return null;
				}
				isEventProjStartDone = false;
			}
			
			final boolean isReturnBackFinal = isReturnBack;
			if(isReturnBackFinal == false){
				//login或start时，先执行ROOT
				responsors[rootIdx].onScriptEventInSequence(j2seCoreSS, event);
			}
			
			//执行非ROOT
			for (int i = 0; i < responserSize; i++) {
				if(i != rootIdx){
					responsors[i].onScriptEventInSequence(j2seCoreSS, event);//注意：请与fireSystemEventListenerOnAppendProject保持同步
				}
			}

			if(isReturnBackFinal){
				//logout或shutdown时，最后执行ROOT
				responsors[rootIdx].onScriptEventInSequence(j2seCoreSS, event);
			}
			
			fireSystemEventInSequence(j2seCoreSS, event);
			
			if(event == ProjectContext.EVENT_SYS_MOBILE_LOGIN){
				UserThreadResourceUtil.getMobileAgent(j2seCoreSS).set(ConfigManager.UI_IS_BACKGROUND, IConstant.FALSE);
				fireSystemEventInSequence(j2seCoreSS, ProjectContext.EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND);
				
				if(AIPersistentManager.isEnableHCAI()){
					AIPersistentManager.processProjectNameAndItemName(j2seCoreSS, this);
				}
			}
			//以上是触发脚本，而非SystemEventListener
		}else{
			fireSystemEventInSequence(j2seCoreSS, event);
		}
		
		if(event == ProjectContext.EVENT_SYS_MOBILE_LOGOUT){
			ContextManager.getThreadPool().run(new Runnable() {//上面动作异步
				@Override
				public void run() {
					final int sleepMS = ResourceUtil.getIntervalSecondsForNextStartup() * 1000 + 2000;
					try{
						Thread.sleep(sleepMS);//时间不定，releaseClientSession所以为虚操作
					}catch (final Throwable e) {
					}
					releaseClientSession(j2seCoreSS);//必须最后释放
				}
			});
			for (int i = 0; i < responserSize; i++) {
				final ProjResponser projResponser = responsors[i];
				ServerUIAPIAgent.addSequenceWatcherInProjContextForSessionFirst(j2seCoreSS, projResponser, new ReturnableRunnable() {
					@Override
					public Object run() {
						j2seCoreSS.shutdowScheduler(projResponser.context);
						return null;
					}
				});
			}
		}else if(event == ProjectContext.EVENT_SYS_PROJ_SHUTDOWN){
			sendFinishAllNotify();
		}
		
		return null;
	}

	/**
	 * 
	 * @param coreSS 有些事件为null
	 * @param event
	 */
	private void fireSystemEventInSequence(final J2SESession coreSS, final String event) {
		//以下是触发SystemEventListener。
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser projResponser = responsors[i];
			//必须使用run。如果同步，可能导致异常程序占住服务器线程。参见SystemEventListener.onEvent
			fireSystemEventListenerInSequence(coreSS, projResponser, projResponser.context, event);//注意：请与fireSystemEventListenerOnAppendProject保持同步
		}
	}
	
	private final void sendFinishAllNotify(){
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser projResponser = responsors[i];
			final ProjectContext context = projResponser.context;
			ServerUIAPIAgent.addSequenceWatcherInProjContext(context, new BaseWatcher() {
				@Override
				public boolean watch() {
					ServerUIAPIAgent.runAndWaitInProjContext(context, new ReturnableRunnable() {
						@Override
						public Object run() {
							projResponser.notifyFinishAllSequTask();
							return null;
						}
					});
					return true;
				}
			});
		}
	}
	
	/**
	 * 使后追加的工程达到运行时的相同状态和初始化过程
	 * @param resp
	 */
	public final void fireSystemEventListenerOnAppendProject(final ProjResponser[] resp, final ArrayList<LinkProjectStore> appendLPS){
		final int size = resp.length;
		
		for (int i = 0; i < appendLPS.size(); i++) {
			msbAgent.workbench.appendProjectToBindSet(appendLPS.get(i));
		}
		
		for (int i = 0; i < size; i++) {
			try{
				resp[i].loadProcessors(msbAgent);
			}catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		
		msbAgent.startAllProcessor();//每个Processor内部会检查是否已started，而相应忽略
		
		if(isEventProjStartDone){
			final String event = ProjectContext.EVENT_SYS_PROJ_STARTUP;
			for (int i = 0; i < size; i++) {
				final ProjResponser projResponser = resp[i];
				projResponser.onScriptEventInSequence(J2SESession.NULL_J2SESESSION_FOR_PROJECT, event);//注意：必须为NULL_J2SESESSION_FOR_PROJECT
				fireSystemEventListenerInSequence(J2SESession.NULL_J2SESESSION_FOR_PROJECT, projResponser, projResponser.context, event);
			}
		}
		
		//注意：因为MenuItem是Project级，对所有用户可见，所以需要
		//注意：要将全部在线Session做一把
		final J2SESession[] coreSSS = J2SESessionManager.getAllOnlineSocketSessions();
		if(coreSSS != null){
			for (int i = 0; i < coreSSS.length; i++) {
				final J2SESession oneSS = coreSSS[i];
				applyToSession(oneSS, resp, size);
			}
		}
		
		for (int i = 0; i < size; i++) {
			resp[i].preLoadJRubyScripts();
		}
	}

	private final void applyToSession(final J2SESession coreSS, final ProjResponser[] resp, final int size) {
		if(coreSS.isEventMobileLoginDone){
			final String event = ProjectContext.EVENT_SYS_MOBILE_LOGIN;
			
			for (int i = 0; i < size; i++) {
				final ProjResponser projResponser = resp[i];
				
				setClientSessionForProjResponser(coreSS, projResponser);
				projResponser.onScriptEventInSequence(coreSS, event);
				fireSystemEventListenerInSequence(coreSS, projResponser, projResponser.context, event);
			}
		}
	}

	/**
	 * 
	 * @param coreSS 有些事件为null
	 * @param ctx
	 * @param event
	 */
	private final void fireSystemEventListenerInSequence(final J2SESession coreSS, final ProjResponser resp, final ProjectContext ctx, final String event) {
		final ReturnableRunnable runnable = new ReturnableRunnable() {
			@Override
			public Object run() {
				final Enumeration<SystemEventListener> sels = ServerUIAPIAgent.getSystemEventListener(coreSS, ctx);
				try{
					while(sels.hasMoreElements()){
						final SystemEventListener sel = sels.nextElement();
						try{
							sel.onEvent(event);
						}catch (final Throwable e) {
							ExceptionReporter.printStackTrace(e);
						}
					}
				}catch (final NoSuchElementException e) {
				}
				
				
				if(ProjectContext.EVENT_SYS_PROJ_STARTUP == event){
					resp.jarMainMenu.projectMenu.notifyEnableFlushMenu();//完成初始状态，后续转为增量方式
					if(L.isInWorkshop){
						LogManager.log("change project level menu [" + resp.projectID + "] to increment mode.");
					}
				}else if(ProjectContext.EVENT_SYS_MOBILE_LOGIN == event){
					coreSS.getMenu(resp.projectID).notifyEnableFlushMenu();//完成初始状态，后续转为增量方式
					if(L.isInWorkshop){
						LogManager.log("change session level menu [" + resp.projectID + "] to increment mode.");
					}
				}
				
				return null;
			}
		};
		
		//NOT block main thread
		if(coreSS != null){
			ServerUIAPIAgent.addSequenceWatcherInProjContextForSessionFirst(coreSS, resp, runnable);
		}else{
			ServerUIAPIAgent.addSequenceWatcherInProjContext(ctx, new BaseWatcher() {
				@Override
				public boolean watch() {
					ServerUIAPIAgent.runAndWaitInProjContext(ctx, runnable);
					return true;
				}
			});
		}
	}

	private final SessionCurrContext currContext = new SessionCurrContext();
	
	private int rootIdx;
	
	public final String findRootContextID(){
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser projResponser = responsors[i];
			if(projResponser.isRoot){
				rootIdx = i;
				return projIDs[i];
			}
		}
		return null;
	}
	
	public final ProjResponser getCurrentProjResponser(final J2SESession session){
		return findContext(getCurrProjectID(session));
	}

	public final String getCurrProjectID(final J2SESession session) {
		return currContext.getCurrContext(session);
	}
	
	public final ProjResponser findContext(final String context){
		for (int i = 0; i < responserSize; i++) {
			if(projIDs[i].equals(context)){
				return responsors[i];
			}
		}
		return null;
	}

	@Override
	public boolean doBiz(final CoreSession coreSS, final HCURL url) {
		final J2SESession j2seCoreSS = (J2SESession)coreSS;
		
		//拦截Menu处理
		if(url.protocal == HCURL.MENU_PROTOCAL){
			j2seCoreSS.notifyCanvasMenuResponse();
			final String newContext = url.elementID;
			
			if(newContext.equals(HCURL.ROOT_MENU) == false //保留支持旧的ROOT_ID
					&& newContext.equals(getCurrProjectID(j2seCoreSS)) == false){
				enterContext(j2seCoreSS, newContext);//内部含CLASS_CHANGE_PROJECT_ID
				changeMobileProjectID(j2seCoreSS, newContext);
				
				final ProjResponser resp = findContext(newContext);
				final JarMainMenu linkMenu = resp.jarMainMenu;
				
				linkMenu.transMenuWithCache(j2seCoreSS);
				
				ScreenServer.pushScreen(j2seCoreSS, linkMenu);
				
				LogManager.log(ILog.OP_STR + "enter project : [" + linkMenu.projectID + "]");
				LogManager.log(ILog.OP_STR + "open menu : [" + linkMenu.linkOrProjectName + "]");
				
				return true;
			}
		}
		
		return findContext(getCurrProjectID(j2seCoreSS)).doBiz(j2seCoreSS, url, true);
	}

	@Override
	public void createClientSession(final J2SESession ss) {
		if(ss != null){
			for (int i = 0; i < responserSize; i++) {
				final ProjResponser pr = responsors[i];
				setClientSessionForProjResponser(ss, pr);
			}
		}
	}

	final void setClientSessionForProjResponser(final J2SESession coreSS, final ProjResponser pr) {
		pr.initSessionContext(coreSS);
		
		final SessionMobiMenu menu = new SessionMobiMenu(coreSS, pr, pr.isRoot, pr.jarMainMenu.projectMenu);
		coreSS.setSessionMenu(pr.projectID, menu);
		
		if(L.isInWorkshop){
			LogManager.log("set clientSession for project [" + pr.context.getProjectID() + "].");
		}
	}

	@Override
	public void releaseClientSession(final J2SESession coreSS) {
		if(coreSS != null){
			currContext.removeSession(coreSS);
			
			for (int i = 0; i < responserSize; i++) {
				final ProjResponser resp = responsors[i];
				if(resp != null){
					try{
						final String projectID = resp.context.getProjectID();
						resp.removeMobileContext(coreSS);
						if(L.isInWorkshop){
							LogManager.log("release clientSession for project [" + projectID + "].");
						}
					}catch (final Throwable e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
}
