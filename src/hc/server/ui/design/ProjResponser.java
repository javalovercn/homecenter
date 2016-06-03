package hc.server.ui.design;

import hc.App;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.L;
import hc.core.cache.CacheManager;
import hc.core.util.ByteUtil;
import hc.core.util.CtrlMap;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.MobileAgent;
import hc.core.util.RecycleThread;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StoreableHashMap;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPool;
import hc.core.util.ThreadPriorityManager;
import hc.core.util.UIUtil;
import hc.server.HCException;
import hc.server.ScreenServer;
import hc.server.data.screen.ScreenCapturer;
import hc.server.html5.syn.MletHtmlCanvas;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.MSBAgent;
import hc.server.msb.Robot;
import hc.server.ui.ClientDesc;
import hc.server.ui.CtrlResponse;
import hc.server.ui.HTMLMlet;
import hc.server.ui.ICanvas;
import hc.server.ui.IMletCanvas;
import hc.server.ui.MUIView;
import hc.server.ui.Mlet;
import hc.server.ui.MletSnapCanvas;
import hc.server.ui.ProjClassLoaderFinder;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServCtrlCanvas;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.hpj.HCjar;
import hc.server.ui.design.hpj.HCjarHelper;
import hc.server.ui.design.hpj.HPNode;
import hc.server.ui.design.hpj.RubyExector;
import hc.server.util.CacheComparator;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.util.BaseResponsor;
import hc.util.ClassUtil;
import hc.util.RecycleProjThreadPool;
import hc.util.ResourceUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Vector;

public class ProjResponser {
	public final Map<String, Object> map;
	public final ThreadPool threadPool;
	final JarMainMenu[] menu;
	final int mainMenuIdx;
	public final HCJRubyEngine hcje;
	public final ProjectContext context;
	Robot[] robots;
	Device[] devices;
	Converter[] converters;
	
	boolean isStoped = false;
	
	public final void stop(){
		synchronized (this) {
			isStoped = true;
			try{
				hcje.terminate();
			}catch (final Throwable e) {
				LogManager.errToLog("fail terminate JRuby engine : " + e.toString());
				ExceptionReporter.printStackTrace(e);
			}
		}
		
		RecycleProjThreadPool.recycle(context.getProjectID(), threadPool);
		
		ServerUIAPIAgent.set__projResponserMaybeNull(context, null);
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
			
				L.V = L.O ? false : LogManager.log("try build instance for Converter [" + converterName + "] in project [" + context.getProjectID() + "]...");
				
				//将转换器名称装入properties
				ServerUIAPIAgent.setSuperProp(context, ServerUIAPIAgent.CONVERT_NAME_PROP, converterName);
				final Object converter = RubyExector.run(src.elementAt(itemIdx), scriptName, null, hcje, context, Converter.class);
				if(converter != null){
					L.V = L.O ? false : LogManager.log("succesful build instance for Converter [" + converterName + "] in project [" + context.getProjectID() + "].");
				}else{
					final String msg = "Fail instance Converter [" + converterName + "] in project [" + context.getProjectID() + "].";
					LogManager.err(msg);
					throw new HCException(msg);
				}
			
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
		
		final Device[] devices = getDevices();
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
	
	public final Device[] getDevices() throws Exception {
		if(devices != null){
			return devices;
		}
		
		final int itemCount = HCjarHelper.getDeviceNum(map);
		
		if(itemCount <=0) {
			final Device[] out = {};
			devices = out;
			return devices;
		}
		
		final Vector<String>[] vectors = HCjarHelper.getDevicesSrc(map);
		final Vector<String> names = vectors[0];
		final Vector<String> src = vectors[1];
		
		devices = new Device[itemCount];
		
		for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
			
			final String devName = names.elementAt(itemIdx);
			final String devListener = src.elementAt(itemIdx);
			final String scriptName = devName;
			
			L.V = L.O ? false : LogManager.log("try build instance for Device [" + devName + "] in project [" + context.getProjectID() + "]...");
			
			//将设备名称装入properties
			ServerUIAPIAgent.setSuperProp(context, ServerUIAPIAgent.DEVICE_NAME_PROP, devName);
			final Object device = RubyExector.run(devListener, scriptName, null, hcje, context, Device.class);
			if(device != null){
				L.V = L.O ? false : LogManager.log("successful build instance for Device [" + devName + "] in project [" + context.getProjectID() + "].");
			}else{
				final String msg = "Fail instance Device [" + devName + "] in project [" + context.getProjectID() + "].";
				LogManager.err(msg);
				throw new HCException(msg);
			}
			MSBAgent.addSuperRightSet((Device)device);
			
			devices[itemIdx] = (Device)device;
		}
		return devices;
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
			
			L.V = L.O ? false : LogManager.log("try build intance for Robot [" + robotName + "] in project [" + context.getProjectID() + "]...");
			
			//将设备名称装入properties
			ServerUIAPIAgent.setSuperProp(context, ServerUIAPIAgent.ROBOT_NAME_PROP, robotName);
			final Object robot = RubyExector.run(robotListener, scriptName, null, hcje, context, Robot.class);
			if(robot != null){
				L.V = L.O ? false : LogManager.log("successful build intance for Robot [" + robotName + "] in project [" + context.getProjectID() + "].");
			}else{
				final String msg = "Fail instance Robot [" + robotName + "] in project [" + context.getProjectID() + "].";
				LogManager.err(msg);
				throw new HCException(msg);
			}
			
			robots[itemIdx] = (Robot)robot;
		}
		
		return robots;
	}
	
	public ProjResponser(final String projID, final Map<String, Object> p_map, final BaseResponsor baseRep,
			final LinkProjectStore lps) {
		this.map = p_map;
		final File deployPath = new File(App.getBaseDir(), lps.getDeployTmpDir());
		final ClassLoader projClassLoader = ResourceUtil.buildProjClassLoader(deployPath, projID);
		final String reportExceptionURL = (String)this.map.get(HCjar.PROJ_EXCEPTION_REPORT_URL);
		this.hcje = new HCJRubyEngine(deployPath.getAbsolutePath(), projClassLoader, reportExceptionURL != null && reportExceptionURL.length() > 0);
		
		final ThreadPool tmpPool = RecycleProjThreadPool.getFree(projID);
		
		ThreadGroup threadGroup;
		if(tmpPool != null){
			threadPool = tmpPool;
			threadGroup = (ThreadGroup)threadPool.getThreadGroup();
//			System.out.println("recycle threadPool for ProjResponser");
		}else{
			threadGroup = new ThreadGroup("HarLimitThreadPoolGroup"){
				@Override
				public final void uncaughtException(final Thread t, final Throwable e) {
//					L.V = L.O ? false : LogManager.log("******************uncaughtException*****************=>" + e.getMessage());
					ExceptionReporter.printStackTraceFromHAR(e, null, null);
				}
			};
			ClassUtil.changeParentToNull(threadGroup);
			threadPool =	new ThreadPool(threadGroup){
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
		}
		threadPool.setName(projID + "-ThreadPool");
		context = new ProjectContext(projID, (String)map.get(HCjar.PROJ_VER), threadPool, this, new ProjClassLoaderFinder() {
			@Override
			public ClassLoader findProjClassLoader() {
				return projClassLoader;
			}
		});
		
		final ContextSecurityConfig csc = ContextSecurityConfig.getContextSecurityConfig(lps);
		csc.setProjResponser(this);
		csc.initSocketPermissionCollection();
		ContextSecurityManager.putContextSecurityConfig(threadGroup, csc);
		
		{
			//加载全部native lib
			final String str_NativeNum = (String)p_map.get(HCjar.SHARE_NATIVE_FILES_NUM);
			if(str_NativeNum != null){
				final int shareRubyNum = Integer.parseInt(str_NativeNum);
				final boolean hasLoadNativeLibPermission = csc.isLoadLib();
				for (int idx = 0; idx < shareRubyNum; idx++) {
					final String nativeLibName = (String)p_map.get(HCjar.replaceIdxPattern(HCjar.SHARE_NATIVE_FILE_NAME, idx));
					if(hasLoadNativeLibPermission){
						final File nativeFile = new File(deployPath, nativeLibName);
						final String absolutePath = nativeFile.getAbsolutePath();
						try{
							System.load(absolutePath);
							L.V = L.O ? false : LogManager.log("successful load native lib [" + nativeLibName + "] in project [" + projID + "].");
						}catch (final Throwable e) {
							LogManager.err("Fail to load native lib [" + nativeLibName + "] in project [" + projID + "]");
							ExceptionReporter.printStackTrace(e);
						}
					}else{
						LogManager.err("No permission to load native lib [" + nativeLibName + "] in project [" + projID + "]");
					}
				}
				
				if(shareRubyNum > 0 && hasLoadNativeLibPermission == false){
					LogManager.err("please make sure enable permission in [Project Manager/project id/modify | permission/permission/load native lib]");
					LogManager.errToLog("the permissions in [root node/permission/load native lib] are for designing, Not for running.");
				}
			}
		}
		
		baseRep.addProjectContext(context);
		
		final Object object = map.get(HCjar.MENU_NUM);
		if(object != null){
			final int menuNum = Integer.parseInt((String)object);
			mainMenuIdx = Integer.parseInt((String)map.get(HCjar.MAIN_MENU_IDX));
			menu = new JarMainMenu[menuNum];
			
			for (int i = 0; i < menuNum; i++) {
				menu[i] = new JarMainMenu(i, map, 
						lps.isRoot(), 
						baseRep, lps.getLinkName());
			}
		}else{
			menu = new JarMainMenu[0];
			mainMenuIdx = 0;
		}
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
					
					final int shareRubyNum = Integer.parseInt(str_NativeNum);
					for (int idx = 0; idx < shareRubyNum; idx++) {
						final String fileName = (String)deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_NATIVE_FILE_NAME, idx));
						final byte[] fileContent = (byte[])deployMap.get(HCjar.MAP_FILE_PRE + fileName);
						
						final File file = new File(shareResourceTopDir, fileName);
						
						final FileOutputStream fos = new FileOutputStream(file);
						fos.write(fileContent);
						fos.flush();
						fos.close();
						
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
	
	public final Object onScriptEvent(final Object event) {
		if(event == null){
			return null;
		}else if(isScriptEvent(event)){
			
			//优先执行主菜单的事件
			String script = (String)map.get(HCjar.buildEventMapKey(mainMenuIdx, event.toString()));
			if(script != null && script.trim().length() > 0){
				final String scriptName = event.toString();
				
//				System.out.println("OnEvent : " + event + ", script : " + script + ", scriptcontain : " + hcje.container);
				RubyExector.run(script, scriptName, null, hcje, context);//考虑到PROJ_SHUTDOWN，所以改为阻塞模式
			}
			
			//其次执行非主菜单的事件脚本，依自然序列
			for (int i = 0; i < menu.length; i++) {
				if(i == mainMenuIdx){
					continue;
				}
				script = (String)map.get(HCjar.buildEventMapKey(i, event.toString()));
				if(script != null && script.trim().length() > 0){
					final String scriptName = event.toString();
					
//					System.out.println("OnEvent : " + event + ", script : " + script + ", scriptcontain : " + hcje.container);
					RubyExector.run(script, scriptName, null, hcje, context);//考虑到PROJ_SHUTDOWN，所以改为阻塞模式
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
	
	/**
	 * 没找到对应项目，返回null
	 */
	private final String getItemProp(final HCURL url, final int type, final boolean log) {
		final String oriURL = url.url;
		final String aliasURL = HCURL.buildMletAliasURL(oriURL);
		
		for (int i = 0; i < menu.length; i++) {
			final JarMainMenu jarMainMenu = menu[i];
			final String[][] menuItems = jarMainMenu.items;
			for (int j = 0, size = menuItems.length; j < size; j++) {
				final String menuURL = menuItems[j][JarMainMenu.ITEM_URL_IDX];
				if(menuURL.equals(oriURL) || menuURL.equals(aliasURL)){
					if(log){
						L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "click/go item : [" + menuItems[j][JarMainMenu.ITEM_NAME_IDX] + "]");
					}
					if(type == PROP_LISTENER){
						return jarMainMenu.listener[j];
					}else if(type == PROP_EXTENDMAP){
						return jarMainMenu.extendMap[j];
					}else if(type == PROP_ITEM_NAME){
						return menuItems[j][JarMainMenu.ITEM_NAME_IDX];
					}else{
						return null;
					}
				}
			}
		}
		return null;
	}
	
	public final void preLoadJRubyScripts(){
		for (int i = 0; i < menu.length; i++) {
			final JarMainMenu jarMainMenu = menu[i];
			
			jarMainMenu.rebuildMenuItemArray();
			
			for (int j = 0; j < jarMainMenu.items.length; j++) {
				final String script = jarMainMenu.listener[j];
				final String scriptName = jarMainMenu.items[j][JarMainMenu.ITEM_NAME_IDX];
				
				//与stop()互锁
				synchronized (this) {
					if(isStoped){
						return;
					}
					RubyExector.parse(script, scriptName, hcje, true);
				}
			}
		}
	}
	
	public static final boolean isScriptEvent(final Object event){
		for (int i = 0; i < BaseResponsor.SCRIPT_EVENT_LIST.length; i++) {
			if(event.equals(BaseResponsor.SCRIPT_EVENT_LIST[i])){
				return true;
			}
		}
		return false;
	}
	
	private final boolean isMainElementID(final String elementID){
		for (int i = 0; i < menu.length; i++) {
			if(menu[i].menuId.equals(elementID)){
				return menu[i].isRoot;
			}
		}
		return false;
	}

	public final boolean doBiz(final HCURL url) {//因为HCURL要回收，所以不能final
		ServerUIAPIAgent.setTMPTarget(context, url.url, url.elementID);
		
		final MUIView e = null;
		if(url.protocal == HCURL.MENU_PROTOCAL){
			int currMenuIdx = -1;
			
			final String elementID = url.elementID;
			if(isMainElementID(elementID)){
				currMenuIdx = mainMenuIdx;
			}else{
//				关闭多菜单模式
				for (int i = 0; i < menu.length; i++) {
					if(elementID.equals(menu[i].menuId)){
						currMenuIdx = i;
						break;
					}
				}
			}
			if(currMenuIdx >= 0 && currMenuIdx < menu.length){
				final JarMainMenu currMainMenu = menu[currMenuIdx];
				L.V = L.O ? false : LogManager.log(ScreenCapturer.OP_STR + "open menu : [" + currMainMenu.getTitle() + "]");
				
				ServerUIUtil.transMenuWithCache(currMainMenu.buildJcip(), context.getProjectID());//elementID
				
				ScreenServer.pushScreen(currMainMenu);
				return true;
	//			}else if(url.elementID.equals("no1")){
	//			e = new Mno1Menu();
			}

		}else if(url.protocal == HCURL.CMD_PROTOCAL){
			//由于可能不同context，所以要进行全遍历，而非假定在前一当前打开的基础上。
			final String listener = getItemProp(url, PROP_LISTENER, true);
			if(listener != null){
				if(listener.trim().length() > 0){
					final Map<String, String> mapRuby = RubyExector.toMap(url);
					final String scriptName = getItemProp(url, PROP_ITEM_NAME, false);
							
					//由于某些长任务，可能导致KeepAlive被长时间等待，而导致手机端侦测断线，所以本处采用后台模式
					RubyExector.runLater(listener, scriptName, mapRuby, hcje, context);
				}
				return true;
			}

//		}else if(url.protocal == HCURL.FORM_PROTOCAL){//与下段进行合并
//			if(url.elementID.equals("form1")){
//				e = new TestMForm();
//				JcipManager.addFormTimer("form1", new IFormTimer(){
//					int count = 1;
//					boolean isSend = false;
//					public String doAutoResponse() {
//						if(isSend == false){
//							isSend = true;
//							try {
//								ServerUIUtil.sendMessage("Cap", "Hello,Message", IContext.ERROR, ImageIO.read(ResourceUtil.getResource("hc/res/hc_48.jpg")), 0);
//								
//								ServerUIUtil.sendMessage("Cap2", "Hello,Message2", IContext.ERROR, null, 0);
//								
//								try {
//									ServerUIUtil.sendAUSound(ResourceUtil.getAbsPathContent("/hc/server/ui/ship.au"));
//								} catch (Exception e) {
//									ExceptionReporter.printStackTrace(e);
//								}
//							} catch (IOException e1) {
//								e1ExceptionReporter.printStackTrace(e);
//							}
//						}
//						if(1+5 < 2){
//							L.V = L.O ? false : LogManager.log("Send out Alert");
//							if(count < 100){
//								ServerUIUtil.alertOn();
//							}else{
//								ServerUIUtil.alertOff();	
//							}
//						}
//						return "{<'/form1'>,<['false','true'],'1'>,<'"+System.currentTimeMillis()+"'>,<'','Sys_Img','1'>,<{'0', '1','16'},'" + (count++) + "'><'0','UCB_BASIC_LATIN','" + (count++) + "'>,<'50','50','50'>}";
//					}
//
//					public int getSecondMS() {
//						return 1000;
//					}});
//				TODO onPause, onResume
//				ScreenServer.pushScreen(e);
//				L.V = L.O ? false : LogManager.log("onStart Form : " + url.elementID);
//			}
		}else if(url.protocal == HCURL.SCREEN_PROTOCAL || url.protocal == HCURL.FORM_PROTOCAL){
			//由于可能不同context，所以要进行全遍历，而非假定在前一当前打开的基础上。
			final String listener = getItemProp(url, PROP_LISTENER, true);
			if(listener != null){
				if(listener.trim().length() > 0){
					final Map<String, String> mapRuby = RubyExector.toMap(url);
					final String elementID = url.elementID;
					final String strUrl = url.url;

					if(bringMletToTop(context, strUrl)){
						return true;
					}
					
					final String title = getItemProp(url, PROP_ITEM_NAME, false);
					
					//由于可能导致长时间占用Event线程，所以另起线程。同时因为url要回收，所以不能final
					startMlet(listener, mapRuby, elementID, title, hcje, context);
				}
				return true;
			}
		}else if(url.protocal == HCURL.CONTROLLER_PROTOCAL){
			final StoreableHashMap map = new StoreableHashMap();
			final String map_str = getItemProp(url, PROP_EXTENDMAP, true);
//			System.out.println("extendMap : " + map_str);
			map.restore(map_str);
			final String listener = getItemProp(url, PROP_LISTENER, false);
			if(listener != null){
				if(listener.trim().length() > 0){
					final String url_url = url.url;
					final String title = getItemProp(url, PROP_ITEM_NAME, false);
					final String scriptName = title;
					
					//由于可能导致长时间占用Event线程，所以另起线程。同时因为url要回收，所以不能final
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run(){
							final Map<String, String> mapRuby = null;//RubyExector.toMap(url);
	
							Object obj;
							obj = RubyExector.run(listener, scriptName, mapRuby, hcje, context, CtrlResponse.class);
							if(obj == null){
								return;
							}
							
							final CtrlResponse responsor = (CtrlResponse)obj;
							try{
								final CtrlMap cmap = new CtrlMap(map);
								responsor.getProjectContext().runAndWait(new Runnable() {
									@Override
									public void run() {
										//添加初始按钮名
										final int[] keys = cmap.getButtonsOnCanvas();
										for (int i = 0; i < keys.length; i++) {
											final String txt = responsor.getButtonInitText(keys[i]);
											cmap.setButtonTxt(keys[i], txt);
										}
									}
								});
								
								cmap.setTitle(title);
								cmap.setID(url_url);
								ServerUIUtil.response(new MController(map, cmap.map.toSerial()).buildJcip());
								
								ServerUIAPIAgent.setCurrentCtrlID(responsor, url_url);
								
								final ServCtrlCanvas ccanvas = new ServCtrlCanvas(responsor);
								ScreenServer.pushScreen(ccanvas);
	
	//							L.V = L.O ? false : LogManager.log("onLoad controller : " + url.elementID);
							}catch (final Exception e) {
								ExceptionReporter.printStackTrace(e);
							}
						}
					});
				}
				return true;
			}
		}
		if(e != null){
			ServerUIUtil.response(e.buildJcip());
			return true;
		}else{
			//没有找到相应的资源实现，比如:cmd://myCmd, screen://myScreen
			final String resource = StringUtil.replace((String)ResourceUtil.get(9122), "{resource}", url.url);
			context.sendMovingMsg(resource);
			LogManager.err(resource);
			return false;
		}
	}

	public static final boolean bringMletToTop(final ProjectContext ctx, final String mletURL) {
		if(ServerUIAPIAgent.isOnTopHistory(ctx, mletURL) 
				|| ServerUIAPIAgent.pushMletURLToHistory(ctx, mletURL) == false){//已经打开，进行置顶操作
			return true;
		}
		return false;
	}
	
	private static String loadLocalJS(final String name){
		try{
			//支持源码和jar模式
			return ResourceUtil.getStringFromInputStream(
					ResourceUtil.getResourceAsStream("hc/server/html5/res/" + name), IConstant.UTF_8, true, true);
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
        return "";
	}
	
	private static final String hcloaderScript = loadLocalJS("hcloader.js");
	private static final String hcj2seScript = loadLocalJS("hcj2se.js");
	private static final String iOS2serverScript = loadLocalJS("ios2server.js");
	
	private static final void sendMletBodyOnlyOneTime(final ProjectContext ctx){
		final String attBody = ServerUIAPIAgent.CLIENT_SESSION_ATTRIBUTE_IS_TRANSED_MLET_BODY;
		
		final Object result = ServerUIAPIAgent.getClientSessionAttribute(ctx, attBody);
		if(result == null){
			ServerUIAPIAgent.setClientSessionAttribute(ctx, attBody, Boolean.TRUE);
		}else{
			return;//已发送，无需再发送。
		}
		
		final String defaultRGB = StringUtil.toARGB(UIUtil.DEFAULT_COLOR_BACKGROUND, false);
//		final boolean isAndroid = ConfigManager.getOSType()==ConfigManager.OS_ANDROID;
		
		final boolean isIOS = ctx.getMobileOS().equals(ProjectContext.OS_IOS);
		
		final StringBuilder initHTML = new StringBuilder(isIOS?iOS2serverScript.length():0 + hcloaderScript.length() + hcj2seScript.length() 
				+ 10000);
		
		final String scale;
		if(ctx.getMobileOS() == ProjectContext.OS_IOS){
			scale = String.valueOf( 1 / Float.valueOf(ClientDesc.getClientScale()));
		}else{
			scale = MobileAgent.DEFAULT_SCALE;
		}
		
		final String startBody = 
				"<html dir='ltr'>" +
				"<head>" +
				"<meta charset=\"utf-8\" />" +//注意：某些手机不支持width=device-width，必须width=600
				"<meta name=\"viewport\" content=\"user-scalable=no, width=" + ctx.getMobileWidth() + ", initial-scale=" + scale + ", minimum-scale=" + scale + ", maximum-scale=" + scale + "\"/>" +
				"<style>div > img {vertical-align:middle;}div > input {vertical-align:middle;}div > label{vertical-align:middle;}</style>" +
				"</head>" +
				"<body style=\"position:relative;margin:0px;\">" +
				"<div id=\"hc_div_0\"></div>" +
				"<div id=\"hc_div_loading\" style=\"position:absolute;background:#" + defaultRGB + ";" +
						"width:" + ctx.getMobileWidth() + "px;" +
						"height:" + ctx.getMobileHeight() + "px;\"></div>" +
				"<div id=\"hc_div_tip\" style=\"pointer-events:none;font-size:" + (int)(ClientDesc.getDPI() * 1.4 /10) + "px;position:absolute;visibility:hidden;height:auto;top:30px;\">" +
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
		
		final byte[] bs = StringUtil.getBytes(initHTML.toString());
		
		final String projID = CacheManager.ELE_PROJ_ID_HTML_PROJ;
		final byte[] projIDbs = CacheManager.ELE_PROJ_ID_HTML_PROJ_BS;
		final String uuid = ServerUIAPIAgent.getMobileUID();
		final byte[] uuidBS = ByteUtil.getBytes(uuid, IConstant.UTF_8);
		final String urlID = CacheManager.ELE_URL_ID_HTML_BODY;
		final byte[] urlIDbs = CacheManager.ELE_URL_ID_HTML_BODY_BS;
		
		final CacheComparator bodyCacheComp = new CacheComparator(projID, uuid, urlID, projIDbs, uuidBS, urlIDbs) {
			@Override
			public void sendData(final Object[] paras) {
				final byte[] bodyBS = (byte[])paras[0];
				
				HCURLUtil.sendEClass(HCURLUtil.CLASS_BODY_TO_MOBI, bodyBS, 0, bodyBS.length);
			}
		};
		final Object[] paras = {bs};
		bodyCacheComp.encodeGetCompare(bs, 0, bs.length, paras);
	}
	
	public static final int getMletComponentCount(final ProjectContext ctx, final Mlet mlet){
		return (Integer)ServerUIAPIAgent.getThreadPoolFromProjectContext(ctx).runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				return mlet.getComponentCount();
			}
		});
	}

	public static final void startMlet(final String listener,
			final Map<String, String> mapRuby, final String elementID,
			final String title, final HCJRubyEngine hcje, final ProjectContext context) {
		ContextManager.getThreadPool().run(new Runnable() {
			
			@Override
			public void run() {
				try{
					final String scriptName = title;
					final Object obj = RubyExector.run(listener, scriptName, mapRuby, hcje, context, Mlet.class);
					if(obj == null){
						return;
					}
					
					final Mlet mlet = (Mlet)obj;
					
					openMlet(elementID, title, context, mlet);
				}catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		});
	}
	
	public static boolean isMletMobileEnv() {
		return ClientDesc.getAgent().getOS().equals(ConfigManager.OS_J2ME_DESC);
	}

	private static void sendReceiver(final String receiver, final String elementID) {
		final String[] paras = {HCURL.DATA_PARA_NOTIFY_RECEIVER, HCURL.DATA_PARA_NOTIFY_RECEIVER_PARA};
		final String[] values = {receiver, elementID};
		HCURLUtil.sendCmd(HCURL.DATA_CMD_SendPara, paras, values);
	}

	/**
	 * 
	 * @param elementID 标识当前对象的唯一串，长度不限。同时被应用于缓存。注：是设计器中url.elementID段
	 * @param title
	 * @param context
	 * @param mlet
	 */
	public static void openMlet(final String elementID, final String title,
			final ProjectContext context, final Mlet mlet) {
		boolean isHTMLMlet = (mlet instanceof HTMLMlet);
		final IMletCanvas mcanvas;
		if(isHTMLMlet == false || isMletMobileEnv() || getMletComponentCount(context, mlet) == 0){
			if(isHTMLMlet){
				L.V = L.O ? false : LogManager.log("force HTMLMlet to Mlet, because there is no component in it or for J2ME mobile.");
				isHTMLMlet = false;
			}
			sendReceiver(HCURL.DATA_RECEIVER_MLET, elementID);
			mcanvas = new MletSnapCanvas(ClientDesc.getClientWidth(), ClientDesc.getClientHeight());
		}else{
			sendMletBodyOnlyOneTime(context);
			sendReceiver(HCURL.DATA_RECEIVER_HTMLMLET, elementID);
			mcanvas = new MletHtmlCanvas(ClientDesc.getClientWidth(), ClientDesc.getClientHeight());
		}
		
		mcanvas.setScreenIDAndTitle(elementID, title);//注意：要在setMlet之前，因为后者可能用到本参数
		
		try{
			Thread.sleep(ThreadPriorityManager.UI_WAIT_MS * 3);//如果手机性能较差，可能会导致手机端对象正在初始中，而后续数据已送达。
		}catch (final Throwable e) {
		}

		mcanvas.setMlet(mlet, context);
		context.runAndWait(new Runnable() {
			@Override
			public void run() {
				mcanvas.init();//in user thread
			}
		});
		
		ScreenServer.pushScreen((ICanvas)mcanvas);

		if(isHTMLMlet){
			L.V = L.O ? false : LogManager.log(" onStart HTMLMlet screen : [" + title + "]");
		}else{
			L.V = L.O ? false : LogManager.log(" onStart Mlet screen : [" + title + "]");
		}
	}

}
