package hc.server.data;

import hc.App;

import java.io.File;

public class StoreDirManager {
	public static final String HCTMP_EXT = ".hctmp";

	public static final String ICO_DIR = App.FILE_SEPARATOR + "user_ico";
	public static final String HC_RMS = "hc_rms";
	public static final String TEMP_DIR_NAME = "temp";
	public static final String LINK_DIR_NAME = "link";
	
	public static void createDirIfNeccesary(final String dir){
		final File file = new File(App.getBaseDir(), "." + dir);
		if(file.isDirectory()){
		}else{
			file.mkdirs();
		}
	}

	public static final File TEMP_DIR = new File(App.getBaseDir(), TEMP_DIR_NAME);
	public static final File RMS_DIR = new File(App.getBaseDir(), HC_RMS);
	public static final File LINK_DIR = new File(App.getBaseDir(), LINK_DIR_NAME);
	public static final File RUN_TEST_DIR = new File(App.getBaseDir(), "runtest");

	static {
		if(TEMP_DIR.exists() == false){
			TEMP_DIR.mkdirs();
		}
		
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
