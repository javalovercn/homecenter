package hc.server.ui.design.code;

import hc.core.L;
import hc.core.util.LogManager;
import hc.util.IBiz;
import hc.util.MultiThreadDownloader;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JPanel;

public class J2SEDocHelper {
	private static final String j2seDoc = "j2sedoc.jar";
	
	private static boolean isBuildIn = checkIsBuildIn();
	private static ClassLoader j2seDocLoader;
	
	private static boolean checkIsBuildIn(){
		final String docPath = DocHelper.buildClassDocPath(JPanel.class.getName().replace('.', '/'));
		final InputStream in = ResourceUtil.getResourceAsStream(docPath);
		return in != null;
	}
	
	public static boolean isBuildIn(){
		return isBuildIn;
	}
	
	public static synchronized void downloadJ2SEDoc(){
		if(j2seDocLoader != null){
			return;
		}
		
		final File docFile = new File(ResourceUtil.getBaseDir(), j2seDoc);
		if(PropertiesManager.getValue(PropertiesManager.p_J2SEDocVersion) == null){
			docFile.delete();
			final String j2seDocVersion = "8";
			final String j2seDocMD5 = "ffc867acdf4b7411fa0d6aa3be10a7df";
			final String downloadURL = "http://homecenter.mobi/download/" + j2seDoc;
			final MultiThreadDownloader mtd = new MultiThreadDownloader();
			final Vector<String> urls = new Vector<String>(1);
			urls.add(downloadURL);
			
			final Boolean[] isDone = {false};
			
			final IBiz succBiz = new IBiz() {
				@Override
				public void start() {
					isDone[0] = true;
					synchronized (isDone) {
						isDone.notify();
					}
					PropertiesManager.setValue(PropertiesManager.p_J2SEDocVersion, j2seDocVersion);
					PropertiesManager.saveFile();
				}
				
				@Override
				public void setMap(final HashMap map) {
				}
			};
			final IBiz failBiz = new IBiz() {
				@Override
				public void start() {
					synchronized (isDone) {
						isDone.notify();
					}
				}
				
				@Override
				public void setMap(final HashMap map) {
				}
			};
			mtd.download(urls, docFile, j2seDocMD5, succBiz, failBiz, true);
			
			synchronized (isDone) {
				try {
					isDone.wait();
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(isDone[0] == false){
				docFile.delete();
				L.V = L.WShop ? false : LogManager.log("fail to download j2sedoc.jar");
				return;
			}
		}
		try{
			final URL[] files = {docFile.toURI().toURL()};
			j2seDocLoader = new URLClassLoader(files);
		}catch (final Throwable e) {
			e.printStackTrace();
		}
	}
	
	public static InputStream getDocStream(final String docPath){
		if(isBuildIn){
			return ResourceUtil.getResourceAsStream(docPath);
		}else{
			return j2seDocLoader.getResourceAsStream(docPath);
		}
	}
}
