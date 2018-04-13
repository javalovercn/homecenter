package hc.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.undo.UndoableEdit;

import hc.App;
import hc.core.ConfigManager;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.util.BooleanValue;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LangUtil;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.StringUtil;
import hc.core.util.WiFiDeviceManager;
import hc.res.ImageSrc;
import hc.server.DefaultManager;
import hc.server.HCSecurityException;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.server.StarterManager;
import hc.server.TrayMenuUtil;
import hc.server.data.KeyComperPanel;
import hc.server.data.StoreDirManager;
import hc.server.msb.AnalysableRobotParameter;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.DialogHTMLMlet;
import hc.server.ui.Mlet;
import hc.server.ui.ProjectContext;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.SystemDialog;
import hc.server.ui.design.SystemHTMLMlet;
import hc.server.util.SafeDataManager;

public class ResourceUtil {
	private static final int MAX_RES_ID = 10000;
	public static final String USER_PROJ = "user.proj.";
	private static boolean isCheckStarter;
	private static Class starterClass;
	public static final String SYS_LOOKFEEL = "System - ";
	public static final String LF_NIMBUS = SYS_LOOKFEEL + "Nimbus";
	final static Random random = new Random();
	private static final Calendar calendarForRandom = Calendar.getInstance();
	private static final StringBuilder stringBuilderForRandom = new StringBuilder(128);
	private static boolean isDoneForDocumentEventTypeWrapper = false;
	private static Field documentEvent;

	public static final String JAVA_VENDOR = System.getProperty("java.vm.vendor", "Unknown");

	private static String[] JAVA_VERSION = { "1.2", "1.3", "1.4", "5", "6", "7", "8", "9", "10" };
	private static float[] JRE_VERSION = { 1.2F, 1.3F, 1.4F, 1.5F, 1.6F, 1.7F, 1.8F, 9F, 10F };
	private static int[] CAFE_CODE = { 46, 47, 48, 49, 50, 51, 52, 53, 54 };
	private static final int JAVA_CLASS_MAGIC = 0xCAFEBABE;

	/**
	 * 0 means error.
	 * 
	 * @param cafe
	 * @return
	 */
	public static float getJREVersionFromCafeCode(final int cafe) {
		for (int i = 0; i < CAFE_CODE.length; i++) {
			if (CAFE_CODE[i] == cafe) {
				return JRE_VERSION[i];
			}
		}

		return 0;
	}

	public static boolean containsIgnoreCase(final String where, final String what) {
		return where.toLowerCase().indexOf(what.toLowerCase(), 0) >= 0;
	}

	public static final boolean isAppleJvm() {
		return containsIgnoreCase(JAVA_VENDOR, "Apple");
	}

	public static final boolean isOracleJvm() {
		return containsIgnoreCase(JAVA_VENDOR, "Oracle");
	}

	public static final boolean isSunJvm() {
		return containsIgnoreCase(JAVA_VENDOR, "Sun") && containsIgnoreCase(JAVA_VENDOR, "Microsystems");
	}

	public static final boolean isIbmJvm() {
		return containsIgnoreCase(JAVA_VENDOR, "IBM");
	}
	
	final static BooleanValue isShowTrayReady = new BooleanValue(false);
	
	public static void notifyTrayReady() {
		isShowTrayReady.value = true;
		synchronized (isShowTrayReady) {
			isShowTrayReady.notify();
		}
	}
	
	public static void showTrayWithCheckReady(final boolean isDispInitlizing) {
		if (ResourceUtil.isNonUIServer()) {
			return;
		}
		
		synchronized (isShowTrayReady) {
			if(isShowTrayReady.value == false) {
				try {
					isShowTrayReady.wait();
				} catch (final InterruptedException e) {
				}
			}
		}

		TrayMenuUtil.showTray(isDispInitlizing);
	}

	private static Integer screenDeviceScale;

	public static int getScreenDeviceScale() {// for example, MacBook Pro retina
												// screen
		if (screenDeviceScale == null) {
			final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			final GraphicsDevice device = env.getDefaultScreenDevice();

			try {
				final Field field = device.getClass().getDeclaredField("scale");// SystemInfo.isJavaVersionAtLeast("1.7.0_40")
																				// &&
																				// SystemInfo.isOracleJvm

				if (field != null) {
					field.setAccessible(true);
					final Object scale = field.get(device);

					if (scale instanceof Integer) {
						screenDeviceScale = ((Integer) scale).intValue();
					}
					field.setAccessible(false);
				}
			} catch (final Exception e) {
			} finally {
				if (screenDeviceScale == null) {
					screenDeviceScale = PropertiesManager.getIntValue(PropertiesManager.p_screenDeviceScale, 1);
				}
			}
		}

		return screenDeviceScale;
	}
	
	public static char searchStringBoundChar(final char[] chars, final int idx) {
		char stringChar = 0;
		for (int i = 0; i < idx; i++) {
			final char c = chars[i];
			if(c == '#') {
				break;
			}
			
			if(c == '"' || c == '\'') {
				if(i > 0) {
					if(chars[i - 1] == '\\') {
						continue;
					}
				}
				if(stringChar != 0) {
					if(stringChar != c) {
						continue;
					}else {
						stringChar = 0;
						continue;
					}
				}else {
					stringChar = c;
				}
			}
		}
		
		return stringChar;
	}
	
	public static boolean isInString(final char[] chars, final int startIdx) {
		return searchStringBoundChar(chars, startIdx) != 0;
	}
	
	public static final boolean isWordSpliter(final char[] chars, final int idx) {
		if(idx >=0 && idx < chars.length) {
			final char c = chars[idx];
			if(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_') {
				return false;
			}else {
				return true;
			}
		}else {
			return true;
		}
	}
	
	public final static boolean isSameContent(final InputStream is1, final InputStream is2) {
		final byte[] src1BS = new byte[2048];
		final byte[] src2BS = new byte[src1BS.length];//注意：请勿使用cache byte

		try {
			while (true) {
				final int len1 = is1.read(src1BS);
				final int len2 = is2.read(src2BS);
				if (len1 != len2) {
					return false;
				}

				if (len1 == -1) {
					return true;
				}

				for (int i = 0; i < len1; i++) {
					if (src1BS[i] != src2BS[i]) {
						return false;
					}
				}
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		} finally {
			try {
				is1.close();
			} catch (final Throwable e) {
			}
			try {
				is2.close();
			} catch (final Throwable e) {
			}
		}
		return false;
	}

	public static String[] reverseStringArray(final String[] src) {
		final int size = src.length;
		final String[] out = new String[size];
		for (int i = 0; i < size; i++) {
			out[size - i - 1] = src[i];
		}
		return out;
	}
	
	public static String getJavaVersionFromStringJRE(final String version) {
		return getJavaVersionFromFloatJRE(Float.valueOf(version));
	}
	
	public static String getJavaVersionFromFloatJRE(final float version) {
		for (int i = 0; i < JRE_VERSION.length; i++) {
			if(JRE_VERSION[i] == version) {
				return JAVA_VERSION[i];
			}
		}
		
		return null;
	}
	
	public static float getJREFromJavaVersion(final String javaVer) {
		for (int i = 0; i < JAVA_VERSION.length; i++) {
			if(JAVA_VERSION[i].equals(javaVer)) {
				return JRE_VERSION[i];
			}
		}
		
		throw new Error("invalid java version : " + javaVer);
	}

	public static String[] getAllJavaVersion() {
		final int size = JAVA_VERSION.length;
		final String[] out = new String[size];
		for (int i = 0; i < size; i++) {
			out[i] = JAVA_VERSION[i];
		}
		return out;
	}

	/**
	 * 0 means unknown or pure resource jar file.
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static float getMaxJREVersionFromCompileJar(final File file) {
		final int cafeCode = getMaxCompileCafeCode(file);
		return getJREVersionFromCafeCode(cafeCode);
	}

	private static int getMaxCompileCafeCode(final File file) {
		int maxVersion = -1;
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(file, false);
			final Enumeration<JarEntry> enumeration = jarFile.entries();
			while (enumeration.hasMoreElements()) {
				final JarEntry entry = enumeration.nextElement();
				if (entry.getName().endsWith(".class")) {
					final InputStream in = jarFile.getInputStream(entry);
					final int ver = getVersion(in);
					if (ver > maxVersion) {
						maxVersion = ver;
					}
					in.close();
				}
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		} finally {
			try {
				jarFile.close();
			} catch (final Exception e) {
			}
		}
		return maxVersion;
	}

	private static int getVersion(final InputStream in) {
		final DataInputStream dis = new DataInputStream(in);
		try {
			final int magic = dis.readInt();
			if (magic == JAVA_CLASS_MAGIC) {
				dis.readUnsignedShort();
				final int majorVersion = dis.readUnsignedShort();
				return majorVersion;
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return -1;
	}

	public static AbstractDocument.DefaultDocumentEvent getDocumentEventType(final UndoableEdit edit) {
		if (edit instanceof AbstractDocument.DefaultDocumentEvent) {
			return (AbstractDocument.DefaultDocumentEvent) edit;
		}

		try {
			if (documentEvent == null) {
				if (isDoneForDocumentEventTypeWrapper == false) {
					// 以下是Java 9的实现
					final Class claz = Class.forName("javax.swing.text.AbstractDocument$DefaultDocumentEventUndoableWrapper");
					// final DefaultDocumentEvent dde;
					isDoneForDocumentEventTypeWrapper = true;
					documentEvent = claz.getDeclaredField("dde");
					documentEvent.setAccessible(true);
				}
			}
			if (documentEvent != null) {
				return getDocumentEventType((UndoableEdit) documentEvent.get(edit));
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		throw new Error("unknow DocumentEventType.");
	}

	private static Class getStarterClass() {
		if (isCheckStarter == false) {
			isCheckStarter = true;
			starterClass = loadClass(StarterManager.CLASSNAME_STARTER_STARTER, false);
		}
		return starterClass;
	}

	public static JPanel buildFixedWidthPanel(final JLabel label, final JComponent comp) {
		final JPanel grid = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();

		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		grid.add(label, c);

		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		grid.add(comp, c);

		final Dimension dimen = grid.getPreferredSize();
		dimen.width = dimen.height * 10;
		grid.setPreferredSize(dimen);
		return grid;
	}

	public static String buildDescPrefix(final CoreSession coreSS, final String msg) {
		final StringBuilder sb = StringBuilderCacher.getFree();
		sb.append(get(coreSS, 9095));
		sb.append(get(coreSS, 1041));
		sb.append(msg);
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		return out;
	}

	public static void printStackTrace(final Throwable t) {
		final StackTraceElement[] trace = t.getStackTrace();
		final StringBuilder sb = StringBuilderCacher.getFree();
		for (final StackTraceElement traceElement : trace) {
			sb.append("\tat ");
			sb.append(traceElement);
			sb.append('\n');
		}
		LogManager.errToLog(sb.toString());
		StringBuilderCacher.cycle(sb);

		// Print suppressed exceptions, if any
		// Throwable[] se = t.getSuppressed();
		try {
			final Throwable[] se = (Throwable[]) ClassUtil.invokeWithExceptionOut(Throwable.class, t, "getSuppressed",
					ClassUtil.NULL_PARA_TYPES, ClassUtil.NULL_PARAS, false);
			if (se != null) {
				for (int i = 0; i < se.length; i++) {
					printStackTrace(se[i]);
				}
			}
		} catch (final Throwable e) {
		}

		// Print cause, if any
		final Throwable ourCause = t.getCause();
		if (ourCause != null) {
			printStackTrace(ourCause);
		}
	}

	public static Color toDarker(final Color color, final float factor) {
		return new Color(Math.max((int) (color.getRed() * factor), 0), Math.max((int) (color.getGreen() * factor), 0),
				Math.max((int) (color.getBlue() * factor), 0), color.getAlpha());
	}

	/**
	 * https://zh.wikipedia.org/wiki/Template:ISO_639_name
	 * 
	 * @param locale
	 * @param map
	 * @param isMaybeEqualLang
	 *            true : 可能存在相等的lang，比如he=iw
	 * @return
	 */
	public static String matchLocale(final String locale, final Map map, final boolean isMaybeEqualLang) {
		String out = (String) map.get(locale);
		if (out != null) {
			return out;
		}

		final int lastSplitIdx = locale.lastIndexOf(LangUtil.LOCALE_SPLIT);
		if (lastSplitIdx >= 0) {
			final String[] parts = StringUtil.splitToArray(locale, LangUtil.LOCALE_SPLIT);
			boolean isEqualLang = false;
			if (isMaybeEqualLang) {
				final String part0 = parts[0];
				for (int i = 0; i < LangUtil.equalLocale.length; i++) {
					if (part0.equals(LangUtil.equalLocale[i])) {
						isEqualLang = true;

						out = (String) map.get(LangUtil.buildEqualLocale(parts, LangUtil.equalLocaleTo[i]));
						if (out != null) {
							return out;
						}

						break;
					}
				}
				for (int i = 0; i < LangUtil.equalLocaleTo.length; i++) {
					if (part0.equals(LangUtil.equalLocaleTo[i])) {
						isEqualLang = true;

						out = (String) map.get(LangUtil.buildEqualLocale(parts, LangUtil.equalLocale[i]));
						if (out != null) {
							return out;
						}

						break;
					}
				}
			}

			if (parts.length == 3) {// zh-Hans-CN => zh-CN
				final String oneTwo = parts[0] + LangUtil.LOCALE_SPLIT + parts[1];
				out = (String) map.get(oneTwo);
				if (out != null) {
					return out;
				}

				for (int i = 0; i < LangUtil.oneTwoEquals.length; i++) {
					if (oneTwo.equals(LangUtil.oneTwoEquals[i])) {
						out = (String) map.get(LangUtil.oneThreeEquals[i]);
						if (out != null) {
							return out;
						}
					}
				}

				final String oneThree = parts[0] + LangUtil.LOCALE_SPLIT + parts[2];
				out = (String) map.get(oneThree);
				if (out != null) {
					return out;
				}

				for (int i = 0; i < LangUtil.oneThreeEquals.length; i++) {
					if (oneThree.equals(LangUtil.oneThreeEquals[i])) {
						out = (String) map.get(LangUtil.oneTwoEquals[i]);
						if (out != null) {
							return out;
						}
					}
				}

				final String ml = matchLocale(oneTwo, map, isEqualLang);
				if (ml == null) {
					return matchLocale(oneThree, map, isEqualLang);
				} else {
					return ml;
				}
			} else if (parts.length == 2) {// zh-CN => zh
				for (int i = 0; i < LangUtil.oneTwoEquals.length; i++) {
					if (locale.equals(LangUtil.oneTwoEquals[i])) {
						out = (String) map.get(LangUtil.oneThreeEquals[i]);
						if (out != null) {
							return out;
						}
					}
				}

				for (int i = 0; i < LangUtil.oneThreeEquals.length; i++) {
					if (locale.equals(LangUtil.oneThreeEquals[i])) {
						out = (String) map.get(LangUtil.oneTwoEquals[i]);
						if (out != null) {
							return out;
						}
					}
				}

				return matchLocale(parts[0], map, isEqualLang);
			}
		} else if (isMaybeEqualLang) {
			for (int i = 0; i < LangUtil.equalLocale.length; i++) {
				if (locale.equals(LangUtil.equalLocale[i])) {
					out = (String) map.get(LangUtil.equalLocaleTo[i]);
					if (out != null) {
						return out;
					}
				}
			}

			for (int i = 0; i < LangUtil.equalLocaleTo.length; i++) {
				if (locale.equals(LangUtil.equalLocaleTo[i])) {
					out = (String) map.get(LangUtil.equalLocale[i]);
					if (out != null) {
						return out;
					}
				}
			}
		}

		if (locale.equals(LangUtil.EN_US)) {
			return null;
		} else {
			if (locale.equals("en")) {
				out = (String) map.get(LangUtil.EN_US);
				if (out != null) {
					return out;
				}
				return null;
			} else {
				return matchLocale(LangUtil.EN_US, map, false);
			}
		}
	}

	public static boolean isAnalysableParameter(final Object obj) {
		if (obj == null) {
			return true;
		}

		if (obj instanceof String || obj instanceof Boolean || obj instanceof Long || obj instanceof Byte || obj instanceof Short
				|| obj instanceof Integer || obj instanceof Float || obj instanceof Double || obj instanceof Character) {
			return true;
		}

		if (obj instanceof String[] || obj instanceof Boolean[] || obj instanceof Long[] || obj instanceof Byte[] || obj instanceof Short[]
				|| obj instanceof Integer[] || obj instanceof Float[] || obj instanceof Double[] || obj instanceof Character[]) {
			return true;
		}

		if (obj instanceof AnalysableRobotParameter) {
			return true;
		}

		return false;
	}

	public static String toLowerCaseFirstChar(final String s) {
		if (Character.isLowerCase(s.charAt(0)))
			return s;
		else {
			final StringBuilder sb = StringBuilderCacher.getFree();
			sb.append(Character.toLowerCase(s.charAt(0))).append(s.substring(1));
			final String out = sb.toString();
			StringBuilderCacher.cycle(sb);
			return out;
		}
	}

	public static byte[] buildFixLenBS(final byte[] srcBS, final int ivLen) {
		if (srcBS.length >= ivLen) {
			final byte[] newIV = new byte[ivLen];
			System.arraycopy(srcBS, 0, newIV, 0, ivLen);
			return newIV;
		} else {
			final byte[] newIV = new byte[ivLen];
			int startIdx = 0;
			while (startIdx < ivLen) {
				final int leftMax = ivLen - startIdx;
				System.arraycopy(srcBS, 0, newIV, startIdx, leftMax < srcBS.length ? leftMax : srcBS.length);
				startIdx += srcBS.length;
			}
			return newIV;
		}
	}

	public static void buildMenu() {
		TrayMenuUtil.buildMenu(UILang.getUsedLocale());
	}

	public static ImageIcon getHideIcon() {
		try {
			return new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/hide_22.png")));
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static String buildFirstUpcaseString(final String str) {
		if (str.length() == 0) {
			return str;
		}

		final String up_str = str.toUpperCase();
		return up_str.substring(0, 1) + str.substring(1);
	}

	private static Boolean isNonUIServer;// 有些线程没有权限
	
	/**
	 * Android服务器有可能从后台无UI模式切换到前台UI模式
	 */
	public static void changeToUIServer() {
		CCoreUtil.checkAccess();
		isNonUIServer = Boolean.FALSE;
		System.setProperty(ConfigManager.UI_IS_NON_UI_SERVER, IConstant.toString(isNonUIServer));//for debug report
	}
	
	public static void changeToNonUIServer() {
		CCoreUtil.checkAccess();
		isNonUIServer = Boolean.TRUE;
		System.setProperty(ConfigManager.UI_IS_NON_UI_SERVER, IConstant.toString(isNonUIServer));//for debug report
	}

	public static boolean isNonUIServer() {
		if (isNonUIServer == null) {
			isNonUIServer = PropertiesManager.isTrue(PropertiesManager.p_isNonUIServer, false);
			System.setProperty(ConfigManager.UI_IS_NON_UI_SERVER, IConstant.toString(isNonUIServer));//for debug report
		}
		return isNonUIServer;
	}

	public static boolean isEnableClientAddHAR() {
		return PropertiesManager.isTrue(PropertiesManager.p_isEnableClientAddHAR, true);
	}

	public static boolean isDemoServer() {
		return PropertiesManager.isTrue(PropertiesManager.p_isDemoServer, false);
	}

	public static boolean isDemoMaintenance() {
		return isDemoServer() && PropertiesManager.isTrue(PropertiesManager.p_isDemoMaintenance, false);
	}

	/**
	 * copy file only, not dir and recursive
	 * 
	 * @param from
	 * @param to
	 * @return true means OK
	 */
	public static boolean copy(final File from, final File to) {
		L.V = L.WShop ? false : LogManager.log("copy file : " + from.getAbsolutePath() + ", to : " + to.getAbsolutePath());

		FileInputStream in = null;
		FileOutputStream out = null;
		final byte[] buffer = ByteUtil.byteArrayCacher.getFree(1024 * 10);
		try {
			in = new FileInputStream(from);
			out = new FileOutputStream(to);
			int ins = 0;
			while ((ins = in.read(buffer)) != -1) {
				out.write(buffer, 0, ins);
			}
			return true;
		} catch (final Throwable e) {
			return false;
		} finally {
			ByteUtil.byteArrayCacher.cycle(buffer);

			try {
				in.close();
			} catch (final Exception e) {
			}
			try {
				out.flush();
			} catch (final Exception e) {
			}
			try {
				out.close();
			} catch (final Exception e) {
			}
		}
	}

	public static String getHideText() {
		return ResourceUtil.get(9179);
	}

	/**
	 * if fail return null.
	 * 
	 * @param file
	 * @return
	 */
	public static byte[] getContent(final File file) {
		InputStream ios;
		int length;
		try {
			ios = new FileInputStream(file);
			length = (int) file.length();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}

		return getContent(ios, length);
	}

	/**
	 * 
	 * @param ios
	 * @param length
	 *            0 means unknow.
	 * @return
	 */
	public static byte[] getContent(final InputStream ios, int length) {
		ByteArrayOutputStream ous = null;
		if (length == 0) {
			length = 1024 * 200;
		}

		final byte[] buffer = ByteUtil.byteArrayCacher.getFree(4096);
		try {
			ous = new ByteArrayOutputStream(length);
			int read = 0;
			while ((read = ios.read(buffer)) != -1) {
				ous.write(buffer, 0, read);
			}
		} catch (final Throwable e) {
			return null;
		} finally {
			ByteUtil.byteArrayCacher.cycle(buffer);
			try {
				if (ous != null)
					ous.close();
			} catch (final IOException e) {
			}

			try {
				if (ios != null)
					ios.close();
			} catch (final IOException e) {
			}
		}
		return ous.toByteArray();
	}

	public static String getStringFromURL(final String urlPath, final boolean keepReturnChar) {
		try {
			final URL url = new URL(urlPath);
			final URLConnection con = url.openConnection();
			con.setConnectTimeout(10 * 1000);
			con.setReadTimeout(10 * 1000);
			return getStringFromInputStream(con.getInputStream(), IConstant.UTF_8, keepReturnChar, false);
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return "";
	}

	public static final String toYYYYMMDD_HHMMSS(final long ms) {
		final StringBuilder sb = StringBuilderCacher.getFree();

		final Timestamp timestamp = new Timestamp(ms);
		sb.append((timestamp.getYear() + 1900));
		final int month = (timestamp.getMonth() + 1);
		if (month < 10) {
			sb.append('0');
		}
		sb.append(month);
		final int day = timestamp.getDate();
		if (day < 10) {
			sb.append('0');
		}
		sb.append(day);
		sb.append('_');
		final int hour = timestamp.getHours();
		if (hour < 10) {
			sb.append('0');
		}
		sb.append(hour);
		final int minute = timestamp.getMinutes();
		if (minute < 10) {
			sb.append('0');
		}
		sb.append(minute);
		final int second = timestamp.getSeconds();
		if (second < 10) {
			sb.append('0');
		}
		sb.append(second);

		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		return out;
	}

	/**
	 * 0 means unknow
	 * 
	 * @param file
	 * @return
	 */
	public static final long getFileCreateTime(final File file) {
		try {
			final Path filePath = file.toPath();
			final BasicFileAttributes attributes = Files.readAttributes(filePath, BasicFileAttributes.class);
			return attributes.creationTime().to(TimeUnit.MILLISECONDS);
		} catch (final Throwable e) {// 某些android不需要输出
		}
		return 0;
	}

	public static final boolean saveToFile(final InputStream is, final File file) {
		final byte[] buffer = ByteUtil.byteArrayCacher.getFree(1024);
		try {
			final OutputStream os = new FileOutputStream(file);
			int bytesRead = 0;
			while ((bytesRead = is.read(buffer, 0, buffer.length)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.close();
			is.close();
			return true;
		} catch (final Throwable e) {
			e.printStackTrace();
			return false;
		} finally {
			ByteUtil.byteArrayCacher.cycle(buffer);
		}
	}

	static final String rem_format1 = "//";

	public static String getStringFromInputStream(final InputStream is, final String charset, final boolean keepReturnChar,
			final boolean removeRem) {
		BufferedReader br = null;
		final StringBuilder sb = StringBuilderCacher.getFree();

		String line;
		try {
			br = new BufferedReader(new InputStreamReader(is, charset));
			while ((line = br.readLine()) != null) {
				if (removeRem) {
					final String lineTrim = line.trim();
					if (lineTrim.startsWith(rem_format1)) {
						continue;
					}
					if (lineTrim.indexOf(rem_format1) > 0) {
						LogManager.warning("please replace // with /**/ for rem!!!");
					}
				}
				sb.append(line);
				if (keepReturnChar) {
					sb.append("\n");
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

			try {
				is.close();
			} catch (final Throwable e) {
			}
		}
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);

		return out;
	}

	/**
	 * 清空指定目录，但保留目录不删除。
	 * 
	 * @param tmpDir
	 * @return
	 */
	public static boolean clearDir(final File tmpDir) {
		final File TrxFiles[] = tmpDir.listFiles();
		try {
			for (final File curFile : TrxFiles) {
				if (curFile.isDirectory()) {
					deleteDirectoryNow(curFile, true);
				} else {
					curFile.delete();
				}
			}
			return true;
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void copyDirAndSub(final File srcDir, final File targetDir) {
		if (targetDir.exists() == false) {
			targetDir.mkdirs();
		} else {
			deleteDirectoryNow(targetDir, false);
		}

		final File[] subs = srcDir.listFiles();
		final int length = subs.length;
		for (int i = 0; i < length; i++) {
			final File sub = subs[i];
			final String name = sub.getName();
			final File subTarget = new File(targetDir, name);
			if (sub.isDirectory()) {
				copyDirAndSub(sub, subTarget);
			} else {
				copy(sub, subTarget);
			}
		}
	}

	public static String getShowText() {
		return ResourceUtil.get(9180);
	}

	public static String getHideTip() {
		return ResourceUtil.get(9181);
	}

	public static String getShowTip() {
		return ResourceUtil.get(9186);
	}

	public static boolean refreshHideCheckBox(final JCheckBox checkBox, final JMenuItem hideIDForErrCert) {
		final boolean isHide = DefaultManager.isHideIDForErrCert();
		final String tip = "<html>" + ResourceUtil.get(9236) + (isHide ? getHideText() : getShowText()) + "<BR><BR>" + "<STRONG>"
				+ getHideText() + "</STRONG>&nbsp;" + getHideTip() + "<BR>" + "<STRONG>" + getShowText() + "</STRONG>&nbsp;" + getShowTip()
				+ "<BR><BR>" + StringUtil.replace(ResourceUtil.get(9212), "{disable}", ResourceUtil.get(1021)) + "</html>";

		final String hideCheckText;
		if (isHide) {
			hideCheckText = ResourceUtil.getShowText();
		} else {
			hideCheckText = ResourceUtil.getHideText();
		}
		final ImageIcon hideCheckIcon;
		if (isHide) {
			hideCheckIcon = ResourceUtil.getHideIcon();
		} else {
			hideCheckIcon = ResourceUtil.getShowIcon();
		}

		if (checkBox != null) {
			checkBox.setText(hideCheckText);
			checkBox.setIcon(hideCheckIcon);

			checkBox.setToolTipText(tip);
		}

		hideIDForErrCert.setText(hideCheckText);
		hideIDForErrCert.setIcon(hideCheckIcon);
		hideIDForErrCert.setToolTipText(tip);

		if (DefaultManager.isEnableTransNewCertNow()) {
			hideIDForErrCert.setEnabled(false);// 允许传送证书时，不能修改此项
		} else {
			hideIDForErrCert.setEnabled(true);
		}

		return isHide;
	}

	public static String refreshRootAlive() {
		final String token = TokenManager.getToken();
		final String hideToken = RootServerConnector.getHideToken();

		boolean hideIP = false;

		if (IConstant.isHCServerAndNotRelayServer()) {
			final boolean isEnableTrans = DefaultManager.isEnableTransNewCertNow();
			hideIP = isEnableTrans ? false : DefaultManager.isHideIDForErrCert();
		}

		return RootServerConnector.refreshRootAlive_impl(token, hideIP, hideToken);
	}

	public static HCTimer buildAliveRefresher(final CoreSession coreSS, final boolean isRootRelay) {
		// 每小时刷新alive变量到Root服务器
		// 采用58秒，能保障两小时内可刷新两次。

		long refreshMS = isRootRelay ? (1000 * 60 * 5) : RootConfig.getInstance().getLongProperty(RootConfig.p_RootDelNotAlive);
		if (refreshMS > HCTimer.ONE_DAY || refreshMS < HCTimer.ONE_MINUTE) {
			refreshMS = HCTimer.ONE_DAY;
		}
		return new HCTimer("AliveRefresher", refreshMS, true) {
			@Override
			public final void doBiz() {
				if (isRootRelay == false) {
					LogManager.log("refresh server online info.");
				}
				final String back = ResourceUtil.refreshRootAlive();
				if (back == null || (back.equals(RootServerConnector.ROOT_AJAX_OK) == false)) {
					if (isRootRelay) {
						// 服务器出现错误，需要进行重启服务
						LogManager.errToLog("fail notify Root Server Alive");
						//						coreSS.context.notifyShutdown();
						LogManager.flush();
						System.exit(1);
					} else {
						LogManager.errToLog("fail to refresh server online info, reconnect...");
						coreSS.notifyLineOff(false, false);
					}
				}
			}
		};
	}

	/**
	 * 
	 * @param isShow
	 *            true : is hide , false : is show
	 */
	public static void setHideIDForErrCertAndSave(final boolean isHide) {
		PropertiesManager.setValue(PropertiesManager.p_HideIDForErrCert, isHide ? IConstant.TRUE : IConstant.FALSE);
		PropertiesManager.saveFile();

		SafeDataManager.startSafeBackupProcess(true, false);

		ResourceUtil.refreshRootAlive();
	}

	public static ImageIcon getShowIcon() {
		try {
			return new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/show_22.png")));
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public final static Object moveToDoubleArraySize(final Object srcArray) {
		final int length = Array.getLength(srcArray);
		final Object newArray = Array.newInstance(srcArray.getClass().getComponentType(), length * 2);
		System.arraycopy(srcArray, 0, newArray, 0, length);
		return newArray;
	}

	public final static BufferedImage toBufferedImage(final Icon icon) {
		final BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics g = bi.createGraphics();
		icon.paintIcon(null, g, 0, 0);
		g.dispose();
		return bi;
	}

	/**
	 * 将window下的path转为unix标准格式
	 * 
	 * @param path
	 * @return
	 */
	public static String toStandardPath(final String path) {
		return path.replace(App.WINDOW_PATH_SEPARATOR, '/');
	}

	public static void delProjOptimizeDir(final String projID) {
		PlatformManager.getService().doExtBiz(PlatformService.BIZ_DEL_HAR_OPTIMIZE_DIR, USER_PROJ + projID);
	}

	public static int[] getSimuScreenSize() {
		// int[] out = {220, 240};
		// return out;
		if (ResourceUtil.isNonUIServer()) {
			final int[] out = { 1024, 768 };
			return out;
		}

		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final int[] out = { screenSize.width, screenSize.height };
		return out;
	}

	public static boolean validEmail(final String email) {
		final String email_pattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		final Pattern pattern = Pattern.compile(email_pattern);
		final Matcher m = pattern.matcher(email);
		return m.find();
	}

	public static boolean checkEmailID(final String donateIDStr, final Component parent) {
		if (donateIDStr.startsWith("0") == false && ResourceUtil.validEmail(donateIDStr) == false) {// 保留旧HomeCenterID支持
			App.showMessageDialog(parent, ResourceUtil.get(9073), ResourceUtil.get(IConstant.ERROR), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (IConstant.checkUUID(donateIDStr) == false) {
			App.showMessageDialog(parent, StringUtil.replace(ResourceUtil.get(9072), "{max}", "" + MsgBuilder.LEN_MAX_UUID_VALUE),
					ResourceUtil.get(IConstant.ERROR), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	public static synchronized String getUniqueTimeStamp() {
		try {
			Thread.sleep(10);// 足够可以错开
		} catch (final Exception e) {
		}

		final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		final StringBuffer timestampBuf = new StringBuffer(25);
		timestampBuf.append((timestamp.getYear() + 1900));
		final int month = (timestamp.getMonth() + 1);
		timestampBuf.append((month < 10 ? ("0" + month) : month));
		final int day = timestamp.getDate();
		timestampBuf.append(day < 10 ? ("0" + day) : day);
		final int hour = timestamp.getHours();
		timestampBuf.append("_");
		timestampBuf.append(hour < 10 ? ("0" + hour) : hour);
		final int minute = timestamp.getMinutes();
		timestampBuf.append((minute < 10 ? ("0" + minute) : minute));
		final int second = timestamp.getSeconds();
		timestampBuf.append((second < 10 ? ("0" + second) : second));
		final int nanos = timestamp.getNanos();
		final String zeros = "000000000";
		String nanosString;
		if (nanos == 0) {
			nanosString = "0";
		} else {
			nanosString = Integer.toString(nanos);

			nanosString = zeros.substring(0, (9 - nanosString.length())) + nanosString;

			final char[] nanosChar = new char[nanosString.length()];
			nanosString.getChars(0, nanosString.length(), nanosChar, 0);
			int truncIndex = 8;
			while (nanosChar[truncIndex] == '0') {
				truncIndex--;
			}

			nanosString = new String(nanosChar, 0, truncIndex + 1);
		}
		timestampBuf.append(nanosString);

		return timestampBuf.toString();
	}

	public static String getTimeStamp() {
		final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		final StringBuffer timestampBuf = new StringBuffer(25);
		timestampBuf.append((timestamp.getYear() + 1900));
		final int month = (timestamp.getMonth() + 1);
		timestampBuf.append((month < 10 ? ("0" + month) : month));
		final int day = timestamp.getDate();
		timestampBuf.append(day < 10 ? ("0" + day) : day);
		final int hour = timestamp.getHours();
		timestampBuf.append("_");
		timestampBuf.append(hour < 10 ? ("0" + hour) : hour);
		final int minute = timestamp.getMinutes();
		timestampBuf.append((minute < 10 ? ("0" + minute) : minute));
		final int second = timestamp.getSeconds();
		timestampBuf.append((second < 10 ? ("0" + second) : second));

		return timestampBuf.toString();
	}

	public static int getAbstractCtrlKeyMask() {
		if (isMacOSX()) {
			return KeyEvent.META_MASK;
		} else {
			return KeyEvent.CTRL_MASK;
		}
	}

	public static int getAbstractCtrlKeyCode() {// 注意：请与上段的Mask保持同步
		if (isMacOSX()) {
			return KeyEvent.VK_META;
		} else {
			return KeyEvent.VK_CONTROL;
		}
	}

	/**
	 * 替换串中如{1234}，{uuid}等内容
	 * 
	 * @param str
	 * @return
	 */
	public static String replaceWithI18N(String str) {
		// 替换{1234}为相应值
		while (true) {
			final String regua = "\\{.*?\\}";
			final Pattern p = Pattern.compile(regua);
			final Matcher ma = p.matcher(str);
			if (ma.find()) {
				final String g = ma.group();
				final String key = g.substring(1, g.length() - 1);
				if (key.equals("uuid")) {
					str = str.replace(g, IConstant.getUUID());
				} else {
					str = str.replace(g, ResourceUtil.get(Integer.parseInt(key)));
				}
			} else {
				break;
			}
		}

		return str;
	}

	public static int getAbstractCtrlInputEvent() {
		if (isMacOSX()) {
			return InputEvent.META_MASK;
		} else {
			return InputEvent.CTRL_MASK;
		}
	}

	public static KeyStroke getRefreshKeyStroke() {
		if (isMacOSX()) {
			return KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_MASK);
		} else {
			return KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
		}
	}

	public static String getRefreshKeyText() {
		if (isMacOSX()) {
			return getMacOSCommandKeyText() + " + R";
		} else {
			return "F5";
		}
	}

	public static String getAbstractCtrlInputEventText() {
		final int key = getAbstractCtrlInputEvent();

		if (key == InputEvent.META_MASK) {
			return getMacOSCommandKeyText();
		} else if (key == InputEvent.CTRL_MASK) {
			return "Ctrl";
		}
		return "";
	}

	public static String getAbstractCtrlKeyText() {
		return KeyComperPanel.getHCKeyText(getAbstractCtrlKeyCode());
	}

	public static String getMacOSCommandKeyText() {
		return String.valueOf((char) Integer.parseInt("8984"));// ⌘
	}

	public static String getMacOSOptionKeyText() {
		return "Option/Alt";
	}

	public static void buildAcceleratorKeyOnAction(final Action action, final int keyCode) {
		action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public static String toMD5(final byte tmp[]) {
		final char hexDigits[] = { // 用来将字节转换成 16 进制表示的字符
				'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		final char str[] = new char[16 * 2]; // 每个字节用 16 进制表示的话，使用两个字符，
		int k = 0; // 表示转换结果中对应的字符位置
		for (int i = 0; i < 16; i++) { // 从第一个字节开始，对 MD5 的每一个字节
			// 转换成 16 进制字符的转换
			final byte byte0 = tmp[i]; // 取第 i 个字节
			str[k++] = hexDigits[byte0 >>> 4 & 0xf]; // 取字节中高 4 位的数字转换,
														// >>> 为逻辑右移，将符号位一起右移
			str[k++] = hexDigits[byte0 & 0xf]; // 取字节中低 4 位的数字转换
		}
		return new String(str);
	}

	public static Object getResInUT(final int id, final Object threadToken) {
		return ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				return get(id);
			}
		}, threadToken);
	}

	/**
	 * 5000段，应用于AndroidServer
	 * 
	 * @param id
	 * @return
	 */
	public static String get(final int id) {
		if (id < MAX_RES_ID) {
			return UILang.getUILang(id);
		}
		return null;
	}

	/**
	 * 可同时用于用户级线程中。
	 * 
	 * @param coreSS
	 *            可以为null
	 * @param id
	 * @return
	 */
	public static String get(final CoreSession coreSS, final int id) {
		if (id < MAX_RES_ID) {
			if (coreSS == null) {
				return get(id);
			}
			return UILang.getUILang(UserThreadResourceUtil.getMobileLocaleFrom((J2SESession) coreSS), id);
		}
		return null;
	}
	
	public static String wrapHTMLTag(final String tip) {
		final StringBuilder sb = StringBuilderCacher.getFree();
		
		sb.append("<html>").append(tip).append("</html>");
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		
		return out;
	}
	
	public static String wrapHTMLTag(final int resID) {
		return wrapHTMLTag(get(resID));
	}
	
	private static final HashMap<Integer, I18NStoreableHashMapWithModifyFlag> i18nMap = new HashMap<Integer, I18NStoreableHashMapWithModifyFlag>(
			4);

	/**
	 * 返回全部可用的指定id的uilang的全部map
	 * 
	 * @param id
	 * @return 结果不为null。key = en-US(不是_), value = i18n(id)
	 */
	public static I18NStoreableHashMapWithModifyFlag getI18NByResID(final int id) {
		I18NStoreableHashMapWithModifyFlag out = i18nMap.get(id);
		if (out != null) {
			return out;
		}

		out = new I18NStoreableHashMapWithModifyFlag(32);
		try {
			final Pattern pattern = Pattern.compile(UILang.UI_LANG_FILE_NAME + "(\\w+)\\.properties$");
			final URL url = UILang.class.getResource(UILang.UI_LANG_FILE_NAME_PREFIX + "en_US.properties");
			final URI uri = url.toURI();
			if (uri != null && uri.getScheme().equals("jar")) {// 此条件支持android服务器
				Object pathurl;
				if (isAndroidServerPlatform()) {
					// jar:file:/data/app/homecenter.mobi.server-2.apk!/uilang_en_US.properties
					pathurl = "/";
				} else {
					pathurl = UILang.class.getProtectionDomain().getCodeSource().getLocation();
				}
				final String[] enterNames = PlatformManager.getService().listAssets(pathurl);
				for (int i = 0; i < enterNames.length; i++) {
					addItem(id, out, pattern, enterNames[i]);
				}
			} else {
				final File file_base = new File(uri).getParentFile();
				final File[] files = file_base.listFiles();
				for (int i = 0; i < files.length; i++) {
					final File file_temp = files[i];
					final String path = file_temp.getAbsolutePath();
					addItem(id, out, pattern, path);
				}

			}
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}

		i18nMap.put(id, out);
		return out;
	}

	private static void addItem(final int id, final I18NStoreableHashMapWithModifyFlag out, final Pattern pattern, final String path) {
		final Matcher matcher = pattern.matcher(path);
		if (matcher.find()) {
			final String local_ = matcher.group(1);
			final Hashtable table = UILang.buildResourceBundle(local_);
			final Object value = table.get(String.valueOf(id));
			if (value != null) {
				out.put(local_.replace('_', '-'), value);
				// LogManager.log(local_ + " : " + value);
			}
		}
	}

	private static final ClassLoader cldr = ResourceUtil.class.getClassLoader();

	public static URL getResource(final String fileName) {
		return cldr.getResource(fileName);
	}

	public static InputStream getResourceAsStream(final String fileName) {
		return cldr.getResourceAsStream(fileName);
	}
	
	public static void setDisableFieldHint(final JTextField field) {
		PlatformManager.getService().doExtBiz(PlatformService.BIZ_DISABLE_TEXTFIELD_HINT, field);
	}

	public static BufferedImage getImage(final URL url) {
		try {
			return ImageIO.read(url);
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static byte[] getAbsPathContent(final String path) throws Exception {
		return getResource(getAbsPathInputStream(path));
	}

	public static DataInputStream getAbsPathInputStream(final String path) throws Exception {
		URL uri = null;
		try {
			final Class startC = getStarterClass();
			if (startC != null) {
				uri = startC.getResource(path);
				return getInputStream(uri);
			}
		} catch (final Throwable e) {
		}

		try {
			return getInputStream(ResourceUtil.class.getResource(path));
		} catch (final Throwable e) {
		}

		uri = new File(ResourceUtil.getBaseDir(), "." + path).toURI().toURL();
		return getInputStream(uri);
	}

	private static DataInputStream getInputStream(final URL url) throws Exception {
		return new DataInputStream(url.openStream());
	}

	public static BufferedImage loadImage(final String imageFileName) {
		try {
			return ImageIO.read(ResourceUtil.getResource("hc/res/" + imageFileName));
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static byte[] getResource(final DataInputStream dataIs) throws Exception {
		final byte[] imgDataBa = new byte[dataIs.available()];
		// DataInputStream dataIs = new DataInputStream(uri.openStream());
		dataIs.readFully(imgDataBa);
		dataIs.close();
		return imgDataBa;
	}

	public static synchronized String createResourceID() {
		String p_id = PropertiesManager.getValue(PropertiesManager.p_ResourceID);
		if (p_id == null) {
			p_id = "1";
		} else {
			p_id = String.valueOf(Integer.parseInt(p_id) + 1);
		}
		PropertiesManager.setValue(PropertiesManager.p_ResourceID, p_id);
		PropertiesManager.saveFile();

		return p_id;
	}

	public static void notifySave() {
		PropertiesManager.setValue(PropertiesManager.p_ResourcesMaybeUnusedNew, "");
		removeUnused(PropertiesManager.p_ResourcesMaybeUnusedOld);
		PropertiesManager.saveFile();
	}

	public static void notifyCancel() {
		PropertiesManager.setValue(PropertiesManager.p_ResourcesMaybeUnusedOld, "");
		removeUnused(PropertiesManager.p_ResourcesMaybeUnusedNew);
		PropertiesManager.saveFile();
	}

	private static void removeUnused(final String pNew) {
		final String rr = PropertiesManager.getValue(pNew);
		if (rr == null) {
			return;
		}
		String baseDir = null;
		try {
			// 注意：服务器停止使用System.getProperty("user.dir")
			baseDir = ResourceUtil.getBaseDir().getCanonicalPath();// System.getProperty("user.dir");
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		final String[] toRemove = rr.split(";");
		for (int i = 0; i < toRemove.length; i++) {
			final String url = toRemove[i];
			if (url.length() > 0) {
				if (url.indexOf("hc/res/") >= 0) {
					continue;
				}
				final File file = new File(baseDir + url);
				// 强制释放可能没有关闭的资源。
				System.gc();
				final boolean b = file.delete();
				// 强制释放可能没有关闭的资源。
				System.gc();
				// System.out.println("Del ico : " + file.toString() + ",
				// succe:" + b);
			}
		}
		PropertiesManager.setValue(pNew, "");
		PropertiesManager.saveFile();
	}

	public static void addMaybeUnusedResource(final String url, final boolean isNew) {
		String p = null;
		if (isNew) {
			p = PropertiesManager.p_ResourcesMaybeUnusedNew;
		} else {
			p = PropertiesManager.p_ResourcesMaybeUnusedOld;
		}
		addUnused(url, p);
	}

	private static void addUnused(final String url, final String p_new) {
		String rr = PropertiesManager.getValue(p_new);
		if (rr == null) {
			rr = "";
		}
		if (rr.length() == 0) {
			rr = url;
		} else {
			rr += ";" + url;
		}
		PropertiesManager.setValue(p_new, rr);
		PropertiesManager.saveFile();
	}

	// public static void loadJar(File file){
	// PlatformManager.getService().addSystemLib(file);
	// }

	public static BufferedImage resizeImage(final BufferedImage bufferedimage, final int w, final int h) {
		return PlatformManager.getService().resizeImage(bufferedimage, w, h);
	}

	/**
	 * 去掉全通透部分，改半透明为底白效果，即不透明
	 * 
	 * @param src
	 * @return
	 */
	public static BufferedImage unAlphaImage(final BufferedImage src) {
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] data = new int[w * h];
		src.getRGB(0, 0, w, h, data, 0, w);
		for (int i = 0; i < data.length; i++) {
			final int rgb = data[i];
			if ((rgb | 0x00FFFFFF) != 0x00FFFFFF) {
				data[i] = 0xFFFFFFFF;
			}
		}
		final BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		out.setRGB(0, 0, w, h, data, 0, w);
		final Graphics g = out.getGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return out;
	}

	public static boolean isJ2SELimitFunction() {
		return isStandardJ2SEServer();
	}

	public static boolean isStandardJ2SEServer() {
		final String osPlatform = System.getProperty(CCoreUtil.SYS_SERVER_OS_PLATFORM);
		return (osPlatform == null);
	}

	public static boolean isAndroidServerPlatform() {
		final String androidplatform = System.getProperty(CCoreUtil.SYS_SERVER_OS_PLATFORM);
		if (androidplatform != null) {
			return androidplatform.equalsIgnoreCase(CCoreUtil.SYS_SERVER_OS_ANDROID_SERVER);
		}
		return false;
	}

	public static String getUserAgentForHAD() {
		return PlatformManager.getService().getOsNameAndVersion() + " J2SE/" + App.getJREVer() + " HomeCenter/"
				+ StarterManager.getHCVersion() + " JRuby/" + getJRubyVersion();// 如果没安装，则返回null；可能联机升级，所以不cache
	}

	public static boolean isWindowsOS() {
		final String os = System.getProperty("os.name");
		return os.toLowerCase().indexOf("windows") >= 0;
	}

	public static boolean isLinuxRelease(final String issue) {
		final String lowIssue = issue.toLowerCase();
		boolean isRelease = false;
		try {
			final Process process = Runtime.getRuntime().exec("lsb_release -a");// cat
																				// /etc/issue
			final InputStreamReader ir = new InputStreamReader(process.getInputStream());
			final LineNumberReader input = new LineNumberReader(ir);
			String line;
			while ((line = input.readLine()) != null) {
				if ((isRelease == false) && (line.toLowerCase().indexOf(lowIssue) >= 0)) {
					isRelease = true;
				}
			}
		} catch (final Throwable e) {
		}
		return isRelease;
	}

	public static boolean isLinux() {
		final String os = System.getProperty("os.name");
		return os.toLowerCase().indexOf("linux") >= 0;
	}

	public static boolean isOpenJDK() {
		final String os = System.getProperty("java.vm.name");// OpenJDK Client
																// VM or OpenJDK
																// 64-Bit Server
																// VM
		return os != null && os.indexOf("OpenJDK") >= 0;
	}

	public static boolean isWindowsXP() {
		final String os = System.getProperty("os.name");
		return os.equals("Windows XP");
	}

	public static boolean isWindows2003() {
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("windows") >= 0) && (os.indexOf("2003") > 0);
	}

	public static boolean isWindowsVista() {
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("windows vista") >= 0);
	}

	public static boolean isWindows2008() {
		// Windows Server 2008
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("windows") >= 0) && (os.indexOf("2008") > 0);
	}

	public static boolean isMacOSX() {
		// Mac OS X
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("mac") >= 0);// 改mac os x => mac
	}

	public static Properties loadThirdLibs() {
		final Properties thirdlibs = new Properties();
		final String url = RootServerConnector.AJAX_HTTPS_44X_URL_PREFIX + "thirdlib.php";
		if (L.isInWorkshop) {
			LogManager.log("try get download online lib information from : " + url);
		}
		try {
			loadFromURL(thirdlibs, url, null);
		} catch (final Exception e) {
		}
		if (thirdlibs.isEmpty()) {
			App.showMessageDialog(null, "Can NOT connect HomeCenter, please try after few seconds!", "Error Connect",
					JOptionPane.ERROR_MESSAGE);
		}
		return thirdlibs;
	}

	public static void loadFromURL(final Properties thirdlibs, String url, final String userAgent) throws Exception {
		url = HttpUtil.replaceSimuURL(url, PropertiesManager.isSimu());
		final String libs = HttpUtil.getAjax(url, userAgent);
		if (libs == null) {
			return;
		}
		thirdlibs.load(new StringReader(libs));// 否则new
												// URL(url).openStream()会出现字符集不正确
	}

	public static BufferedImage rotateImage(final BufferedImage bufferedimage, final int degree) {
		final int w = bufferedimage.getWidth();
		final int h = bufferedimage.getHeight();
		// int type = bufferedimage.getColorModel().getTransparency();
		BufferedImage img;
		Graphics2D graphics2d;
		(graphics2d = (img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)).createGraphics())
				.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2d.rotate(Math.toRadians(degree), w / 2, h / 2);
		graphics2d.drawImage(bufferedimage, 0, 0, null);
		graphics2d.dispose();
		return img;
	}

	/**
	 * 水平翻转
	 * 
	 * @param bufferedImage
	 * @return
	 */
	public static BufferedImage flipHorizontalJ2D(final BufferedImage bufferedimage) {
		final int w = bufferedimage.getWidth();
		final int h = bufferedimage.getHeight();
		BufferedImage img;
		Graphics2D graphics2d;
		(graphics2d = (img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)).createGraphics()).drawImage(bufferedimage, 0, 0, w, h, w,
				0, 0, h, null);
		graphics2d.dispose();
		return img;
	}

	public static Dimension getScreenSize() {
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		return toolkit.getScreenSize();
	}

	public static final Icon toIcon(final BufferedImage bi) {
		return new ImageIcon(bi);
	}

	public static BufferedImage toBufferedImage(final Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		final BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		final Graphics2D g = bimage.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();

		return bimage;
	}
	
	/**
	 * 
	 * @param prefix
	 * @param suffix may be null, in which case the suffix ".tmp" will be used
	 * @return
	 */
	public static final File createTempFile(final String prefix, final String suffix) {
		try {
			return File.createTempFile(prefix, suffix, StoreDirManager.TEMP_DIR);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static final int RANDOM_DIR_MAX = 99999999;

	/**
	 * 注意：与HCAndroidStarter同步
	 * 
	 * @param parent
	 * @param ext
	 *            with ., such as ".har", ".had"
	 * @return
	 * @see ResourceUtil#createDateTimeSerialUUID()
	 */
	public static String createRandomFileNameWithExt(final File parent, final String ext) {
		while (true) {
			final int r = random.nextInt(RANDOM_DIR_MAX);//注意：请勿修改此算法，它与ResourceUtil.isRandomNumDir保持一致
			final String str_r = String.valueOf(r) + ext;
			File file_r;
			if (parent == null) {
				file_r = new File(ResourceUtil.getBaseDir(), str_r);
			} else {
				// parent.mkdirs();//未经充分测试，可能存在用户线程操作系统目录
				file_r = new File(parent, str_r);
			}
			if (file_r.exists() == false) {
				return str_r;
			}
		}
	}
	
	public static boolean isRandomNumDir(final File dir) {
		try {
			return Integer.parseInt(dir.getName()) < RANDOM_DIR_MAX;
		}catch (final Throwable e) {
		}
		return false;
	}

	/**
	 * @param parent
	 * @param ext
	 *            with ., such as ".har", ".had"，可以为null
	 * @return
	 */
	public static File createRandomFileWithExt(final File parent, final String ext) {
		while (true) {
			final int r = random.nextInt(99999999);
			String str_r = String.valueOf(r);
			if (ext != null) {
				str_r += ext;
			}
			File file_r;
			if (parent == null) {
				file_r = new File(ResourceUtil.getBaseDir(), str_r);
			} else {
				parent.mkdirs();// 经充分测试
				file_r = new File(parent, str_r);
			}
			if (file_r.exists() == false) {
				return file_r;
			}
		}
	}

	public static String createRandomVariable(final int length, final int startR) {
		final char[] chars = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
				'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
				'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '_' };
		final int charsLen = chars.length;

		final char[] out = new char[length];
		try {
			Thread.sleep(20);
		} catch (final Exception e) {
		}
		long rand = System.currentTimeMillis() + startR;
		for (int i = 0; i < length; i++) {
			final Random random = new Random();
			random.setSeed(rand);
			final int r = random.nextInt(99999999);
			rand += r;
			out[i] = chars[r % charsLen];
		}

		return String.valueOf(out);
	}

	public static byte[] getMD5Bytes(final String src) {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(ByteUtil.getBytes(src, IConstant.UTF_8));
			return digest.digest();
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return new byte[0];
	}

	public static byte[] getMD5Bytes(final byte[] src, final int off, final int len) {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(src, off, len);
			return digest.digest();
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return new byte[0];
	}

	public static String getMD5(final String src) {
		final byte[] bs = getMD5Bytes(src);

		if (bs.length == 0) {
			return "";
		} else {
			return convertMD5BytesToString(bs);
		}
	}

	public static String getMD5(final byte[] src) {
		return getMD5(src, 0, src.length);
	}

	public static String getMD5(final byte[] src, final int off, final int len) {
		final byte[] bs = getMD5Bytes(src, off, len);

		if (bs.length == 0) {
			return "";
		} else {
			return convertMD5BytesToString(bs);
		}
	}

	public static String getMD5(final File file) {
		final String algorithm = "MD5";
		return getCheckSum(file, algorithm);
	}

	private static String getCheckSum(final File file, final String algorithm) {
		MessageDigest digest = null;
		FileInputStream in = null;
		final byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance(algorithm);
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			return convertMD5BytesToString(digest.digest());
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		} finally {
			try {
				in.close();
			} catch (final Throwable e) {
			}
		}
		return "";
	}

	public static String getSHA512(final File file) {
		final String algorithm = "SHA-512";
		return getCheckSum(file, algorithm);
	}

	private static String convertMD5BytesToString(final byte[] result) {
		final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		final int j = result.length;
		final char str[] = new char[j * 2];
		int k = 0;
		for (int i = 0; i < j; i++) {
			final byte byte0 = result[i];
			str[k++] = hexDigits[byte0 >>> 4 & 0xf];
			str[k++] = hexDigits[byte0 & 0xf];
		}
		return new String(str);
	}

	public static final String EXT_APK = ".apk";
	public static final String EXT_DEX_JAR = ".dex.jar";
	public static final String EXT_JAR = ".jar";

	public static final boolean deleteDirectoryNow(final File directory, final boolean isRemoveDirAlso) {
		// CCoreUtil.checkAccess();//projectCtx.removeDB is using

		if (directory.exists()) {
			final File[] files = directory.listFiles();
			if (null != files) {
				for (int i = 0; i < files.length; i++) {
					final File file = files[i];
					if (file.isDirectory()) {
						deleteDirectoryNow(file, true);
					} else {
						if (file.delete() == false) {
							LogManager.errToLog("fail del file : " + file.getAbsolutePath());
						}
					}
				}
			}

			if (isRemoveDirAlso) {
				final boolean isDel = directory.delete();
				if (isDel == false) {
					LogManager.errToLog("fail del dir/file : " + directory.getAbsolutePath());
				}
				return isDel;
			} else {
				return false;
			}
		}
		return true;
	}

	public static int getIntervalSecondsForNextStartup() {
		try {
			return Integer.parseInt(PropertiesManager.getValue(PropertiesManager.p_intervalSecondsNextStartup,
					DefaultManager.INTERVAL_SECONDS_FOR_NEXT_STARTUP));
		} catch (final Exception e) {
			return 5;
		}
	}

	public static int getSecondsForPreloadJRuby() {
		final String preloadAfterStartup = PropertiesManager.getValue(PropertiesManager.p_preloadAfterStartup,
				DefaultManager.PRELOAD_AFTER_STARTUP_FOR_INPUT);
		int seconds = 0;
		try {
			seconds = Integer.parseInt(preloadAfterStartup);
		} catch (final Throwable e) {
		} finally {
			if (seconds < 0) {
				seconds = -1;
			}
		}
		return seconds;
	}

	public static boolean writeToFile(final byte[] bfile, final File fileName) {
		return writeToFile(bfile, 0, bfile.length, fileName);
	}

	public static boolean writeToFile(final byte[] bfile, final int off, final int len, final File fileName) {
		BufferedOutputStream bos = null;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileName);
			bos = new BufferedOutputStream(fos);
			bos.write(bfile, off, len);
			return true;
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (final IOException e) {
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (final IOException e) {
				}
			}
		}
		return false;
	}

	/**
	 * 手机或服务器能否发布WiFi广播
	 * 
	 * @return
	 */
	public static final boolean canCtrlWiFi(final J2SESession coreSS) {
		return UserThreadResourceUtil.getMobileAgent(coreSS).ctrlWiFi() || WiFiDeviceManager.getInstance(coreSS).canCreateWiFiAccount();
	}

	public static String getLibNameForAllPlatforms(final String libName) {
		final boolean isAndroid = isAndroidServerPlatform();
		final String newName = libName + (isAndroid ? EXT_DEX_JAR : EXT_JAR);

		if (isAndroid) {
			final File jruby1_7_3_in_android = new File(getBaseDir(), "jruby.jar" + EXT_APK);// 旧采用roboto-X.apk方式
			if (jruby1_7_3_in_android.exists()) {
				jruby1_7_3_in_android.renameTo(new File(getBaseDir(), newName));
			}
		}
		return newName;
	}

	public static boolean isEnableDesigner() {
		return isJ2SELimitFunction();
	}

	public static File getTempFileName(final String extFileName) {
		return createRandomFileWithExt(StoreDirManager.TEMP_DIR, extFileName);
	}

	/**
	 * 必须在用户线程
	 * 
	 * @param icon
	 * @return
	 */
	public static Icon clearIcon(final Icon icon) {
		if (icon == null) {
			return null;
		}

		try {
			return toIcon(toBufferedImage(icon));
		} catch (final Throwable e) {
		}
		return null;
	}

	public static URL getBCLURL() {
		return getResource("hc/res/" + PlatformManager.getService().doExtBiz(PlatformService.BIZ_BCL, null));
	}

	private final static long startMS = System.currentTimeMillis();

	public final static long getStartMS() {
		CCoreUtil.checkAccess();
		return startMS;
	}

	private static File baseDir;

	public static File getBaseDir() {
		if (baseDir == null) {
			baseDir = PlatformManager.getService().getBaseDir();
		}
		return baseDir;
	}

	/**
	 * 将 s 进行 BASE64 编码
	 * 
	 * @param s
	 * @return
	 * @see #getFromBASE64(String)
	 */
	public static String getBASE64(final String s) {
		if (s == null)
			return null;
		try {
			return ByteUtil.encodeBase64(s.getBytes(IConstant.UTF_8));
		} catch (final UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static boolean isLoggerOn() {
		return PropertiesManager.isTrue(PropertiesManager.p_IsLoggerOn, true);
	}

	public static String getInfoI18N() {
		return get(IConstant.INFO);
	}
	
	public static String getHintI18N() {
		return get(IConstant.HINT);
	}

	public static String getWarnI18N() {
		return get(IConstant.WARN);
	}

	public static String getErrorI18N() {
		return get(IConstant.ERROR);
	}

	public static String getConfirmI18N() {
		return get(IConstant.CONFIRMATION);
	}

	public static String getYesI18N() {
		return get(1032);
	}

	public static String getNoI18N() {
		return get(1033);
	}

	public static String getHintI18N(final J2SESession coreSS) {
		return get(coreSS, IConstant.HINT);
	}
	
	public static String getInfoI18N(final J2SESession coreSS) {
		return get(coreSS, IConstant.INFO);
	}

	public static String getWarnI18N(final J2SESession coreSS) {
		return get(coreSS, IConstant.WARN);
	}

	public static String getErrorI18N(final J2SESession coreSS) {
		return get(coreSS, IConstant.ERROR);
	}

	/**
	 * 
	 * @param className
	 * @return true, className包含系统保留的包名
	 */
	public static boolean checkSysPackageName(final String className) {
		return className.startsWith("hc.", 0) || className.startsWith("java.", 0) || className.startsWith("javax.", 0);
	}

	/**
	 * 
	 * @param file
	 * @return true, className包含系统保留的包名
	 */
	public static boolean checkSysPackageNameInJar(final File file) {
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(file, false);
			final Enumeration<JarEntry> entrys = jarFile.entries();
			while (entrys.hasMoreElements()) {
				final JarEntry jarEntry = entrys.nextElement();
				String entryName = jarEntry.getName();
				if (entryName.endsWith(".class")) {
					entryName = entryName.replace("/", ".");
					if (checkSysPackageName(entryName)) {
						LogManager.errToLog("class [" + entryName + "] has reserved package name.");
						return true;
					}
				}
			}
		} catch (final Throwable e) {
		} finally {
			try {
				jarFile.close();
			} catch (final Throwable e) {
			}
		}
		return false;
	}

	public static final String FILE_IS_MODIFIED_AFTER_SIGNED = "file is incomplete or modified after signed";
	public static final String RESERVED_PACKAGE_NAME_IS_IN_HAR = "reserved package name [hc/java/javax] is in HAR!";
	public static final String HAR_PROJECT_FILE_IS_CORRUPTED = "HAR project file is corrupted or incomplete.";

	public static String getErrProjIsDeledNeedRestart(final J2SESession coreSS) {
		return ResourceUtil.get(coreSS, 9271);// project is removed, restart
												// server and add again!
	}

	public static void generateCertForNullOrError() {
		CCoreUtil.checkAccess();

		App.generateCertAndSave();
		PropertiesManager.setValue(PropertiesManager.p_EnableTransNewCertKeyNow, IConstant.TRUE);

		App.setNoTransCert();
	}

	/**
	 * 检查stackTrace的类源都是系统包
	 */
	public static final void checkHCStackTrace() {
		ResourceUtil.checkHCStackTraceInclude(null, null);
	}

	/**
	 * 检查stackTrace的类源都是系统包，且callerClass非null时，必须在stackTrace中。否则抛出异常。
	 * 
	 * @param callerClass
	 * @param loader
	 *            使用指定的类加载器来检查类
	 */
	public static final void checkHCStackTraceInclude(final String callerClass, final ClassLoader loader) {
		checkHCStackTraceInclude(callerClass, loader, null);
	}

	/**
	 * 检查stackTrace的类源都是系统包，且callerClass非null时，必须在stackTrace中。否则抛出异常。
	 * 
	 * @param callerClass
	 * @param loader
	 *            使用指定的类加载器来检查类
	 * @param moreMsg
	 */
	public static final void checkHCStackTraceInclude(final String callerClass, final ClassLoader loader, final String moreMsg) {
		checkHCStackTraceInclude(callerClass, loader, moreMsg, null);
	}

	/**
	 * 检查stackTrace的类源都是系统包，且callerClass非null时，必须在stackTrace中。否则抛出异常。
	 * 
	 * @param callerClass
	 * @param loader
	 *            使用指定的类加载器来检查类
	 * @param moreMsg
	 */
	public static final void checkHCStackTraceInclude(final String callerClass, final ClassLoader loader, final String moreMsg,
			final String hclimitSecurityClassName) {
		final StackTraceElement[] el = Thread.currentThread().getStackTrace();// index越小，距本方法越近
		final ClassLoader checkLoader = (loader == null ? ResourceUtil.class.getClassLoader() : loader);
		boolean isFromCallerClass = false;
		int countInvokehclimitSecurityClass = 0;

		for (int i = el.length - 1; i >= 0; i--) {
			String className = el[i].getClassName();
			final int dollarIdx = className.indexOf('$', 0);
			if (dollarIdx > 0) {
				className = className.substring(0, dollarIdx);
			}
			// if(className.equals("sun.reflect.GeneratedMethodAccessor3")
			// || className.equals("sun.reflect.GeneratedMethodAccessor2")){
			// return;
			// }
			if (hclimitSecurityClassName != null && className.equals(hclimitSecurityClassName)) {
				if (countInvokehclimitSecurityClass++ > 4) {
					return;
				}
			}
			if (className.equals("org.jruby.embed.internal.EmbedEvalUnitImpl")) {// 动态解释执行
				if (PropertiesManager.isSimu()) {
					LogManager.errToLog("Illegal class [" + className + "] is NOT allowed in stack trace in ClassLoader["
							+ checkLoader.toString() + "].");
				}
				if (moreMsg != null) {
					LogManager.errToLog(moreMsg);
				}
				throw new HCSecurityException(PropertiesManager.ILLEGAL_CLASS);
			}
			try {
				Class.forName(className, false, checkLoader);
			} catch (final Exception e) {
				if (PropertiesManager.isSimu()) {
					LogManager.errToLog("Illegal class [" + className + "] is NOT allowed in stack trace in ClassLoader["
							+ checkLoader.toString() + "].");
				}
				// panel
				// App.showCenterPanel(panel, 0, 0, ResourceUtil.getErrorI18N(),
				// false, null, null, null, null, null, false, true, null,
				// false, true);
				if (moreMsg != null) {
					LogManager.errToLog(moreMsg);
				}
				throw new HCSecurityException(PropertiesManager.ILLEGAL_CLASS);
			}
			if (callerClass != null && className.equals(callerClass)) {
				isFromCallerClass = true;
			}
		}

		if (callerClass != null && isFromCallerClass == false) {
			if (PropertiesManager.isSimu()) {
				LogManager.errToLog(
						"Class [" + callerClass + "] should be in stack trace in ClassLoader[" + checkLoader.toString() + "], but NOT.");
			}
			if (moreMsg != null) {
				LogManager.errToLog(moreMsg);
			}
			throw new HCSecurityException(PropertiesManager.ILLEGAL_CLASS);
		}
	}

	/**
	 * 将 BASE64 编码的字符串 s 进行解码
	 * 
	 * @param s
	 * @return
	 * @see #getBASE64(String)
	 */
	public static String getFromBASE64(final String s) {
		if (s == null)
			return null;
		try {
			final byte[] b = ByteUtil.decodeBase64(s);
			return new String(b, IConstant.UTF_8);
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			return null;
		}
	}

	public static String getDefaultWordCompletionKeyChar() {
		return isMacOSX() ? "÷" : "";
	}

	public static String buildUUID() {
		return UUID.randomUUID().toString();
	}

	public static void buildRandom(final byte[] bs) {
		for (int i = 0; i < bs.length; i++) {
			bs[i] = (byte) (random.nextInt() & 0xFF);
		}
	}

	/**
	 * 返回 日期时间+最长6位的随机数
	 */
	public synchronized static String createDateTimeSerialUUID() {
		calendarForRandom.setTimeInMillis(System.currentTimeMillis());
		stringBuilderForRandom.append(calendarForRandom.get(Calendar.YEAR));
		stringBuilderForRandom.append((calendarForRandom.get(Calendar.MONTH) + 1));
		stringBuilderForRandom.append(calendarForRandom.get(Calendar.DAY_OF_MONTH));
		stringBuilderForRandom.append(calendarForRandom.get(Calendar.HOUR_OF_DAY));
		stringBuilderForRandom.append(calendarForRandom.get(Calendar.MINUTE));
		stringBuilderForRandom.append(calendarForRandom.get(Calendar.SECOND));
		stringBuilderForRandom.append(calendarForRandom.get(Calendar.MILLISECOND));
		stringBuilderForRandom.append('_');
		stringBuilderForRandom.append(random.nextInt(999999));

		final String result = stringBuilderForRandom.toString();
		stringBuilderForRandom.setLength(0);
		return result;
	}

	public static void sendToClipboard(final String text) {
		final Clipboard clipbd = getClipboard();
		final StringSelection clipString = new StringSelection(text);
		clipbd.setContents(clipString, clipString);
	}

	public static Clipboard getClipboard() {
		return Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	public static String getTxtFromClipboard() {
		final Clipboard clipbd = getClipboard();
		final Transferable clipT = clipbd.getContents(null);
		String d = null;
		if (clipT != null) {
			// 检查内容是否是文本类型
			if (clipT.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				try {
					d = (String) clipT.getTransferData(DataFlavor.stringFlavor);
				} catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		}
		if (d == null) {
			return "";
		} else {
			return d.trim();
		}
	}

	/**
	 * 
	 * @param host
	 * @param timeout
	 *            the time, in milliseconds, before the call aborts
	 * @return
	 */
	public static NetworkInterface searchReachableNetworkInterface(final String host, final int timeout) {
		try {
			final InetAddress ia = InetAddress.getByName(host);
			final Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				final NetworkInterface nextElement = e.nextElement();
				if (ia.isReachable(nextElement, 100, timeout)) {
					return nextElement;
				}
			}
		} catch (final Throwable e) {
		}
		return null;
	}

	public static boolean canCreateWiFiAccountOnPlatform(final WiFiDeviceManager platManager) {
		return platManager != null && platManager.hasWiFiModule() && platManager.canCreateWiFiAccount();
	}

	/**
	 * 没有找到返回null
	 * 
	 * @param domainOrIP
	 * @return
	 */
	public static InetAddress searchReachableInetAddress(final String domainOrIP) {
		final int timeout = 2000;
		NetworkInterface availableNI = searchReachableNetworkInterface(domainOrIP, timeout);

		int total = 0;
		while (availableNI == null && total < 5000) {
			try {
				total += 500;
				Thread.sleep(500);
			} catch (final Exception e) {
			}

			availableNI = searchReachableNetworkInterface(domainOrIP, timeout);
		}

		if (availableNI != null) {
			InetAddress out = HttpUtil.filerInetAddress(availableNI, false);
			if (out == null) {
				out = HttpUtil.filerInetAddress(availableNI, true);
			}
			return out;
		} else {
			LogManager.errToLog("[" + domainOrIP + "] is unreachable via ICMP ECHO REQUEST!");
		}

		return null;
	}

	public static String getProductName() {
		return get(UILang.PRODUCT_NAME);
	}

	public static void doCopyShortcutForMac() {
		if (isMacOSX()) {// JRE7缺省是正确的，但是不同Skin会导致被覆盖
			final UIDefaults defaultsUI = UIManager.getDefaults();
			for (final Enumeration keys = defaultsUI.keys(); keys.hasMoreElements();) {
				final Object key = keys.nextElement();

				if (key instanceof String) {
					final String keyStr = (String) key;
					if (keyStr.endsWith(".focusInputMap")) {
						final InputMap im = (InputMap) defaultsUI.get(keyStr);// "TextField.focusInputMap"
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);
					}
				}
			}
		}
	}

	public static BufferedImage standardMenuIconForAllPlatform(BufferedImage bi, final int toSize, final boolean roundWithSize) {
		final int cornDegree = 30;

		if (bi.getWidth() != toSize || bi.getHeight() != toSize) {
			bi = resizeImage(bi, toSize, toSize);
			if (roundWithSize) {
				bi = ImageSrc.makeRoundedCorner(bi, cornDegree);
			}
		}
		if (roundWithSize == false) {
			bi = ImageSrc.makeRoundedCorner(bi, cornDegree);
		}
		return bi;
	}

	public static String getElementIDFromTarget(final String target) {
		try {
			return target.substring(target.indexOf(HCURL.HTTP_SPLITTER) + HCURL.HTTP_SPLITTER.length());
		} catch (final Throwable e) {
			throw new MissingFormatArgumentException("invalid target : " + target);
		}
	}

	public static boolean isDisableUIForTest() {
		return PropertiesManager.isSimu();
	}

	public static Class loadClass(final String className, final boolean printWhenNoFound) {
		try {
			return Class.forName(className);
		} catch (final Throwable e) {
			try {
				return ResourceUtil.class.getClassLoader().loadClass(className);
			} catch (final Throwable e1) {
				if (printWhenNoFound) {
					e1.printStackTrace();// 不建议用ExceptionReporter
				}
			}
		}
		return null;
	}

	public static boolean needAccepLicense(final String licenseURL) {
		return PropertiesManager.isTrue(PropertiesManager.p_isAcceptAllHARLicenses, false) == false && licenseURL != null
				&& licenseURL.length() > 0;
	}

	public static String getJRubyVersion() {
		return PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);
	}

	public static String getOKI18N(final J2SESession coreSS) {
		return get(coreSS, 1010);
	}

	public static String getOKI18N() {
		return get(1010);
	}

	public static boolean isReceiveDeployFromLocalNetwork() {
		return PropertiesManager.isTrue(PropertiesManager.p_Deploy_EnableReceive, true);
	}

	public static String getDefaultSkin() {
		if (isWindowsOS()) {
			return LF_NIMBUS;// "Windows Classic";
		}
		return "";
	}

	public static final String LOAD_NATIVE_LIB = "load native lib";

	public static final String LOCATION_OF_MOBILE = "location of mobile";
	public static final String SCRIPT_PANEL = "ScriptPanel";

	/**
	 * 
	 * @param coreSS
	 *            有null情形
	 * @return
	 */
	public static String getSaveAndApply(final CoreSession coreSS) {
		return get(coreSS, 1017) + " + " + get(coreSS, 9041);
	}

	public static boolean isSystemMletOrDialog(final Mlet mlet) {
		return (mlet instanceof SystemHTMLMlet)
				|| (mlet instanceof DialogHTMLMlet) && ((DialogHTMLMlet) mlet).dialog instanceof SystemDialog;
	}

	public static final String SHUTDOWN_COMPACT = "SHUTDOWN COMPACT";
	private static final String SHUTDOWN = "SHUTDOWN";

	public static void shutdownHSQLDB(final Connection connection, final boolean isCompactAlso) {
		if (connection == null) {
			return;
		}

		try {
			final Statement state = connection.createStatement();
			if (isCompactAlso) {
				state.execute(SHUTDOWN_COMPACT);
			} else {
				state.execute(SHUTDOWN);
			}
			state.close();
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static final JPanel addSpaceBeforePanel(final JComponent content) {
		final JPanel out = new JPanel(new FlowLayout(FlowLayout.LEADING));
		out.add(new JLabel(" "));
		out.add(content);
		return out;
	}

	public static SimpleAttributeSet buildAttrSet(final Color c, final boolean bold) {
		final SimpleAttributeSet attributes = new SimpleAttributeSet();
		StyleConstants.setForeground(attributes, c);
		StyleConstants.setBold(attributes, bold);
		// StyleConstants.setFontSize(attributes, fontSize);
		// StyleConstants.setFontFamily(attrSet, "黑体");
		return attributes;
	}

	public static final void removeFromParent(final JComponent subPanel) {
		final Container parent = subPanel.getParent();
		if (parent != null) {
			parent.remove(subPanel);
		}
	}

	public static File createTempFileForHAR(final ProjectContext ctx, File parent, final String fileExtension) {
		if (parent == null) {
			parent = StoreDirManager.getTmpSubForUserManagedByHcSys(ctx);
		}
		try {
			return File.createTempFile("tmp", fileExtension == null ? null : ("." + fileExtension), parent);
		} catch (final Exception e) {
			return createRandomFileWithExt(parent, fileExtension == null ? null : ("." + fileExtension));
		}
	}

	public static boolean isResPath(final String path) {
		final int pathIdx = path.indexOf("/");
		return pathIdx == 0;
	}

	public static String toHHMMSS(final int timeSecond) {
		final int hour = timeSecond / 60 / 60;
		final int minute = (timeSecond - hour * 60) / 60;
		final int second = timeSecond % 60;
		return (hour > 9 ? String.valueOf(hour) : "0" + String.valueOf(hour)) + ":"
				+ (minute > 9 ? String.valueOf(minute) : "0" + String.valueOf(minute)) + ":"
				+ (second > 9 ? String.valueOf(second) : "0" + String.valueOf(second));
	}
}
