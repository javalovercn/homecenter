package hc.server.data;

import hc.App;

import java.io.File;

public class DaoUtil {
	public static void createUserDir(String dirName){
		File file = new File(App.getBaseDir(), dirName);
		file.mkdirs();
	}
}
