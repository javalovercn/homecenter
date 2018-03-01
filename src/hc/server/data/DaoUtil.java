package hc.server.data;

import hc.util.ResourceUtil;

import java.io.File;

public class DaoUtil {
	public static void createUserDir(String dirName) {
		File file = new File(ResourceUtil.getBaseDir(), dirName);
		file.mkdirs();
	}
}
