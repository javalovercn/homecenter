package hc.server.ui.design.hpj;

import java.util.HashMap;
import java.util.Map;

public class MyProjectStru {
	/**
	 * 
	 * @param baseDir
	 *            以/结尾的路径目录。如./或./src/
	 */
	public MyProjectStru(String baseDir) {
		this.baseDir = baseDir;
	}

	private final Map<String, String> mapAttributes = new HashMap<String, String>();
	private final String baseDir;

	public void setAttribute(String key, String value) {
		mapAttributes.put(key, value);
	}

	public Map<String, String> getMapAttributes() {
		return mapAttributes;
	}

	public static void log(String msg) {
		System.out.println(msg);
	}

	public String getBaseDir() {
		return baseDir;
	}
}
