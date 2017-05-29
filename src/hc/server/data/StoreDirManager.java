package hc.server.data;

import hc.App;
import hc.util.ResourceUtil;

import java.io.File;

public class StoreDirManager {
	public static final String HCTMP_EXT = ".hctmp";

	public static final String ICO_DIR = App.FILE_SEPARATOR + "user_ico";
	public static final String HC_RMS = "hc_rms";
	private static final String TEMP_DIR_NAME = "temp";
	public static final String LINK_DIR_NAME = "link";
	
	public final static String HC_SYS_FOR_USER_PRIVATE_DIR = "_HC/";//getPrivateFile("mySubDir2/subSubDir").mkdirs();
	public final static String DB_SUB_DIR_FOR_USER_PRIVATE_DIR = "DB/";
	public final static String CRON_SUB_DIR_FOR_USER_PRIVATE_DIR = "CRON/";
	
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

		if(RUN_TEST_DIR.exists() == false){
			RUN_TEST_DIR.mkdirs();
		}
	}

}
