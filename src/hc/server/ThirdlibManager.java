package hc.server;

import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ThirdlibManager {
	public final static String EXT_JAR = ".jar";
	private final static Vector<String> loadedClassPath = new Vector<String>();
	
	public static void loadClassPath(){
		Vector<String> v = getClassPath();
		for (int i = 0; i < v.size(); i++) {
			String lib = v.elementAt(i);
			if(loadedClassPath.contains(lib)){
				continue;
			}
			try {
				loadedClassPath.add(lib);
				ResourceUtil.loadJar(new File(lib));
//				L.V = L.O ? false : LogManager.log("load classpath : " + lib);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static PropertiesSet libsSet = new PropertiesSet(PropertiesManager.S_THIRD_DIR);
	static {
		File lib_dir = new File(PropertiesManager.S_THIRD_DIR);
		if(lib_dir.exists()){
		}else{
			lib_dir.mkdir();
		}
	}

	public static void refill(Object[] libNames){
		libsSet.refill(libNames);
		libsSet.save();
	}
	
	public static void loadThirdLibs() {
		ThirdlibManager.loadClassPath();
		
		int size = libsSet.size();
		for (int i = 0; i < size; i++) {
			try {
				final String item = libsSet.getItem(i);
				ResourceUtil.loadJar(new File(item));
//				L.V = L.O ? false : LogManager.log("Load user lib : " + item);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static File buildTarget(String libName){
		return new File(buildPath(libName));
	}

	/**
	 * 注意：与removePath对应
	 * @param libName
	 * @return
	 */
	public static String buildPath(String libName) {
		return "." + "/" + PropertiesManager.S_THIRD_DIR + "/" + libName + EXT_JAR;
	}
	
	/**
	 * 注意：与buildPath对应
	 * @param pathName
	 * @param keepExtJar
	 * @return
	 */
	public static String removePath(String pathName, boolean keepExtJar){
		final String name = new File(pathName).getName();
		if(keepExtJar){
			return name;
		}else{
			return removeExtJar(name);
		}
	}

	private static String removeExtJar(final String name) {
		return name.substring(0, name.length() - EXT_JAR.length());
	}
	
	/**
	 * 加入工程时，执行jar库复制，动态加载
	 * @param sourceLib
	 * @param libName
	 * @return
	 */
	public static File addLib(File sourceLib, String libName){
		final File targetFile = buildTarget(libName);
		final String buildPath = buildPath(libName);
		if(libsSet.contains(buildPath)){
			return targetFile;
		}
		
		libsSet.appendItem(buildPath);
		libsSet.save();

		copy(sourceLib, targetFile);
		
		try {
			ResourceUtil.loadJar(targetFile);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		return targetFile;
	}
	
	public static void removeLib(String libName, boolean keepExtJar){
		if(keepExtJar){
			libName = removeExtJar(libName);
		}
		
		File remove = buildTarget(libName);
		remove.delete();
		
		final String path = buildPath(libName);

		if(remove.exists()){
			PropertiesManager.addDelDir(path);
		}
		
		libsSet.delItem(path);
		libsSet.save();
	}

	public static boolean copy(File from, File to) {
		FileInputStream in = null;
		FileOutputStream out = null;
		try{
			in = new FileInputStream(from);
			out = new FileOutputStream(to);
			byte[] buffer = new byte[1024 * 5];
			int ins = 0;
			while ((ins = in.read(buffer)) != -1) {
				out.write(buffer, 0, ins);
			}
			return true;
		}catch (Exception e) {
			return false;
		}finally{
			try{
				in.close();
			}catch (Exception e) {
				
			}
			try{
				out.flush();
			}catch (Exception e) {
				
			}
			try{
				out.close();
			}catch (Exception e) {
				
			}
		}
	}

	public static String createLibName(final JFrame self, File file) {
		String fileName = file.getName();
		String libName = fileName.substring(0, fileName.indexOf("."));
		while(buildTarget(libName).exists()){
			libName = JOptionPane.showInputDialog(self, "Lib name is exists, please input new lib name", "Same lib name!!!", JOptionPane.QUESTION_MESSAGE);
			if(libName == null){
				return null;
			}
		}
		return libName;
	}

	public static Vector<String> getClassPath(){
		Vector<String> v = new Vector<String>();
		String cp = System.getenv("classpath");
		if(cp == null){
			return v;
		}
		String[] out = cp.split(System.getProperty("path.separator"));
		
		for (int i = 0; i < out.length; i++) {
			final String element = out[i];
			if(v.contains(element)){
				continue;
			}
			v.add(element);
		}
		return v;
	}
}
