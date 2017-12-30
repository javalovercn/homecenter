package hc.server.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.CtrlKey;
import hc.core.util.ExceptionReporter;
import hc.core.util.HarHelper;
import hc.core.util.HarInfoForJSON;
import hc.core.util.LogManager;
import hc.core.util.RecycleRes;
import hc.core.util.RecycleThread;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPool;
import hc.res.ImageSrc;
import hc.server.HCSecurityException;
import hc.server.data.StoreDirManager;
import hc.server.msb.AnalysableRobotParameter;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.DeviceCompatibleDescription;
import hc.server.msb.Message;
import hc.server.msb.Processor;
import hc.server.msb.Robot;
import hc.server.msb.RobotEvent;
import hc.server.msb.RobotListener;
import hc.server.msb.RobotWrapper;
import hc.server.msb.SimuRobot;
import hc.server.msb.WiFiAccount;
import hc.server.ui.ClientSession;
import hc.server.ui.CtrlResponse;
import hc.server.ui.Dialog;
import hc.server.ui.DialogHTMLMlet;
import hc.server.ui.DialogMlet;
import hc.server.ui.HTMLMlet;
import hc.server.ui.ICanvas;
import hc.server.ui.MenuItem;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.ProjectInputDialog;
import hc.server.ui.ScriptPanel;
import hc.server.ui.ScriptTester;
import hc.server.ui.design.AddHarHTMLMlet;
import hc.server.ui.design.AddHarIsBusy;
import hc.server.ui.design.BindHTMLMlet;
import hc.server.ui.design.LicenseHTMLMlet;
import hc.server.ui.design.ProjListScriptPanel;
import hc.server.ui.design.ProjMgrDialog;
import hc.server.ui.design.SystemDialog;
import hc.server.ui.design.SystemHTMLMlet;
import hc.server.ui.design.hpj.HCjar;
import hc.server.util.json.JSONArray;
import hc.server.util.json.JSONException;
import hc.server.util.json.JSONML;
import hc.server.util.json.JSONObject;
import hc.server.util.json.JSONPointer;
import hc.server.util.json.JSONPointerBuilder;
import hc.server.util.json.JSONPointerException;
import hc.server.util.json.JSONString;
import hc.server.util.json.JSONTokener;
import hc.server.util.json.JSONXML;
import hc.server.util.json.JSONXMLTokener;
import hc.server.util.scheduler.AnnualJobCalendar;
import hc.server.util.scheduler.BaseJobCalendar;
import hc.server.util.scheduler.CronExcludeJobCalendar;
import hc.server.util.scheduler.DailyJobCalendar;
import hc.server.util.scheduler.HolidayJobCalendar;
import hc.server.util.scheduler.JobCalendar;
import hc.server.util.scheduler.MonthlyJobCalendar;
import hc.server.util.scheduler.WeeklyJobCalendar;
import hc.util.ClassUtil;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.SecurityDataProtector;
import hc.util.SocketDesc;

import java.awt.AWTPermission;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.net.NetPermission;
import java.net.SocketPermission;
import java.net.URLClassLoader;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Vector;
import java.util.logging.LoggingPermission;

import javax.net.ssl.SSLPermission;

import third.quartz.utils.DBConnectionManager;

public class HCLimitSecurityManager extends WrapperSecurityManager implements HarHelper{
	private final String USE_JAVA_NET_PROXY_CLASS = "please use java.net.Proxy class in HAR project.";
	private static final String devCertFileName = SecurityDataProtector.getDevCertFileName();
	private static final String hcHardIdFileName = SecurityDataProtector.getHcHardId();

	private final String OUTSIDE_HAR_WORKING_THREAD = " in EventQueue thread, try using ProjectContext.invokeLater and ProjectContext.getPrivateFile";
	public static final String SYS_THREAD_POOL = "block access system level ThreadPool instance.";
	public static final String SYS_PROPERTIES = "block access properties in PropertiesManager.";
	public static final String HC_FAIL_TO_ACCESS_HOME_CENTER_NON_PUBLIC_API = "block access HomeCenter non-public API.";
	private static final Class QUARTZ_CONN_MGR_CLASS = DBConnectionManager.class;
	private static final SecurityManager oriSecurityManager = System.getSecurityManager();
	private static HCLimitSecurityManager hcSecurityManager;
	private static RecycleRes tempLimitRecycleRes;
	private static ThreadGroup tempLimitThreadgroup;
	private static final Locale locale = Locale.getDefault();
	private final float jreVersion = App.getJREVer();
	private static final HCEventQueue hcEventQueue = buildHCEventQueue();
	private static final Thread eventDispatchThread = hcEventQueue.eventDispatchThread;
	private static long propertiesLockThreadID = PropertiesManager.PropertiesLockThreadID;
	private static final String tempDirCanonicalPath = getTempCanonicalPath();
	final String selfClassName;
	
	private static final String getTempCanonicalPath(){
		try{
			return StoreDirManager.TEMP_DIR.getCanonicalPath() + File.separator;
		}catch (final Exception e) {
		}
		return null;
	}
	
	private final static boolean isExistSeurityField = getSecurityField();
	
	public static boolean isSecurityManagerOn(){
		return ResourceUtil.isJ2SELimitFunction();//System.setSecurityManager in Android will rise java.lang.SecurityException
	}

	private static boolean getSecurityField(){
		try{
			final Field field = System.class.getDeclaredField("security");
			return field != null;
		}catch (final Throwable e) {
		}
		return false;
	}
	
	@Override
	public final String getExceptionReportURL(){
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
			try{
				final String url = (String)csc.projResponser.map.get(HCjar.PROJ_EXCEPTION_REPORT_URL);
				if(url == null || url.length() == 0){//作null处理
					return HarHelper.NO_REPORT_URL_IN_HAR;
				}
				return url;
			}catch (final Throwable e) {
				//开发环境下，可能为null
			}
		}
		return null;
	}
	
	@Override
	public void checkSetFactory() {
		//比如HttpsURLConnection.setDefaultSSLSocketFactory
		
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
			if(csc.isSetFactory() == false){
				throw new HCSecurityException("block SetFactory in HAR Project  [" + csc.projID + "]."
					+ buildPermissionOnDesc(HCjar.PERMISSION_SET_FACTORY));
			}
		}
		super.checkSetFactory();
	}
	
	@Override
	public final HarInfoForJSON getHarInfoForJSON(){
		final HarInfoForJSON harInfo = new HarInfoForJSON();
		
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
			try{
				final Map<String, Object> map = csc.projResponser.map;
				harInfo.projectID = (String)map.get(HCjar.PROJ_ID);
				harInfo.projectVersion = (String)map.get(HCjar.PROJ_VER);
				return harInfo;
			}catch (final Throwable e) {
				//开发环境下，可能为null
			}
		}
		return harInfo;
	}
	
	private static HCEventQueue buildHCEventQueue(){
		try{
			return new HCEventQueue();
		}catch (final Exception e) {
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {//重要，请勿在Event线程中调用，
					App.showOptionDialog(null, "Fail to modify Thread.group to null", "JVM Error");
				}
			});
		}
		return null;
	}
	
	private final String getUserDataBaseDir(final ContextSecurityConfig csc) {
		final String tempUserDir = csc.tempUserDir;
		if(tempUserDir == null){
			csc.tempUserDir = StoreDirManager.getUserDataBaseDir(csc.projID);
			return csc.tempUserDir;
		}else{
			return tempUserDir;
		}
	}
	
	public static final void switchHCSecurityManager(final boolean on){
		if(ResourceUtil.isDemoServer()){
			return;
		}
		
		if(isSecurityManagerOn() == false){
			LogManager.log("stop SecurityManager in current server!");
			return;
		}
		
		CCoreUtil.checkAccess();
		
		if(on){
			ContextSecurityManager.getConfig(Thread.currentThread().getThreadGroup());//init
			System.setSecurityManager(getHCSecurityManager());
		}else{
			System.setSecurityManager(oriSecurityManager);
		}
	}
	
	public final static Thread getEventDispatchThread(){
		CCoreUtil.checkAccess();
		
		return eventDispatchThread;
	}
	
	public final static HCEventQueue getHCEventQueue(){
		CCoreUtil.checkAccess();
		
		return hcEventQueue;
	}
	
	public static synchronized final RecycleRes getTempLimitRecycleRes(){
		CCoreUtil.checkAccess();
		
		if(tempLimitRecycleRes == null){
			tempLimitThreadgroup = new ThreadGroup("tempLimitThreadGroup");
			ClassUtil.changeParentToNull(tempLimitThreadgroup);
			final ThreadPool tempLimitThreadPool = new ThreadPool(tempLimitThreadgroup){
				//每个工程实例下，用一个ThreadPool实例，以方便权限管理
				
				@Override
				protected Thread buildThread(final RecycleThread rt) {
					return new Thread((ThreadGroup)threadGroup, rt);
				}
				
				@Override
				protected void checkAccessPool(final Object token){
				}
			};
			tempLimitThreadPool.setName("tempLimitThreadPool");
			final ContextSecurityConfig csc = new ContextSecurityConfig("");
			csc.buildNewProjectPermissions();
			
			ContextSecurityManager.putContextSecurityConfig(tempLimitThreadgroup, csc);
			
			tempLimitRecycleRes = new RecycleRes("tempLimit", tempLimitThreadPool, RecycleRes.getSequenceTempWatcher());
		}
		
		return tempLimitRecycleRes;
	}
	
	public static synchronized final HCLimitSecurityManager getHCSecurityManager(){
		CCoreUtil.checkAccess();
		
		final Object nullObject = "";
		
		if(hcSecurityManager == null){
			String[] blockWriteFullPathLists;
			String[] blockMemberAccessLists;
			final Class[] memberAccessLists;

	    	{
		    	final String[] writebats = {"HomeCenter.bat", "HomeCenter.sh", "HomeCenter.command",  
		    			"splash.png", "starter.jar", "starter.properties", "jruby.jar", "hc.pem", SafeDataManager.HC_JAR, 
		    			SafeDataManager.HC_THIRDS_JAR, SafeDataManager.STUB_JAR,
		    			hcHardIdFileName, devCertFileName};
		    	blockWriteFullPathLists = new String[writebats.length];
		    	for (int i = 0; i < writebats.length; i++) {
		    		final String file = writebats[i];
		    		try {
		    			blockWriteFullPathLists[i] = new File(ResourceUtil.getBaseDir(), file).getCanonicalPath();
		    		} catch (final IOException e) {
		    			ExceptionReporter.printStackTrace(e);
		    			blockWriteFullPathLists[i] = file;
		    		}
				}
	    	}
	    	
	    	{
    			final Vector<String> blockVect = new Vector<String>();
    			
    			//remove "java.lang.UNIXProcess" from classNames
    			final String SystemClass = "java.lang.System";
				final String[] classNames = {SystemClass, "sun.security.util.SecurityConstants"};//Runtime.thread, "java.awt.EventQueue", 
    			for (int i = 0; i < classNames.length; i++) {
    				try{
    	    			final String processName = classNames[i];
    	    			if(processName.equals(SystemClass) && isExistSeurityField == false){
    	    				continue;
    	    			}
    					final Class clazz = Class.forName(processName);
    					if(clazz != null){
    						final String className = clazz.getName();
							blockVect.add(className);//由于是String==，所以采用getName以增加性能
    					}
    	    		}catch (final Throwable e) {
    	    			//非Android环境报错
    	    			if(ResourceUtil.isJ2SELimitFunction()){
    	    				System.err.println(classNames[i] + " is NOT in some JVM (Not Oracle/Sun JVM).");
    	    				ExceptionReporter.printStackTrace(e);
    	    			}
    				}
				}
	    		blockMemberAccessLists = new String[blockVect.size()];
	    		for (int i = 0; i < blockMemberAccessLists.length; i++) {
					blockMemberAccessLists[i] = blockVect.elementAt(i);
				}
	    	}
	    	
	    	//允许反射且不限的类
	    	final Class[] arrClazz = {ProjectContext.class, Processor.class, 
	    			Converter.class, DialogHTMLMlet.class, DialogMlet.class, Device.class, Message.class, 
	    			Robot.class, RobotWrapper.class, RobotEvent.class, RobotListener.class, SimuRobot.class,
	    			DeviceCompatibleDescription.class, 
	    			AddHarHTMLMlet.class, AddHarIsBusy.class, BindHTMLMlet.class, Dialog.class, SystemDialog.class,
	    			ProjMgrDialog.class, ProjListScriptPanel.class,
	    			LicenseHTMLMlet.class, SystemHTMLMlet.class, //由于需要传递token，会被JRuby反射，所以要开权限。
	    			ClientSession.class, CtrlResponse.class, Mlet.class, MenuItem.class, HTMLMlet.class, ICanvas.class, ProjectInputDialog.class,
	    			Assistant.class, VoiceCommand.class, AnalysableRobotParameter.class, JavaString.class, IDEUtil.class, Scheduler.class, 
	    			HCInputStream.class, HCFileInputStream.class, HCImageInputStream.class, HCAudioInputStream.class, 
	    			JobCalendar.class, BaseJobCalendar.class, AnnualJobCalendar.class, CronExcludeJobCalendar.class, DailyJobCalendar.class, 
	    			HolidayJobCalendar.class, MonthlyJobCalendar.class, WeeklyJobCalendar.class, 
	    			JSONArray.class, JSONException.class, JSONML.class, JSONObject.class, JSONPointer.class, JSONPointerException.class,
	    			JSONString.class, JSONTokener.class, JSONXML.class, JSONXMLTokener.class, JSONPointerBuilder.class, Null.class,
	    			WiFiAccount.class, ScriptPanel.class, ScriptTester.class, SystemEventListener.class, JavaLangSystemAgent.class, CtrlKey.class};//按API类单列
//	    	{
//	    		Vector<Class> allowVect = new Vector<Class>();
//				
//	    		Class[] allowClasses 
//	    		for (int i = 0; i < allowClasses.length; i++) {
//					allowVect.add(allowClasses[i]);
//				}
//	    		
//				String[] classNames = {"sun.awt.image.ImageFetcher"};//按名单列
//				for (int i = 0; i < classNames.length; i++) {
//					try{
//		    			final String processName = classNames[i];
//						Class clazz = Class.forName(processName);
//						if(clazz != null){
//							final String className = clazz.getName();
//							allowVect.add(clazz);//由于是String==，所以采用getName以增加性能
//						}
//		    		}catch (Throwable e) {
//		    			ExceptionReporter.printStackTrace(e);
//					}
//				}
//				
//				arrClazz = new Class[allowVect.size()];
//	    		for (int i = 0; i < blockMemberAccessLists.length; i++) {
//	    			arrClazz[i] = allowVect.elementAt(i);
//				}
//	    	}
	    	
			hcSecurityManager = new HCLimitSecurityManager(oriSecurityManager, 
					blockWriteFullPathLists, blockMemberAccessLists, arrClazz);
		}
		return hcSecurityManager;
	}
	
//	private final HashSet<String> blockReadFullPathLists = new HashSet<String>();
	private final String[] blockWriteFullPathLists;
	private final String[] blockMemberAccessLists;
	private final Class[] memberAccessLists;
	
	private final String propertiesName;
	
	public HCLimitSecurityManager(final SecurityManager sm, 
			final String[] blockWrite, final String[] blockMem, final Class[] allowClazz){
		super(sm);
		
		selfClassName = this.getClass().getName();
		
		final Object obj = ServerUtil.rubyAnd3rdLibsClassLoaderCache;//强制init
		
		propertiesName = StoreDirManager.getCanonicalPath(PropertiesManager.getPropertiesFileName());
		
		if(propertiesLockThreadID == 0){
			throw new HCSecurityException("unknow propertiesLockThreadID!");
		}
		
		this.blockWriteFullPathLists = blockWrite;
		this.blockMemberAccessLists = blockMem;
		this.memberAccessLists = allowClazz;
	}

	boolean allowAccessSystemResource = false;
	
	/**
	 * 暂时开放读取资源文件权限或关闭
	 * @param allow
	 */
	public final void setAllowAccessSystemImageResource(final boolean allow){
		//不需要加checkAccess，因为本实例是安全的。
		allowAccessSystemResource = allow;
	}

	@Override
	public final void checkPermission(final Permission perm) {
		final Class permClass = perm.getClass();
		
//		System.out.println("checkPermission : " + perm);
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
			if(permClass == ReflectPermission.class){
				if(perm.getName().equals("suppressAccessChecks")){//JRuby使用反射
//					if(csc != null && csc.isAccessPrivateField() == false){
//						throw new HCSecurityException("block Field/Method/Constructor setAccessible in HAR Project  [" + csc.projID + "]."
//							+ buildPermissionOnDesc(HCjar.PERMISSION_ACCESS_PRIVATE_FIELD));
//					}
				}else{
					throw new HCSecurityException("block ReflectPermission : " + perm.toString() + " in HAR Project  [" + csc.projID + "].");
				}
			}else if(permClass == SocketPermission.class){
				if(csc != null){
					final PermissionCollection collection = csc.getSocketPermissionCollection();
					if(collection != null){//enable socket limit
						boolean passPrivateCheck = false;
						if(csc.isAccessPrivateAddress()){
//								私有IP地址
//								A：10.0.0.0-10.255.255.255
//								B：172.16.0.0-172.31.255.255，169.254.0.0-169.254.255.255
//								C：192.168.0.0-192.168.255.255
							final SocketPermission sp = (SocketPermission)perm;
							final String ipAndPortAddress = sp.getName();
							final boolean isIPv6 = ipAndPortAddress.startsWith("[");
							if(isIPv6 == false){
								//to get ip6
								final String ip;
								if(isIPv6){
									//ipv6
									ip = ipAndPortAddress.substring(0, ipAndPortAddress.lastIndexOf("]") + 1);
								}else{
									final int lastPortSplitIdx = ipAndPortAddress.lastIndexOf(":");
									ip = (lastPortSplitIdx > 0)?ipAndPortAddress.substring(0, lastPortSplitIdx):ipAndPortAddress;
								}
								
								int firstDotIdx;
								if(ip.equals(SocketDesc.LOCAL_HOST_FOR_SOCK_PERMISSION)){
									passPrivateCheck = true;
								}else if((firstDotIdx = ip.indexOf(".")) > 0){
									if(HttpUtil.isLocalNetworkIP(ip)){
										passPrivateCheck = true;//需要进行后续ipv4的检查
									}else{
										//224.0.0.0 - 239.255.255.255, isMulticastAddress
										final String firstDotPart = ip.substring(0, firstDotIdx);
										try{
											final int intPart = Integer.parseInt(firstDotPart);
											if(intPart >=  224 && intPart <= 239){
												passPrivateCheck = true;
											}
										}catch (final Exception e) {
										}
									}
									
									if(passPrivateCheck){
										final String[] ipv4 = StringUtil.splitToArray(ip, ".");
										if(ipv4.length != 4){
											passPrivateCheck = false;
										}else{
											try{
												for (int i = 0; i < ipv4.length; i++) {
													Integer.parseInt(ipv4[i]);
												}
											}catch (final NumberFormatException e) {
												passPrivateCheck = false;
											}
										}
									}
								}
							}//IPv4
						}
						if(passPrivateCheck == false){
							if(collection.implies(perm) == false){
								throw new HCSecurityException("block Socket : " + perm.toString() + " in HAR Project  [" + csc.projID + "]. To enable socket, add it to permission list.");
							}
						}
					}
				}
			}else if(permClass == PropertyPermission.class){
				final String actions = perm.getActions();
				if(csc != null){
					if(csc.isSysPropRead() == false  && actions.indexOf("read") >= 0){
						throw new HCSecurityException("block PropertyPermission : " + perm.toString() + " in HAR Project  [" + csc.projID + "]."
								+ buildPermissionOnDesc(HCjar.PERMISSION_SYS_PROP_READ));
					}
					if(csc.isSysPropWrite() == false  && actions.indexOf("write") >= 0){
						throw new HCSecurityException("block PropertyPermission : " + perm.toString() + " in HAR Project  [" + csc.projID + "]."
								+ buildPermissionOnDesc(HCjar.PERMISSION_SYS_PROP_WRITE));
					}
				}else{
					if(actions.indexOf("write") >= 0){
						final String p_key = perm.getName();
						//阻止修改重要系统属性
						if(p_key.equals("file.separator")){
							throw new HCSecurityException("block modify important system property : " + perm.toString());
						}else if(p_key.startsWith("http.")){
							if(p_key.equals("http.proxyHost") || p_key.equals("http.proxyPort") || p_key.equals("http.nonProxyHosts")){
								checkHCStackTraceInclude(null, null, USE_JAVA_NET_PROXY_CLASS);
							}
						}else if(p_key.startsWith("ftp.")){
							if(p_key.equals("ftp.proxyHost") || p_key.equals("ftp.proxyPort") || p_key.equals("ftp.nonProxyHosts")){
								checkHCStackTraceInclude(null, null, USE_JAVA_NET_PROXY_CLASS);
							}
						}else if(p_key.startsWith("https.")){
							if(p_key.equals("https.proxyHost") || p_key.equals("https.proxyPort")){
								checkHCStackTrace();
							}
						}else if(p_key.startsWith("socksProxy")){
							if(p_key.equals("socksProxyHost") || p_key.equals("socksProxyPort")){//there is no dot ('.') after the prefix this time
								checkHCStackTraceInclude(null, null, USE_JAVA_NET_PROXY_CLASS);
							}
						}
					}
				}
			}else if(permClass == AWTPermission.class){
				final String permName = perm.getName();
				if(permName.equals("createRobot")){
					if(csc != null && csc.isRobot() == false){
						throw new HCSecurityException("block createRobot in HAR Project  [" + csc.projID + "]."
								+ buildPermissionOnDesc(HCjar.PERMISSION_ROBOT));
					}
//				}else if(permName.equals("listenToAllAWTEvents")){
//					if(csc != null && csc.isListenAllAWTEvents() == false){
//						throw new HCSecurityException("block addAWTEventListener/removeAWTEventListener in HAR Project  [" + csc.projID + "]."
//								+ buildPermissionOnDesc(HCjar.PERMISSION_LISTEN_ALL_AWT_EVNTS));
//					}
				}else if(permName.equals("accessEventQueue")){
					if(csc != null){
						throw new HCSecurityException("block java.awt.Toolkit.getSystemEventQueue in HAR Project  [" + csc.projID + "].");
					}
//				}else if(permName.equals("accessClipboard")){
//					if(csc != null && csc.isAccessClipboard() == false){
//						throw new HCSecurityException("block accessClipboard in HAR Project  [" + csc.projID + "]."
//								+ buildPermissionOnDesc(HCjar.PERMISSION_ACCESS_CLIPBOARD));
//					}
				}else if(permName.equals("readDisplayPixels")){
					if(csc != null){// && csc.isReadDisplayPixels() == false
						throw new HCSecurityException("block readDisplayPixels on java.awt.Graphics2d.setComposite in HAR Project  [" + csc.projID + "].");
					}
				}else if(permName.equals("showWindowWithoutWarningBanner")){//no config
//					if (limitRootThreadGroup.parentOf(currentThreadGroup)){
					throw new HCSecurityException("block showWindowWithoutWarningBanner");
//					}
				}
			}else if(permClass == RuntimePermission.class) {
				final String permissionName = perm.getName();
				if (permissionName.equals("setSecurityManager")){
					throw new HCSecurityException("block setSecurityManager.");
				}else if(permissionName.equals("shutdownHooks")){
					if(csc != null && csc.isShutdownHooks() == false){
						throw new HCSecurityException("block java.lang.Runtime.addShutdownHook/removeShutdownHook in HAR Project  [" + csc.projID + "]."
								+ buildPermissionOnDesc(HCjar.PERMISSION_SHUTDOWNHOOKS));
					}
				}else if(permissionName.equals("setIO")){
					if(csc != null && csc.isSetIO() == false){
						throw new HCSecurityException("block java.lang.System.setIn/setOut/setErr in HAR Project  [" + csc.projID + "]."
								+ buildPermissionOnDesc(HCjar.PERMISSION_SETIO));
					}
				}else if(permissionName.equals("getClassLoader") 
						|| permissionName.equals("getProtectionDomain")
						|| permissionName.equals("createClassLoader")
						|| permissionName.startsWith("accessClassInPackage")//accessClassInPackage.sun.reflect, accessClassInPackage.sun.misc
						){//JRuby正常反射需要
				}else if(permissionName.equals("setDefaultUncaughtExceptionHandler")){
					throw new HCSecurityException("block RuntimePermission [setDefaultUncaughtExceptionHandler] in HAR Project  [" + csc.projID + "].");
//				}else if(permissionName.equals("getFileSystemAttributes")){//block getBaseDir().getTotalSpace() for getPrivateHardwareCode
//					throw new HCSecurityException("block RuntimePermission [getFileSystemAttributes] in HAR Project  [" + csc.projID + "], you may need ProjectContext.getFreeSpace(File).");
				}else{
//					if(csc != null){//阻止其它RuntimePermission，比如setDefaultUncaughtExceptionHandler
//						throw new HCSecurityException("block RuntimePermission [" + permissionName + "] in HAR Project  [" + csc.projID + "].");
//					}
				}
			}else if(permClass == NetPermission.class){
//				System.out.println("NetPermission : " + perm.toString());
				final String permissionName = perm.getName();
				//new NetPermission("getNetworkInformation") to getMacAddress in getPrivateHardwareCode
				if(permissionName.equals("getNetworkInformation")){//禁止csc访问getMacAddress
					throw new HCSecurityException("block NetPermission [" + permissionName + "] in HAR Project  [" + csc.projID + "].");
				}
//				if(permissionName.equals("setProxySelector") || permissionName.equals("setCookieHandler")
//					|| permissionName.equals("specifyStreamHandler")){
//					throw new HCSecurityException("block " + perm.toString());
//				}
			}else if(permClass == LoggingPermission.class){
				final String permissionName = perm.getName();
				if(permissionName.equals("control")){//JRuby正常反射需要
				}else{
//					throw new HCSecurityException("block LoggingPermission [" + permissionName + "] in HAR Project  [" + csc.projID + "].");
				}
			}else if(permClass == SSLPermission.class){
				final String setHostVerifier = "setHostnameVerifier";
				if(perm.getName().equals(setHostVerifier)){
					//HttpsURLConnection.setDefaultHostnameVerifier new SSLPermission("setHostnameVerifier")
					throw new HCSecurityException("block SSLPermission [" + setHostVerifier + "] in HAR Project  [" + csc.projID + "].");
				}
			}else{
//				if(csc != null){//阻止其它Permission，
//					throw new HCSecurityException("block " + perm.getClass().getSimpleName() + " [" + perm.getName() + "] in HAR Project  [" + csc.projID + "].");
//				}
			}
		}else{
			//No in csc
			if(permClass == NetPermission.class){
//				System.out.println("NetPermission : " + perm.toString());
				final String permissionName = perm.getName();
				//new NetPermission("getNetworkInformation") to getMacAddress in getPrivateHardwareCode
				if(permissionName.equals("getNetworkInformation")){//禁止非法类访问getMacAddress
					//有可能为J2SEPlatformService，hc.util.HttpUtil.getServerInetAddress
					checkHCStackTrace();//不能class.getName，因为Android环境没有
				}
			}else if(permClass == RuntimePermission.class) {
				final String permissionName = perm.getName();
				if (permissionName.equals("setSecurityManager")){
					checkHCStackTraceInclude(HCLimitSecurityManager.class.getName(), null);
//				}else if(permissionName.equals("getFileSystemAttributes")){//block getBaseDir().getTotalSpace() for getPrivateHardwareCode
//					checkHCStackTrace();
				}
			}else if(permClass == SocketPermission.class){
				checkHCStackTrace();
			}
		}
		
		super.checkPermission(perm);
	}

	@Override
	public final void checkMemberAccess(final Class<?> clazz, final int which){
		if(which == Method.DECLARED){
			ContextSecurityConfig csc = null;
			final Thread currentThread = Thread.currentThread();
			if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
				if(clazz == System.class){
					LogManager.warning("memberAccess(reflection) on Class [java.lang.System] in JRuby script, it is recommended to use Class [" + JavaLangSystemAgent.class.getName() + "].");
				}else if(clazz == URLClassLoader.class){
					//由于GET_CLASSLOADER_PERMISSION = new RuntimePermission("getClassLoader");
					//防止URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
					throw new HCSecurityException("block checkMemberAccess [URLClassLoader.class] in HAR Project  [" + csc.projID + "].");
				}
				
				boolean containmemberAccessLists = false;
				for (int i = 0; i < memberAccessLists.length; i++) {
					if(memberAccessLists[i] == clazz){
						containmemberAccessLists = true;
						break;
					}
				}
				if(containmemberAccessLists == false){
					final String name = clazz.getName();
		
					boolean containblockMemberAccessLists = false;
					for (int i = 0; i < blockMemberAccessLists.length; i++) {
						if(blockMemberAccessLists[i].equals(name)){
							containblockMemberAccessLists = true;
							break;
						}
					}
					boolean startWithHC = false;
					if(containblockMemberAccessLists 
							|| clazz == QUARTZ_CONN_MGR_CLASS
							|| (startWithHC = name.startsWith("hc.", 0))){
//									&& ( ! (name.startsWith("hc.hsqldb.", 0))))){
						if(containblockMemberAccessLists){
							if(clazz == System.class){
								if(jreVersion < 1.7 && csc.isMemberAccessSystem() == false){
									throw new HCSecurityException("block memberAccess(reflection) on Class [" + name + "] in JRuby, please use methods in [" + JavaLangSystemAgent.class.getName() + "].");
								}
							}else{
								throw new HCSecurityException("block memberAccess(reflection) on Class [" + name + "] in JRuby, please create agent/wrap class for it.");
							}
						}else{
							if(startWithHC){
								throw new HCSecurityException("block memberAccess on Class [" + name + "] in package [hc.]");
							}else{
								throw new HCSecurityException("block memberAccess on Class : " + name);//QUARTZ_CONN_MGR_CLASS
							}
						}
					}
				}
			}else{
				//Not in csc
				if(clazz == SecurityManager.class || clazz == IConstant.class || clazz == CUtil.class
						|| clazz == HCLimitSecurityManager.class || clazz == SecurityDataProtector.class
						|| clazz == URLClassLoader.class || clazz == BCProvider.class){//禁止反射操作的类
					ResourceUtil.checkHCStackTrace();
				}
				
				//HCJRubyEngine.parse时，会出现如下：
				//at : Users.homecenter.Documents.eclipse_workspace.homecenter.$_dot_.jruby_dot_jar.jruby.java.java_ext.__file__(file:/Users/homecenter/Documents/eclipse_workspace/homecenter/./jruby.jar!/jruby/java/java_ext.rb:3)
				//at : Users.homecenter.Documents.eclipse_workspace.homecenter.$_dot_.jruby_dot_jar.jruby.java.java_ext.load(file:/Users/homecenter/Documents/eclipse_workspace/homecenter/./jruby.jar!/jruby/java/java_ext.rb)
				//故关闭下行
//				ResourceUtil.checkHCStackTraceInclude(null, jrubyClassLoad);//不能用系统ResourceUtil.class.Loader，因为JRuby引擎初始化时，会触发此处。
			}
			//白名单方式
//			final String name = clazz.getName();
//			if(blockMemberAccessLists.contains(name)){
//				final Thread currentThread = Thread.currentThread();
//				if(currentThread == eventQueueThread || limitGroup.parentOf(currentThread.getThreadGroup())){
//					throw new HCSecurityException("fail to memberAccess on : " + name);
//				}
//			}
		}
//		}
//		System.out.println("checkMemberAccess : " + clazz.getName() + ", which :" + which);
		super.checkMemberAccess(clazz, which);
	}
	
	@Override
	public final void checkAccess(final Thread t){
		if(t == eventDispatchThread){
			throw new HCSecurityException("block modify EventDispatchThread");
		}
//		super.checkAccess(t);
	}
	
	@Override
	public final void checkAccess(final ThreadGroup g) {
//		super.checkAccess(g);
	}
	
	@Override
	public final void checkRead(final String file) {
		checkReadImpl(file);
		
		this.checkRead(file, null);
	}
	
    @Override
	public final void checkRead(final String file, final Object context) {
		checkReadImpl(file);
		
    	super.checkRead(file, context);
    }
    
    final boolean isWindowPlatform = ResourceUtil.isWindowsOS();
    
	private final void checkReadImpl(final String file) {
		final String fileCanonicalPath = toFileCanonicalPathForCheck(file);
		if(allowAccessSystemResource){
			if(fileCanonicalPath.endsWith(".png")){
				if(isWindowPlatform && 
						(
								(fileCanonicalPath.indexOf(ImageSrc.HC_RES_PATH_FOR_WIN) > 0) 
								|| 
								(fileCanonicalPath.indexOf(ImageSrc.HC_SERVER_UI_DESIGN_RES_FOR_WIN) > 0)
						)){
					return;
				}else if((fileCanonicalPath.indexOf(ImageSrc.HC_RES_PATH) > 0) 
						|| 
						(fileCanonicalPath.indexOf(ImageSrc.HC_SERVER_UI_DESIGN_RES) > 0)){
					return;
				}
			}
		}
		
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}
		String harDir;
		if(csc == null){
			//checkHCStackTraceInclude会导致运算加重
			if(fileCanonicalPath.startsWith(StoreDirManager.TEMP_CANONICAL_PATH, 0)){
			}else if(fileCanonicalPath.endsWith(hcHardIdFileName) || fileCanonicalPath.endsWith(devCertFileName)){
				checkHCStackTrace();
			}
		}else{
			if(csc != null 
					&& (fileCanonicalPath.startsWith(StoreDirManager.RUN_TEST_CANONICAL_PATH, 0)
							|| fileCanonicalPath.startsWith((harDir = getUserDataBaseDir(csc)), 0)
							|| ((harDir.startsWith(fileCanonicalPath, 0) 
									&& 
								harDir.length() == fileCanonicalPath.length() + 1)))){
			}else{
				if(currentThread.getId() != propertiesLockThreadID){
					if(fileCanonicalPath.equalsIgnoreCase(propertiesName)){
						throw new HCSecurityException("block read file :" + file);
					}
				}
				if(csc != null){
					if(fileCanonicalPath.startsWith(tempDirCanonicalPath, 0) == false){//非temp目录
						final String fileCanonicalPathLower = fileCanonicalPath.toLowerCase(locale);
						if(fileCanonicalPathLower.startsWith(StoreDirManager.user_data_dirLower, 0) || fileCanonicalPathLower.startsWith(StoreDirManager.user_data_safe_dirLower, 0)){
							//非法读取其它工程
							throw new HCSecurityException("block read file :" + file + OUTSIDE_HAR_WORKING_THREAD);
						}
					}
				}
			}
		}
	}
	
	private final void checkHCStackTrace(){
		ResourceUtil.checkHCStackTraceInclude(null, ServerUtil.rubyAnd3rdLibsClassLoaderCache, null, selfClassName);//ResourceUtil.getJRubyClassLoader(false)
	}
	
	private final void checkHCStackTraceInclude(final String callerClass, final ClassLoader loader) {
		ResourceUtil.checkHCStackTraceInclude(callerClass, loader==null?ServerUtil.rubyAnd3rdLibsClassLoaderCache:loader, null, selfClassName);
	}
	
	private final void checkHCStackTraceInclude(final String callerClass, final ClassLoader loader, final String moreMsg) {
		ResourceUtil.checkHCStackTraceInclude(callerClass, loader==null?ServerUtil.rubyAnd3rdLibsClassLoaderCache:loader, moreMsg, selfClassName);
	}
	
	@Override
	public final void checkWrite(final String file) {
		final String fileCanonicalPath = toFileCanonicalPathForCheck(file);
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}
		String harDir;
		if(csc == null){
//			System.out.println("==> check write : " + file.toString());
			if(fileCanonicalPath.startsWith(StoreDirManager.TEMP_CANONICAL_PATH, 0)){
			}else if(currentThread.getId() == propertiesLockThreadID && fileCanonicalPath.equalsIgnoreCase(propertiesName)){
			}else{
				checkHCStackTrace();
			}
		}else{
			if(csc != null 
					&& (fileCanonicalPath.startsWith(StoreDirManager.RUN_TEST_CANONICAL_PATH, 0)
							|| fileCanonicalPath.startsWith((harDir = getUserDataBaseDir(csc)), 0)
							|| ((harDir.startsWith(fileCanonicalPath, 0) 
									&& 
								harDir.length() == fileCanonicalPath.length() + 1)))){
			}else{
				if(currentThread.getId() != propertiesLockThreadID){
					if(fileCanonicalPath.equalsIgnoreCase(propertiesName)){
						throw new HCSecurityException("block write file :" + file);
					}
				}
	
				if(csc != null){
					if(fileCanonicalPath.startsWith(tempDirCanonicalPath, 0) == false){//非temp目录
						for (int i = 0; i < blockWriteFullPathLists.length; i++) {
							if(blockWriteFullPathLists[i].equalsIgnoreCase(fileCanonicalPath)){
								throw new HCSecurityException("block write file :" + file);
							}
						}
		
						final String fileCanonicalPathLower = fileCanonicalPath.toLowerCase(locale);
						if(fileCanonicalPathLower.startsWith(StoreDirManager.user_data_dirLower, 0) || fileCanonicalPathLower.startsWith(StoreDirManager.user_data_safe_dirLower, 0)){
							//非法读取其它工程
							throw new HCSecurityException("block write file :" + file + OUTSIDE_HAR_WORKING_THREAD);
						}

//						{
//							final String[] forbidExt = {".jar", ".rb", ".har"};
//							for (int i = 0; i < forbidExt.length; i++) {
//								if(canonicalLowerPath.endsWith(forbidExt[i])){
//									throw new HCSecurityException("block write file :" + file + ", file type [" + forbidExt[i] + "] is forbid.");
//								}
//							}
//						}
//						if(canonicalLowerPath.startsWith(hcRootPathLower, 0) == false){
//							isTryWrite = true;
////							throw new HCSecurityException("block write file :" + file + ", outside dir:" + hcRootPath);
//						}
						
						if(csc.isWrite() == false){
							throw new HCSecurityException("block write file :" + file + " in HAR security permission in project [" + csc.projID + "]." 
									+ buildPermissionOnDesc(HCjar.PERMISSION_WRITE));
						}
					}
				}
			}
		}
		
		super.checkWrite(file);
	}
	
	@Override
	public void checkLink(final String lib) {
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}
		if(csc != null){
			final boolean isJFFIStubLoader = false;
			//com.kenai.jffi.internal.StubLoader.loadFromJar(StubLoader.java:367)
//			final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
//			final int size = stack.length;
//			for (int i = 0; i < size; i++) {
//				final StackTraceElement element = stack[i];
//				if(element.getClassName().equals("com.kenai.jffi.internal.StubLoader")){
//					LogManager.log("ignore checkLink for Class [com.kenai.jffi.internal.StubLoader].");
//					isJFFIStubLoader = true;
//					break;
//				}
//			}
			if(isJFFIStubLoader ==  false && csc.isLoadLib() == false){
				throw new HCSecurityException("block java.lang.Runtime.load(lib) in HAR project  [" + csc.projID + "]."
						+ buildPermissionOnDesc(HCjar.PERMISSION_LOAD_LIB));
			}
		}
		
		super.checkLink(lib);
    }
	
    @Override
	public final void checkExit(final int status) {
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}
		if(csc != null){
			if(csc.isExit() == false){
				throw new HCSecurityException("block execute [exit] by HAR security permission in project [" + csc.projID + "]." 
						+ buildPermissionOnDesc(HCjar.PERMISSION_EXIT));
			}
    	}else{
    		//Not in csc
    		checkHCStackTrace();
    	}
		
    	super.checkExit(status);
    }
    
    @Override
	public final void checkDelete(final String file) {
    	final String fileCanonicalPath = toFileCanonicalPathForCheck(file);
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}			
		String harDir;
		if(csc != null 
				&& (fileCanonicalPath.startsWith(StoreDirManager.RUN_TEST_CANONICAL_PATH, 0)
						|| fileCanonicalPath.startsWith((harDir = getUserDataBaseDir(csc)), 0)
						|| ((harDir.startsWith(fileCanonicalPath, 0) 
								&& 
							harDir.length() == fileCanonicalPath.length() + 1)))){
		}else{
			if(csc == null){
				if(fileCanonicalPath.startsWith(StoreDirManager.TEMP_CANONICAL_PATH, 0)){
				}else{
					checkHCStackTrace();
				}
			}else{
				if(fileCanonicalPath.equalsIgnoreCase(propertiesName)){
					throw new HCSecurityException("block delete file :" + file);
				}
				
				if(fileCanonicalPath.startsWith(tempDirCanonicalPath, 0) == false){//非temp目录
					for (int i = 0; i < blockWriteFullPathLists.length; i++) {
						if(blockWriteFullPathLists[i].equalsIgnoreCase(fileCanonicalPath)){
							throw new HCSecurityException("block delete file :" + file);
						}
					}
	
					final String fileCanonicalPathLower = fileCanonicalPath.toLowerCase(locale);
					if(fileCanonicalPathLower.startsWith(StoreDirManager.user_data_dirLower, 0) || fileCanonicalPathLower.startsWith(StoreDirManager.user_data_safe_dirLower, 0)){
						//非法读取其它工程
						throw new HCSecurityException("block delete file :" + file + OUTSIDE_HAR_WORKING_THREAD);
					}

					if(csc.isDelete() == false){
						throw new HCSecurityException("block delete file :" + file + " in HAR security permission in project [" + csc.projID + "]." 
								+ buildPermissionOnDesc(HCjar.PERMISSION_DELETE));
					}
				}
			}
		}
    	
    	super.checkDelete(file);
    }

	public static final String buildPermissionOnDesc(final String permssionStr) {
		return " To enable permission, select item [" + permssionStr + "] or reset permissions.";
	}
    
    @Override
	public final void checkExec(final String cmd) {
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}
		if(csc != null){
			if(csc.isExecute() == false){
				throw new HCSecurityException("block execute [" + cmd + "] in HAR security permission in project [" + csc.projID + "]." 
						+ buildPermissionOnDesc(HCjar.PERMISSION_EXECUTE));
			}else{
				//工程开启了Execute权限
				
				if(cmd.indexOf(hcHardIdFileName, 0) >= 0){
					throw new HCSecurityException("block execute [" + cmd + "] in project [" + csc.projID + "] for security data [" + hcHardIdFileName + "].");
//				}else if(cmd.indexOf(PATH_USER_DATA_OF_OS, 0) >= 0){
//					throw new HCSecurityException("block execute [" + cmd + "] in project [" + csc.projID + "] for security data [" + PATH_USER_DATA_OF_OS + "].");
				}
			}
//    		final Iterator<String> it = blockReadFullPathLists.iterator();
//    		while(it.hasNext()){
//    			final String blockFile = it.next();
//    			if(cmd.indexOf(blockFile) >= 0){
//    				throw new HCSecurityException("block exec on file :" + blockFile);
//    			}
//    		}
			LogManager.log("execute OS cmd : [" + cmd + "] in project [" + csc.projID + "].");
    	}else{
    		//Not in csc
//    		if(fileCanonicalPath.startsWith(StoreDirManager.TEMP_CANONICAL_PATH, 0)){
//			}else{
				checkHCStackTrace();
//			}
    	}
    	
    	super.checkExec(cmd);
    }
    
    @Override
	public final void checkSecurityAccess(final String target) {
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
    		throw new HCSecurityException("block checkSecurityAccess :" + target);
    	}
    	
    	super.checkSecurityAccess(target);
    }
    
    @Override
    public ThreadGroup getThreadGroup() {
//    	if(Thread.currentThread() == eventDispatchThread){
//    		ContextSecurityConfig csc = hcEventQueue.currentConfig;
//			if(csc != null){
//    			return csc.threadGroup;//projResponser.threadPool.getThreadGroup();
//    		}else{
//    			return App.getThreadPoolToken();
//    		}
//    	}
    	return super.getThreadGroup();
    }

	private final String toFileCanonicalPathForCheck(final String fileName) {
		try{
			//注意：返回window/linux分隔符不同
			return new File(fileName).getCanonicalPath();//注意：不getBaseDir
		}catch (final Exception e) {
			return fileName;
		}
	}

//	public final void addBlockReadFile(String file){
//		try {
//			blockReadFullPathLists.add(new File(file).getCanonicalPath().toLowerCase(locale));
//		} catch (IOException e) {
//			ExceptionReporter.printStackTrace(e);
//			blockReadFullPathLists.add(file);
//		}
//	}
	
}
