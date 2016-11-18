package hc.server.ui;

import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.ThreadPool;
import hc.core.util.UIUtil;
import hc.server.data.StoreDirManager;
import hc.server.msb.DeviceCompatibleDescription;
import hc.server.msb.Message;
import hc.server.msb.Robot;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.JarMainMenu;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.util.HCLimitSecurityManager;
import hc.util.I18NStoreableHashMapWithModifyFlag;
import hc.util.ResourceUtil;

public class SimuMobile {
	public static final J2SESession SIMU_NULL = null;
	
	public static final int MOBILE_WIDTH = 1024;
	public static final int MOBILE_HEIGHT = 768;
	public static final int MOBILE_DPI = 300;
	
	public static final int PROJ_LEVEL_MOBILE_WIDTH = 0;
	public static final int PROJ_LEVEL_MOBILE_HEIGHT = 0;
	public static final int PROJ_LEVEL_MOBILE_DPI = -1;
	public static final String PROJ_LEVEL_MOBILE_OS_VER = "0.0.1";
	public static final String PROJ_LEVEL_MOBILE_OS = "";
	public static final String PROJ_LEVEL_MOBILE_LOCALE = "";
	
	public static final boolean MOBILE_SUCC_INSERT = true;
	public static final boolean MOBILE_SUCC_REMOVED = true;
	public static final int MOBILE_MENU_ITEMS_SIZE = 100;//太小可能导致getMenuItem出现异常
	public static final MenuItem MOBILE_MENU_ITEM = new MenuItem("SIMU_ITEM", JarMainMenu.FOLD_TYPE, UIUtil.SYS_FOLDER_ICON, 
			HCURL.URL_CMD_EXIT, new I18NStoreableHashMapWithModifyFlag(), "", "");
	public static final String MOBILE_OS_VER = "2.0";
	public static final String MOBILE_OS = ProjectContext.OS_ANDROID;
	public static final String MOBILE_DEFAULT_LOCALE = "en-US";
	public static final String MOBILE_LOCALE = MOBILE_DEFAULT_LOCALE;
	public static final String MOBILE_SOFT_UID = "UID_1234567890";
	public static final boolean MOBILE_CONNECTING = true;
	public static final boolean MOBILE_ON_RELAY = false;
	public static final boolean MOBILE_IN_BACKGROUND = false;
	public static final boolean CURR_IN_SESSION = true;
	
	final static ThreadPool tempLimitThreadPool = HCLimitSecurityManager.getTempLimitThreadPool();
	
	public final static boolean checkSimuProjectContext(final ProjectContext ctx){
		return ctx.projectPool == tempLimitThreadPool;
	}
	
	final static ProjectContext simuContext = ServerUIUtil.buildProjectContext("SimuProjContext", "1.0", 
			tempLimitThreadPool, null, (ProjClassLoaderFinder)null);
	
	public static Robot buildSimuRobot() {
		LogManager.err("In designer panel, create simu robot for testing script.");
		return new Robot() {
			@Override
			public void startup() {
			}
			
			@Override
			public void shutdown() {
			}
			
			@Override
			public void response(final Message msg) {
			}
			
			@Override
			public Object operate(final long functionID, final Object parameter) {
				LogManager.err("In designer panel, create simu result object (empty string) for method [operate] of simu Robot.");
				return "";
			}
			
			@Override
			public DeviceCompatibleDescription getDeviceCompatibleDescription(
					final String referenceDeviceID) {
				return null;
			}
			
			@Override
			public String[] declareReferenceDeviceID() {
				return null;
			}
		};
	}

	//要置于createRunTestDir之后
	private static HCJRubyEngine runTestEngine;

	public static synchronized final HCJRubyEngine getRunTestEngine(){
		if(runTestEngine == null){
			SimuMobile.rebuildJRubyEngine();
		}
		return runTestEngine;
	}

	public static synchronized void rebuildJRubyEngine() {
		terminateJRubyEngine();
		runTestEngine = new HCJRubyEngine(StoreDirManager.RUN_TEST_DIR.getAbsolutePath(), ResourceUtil.buildProjClassLoader(StoreDirManager.RUN_TEST_DIR, "hc.testDir"), true);
	}

	public static synchronized void terminateJRubyEngine() {
		if(runTestEngine != null){
			runTestEngine.terminate();
			runTestEngine = null;
		}
	}

	public static ClientSession testSimuClientSession = new ClientSession();
}
