package hc.server.data;

import java.io.File;

public class StoreDirManager {
	public static final String ICO_DIR = "/user_ico";
	
	public static void createDirIfNeccesary(String dir){
		File file = new File("." + dir);
		if(file.isDirectory()){
		}else{
			file.mkdir();
		}
	}
}
