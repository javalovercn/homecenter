package hc.server;

import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.util.HCVerify;
import hc.util.HttpUtil;
import hc.util.ResourceUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class StarterManager {//注意：本类的getHCVersion被starter.jar反射调用
	public static final String CLASSNAME_STARTER_STARTER = "starter.Starter";
	public static final String METHOD_GETVER = "getVer";
	public static final String _STARTER_PROP_FILE_NAME = "starter.properties";
	public static final String STR_STARTER = "starter.jar";
	public static final String STR_STARTER_TMP_UP = "starterTmpUp.jar";
	private static String currStartVer = "0";
	
	public static void setCurrStarterVer(final String currVer){
		currStartVer = currVer;
	}
	
	private static String getNewStarterVersion(){
		return "7.5";
	}
	
	public static boolean hadUpgradeError = false;
	
	public static void startUpgradeStarter(){
		if(ResourceUtil.isAndroidServerPlatform()){
			return;
		}
		
		//检查是否存在starter.jar
		final File starterFile = new File(ResourceUtil.getBaseDir(), STR_STARTER);
		if(starterFile.exists() == false){
			//Source code run mode or develop mode, skip download and upgrade starter.jar
			return;
		}
		if(StringUtil.higher(getNewStarterVersion(), currStartVer)){
			if(3 > 2){//Windows下文件被锁，无法升级
				return;
			}
			
			new Thread(){
				@Override
				public void run(){
					try{
						if(starterFile.setWritable(true, true)==false){
							throw new Exception("no permission to modify file " + STR_STARTER + ", fail upgrade.");
						}
						Thread.sleep(20 * 1000);
						
//						LogManager.log("try set write permission to file " + STR_STARTER);

						LogManager.log("find new ver starter, try downloading...");
						
						final File starterTmp = new File(ResourceUtil.getBaseDir(), STR_STARTER_TMP_UP);
						
						if(HttpUtil.download(starterTmp, new URL("http://homecenter.mobi/download/starter.jar"), 3, null)){
							//检查签名
							if(HCVerify.verifyJar(STR_STARTER_TMP_UP, HCVerify.getCert()) == false){
								throw new Exception("fail verify new version starter.jar, maybe there is problem on net.");
							}
							
							LogManager.log("pass verify file " + STR_STARTER);
							//检查新版本
							{
								final URL url = starterTmp.toURI().toURL();  
								LogManager.log("new starter url:" + url.toString());
								final URL[] urls = {url};  
								final ClassLoader loader = new URLClassLoader(urls, null); //parent必须为null，否则会加载旧文件 
								final Class myClass = loader.loadClass(CLASSNAME_STARTER_STARTER);  
								final Method m = myClass.getDeclaredMethod(METHOD_GETVER, new Class[] { });
								final String testVer = (String)m.invoke(null, new Object[]{});
								if(testVer.equals(getNewStarterVersion())){
									LogManager.log("pass the right new version:" + getNewStarterVersion());
									
									//考虑多用户使用及升级情形，所以允许全部writable
									starterTmp.setWritable(true, false);	
									
									final Object globalLock = CCoreUtil.getGlobalLock();
									synchronized (globalLock) {
										starterFile.delete();
										if(starterFile.exists()){
											if(starterTmp.renameTo(starterFile)){
												LogManager.log("successful finish download and upgrade file " + STR_STARTER);
												return;
											}
											throw new Exception("fail to del old version " + STR_STARTER);
										}
										
										if(starterTmp.renameTo(starterFile) == false){
											throw new Exception("fail to mv " + STR_STARTER_TMP_UP + " to " + STR_STARTER);
										}
										
										LogManager.log("successful finish download and upgrade file " + STR_STARTER);
										return ;	
									}
								}else{
									throw new Exception("fail check on the new file " + STR_STARTER + " ver:" + testVer + ", expected ver:" + getNewStarterVersion());
								}
							}
						}
					}catch (final Throwable e) {
						LogManager.errToLog("fail upgrade file " + STR_STARTER + ", exception : " + e.toString());
						ExceptionReporter.printStackTrace(e);
					}
					hadUpgradeError = true;
					// 刷新主菜单，增加手工升级starter
					ResourceUtil.buildMenu();
				}
			}.start();
		}
	}
	
	public static String getHCVersion(){//注意：本方法被starter.jar反射调用
		//从6.96(含)开始，源代码中内置版本信息，而无需从starter中获得
		//注意：如果AgreeLicense发生变化，请同时更改App.getAgreeVersion()
		
		//服务器对客户端最低版本要求，在J2SEContext.minMobiVerRequiredByServer
		
		//客户端对服务器最低版本要求，在J2MEContext.miniHCServerVer
		
		return "7.70";//请同步修改go.php, android.php
	}

}
