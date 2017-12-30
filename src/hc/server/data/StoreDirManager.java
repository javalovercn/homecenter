package hc.server.data;

import hc.core.util.ExceptionReporter;
import hc.server.ui.ProjectContext;
import hc.server.util.SafeDataManager;
import hc.util.HttpUtil;
import hc.util.ResourceUtil;

import java.io.File;
import java.util.Locale;

public class StoreDirManager {
	public static final String HCTMP_EXT = ".hctmp";

	public static final String ICO_DIR = File.separator + "user_ico";
	public static final String HC_RMS = "hc_rms";
	private static final String TEMP_DIR_NAME = "temp";
	public static final String LINK_DIR_NAME = "link";
	static final String CFG_DIR_NAME = "cfg";
	
	public final static String HC_SYS_FOR_USER_PRIVATE_DIR = "_HC" + File.separator;//getPrivateFile("mySubDir2/subSubDir").mkdirs();
	public final static String DB_SUB_DIR_FOR_USER_PRIVATE_DIR = "DB" + File.separator;
	public final static String CRON_SUB_DIR_FOR_USER_PRIVATE_DIR = "CRON" + File.separator;
	public final static String TEMP_SUB_DIR_FOR_USER_PRIVATE_DIR = "TEMP" + File.separator;
	
	public final static String PROJ_PROPERTIES = "project.properties";
	public static void createDirIfNeccesary(final String dir){
		final File file = new File(ResourceUtil.getBaseDir(), "." + dir);
		if(file.isDirectory()){
		}else{
			file.mkdirs();
		}
	}
	
	public static final Locale locale = Locale.getDefault();
	private static final String hcRootPath = getCanonicalPath("./") + File.separator;
	private static final String user_data_dir = hcRootPath + SafeDataManager.USER_DATA + File.separator;
	private static final String user_data_safe_dir = hcRootPath + SafeDataManager.USER_DATA_SAFE + File.separator;
	public static final String user_data_dirLower = user_data_dir.toLowerCase(locale);
	public static final String user_data_safe_dirLower = user_data_safe_dir.toLowerCase(locale);

	/**
	 * 返回格式：user_data/projectID/。含尾的/
	 * @param projID
	 * @return
	 */
	public static final String getUserDataBaseDir(final String projID) {
		return user_data_dir + HttpUtil.encodeFileName(projID) + File.separator;
	}
	
	public static String getCanonicalPath(final String fileName) {
		try{
			return new File(ResourceUtil.getBaseDir(), fileName).getCanonicalPath();
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return fileName;
	}
	
	public static final File TEMP_DIR = new File(ResourceUtil.getBaseDir(), TEMP_DIR_NAME);
	public static final File RMS_DIR = new File(ResourceUtil.getBaseDir(), HC_RMS);
	public static final File LINK_DIR = new File(ResourceUtil.getBaseDir(), LINK_DIR_NAME);
	public static final File CFG_DIR = new File(ResourceUtil.getBaseDir(), CFG_DIR_NAME);
	public static final File RUN_TEST_DIR = new File(ResourceUtil.getBaseDir(), "runtest");
	public static final String RUN_TEST_ABS_PATH = RUN_TEST_DIR.getAbsolutePath();
	public static final String RUN_TEST_CANONICAL_PATH = buildCanonicalPath(RUN_TEST_DIR);
	public static final String TEMP_CANONICAL_PATH = buildCanonicalPath(TEMP_DIR);
	
	private static String buildCanonicalPath(final File file) {
		try{
			return file.getCanonicalPath() + File.separator;
		}catch (final Exception e) {
		}
		return null;
	}
	
	public final static File getTmpSubForUserManagedByHcSys(final ProjectContext projectContext){
		return projectContext.getPrivateFile(HC_SYS_FOR_USER_PRIVATE_DIR + TEMP_SUB_DIR_FOR_USER_PRIVATE_DIR);
	}

	static {
		if(TEMP_DIR.exists() == false){
			TEMP_DIR.mkdirs();
		}
		ResourceUtil.clearDir(TEMP_DIR);
		
		if(LINK_DIR.exists() == false){
			LINK_DIR.mkdirs();
		}
		
		if(CFG_DIR.exists() == false){
			CFG_DIR.mkdirs();
		}

		if(RMS_DIR.exists() == false){
			RMS_DIR.mkdirs();
		}

		if(RUN_TEST_DIR.exists() == false){
			RUN_TEST_DIR.mkdirs();
		}
	}

}
