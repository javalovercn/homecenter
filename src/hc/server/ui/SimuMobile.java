package hc.server.ui;

import hc.core.util.HCURL;
import hc.core.util.LangUtil;
import hc.core.util.RecycleRes;
import hc.core.util.UIUtil;
import hc.server.data.StoreDirManager;
import hc.server.msb.Robot;
import hc.server.msb.SimuRobot;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.JarMainMenu;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.util.HCAudioInputStream;
import hc.server.util.HCFileInputStream;
import hc.server.util.HCImageInputStream;
import hc.server.util.HCInputStreamBuilder;
import hc.server.util.HCLimitSecurityManager;
import hc.util.I18NStoreableHashMapWithModifyFlag;
import hc.util.ResourceUtil;

import java.io.ByteArrayInputStream;

public class SimuMobile {
	public static final J2SESession SIMU_NULL = null;
	
	public static final int MOBILE_WIDTH = 1024;
	public static final int MOBILE_HEIGHT = 768;
	public static final int MOBILE_DPI = 300;

	public static final double MOBILE_LATITUDE = 90.1;
	public static final double MOBILE_LONGITUDE = 60.2;
	public static final double MOBILE_ALTITUDE = 123.4;
	public static final double MOBILE_COURSE = 90.2;
	public static final double MOBILE_SPEED = 12.3;
	public static final boolean MOBILE_GPS = true;
	public static final boolean MOBILE_FRESH = true;
	
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
	public static final String MOBILE_DEFAULT_LOCALE = LangUtil.EN_US;
	public static final String MOBILE_LOCALE = MOBILE_DEFAULT_LOCALE;
	public static final String MOBILE_SOFT_UID = "UID_1234567890";
	public static final boolean MOBILE_CONNECTING = true;
	public static final boolean MOBILE_ON_RELAY = false;
	public static final boolean MOBILE_IN_BACKGROUND = false;
	public static final boolean CURR_IN_SESSION = true;
	public static final String MOBILE_QR_RESULT = "qrcode:1234567890";
	
	public static final HCFileInputStream buildMobileImageFileStream(){
		final String fileExtension = "png";
		final String fileName = "screen.png";
		return buildMobileFileStream(fileExtension, fileName);
	}
	
	public static final HCFileInputStream buildMobileAudioFileStream(){
		final String fileExtension = "3gp";
		final String fileName = "audio.3gp";
		return buildMobileFileStream(fileExtension, fileName);
	}

	private static HCFileInputStream buildMobileFileStream(final String fileExtension,
			final String fileName) {
		final ByteArrayInputStream bis = new ByteArrayInputStream(new byte[0], 0, 0);
		return HCInputStreamBuilder.build(bis, fileExtension, fileName);
	}
	
	public static final HCImageInputStream buildMobileImageStream(){
		return HCInputStreamBuilder.buildImageStream(ResourceUtil.getResourceAsStream("hc/res/ok_22.png"), 0, -90, 0, "png", -1.0, -1.0, -1.0);
	}
	
	public static final HCAudioInputStream buildMobileAudioStream(){
		return HCInputStreamBuilder.buildAudioStream(ResourceUtil.getResourceAsStream("hc/res/helloworld.mp3"), "mp3", -1.0, -1.0, -1.0);
	}
	
	final static RecycleRes tempLimitRecycleRes = HCLimitSecurityManager.getTempLimitRecycleRes();
	
	/**
	 * 先于用户线程初始化，因为Mlet构造内，依赖于相关检查
	 */
	public final static void init(){
	}
	
	public final static boolean checkSimuProjectContext(final ProjectContext ctx){
		return ctx.recycleRes.threadPool == tempLimitRecycleRes.threadPool;
	}
	
	final static ProjectContext simuContext = ServerUIUtil.buildProjectContext("SimuProjContext", "1.0", 
			tempLimitRecycleRes, null, (ProjClassLoaderFinder)null);
	
	public static Robot buildSimuRobot() {
//		LogManager.err("In designer panel, create simu robot for testing script.");//会导致block access Homecenter Non-public api
		return new SimuRobot();
	}

	//要置于createRunTestDir之后
	private static HCJRubyEngine runTestEngine;

	public static synchronized final HCJRubyEngine getRunTestEngine(){
		if(runTestEngine == null){
			SimuMobile.rebuildJRubyEngine();
		}
		return runTestEngine;
	}

	public static synchronized HCJRubyEngine rebuildJRubyEngine() {
		terminateJRubyEngine();
		runTestEngine = new HCJRubyEngine(StoreDirManager.RUN_TEST_DIR.getAbsolutePath(), 
				ResourceUtil.buildProjClassLoader(StoreDirManager.RUN_TEST_DIR, "hc.testDir"), true, HCJRubyEngine.IDE_LEVEL_ENGINE + "TestEngine");
		return runTestEngine;
	}

	public static synchronized void terminateJRubyEngine() {
		if(runTestEngine != null){
			runTestEngine.terminate();
			runTestEngine = null;
		}
	}

	public static ClientSession testSimuClientSession = new ClientSession(null, true);
}
