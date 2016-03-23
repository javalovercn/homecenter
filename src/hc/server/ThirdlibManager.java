package hc.server;

import hc.App;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
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
//	private final static Vector<String> loadedClassPath = new Vector<String>();
	
//	public static void loadClassPath(){
//		Vector<String> v = getClassPath();
//		for (int i = 0; i < v.size(); i++) {
//			String lib = v.elementAt(i);
//			if(loadedClassPath.contains(lib)){
//				continue;
//			}
//			try {
//				loadedClassPath.add(lib);
//				ResourceUtil.loadJar(new File(App.getBaseDir(), lib));
////				L.V = L.O ? false : LogManager.log("load classpath : " + lib);
//			} catch (Exception e) {
//				ExceptionReporter.printStackTrace(e);
//			}
//		}
//	}
	
	public static PropertiesSet libsSet = new PropertiesSet(PropertiesManager.S_THIRD_DIR);
	static {
		final File lib_dir = new File(App.getBaseDir(), PropertiesManager.S_THIRD_DIR);
		if(lib_dir.exists()){
		}else{
			lib_dir.mkdirs();
		}
	}

	public static void refill(final Object[] libNames){
		CCoreUtil.checkAccess();
		
		libsSet.refill(libNames);
		libsSet.save();
	}
	
	public static void loadThirdLibs() {
//		ThirdlibManager.loadClassPath();
		final int size = libsSet.size();
		final File[] libFiles = new File[size];
		for (int i = 0; i < size; i++) {
			final String item = libsSet.getItem(i);
			libFiles[i] = new File(App.getBaseDir(), item);
		}
		PlatformManager.getService().get3rdClassLoader(libFiles);
	}
	
	public static File buildTarget(final String libNameWithoutJarExt){
		return new File(App.getBaseDir(), buildPath(libNameWithoutJarExt));
	}

	/**
	 * 注意：与removePath对应
	 * @param libNameWithoutJarExt
	 * @return
	 */
	public static String buildPath(final String libNameWithoutJarExt) {
		return "." + "/" + PropertiesManager.S_THIRD_DIR + "/" + libNameWithoutJarExt + EXT_JAR;
	}
	
	/**
	 * 注意：与buildPath对应
	 * @param pathName
	 * @param keepExtJar
	 * @return
	 */
	public static String removePath(final String pathName, final boolean keepExtJar){
		final String name = new File(App.getBaseDir(), pathName).getName();
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
	 * @param libNameWithoutJarExt
	 * @return
	 */
	public static File addLib(final File sourceLib, final String libNameWithoutJarExt){
		CCoreUtil.checkAccess();
		
		final File targetFile = buildTarget(libNameWithoutJarExt);
		final String buildPath = buildPath(libNameWithoutJarExt);
		if(libsSet.contains(buildPath)){
			return targetFile;
		}
		
		libsSet.appendItem(buildPath);
		libsSet.save();

		copy(sourceLib, targetFile);
		
//		try {
//			ResourceUtil.loadJar(targetFile);
//		} catch (Exception e1) {
//			e1ExceptionReporter.printStackTrace(e);
//		}
		
		return targetFile;
	}
	
	/**
	 * 
	 * @param libName
	 * @param withExtJar true:表示libName中含有.jar
	 */
	public static void removeLib(String libName, final boolean withExtJar){
		CCoreUtil.checkAccess();
		
		if(withExtJar){
			libName = removeExtJar(libName);
		}
		
		final File remove = buildTarget(libName);
		remove.delete();
		
		final String path = buildPath(libName);

		if(remove.exists()){
			L.V = L.O ? false : LogManager.log("jar lib [" + libName + "] may be in using. Operation of delete will be done at next startup!");
			PropertiesManager.addDelDir(path);
		}
		if(ResourceUtil.isAndroidServerPlatform()){
			final File dexFile = new File(path + ResourceUtil.EXT_DEX_JAR);
			if(dexFile.exists()){
				PropertiesManager.addDelDir(path + ResourceUtil.EXT_DEX_JAR);
			}
		}
		libsSet.delItem(path);
		libsSet.save();
	}

	public static boolean copy(final File from, final File to) {
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("copy file : " + from.getAbsolutePath() + ", to : " + to.getAbsolutePath());
		}
		FileInputStream in = null;
		FileOutputStream out = null;
		try{
			in = new FileInputStream(from);
			out = new FileOutputStream(to);
			final byte[] buffer = new byte[1024 * 5];
			int ins = 0;
			while ((ins = in.read(buffer)) != -1) {
				out.write(buffer, 0, ins);
			}
			return true;
		}catch (final Exception e) {
			return false;
		}finally{
			try{
				in.close();
			}catch (final Exception e) {
				
			}
			try{
				out.flush();
			}catch (final Exception e) {
				
			}
			try{
				out.close();
			}catch (final Exception e) {
				
			}
		}
	}

	public static String createLibName(final JFrame self, final File file) {
		final String fileName = file.getName();
		String libName = fileName.substring(0, fileName.lastIndexOf("."));
		while(buildTarget(libName).exists()){
			libName = App.showInputDialog(self, "name is used/exists, please input new lib name (without extension name)", "Same lib name!!!", JOptionPane.QUESTION_MESSAGE);
			if(libName == null){
				return null;
			}
		}
		return libName;
	}

	public static Vector<String> getClassPath(){
		final Vector<String> v = new Vector<String>();
		final String cp = System.getenv("classpath");
		if(cp == null){
			return v;
		}
		final String[] out = cp.split(System.getProperty("path.separator"));
		
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
