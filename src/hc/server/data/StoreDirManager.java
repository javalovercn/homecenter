package hc.server.data;

import hc.server.ui.ProjectContext;
import hc.util.ResourceUtil;

import java.io.File;

public class StoreDirManager {
	public static final String USER_DATA = "user_data";
	public static final String USER_DATA_SAFE = USER_DATA + "_safe";
	public static final String PATH_USER_DATA_OF_OS = USER_DATA + File.separator;//前部不能用File.separator，因为是当前目录

	public static final String HCTMP_EXT = ".hctmp";

	public static final String ICO_DIR = File.separator + "user_ico";
	public static final String HC_RMS = "hc_rms";
	private static final String TEMP_DIR_NAME = "temp";
	public static final String LINK_DIR_NAME = "link";
	
	public final static String HC_SYS_FOR_USER_PRIVATE_DIR = "_HC" + File.separator;//getPrivateFile("mySubDir2/subSubDir").mkdirs();
	public final static String DB_SUB_DIR_FOR_USER_PRIVATE_DIR = "DB" + File.separator;
	public final static String CRON_SUB_DIR_FOR_USER_PRIVATE_DIR = "CRON" + File.separator;
	public final static String TEMP_SUB_DIR_FOR_USER_PRIVATE_DIR = "TEMP" + File.separator;
	
	public final static String PROJ_PROPERTIES = "project.properties";
	public final static String BACKUP_SUB_TEMP = "temp" + File.separator;
	public final static String BACKUP_SUB_STORE = "store" + File.separator;
	
	public static void createDirIfNeccesary(final String dir){
		final File file = new File(ResourceUtil.getBaseDir(), "." + dir);
		if(file.isDirectory()){
		}else{
			file.mkdirs();
		}
	}

	public static final File TEMP_DIR = new File(ResourceUtil.getBaseDir(), TEMP_DIR_NAME);
	public static final File RMS_DIR = new File(ResourceUtil.getBaseDir(), HC_RMS);
	public static final File LINK_DIR = new File(ResourceUtil.getBaseDir(), LINK_DIR_NAME);
	public static final File RUN_TEST_DIR = new File(ResourceUtil.getBaseDir(), "runtest");
	public static final File SAFE_DATA_DIR = new File(ResourceUtil.getBaseDir(), StoreDirManager.USER_DATA_SAFE);
	
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

		if(RMS_DIR.exists() == false){
			RMS_DIR.mkdirs();
		}

		if(SAFE_DATA_DIR.exists() == false){
			SAFE_DATA_DIR.mkdirs();
		}

		if(RUN_TEST_DIR.exists() == false){
			RUN_TEST_DIR.mkdirs();
		}
	}

}
