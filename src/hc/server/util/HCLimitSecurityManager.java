package hc.server.util;

import hc.App;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.CtrlKey;
import hc.core.util.LogManager;
import hc.core.util.RecycleThread;
import hc.core.util.ThreadPool;
import hc.server.HCSecurityException;
import hc.server.msb.Converter;
import hc.server.msb.Device;
import hc.server.msb.DeviceCompatibleDescription;
import hc.server.msb.Message;
import hc.server.msb.Processor;
import hc.server.msb.Robot;
import hc.server.msb.RobotEvent;
import hc.server.msb.RobotListener;
import hc.server.msb.WiFiAccount;
import hc.server.ui.CtrlResponse;
import hc.server.ui.HCFont;
import hc.server.ui.HTMLMlet;
import hc.server.ui.ICanvas;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.hpj.HCjar;
import hc.util.ClassUtil;
import hc.util.FileLocker;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.AWTPermission;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Locale;
import java.util.PropertyPermission;
import java.util.Vector;

public class HCLimitSecurityManager extends WrapperSecurityManager {
	private final String OUTSIDE_HAR_WORKING_THREAD = " in EventQueue thread, try using ProjectContext.invokeLater and ProjectContext.getUserFile";
	public static final String USER_DATA = "user_data";
	public static final String SYS_THREAD_POOL = "block access system level ThreadPool instance.";
	public static final String SYS_PROPERTIES = "block access properties in PropertiesManager.";
	public static final String HC_FAIL_TO_ACCESS_HOME_CENTER_NON_PUBLIC_API = "block access HomeCenter non-public API.";
	
	private static final SecurityManager oriSecurityManager = System.getSecurityManager();
	private static HCLimitSecurityManager hcSecurityManager;
	private static ThreadPool tempLimitThreadPool;
	private static ThreadGroup tempLimitThreadgroup;
	private final Locale locale = Locale.getDefault();
	private final float jreVersion = App.getJREVer();
	private static final HCEventQueue hcEventQueue = buildHCEventQueue();
	private static final Thread eventDispatchThread = hcEventQueue.eventDispatchThread;
	private static long propertiesLockThreadID = PropertiesManager.PropertiesLockThreadID;
	
	private final static boolean isExistSeurityField = getSecurityField();

	private static boolean getSecurityField(){
		try{
			final Field field = System.class.getDeclaredField("security");
			return field != null;
		}catch (final Throwable e) {
		}
		return false;
	}
	
	private static HCEventQueue buildHCEventQueue(){
		try{
			return new HCEventQueue();
		}catch (final Exception e) {
			App.showOptionDialog(null, "Fail to modify Thread.group to null", "JVM Error");
		}
		return null;
	}
	
	public final String getUserHARDataDir(final ContextSecurityConfig csc) {
		final String tempUserDir = csc.tempUserDir;
		if(tempUserDir == null){
			csc.tempUserDir = getUserHARDataDir(csc.projID);
			return csc.tempUserDir;
		}else{
			return tempUserDir;
		}
	}
	
	/**
	 * 返回格式：user_data/projectID/。含尾的/
	 * @param projID
	 * @return
	 */
	public final String getUserHARDataDir(final String projID) {
		return user_data_dir + HttpUtil.encodeFileName(projID) + System.getProperty("file.separator");
	}
	
	public static final ProjectContext getProjectContextFromDispatchThread(){
		if (Thread.currentThread() == eventDispatchThread){
			final ContextSecurityConfig csc = hcEventQueue.currentConfig;
			if(csc != null){
				return csc.getProjectContext();
			}
		}
		return null;
	}
	
	public static final void switchHCSecurityManager(final boolean on){
		if(ResourceUtil.isAndroidServerPlatform()){
			L.V = L.O ? false : LogManager.log("Stop SecurityManager in Android Server!");
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
	
	public final Thread getEventDispatchThread(){
		return eventDispatchThread;
	}
	
	public final HCEventQueue getHCEventQueue(){
		return hcEventQueue;
	}
	
	public static synchronized final ThreadPool getTempLimitThreadPool(){
		CCoreUtil.checkAccess();
		
		if(tempLimitThreadPool == null){
			tempLimitThreadgroup = new ThreadGroup("tempLimitThreadGroup");
			ClassUtil.changeParentToNull(tempLimitThreadgroup);
			tempLimitThreadPool = new ThreadPool(tempLimitThreadgroup){
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
		}
		
		return tempLimitThreadPool;
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
		    			"splash.png", "starter.jar", "starter.properties", "jruby.jar", "hc.pem", "hc.jar"};
		    	blockWriteFullPathLists = new String[writebats.length];
		    	for (int i = 0; i < writebats.length; i++) {
		    		final String file = writebats[i];
		    		try {
		    			blockWriteFullPathLists[i] = new File(App.getBaseDir(), file).getCanonicalPath();
		    		} catch (final IOException e) {
		    			e.printStackTrace();
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
    	    				e.printStackTrace();
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
	    			Converter.class, Device.class, Message.class, Robot.class, RobotEvent.class, RobotListener.class,
	    			DeviceCompatibleDescription.class,
	    			CtrlResponse.class, Mlet.class, HTMLMlet.class, ICanvas.class,
	    			WiFiAccount.class, SystemEventListener.class, JavaLangSystemAgent.class, CtrlKey.class, HCFont.class};//按API类单列
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
//		    			e.printStackTrace();
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
	
	private final String hcRootPath = FileLocker.toFileCanonicalPath("./") + System.getProperty("file.separator");
	private final String hcRootPathLower = hcRootPath.toLowerCase(locale);
	private final String user_data_dir = hcRootPath + USER_DATA + System.getProperty("file.separator");
	private final String user_data_dirLower = user_data_dir.toLowerCase(locale);
	private final String propertiesName;
	
	public HCLimitSecurityManager(final SecurityManager sm, 
			final String[] blockWrite, final String[] blockMem, final Class[] allowClazz){
		super(sm);
		
		propertiesName = FileLocker.toFileCanonicalPath(PropertiesManager.getPropertiesFileName());
		
		if(propertiesLockThreadID == 0){
			throw new HCSecurityException("unknow propertiesLockThreadID!");
		}
		
		this.blockWriteFullPathLists = blockWrite;
		this.blockMemberAccessLists = blockMem;
		this.memberAccessLists = allowClazz;
	}

	@Override
	public final void checkPermission(final Permission perm) {
		
//		System.out.println("checkPermission : " + perm);
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
			if(perm instanceof SocketPermission){
				if(csc != null){
					final PermissionCollection collection = csc.getSocketPermissionCollection();
					if(collection != null){
						if(collection.implies(perm) == false){
							throw new HCSecurityException("block Socket : " + perm.toString() + " in HAR Project  [" + csc.projID + "]. To enable socket, add it to permission list.");
						}
					}
				}
			}else if(perm instanceof PropertyPermission){
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
				}
				if(actions.indexOf("write") >= 0){
					final String p_key = perm.getName();
					//阻止修改重要系统属性
					if(p_key.equals("file.separator")){
						throw new HCSecurityException("block modify important system property : " + perm.toString());
					}
				}
			}else if(perm instanceof AWTPermission){
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
			}else if(perm instanceof RuntimePermission) {
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
				}
//			}else if(perm instanceof NetPermission){
//				System.out.println("NetPermission : " + perm.toString());
//				final String permissionName = perm.getName();
//				if(permissionName.equals("setProxySelector") || permissionName.equals("setCookieHandler")
//					|| permissionName.equals("specifyStreamHandler")){
//					throw new HCSecurityException("block " + perm.toString());
//				}
//			}else if(perm instanceof ReflectPermission){
//				final String permissionName = perm.getName();
//				if (permissionName.equals("suppressAccessChecks")){
//					if(csc != null && csc.isAccessPrivateField() == false){
//						throw new HCSecurityException("block Field/Method/Constructor setAccessible in HAR Project  [" + csc.projID + "]."
//								+ buildPermissionOnDesc(HCjar.PERMISSION_ACCESS_PRIVATE_FIELD));
//					}
//				}
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
					if(containblockMemberAccessLists || (startWithHC = name.startsWith("hc.", 0))){//|| name.startsWith("sun.", 0)
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
//							}else{
//								throw new HCSecurityException("block memberAccess on Class : " + name + ", package in [sun.]");
							}
						}
					}
				}
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
		final String fileCanonicalPath = FileLocker.toFileCanonicalPath(file);
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}
		String harDir;
		if(csc != null 
				&& (fileCanonicalPath.startsWith((harDir = getUserHARDataDir(csc)), 0)
						|| ((harDir.startsWith(fileCanonicalPath, 0) 
								&& 
							harDir.length() == fileCanonicalPath.length() + 1)))){
		}else{
			if(currentThread.getId() != propertiesLockThreadID){
				if(fileCanonicalPath.equalsIgnoreCase(propertiesName)){
					throw new HCSecurityException("block read file :" + file);
				}
			}
			final String fileCanonicalPathLower = fileCanonicalPath.toLowerCase(locale);
			if(fileCanonicalPathLower.startsWith(user_data_dirLower, 0)){
				//非法读取其它工程
				throw new HCSecurityException("block read file :" + file + OUTSIDE_HAR_WORKING_THREAD);
			}
		}
		
		super.checkRead(file);
	}
	
    @Override
	public final void checkRead(final String file, final Object context) {
		final String fileCanonicalPath = FileLocker.toFileCanonicalPath(file);
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}
		String harDir;
		if(csc != null 
				&& (fileCanonicalPath.startsWith((harDir = getUserHARDataDir(csc)), 0)
						|| ((harDir.startsWith(fileCanonicalPath, 0) 
								&& 
							harDir.length() == fileCanonicalPath.length() + 1)))){
		}else{
			if(currentThread.getId() != propertiesLockThreadID){
				if(fileCanonicalPath.equalsIgnoreCase(propertiesName)){
					throw new HCSecurityException("block read file :" + file);
				}
			}
			final String fileCanonicalPathLower = fileCanonicalPath.toLowerCase(locale);
			if(fileCanonicalPathLower.startsWith(user_data_dirLower, 0)){
				//非法读取其它工程
				throw new HCSecurityException("block read file :" + file + OUTSIDE_HAR_WORKING_THREAD);
			}
		}
		
    	super.checkRead(file, context);
    }
    
	@Override
	public final void checkWrite(final String file) {
		final String fileCanonicalPath = FileLocker.toFileCanonicalPath(file);
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}
		String harDir;
		if(csc != null 
				&& (fileCanonicalPath.startsWith((harDir = getUserHARDataDir(csc)), 0)
						|| ((harDir.startsWith(fileCanonicalPath, 0) 
								&& 
							harDir.length() == fileCanonicalPath.length() + 1)))){
		}else{
			if(csc != null){
				if(csc.isWrite() == false){
					throw new HCSecurityException("block write file :" + file + " in HAR security permission in project [" + csc.projID + "]." 
							+ buildPermissionOnDesc(HCjar.PERMISSION_WRITE));
				}
			}
			
			if(currentThread.getId() != propertiesLockThreadID){
				if(fileCanonicalPath.equalsIgnoreCase(propertiesName)){
					throw new HCSecurityException("block write file :" + file);
				}
			}

			String canonicalLowerPath = null;
			if(csc != null){
				for (int i = 0; i < blockWriteFullPathLists.length; i++) {
					if(blockWriteFullPathLists[i].equalsIgnoreCase(fileCanonicalPath)){
						throw new HCSecurityException("block write file :" + file);
					}
				}

				canonicalLowerPath = fileCanonicalPath.toLowerCase(locale);
				if(canonicalLowerPath.startsWith(user_data_dirLower, 0)){
					//非法读取其它工程
					throw new HCSecurityException("block write file :" + file + OUTSIDE_HAR_WORKING_THREAD);
				}
				{
					final String[] forbidExt = {".jar", ".rb", ".har"};
					for (int i = 0; i < forbidExt.length; i++) {
						if(canonicalLowerPath.endsWith(forbidExt[i])){
							throw new HCSecurityException("block write file :" + file + ", file type [" + forbidExt[i] + "] is forbid.");
						}
					}
				}
				if(canonicalLowerPath.startsWith(hcRootPathLower, 0) == false){
					throw new HCSecurityException("block write file :" + file + ", outside dir:" + hcRootPath);
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
			if(csc.isLoadLib() == false){
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
    	}
    	super.checkExit(status);
    }
    
    @Override
	public final void checkDelete(final String file) {
    	final String fileCanonicalPath = FileLocker.toFileCanonicalPath(file);
		ContextSecurityConfig csc = null;
		final Thread currentThread = Thread.currentThread();
		if ((currentThread == eventDispatchThread && ((csc = hcEventQueue.currentConfig) != null)) || (csc = ContextSecurityManager.getConfig(currentThread.getThreadGroup())) != null){
		}			
		String harDir;
		if(csc != null 
				&& (fileCanonicalPath.startsWith((harDir = getUserHARDataDir(csc)), 0)
						|| ((harDir.startsWith(fileCanonicalPath, 0) 
								&& 
							harDir.length() == fileCanonicalPath.length() + 1)))){
		}else{
			if(csc != null){
				if(csc.isDelete() == false){
					throw new HCSecurityException("block delete file :" + file + " by HAR security permission in project [" + csc.projID + "]." 
							+ buildPermissionOnDesc(HCjar.PERMISSION_DELETE));
				}
			}

			if(csc != null){
				if(fileCanonicalPath.equalsIgnoreCase(propertiesName)){
					throw new HCSecurityException("block delete file :" + file);
				}
				
				for (int i = 0; i < blockWriteFullPathLists.length; i++) {
					if(blockWriteFullPathLists[i].equalsIgnoreCase(fileCanonicalPath)){
						throw new HCSecurityException("block delete file :" + file);
					}
				}
				
				final String canonicalPathLower = fileCanonicalPath.toLowerCase(locale);
				if(canonicalPathLower.startsWith(user_data_dirLower, 0)){
					//非法读取其它工程
					throw new HCSecurityException("block delete file :" + file + OUTSIDE_HAR_WORKING_THREAD);
				}
	
				if(canonicalPathLower.startsWith(hcRootPathLower, 0) == false){
					throw new HCSecurityException("block delete file :" + file + ", outside dir:" + hcRootPath);
				}
	//				if(blockReadFullPathLists.contains(canonicalPath)){
	//					throw new HCSecurityException("block delete file :" + file);
	//				}
			}
		}
    	
    	super.checkDelete(file);
    }

	public static final String buildPermissionOnDesc(final String permssionStr) {
		return " To enable permission, check item [" + permssionStr + "] or reset permissions.";
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
			}
//    		final Iterator<String> it = blockReadFullPathLists.iterator();
//    		while(it.hasNext()){
//    			final String blockFile = it.next();
//    			if(cmd.indexOf(blockFile) >= 0){
//    				throw new HCSecurityException("block exec on file :" + blockFile);
//    			}
//    		}
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

//	public final void addBlockReadFile(String file){
//		try {
//			blockReadFullPathLists.add(new File(file).getCanonicalPath().toLowerCase(locale));
//		} catch (IOException e) {
//			e.printStackTrace();
//			blockReadFullPathLists.add(file);
//		}
//	}
	
}
