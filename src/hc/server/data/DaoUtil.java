package hc.server.data;

import java.io.File;

public class DaoUtil {
	public static void createUserDir(String dirName){
		File file = new File(dirName);
		file.mkdirs();
	}
}
