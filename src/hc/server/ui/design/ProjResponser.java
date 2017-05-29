package hc.server.ui.design;

import hc.App;
import hc.core.BaseWatcher;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.IConstant;
import hc.core.L;
import hc.core.cache.CacheManager;
import hc.core.util.ByteUtil;
import hc.core.util.CtrlMap;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLCacher;
import hc.core.util.HCURLUtil;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.core.util.MobileAgent;
import hc.core.util.RecycleRes;
import hc.core.util.RecycleThread;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StoreableHashMap;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPool;
import hc.core.util.ThreadPriorityManager;
import hc.server.CallContext;
import hc.server.HCJRubyException;
import hc.server.MultiUsingManager;
import hc.server.ScreenServer;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.MSBAgent;
import hc.server.msb.Robot;
import hc.server.msb.RobotWrapper;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.msb.WorkingDeviceList;
import hc.server.ui.ClientSession;
import hc.server.ui.ClientSessionForSys;
import hc.server.ui.CtrlResponse;
import hc.server.ui.HTMLMlet;
import hc.server.ui.MenuItem;
import hc.server.ui.Mlet;
import hc.server.ui.ProjClassLoaderFinder;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServCtrlCanvas;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SizeHeightForXML;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.engine.RubyExector;
import hc.server.ui.design.hpj.HCjar;
import hc.server.ui.design.hpj.HCjarHelper;
import hc.server.ui.design.hpj.HPNode;
import hc.server.util.Assistant;
import hc.server.util.CacheComparator;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.VoiceCommand;
import hc.server.util.ai.AIPersistentManager;
import hc.util.BaseResponsor;
import hc.util.ClassUtil;
import hc.util.RecycleProjThreadPool;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

public class ProjResponser {
	public final Map<String, Object> map;
	public final RecycleRes recycleRes;
	public JarMainMenu jarMainMenu;//注意：menu不允许为null，主菜单可能出现空内容
	public final HCJRubyEngine hcje;
	final ThreadGroup threadGroup;
	final String lpsLinkName;
	public final ProjectContext context;
	private final Hashtable<String, Object> sys_att_map_sys = new Hashtable<String, Object>();
	Robot[] robots;
	Device[] devices;
	Converter[] converters;
	final MobiUIResponsor mobiResp;
	public final String projectID;
	final boolean isRoot;
	final SessionMobileContext mobileContexts = new SessionMobileContext();
	private final ThreadGroup token = App.getThreadPoolToken();
	
	private boolean isFinishAllSequ = false;
	
	final void notifyFinishAllSequTask(){
		synchronized (this) {
			isFinishAllSequ = true;
			this.notify();
		}
		L.V = L.WShop ? false : LogManager.log("notify all sequence task is done [" + projectID + "].");
	}
	
	final void waitForFinishAllSequTask(){
		synchronized (this) {
			if(isFinishAllSequ == false){
				L.V = L.WShop ? false : LogManager.log("wait for all sequence task done when shutdown project [" + projectID + "].");
				try {
					this.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		L.V = L.WShop ? false : LogManager.log("all sequence task is done [" + projectID + "].");
	}
	
	public final void initSessionContext(final J2SESession coreSS){
		if(getMobileSession(coreSS) == null){//有可能安装HAR时，因为showInputDialog，已设置完成
			final SessionContext mc = useFreeMobileContext(coreSS);
			mc.setClientSession(coreSS, new ClientSession(), new ClientSessionForSys(coreSS, token));
		}
	}
	
	public final SessionContext useFreeMobileContext(final J2SESession coreSS){
		final SessionContext mc = SessionContext.getFreeMobileContext(projectID, threadGroup, this);
		mobileContexts.appendCurrContext(coreSS, mc);
		return mc;
	}
	
	public final boolean dispatchVoiceCommandAndWait(final J2SESession coreSS, final VoiceCommand vc){
		final Assistant assistant = ServerUIAPIAgent.getVoiceAssistant(context);
		if(assistant != null){
			try{
				return (Boolean)ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, this, new ReturnableRunnable() {
					@Override
					public Object run() {
						return assistant.onVoice(vc);
					}
				});
			}catch (final Throwable e) {//拦截未正常返回的异常
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	public final SessionContext getMobileSession(final J2SESession socket){
		return mobileContexts.getMobileContext(socket);
	}
	
	public final void removeMobileContext(final J2SESession socket){
		mobileContexts.removeSession(projectID, socket);
	}
	
	public final Object __setSysAtt(final String name, final Object obj) {
		return sys_att_map_sys.put(name, obj);
	}
	
	public final Object __removeSysAtt(final String name) {
		return sys_att_map_sys.remove(name);
	}
	
	public final Object __getSysAtt(final String name) {
		return sys_att_map_sys.get(name);
	}
	
	/**
	 * 
	 * @return 有可能返回null
	 */
	public final SessionContext getSessionContextFromCurrThread(){
		ThreadGroup group, mobileGroup;
		mobileGroup = Thread.currentThread().getThreadGroup();
		if(mobileGroup == threadGroup){
			return null;
		}
		
		boolean isMatch = false;
		try{
			do{
				group = mobileGroup.getParent();
				
				if(group == threadGroup){
					isMatch = true;
					break;
				}
				
				if(group == null){
					break;
				}
				
				mobileGroup = group;
			}while(true);
		}catch (final Exception e) {
		}
		
		if(isMatch){
			return mobileContexts.getSessionContextByThreadGroup(mobileGroup);
		}
		
		return null;
	}
	
	boolean isStoped = false;
	
	public final void stop(){
		L.V = L.WShop ? false : LogManager.log("stop ProjResponser [" + projectID + "]");

		ServerUIAPIAgent.shutdownSchedulers(context);//置于hcje.terminate之前

		try{
			synchronized (this) {
				isStoped = true;
				try{
					hcje.terminate();
				}catch (final Throwable e) {
					LogManager.errToLog("fail terminate JRuby engine : " + e.toString());
					ExceptionReporter.printStackTrace(e);
				}
			}
			
			RecycleProjThreadPool.recycle(projectID, recycleRes);
			
			mobileContexts.release(projectID);
			
			ServerUIAPIAgent.set__projResponserMaybeNull(context, null);
			
			if(robots != null){
				final int size = robots.length;
				for (int i = 0; i < size; i++) {
					MSBAgent.setRobotWrapperNull(robots[i]);
				}
			}
		}catch (final Throwable e) {
			if(L.isInWorkshop){
				e.printStackTrace();
			}
		}
	}
	
	public final Converter[] getConverters() throws Exception{
		if(converters != null){
			return converters;
		}
		
		final int size = HCjarHelper.getConverterNum(map);
		
		if(size <=0) {
			final Converter[] out = {};
			converters = out;
			return converters;
		}
		
		final Vector<String>[] vectors = HCjarHelper.getConvertersSrc(map);
		final Vector<String> names = vectors[0];
		final Vector<String> src = vectors[1];
		
		converters = new Converter[size];
		
		for (int itemIdx = 0; itemIdx < size; itemIdx++) {
			final String converterName = names.elementAt(itemIdx);
			final String scriptName = converterName;
			
			LogManager.log("try build instance for Converter [" + converterName + "] in project [" + projectID + "]...");
			
			//将转换器名称装入properties
			ServerUIAPIAgent.setSuperProp(context, ServerUIAPIAgent.CONVERT_NAME_PROP, converterName);
			final CallContext callCtx = CallContext.getFree();
			final Object converter = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(J2SESession.NULL_J2SESESSION_FOR_PROJECT, callCtx, 
					src.elementAt(itemIdx), scriptName, null, hcje, context, Converter.class);
			if(converter != null){
				LogManager.log("succesful build instance for Converter [" + converterName + "] in project [" + projectID + "].");
			}else{
				final String msg = "Fail instance Converter [" + converterName + "] in project [" + projectID + "].";
				LogManager.err(msg);
				throw new HCJRubyException(callCtx, msg);
			}
			CallContext.cycle(callCtx);
			
			converters[itemIdx] = (Converter)converter;
		}
		
		return converters;
	}
	
	
	final void loadProcessors(final MSBAgent agent) throws Exception{
		final Converter[] converters = getConverters();
		if(converters != null){
			for (int j = 0; j < converters.length; j++) {
				agent.addConverter(converters[j]);
			}
		}
		
		final Device[] devices = getDevices(agent.workbench.getWorkingDeviceList(projectID));
		if(devices != null){
			for (int j = 0; j < devices.length; j++) {
				agent.addDevice(devices[j]);
			}
		}
		
		final Robot[] robots = getRobots();
		if(robots != null){
			for (int j = 0; j < robots.length; j++) {
				agent.addRobot(robots[j]);
			}
		}
	}
	
	public final Device[] getDevices(final WorkingDeviceList list) throws Exception {
		if(devices != null){
			return devices;
		}
		
		{
			final int itemCount = HCjarHelper.getDeviceNum(map);
			
			if(itemCount <=0) {
				final Device[] out = {};
				devices = out;
				return devices;
			}
		}
		
		final Vector<String>[] vectors = HCjarHelper.getDevicesSrc(map, list);
		final Vector<String> names = vectors[0];
		final Vector<String> src = vectors[1];
		
		final int itemCount = names.size();
		devices = new Device[itemCount];
		
		for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
			
			final String devName = names.elementAt(itemIdx);
			final String devListener = src.elementAt(itemIdx);
			final String scriptName = devName;
			
			LogManager.log("try build instance for Device [" + devName + "] in project [" + projectID + "]...");
			
			//将设备名称装入properties
			ServerUIAPIAgent.setSuperProp(context, ServerUIAPIAgent.DEVICE_NAME_PROP, devName);
			final CallContext callCtx = CallContext.getFree();
			final Object device = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(J2SESession.NULL_J2SESESSION_FOR_PROJECT, callCtx,
					devListener, scriptName, null, hcje, context, Device.class);
			if(device != null){
				LogManager.log("successful build instance for Device [" + devName + "] in project [" + projectID + "].");
			}else{
				final String msg = "Fail instance Device [" + devName + "] in project [" + projectID + "].";
				LogManager.err(msg);
				throw new HCJRubyException(callCtx, msg);
			}
			CallContext.cycle(callCtx);
			MSBAgent.addSuperRightSet((Device)device);
			
			devices[itemIdx] = (Device)device;
		}
		return devices;
	}
	
	public final RobotWrapper getRobotWrapper(final String name) {
		Object robotWraps = null;
		try{
			robotWraps = getRobots();
		}catch (final Throwable e) {
		}
		
		if (robotWraps != null) {
			final String nameLower = name.toLowerCase();
			final int len = robots.length;
			for (int j = 0; j < len; j++) {
				final Robot robot = robots[j];
				if (nameLower.equals(MSBAgent.getNameLower(robot))) {
					return MSBAgent.getRobotWrapper(robot);
				}
			}
		}
		
		LogManager.errToLog("no Robot [" + name + "] in project [" + projectID + "].");
		return null;
	}
	
	public final Robot[] getRobots() throws Exception{
		if(robots != null){
			return robots;
		}
		
		final int itemCount = HCjarHelper.getRobotNum(map);
		
		if(itemCount <=0) {
			final Robot[] out = {};
			robots = out;
			return robots;
		}
		
		final Vector<String>[] vectors = HCjarHelper.getRobotsSrc(map);
		final Vector<String> names = vectors[0];
		final Vector<String> src = vectors[1];
		
		robots = new Robot[itemCount];
		
		for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
			
			final String robotName = names.elementAt(itemIdx);
			final String robotListener = src.elementAt(itemIdx);
			final String scriptName = robotName;
			
			LogManager.log("try build intance for Robot [" + robotName + "] in project [" + projectID + "]...");
			
			//将设备名称装入properties
			ServerUIAPIAgent.setSuperProp(context, ServerUIAPIAgent.ROBOT_NAME_PROP, robotName);
			final CallContext callCtx = CallContext.getFree();
			final Object robot = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(J2SESession.NULL_J2SESESSION_FOR_PROJECT, callCtx,
					robotListener, scriptName, null, hcje, context, Robot.class);
			if(robot != null){
				LogManager.log("successful build intance for Robot [" + robotName + "] in project [" + projectID + "].");
			}else{
				final String msg = "Fail instance Robot [" + robotName + "] in project [" + projectID + "].";
				LogManager.err(msg);
				throw new HCJRubyException(callCtx, msg);
			}
			CallContext.cycle(callCtx);
			robots[itemIdx] = (Robot)robot;
		}
		
		return robots;
	}
	
	public ProjResponser(final String projID, final Map<String, Object> p_map, final MobiUIResponsor baseRep,
			final LinkProjectStore lps) {
		this.projectID = projID;
		{
			final String absProjBasePath = HCLimitSecurityManager.getUserDataBaseDir(projectID);
			final File userDir = new File(absProjBasePath);//不能使用App.getBaseDir

			if (userDir.exists() == false) {
				userDir.mkdirs();
			}
		}
		isRoot = lps.isRoot();
		this.mobiResp = baseRep;
		this.map = p_map;
		this.lpsLinkName = lps.getLinkName();
		final File deployPath = new File(ResourceUtil.getBaseDir(), lps.getDeployTmpDir());
		final ClassLoader projClassLoader = ResourceUtil.buildProjClassLoader(deployPath, projID);
		final String reportExceptionURL = (String)this.map.get(HCjar.PROJ_EXCEPTION_REPORT_URL);
		this.hcje = new HCJRubyEngine(deployPath.getAbsolutePath(), projClassLoader, reportExceptionURL != null && reportExceptionURL.length() > 0);
		
		final RecycleRes tmpRecycleRes = RecycleProjThreadPool.getFree(projID);
		
		if(tmpRecycleRes != null){
			recycleRes = tmpRecycleRes;
			threadGroup = (ThreadGroup)recycleRes.threadPool.getThreadGroup();
//			System.out.println("recycle threadPool for ProjResponser");
		}else{
			threadGroup = new ThreadGroup("HarLimitThreadPoolGroup"){
				@Override
				public final void uncaughtException(final Thread t, final Throwable e) {
//					LogManager.log("******************uncaughtException*****************=>" + e.getMessage());
					ExceptionReporter.printStackTraceFromHAR(e, null, null);
				}
			};
			ClassUtil.changeParentToNull(threadGroup);
			final ThreadPool threadPool =	new ThreadPool(threadGroup){
				//每个工程实例下，用一个ThreadPool实例，以方便权限管理
				@Override
				public final Thread buildThread(final RecycleThread rt) {
					final Thread thread = new Thread((ThreadGroup)threadGroup, rt);
					thread.setName("lmtThread-in-" + ((ThreadGroup)threadGroup).getName() + "-" + thread.getId());
					thread.setPriority(ThreadPriorityManager.PROJ_CONTEXT_THREADPOOL_PRIORITY);
					return thread;
				}
				
				@Override
				protected void checkAccessPool(final Object token){
				}
				
				@Override
				public void printStack(){
					ClassUtil.printCurrentThreadStack("--------------nest stack--------------");
				}
			};
			recycleRes = new RecycleRes(projID + "-Project", threadPool);
		}
		recycleRes.threadPool.setName(projID + "-ThreadPool");
		context = ServerUIUtil.buildProjectContext(projID, (String)map.get(HCjar.PROJ_VER), recycleRes, this, new ProjClassLoaderFinder() {
			@Override
			public ClassLoader findProjClassLoader() {
				return projClassLoader;
			}
		}, ServerUIAPIAgent.CRATE_DB_PASSWORD);
		
		final ContextSecurityConfig csc = ContextSecurityConfig.getContextSecurityConfig(lps);
		csc.setProjResponser(this);
		csc.initSocketPermissionCollection();
		ContextSecurityManager.putContextSecurityConfig(threadGroup, csc);
		
		{
			//加载全部native lib
			final String str_NativeNum = (String)p_map.get(HCjar.SHARE_NATIVE_FILES_NUM);
			if(str_NativeNum != null){
				final int nativeNum = Integer.parseInt(str_NativeNum);
				final boolean hasLoadNativeLibPermission = csc.isLoadLib();
				
				if(nativeNum > 0 && hasLoadNativeLibPermission == false){
					LogManager.err("please make sure enable permission in [Project Manager/project id/modify | permission/permission/load native lib]");
					LogManager.errToLog("the permissions in [root node/permission/load native lib] are for designing, Not for running.");
				}else{
					final int currOS = NativeOSManager.getOS();
					for (int idx = 0; idx < nativeNum; idx++) {
						final int osMask = NativeOSManager.getOSMaskFromMap(p_map, idx);
						if(NativeOSManager.isMatchOS(currOS, osMask) == false){
							continue;
						}
						
						final String nativeLibName = (String)p_map.get(HCjar.replaceIdxPattern(HCjar.SHARE_NATIVE_FILE_NAME, idx));
						final File nativeFile = new File(deployPath, nativeLibName);
						final String absolutePath = nativeFile.getAbsolutePath();
						
						try{
							final String scripts = "import Java::hc.server.util.JavaLangSystemAgent\n" +
									"path = \"" + absolutePath + "\"\n" +
									"JavaLangSystemAgent.load(path)\n";
							
							//注意：
							//1. 不能在工程级线程中执行，因为目录无权限
							//2. 必须要用hcje的classloader来加载
							RubyExector.runAndWaitOnEngine(scripts, "loadNativeLib", null, hcje);
							LogManager.log("successful load native lib [" + nativeLibName + "] in project [" + projID + "].");
						}catch (final Throwable e) {
							LogManager.err("Fail to load native lib [" + nativeLibName + "] in project [" + projID + "]");
//							ExceptionReporter.printStackTrace(e);
						}
					}
				}
			}
		}
		
		final Object object = map.get(HCjar.MENU_NUM);
		if(object != null){
			//final int menuNum;
			Integer.parseInt((String)object);
			
			//mainMenuIdx;
			Integer.parseInt((String)map.get(HCjar.MAIN_MENU_IDX_PRE));
			
//			new JarMainMenu[menuNum];
//			for (int i = HCjar.MAIN_MENU_IDX; i < menuNum; i++) {
//				menu = new JarMainMenu(i, map, 
//						lps.isRoot(), 
//						baseRep, lps.getLinkName());
//			}
		}else{
//			menu = null;
		}
	}
	
	public final void initJarMainMenu(){
		jarMainMenu = new JarMainMenu(HCjar.MAIN_MENU_IDX, map, 
				isRoot, 
				mobiResp, lpsLinkName, this);//有可能为空内容
	}

	public static final boolean deloyToWorkingDir(final Map<String, Object> deployMap, final File shareResourceTopDir) {
		if(!shareResourceTopDir.exists()){
			shareResourceTopDir.mkdirs();
		}
		
		boolean hasResource = false;
		
		//创建共享资源目录
		try{
			{
				final String str_shareRubyNum = (String)deployMap.get(HCjar.SHARE_JRUBY_FILES_NUM);
				if(str_shareRubyNum != null){
					
					final int shareRubyNum = Integer.parseInt(str_shareRubyNum);
					for (int idx = 0; idx < shareRubyNum; idx++) {
						final String fileName = (String)deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_JRUBY_FILE_NAME, idx));
						final String fileContent = (String)deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_JRUBY_FILE_CONTENT, idx));
						
						final File jrubyFile = new File(shareResourceTopDir, fileName);
						
						final FileOutputStream fos = new FileOutputStream(jrubyFile);
						fos.write(fileContent.getBytes(Charset.forName(IConstant.UTF_8)));
						fos.flush();
						fos.close();
						
						hasResource = true;
					}
				}
			}
			
			{
				final String str_NativeNum = (String)deployMap.get(HCjar.SHARE_NATIVE_FILES_NUM);
				if(str_NativeNum != null){
					final int currOS = NativeOSManager.getOS();
					final int nativeNum = Integer.parseInt(str_NativeNum);
					for (int idx = 0; idx < nativeNum; idx++) {
						try{
							final int osMask = NativeOSManager.getOSMaskFromMap(deployMap, idx);
							if(NativeOSManager.isMatchOS(currOS, osMask) == false){
								continue;
							}
							
							final String fileName = (String)deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_NATIVE_FILE_NAME, idx));
							final byte[] fileContent = (byte[])deployMap.get(HCjar.MAP_FILE_PRE + fileName);
							
							final File file = new File(shareResourceTopDir, fileName);
							
							final FileOutputStream fos = new FileOutputStream(file);
							fos.write(fileContent);
							fos.flush();
							fos.close();
							
							LogManager.log("successful save native lib idx : " + idx);
						}catch (final Throwable e) {
							LogManager.errToLog("fail to save native lib idx : " + idx);
							ExceptionReporter.printStackTrace(e);
						}
						hasResource = true;
					}
				}
			}
			
			//创建共享jar
			for(final Map.Entry<String, Object> entry:deployMap.entrySet()){  
				final String keyName = entry.getKey();
				if(keyName.startsWith(HCjar.VERSION_FILE_PRE)){
					final String name = keyName.substring(HCjar.VERSION_FILE_PRE.length());
					
					String type = (String)deployMap.get(HCjar.MAP_FILE_TYPE_PRE + name);
					if(type == null){
						type = HPNode.MAP_FILE_JAR_TYPE;
					}
					
					if(HPNode.isNodeType(Integer.parseInt(type), HPNode.MASK_RESOURCE_ITEM)){
						final byte[] content = (byte[])deployMap.get(HCjar.MAP_FILE_PRE + name);
						
						final File file = new File(shareResourceTopDir, name);
						
						final FileOutputStream fos = new FileOutputStream(file);
						fos.write(content);
						fos.flush();
						fos.close();
						
						hasResource = true;
					}
				}
			}
		}catch(final Throwable e){
			ExceptionReporter.printStackTrace(e);
		}
		
		return hasResource;
	}
	
	public final Object onScriptEventInSequence(final J2SESession coreSS, final Object event) {
		if(event == null){
			return null;
		}else if(isScriptEventToAllProjects(event)){
			//优先执行主菜单的事件
			final String script = (String)map.get(HCjar.buildEventMapKey(HCjar.MAIN_MENU_IDX, event.toString()));
			if(script != null && script.trim().length() > 0){
				final ReturnableRunnable runnable = new ReturnableRunnable() {
					@Override
					public Object run() {
						final String eventName = event.toString();
						
						L.V = L.WShop ? false : LogManager.log("start OnEvent : " + event + " for project : " + context.getProjectID());
						final CallContext callCtx = CallContext.getFree();
						RubyExector.runAndWaitInProjectOrSessionPool(coreSS, callCtx, script, eventName, null, hcje, context);//考虑到PROJ_SHUTDOWN，所以改为阻塞模式
						if(callCtx.isError){
							if(mobiResp.ec != null){
								mobiResp.ec.setThrowable(new HCJRubyException(callCtx));
							}
						}
						CallContext.cycle(callCtx);
						L.V = L.WShop ? false : LogManager.log("done OnEvent : " + event + " for project : " + context.getProjectID());
						return null;
					}
				};
				
				if(coreSS != null){
					ServerUIAPIAgent.addSequenceWatcherInProjContextForSessionFirst(coreSS, this, runnable);
				}else{
					ServerUIAPIAgent.addSequenceWatcherInProjContext(context, new BaseWatcher() {
						@Override
						public boolean watch() {
							ServerUIAPIAgent.runAndWaitInProjContext(context, runnable);
							return true;
						}
					});
				}
			}

			return null;
		}else{
			return null;
		}
	}
	
	public final int PROP_LISTENER = 1;
	public final int PROP_EXTENDMAP = 2;
	public final int PROP_ITEM_NAME = 3;
	public final int PROP_ITEM_URL_ORI = 4;
	
	/**
	 * 没找到对应项目，返回null
	 */
	private final String getItemProp(final MenuItem item, final int type) {
		if(type == PROP_LISTENER){
			return ServerUIAPIAgent.getMobiMenuItem_Listener(item);//jarMainMenu.listener[j];
		}else if(type == PROP_EXTENDMAP){
			return ServerUIAPIAgent.getMobiMenuItem_extendMap(item);//jarMainMenu.extendMap[j];
		}else if(type == PROP_ITEM_NAME){
			return ServerUIAPIAgent.getMobiMenuItem_Name(item);//item[JarMainMenu.ITEM_NAME_IDX];
		}else if(type == PROP_ITEM_URL_ORI){
			return ServerUIAPIAgent.getMobiMenuItem_URL(item);
		}else{
			return null;
		}
	}

	private final MenuItem getItem(final J2SESession coreSS, final HCURL url, final boolean log) {
		final String oriURLLower = url.getURLLower();
		final String aliasLowerURL = url.getMletAliasURL();
		
		final MenuItem item = coreSS.searchMenuItem(projectID, oriURLLower, aliasLowerURL);
		if(item != null){
			if(log){
				LogManager.log(ILog.OP_STR + "click/go item : [" + ServerUIAPIAgent.getMobiMenuItem_Name(item) + "]");
			}
		}
		return item;
	}
	
	public final void preLoadJRubyScripts(){
		final Object menuItemObj = map.get(HCjar.replaceIdxPattern(HCjar.MENU_CHILD_COUNT, HCjar.MAIN_MENU_IDX));
		if(menuItemObj != null){
			final int itemCount = Integer.parseInt((String)menuItemObj);
			
			final String Iheader = HCjar.replaceIdxPattern(HCjar.MENU_ITEM_HEADER, HCjar.MAIN_MENU_IDX);
			for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
				final String header = Iheader + itemIdx + ".";
				
				final String scriptName = (String)map.get(header + HCjar.ITEM_NAME);
				final String script = (String)map.get(header + HCjar.ITEM_LISTENER);
			
				if(script == null || script.length() == 0){
					continue;
				}
				
				//与stop()互锁
				synchronized (this) {
					if(isStoped){
						return;
					}
					final CallContext callCtx = CallContext.getFree();
					RubyExector.parse(callCtx, script, scriptName, hcje, true);
					if(callCtx.isError){
						callCtx.rubyThrowable.printStackTrace();
					}
					CallContext.cycle(callCtx);
				}
			}
		}
	}
	
	public static final boolean isScriptEventToAllProjects(final Object event){
		for (int i = 0; i < BaseResponsor.SCRIPT_EVENT_LIST.length; i++) {
			if(event.equals(BaseResponsor.SCRIPT_EVENT_LIST[i])){
				return true;
			}
		}
		return false;
	}
	
//	private final boolean isMainElementID(final String elementID){
//		for (int i = 0; i < menu.length; i++) {
//			if(menu[i].menuId.equals(elementID)){
//				return menu[i].isRoot;
//			}
//		}
//		return false;
//	}

	public final boolean doBiz(final J2SESession coreSS, final HCURL url, final boolean sendMsgIfFail) {//因为HCURL要回收，所以不能final
		if(url.protocal == HCURL.MENU_PROTOCAL){
			final String elementID = url.elementID;
//			if(isMainElementID(elementID)){
//				currMenuIdx = mainMenuIdx;
//			}else{
////				关闭多菜单模式
//				for (int i = 0; i < menu.length; i++) {
//					if(elementID.equals(menu[i].menuId)){
//						currMenuIdx = i;
//						break;
//					}
//				}
//			}
			if(jarMainMenu != null && elementID.equals(jarMainMenu.menuId)){
				final JarMainMenu currMainMenu = jarMainMenu;
				LogManager.log(ILog.OP_STR + "open menu : [" + currMainMenu.getTitle(coreSS) + "]");
				
				ServerUIUtil.transMenuWithCache(coreSS, currMainMenu.buildJcip(coreSS), projectID);//elementID
				
				ScreenServer.pushScreen(coreSS, currMainMenu);
				return true;
	//			}else if(url.elementID.equals("no1")){
	//			e = new Mno1Menu();
			}

		}else if(url.protocal == HCURL.CMD_PROTOCAL){
			//由于可能不同context，所以要进行全遍历，而非假定在前一当前打开的基础上。
			final MenuItem item = getItem(coreSS, url, true);
			if(item != null){
				final String listener = getItemProp(item, PROP_LISTENER);
				if(listener != null){
					if(listener.trim().length() > 0){
						final Map<String, String> mapRuby = RubyExector.toMap(url);
						final String scriptName = getItemProp(item, PROP_ITEM_NAME);
								
						//由于不占用KeepAlive，不采用后台模式
						final CallContext callCtx = CallContext.getFree();
						RubyExector.runAndWaitInProjectOrSessionPool(coreSS, callCtx, listener, scriptName, mapRuby, hcje, context);
						CallContext.cycle(callCtx);
					}
					return true;
				}
			}
		}else if(((url.protocal == HCURL.SCREEN_PROTOCAL) && (HCURL.REMOTE_HOME_SCREEN.equals(url.getElementIDLower()) == false))//注意：screen://home必须手机先行调用
				|| url.protocal == HCURL.FORM_PROTOCAL){
			//由于可能不同context，所以要进行全遍历，而非假定在前一当前打开的基础上。
			final MenuItem item = getItem(coreSS, url, true);
			if(item != null){
				final String listener = getItemProp(item, PROP_LISTENER);
				if(listener != null){
					if(listener.trim().length() > 0){
						final Map<String, String> mapRuby = RubyExector.toMap(url);
						
						final String title = getItemProp(item, PROP_ITEM_NAME);
	
						final String urlOri = getItemProp(item, PROP_ITEM_URL_ORI);
						final HCURL oriHcurl = HCURLUtil.extract(urlOri);
						final String elementID = oriHcurl.elementID;
						HCURLCacher.getInstance().cycle(oriHcurl);
						
						//由于可能导致长时间占用Event线程，所以另起线程。同时因为url要回收，所以不能final
						final boolean isSynchronized = false;
	
						startMlet(coreSS, listener, mapRuby, urlOri, elementID, title, hcje, context, isSynchronized);
					}
					return true;
				}
			}
		}else if(url.protocal == HCURL.CONTROLLER_PROTOCAL){
			final MenuItem item = getItem(coreSS, url, true);
			if(item != null){
				final String listener = getItemProp(item, PROP_LISTENER);
				if(listener != null){
					if(listener.trim().length() > 0){
						final String targetURL = getItemProp(item, PROP_ITEM_URL_ORI);
						final String title = getItemProp(item, PROP_ITEM_NAME);
						final String scriptName = title;
						
						//由于可能导致长时间占用Event线程，所以另起线程。同时因为url要回收，所以不能final
						ContextManager.getThreadPool().run(new Runnable() {
							@Override
							public void run(){
								final StoreableHashMap map = new StoreableHashMap();
								final String map_str = getItemProp(item, PROP_EXTENDMAP);
	//							System.out.println("extendMap : " + map_str);
								map.restore(map_str);
	
								final Map<String, String> mapRuby = null;//RubyExector.toMap(url);
		
								Object obj;
								final CallContext callCtx = CallContext.getFree();
								callCtx.targetURL = targetURL;
								obj = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(coreSS, callCtx, listener, scriptName, mapRuby, hcje, context, CtrlResponse.class);
								CallContext.cycle(callCtx);
								if(obj == null){
									return;
								}
								
								final CtrlResponse responsor = (CtrlResponse)obj;
								try{
									final CtrlMap cmap = new CtrlMap(map);
									final ProjectContext ctx = ServerUIAPIAgent.getProjectContextFromCtrl(responsor);
									ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(ctx), new ReturnableRunnable() {
										@Override
										public Object run() {
											//添加初始按钮名
											final int[] keys = cmap.getButtonsOnCanvas();
											for (int i = 0; i < keys.length; i++) {
												final int key = keys[i];
												final String txt = responsor.getButtonInitText(key);
												if(txt != null && txt.length() > 0){
													if(AIPersistentManager.isEnableHCAI()){
														final String locale = UserThreadResourceUtil.getMobileLocaleFrom(coreSS);
														AIPersistentManager.processCtrl(locale, responsor.getTarget(), txt, ctx);
													}
												}
												cmap.setButtonTxt(key, txt);
											}
											return null;
										}
									});
									
									cmap.setTitle(title);
									final String screenID = ServerUIAPIAgent.buildScreenID(projectID, targetURL);
									cmap.setID(screenID);
									ServerUIUtil.response(coreSS, new MController(map, cmap.map.toSerial()).buildJcip(coreSS));
									
									final ServCtrlCanvas ccanvas = new ServCtrlCanvas(coreSS, responsor);
									ScreenServer.pushScreen(coreSS, ccanvas);
									MultiUsingManager.enter(coreSS, screenID, targetURL);
									
		//							LogManager.log("onLoad controller : " + url.elementID);
								}catch (final Exception e) {
									ExceptionReporter.printStackTrace(e);
								}
							}
						});
					}
					return true;
				}
			}
		}
		
		if(sendMsgIfFail){
			//没有找到相应的资源实现，比如:cmd://myCmd, screen://myScreen
			final String resource = StringUtil.replace((String)ResourceUtil.get(coreSS, 9122), "{resource}", url.url);
			final J2SESession[] coreSSS = {coreSS};
			ServerUIAPIAgent.sendMovingMsg(coreSSS, resource);
			LogManager.err(resource);
		}
		
		return false;
	}

	public static final boolean bringMletToTop(final J2SESession coreSS, final ProjectContext ctx, final String screenIDLower, final String targetURLLower) {
		if(ServerUIAPIAgent.isOnTopHistory(coreSS, ctx, screenIDLower, targetURLLower) 
				|| ServerUIAPIAgent.pushMletURLToHistory(coreSS, ctx, screenIDLower, targetURLLower) == false){//已经打开，进行置顶操作
			return true;
		}
		return false;
	}
	
	private static String loadLocalJS(final String name){
		try{
			//支持源码和jar模式
			return ResourceUtil.getStringFromInputStream(
					ResourceUtil.getResourceAsStream("hc/res/js/" + name), IConstant.UTF_8, true, true);
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
        return "";
	}
	
	private static final String hcloaderScript = loadLocalJS("hcloader.js");
	private static final String hcj2seScript = loadLocalJS("hcj2se.js");
	private static final String iOS2serverScript = loadLocalJS("ios2server.js");
	
	public static final void sendMletBodyOnlyOneTime(final J2SESession coreSS, final ProjectContext ctx){
		final String attBody = ClientSessionForSys.CLIENT_SESSION_ATTRIBUTE_IS_TRANSED_MLET_BODY;
		final ProjResponser pr = ServerUIAPIAgent.getProjResponserMaybeNull(ctx);
		
		final Object result = ServerUIAPIAgent.getClientSessionAttributeForSys(coreSS, pr, attBody);
		if(result == null){
			ServerUIAPIAgent.setClientSessionAttributeForSys(coreSS, pr, attBody, Boolean.TRUE);
		}else{
			return;//已发送，无需再发送。
		}
		
//		final boolean isAndroid = ConfigManager.getOSType()==ConfigManager.OS_ANDROID;
		
		final String mobileOS = UserThreadResourceUtil.getMobileOSFrom(coreSS);
		final boolean isIOS = mobileOS.equals(ProjectContext.OS_IOS);
		
		final StringBuilder initHTML = StringBuilderCacher.getFree();
		
		final String scale;
		if(isIOS){
			scale = String.valueOf( 1 / Float.valueOf(coreSS.clientDesc.getClientScale()));
		}else{
			scale = MobileAgent.DEFAULT_SCALE;
		}
		
		if(L.isInWorkshop){
			LogManager.log("HTMLMlet scale : " + scale);
		}
		
		final String colorForBodyByHexString = HTMLMlet.getColorForBodyByHexString();
		final SizeHeightForXML sizeHeightForXML = new SizeHeightForXML(coreSS);
		
		final String startBody = 
				"<html dir='ltr'>" +
				"<head>" +
				"<meta charset=\"utf-8\" />" +//注意：某些手机不支持width=device-width，必须width=600
				"<meta name=\"viewport\" content=\"target-densitydpi=device-dpi, user-scalable=no, width=" + UserThreadResourceUtil.getMobileWidthFrom(coreSS) + ", initial-scale=" + scale + ", minimum-scale=" + scale + ", maximum-scale=" + scale + "\"/>" +
				"<style type=\"text/css\">" +
				"  div > img {vertical-align:middle;}div > input {vertical-align:middle;}div > label{vertical-align:middle;}\n" +
				"  input, select, textarea, button{font-family:inherit;font-size:inherit;}\n" +
				"  .HC_DIV_SYS_0 {" +
					"font-size:" + sizeHeightForXML.getFontSizeForNormal() + "px;" +
					"background-color:#" + colorForBodyByHexString + ";" +
					"}" +
				"</style>" +
				"</head>" +
				"<body style=\"position:relative;margin:0px;background-color:transparent;color:#" + HTMLMlet.getColorForFontByHexString() + ";\">" +
				"<div id=\"HC_DIV_0\" class=\"HC_DIV_SYS_0\"></div>" +
				"<div id=\"HC_DIV_loading\" style=\"position:absolute;background-color:#" + colorForBodyByHexString + ";" +
						"width:" + UserThreadResourceUtil.getMobileWidthFrom(coreSS) + "px;" +
						"height:" + UserThreadResourceUtil.getMobileHeightFrom(coreSS) + "px;\"></div>" +
				"<div id=\"HC_DIV_tip\" style=\"pointer-events:none;font-size:" + (int)(UserThreadResourceUtil.getMobileDPIFrom(coreSS) * 1.4 /10) + "px;position:absolute;visibility:hidden;height:auto;top:30px;\">" +
				"</div>";
		final String endBody = 
				"</body>" +
				"</html>";

		initHTML.append(startBody);
		{
			final String scriptHeader = "<script type='text/javascript'>";
			final String scriptTail = "</script>";
			if(isIOS){
				initHTML.append(scriptHeader);
				initHTML.append(iOS2serverScript);
				initHTML.append(scriptTail);
			}

			initHTML.append(scriptHeader);
			initHTML.append(hcloaderScript);
			initHTML.append(scriptTail);
			
			initHTML.append(scriptHeader);
			initHTML.append(hcj2seScript);
			initHTML.append(scriptTail);
		}
		initHTML.append(endBody);
		
		final String sbStr = initHTML.toString();
		StringBuilderCacher.cycle(initHTML);
		final byte[] bs = StringUtil.getBytes(sbStr);
		
		final String projID = CacheManager.ELE_PROJ_ID_HTML_PROJ;
		final byte[] projIDbs = CacheManager.ELE_PROJ_ID_HTML_PROJ_BS;
		final String softUID = UserThreadResourceUtil.getMobileSoftUID(coreSS);
		final byte[] softUidBS = ByteUtil.getBytes(softUID, IConstant.UTF_8);
		final String urlID = CacheManager.ELE_URL_ID_HTML_BODY;
		final byte[] urlIDbs = CacheManager.ELE_URL_ID_HTML_BODY_BS;
		
		final CacheComparator bodyCacheComp = new CacheComparator(projID, softUID, urlID, projIDbs, softUidBS, urlIDbs) {
			@Override
			public void sendData(final Object[] paras) {
				final byte[] bodyBS = (byte[])paras[0];
				
				HCURLUtil.sendEClass(coreSS, HCURLUtil.CLASS_BODY_TO_MOBI, bodyBS, 0, bodyBS.length);
			}
		};
		final Object[] paras = {bs};
		bodyCacheComp.encodeGetCompare(coreSS, true, bs, 0, bs.length, paras);
	}
	
	public static final int getMletComponentCount(final J2SESession coreSS, final ProjectContext ctx, final Mlet mlet){
		final ReturnableRunnable run = new ReturnableRunnable() {
			@Override
			public Object run() {
				return mlet.getComponentCount();
			}
		};

		return (Integer)ServerUIAPIAgent.getProjResponserMaybeNull(ctx).getMobileSession(coreSS).recycleRes.threadPool.runAndWait(run);
	}

	public static final Mlet startMlet(final J2SESession coreSS,
			final String listener, final Map<String, String> mapRuby, final String targetURL,
			final String elementID, final String title, final HCJRubyEngine hcje, final ProjectContext context, final boolean isSynchronized) {
		final CallContext runCtx = CallContext.getFree();
		runCtx.targetURL = targetURL;
		
		if(isSynchronized){
			final Mlet out = pushInMlet(coreSS, runCtx, listener, mapRuby, targetURL, title, hcje, context);
			CallContext.cycle(runCtx);
			return out;
		}else{
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					pushInMlet(coreSS, runCtx, listener, mapRuby, targetURL, title, hcje, context);
					CallContext.cycle(runCtx);
				}
			});
			return null;
		}
	}
	
	public static boolean isMletMobileEnv(final J2SESession coreSS) {
		return ConfigManager.OS_J2ME_DESC.equals(UserThreadResourceUtil.getMobileAgent(coreSS).getOS());
	}

	public static void sendReceiver(final CoreSession coreSS, final String receiver, final String elementID) {
		final String[] paras = {HCURL.DATA_PARA_NOTIFY_RECEIVER, HCURL.DATA_PARA_NOTIFY_RECEIVER_PARA};
		final String[] values = {receiver, elementID};
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, paras, values);
	}

	private static Mlet pushInMlet(final J2SESession coreSS, final CallContext runCtx, 
			final String listener, final Map<String, String> mapRuby,
			final String targetURL, final String title, final HCJRubyEngine hcje,
			final ProjectContext context) {
		try{
			final String scriptName = title;
			
			final Object obj = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(coreSS, runCtx, listener, scriptName, mapRuby, hcje, context, Mlet.class);
			if(obj == null){
				return null;
			}
			
			final Mlet mlet = (Mlet)obj;
			
			ServerUIAPIAgent.openMlet(coreSS, context, mlet, targetURL, false, null);
			
			return mlet;
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

}
