package hc.server.ui.design;

import hc.App;
import hc.UIActionListener;
import hc.core.ContextManager;
import hc.core.L;
import hc.core.cache.CacheManager;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.ScreenServer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.msb.Device;
import hc.server.msb.MSBAgent;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.hpj.HCjar;
import hc.server.ui.design.hpj.RubyExector;
import hc.server.util.SystemEventListener;
import hc.util.BaseResponsor;
import hc.util.PropertiesManager;
import hc.util.RecycleProjThreadPool;
import hc.util.ResourceUtil;

import java.awt.Frame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;

public class MobiUIResponsor extends BaseResponsor {
	int totalRobotRefDevices;
	String[] projIDs;
	Map<String, Object>[] maps;
	ProjResponser[] responsors;
	int responserSize; 
	MSBAgent msbAgent;
	BindRobotSource bindRobotSource;
	final Vector<ProjectContext> listsProjectContext = new Vector<ProjectContext>();
	
	@Override
	public void enableLog(final boolean enable){
		msbAgent.enableDebugInfo(enable);
	}
	
	@Override
	public Object getObject(final int funcID, final Object para){
		return null;
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
				out.put(projResp, projResp.getDevices());
			}catch (final Exception e) {
			}
		}
		
		return out;
	}
	
	public boolean hasRobotReferenceDevice(){
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
	
	private final void notifyMobileCacheProjList(){
		if(responserSize > 0){
			final String[] projList = new String[responserSize];
			for (int i = 0; i < responserSize; i++) {
				projList[i] = projIDs[i];
			}
			
			final String serialProjs = StringUtil.toSerialBySplit(projList);
			HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_NOTIFY_PROJ_LIST, serialProjs);//将最新的工程名列表，通知到手机的cache
			CacheManager.checkAndDelCacheOverload();//可能超载，只限于服务器端
		}
	}
	
	/**
	 * 重新从lps仓库中寻找未添加的工程，不启动，不运行
	 */
	public final synchronized ProjResponser[] appendNewHarProject(){
		final Vector<LinkProjectStore> appendLPS = new Vector<LinkProjectStore>();

		final int oldRespSize = responserSize;
		final int newRespSize = LinkProjectManager.getActiveProjNum();
		if(oldRespSize >= newRespSize){
			final ProjResponser[] projResp = {};
			return projResp;
		}
		
		final Iterator<LinkProjectStore> lpsIt = LinkProjectManager.getLinkProjsIterator(true);
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
			buildProjResp(projectID, lps, nextIdx);
			
			appResp[i] = responsors[nextIdx];
		}
		
		responserSize = newRespSize;
		
		return appResp;
	}
	
	public MobiUIResponsor() {
		//获得active数量
		responserSize = LinkProjectManager.getActiveProjNum();
		
		final LinkProjectStore[] lpss = new LinkProjectStore[responserSize];
		
		projIDs = new String[responserSize];
		maps = new Map[responserSize];
		responsors = new ProjResponser[responserSize];
		
		final Iterator<LinkProjectStore> lpsIt = LinkProjectManager.getLinkProjsIterator(true);
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
		
		//先暂存Map，后实例化ProjResponser，以供检查子工程的菜单结点为0时，不显示Folder
		final boolean[] findExitsCache = new boolean[responserSize];
		for (int i = 0; i < responserSize; i++) {
			final String projID = (String)maps[i].get(HCjar.PROJ_ID);
			final boolean isInCache = RecycleProjThreadPool.containsProjID(projID);//重用缓存中的对象
			findExitsCache[i] = isInCache;
			if(isInCache){
				buildProjResp(projID, lpss[i], i);
			}
		}
		for (int i = 0; i < responserSize; i++) {
			if(findExitsCache[i] == false){
				final String projID = (String)maps[i].get(HCjar.PROJ_ID);
				buildProjResp(projID, lpss[i], i);
			}
		}
		
		//由于JRuby引擎初始化时，不能受限，所以增加下行代码，以完成初始化
		final HCJRubyEngine hcje = responsors[0].hcje;
		RubyExector.initActive(hcje);
	}

	public void preLoadJRubyScripts(){
		final int seconds = ResourceUtil.getSecondsForPreloadJRuby();
		if(seconds < 0){
			return;
		}

		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				try{
					Thread.sleep(seconds * 1000);//可能用户需要执行高优先级任务
				}catch (final Exception e) {
				}
				
				L.V = L.O ? false : LogManager.log("pre load and compile JRuby scripts to improve performance.");
				L.V = L.O ? false : LogManager.log("it maybe occupy some memory and CPU resources to pre-load.");
				for (int i = 0; i < responserSize; i++) {
					responsors[i].preLoadJRubyScripts();
				}
			}
		});
	}
	
	private final void buildProjResp(final String projID, final LinkProjectStore lps, final int i) {
		responsors[i] = new ProjResponser(projID, maps[i], this, lps);
	}
	
	/**
	 * 如果放弃绑定，则返回null；成功则返回实例。
	 */
	@Override
	public BaseResponsor checkAndReady(final Frame owner) throws Exception{
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
							e.printStackTrace();
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
						e.printStackTrace();
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
	
	public ProjResponser getProjResponser(final String projID){
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
	public Map<String, Object> getHarMap(final String projID){
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
	public void start(){
		try{
			startupIOT();
		}catch (final Exception e) {
			e.printStackTrace();
			throw new Error(e.toString());
		}
		
		onEvent(ProjectContext.EVENT_SYS_PROJ_STARTUP);
	}
	

	@Override
	public void enterContext(final String contextName){
		this.currContext = contextName;
	}
	
	private void startupIOT() throws Exception{
		msbAgent = new MSBAgent(MSBAgent.buildWorkbench(new IoTNameMapper()));
		if(PropertiesManager.isTrue(PropertiesManager.p_isEnableMSBLog)){
			msbAgent.enableDebugInfo(true);
		}
		
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser pr = responsors[i];
			
			pr.loadProcessors(msbAgent);
		}
		
		msbAgent.startAllProcessor();
	}
	
	private void shutdownIOT(){
		if(msbAgent != null){//有可能未启动成功，而需要release
			msbAgent.stopAllProcessor();
		}
	}
	
	@Override
	public void stop(){
		super.stop();
		
		onEvent(ProjectContext.EVENT_SYS_PROJ_SHUTDOWN);
		
		release();
	}

	public final void release() {
		//由于重启时，需要先关闭旧连接，才建立新连接，所以此处不加线程（异步）
		shutdownIOT();

		//terminate
		for (int i = 0; i < responserSize; i++) {
			responsors[i].stop();
		}
		
		listsProjectContext.clear();
		bindRobotSource = null;
		
//		ContextSecurityManager.clear();
//		HCLimitSecurityManager.switchHCSecurityManager(false);
	}
	
	@Override
	public synchronized Object onEvent(final Object event) {
		if(ProjResponser.isScriptEvent(event)){
			//logout或shutdown属于后入先出型，应最后执行ROOT
			boolean isReturnBack = false;

			//处理可能没有mobile_login，而导致调用mobile_logout事件
			if(event == ProjectContext.EVENT_SYS_MOBILE_LOGIN){//注意：请与fireSystemEventListenerOnAppendProject保持同步
				isMobileLogined = true;
				
				notifyMobileLogin();
				notifyMobileCacheProjList();
			}else if(event == ProjectContext.EVENT_SYS_MOBILE_LOGOUT){
				notifyMobileLogout(false);
				
				isReturnBack = true;
				
				currContext = findRootContextID();
				if(isMobileLogined == false){
					return null;
				}
				isMobileLogined = false;
			}else if(event == ProjectContext.EVENT_SYS_PROJ_STARTUP){//注意：请与fireSystemEventListenerOnAppendProject保持同步
				isProjStarted = true;
				
				currContext = findRootContextID();
			}else if(event == ProjectContext.EVENT_SYS_PROJ_SHUTDOWN){
				isReturnBack = true;
				
				if(isProjStarted == false){
					return null;
				}
				isProjStarted = false;
			}
			
			if(isReturnBack == false){
				//login或start时，先执行ROOT
				responsors[rootIdx].onScriptEvent(event);
			}
			
			//执行非ROOT
			for (int i = 0; i < responserSize; i++) {
				if(i != rootIdx){
					responsors[i].onScriptEvent(event);//注意：请与fireSystemEventListenerOnAppendProject保持同步
				}
			}

			if(isReturnBack){
				//logout或shutdown时，最后执行ROOT
				responsors[rootIdx].onScriptEvent(event);
			}
			//以上是触发脚本，而非SystemEventListener
		}
		
		//以下是触发SystemEventListener。
		final Enumeration<ProjectContext> enu = listsProjectContext.elements();
		while(enu.hasMoreElements()){
			final ProjectContext pc = enu.nextElement();
			//必须使用run。如果同步，可能导致异常程序占住服务器线程。参见SystemEventListener.onEvent
			fireSystemEventListener(pc, event);//注意：请与fireSystemEventListenerOnAppendProject保持同步
		}
		
		return null;
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
				e.printStackTrace();
			}
		}
		
		msbAgent.startAllProcessor();//每个Processor内部会检查是否已started，而相应忽略
		
		if(isProjStarted){
			final String event = ProjectContext.EVENT_SYS_PROJ_STARTUP;
			for (int i = 0; i < size; i++) {
				final ProjResponser projResponser = resp[i];
				projResponser.onScriptEvent(event);
				fireSystemEventListener(projResponser.context, event);
			}
		}
		
		if(isMobileLogined){
			final String event = ProjectContext.EVENT_SYS_MOBILE_LOGIN;
			for (int i = 0; i < size; i++) {
				final ProjResponser projResponser = resp[i];
				projResponser.onScriptEvent(event);
				fireSystemEventListener(projResponser.context, event);
			}
		}
		
		for (int i = 0; i < size; i++) {
			resp[i].preLoadJRubyScripts();
		}
	}

	private final void fireSystemEventListener(final ProjectContext pc,
			final Object event) {
		pc.run(new Runnable() {
			@Override
			public void run() {
				final Enumeration sels = ServerUIAPIAgent.getSystemEventListener(pc);
				while(sels.hasMoreElements()){
					final SystemEventListener sel = (SystemEventListener)sels.nextElement();
					try{
						sel.onEvent(event.toString());
					}catch (final Throwable e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void addProjectContext(final ProjectContext pc){
		listsProjectContext.add(pc);
	}
	
	private String currContext;
	private int rootIdx;
	public boolean isMobileLogined = false, isProjStarted = false;
	
	public String findRootContextID(){
		for (int i = 0; i < responserSize; i++) {
			final ProjResponser projResponser = responsors[i];
			if(projResponser.menu[projResponser.mainMenuIdx].isRoot){
				rootIdx = i;
				return projIDs[i];
			}
		}
		return null;
	}
	
	public final String getCurrentContext(){
		return currContext;
	}
	
	public final ProjResponser getCurrentProjResponser(){
		return findContext(getCurrentContext());
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
	public boolean doBiz(final HCURL url) {

		//拦截Menu处理
		if(url.protocal == HCURL.MENU_PROTOCAL){
			final String newContext = url.elementID;
			
			if(newContext.equals(HCURL.ROOT_MENU) == false //保留支持旧的ROOT_ID
					&& newContext.equals(currContext) == false){
				final ProjResponser resp = findContext(newContext);
				final JarMainMenu linkMenu = resp.menu[resp.mainMenuIdx];
				
				ServerUIUtil.response(linkMenu.buildJcip());
				
				ScreenServer.pushScreen(linkMenu);
				
				enterContext(newContext);

				L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "enter project : [" + linkMenu.projectID + "]");
				L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "open menu : [" + linkMenu.linkOrProjectName + "]");
				return true;
			}
		}
		
		return findContext(currContext).doBiz(url);
	}
	
}
