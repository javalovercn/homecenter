package hc.util;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import hc.App;
import hc.core.IConstant;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringValue;
import hc.server.CallContext;
import hc.server.HCActionListener;
import hc.server.JRubyInstaller;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.design.engine.HCJRubyEngine;
import hc.server.ui.design.engine.RubyExector;
import hc.server.util.ServerUtil;

public class SecurityDataProtector {
	private static final String AES_CBC_ISO10126_PADDING = "AES/CBC/ISO10126Padding";
	public static final String DEV_CERT_FILE_EXT = "pfx";
	final static String fileDevCert = "dev_cert." + DEV_CERT_FILE_EXT;
	final static String fileHcHardId = "hc_hard_id.txt";
	static final String testAESSrc = "hello, my server";

	public static String getDevCertFileName() {
		return fileDevCert;
	}

	public static File getDevCertFile() {
		return new File(ResourceUtil.getBaseDir(), SecurityDataProtector.getDevCertFileName());
	}

	public static String getHcHardId() {
		return fileHcHardId;
	}

	private static boolean runOnce = false;
	private static final boolean isHCServerAndNotRelayServer = IConstant
			.isHCServerAndNotRelayServer();

	public static boolean isEnableSecurityData() {
		if (ResourceUtil.isAndroidServerPlatform()) {
			return JRubyInstaller.isJRubyInstalled();// 依赖于JRuby进行数据加密，此处不强制升级，有用即可
		} else {
			return true;
		}
	}

	private static boolean checkAESChanged() {
		final String checkResult = PropertiesManager.getValue(PropertiesManager.p_SecurityCheckAES);// 将目录数据复制到其它应用环境时，可能由于算法实现差异，导致数据差错

		if (checkResult == null) {
			LogManager.log("[SecurityDataProtector] SecurityCheckAES : null");
		} else {
			if (testAESSrc.equals(checkResult) == false) {
				LogManager.log("[SecurityDataProtector] fail SecurityCheckAES : ***, expected : "
						+ testAESSrc);
				return true;
			}
		}

		return false;
	}

	private static boolean checkNeedUpgrade() {
		// 缺省加密算法，在将工作目录复制到其它应用环境时，可能会产生错误，
		// if(PropertiesManager.getValue(PropertiesManager.p_EPC_EncodeProvider)
		// == null){
		// return true;
		// }

		return false;
	}

	private static Map<String, String> getOldSecurityDataValues() {
		final HashMap<String, String> map = new HashMap<String, String>(8);

		final PropertiesSet securityPropertiesSet = new PropertiesSet(
				PropertiesManager.S_SecurityProperties);
		final int size = securityPropertiesSet.size();
		for (int i = 0; i < size; i++) {
			final String key = securityPropertiesSet.getItem(i);
			final String value = PropertiesManager.getValue(key);
			if (value != null) {
				map.put(key, value);
			}
		}

		return map;

	}

	/**
	 * 注意：此方法新安装时，执行一次。<BR>
	 * 此方法内不能 加密用户型(EU) 数据，只能含 加密参数配置型(EPC) 数据的增减。
	 */
	private static void doUpgrade() {
	}

	private static void doUpgradeSaveBack(final Map<String, String> oldValues) {
		final Iterator<String> keys = oldValues.keySet().iterator();
		while (keys.hasNext()) {
			final String key = keys.next();
			PropertiesManager.setValue(key, oldValues.get(key));
			LogManager.log("[SecurityDataProtector] upgrading property : " + key);
		}

		PropertiesManager.saveFile();
	}

	private static void startUpgrade() {
		if (checkNeedUpgrade()) {
			LogManager.log("[SecurityDataProtector] need upgrade.");
			final Map<String, String> oldValues = getOldSecurityDataValues();
			doUpgrade();
			doUpgradeSaveBack(oldValues);
		}
	}

	public static void init() {
		final Object gLock = CCoreUtil.getGlobalLock();

		if (isEnableSecurityData() == false) {
			return;
		}

		if (runOnce == true) {
			return;
		} else {
			runOnce = true;
		}

		if (isHCServerAndNotRelayServer == false) {
			return;
		}

		CCoreUtil.checkAccess();

		if (ResourceUtil.isAndroidServerPlatform()) {
			initForAndroid();
		}

		synchronized (gLock) {
			final String serverKeyMD5 = getServerKeyMD5();
			final String realMD5 = ResourceUtil.getMD5(new String(getServerKey()));
			if (serverKeyMD5 == null) {
				initSecurityData(realMD5);
				doUpgrade();
				PropertiesManager.encodeSecurityDataFromTextMode();
				LogManager.log("encode security data from text mode!");
			} else {
				if (PropertiesManager.isTrue(PropertiesManager.p_isNeedResetPwd, false)// 上次故障后，没有修改密码。
						|| serverKeyMD5.equals(realMD5) == false || checkAESChanged()) {// 必须先检查md5，因为checkAESChanged依赖于前者
					final String uuid = PropertiesManager.getValue(PropertiesManager.p_uuid);
					PropertiesManager.setValue(PropertiesManager.p_isNeedResetPwd, true);
					doUpgrade();
					PropertiesManager.notifyErrorOnSecurityProperties();
					PropertiesManager.saveFile();

					new Thread() {
						@Override
						public void run() {
							try {
								Thread.sleep(20 * 1000);
							} catch (final Exception e) {
							}

							// 服务器也变化，比如直接拷贝到其它机器上
							LogManager.errToLog(
									"environment (network interface, disk, software...) is based by HomeCenter!"
											+ "\nthey are changed, it will clause security data failure, you need reset password!");

							final JPanel panel = new JPanel(new BorderLayout());
							panel.add(new JLabel("<html>" + ResourceUtil.get(9208) + "</html>",
									App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING),
									BorderLayout.CENTER);
							final ActionListener listener = new HCActionListener(new Runnable() {
								@Override
								public void run() {
									initSecurityData(realMD5);
									ResourceUtil.generateCertForNullOrError();// 需要系统初始化完成，所以不能置入notifyErrorOnSecurityProperties

									App.showInputPWDDialog(uuid, "", "", false);

									J2SESessionManager.stopAllSession(false, true, false);

									PropertiesManager.remove(PropertiesManager.p_isNeedResetPwd);
									PropertiesManager.saveFile();
								}
							}, App.getThreadPoolToken());

							App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getErrorI18N(), false,
									null, null, listener, listener, null, true, false, null, false,
									true);
							// App.showConfirmDialog(null,
							// "<html>" + (String)ResourceUtil.get(9208) +
							// "</html>", ResourceUtil.getErrorI18N(),
							// JOptionPane.CLOSED_OPTION,
							// JOptionPane.ERROR_MESSAGE,
							// App.getSysIcon(App.SYS_ERROR_ICON));
						}
					}.start();
				} else {
					LogManager.log("[SecurityDataProtector] pass check!");
					// 加密环境检查正常
					startUpgrade();
					PropertiesManager.encodeSecurityDataFromTextMode();// 可能后期扩展
				}
			}
		} // end gLock
	}

	// public static void main(final String[] args){
	// final String source = "Hello World!";
	// System.out.println(decode(encode(source)));
	// }

	private static String getCipherName() {
		return PropertiesManager.getValue(PropertiesManager.p_SecurityCipher,
				AES_CBC_ISO10126_PADDING);
	}

	private static int sdpVersion = -1;

	private static int getSDPVersion() {
		if (sdpVersion == -1) {
			try {
				sdpVersion = Integer.valueOf(
						PropertiesManager.getValue(PropertiesManager.p_SecuritySDPVersion, "1"));
			} catch (final Throwable e) {
				e.printStackTrace();
				sdpVersion = 1;
			}
		}
		return sdpVersion;
	}

	private static int securityKeySize = -1;

	private static int getSecretKeySize() {
		if (securityKeySize == -1) {
			try {
				securityKeySize = Integer.valueOf(
						PropertiesManager.getValue(PropertiesManager.p_SecuritySecretKeySize, "0"));
			} catch (final Throwable e) {
				e.printStackTrace();
				securityKeySize = 0;
			}
		}
		return securityKeySize;
	}

	private static final int defaultSDPVersion = 2;

	static String encode(final String value) {
		if (isHCServerAndNotRelayServer == false) {
			return value;
		}

		// Android环境下，没有下载JRuby前，是不加密状态的。
		if (PropertiesManager.getValue(PropertiesManager.p_ServerSecurityKeyMD5) == null) {
			return value;
		}

		try {
			final int ver = getSDPVersion();

			if (ver == defaultSDPVersion) {
				return encodeByCipher(value, getCipherName(), getSecretKeySize());
			} else if (ver == 1) {
				return encodeByCipherV1(value, getCipherName());
			} else {
				LogManager.errToLog("unknow SDPVersion : " + ver);
			}
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}

		return value;
	}

	private static String encodeByCipher(final String value, final String cipherName,
			final int keySize) throws Throwable {
		final byte[] privateBS = getServerKey();

		final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
		secureRandom.setSeed(privateBS);

		final Cipher cipher = Cipher.getInstance(cipherName);
		final SecretKeySpec secretKeySpec = new SecretKeySpec(
				ResourceUtil.buildFixLenBS(privateBS, keySize), "AES");
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, buildIV(privateBS, 16), secureRandom);

		final byte[] doFinal = cipher.doFinal(ByteUtil.getBytes(value, IConstant.UTF_8));
		final String out = ByteUtil.toHex(doFinal);
		return out;
	}

	private static String encodeByCipherV1(final String value, final String cipherName)
			throws Throwable {
		final byte[] privateBS = getServerKey();

		final KeyGenerator kgen = KeyGenerator.getInstance("AES");
		final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
		secureRandom.setSeed(privateBS);
		kgen.init(128, secureRandom);

		final Cipher cipher = Cipher.getInstance(cipherName);
		final SecretKeySpec secretKeySpec = new SecretKeySpec(kgen.generateKey().getEncoded(),
				"AES");
		cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, buildIV(privateBS, 16), secureRandom);

		final byte[] doFinal = cipher.doFinal(ByteUtil.getBytes(value, IConstant.UTF_8));
		final String out = ByteUtil.toHex(doFinal);
		return out;
	}

	static String decode(final String data) {
		if (isHCServerAndNotRelayServer == false) {// 有可能中继服务器
			return data;
		}

		// Android环境下，没有下载JRuby前
		if (PropertiesManager.getValue(PropertiesManager.p_ServerSecurityKeyMD5) == null) {
			return data;
		}

		try {
			final int ver = getSDPVersion();

			if (ver == defaultSDPVersion) {
				return decodeByCipher(data, getCipherName(), getSecretKeySize());
			} else if (ver == 1) {
				return decodeByCipherV1(data, getCipherName());
			} else {
				LogManager.errToLog("unknow SDPVersion : " + ver);
			}
		} catch (final Throwable e) {
			e.printStackTrace();// 可能环境变化，导致解密失败
		}

		return "";// 不能返回data，会导致解密成功
	}

	private static String decodeByCipher(final String data, final String cipherName,
			final int keySize) throws Throwable {
		final byte[] privateBS = getServerKey();
		final byte[] src = ByteUtil.toBytesFromHexStr(data);

		final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
		secureRandom.setSeed(privateBS);

		final Cipher cipher = Cipher.getInstance(cipherName);
		final SecretKeySpec secretKeySpec = new SecretKeySpec(
				ResourceUtil.buildFixLenBS(privateBS, keySize), "AES");
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, buildIV(privateBS, 16), secureRandom);
		final byte[] decryptBytes = cipher.doFinal(src);

		return new String(decryptBytes, IConstant.UTF_8);
	}

	private static String decodeByCipherV1(final String data, final String cipherName)
			throws Throwable {
		final byte[] privateBS = getServerKey();
		final byte[] src = ByteUtil.toBytesFromHexStr(data);

		final KeyGenerator kgen = KeyGenerator.getInstance("AES");
		final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
		secureRandom.setSeed(privateBS);
		kgen.init(128, secureRandom);

		final Cipher cipher = Cipher.getInstance(cipherName);
		final SecretKeySpec secretKeySpec = new SecretKeySpec(kgen.generateKey().getEncoded(),
				"AES");
		cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, buildIV(privateBS, 16), secureRandom);
		final byte[] decryptBytes = cipher.doFinal(src);

		return new String(decryptBytes, IConstant.UTF_8);
	}

	private static IvParameterSpec buildIV(final byte[] privateBS, final int ivLen) {
		return new IvParameterSpec(ResourceUtil.buildFixLenBS(privateBS, ivLen));
	}

	private static String getServerKeyMD5() {
		return PropertiesManager.getValue(PropertiesManager.p_ServerSecurityKeyMD5);
	}

	private static final byte[] getServerKey() {
		if (ResourceUtil.isAndroidServerPlatform()) {
			return getPrivateHardwareCodeForAndroid();
		} else {
			return getPrivateHardwareCodeForJ2SE();
		}
	}

	static byte[] hc_hard_id_bs;

	private static byte[] getPrivateHardwareCodeForJ2SE() {
		ResourceUtil.checkHCStackTrace();// 由于缓存于field中，所以加此

		// 注意：由于本类入HCLimitSecurityManager.checkMemberAccess，所以保护属性即可
		if (hc_hard_id_bs == null) {
			long hc_hard_id_file_len = 0;
			int readNum = 0;

			final File hardIDFile = new File(ResourceUtil.getBaseDir(), fileHcHardId);// 注意：此文件名HCLimitSecurityManager.checkReadImpl方法内csc
																						// ==
																						// null时检查

			if (hardIDFile.exists() == false) {
			} else {
				hc_hard_id_file_len = hardIDFile.length();

				InputStream ios = null;
				try {
					ios = new FileInputStream(hardIDFile);
					hc_hard_id_bs = new byte[(int) hc_hard_id_file_len];
					readNum = ios.read(hc_hard_id_bs);
				} catch (final Throwable e) {
				} finally {
					try {
						ios.close();
					} catch (final Throwable e) {
					}
				}
			}

			// 文件出错，或新建
			if (readNum == 0 || readNum != hc_hard_id_file_len) {
				hc_hard_id_bs = ResourceUtil.buildUUID().getBytes();

				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(hardIDFile);
					fos.write(hc_hard_id_bs);
					fos.flush();
				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					try {
						fos.close();
					} catch (final Exception e) {
					}
				}
			}
		}

		return hc_hard_id_bs;

		// try {
		// final Enumeration<NetworkInterface> el =
		// NetworkInterface.getNetworkInterfaces();
		// while (el.hasMoreElements()) {
		// final byte[] mac = el.nextElement().getHardwareAddress();
		// if (mac == null)
		// continue;
		//
		// return mac;
		// }
		// }catch (final Exception exception) {
		// exception.printStackTrace();
		// }

		// final long baseDirTotalSpace =
		// PlatformManager.getService().getBaseDir().getTotalSpace();
		//
		// try{
		// final StringBuilder sb = new StringBuilder();
		//
		// final Class fileStoreClass =
		// Class.forName("java.nio.file.FileStore");
		// final Class fileSystemsClass =
		// Class.forName("java.nio.file.FileSystems");
		// final Class fileSystemClass =
		// Class.forName("java.nio.file.FileSystem");
		// final Object defaultFileSystem =
		// ClassUtil.invokeWithExceptionOut(fileSystemsClass, fileSystemsClass,
		// "getDefault", ClassUtil.NULL_PARA_TYPES, ClassUtil.NULL_PARAS,
		// false);
		// final Iterable it =
		// (Iterable)ClassUtil.invokeWithExceptionOut(fileSystemClass,
		// defaultFileSystem, "getFileStores", ClassUtil.NULL_PARA_TYPES,
		// ClassUtil.NULL_PARAS, false);
		//
		// for (final Object store: it) {
		// final long total =
		// (Long)ClassUtil.invokeWithExceptionOut(fileStoreClass, store,
		// "getTotalSpace", ClassUtil.NULL_PARA_TYPES, ClassUtil.NULL_PARAS,
		// false);
		// if(total == baseDirTotalSpace){
		// sb.append(store.toString());
		// sb.append(total);
		// }
		// }
		//
		// if(sb.length() > 0){
		//// System.out.println("getPrivateHardwareCodeForJ2SE : " +
		// sb.toString());
		// return ResourceUtil.getMD5(sb.toString()).getBytes();
		// }
		// }catch (final Throwable e) {
		// }
		//
		// return String.valueOf(baseDirTotalSpace).getBytes();
	}

	private static Object androidHardwareObject;
	final static String dynClassName0 = "org.jruby.proxy.java.lang.Object$Proxy0";
	final static String dynClassName1 = "org.jruby.proxy.java.lang.Object$Proxy1";// 兼容旧的
	final static String toStrMethodName = "toString";

	public static final void checkHCStackTraceForSDP(final StackTraceElement[] el)
			throws Exception {// 注意：请勿改方法

		final ClassLoader checkLoader = SecurityDataProtector.class.getClassLoader();

		// for (int i = el.length - 1; i >= 0; i--) {
		// final StackTraceElement ste = el[i];
		// final String className = ste.getClassName();
		// LogManager.log("idx : " + i + ", className : " + className + ",
		// method : " + ste.getMethodName());
		// }

		for (int i = el.length - 1; i >= 0; i--) {
			final StackTraceElement ste = el[i];
			final String className = ste.getClassName();
			if ((className.equals(dynClassName0) || className.equals(dynClassName1))
					&& ste.getMethodName().equals(toStrMethodName)) {
				// 02-02 10:26:51.082: I/HomeCenter(2007): idx : 19
				// hc.util.SecurityDataProtector.getPrivateHardwareCodeForAndroid(SecurityDataProtector.java:656)
				// 02-02 10:26:51.082: I/HomeCenter(2007): idx : 18
				// org.jruby.proxy.java.lang.Object$Proxy1.toString(Unknown
				// Source)
				final StackTraceElement backSte = el[i + 1];
				if (backSte.getMethodName().equals("getPrivateHardwareCodeForAndroid")
						&& backSte.getClassName().equals(SecurityDataProtector.class.getName())) {
					return;// pass
				}
			}

			try {
				Class.forName(className, false, checkLoader);
			} catch (final Exception e) {
				throw new Exception(PropertiesManager.ILLEGAL_CLASS);
			}
		}
	}

	private static void initForAndroid() {

		final StringBuilder values = new StringBuilder();

		final String HC = "HomeCenter";
		try {
			{
				final Class<?> c = Class.forName("android.os.Build");
				final Field get = c.getField("SERIAL");
				values.append((String) get.get(c));
				get.setAccessible(true);
				get.set(c, HC);
			}
		} catch (final Exception ignored) {
		}

		// 07-30 06:18:37.370: I/HomeCenter(924): os.version ==> X.X-TTXX
		// 07-30 06:18:37.420: I/HomeCenter(924): android.zlib.version ==> X.X.X
		// 07-30 06:18:37.430: I/HomeCenter(924): android.openssl.version ==>
		// OpenSSL X.Xe XX Feb XXXX
		// 07-30 06:18:37.390: I/HomeCenter(924): android.icu.library.version
		// ==> XX.X
		// 07-30 06:18:37.370: I/HomeCenter(924): android.icu.unicode.version
		// ==> X.X
		// 08-02 16:44:17.542: I/System.out(20270): http.agent====>Dalvik/X.X.X
		// (Linux; U; Android X.X; M-XXX Build/XX)

		final String httpAgent = "http.agent";

		// 注意：os.name被getUserAgentForHAR使用
		final String[] keys = { "os.version", httpAgent, "android.zlib.version",
				"android.openssl.version", "android.icu.library.version",
				"android.icu.unicode.version" };

		for (int i = 0; i < keys.length; i++) {
			final String key = keys[i];
			String k1 = System.getProperty(key);
			if (k1 == null) {
				k1 = "0.0.1-hc";
			}
			values.append(k1);
			try {
				if (key.equals(httpAgent)) {
					System.setProperty(key, "Dalvik(HomeCenter)");
				} else {
					System.setProperty(key, HC);
				}
			} catch (final Throwable e) {
				e.printStackTrace();
			}
		}

		// <<18>>
		// dalvik.system.VMStack.getThreadStackTrace(Native Method)
		// java.lang.Thread.getStackTrace(Thread.java:591)
		// java.lang.reflect.Method.invokeNative(Native Method)
		// java.lang.reflect.Method.invoke(Method.java:511)
		// org.jruby.javasupport.JavaMethod.invokeDirectWithExceptionHandling(JavaMethod.java:440)
		// org.jruby.javasupport.JavaMethod.invokeDirect(JavaMethod.java:304)
		// org.jruby.java.invokers.InstanceMethodInvoker.call(InstanceMethodInvoker.java:52)
		// org.jruby.runtime.callsite.CachingCallSite.call(CachingCallSite.java:134)
		// org.jruby.ast.CallNoArgNode.interpret(CallNoArgNode.java:60)
		// org.jruby.ast.LocalAsgnNode.interpret(LocalAsgnNode.java:123)
		// org.jruby.ast.NewlineNode.interpret(NewlineNode.java:105)
		// org.jruby.ast.BlockNode.interpret(BlockNode.java:71)
		// org.jruby.evaluator.ASTInterpreter.INTERPRET_METHOD(ASTInterpreter.java:74)
		// org.jruby.internal.runtime.methods.InterpretedMethod.call(InterpretedMethod.java:112)
		// org.jruby.internal.runtime.methods.InterpretedMethod.call(InterpretedMethod.java:126)
		// org.jruby.internal.runtime.methods.DefaultMethod.call(DefaultMethod.java:178)
		// org.jruby.javasupport.proxy.JavaProxyConstructor$2.invoke(JavaProxyConstructor.java:224)
		// org.jruby.proxy.java.lang.Object$Proxy0.toString(Unknown Source)
		// hc.util.SecurityDataProtector.getPrivateHardwareCodeForAndroid(SecurityDataProtector.java:306)
		// final Object[] objs =
		// J2SEServerURLAction.getProjectContextForSystemLevelNoTerminate();
		// <<19>>
		// dalvik.system.VMStack.getThreadStackTrace(Native Method)
		// java.lang.Thread.getStackTrace(Thread.java:591)
		// java.lang.reflect.Method.invokeNative(Native Method)
		// java.lang.reflect.Method.invoke(Method.java:511)
		// org.jruby.javasupport.JavaMethod.invokeDirectWithExceptionHandling(JavaMethod.java:440)
		// org.jruby.javasupport.JavaMethod.invokeDirect(JavaMethod.java:304)
		// org.jruby.java.invokers.InstanceMethodInvoker.call(InstanceMethodInvoker.java:52)
		// org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(CachingCallSite.java:306)
		// org.jruby.runtime.callsite.CachingCallSite.call(CachingCallSite.java:136)
		// org.jruby.ast.CallNoArgNode.interpret(CallNoArgNode.java:60)
		// org.jruby.ast.LocalAsgnNode.interpret(LocalAsgnNode.java:123)
		// org.jruby.ast.NewlineNode.interpret(NewlineNode.java:105)
		// org.jruby.ast.BlockNode.interpret(BlockNode.java:71)
		// org.jruby.evaluator.ASTInterpreter.INTERPRET_METHOD(ASTInterpreter.java:74)
		// org.jruby.internal.runtime.methods.InterpretedMethod.call(InterpretedMethod.java:112)
		// org.jruby.internal.runtime.methods.InterpretedMethod.call(InterpretedMethod.java:126)
		// org.jruby.internal.runtime.methods.DefaultMethod.call(DefaultMethod.java:178)
		// org.jruby.javasupport.proxy.JavaProxyConstructor$2.invoke(JavaProxyConstructor.java:224)
		// org.jruby.proxy.java.lang.Object$Proxy0.toString(Unknown Source)
		// hc.util.SecurityDataProtector.getPrivateHardwareCodeForAndroid(SecurityDataProtector.java:306)

		final HCJRubyEngine engine = new HCJRubyEngine(null, null,
				ServerUtil.getJRubyClassLoader(false), true,
				HCJRubyEngine.IDE_LEVEL_ENGINE + "SecurityDataProtect");
		try {
			final String script = "" + "import java.lang.Thread\n"
					+ "import java.lang.StackTraceElement\n" + "import java.lang.Exception\n"
					+ "import java.lang.Object\n" + "import java.lang.System\n"
					+ "import Java::hc.util.SecurityDataProtector\n" + "\n"
					+ "class AndroidSecurityData < Object\n" + "	def toString\n"
					+ "		#check traceStack\n"
					+ "		java_array = Thread::currentThread().getStackTrace()\n" +
					// " ruby_array = java_array.to_a\n" +
					// " step = 0\n" +
					// " while step < ruby_array.size do\n" +
					// " System::err.println(ruby_array[step])\n" +
					// " step = step + 1\n" +
					// " end\n" +
					"		\n"
					+ "		SecurityDataProtector::checkHCStackTraceForSDP(java_array)\n"
					+ "		\n" + "		k1 = \"" + values.toString() + "\"\n" + "		return k1\n"
					+ "	end\n" + "end\n" + "\n" + "return AndroidSecurityData.new\n";
			final CallContext callCtx = new CallContext();
			androidHardwareObject = RubyExector.runAndWaitOnEngine(callCtx, new StringValue(script),
					"AndroidSecurityData", null, engine);
			if (callCtx.isError) {
				callCtx.rubyThrowable.printStackTrace();
			}
			engine.terminate();

		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}

	}

	/**
	 * 请勿更改此方法名，被androidHardwareObject使用中。
	 * 
	 * @return
	 */
	private static byte[] getPrivateHardwareCodeForAndroid() {
		final String hardID = androidHardwareObject.toString();
		// System.out.println("=====> " + hardID);
		return ByteUtil.getBytes(ResourceUtil.getMD5(hardID), IConstant.UTF_8);
	}

	private static void initSecurityData(final String realMD5) {
		PropertiesManager.setValue(PropertiesManager.p_ServerSecurityKeyMD5, realMD5);

		PropertiesManager.remove(PropertiesManager.p_SecurityCipher);
		PropertiesManager.remove(PropertiesManager.p_SecuritySDPVersion);

		final String[] ciphers = { AES_CBC_ISO10126_PADDING, "AES/CBC/PKCS7Padding",
				"AES/CBC/PKCS5Padding", "AES/CFB/NoPadding", "AES" };
		final String testSrc = "Hello,My Server";// 15字符，不应为16
		sdpVersion = defaultSDPVersion;

		for (int i = 0; i < ciphers.length; i++) {
			final String cipherName = ciphers[i];
			int searchKeySize = -1;
			try {
				final KeyGenerator kgen = KeyGenerator.getInstance("AES");
				final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
				secureRandom.setSeed(getServerKey());
				kgen.init(128, secureRandom);
				searchKeySize = kgen.generateKey().getEncoded().length;

				LogManager.log("[SecurityDataProtector] try cipher : " + cipherName);
				String out = null;

				while (true) {
					try {
						out = decodeByCipher(encodeByCipher(testSrc, cipherName, searchKeySize),
								cipherName, searchKeySize);
						break;
					} catch (final Throwable e) {
						if (e instanceof java.security.InvalidKeyException) {
							searchKeySize *= 2;
							continue;
						} else {
							throw e;
						}
					}
				}

				if (testSrc.equals(out)) {
					PropertiesManager.setValue(PropertiesManager.p_SecurityCipher, cipherName);
					PropertiesManager.setValue(PropertiesManager.p_SecuritySDPVersion,
							Integer.toString(sdpVersion));
					PropertiesManager.setValue(PropertiesManager.p_SecuritySecretKeySize,
							Integer.toString(searchKeySize));
					LogManager.log("[SecurityDataProtector] cipher : " + cipherName
							+ ", SDPVersion : " + sdpVersion + "OK!");
					break;
				}
			} catch (final Throwable e) {
				e.printStackTrace();
			}
		}

		if (PropertiesManager.propertie.getProperty(PropertiesManager.p_SecurityCheckAES) == null) {// 不用能getValue()
			PropertiesManager.setValue(PropertiesManager.p_SecurityCheckAES, testAESSrc);
		}
	}

}
