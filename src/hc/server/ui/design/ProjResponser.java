package hc.server.ui.design;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

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
import hc.server.PlatformManager;
import hc.server.ScreenServer;
import hc.server.data.StoreDirManager;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.MSBAgent;
import hc.server.msb.Robot;
import hc.server.msb.RobotWrapper;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.msb.WorkingDeviceList;
import hc.server.ui.ClientSessionForSys;
import hc.server.ui.CtrlResponse;
import hc.server.ui.HTMLMlet;
import hc.server.ui.MenuItem;
import hc.server.ui.Mlet;
import hc.server.ui.MobiMenu;
import hc.server.ui.ProjClassLoaderFinder;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServCtrlCanvas;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SizeHeight;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.engine.RubyExector;
import hc.server.ui.design.engine.ScriptValue;
import hc.server.ui.design.hpj.HCjar;
import hc.server.ui.design.hpj.HCjarHelper;
import hc.server.ui.design.hpj.HPNode;
import hc.server.util.Assistant;
import hc.server.util.CacheComparator;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.ContextSecurityManager;
import hc.server.util.SafeDataManager;
import hc.server.util.ServerUtil;
import hc.server.util.VoiceCommand;
import hc.server.util.ai.AIPersistentManager;
import hc.util.BaseResponsor;
import hc.util.ClassUtil;
import hc.util.RecycleProjThreadPool;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

public class ProjResponser {
	final static boolean threadPoolDaemon = true;
	public final Map<String, Object> map;
	public final RecycleRes recycleRes;
	public JarMainMenu jarMainMenu;// 注意：menu不允许为null，主菜单可能出现空内容
	public final HCJRubyEngine hcje;
	final ThreadGroup threadGroup;
	final String lpsLinkName;
	public final ProjectContext context;
	final boolean isCSCSocketLimitOn;
	final Vector<String> allowedDomains;
	private final Object assistantLock = new Object();
	private final Hashtable<String, Object> sys_att_map_sys = new Hashtable<String, Object>();
	Robot[] robots;
	Device[] devices;
	Converter[] converters;
	final MobiUIResponsor mobiResp;
	public final String projectID;
	private final Object lockSequTask = new Object();
	final boolean isRoot;
	final ClassLoader jrubyClassLoader;
	final boolean hasNative;
	public final boolean hasLocationOfMobile;
	public final boolean hasPermissionScriptPanel;
	final SessionMobileContext mobileContexts = new SessionMobileContext();
	private final ThreadGroup token = App.getThreadPoolToken();

	private boolean isFinishAllSequ = false;

	final void notifyFinishAllSequTask() {
		synchronized (lockSequTask) {
			isFinishAllSequ = true;
			lockSequTask.notifyAll();
		}
		L.V = L.WShop ? false : LogManager.log("notify all sequence task is done [" + projectID + "].");
	}

	public final boolean isCSCSocketLimitOn() {
		return isCSCSocketLimitOn;
	}

	public final Vector<String> getCSCAllowedDomains() {
		return allowedDomains;
	}

	final void waitForFinishAllSequTask() {
		synchronized (lockSequTask) {
			if (isFinishAllSequ == false) {
				L.V = L.WShop ? false : LogManager.log("wait for all sequence task done when shutdown project [" + projectID + "].");
				try {
					lockSequTask.wait(ThreadPriorityManager.SEQUENCE_TASK_MAX_WAIT_MS);
				} catch (final InterruptedException e) {
				}
			}
		}
		L.V = L.WShop ? false : LogManager.log("all sequence task is done [" + projectID + "].");
	}

	public final void initSessionContext(final J2SESession coreSS) {
		if (getMobileSession(coreSS) == null) {// 有可能安装HAR时，因为showInputDialog，已设置完成
			final SessionContext mc = useFreeMobileContext(coreSS);
			mc.setClientSession(coreSS, ServerUIAPIAgent.buildClientSession(coreSS, hasLocationOfMobile),
					new ClientSessionForSys(coreSS, token));
		}
	}

	public final SessionContext useFreeMobileContext(final J2SESession coreSS) {
		final SessionContext mc = SessionContext.getFreeMobileContext(projectID, threadGroup, this);
		mobileContexts.appendCurrContext(coreSS, mc);
		return mc;
	}

	public final boolean dispatchVoiceCommandAndWait(final J2SESession coreSS, final VoiceCommand vc) {
		final Assistant assistant = ServerUIAPIAgent.getVoiceAssistant(context);
		if (assistant != null) {
			try {
				return (Boolean) ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, this, new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						synchronized (assistantLock) {
							return assistant.onVoice(vc);
						}
					}
				});
			} catch (final Throwable e) {// 拦截未正常返回的异常
				e.printStackTrace();
			}
		}

		return false;
	}

	public final SessionContext getMobileSession(final J2SESession socket) {
		return mobileContexts.getMobileContext(socket);
	}

	public final void removeMobileContext(final J2SESession socket) {
		mobileContexts.removeSession(projectID, socket);
	}

	public final Object __setSysAtt(final String name, final Object obj) {
		return sys_att_map_sys.put(name, obj);
	}

	public final Object __removeSysAtt(final String name) {
		return sys_att_map_sys.remove(name);
	}

	public final Object __getSysAtt(final String name, final boolean isClearAfterGet) {
		if (isClearAfterGet) {
			return sys_att_map_sys.remove(name);
		} else {
			return sys_att_map_sys.get(name);
		}
	}

	/**
	 * 
	 * @return 有可能返回null
	 */
	public final SessionContext getSessionContextFromCurrThread() {
		ThreadGroup group, mobileGroup;
		mobileGroup = Thread.currentThread().getThreadGroup();
		if (mobileGroup == threadGroup) {
			return null;
		}

		boolean isMatch = false;
		try {
			do {
				group = mobileGroup.getParent();

				if (group == threadGroup) {
					isMatch = true;
					break;
				}

				if (group == null) {
					break;
				}

				mobileGroup = group;
			} while (true);
		} catch (final Exception e) {
		}

		if (isMatch) {
			return mobileContexts.getSessionContextByThreadGroup(mobileGroup);
		}

		return null;
	}

	boolean isStoped = false;

	public final void stop() {
		L.V = L.WShop ? false : LogManager.log("stop ProjResponser [" + projectID + "]");

		ServerUIAPIAgent.shutdownSchedulers(context, null);// 置于hcje.terminate之前

		try {
			synchronized (this) {
				isStoped = true;
				try {
					hcje.terminate();
				} catch (final Throwable e) {
					LogManager.errToLog("fail terminate JRuby engine : " + e.toString());
					ExceptionReporter.printStackTrace(e);
				}
			}

			RecycleProjThreadPool.recycle(projectID, recycleRes);

			mobileContexts.release(projectID);

			ServerUIAPIAgent.notifyShutdown(context);
			ServerUIAPIAgent.set__projResponserMaybeNull(context, null);

			if (robots != null) {
				final int size = robots.length;
				for (int i = 0; i < size; i++) {
					MSBAgent.setRobotWrapperNull(robots[i]);
				}
			}
		} catch (final Throwable e) {
			if (L.isInWorkshop) {
				e.printStackTrace();
			}
		}

		if (hasNative) {
			try {
				ClassUtil.invokeWithExceptionOut(Object.class, jrubyClassLoader, "finalize", ClassUtil.NULL_PARA_TYPES,
						ClassUtil.NULL_PARAS, true);
			} catch (final Throwable e) {
			}
		}
	}

	public final Converter[] getConverters() throws Exception {
		if (converters != null) {
			return converters;
		}

		final int size = HCjarHelper.getConverterNum(map);

		if (size <= 0) {
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

			// 将转换器名称装入properties
			ServerUIAPIAgent.setHCSysProperties(context, ServerUIAPIAgent.CONVERT_NAME_PROP, converterName);
			final CallContext callCtx = CallContext.getFree();
			final Object converter = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(J2SESession.NULL_J2SESESSION_FOR_PROJECT,
					callCtx, new ScriptValue(src.elementAt(itemIdx)), scriptName, null, hcje, context, Converter.class);
			if (converter != null) {
				LogManager.log("succesful build instance for Converter [" + converterName + "] in project [" + projectID + "].");
			} else {
				final String msg = "Fail instance Converter [" + converterName + "] in project [" + projectID + "].";
				LogManager.err(msg);
				throw new HCJRubyException(callCtx, msg);
			}
			CallContext.cycle(callCtx);

			converters[itemIdx] = (Converter) converter;
		}

		return converters;
	}

	final void loadProcessors(final MSBAgent agent) throws Exception {
		final Converter[] converters = getConverters();
		if (converters != null) {
			for (int j = 0; j < converters.length; j++) {
				agent.addConverter(converters[j]);
			}
		}

		final Device[] devices = getDevices(agent.workbench.getWorkingDeviceList(projectID));
		if (devices != null) {
			for (int j = 0; j < devices.length; j++) {
				agent.addDevice(devices[j]);
			}
		}

		final Robot[] robots = getRobots();
		if (robots != null) {
			for (int j = 0; j < robots.length; j++) {
				agent.addRobot(robots[j]);
			}
		}
	}

	public final Device[] getDevices(final WorkingDeviceList list) throws Exception {
		if (devices != null) {
			return devices;
		}

		{
			final int itemCount = HCjarHelper.getDeviceNum(map);

			if (itemCount <= 0) {
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

			// 将设备名称装入properties
			ServerUIAPIAgent.setHCSysProperties(context, ServerUIAPIAgent.DEVICE_NAME_PROP, devName);
			final CallContext callCtx = CallContext.getFree();
			final Object device = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(J2SESession.NULL_J2SESESSION_FOR_PROJECT, callCtx,
					new ScriptValue(devListener), scriptName, null, hcje, context, Device.class);
			if (device != null) {
				LogManager.log("successful build instance for Device [" + devName + "] in project [" + projectID + "].");
			} else {
				final String msg = "Fail instance Device [" + devName + "] in project [" + projectID + "].";
				LogManager.err(msg);
				throw new HCJRubyException(callCtx, msg);
			}
			CallContext.cycle(callCtx);
			MSBAgent.addSuperRightSet((Device) device);

			devices[itemIdx] = (Device) device;
		}
		return devices;
	}

	public final RobotWrapper getRobotWrapper(final String name) {
		Object robotWraps = null;
		try {
			robotWraps = getRobots();
		} catch (final Throwable e) {
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

	public final Robot[] getRobots() throws Exception {
		if (robots != null) {
			return robots;
		}

		final int itemCount = HCjarHelper.getRobotNum(map);

		if (itemCount <= 0) {
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

			// 将设备名称装入properties
			ServerUIAPIAgent.setHCSysProperties(context, ServerUIAPIAgent.ROBOT_NAME_PROP, robotName);
			final CallContext callCtx = CallContext.getFree();
			final Object robot = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(J2SESession.NULL_J2SESESSION_FOR_PROJECT, callCtx,
					new ScriptValue(robotListener), scriptName, null, hcje, context, Robot.class);
			if (robot != null) {
				LogManager.log("successful build intance for Robot [" + robotName + "] in project [" + projectID + "].");
			} else {
				final String msg = "Fail instance Robot [" + robotName + "] in project [" + projectID + "].";
				LogManager.err(msg);
				throw new HCJRubyException(callCtx, msg);
			}
			CallContext.cycle(callCtx);
			robots[itemIdx] = (Robot) robot;
		}

		return robots;
	}

	public final void setLocationUpdates(final long minTimes) {
		mobiResp.setPublishLocationMS(minTimes);
	}

	public ProjResponser(final String projID, final Map<String, Object> p_map, final MobiUIResponsor baseRep, final LinkProjectStore lps) {
		this.projectID = projID;
		{
			final String absProjBasePath = StoreDirManager.getUserDataBaseDir(projectID);
			final File userDir = new File(absProjBasePath);// 不能使用App.getBaseDir

			if (userDir.exists() == false) {
				userDir.mkdirs();
			}
		}
		isRoot = lps.isRoot();
		this.mobiResp = baseRep;
		this.map = p_map;
		this.lpsLinkName = lps.getLinkName();
		final File deployPath = new File(ResourceUtil.getBaseDir(), lps.getDeployTmpDir());
		final String str_NativeNum = (String) p_map.get(HCjar.SHARE_NATIVE_FILES_NUM);
		hasNative = str_NativeNum != null;

		// final ClassLoader projClassLoader =
		// ResourceUtil.buildProjClassLoader(deployPath, projID);
		// **注意**：理论上native
		// lib应加载到userLib，而不是JRubyClassLoader。但是无法获得用户类作为参数，所以简化之
		final ClassLoader baseClassLoader = ServerUtil.getJRubyClassLoader(false);
		jrubyClassLoader = hasNative ? buildStubNativeLibClassLoaderWrapper(baseClassLoader) : baseClassLoader;
		final ClassLoader projClassLoader = ServerUtil.buildProjClassPath(deployPath, jrubyClassLoader, projID);
		final String reportExceptionURL = (String) this.map.get(HCjar.PROJ_EXCEPTION_REPORT_URL);
		this.hcje = new HCJRubyEngine(null, deployPath.getAbsolutePath(), projClassLoader,
				reportExceptionURL != null && reportExceptionURL.length() > 0, projID);
		
		final RecycleRes tmpRecycleRes = RecycleProjThreadPool.getFree(projID);

		if (tmpRecycleRes != null) {
			recycleRes = tmpRecycleRes;
			threadGroup = (ThreadGroup) recycleRes.threadPool.getThreadGroup();
			// System.out.println("recycle threadPool for ProjResponser");
		} else {
			threadGroup = new ThreadGroup("HarLimitThreadPoolGroup") {
				@Override
				public final void uncaughtException(final Thread t, final Throwable e) {
					// LogManager.log("******************uncaughtException*****************=>"
					// + e.getMessage());
					ExceptionReporter.printStackTraceFromHAR(e, null, null);
				}
			};
			ClassUtil.changeParentToNull(threadGroup);
			final ThreadPool threadPool = new ThreadPool(threadGroup, false, ThreadPool.TYPE_PROJECT) {
				// 每个工程实例下，用一个ThreadPool实例，以方便权限管理
				@Override
				public final Thread buildThread(final RecycleThread rt) {
					final Thread thread = new Thread((ThreadGroup) threadGroup, rt);
					thread.setName("lmtThread-in-" + ((ThreadGroup) threadGroup).getName() + "-" + thread.getId());
					thread.setPriority(ThreadPriorityManager.PROJ_CONTEXT_THREADPOOL_PRIORITY);

					// 考虑到scheduler,数据库均已关闭，故设置daemon=true
					thread.setDaemon(threadPoolDaemon);// 缺省取parent.isDaemon()
					return thread;
				}

				@Override
				protected void checkAccessPool(final Object token) {
				}

				@Override
				public void printStack() {
					ClassUtil.printCurrentThreadStack("--------------nest stack--------------");
				}
			};
			recycleRes = new RecycleRes(projID + "-Project", threadPool);
		}
		recycleRes.threadPool.setName(projID + "-ThreadPool");
		context = ServerUIUtil.buildProjectContext(projID, (String) map.get(HCjar.PROJ_VER), recycleRes, this, new ProjClassLoaderFinder() {
			@Override
			public ClassLoader findProjClassLoader() {
				return projClassLoader;
			}
		}, ServerUIAPIAgent.CRATE_DB_PASSWORD);

		final ContextSecurityConfig csc = ContextSecurityConfig.getContextSecurityConfig(lps);
		csc.setProjResponser(this);
		csc.initSocketPermissionCollection();
		ContextSecurityManager.putContextSecurityConfig(threadGroup, csc);

		isCSCSocketLimitOn = csc.isSocketLimitOn();
		allowedDomains = csc.getAllowedDomains();
		hasLocationOfMobile = csc.isLocation();
		hasPermissionScriptPanel = csc.isScriptPanel();
		baseRep.hasLocationOfMobile(hasLocationOfMobile);

		{
			// 加载全部native lib
			if (hasNative) {
				final int nativeNum = Integer.parseInt(str_NativeNum);
				final boolean hasLoadNativeLibPermission = csc.isLoadLib();

				if (nativeNum > 0 && hasLoadNativeLibPermission == false) {
					LogManager.err(
							"please make sure enable permission in [Project Manager/project id/modify | permission/permission/load native lib]");
					LogManager.errToLog("the permissions in [root node/permission/load native lib] are for designing, Not for running.");
				} else {
					final int currOS = NativeOSManager.getOS();
					for (int idx = 0; idx < nativeNum; idx++) {
						final int osMask = NativeOSManager.getOSMaskFromMap(p_map, idx);
						if (NativeOSManager.isMatchOS(currOS, osMask) == false) {
							continue;
						}

						final String nativeLibName = (String) p_map.get(HCjar.replaceIdxPattern(HCjar.SHARE_NATIVE_FILE_NAME, idx));
						final File nativeFile = new File(deployPath, nativeLibName);
						final String absolutePath = nativeFile.getAbsolutePath();

						boolean isLoadOK = false;
						try {
							loadNativeLibByClassLoad(jrubyClassLoader, absolutePath);
							isLoadOK = true;
						} catch (final Throwable e) {
						}
						if (isLoadOK == false) {
							try {
								final String script = "import Java::hc.server.util.JavaLangSystemAgent\n" + "path = \"" + absolutePath
										+ "\"\n" + "JavaLangSystemAgent.load(path)\n";
								final ScriptValue sv = new ScriptValue(script);

								// 注意：
								// 1. 不能在工程级线程中执行，因为目录无权限
								// 2. 必须要用hcje的classloader来加载
								RubyExector.runAndWaitOnEngine(sv, "loadNativeLib", null, hcje);

								isLoadOK = true;
							} catch (final Throwable e) {
								e.printStackTrace();
							}
						}

						if (isLoadOK) {
							LogManager.log("successful load native lib [" + nativeLibName + "] in project [" + projID + "].");
						} else {
							LogManager.err("Fail to load native lib [" + nativeLibName + "] in project [" + projID + "]");
						}
					}
				}
			}
		}

		final File tmp_sub_for_hc_sys = StoreDirManager.getTmpSubForUserManagedByHcSys(context);
		ResourceUtil.deleteDirectoryNow(tmp_sub_for_hc_sys, false);// 注意：false means 不删除顶级tmp目录
		
		RubyExector.initActive(hcje, true);//注意：由于可能未来涉及特权，所以不入projectThreads
	}

	private final void loadNativeLibByClassLoad(final ClassLoader loader, final String absolutePath) throws Throwable {
		final Class jrubyClass = Class.forName("stub.StubLib", false, loader);// 加载native到stub的ClassLoader上，而不是JRubyClassLoader
		L.V = L.WShop ? false : LogManager.log("successful load class [stub.StubLib].");

		final Class classLoader = ClassLoader.class;

		// static void loadLibrary(Class fromClass, String name, boolean
		// isAbsolute)
		final Class[] paraTypes = { Class.class, String.class, boolean.class };
		final Object[] para = { jrubyClass, absolutePath, Boolean.TRUE };
		ClassUtil.invokeWithExceptionOut(classLoader, classLoader, "loadLibrary", paraTypes, para, true);
		L.V = L.WShop ? false : LogManager.log("successful loadNativeLibByClassLoad : " + absolutePath);
	}

	public final void initJarMainMenu() {
		jarMainMenu = new JarMainMenu(HCjar.MAIN_MENU_IDX, map, isRoot, mobiResp, lpsLinkName, this);// 有可能为空内容
	}

	public static final boolean deloyToWorkingDir(final Map<String, Object> deployMap, final File shareResourceTopDir) {
		if (!shareResourceTopDir.exists()) {
			shareResourceTopDir.mkdirs();
		}

		boolean hasResource = false;

		// 创建共享资源目录
		try {
			{
				final String str_shareRubyNum = (String) deployMap.get(HCjar.SHARE_JRUBY_FILES_NUM);
				if (str_shareRubyNum != null) {

					final int shareRubyNum = Integer.parseInt(str_shareRubyNum);
					for (int idx = 0; idx < shareRubyNum; idx++) {
						final String fileName = (String) deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_JRUBY_FILE_NAME, idx));
						final String fileContent = (String) deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_JRUBY_FILE_CONTENT, idx));

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
				final String str_NativeNum = (String) deployMap.get(HCjar.SHARE_NATIVE_FILES_NUM);
				if (str_NativeNum != null) {
					final int currOS = NativeOSManager.getOS();
					final int nativeNum = Integer.parseInt(str_NativeNum);
					for (int idx = 0; idx < nativeNum; idx++) {
						try {
							final int osMask = NativeOSManager.getOSMaskFromMap(deployMap, idx);
							if (NativeOSManager.isMatchOS(currOS, osMask) == false) {
								continue;
							}

							final String fileName = (String) deployMap.get(HCjar.replaceIdxPattern(HCjar.SHARE_NATIVE_FILE_NAME, idx));
							final byte[] fileContent = (byte[]) deployMap.get(HCjar.MAP_FILE_PRE + fileName);

							final File file = new File(shareResourceTopDir, fileName);

							final FileOutputStream fos = new FileOutputStream(file);
							fos.write(fileContent);
							fos.flush();
							fos.close();

							L.V = L.WShop ? false : LogManager.log("successful save native lib idx : " + idx);
						} catch (final Throwable e) {
							LogManager.errToLog("fail to save native lib idx : " + idx);
							ExceptionReporter.printStackTrace(e);
						}
						hasResource = true;
					}
				}
			}

			// 创建共享jar
			for (final Map.Entry<String, Object> entry : deployMap.entrySet()) {
				final String keyName = entry.getKey();
				if (keyName.startsWith(HCjar.VERSION_FILE_PRE)) {
					final String name = keyName.substring(HCjar.VERSION_FILE_PRE.length());

					String type = (String) deployMap.get(HCjar.MAP_FILE_TYPE_PRE + name);
					if (type == null) {
						type = HPNode.MAP_FILE_JAR_TYPE;
					}

					if (HPNode.isNodeType(Integer.parseInt(type), HPNode.MASK_RESOURCE_ITEM)) {
						final byte[] content = (byte[]) deployMap.get(HCjar.MAP_FILE_PRE + name);

						final File file = new File(shareResourceTopDir, name);

						final FileOutputStream fos = new FileOutputStream(file);
						fos.write(content);
						fos.flush();
						fos.close();

						hasResource = true;
					}
				}
			}
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}

		return hasResource;
	}

	public final Object onScriptEventInSequence(final J2SESession coreSS, final Object event) {
		if (event == null) {
			return null;
		} else if (isScriptEventToAllProjects(event)) {
			// 优先执行主菜单的事件
			final String eventName = event.toString();
			final String svKey = HCjar.buildEventMapKeyForStringValue(HCjar.MAIN_MENU_IDX, eventName);
			ScriptValue sv = (ScriptValue) map.get(svKey);
			if (sv == null) {
				final String script = (String) map.get(HCjar.buildEventMapKey(HCjar.MAIN_MENU_IDX, eventName));
				sv = new ScriptValue(script);
				map.put(svKey, sv);
			}
			final String script = sv.value;
			if (script != null && script.trim().length() > 0) {
				final boolean checkHasProjStartScriptErr;
				if (ProjectContext.EVENT_SYS_MOBILE_LOGIN.equals(eventName)) {
					checkHasProjStartScriptErr = IConstant.TRUE
							.equals(ServerUIAPIAgent.getSysAttribute(this, ServerUIAPIAgent.KEY_HAS_ERROR_PS_SCRIPT, true));
				} else {
					checkHasProjStartScriptErr = false;
				}
				final ScriptValue strValue = sv;
				final ReturnableRunnable runnable = new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {

						L.V = L.WShop ? false : LogManager.log("start OnEvent : " + event + " for project : " + context.getProjectID());
						if (checkHasProjStartScriptErr) {
							notifyScriptErrorInProjectOrSessionPool(coreSS, ProjectContext.EVENT_SYS_PROJ_STARTUP);
						}
						final CallContext callCtx = CallContext.getFree();
						RubyExector.runAndWaitInProjectOrSessionPool(coreSS, callCtx, strValue, eventName, null, hcje, context);// 考虑到PROJ_SHUTDOWN，所以改为阻塞模式
						if (callCtx.isError) {
							notifyScriptErrorInProjectOrSessionPool(coreSS, eventName);
							if (mobiResp.ec != null) {
								mobiResp.ec.setThrowable(new HCJRubyException(callCtx));
							}
						}
						CallContext.cycle(callCtx);
						L.V = L.WShop ? false : LogManager.log("done OnEvent : " + event + " for project : " + context.getProjectID());
						return null;
					}
				};

				if (coreSS != null) {
					ServerUIAPIAgent.addSequenceWatcherInProjContextForSessionFirst(coreSS, this, runnable);
				} else {
					ServerUIAPIAgent.addSequenceWatcherInProjContext(context, new BaseWatcher() {
						@Override
						public boolean watch() {
							ServerUIAPIAgent.runAndWaitInProjContext(context, runnable);
							return true;
						}
					});
				}
			} else {// else script null or zero length
				if (ProjectContext.EVENT_SYS_MOBILE_LOGIN.equals(eventName)) {
					final Object sysAtt = ServerUIAPIAgent.getSysAttribute(this, ServerUIAPIAgent.KEY_HAS_ERROR_PS_SCRIPT, true);// in
																																	// system
																																	// thread
																																	// level
					if (IConstant.TRUE.equals(sysAtt)) {
						ServerUIAPIAgent.runInSessionThreadPool(coreSS, this, new Runnable() {
							@Override
							public void run() {
								notifyScriptErrorInProjectOrSessionPool(coreSS, ProjectContext.EVENT_SYS_PROJ_STARTUP);// 补充通知Project_StartUp错误
							}
						});
					}
				}
			}

			return null;
		} else {
			return null;
		}
	}

	private final void notifyScriptErrorInProjectOrSessionPool(final J2SESession coreSSMaybeNull, final String eventName) {
		if (ProjectContext.EVENT_SYS_MOBILE_LOGIN.equals(eventName) || ProjectContext.EVENT_SYS_PROJ_STARTUP.equals(eventName)) {
			final String errorI18N = ResourceUtil.getErrorI18N(coreSSMaybeNull);
			String notExecuted = ResourceUtil.get(coreSSMaybeNull, 9267);
			notExecuted = StringUtil.replace(notExecuted, "{projID}", projectID);
			notExecuted = StringUtil.replace(notExecuted, "{eventName}", eventName);
			if (context.sendMessage(errorI18N, notExecuted, ProjectContext.MESSAGE_ERROR, null, 0) == false) {
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						ServerUIAPIAgent.setSysAttribute(ProjResponser.this, ServerUIAPIAgent.KEY_HAS_ERROR_PS_SCRIPT, IConstant.TRUE);
					}
				}, token);
			}
		}
	}

	public final int PROP_EXTENDMAP = 2;
	public final int PROP_ITEM_NAME = 3;
	public final int PROP_ITEM_URL_ORI = 4;

	/**
	 * 没找到对应项目，返回null
	 */
	private final String getItemProp(final MenuItem item, final int type) {
		if (type == PROP_EXTENDMAP) {
			return ServerUIAPIAgent.getMobiMenuItem_extendMap(item);// jarMainMenu.extendMap[j];
		} else if (type == PROP_ITEM_NAME) {
			return ServerUIAPIAgent.getMobiMenuItem_Name(item);// item[JarMainMenu.ITEM_NAME_IDX];
		} else if (type == PROP_ITEM_URL_ORI) {
			return ServerUIAPIAgent.getMobiMenuItem_URL(item);
		} else {
			return null;
		}
	}

	private final MenuItem getItem(final J2SESession coreSS, final HCURL url, final boolean log) {
		final String oriURLLower = url.getURLLower();
		final String aliasLowerURL = url.getMletAliasURL();

		final MenuItem item = coreSS.searchMenuItem(projectID, oriURLLower, aliasLowerURL);
		if (item != null) {
			if (log) {
				LogManager.log(ILog.OP_STR + "click/go item : [" + ServerUIAPIAgent.getMobiMenuItem_Name(item) + "]");
			}
		}
		return item;
	}

	public final void preLoadJRubyScripts() {
		L.V = L.WShop ? false : LogManager.log("pre load JRuby scripts for project [" + projectID + "].");

		final JarMainMenu menu = jarMainMenu;
		if (menu != null) {
			final MobiMenu projMenu = menu.projectMenu;
			final int size = projMenu.getModifiableItemsCount();
			for (int itemIdx = 0; itemIdx < size; itemIdx++) {
				final MenuItem item = projMenu.getModifiableItemAt(itemIdx);
				if (item == null) {
					return;
				}

				final ScriptValue sv = ServerUIAPIAgent.getMobiMenuItem_Listener(item);
				final String script = sv.value;
				if (script == null || script.length() == 0) {
					continue;
				}

				// 与stop()互锁
				synchronized (this) {
					if (isStoped) {
						return;
					}
					final CallContext callCtx = CallContext.getFree();
					RubyExector.parse(callCtx, sv, item.getText(), hcje, true);
					if (callCtx.isError) {
						callCtx.rubyThrowable.printStackTrace();
					}
					CallContext.cycle(callCtx);
				}
			}
		}
	}

	public static final boolean isScriptEventToAllProjects(final Object event) {
		for (int i = 0; i < BaseResponsor.SCRIPT_EVENT_LIST.length; i++) {
			if (event.equals(BaseResponsor.SCRIPT_EVENT_LIST[i])) {
				return true;
			}
		}
		return false;
	}

	// private final boolean isMainElementID(final String elementID){
	// for (int i = 0; i < menu.length; i++) {
	// if(menu[i].menuId.equals(elementID)){
	// return menu[i].isRoot;
	// }
	// }
	// return false;
	// }

	public final boolean doBiz(final J2SESession coreSS, final HCURL url, final boolean sendMsgIfFail) {// 因为HCURL要回收，所以不能final
		if (url.protocal == HCURL.MENU_PROTOCAL) {
			final String elementID = url.elementID;
			// if(isMainElementID(elementID)){
			// currMenuIdx = mainMenuIdx;
			// }else{
			//// 关闭多菜单模式
			// for (int i = 0; i < menu.length; i++) {
			// if(elementID.equals(menu[i].menuId)){
			// currMenuIdx = i;
			// break;
			// }
			// }
			// }
			if (jarMainMenu != null && elementID.equals(jarMainMenu.menuId)) {
				final JarMainMenu currMainMenu = jarMainMenu;
				LogManager.log(ILog.OP_STR + "open menu : [" + currMainMenu.getTitle(coreSS) + "]");

				currMainMenu.transMenuWithCache(coreSS);
				ScreenServer.pushScreen(coreSS, currMainMenu);
				return true;
				// }else if(url.elementID.equals("no1")){
				// e = new Mno1Menu();
			}

		} else if (url.protocal == HCURL.CMD_PROTOCAL) {
			coreSS.notifyCanvasMenuResponse();
			// 由于可能不同context，所以要进行全遍历，而非假定在前一当前打开的基础上。
			final MenuItem item = getItem(coreSS, url, true);
			if (item != null) {
				final ScriptValue sv = ServerUIAPIAgent.getMobiMenuItem_Listener(item);
				final String listener = sv.value;
				if (listener != null) {
					if (listener.trim().length() > 0) {
						final Map<String, String> mapRuby = RubyExector.toMap(url);
						final String scriptName = getItemProp(item, PROP_ITEM_NAME);

						// 由于不占用KeepAlive，不采用后台模式
						final CallContext callCtx = CallContext.getFree();
						RubyExector.runAndWaitInProjectOrSessionPool(coreSS, callCtx, sv, scriptName, mapRuby, hcje, context);
						CallContext.cycle(callCtx);
					}
					return true;
				}
			}
		} else if (((url.protocal == HCURL.SCREEN_PROTOCAL) && (HCURL.REMOTE_HOME_SCREEN.equals(url.getElementIDLower()) == false))// 注意：screen://home必须手机先行调用
				|| url.protocal == HCURL.FORM_PROTOCAL) {
			coreSS.notifyCanvasMenuResponse();
			// 由于可能不同context，所以要进行全遍历，而非假定在前一当前打开的基础上。
			final MenuItem item = getItem(coreSS, url, true);
			if (item != null) {
				final ScriptValue sv = ServerUIAPIAgent.getMobiMenuItem_Listener(item);
				final String listener = sv.value;
				if (listener != null) {
					if (listener.trim().length() > 0) {
						final Map<String, String> mapRuby = RubyExector.toMap(url);

						final String title = getItemProp(item, PROP_ITEM_NAME);

						final String urlOri = getItemProp(item, PROP_ITEM_URL_ORI);
						final HCURL oriHcurl = HCURLUtil.extract(urlOri);
						final String elementID = oriHcurl.elementID;
						HCURLCacher.getInstance().cycle(oriHcurl);

						// 由于可能导致长时间占用Event线程，所以另起线程。同时因为url要回收，所以不能final
						final boolean isSynchronized = false;

						startMlet(coreSS, sv, mapRuby, urlOri, elementID, title, hcje, context, isSynchronized);
					}
					return true;
				}
			}
		} else if (url.protocal == HCURL.CONTROLLER_PROTOCAL) {
			coreSS.notifyCanvasMenuResponse();
			final MenuItem item = getItem(coreSS, url, true);
			if (item != null) {
				final ScriptValue sv = ServerUIAPIAgent.getMobiMenuItem_Listener(item);
				final String listener = sv.value;
				if (listener != null) {
					if (listener.trim().length() > 0) {
						final String targetURL = getItemProp(item, PROP_ITEM_URL_ORI);
						final String title = getItemProp(item, PROP_ITEM_NAME);
						final String scriptName = title;

						// 由于可能导致长时间占用Event线程，所以另起线程。同时因为url要回收，所以不能final
						ContextManager.getThreadPool().run(new Runnable() {
							@Override
							public void run() {
								final StoreableHashMap map = new StoreableHashMap();
								final String map_str = getItemProp(item, PROP_EXTENDMAP);
								// System.out.println("extendMap : " + map_str);
								map.restore(map_str);

								final Map<String, String> mapRuby = null;// RubyExector.toMap(url);

								Object obj;
								final CallContext callCtx = CallContext.getFree();
								callCtx.targetURL = targetURL;
								obj = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(coreSS, callCtx, sv, scriptName, mapRuby, hcje,
										context, CtrlResponse.class);
								CallContext.cycle(callCtx);
								if (obj == null) {
									return;
								}

								final CtrlResponse responsor = (CtrlResponse) obj;
								try {
									final CtrlMap cmap = new CtrlMap(map);
									final ProjectContext ctx = ServerUIAPIAgent.getProjectContextFromCtrl(responsor);
									ServerUIAPIAgent.runAndWaitInSessionThreadPool(coreSS, ServerUIAPIAgent.getProjResponserMaybeNull(ctx),
											new ReturnableRunnable() {
												@Override
												public Object run() throws Throwable {
													// 添加初始按钮名
													final int[] keys = cmap.getButtonsOnCanvas();
													for (int i = 0; i < keys.length; i++) {
														final int key = keys[i];
														final String txt = responsor.getButtonInitText(key);
														if (txt != null && txt.length() > 0) {
															if (AIPersistentManager.isEnableHCAI()) {
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
									ServerUIUtil.response(coreSS, new MController(map, cmap.map.toSerial()).buildJcip(coreSS, null));

									final ServCtrlCanvas ccanvas = new ServCtrlCanvas(coreSS, responsor);
									ScreenServer.pushScreen(coreSS, ccanvas);
									MultiUsingManager.enter(coreSS, screenID, targetURL);

									// LogManager.log("onLoad controller : " +
									// url.elementID);
								} catch (final Exception e) {
									ExceptionReporter.printStackTrace(e);
								}
							}
						});
					}
					return true;
				}
			}
		}

		if (sendMsgIfFail) {
			// 没有找到相应的资源实现，比如:cmd://myCmd, screen://myScreen
			final String resource = StringUtil.replace(ResourceUtil.get(coreSS, 9122), "{resource}", url.url);
			final J2SESession[] coreSSS = { coreSS };
			ServerUIAPIAgent.sendMovingMsg(coreSSS, resource);
			LogManager.err(resource);
		}

		return false;
	}

	static ClassLoader buildStubNativeLibClassLoaderWrapper(final ClassLoader cl) {
		final String stubLibName;
		if (ResourceUtil.isAndroidServerPlatform()) {
			stubLibName = SafeDataManager.STUB_DEX_JAR;
		} else {
			stubLibName = SafeDataManager.STUB_JAR;
		}

		final File stubFile = new File(ResourceUtil.getBaseDir(), stubLibName);
		if (stubFile.exists() == false) {
			final InputStream is = ResourceUtil.getResourceAsStream("hc/res/" + stubLibName);
			ResourceUtil.saveToFile(is, stubFile);
		}

		final File[] files = { stubFile };
		return PlatformManager.getService().loadClasses(files, cl, true, "stub");
	}

	public static final boolean bringMletToTop(final J2SESession coreSS, final ProjectContext ctx, final String screenIDLower,
			final String targetURLLower) {
		if (ServerUIAPIAgent.isOnTopHistory(coreSS, ctx, screenIDLower, targetURLLower)
				|| ServerUIAPIAgent.pushMletURLToHistory(coreSS, ctx, screenIDLower, targetURLLower) == false) {// 已经打开，进行置顶操作
			return true;
		}
		return false;
	}

	private static String loadLocalJS(final String name) {
		try {
			// 支持源码和jar模式
			return ResourceUtil.getStringFromInputStream(ResourceUtil.getResourceAsStream("hc/res/js/" + name), IConstant.UTF_8, true,
					true);
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return "";
	}

	private static final String hcloaderScript = loadLocalJS("hcloader.js");
	private static final String hcj2seScript = loadLocalJS("hcj2se.js");
	private static final String iOS2serverScript = loadLocalJS("ios2server.js");

	public static final void sendMletBodyOnlyOneTime(final J2SESession coreSS) {
		if (coreSS.isTranedMletBody == false) {
			coreSS.isTranedMletBody = true;
		} else {
			return;// 已发送，无需再发送。
		}

		// final boolean isAndroid =
		// ConfigManager.getOSType()==ConfigManager.OS_ANDROID;

		final String mobileOS = UserThreadResourceUtil.getMobileOSFrom(coreSS);
		final boolean isIOS = mobileOS.equals(ProjectContext.OS_IOS);

		final StringBuilder initHTML = StringBuilderCacher.getFree();

		final String scale;
		// if(isIOS){
		// scale = String.valueOf( 1 /
		// Float.valueOf(coreSS.clientDesc.getClientScale()));
		// }else{
		scale = MobileAgent.DEFAULT_SCALE;
		// }

		if (L.isInWorkshop) {
			LogManager.log("HTMLMlet scale : " + scale);
		}

		final String colorForBodyByHexString = HTMLMlet.getColorForBodyByHexString();
		final SizeHeight sizeHeight = new SizeHeight(coreSS);

		final String startBody =
				// "<!DOCTYPE html>" +//开启会导致：1. 不被视为quirks模式，尽管有box-sizing，
				"<html dir='ltr'>" + "<head>" + "<meta charset=\"utf-8\" />" + // 注意：某些手机不支持width=device-width，必须width=600
						"<meta name=\"viewport\" content=\"target-densitydpi=device-dpi, user-scalable=no, width="
						+ UserThreadResourceUtil.getMletWidthFrom(coreSS) + ", initial-scale=" + scale + ", minimum-scale=" + scale
						+ ", maximum-scale=" + scale + "\"/>" + "<style type=\"text/css\">"
						+ "  html {-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;}\n"
						+ "  *, *:before, *:after {-webkit-box-sizing: inherit;-moz-box-sizing: inherit;box-sizing: inherit;}\n"
						+ "  div > img {vertical-align:middle;}div > input {vertical-align:middle;}div > label{vertical-align:middle;}\n"
						+ "  button > img {vertical-align:middle;}\n" +
						// table will not be inherited in quirks mode, so add
						// table as following, tested pass in letv mobile.
						"  input, select, textarea, table, caption{font-family:inherit;font-size:inherit;font-weight: inherit; font-style: inherit; font-variant: inherit;}\n"
						+ "  button {font-family:inherit;font-weight: inherit; font-style: inherit; font-variant: inherit;font-size:"
						+ sizeHeight.getFontSizeForButton() + "px;}\n" + "  .HC_DIV_SYS_0 {" + "font-size:"
						+ sizeHeight.getFontSizeForNormal() + "px;" + "background-color:#" + colorForBodyByHexString + ";" + "}"
						+ "</style>" + "</head>" + "<body style=\"position:relative;margin:0px;background-color:transparent;color:#"
						+ HTMLMlet.getColorForFontByHexString() + ";\">" + "<div id=\"HC_DIV_0\" class=\"HC_DIV_SYS_0\"></div>"
						+ "<div id=\"HC_DIV_loading\" style=\"position:absolute;background-color:#" + colorForBodyByHexString + ";"
						+ "width:" + UserThreadResourceUtil.getMletWidthFrom(coreSS) + "px;" + "height:"
						+ UserThreadResourceUtil.getMletHeightFrom(coreSS) + "px;\"></div>"
						+ "<div id=\"HC_DIV_tip\" style=\"pointer-events:none;font-size:"
						+ (int) (UserThreadResourceUtil.getMletDPIFrom(coreSS) * 1.4 / 10)
						+ "px;position:absolute;visibility:hidden;height:auto;top:30px;\">" + "</div>";
		final String endBody = "</body>" + "</html>";

		initHTML.append(startBody);
		{
			final String scriptHeader = "<script type='text/javascript'>";
			final String scriptTail = "</script>";
			if (isIOS) {
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
		L.V = L.WShop ? false : LogManager.log(sbStr);
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
				ServerUIAPIAgent.sendOneMovingMsg(coreSS, ResourceUtil.get(coreSS, 9293));//9293=reloading, please wait a moment.
				
				final byte[] bodyBS = (byte[]) paras[0];

				HCURLUtil.sendEClass(coreSS, HCURLUtil.CLASS_BODY_TO_MOBI, bodyBS, 0, bodyBS.length);
			}
		};
		final Object[] paras = { bs };
		bodyCacheComp.encodeGetCompare(coreSS, true, bs, 0, bs.length, paras);
	}

	public static final int getMletComponentCount(final J2SESession coreSS, final ProjectContext ctx, final Mlet mlet) {
		final SessionContext mobileSession = ServerUIAPIAgent.getProjResponserMaybeNull(ctx).getMobileSession(coreSS);
		if (mobileSession != null) {
			final ReturnableRunnable run = new ReturnableRunnable() {
				@Override
				public Object run() throws Throwable {
					return mlet.getComponentCount();
				}
			};
			return (Integer) mobileSession.recycleRes.threadPool.runAndWait(run);
		} else {
			L.V = L.WShop ? false : LogManager.log("no mobile session for J2SESession.");
			return -1;
		}
	}

	public static final Mlet startMlet(final J2SESession coreSS, final ScriptValue listener, final Map<String, String> mapRuby,
			final String targetURL, final String elementID, final String title, final HCJRubyEngine hcje, final ProjectContext context,
			final boolean isSynchronized) {
		final CallContext runCtx = CallContext.getFree();
		runCtx.targetURL = targetURL;

		if (isSynchronized) {
			final Mlet out = pushInMlet(coreSS, runCtx, listener, mapRuby, targetURL, title, hcje, context);
			CallContext.cycle(runCtx);
			return out;
		} else {
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

	public static void sendReceiver(final CoreSession coreSS, final String receiver, final String elementID, final boolean isCancelable) {
		final String[] paras = { HCURL.DATA_PARA_NOTIFY_RECEIVER, HCURL.DATA_PARA_NOTIFY_RECEIVER_PARA,
				HCURL.DATA_PARA_NOTIFY_RECEIVER_CANCEL_PARA };
		final String[] values = { receiver, elementID, IConstant.toString(isCancelable) };
		HCURLUtil.sendCmd(coreSS, HCURL.DATA_CMD_SendPara, paras, values);
	}

	private static Mlet pushInMlet(final J2SESession coreSS, final CallContext runCtx, final ScriptValue listener,
			final Map<String, String> mapRuby, final String targetURL, final String title, final HCJRubyEngine hcje,
			final ProjectContext context) {
		try {
			final String scriptName = title;

			final Object obj = RubyExector.runAndWaitInProjectOrSessionPoolWithRepErr(coreSS, runCtx, listener, scriptName, mapRuby, hcje,
					context, Mlet.class);
			if (obj == null) {
				return null;
			}

			final Mlet mlet = (Mlet) obj;

			ServerUIAPIAgent.openMlet(coreSS, context, mlet, targetURL, false, null);

			return mlet;
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

}
