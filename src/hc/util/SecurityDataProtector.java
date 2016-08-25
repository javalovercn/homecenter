package hc.util;

import hc.App;
import hc.core.IConstant;
import hc.core.L;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.HCActionListener;
import hc.server.ui.design.engine.HCJRubyEngine;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class SecurityDataProtector {
	public static final String DEV_CERT_FILE_EXT = "pfx";
	final static String fileDevCert = "dev_cert." + DEV_CERT_FILE_EXT;
	final static String fileHcHardId = "hc_hard_id.txt";
	
	public static String getDevCertFileName(){
		return fileDevCert;
	}
	
	public static File getDevCertFile(){
		return new File(ResourceUtil.getBaseDir(), SecurityDataProtector.getDevCertFileName());
	}
	
	public static String getHcHardId(){
		return fileHcHardId;
	}
	
	private static boolean runOnce = false;
	private static final boolean isHCServer = IConstant.isHCServer();
	
	public static boolean isEnableSecurityData(){
		if(ResourceUtil.isAndroidServerPlatform()){
			return PropertiesManager.getValue(PropertiesManager.p_jrubyJarFile) != null;//依赖于JRuby进行数据加密
		}else{
			return true;
		}
	}
	
	public static void init(final boolean isSimu){
		final Object gLock = CCoreUtil.getGlobalLock();
		
		if(isEnableSecurityData() == false){
			return;
		}
		
		if(runOnce == true){
			return;
		}else{
			runOnce = true;
		}
		
		if(isHCServer == false){
			return;
		}
		
		CCoreUtil.checkAccess();
		
		if(ResourceUtil.isAndroidServerPlatform()){
			initForAndroid();
		}
		
		synchronized (gLock) {
		final String serverKeyMD5 = getServerKeyMD5();
		final String realMD5 = ResourceUtil.getMD5(new String(getServerKey()));
		if(serverKeyMD5 == null){
			PropertiesManager.setValue(PropertiesManager.p_ServerSecurityKeyMD5, realMD5);
			PropertiesManager.encodeSecurityDataFromTextMode();
			L.V = L.O ? false : LogManager.log("encode security data from text mode!");
		}else{
			if(serverKeyMD5.equals(realMD5) == false){
				final String uuid = PropertiesManager.getValue(PropertiesManager.p_uuid);
				PropertiesManager.notifyErrorOnSecurityProperties();
				
				new Thread(){
					@Override
					public void run(){
						try{
							Thread.sleep(20 * 1000);
						}catch (final Exception e) {
						}

						//服务器也变化，比如直接拷贝到其它机器上
						LogManager.errToLog("Hardware information (network interface, disk...) is required by HomeCenter!" +
								"\nIf they are changed, it will clause security data failure!");
						
						final JPanel panel = new JPanel(new BorderLayout());
						panel.add(
								new JLabel("<html>" + (String)ResourceUtil.get(9208) + "</html>", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), 
								BorderLayout.CENTER);
						final ActionListener listener = new HCActionListener(new Runnable() {
							@Override
							public void run() {
								PropertiesManager.setValue(PropertiesManager.p_ServerSecurityKeyMD5, realMD5);
								ResourceUtil.generateCertForNullOrError();//需要系统初始化完成，所以不能置入notifyErrorOnSecurityProperties
								
								App.showInputPWDDialog(uuid, "", "", false);

								SIPManager.startRelineonForce(false);
							}
						}, App.getThreadPoolToken());
						
						App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getErrorI18N(), false, null, null, listener, listener, null, true, false, null, false, true);
//						App.showConfirmDialog(null, 
//								"<html>" + (String)ResourceUtil.get(9208) + "</html>", ResourceUtil.getErrorI18N(), JOptionPane.CLOSED_OPTION, 
//								JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
					}
				}.start();
			}else{
				PropertiesManager.encodeSecurityDataFromTextMode();//可能后期扩展
			}
		}
		}//end gLock
	}
	
//	public static void main(final String[] args){
//		final String source = "Hello World!";
//		System.out.println(decode(encode(source)));
//	}
	
	static String encode(final String value){
		if(isHCServer == false){
			return value;
		}
		
		//Android环境下，没有下载JRuby前，是不加密状态的。
		if(PropertiesManager.getValue(PropertiesManager.p_ServerSecurityKeyMD5) == null){
			return value;
		}
		
		final byte[] privateBS = getServerKey();
		
		try{
			final KeyGenerator kgen = KeyGenerator.getInstance("AES");  
			final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG" );  
	        secureRandom.setSeed(privateBS);
	        kgen.init(128, secureRandom);  
	  
	        final Cipher cipher = Cipher.getInstance("AES/CBC/ISO10126Padding");  
	        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"), buildIV(privateBS));  
	          
	        final byte[] doFinal = cipher.doFinal(ByteUtil.getBytes(value, IConstant.UTF_8));
			final String out = ByteUtil.toHex(doFinal);
			return out;
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		
		return value;
	}

	static String decode(final String data){
		if(isHCServer == false){
			return data;
		}
		
		//Android环境下，没有下载JRuby前
		if(PropertiesManager.getValue(PropertiesManager.p_ServerSecurityKeyMD5) == null){
			return data;
		}
		
		final byte[] privateBS = getServerKey();
		final byte[] src = ByteUtil.toBytesFromHexStr(data);
		
		try{
			final KeyGenerator kgen = KeyGenerator.getInstance("AES");  
			final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG" );  
	        secureRandom.setSeed(privateBS);
	        kgen.init(128, secureRandom);  
	          
	        final Cipher cipher = Cipher.getInstance("AES/CBC/ISO10126Padding");  
	        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"), buildIV(privateBS));  
	        final byte[] decryptBytes = cipher.doFinal(src);  
	          
	        return new String(decryptBytes, IConstant.UTF_8);
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		
		return data;
	}

	private static IvParameterSpec buildIV(final byte[] privateBS) {
		final int ivLen = 16;
		
		if(privateBS.length >= ivLen){
			return new IvParameterSpec(privateBS, 0, ivLen);
		}else{
			final byte[] newIV = new byte[ivLen];
			int startIdx = 0;
			while(startIdx < ivLen){
				final int leftMax = ivLen - startIdx;
				System.arraycopy(privateBS, 0, newIV, startIdx, leftMax<privateBS.length?leftMax:privateBS.length);
				startIdx += privateBS.length;
			}
			return new IvParameterSpec(newIV);
		}
	}

	private static String getServerKeyMD5() {
		return PropertiesManager.getValue(PropertiesManager.p_ServerSecurityKeyMD5);
	}
	
	private static final byte[] getServerKey(){
		if(ResourceUtil.isAndroidServerPlatform()){
			return getPrivateHardwareCodeForAndroid();
		}else{
			return getPrivateHardwareCodeForJ2SE();
		}
	}
	
	static byte[] hc_hard_id_bs;
	
	private static byte[] getPrivateHardwareCodeForJ2SE() {
		ResourceUtil.checkHCStackTraceInclude(null, null);//由于缓存于field中，所以加此
		
		//注意：由于本类入HCLimitSecurityManager.checkMemberAccess，所以保护属性即可
		if(hc_hard_id_bs == null){
			long hc_hard_id_file_len = 0;
			int readNum = 0;

			final File hardIDFile = new File(ResourceUtil.getBaseDir(), fileHcHardId);//注意：此文件名HCLimitSecurityManager.checkReadImpl方法内csc == null时检查
			
			if(hardIDFile.exists() == false){
			}else{
				hc_hard_id_file_len = hardIDFile.length();
				
			    InputStream ios = null;
			    try {
			        ios = new FileInputStream(hardIDFile);
			        hc_hard_id_bs = new byte[(int)hc_hard_id_file_len];
			        readNum = ios.read(hc_hard_id_bs);
			    }catch (final Throwable e) {
			    }finally {
			        try {
		                ios.close();
			        } catch (final Throwable e) {
			        }
			    }
			}
			
			//文件出错，或新建
			if(readNum == 0 || readNum != hc_hard_id_file_len){
				hc_hard_id_bs = ResourceUtil.buildUUID().getBytes();
				
				FileOutputStream fos = null;
				try{
					fos = new FileOutputStream(hardIDFile);
					fos.write(hc_hard_id_bs);
					fos.flush();
				}catch (final Exception e) {
					e.printStackTrace();
				}finally{
					try{
						fos.close();
					}catch (final Exception e) {
					}
				}
			}
		}
		
		return hc_hard_id_bs;
		
//		try {
//			final Enumeration<NetworkInterface> el = NetworkInterface.getNetworkInterfaces();
//			while (el.hasMoreElements()) {
//				final byte[] mac = el.nextElement().getHardwareAddress();
//		        if (mac == null)
//		        	continue;
//
//		        return mac;
//	        }
//		}catch (final Exception exception) {
//			exception.printStackTrace();
//		}
		
//		final long baseDirTotalSpace = PlatformManager.getService().getBaseDir().getTotalSpace();
//
//		try{
//			final StringBuilder sb = new StringBuilder();
//			
//			final Class fileStoreClass = Class.forName("java.nio.file.FileStore");
//			final Class fileSystemsClass = Class.forName("java.nio.file.FileSystems");
//			final Class fileSystemClass = Class.forName("java.nio.file.FileSystem");
//			final Object defaultFileSystem = ClassUtil.invokeWithExceptionOut(fileSystemsClass, fileSystemsClass, "getDefault", ClassUtil.nullParaTypes, ClassUtil.nullParas, false);
//			final Iterable it = (Iterable)ClassUtil.invokeWithExceptionOut(fileSystemClass, defaultFileSystem, "getFileStores", ClassUtil.nullParaTypes, ClassUtil.nullParas, false);
//			
//			for (final Object store: it) {
//				final long total = (Long)ClassUtil.invokeWithExceptionOut(fileStoreClass, store, "getTotalSpace", ClassUtil.nullParaTypes, ClassUtil.nullParas, false);
//		        if(total == baseDirTotalSpace){
//		        	sb.append(store.toString());
//		        	sb.append(total);
//		        }
//		    }
//			
//			if(sb.length() > 0){
////				System.out.println("getPrivateHardwareCodeForJ2SE : " + sb.toString());
//				return ResourceUtil.getMD5(sb.toString()).getBytes();
//			}
//		}catch (final Throwable e) {
//		}
//		
//		return String.valueOf(baseDirTotalSpace).getBytes();
	}
	
	private static Object androidHardwareObject;
	
	private static void initForAndroid(){
		
		final StringBuilder values = new StringBuilder();
                                                                                                                           
		final String HC = "HomeCenter";
		try {
			{
				final Class<?> c = Class.forName("android.os.Build");
				final Field get = c.getField("SERIAL");
				values.append((String)get.get(c));   
				get.setAccessible(true);
				get.set(c, HC);
			}
		}catch (final Exception ignored){                              
		}
		
//		07-30 06:18:37.370: I/HomeCenter(924): os.version ==> X.X-TTXX
//		07-30 06:18:37.420: I/HomeCenter(924): android.zlib.version ==> X.X.X
//		07-30 06:18:37.430: I/HomeCenter(924): android.openssl.version ==> OpenSSL X.Xe XX Feb XXXX
//		07-30 06:18:37.390: I/HomeCenter(924): android.icu.library.version ==> XX.X
//		07-30 06:18:37.370: I/HomeCenter(924): android.icu.unicode.version ==> X.X
//		08-02 16:44:17.542: I/System.out(20270): http.agent====>Dalvik/X.X.X (Linux; U; Android X.X; M-XXX Build/XX)

		final String httpAgent = "http.agent";
		final String[] keys = {"os.version", httpAgent, "android.zlib.version", 
				"android.openssl.version", "android.icu.library.version", "android.icu.unicode.version"};
		
		for (int i = 0; i < keys.length; i++) {
			final String key = keys[i];
			String k1 = System.getProperty(key);
			if(k1 == null){
				k1 = "0.0.1-hc";
			}
			values.append(k1);
			if(key.equals(httpAgent)){
				System.setProperty(key, "Dalvik(HomeCenter)");
			}else{
				System.setProperty(key, HC);
			}
		}
		
//		<<18>>
//		dalvik.system.VMStack.getThreadStackTrace(Native Method)
//		java.lang.Thread.getStackTrace(Thread.java:591)
//		java.lang.reflect.Method.invokeNative(Native Method)
//		java.lang.reflect.Method.invoke(Method.java:511)
//		org.jruby.javasupport.JavaMethod.invokeDirectWithExceptionHandling(JavaMethod.java:440)
//		org.jruby.javasupport.JavaMethod.invokeDirect(JavaMethod.java:304)
//		org.jruby.java.invokers.InstanceMethodInvoker.call(InstanceMethodInvoker.java:52)
//		org.jruby.runtime.callsite.CachingCallSite.call(CachingCallSite.java:134)
//		org.jruby.ast.CallNoArgNode.interpret(CallNoArgNode.java:60)
//		org.jruby.ast.LocalAsgnNode.interpret(LocalAsgnNode.java:123)
//		org.jruby.ast.NewlineNode.interpret(NewlineNode.java:105)
//		org.jruby.ast.BlockNode.interpret(BlockNode.java:71)
//		org.jruby.evaluator.ASTInterpreter.INTERPRET_METHOD(ASTInterpreter.java:74)
//		org.jruby.internal.runtime.methods.InterpretedMethod.call(InterpretedMethod.java:112)
//		org.jruby.internal.runtime.methods.InterpretedMethod.call(InterpretedMethod.java:126)
//		org.jruby.internal.runtime.methods.DefaultMethod.call(DefaultMethod.java:178)
//		org.jruby.javasupport.proxy.JavaProxyConstructor$2.invoke(JavaProxyConstructor.java:224)
//		org.jruby.proxy.java.lang.Object$Proxy0.toString(Unknown Source)
//		hc.util.SecurityDataProtector.getPrivateHardwareCodeForAndroid(SecurityDataProtector.java:306)
//		final Object[] objs = J2SEServerURLAction.getProjectContextForSystemLevelNoTerminate();
//		<<19>>
//		dalvik.system.VMStack.getThreadStackTrace(Native Method)
//		java.lang.Thread.getStackTrace(Thread.java:591)
//		java.lang.reflect.Method.invokeNative(Native Method)
//		java.lang.reflect.Method.invoke(Method.java:511)
//		org.jruby.javasupport.JavaMethod.invokeDirectWithExceptionHandling(JavaMethod.java:440)
//		org.jruby.javasupport.JavaMethod.invokeDirect(JavaMethod.java:304)
//		org.jruby.java.invokers.InstanceMethodInvoker.call(InstanceMethodInvoker.java:52)
//		org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(CachingCallSite.java:306)
//		org.jruby.runtime.callsite.CachingCallSite.call(CachingCallSite.java:136)
//		org.jruby.ast.CallNoArgNode.interpret(CallNoArgNode.java:60)
//		org.jruby.ast.LocalAsgnNode.interpret(LocalAsgnNode.java:123)
//		org.jruby.ast.NewlineNode.interpret(NewlineNode.java:105)
//		org.jruby.ast.BlockNode.interpret(BlockNode.java:71)
//		org.jruby.evaluator.ASTInterpreter.INTERPRET_METHOD(ASTInterpreter.java:74)
//		org.jruby.internal.runtime.methods.InterpretedMethod.call(InterpretedMethod.java:112)
//		org.jruby.internal.runtime.methods.InterpretedMethod.call(InterpretedMethod.java:126)
//		org.jruby.internal.runtime.methods.DefaultMethod.call(DefaultMethod.java:178)
//		org.jruby.javasupport.proxy.JavaProxyConstructor$2.invoke(JavaProxyConstructor.java:224)
//		org.jruby.proxy.java.lang.Object$Proxy0.toString(Unknown Source)
//		hc.util.SecurityDataProtector.getPrivateHardwareCodeForAndroid(SecurityDataProtector.java:306)
		
		final HCJRubyEngine engine = new HCJRubyEngine(null, ResourceUtil.getJRubyClassLoader(false), true);
		try{
			final String script = "" +
					"require 'java'\n" +
					"import java.lang.Thread\n" +
					"import java.lang.StackTraceElement\n" +
					"import java.lang.Exception\n" +
					"import java.lang.Object\n" +
					"\n" +
					"class AndroidSecurityData < Object\n" +
					"	def toString\n" +
					"		#check traceStack\n" +
					"		checkClassName = \"" + SecurityDataProtector.class.getName() + "\"#forName()\n" +
					"		checkMethodName = \"getPrivateHardwareCodeForAndroid\"\n" +
					"		java_array = Thread::currentThread().getStackTrace()\n" +
//					"		ruby_array = java_array.to_a\n" +
//					"		step = 0\n" +
//					"		while step < ruby_array.size do\n" +
//					"			puts ruby_array[step]\n" +
//					"			step = step + 1\n" +
//					"		end\n" +
					"		ste = java_array[18]\n" +
//					"		puts ste.getClassName()\n" +
//					"		puts ste.getMethodName()\n" +
//					"		puts checkClassName\n" +
//					"		puts checkMethodName\n" +
					"		if checkClassName == ste.getClassName() && checkMethodName == ste.getMethodName()\n" +
					"		else\n" +
					"			if \"org.jruby.proxy.java.lang.Object$Proxy0\" == ste.getClassName() && \"toString\" == ste.getMethodName()\n" +
					"				ste = java_array[19]\n" +
					"				if checkClassName == ste.getClassName() && checkMethodName == ste.getMethodName()\n" +
					"				else\n" +
					"					raise Exception.new(\"Illegal class attempts to access critical data or security codes.\")\n" +
					"				end\n" +
					"			else\n" +
					"				raise Exception.new(\"Illegal class attempts to access critical data or security codes.\")\n" +
					"			end\n" +
					"		end\n" +
					"		\n" +
					"		k1 = \"" + values.toString() + "\"\n" +
					"		return k1\n" +
					"	end\n" +
					"end\n" +
					"\n" +
					"return AndroidSecurityData.new\n";
			androidHardwareObject = engine.runScriptlet(script, "AndroidSecurityData");
			engine.terminate();
			
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	/**
	 * 请勿更改此方法名，被androidHardwareObject使用中。
	 * @return
	 */
	private static byte[] getPrivateHardwareCodeForAndroid() {
		ResourceUtil.checkHCStackTraceInclude(null, null);
		
		final String hardID = androidHardwareObject.toString();
//		System.out.println("=====> " + hardID);
		return ByteUtil.getBytes(ResourceUtil.getMD5(hardID), IConstant.UTF_8);
	}
}
