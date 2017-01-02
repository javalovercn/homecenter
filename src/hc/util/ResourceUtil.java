package hc.util;

import hc.App;
import hc.core.CoreSession;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.WiFiDeviceManager;
import hc.res.ImageSrc;
import hc.server.DefaultManager;
import hc.server.HCSecurityException;
import hc.server.J2SEContext;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.server.StarterManager;
import hc.server.TrayMenuUtil;
import hc.server.data.KeyComperPanel;
import hc.server.data.StoreDirManager;
import hc.server.msb.UserThreadResourceUtil;
import hc.server.ui.design.J2SESession;
import hc.server.util.HCLimitSecurityManager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.MissingFormatArgumentException;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;

public class ResourceUtil {
	private static final String USER_PROJ = "user.proj.";
	private static boolean isCheckStarter;
	private static Class starterClass;
	
	private static Class getStarterClass(){
		if(isCheckStarter == false){
			isCheckStarter = true;
			starterClass = loadClass(StarterManager.CLASSNAME_STARTER_STARTER, false);
		}
		return starterClass;
	}
	
	public static void buildMenu(){
		TrayMenuUtil.buildMenu(UILang.getUsedLocale());
	}
	
	public static ImageIcon getHideIcon(){
		try {
			return new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/hide_22.png")));
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}
	
	public static String buildFirstUpcaseString(final String str){
		if(str.length() == 0){
			return str;
		}
		
		final String up_str = str.toUpperCase();
		return up_str.substring(0, 1) + str.substring(1);
	}
	
	public static boolean isDemoServer(){
		return PropertiesManager.isTrue(PropertiesManager.p_isDemoServer, false);
	}
	
	/**
	 * copy file only, not dir and recursive
	 * @param from
	 * @param to
	 */
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
	
	public static String getHideText(){
		return (String)ResourceUtil.get(9179);
	}
	
	/**
	 * if fail return null.
	 * @param file
	 * @return
	 */
	public static byte[] getContent(final File file){
		ByteArrayOutputStream ous = null;
	    InputStream ios = null;
	    try {
	        final byte[] buffer = new byte[4096];
	        ous = new ByteArrayOutputStream();
	        ios = new FileInputStream(file);
	        int read = 0;
	        while ((read = ios.read(buffer)) != -1) {
	            ous.write(buffer, 0, read);
	        }
	    }catch (final Throwable e) {
	    	return null;
	    }finally {
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
		try{
			final URL url = new URL(urlPath);
			final URLConnection con = url.openConnection();
			con.setConnectTimeout(10 * 1000);
			con.setReadTimeout(10 * 1000);
			return getStringFromInputStream(con.getInputStream(), IConstant.UTF_8, keepReturnChar, false);
		}catch (final Throwable e) {
			e.printStackTrace();
		}
		return "";
	}
	
	static final String rem_format1 = "//";

	public static String getStringFromInputStream(final InputStream is, final String charset, final boolean keepReturnChar, final boolean removeRem) {
		BufferedReader br = null;
		final StringBuilder sb = StringBuilderCacher.getFree();

		String line;
		try {
			br = new BufferedReader(new InputStreamReader(is, charset));
			while ((line = br.readLine()) != null) {
				if(removeRem){
					final String lineTrim = line.trim();
					if(lineTrim.startsWith(rem_format1)){
						continue;
					}
					if(lineTrim.indexOf(rem_format1) > 0){
						LogManager.warning("please replace // with /**/ for rem!!!");
					}
				}
				sb.append(line);
				if(keepReturnChar){
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
			
			try{
				is.close();
			}catch (final Throwable e) {
			}
		}
		final String out = sb.toString();
		StringBuilderCacher.cycle(sb);
		
		return out;
	}
	
	/**
	 * 清空指定目录，但保留目录不删除。
	 * @param tmpDir
	 * @return
	 */
	public static boolean clearDir(final File tmpDir){
        final File TrxFiles[] = tmpDir.listFiles();
        try{
	        for(final File curFile:TrxFiles ){
	        	if(curFile.isDirectory()){
	        		deleteDirectoryNowAndExit(curFile);
	        	}else{
	        		curFile.delete();  
	        	}
	        }
	        return true;
        }catch (final Throwable e) {
        	e.printStackTrace();
        }
        return false;
    }
	
	public static String getShowText(){
		return (String)ResourceUtil.get(9180);
	}
	
	public static String getHideTip(){
		return (String)ResourceUtil.get(9181);
	}
	
	public static String getShowTip(){
		return (String)ResourceUtil.get(9186);
	}
	
	public static boolean refreshHideCheckBox(final JCheckBox checkBox, final JMenuItem hideIDForErrCert){
		final boolean isHide = DefaultManager.isHideIDForErrCert();
		final String tip = "<html>" +
				(String)ResourceUtil.get(9236) + (isHide?getHideText():getShowText()) + "<BR><BR>" +
				"<STRONG>" + getHideText() + "</STRONG>&nbsp;" + getHideTip() +
				"<BR>" +
				"<STRONG>" + getShowText() + "</STRONG>&nbsp;" + getShowTip() +
				"<BR><BR>" +
				StringUtil.replace((String)ResourceUtil.get(9212), "{disable}", (String)ResourceUtil.get(1021)) +
				"</html>";
		
		final String hideCheckText;
		if(isHide){
			hideCheckText = ResourceUtil.getShowText();
		}else{
			hideCheckText = ResourceUtil.getHideText();
		}
		final ImageIcon hideCheckIcon;
		if(isHide){
			hideCheckIcon = ResourceUtil.getHideIcon();
		}else{
			hideCheckIcon = ResourceUtil.getShowIcon();
		}
		
		if(checkBox != null){
			checkBox.setText(hideCheckText);
			checkBox.setIcon(hideCheckIcon);
			
			checkBox.setToolTipText(tip);
		}
		
		hideIDForErrCert.setText(hideCheckText);
		hideIDForErrCert.setIcon(hideCheckIcon);
		hideIDForErrCert.setToolTipText(tip);
		
		if(DefaultManager.isEnableTransNewCertNow()){
			hideIDForErrCert.setEnabled(false);//允许传送证书时，不能修改此项
		}else{
			hideIDForErrCert.setEnabled(true);
		}
		
		return isHide;
	}
	
	public static String refreshRootAlive() {
		final String token = TokenManager.getToken();
		final String hideToken = RootServerConnector.getHideToken();

		boolean hideIP = false;
		
		if(IConstant.isHCServerAndNotRelayServer()){
			final boolean isEnableTrans = DefaultManager.isEnableTransNewCertNow();
			hideIP = isEnableTrans?false:DefaultManager.isHideIDForErrCert();
		}
		
		return RootServerConnector.refreshRootAlive_impl(token, hideIP, hideToken);
	}
	
	public static HCTimer buildAliveRefresher(final CoreSession coreSS, final boolean isRoot){
		//每小时刷新alive变量到Root服务器
		//采用58秒，能保障两小时内可刷新两次。
		
		final int refreshMS = isRoot?(1000 * 60 * 5):RootConfig.getInstance().getIntProperty(RootConfig.p_RootDelNotAlive);
		return new HCTimer("AliveRefresher", refreshMS, true){
			@Override
			public final void doBiz() {
				if(isRoot == false){
					L.V = L.O ? false : LogManager.log("refresh server online info.");
				}
				final String back = ResourceUtil.refreshRootAlive();
				if(back == null || (back.equals(RootServerConnector.ROOT_AJAX_OK) == false)){
					if(isRoot){
						//服务器出现错误，需要进行重启服务
						LogManager.errToLog("fail notify Root Server Alive");
						coreSS.context.notifyShutdown();
					}else{
						LogManager.errToLog("fail to refresh server online info, reconnect...");
						SIPManager.notifyLineOff(coreSS, false, false);
					}
				}
			}};		
	}
	
	/**
	 * 
	 * @param isShow true : is hide , false : is show
	 */
	public static void setHideIDForErrCertAndSave(final boolean isHide){
		PropertiesManager.setValue(PropertiesManager.p_HideIDForErrCert, isHide?IConstant.TRUE:IConstant.FALSE);
		PropertiesManager.saveFile();
		
		ResourceUtil.refreshRootAlive();
	}
	
	public static ImageIcon getShowIcon(){
		try {
			return new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/show_22.png")));
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}
	
	public final static Object moveToDoubleArraySize(final Object srcArray){
		final int length = Array.getLength(srcArray);
		final Object newArray = Array.newInstance(srcArray.getClass().getComponentType(), length * 2);
		System.arraycopy(srcArray, 0, newArray, 0, length);
		return newArray;
	}
	
	public final static BufferedImage toBufferedImage(final Icon icon){
		final BufferedImage bi = new BufferedImage(icon.getIconWidth(),
				icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics g = bi.createGraphics();
		icon.paintIcon(null, g, 0, 0);
		g.dispose();
		return bi;
	}
	
	/**
	 * 将window下的path转为unix标准格式
	 * @param path
	 * @return
	 */
	public static String toStandardPath(final String path){
		return path.replace(App.WINDOW_PATH_SEPARATOR, '/');
	}

	private static ClassLoader buildProjClassPath(final File deployPath, final ClassLoader jrubyClassLoader, final String projID){
		final Vector<File> jars = new Vector<File>();
		final String[] subFileNames = deployPath.list();
		for (int i = 0; subFileNames != null && i < subFileNames.length; i++) {
			final String lowerCaseFileName = subFileNames[i].toLowerCase();
			if(lowerCaseFileName.endsWith(EXT_JAR) && (lowerCaseFileName.endsWith(EXT_DEX_JAR) == false)){
				jars.add(new File(deployPath, subFileNames[i]));
			}
		}
		final File[] jars_arr = new File[jars.size()];
		return PlatformManager.getService().loadClasses(jars.toArray(jars_arr), jrubyClassLoader, false, USER_PROJ + projID);
	}
	
	public static void delProjOptimizeDir(final String projID){
		PlatformManager.getService().doExtBiz(PlatformService.BIZ_DEL_HAR_OPTIMIZE_DIR, USER_PROJ + projID);
	}
	
	public static ClassLoader buildProjClassLoader(final File libAbsPath, final String projID){
		CCoreUtil.checkAccess();

		return buildProjClassPath(libAbsPath, ResourceUtil.getJRubyClassLoader(false), projID);
	}
	
	private static ClassLoader rubyAnd3rdLibsClassLoaderCache;
	
	public static synchronized ClassLoader getJRubyClassLoader(final boolean forceRebuild){
		CCoreUtil.checkAccess();

		if(rubyAnd3rdLibsClassLoaderCache == null || forceRebuild){
		}else{
			return rubyAnd3rdLibsClassLoaderCache;
		}
		
		PlatformManager.getService().closeLoader(rubyAnd3rdLibsClassLoaderCache);
				
		final File jruby = new File(ResourceUtil.getBaseDir(), J2SEContext.jrubyjarname);
		
		PlatformManager.getService().setJRubyHome(PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer), jruby.getAbsolutePath());
		
		final File[] files = {jruby};
		rubyAnd3rdLibsClassLoaderCache = PlatformManager.getService().loadClasses(files, PlatformManager.getService().get3rdClassLoader(null), true, "hc.jruby");
		if(rubyAnd3rdLibsClassLoaderCache == null){
			final String message = "Error to get JRuby/3rdLibs ClassLoader!";
			LogManager.errToLog(message);
			final JPanel panel = App.buildMessagePanel(message, App.getSysIcon(App.SYS_ERROR_ICON));
			App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getErrorI18N(), false, null, null, null, null, null, false, true, null, false, false);//JFrame
		}else{
			HCLimitSecurityManager.refreshJRubyClassLoader(rubyAnd3rdLibsClassLoaderCache);
			L.V = L.O ? false : LogManager.log("Successful (re) create JRuby engine classLoader.");
		}
		return rubyAnd3rdLibsClassLoaderCache;
	}
	
	public static int[] getSimuScreenSize(){
//		int[] out = {220, 240};
//		return out;
		if(ResourceUtil.isDemoServer()){
			final int[] out = {1024, 768};
			return out;
		}
		
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final int[] out = {screenSize.width, screenSize.height};
		return out;
	}
	
	public static boolean validEmail(final String email) {
		final String email_pattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
				+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
		final Pattern pattern = Pattern.compile(email_pattern);
		final Matcher m = pattern.matcher(email);
		return m.find();
	}
	
	public static boolean checkEmailID(final String donateIDStr, final Component parent){
		if (donateIDStr.startsWith("0") == false && ResourceUtil.validEmail(donateIDStr) == false) {//保留旧HomeCenterID支持
			App.showMessageDialog(parent,
					(String)ResourceUtil.get(9073),
					(String) ResourceUtil.get(IContext.ERROR),
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (IConstant.checkUUID(donateIDStr) == false) {
			App.showMessageDialog(parent, 
					StringUtil.replace((String)ResourceUtil.get(9072), "{max}", "" + MsgBuilder.LEN_MAX_UUID_VALUE),
					(String) ResourceUtil.get(IContext.ERROR),
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	public static synchronized String getUniqueTimeStamp(){
		try{
			Thread.sleep(10);//足够可以错开
		}catch (final Exception e) {
		}

		final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		final StringBuffer timestampBuf = new StringBuffer(25);
		timestampBuf.append((timestamp.getYear() + 1900));
		final int month = (timestamp.getMonth() + 1);
		timestampBuf.append((month < 10?("0"+month):month));
		final int day = timestamp.getDate();
		timestampBuf.append(day < 10?("0"+day):day);
		final int hour = timestamp.getHours();
		timestampBuf.append("_");
		timestampBuf.append(hour < 10?("0"+hour):hour);
		final int minute = timestamp.getMinutes();
		timestampBuf.append((minute < 10?("0"+minute):minute));
		final int second = timestamp.getSeconds();
		timestampBuf.append((second < 10?("0"+second):second));
		final int nanos = timestamp.getNanos();
		final String zeros = "000000000";
		String nanosString;
        if (nanos == 0) {
            nanosString = "0";
        } else {
            nanosString = Integer.toString(nanos);

            nanosString = zeros.substring(0, (9-nanosString.length())) +
                nanosString;

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

	public static String getTimeStamp(){
		final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		final StringBuffer timestampBuf = new StringBuffer(25);
		timestampBuf.append((timestamp.getYear() + 1900));
		final int month = (timestamp.getMonth() + 1);
		timestampBuf.append((month < 10?("0"+month):month));
		final int day = timestamp.getDate();
		timestampBuf.append(day < 10?("0"+day):day);
		final int hour = timestamp.getHours();
		timestampBuf.append("_");
		timestampBuf.append(hour < 10?("0"+hour):hour);
		final int minute = timestamp.getMinutes();
		timestampBuf.append((minute < 10?("0"+minute):minute));
		final int second = timestamp.getSeconds();
		timestampBuf.append((second < 10?("0"+second):second));
		
		return timestampBuf.toString();
	}
	
	public static int getAbstractCtrlKeyMask(){
		if(isMacOSX()){
			return KeyEvent.META_MASK;
		}else{
			return KeyEvent.CTRL_MASK;
		}
	}
	
	public static int getAbstractCtrlKeyCode(){//注意：请与上段的Mask保持同步
		if(isMacOSX()){
			return KeyEvent.VK_META;
		}else{
			return KeyEvent.VK_CONTROL;
		}
	}
	
	/**
	 * 替换串中如{1234}，{uuid}等内容
	 * @param str
	 * @return
	 */
	public static String replaceWithI18N(String str){
		//替换{1234}为相应值
		while(true){
			final String regua = "\\{.*?\\}";
			final Pattern p = Pattern.compile(regua);
			final Matcher ma = p.matcher(str);
			if(ma.find()){
				final String g = ma.group();
				final String key = g.substring(1, g.length() - 1);
				if(key.equals("uuid")){
					str = str.replace(g, IConstant.getUUID());
				}else{
					str = str.replace(g, (String)ResourceUtil.get(Integer.parseInt(key)));
				}
			}else{
				break;
			}
		}
		
		return str;
	}
	
	public static int getAbstractCtrlInputEvent(){
		if(isMacOSX()){
			return InputEvent.META_MASK;
		}else{
			return InputEvent.CTRL_MASK;
		}
	}
	
	public static KeyStroke getRefreshKeyStroke(){
		if(isMacOSX()){
			return KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_MASK);
		}else{
			return KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
		}
	}
	
	public static String getRefreshKeyText(){
		if(isMacOSX()){
			return getMacOSCommandKeyText() + " + R";
		}else{
			return "F5";
		}
	}
	
	public static String getAbstractCtrlInputEventText(){
		final int key = getAbstractCtrlInputEvent();
		
		if(key == InputEvent.META_MASK){
			return getMacOSCommandKeyText();
		}else if(key == InputEvent.CTRL_MASK){
			return "Ctrl";
		}
		return "";
	}
	
	public static String getAbstractCtrlKeyText(){
		return KeyComperPanel.getHCKeyText(getAbstractCtrlKeyCode());
	}
	
	public static String getMacOSCommandKeyText(){
		return String.valueOf((char)Integer.parseInt("8984"));//⌘
	}
	
	public static String getMacOSOptionKeyText(){
		return "Option/Alt";
	}
	
	public static void buildAcceleratorKeyOnAction(final Action action, final int keyCode){
		action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyCode, 
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}
	
	public static String toMD5(final byte tmp[]){
		final char hexDigits[] = {       // 用来将字节转换成 16 进制表示的字符
			     '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',  'e', 'f'};
		final char str[] = new char[16 * 2];   // 每个字节用 16 进制表示的话，使用两个字符，
		int k = 0;                                // 表示转换结果中对应的字符位置
		for (int i = 0; i < 16; i++) {          // 从第一个字节开始，对 MD5 的每一个字节
		                                 // 转换成 16 进制字符的转换
			final byte byte0 = tmp[i];                 // 取第 i 个字节
			str[k++] = hexDigits[byte0 >>> 4 & 0xf];  // 取字节中高 4 位的数字转换, 
		                                             // >>> 为逻辑右移，将符号位一起右移
			str[k++] = hexDigits[byte0 & 0xf];            // 取字节中低 4 位的数字转换
		} 
		return new String(str);                     
	}

	
	public static Object get(final int id){
		if(id < 10000){
			return UILang.getUILang(id);
		}
		return null;
	}
	
	private static final HashMap<Integer, I18NStoreableHashMapWithModifyFlag> i18nMap = new HashMap<Integer, I18NStoreableHashMapWithModifyFlag>(4);
	
	/**
	 * 返回全部可用的指定id的uilang的全部map
	 * @param id
	 * @return 结果不为null。key = en-US(不是_), value = i18n(id)
	 */
	public static I18NStoreableHashMapWithModifyFlag getI18NByResID(final int id){
		I18NStoreableHashMapWithModifyFlag out = i18nMap.get(id);
		if(out != null){
			return out;
		}
		
		out = new I18NStoreableHashMapWithModifyFlag(32);
		try{
			final Pattern pattern = Pattern.compile(UILang.UI_LANG_FILE_NAME + "(\\w+)\\.properties$");
			final URL url = UILang.class.getResource(UILang.UI_LANG_FILE_NAME_PREFIX + "en_US.properties");
			final URI uri = url.toURI();
		    if (uri != null && uri.getScheme().equals("jar")) {//此条件支持android服务器
		        Object pathurl;
		        if(isAndroidServerPlatform()){
			    	//jar:file:/data/app/homecenter.mobi.server-2.apk!/uilang_en_US.properties
		        	pathurl = "/";
		        }else{
		        	pathurl = UILang.class.getProtectionDomain().getCodeSource().getLocation();  
		        }
		        final String[] enterNames = PlatformManager.getService().listAssets(pathurl);
		        for (int i = 0; i < enterNames.length; i++) {
		        	addItem(id, out, pattern, enterNames[i]);
				}
		    } else {
		    	final File file_base = new File(ResourceUtil.getBaseDir(), ".");
		    	final File[] files = file_base.listFiles();
		    	for (int i = 0; i < files.length; i++) {
		    		final File file_temp = files[i];
		    		final String path = file_temp.getAbsolutePath();
					addItem(id, out, pattern, path);
				}
		    	
		    }
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		
	    i18nMap.put(id, out);
	    return out;
	}

	private static void addItem(final int id, final I18NStoreableHashMapWithModifyFlag out,
			final Pattern pattern, final String path) {
		final Matcher matcher = pattern.matcher(path);
		if(matcher.find()){
			final String local_ = matcher.group(1);
			final Hashtable table = UILang.buildResourceBundle(local_);
			final Object value = table.get(String.valueOf(id));
			if(value != null){
				out.put(local_.replace('_', '-'), value);
//				L.V = L.O ? false : LogManager.log(local_ + " : " + value);
			}
		}
	}
	
	private static final ClassLoader cldr = ResourceUtil.class.getClassLoader();
	
	public static URL getResource(final String fileName){
	    return cldr.getResource(fileName);
	}

	public static InputStream getResourceAsStream(final String fileName){
	    return cldr.getResourceAsStream(fileName);
	}

	public static BufferedImage getImage(final URL url){
		try {
			return ImageIO.read(url);
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static byte[] getAbsPathContent(final String path) throws Exception{
		return getResource(getAbsPathInputStream(path));
	}
	
	public static DataInputStream getAbsPathInputStream(final String path) throws Exception{
		URL uri = null;
		try{
			final Class startC = getStarterClass();
			if(startC != null){
				uri = startC.getResource(path);
				return getInputStream(uri);
			}
		}catch (final Throwable e) {
		}
		
		try{
			return getInputStream(ResourceUtil.class.getResource(path));
		}catch (final Throwable e) {
		}
		
		uri = new File(ResourceUtil.getBaseDir(), "." + path).toURI().toURL();
		return getInputStream(uri);
	}
	
	private static DataInputStream getInputStream(final URL url) throws Exception {
		return new DataInputStream(url.openStream());
	}
	
	public static BufferedImage loadImage(final String imageFileName){
		try {
			return ImageIO.read(ResourceUtil.getResource("hc/res/" + imageFileName));
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static byte[] getResource(final DataInputStream dataIs) throws Exception {
		final byte[] imgDataBa = new byte[dataIs.available()];
//		DataInputStream dataIs = new DataInputStream(uri.openStream());
		dataIs.readFully(imgDataBa);
		dataIs.close();
		return imgDataBa;
	}

	public static synchronized String createResourceID(){
		String p_id = PropertiesManager.getValue(PropertiesManager.p_ResourceID);
		if(p_id == null){
			p_id = "1";
		}else{
			p_id = String.valueOf(Integer.parseInt(p_id) + 1);
		}
		PropertiesManager.setValue(PropertiesManager.p_ResourceID, p_id);
		PropertiesManager.saveFile();
		
		return p_id;
	}
	
	public static void notifySave(){
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
		if(rr == null){
			return;
		}
		String baseDir = null;
		try{
			//注意：服务器停止使用System.getProperty("user.dir")
			baseDir = ResourceUtil.getBaseDir().getCanonicalPath();//System.getProperty("user.dir");
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		final String[] toRemove = rr.split(";");
		for (int i = 0; i < toRemove.length; i++) {
			final String url = toRemove[i];
			if(url.length() > 0){
				if(url.indexOf("hc/res/") >= 0){
					continue;
				}
				final File file = new File(baseDir + url);
				//强制释放可能没有关闭的资源。
				System.gc();
				final boolean b = file.delete();
				//强制释放可能没有关闭的资源。
				System.gc();
//				System.out.println("Del ico : " + file.toString() + ", succe:" + b);
			}
		}
		PropertiesManager.setValue(pNew, "");
		PropertiesManager.saveFile();
	}
	
	public static void addMaybeUnusedResource(final String url, final boolean isNew){
		String p = null;
		if(isNew){
			p = PropertiesManager.p_ResourcesMaybeUnusedNew;
		}else{
			p = PropertiesManager.p_ResourcesMaybeUnusedOld;
		}
		addUnused(url, p);
	}

	private static void addUnused(final String url, final String p_new) {
		String rr = PropertiesManager.getValue(p_new);
		if(rr == null){
			rr = "";
		}
		if(rr.length() == 0){
			rr = url;
		}else{
			rr += ";" + url;
		}
		PropertiesManager.setValue(p_new, rr);
		PropertiesManager.saveFile();
	}

//	public static void loadJar(File file){
//		PlatformManager.getService().addSystemLib(file);
//	}
	
	public static BufferedImage resizeImage(final BufferedImage bufferedimage, final int w, final int h){
		return PlatformManager.getService().resizeImage(bufferedimage, w, h);
	}
	
	/**
	 * 去掉全通透部分，改半透明为底白效果，即不透明
	 * @param src
	 * @return
	 */
	public static BufferedImage unAlphaImage(final BufferedImage src){
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] data = new int[w * h];
		src.getRGB(0, 0, w, h, data, 0, w);
		for (int i = 0; i < data.length; i++) {
			final int rgb = data[i];
			if((rgb | 0x00FFFFFF) != 0x00FFFFFF){
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
	
	public static boolean isJ2SELimitFunction(){
		return isStandardJ2SEServer();
	}
	
	public static boolean isStandardJ2SEServer(){
		final String osPlatform = System.getProperty(CCoreUtil.SYS_SERVER_OS_PLATFORM);
		return (osPlatform == null);
	}
	
	public static boolean isAndroidServerPlatform(){
		final String androidplatform = System.getProperty(CCoreUtil.SYS_SERVER_OS_PLATFORM);
		if(androidplatform != null){
			return androidplatform.equalsIgnoreCase(CCoreUtil.SYS_SERVER_OS_ANDROID_SERVER);
		}
		return false;
	}

	public static boolean isWindowsOS() {
		final String os = System.getProperty("os.name");
		return os.toLowerCase().indexOf("windows") >= 0;
	}
	
	public static boolean isLinuxRelease(final String issue){
		final String lowIssue = issue.toLowerCase();
		boolean isRelease = false;
		try {
			final Process process = Runtime.getRuntime().exec("lsb_release -a");//cat /etc/issue
			final InputStreamReader ir = new InputStreamReader(
					process.getInputStream());
			final LineNumberReader input = new LineNumberReader(ir);
			String line;
			while ((line = input.readLine()) != null){
				if((isRelease == false) && (line.toLowerCase().indexOf(lowIssue) >= 0)){
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
		final String os = System.getProperty("java.vm.name");//OpenJDK Client VM or OpenJDK 64-Bit Server VM
		return os != null && os.indexOf("OpenJDK") >= 0;
	}
	
	public static boolean isWindowsXP(){
		final String os = System.getProperty("os.name");
		return os.equals("Windows XP");
	}
	
	public static boolean isWindows2003(){
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("windows") >= 0) && (os.indexOf("2003") > 0);
	}
	
	
	public static boolean isWindowsVista(){
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("windows vista") >= 0);
	}
	
	
	public static boolean isWindows2008(){
		//Windows Server 2008
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("windows") >= 0) && (os.indexOf("2008") > 0);
	}
	
	public static boolean isMacOSX(){
		//Mac OS X
		final String os = System.getProperty("os.name");
		return (os.toLowerCase().indexOf("mac") >= 0);//改mac os x => mac
	}
	
	public static Properties loadThirdLibs() {
		final Properties thirdlibs = new Properties();
		final String url = RootServerConnector.AJAX_HTTPS_44X_URL_PREFIX + "thirdlib.php";
		if(L.isInWorkshop){
			LogManager.log("try get download online lib information from : " + url);
		}
		try{
			loadFromURL(thirdlibs, url);
		}catch (final Exception e) {
		}
		if(thirdlibs.isEmpty()){
			App.showMessageDialog(null, "Can NOT connect HomeCenter, please try after few seconds!", "Error Connect", JOptionPane.ERROR_MESSAGE);
		}
		return thirdlibs;
	}

	public static void loadFromURL(final Properties thirdlibs, String url) throws Exception {
		url = HttpUtil.replaceSimuURL(url, PropertiesManager.isSimu());
		final String libs = HttpUtil.getAjax(url);
		if(libs == null){
			return;
		}
		thirdlibs.load(new StringReader(libs));//否则new URL(url).openStream()会出现字符集不正确
	}

	public static BufferedImage rotateImage(final BufferedImage bufferedimage,
	        final int degree) {
	    final int w = bufferedimage.getWidth();
	    final int h = bufferedimage.getHeight();
	    //int type = bufferedimage.getColorModel().getTransparency();
	    BufferedImage img;
	    Graphics2D graphics2d;
	    (graphics2d = (img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB))
	            .createGraphics()).setRenderingHint(
	            RenderingHints.KEY_INTERPOLATION,
	            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	    graphics2d.rotate(Math.toRadians(degree), w / 2, h / 2);
	    graphics2d.drawImage(bufferedimage, 0, 0, null);
	    graphics2d.dispose();
	    return img;
	}
	
	/**
	 * 水平翻转
	 * @param bufferedImage
	 * @return
	 */
	public static BufferedImage flipHorizontalJ2D(final BufferedImage bufferedimage) {
		final int w = bufferedimage.getWidth();
        final int h = bufferedimage.getHeight();
        BufferedImage img;
        Graphics2D graphics2d;
        (graphics2d = (img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)).createGraphics())
                .drawImage(bufferedimage, 0, 0, w, h, w, 0, 0, h, null);
        graphics2d.dispose();
        return img;
    }
	
	public static Dimension getScreenSize(){
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		return toolkit.getScreenSize();
	}
	
	public static final Icon toIcon(final BufferedImage bi){
		return new ImageIcon(bi);
	}
	
	public static BufferedImage toBufferedImage(final Image img)
	{
	    if (img instanceof BufferedImage){
	        return (BufferedImage)img;
	    }

	    final BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    final Graphics2D g = bimage.createGraphics();
	    g.drawImage(img, 0, 0, null);
	    g.dispose();

	    return bimage;
	}

	/**
	 * 注意：与HCAndroidStarter同步
	 * @param parent
	 * @param ext with ., such as ".har", ".had"
	 * @return
	 */
	public static String createRandomFileNameWithExt(final File parent, final String ext) {
		final Random random = new Random();
		random.setSeed(System.currentTimeMillis());
		while(true){
			final int r = random.nextInt(99999999);
			final String str_r = String.valueOf(r) + ext;
			File file_r;
			if(parent == null){
				file_r = new File(ResourceUtil.getBaseDir(), str_r);
			}else{
				file_r = new File(parent, str_r);
			}
			if(file_r.exists() == false){
				return str_r;
			}
		}
	}
	
	public static String createRandomVariable(final int length, final int startR){
		final char[] chars = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '_'};
		final int charsLen = chars.length;
		
		final char[] out = new char[length];
		try{
			Thread.sleep(20);
		}catch (final Exception e) {
		}
		long rand = System.currentTimeMillis()+ startR;
		for (int i = 0; i < length; i++){
			final Random random = new Random();
			random.setSeed(rand);
			final int r = random.nextInt(99999999);
			rand += r;
			out[i] = chars[r % charsLen];
		}
		
		return String.valueOf(out);
	}
	
	public static byte[] getMD5Bytes(final String src){
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(ByteUtil.getBytes(src, IConstant.UTF_8));
			return digest.digest();
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return new byte[0];
	}
	
	public static byte[] getMD5Bytes(final byte[] src){
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.update(src);
			return digest.digest();
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		return new byte[0];
	}

	public static String getMD5(final String src){
		final byte[] bs = getMD5Bytes(src);
		
		if(bs.length == 0){
			return "";
		}else{
			return convertMD5BytesToString(bs);
		}
	}

	public static String getMD5(final byte[] src){
		final byte[] bs = getMD5Bytes(src);
		
		if(bs.length == 0){
			return "";
		}else{
			return convertMD5BytesToString(bs);
		}
	}

	public static String getMD5(final File file) {
		MessageDigest digest = null;
		FileInputStream in = null;
		final byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			return convertMD5BytesToString(digest.digest());
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}finally{
			try{
				in.close();
			}catch (final Throwable e) {
			}
		}
		return "";
	}
	
	private static String convertMD5BytesToString(final byte[] result){
		final char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
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
	
	public static final boolean deleteDirectoryNowAndExit(final File directory) {
		//CCoreUtil.checkAccess();
	
	    if(directory.exists()){
	        final File[] files = directory.listFiles();
	        if(null!=files){
	            for(int i=0; i<files.length; i++) {
	                if(files[i].isDirectory()) {
	                    deleteDirectoryNowAndExit(files[i]);
	                }
	                else {
	                    if(files[i].delete() == false){
	                    	L.V = L.O ? false : LogManager.log("fail del file : " + files[i].getAbsolutePath());
	                    }
	                }
	            }
	        }
	        
		    final boolean isDel = directory.delete();
		    if(isDel == false){
		    	L.V = L.O ? false : LogManager.log("fail del dir/file : " + directory.getAbsolutePath());
		    }
		    return isDel;
	    }
	    return true;
	}

	public static int getIntervalSecondsForNextStartup() {
		try{
			return Integer.parseInt(PropertiesManager.getValue(PropertiesManager.p_intervalSecondsNextStartup, DefaultManager.INTERVAL_SECONDS_FOR_NEXT_STARTUP));
		}catch (final Exception e) {
			return 5;
		}
	}

	public static int getSecondsForPreloadJRuby() {
		final String preloadAfterStartup = PropertiesManager.getValue(PropertiesManager.p_preloadAfterStartup, DefaultManager.PRELOAD_AFTER_STARTUP_FOR_INPUT);
		int seconds = 0;
		try{
			seconds = Integer.parseInt(preloadAfterStartup);
		}catch (final Throwable e) {
		}finally{
			if(seconds < 0){
				seconds = -1;
			}
		}
		return seconds;
	}

	public static boolean writeToFile(final byte[] bfile, final File fileName) {  
	    BufferedOutputStream bos = null;  
	    FileOutputStream fos = null;  
	    try {  
	        fos = new FileOutputStream(fileName);  
	        bos = new BufferedOutputStream(fos);  
	        bos.write(bfile);
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
	 * @return
	 */
	public static final boolean canCtrlWiFi(final J2SESession coreSS) {
		return UserThreadResourceUtil.getMobileAgent(coreSS).ctrlWiFi() || WiFiDeviceManager.getInstance(coreSS).canCreateWiFiAccount();
	}

	public static String getLibNameForAllPlatforms(final String libName) {
		return libName + (isAndroidServerPlatform()?EXT_APK:"");
	}

	public static boolean isEnableDesigner(){
		return isJ2SELimitFunction();
	}

	public static File getTempFileName(final String extFileName){
		final String fileName = createRandomFileNameWithExt(StoreDirManager.TEMP_DIR, extFileName);
		final File outTempFile = new File(StoreDirManager.TEMP_DIR, fileName);
		return outTempFile;
	}

	/**
	 * 必须在用户线程
	 * @param icon
	 * @return
	 */
	public static Icon clearIcon(final Icon icon){
		if(icon == null){
			return null;
		}
		
		try{
			return toIcon(toBufferedImage(icon));
		}catch (final Throwable e) {
		}
		return null;
	}

	public static URL getBCLURL() {
		return getResource("hc/res/" + PlatformManager.getService().doExtBiz(PlatformService.BIZ_BCL, null));
	}

	private final static long startMS = System.currentTimeMillis();

	public final static long getStartMS(){
		CCoreUtil.checkAccess();
		return startMS;
	}

	private static File baseDir;

	public static File getBaseDir(){
		if(baseDir == null){
			baseDir = PlatformManager.getService().getBaseDir();
		}
		return baseDir;
	}

	/**
	 * 将 s 进行 BASE64 编码
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
		return (String) get(IContext.INFO);
	}

	public static String getWarnI18N() {
		return (String) get(IContext.WARN);
	}

	public static String getErrorI18N() {
		return (String) get(IContext.ERROR);
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
	public static boolean checkSysPackageNameInJar(final File file){
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(file, false);
			final Enumeration<JarEntry> entrys = jarFile.entries();
			while (entrys.hasMoreElements()) {
				final JarEntry jarEntry = entrys.nextElement();
				String entryName = jarEntry.getName();
				if (entryName.endsWith(".class")) {
					entryName = entryName.replace("/", ".");
					if(checkSysPackageName(entryName)){
						LogManager.errToLog("class [" + entryName + "] has reserved package name.");
						return true;
					}
				}
			}
		} catch (final Throwable e) {
		}finally{
			try{
				jarFile.close();
			}catch (final Throwable e) {
			}
		}
		return false;
	}

	public static final String FILE_IS_MODIFIED_AFTER_SIGNED = "file is incomplete or modified after signed";
	public static final String RESERVED_PACKAGE_NAME_IS_IN_HAR = "reserved package name [hc/java/javax] is in HAR!";
	public static final String HAR_PROJECT_FILE_IS_CORRUPTED = "HAR project file is corrupted or incomplete.";

	public static void generateCertForNullOrError() {
		CCoreUtil.checkAccess();
		
		App.generateCert();
		PropertiesManager.setValue(PropertiesManager.p_EnableTransNewCertKeyNow, IConstant.TRUE);
		
		App.setNoTransCert();
	}

	/**
	 * 检查stackTrace的类源都是系统包，且callerClass非null时，必须在stackTrace中。否则抛出异常。
	 * @param callerClass
	 * @param loader 使用指定的类加载器来检查类
	 */
	public static final void checkHCStackTraceInclude(final String callerClass, final ClassLoader loader) {
		checkHCStackTraceInclude(callerClass, loader, null);
	}
	
	/**
	 * 检查stackTrace的类源都是系统包，且callerClass非null时，必须在stackTrace中。否则抛出异常。
	 * @param callerClass
	 * @param loader 使用指定的类加载器来检查类
	 * @param moreMsg
	 */
	public static final void checkHCStackTraceInclude(final String callerClass, final ClassLoader loader, final String moreMsg) {
		final StackTraceElement[] el = Thread.currentThread().getStackTrace();//index越小，距本方法越近
		final ClassLoader checkLoader = (loader==null?ResourceUtil.class.getClassLoader():loader);
		boolean isFromCallerClass = false;
		
		for (int i = el.length - 1; i >= 0; i--) {
			final String className = el[i].getClassName();
			if(className.equals("org.jruby.embed.internal.EmbedEvalUnitImpl")){//动态解释执行
				if(PropertiesManager.isSimu()){
					LogManager.errToLog("Illegal class [" + className + "] is NOT allowed in stack trace in ClassLoader[" + checkLoader.toString() + "].");
				}
				if(moreMsg != null){
					LogManager.errToLog(moreMsg);
				}
				throw new HCSecurityException(PropertiesManager.ILLEGAL_CLASS);
			}
			try{
				Class.forName(className, false, checkLoader);
			}catch (final Exception e) {
				if(PropertiesManager.isSimu()){
					LogManager.errToLog("Illegal class [" + className + "] is NOT allowed in stack trace in ClassLoader[" + checkLoader.toString() + "].");
				}
//				panel
//				App.showCenterPanel(panel, 0, 0, ResourceUtil.getErrorI18N(), false, null, null, null, null, null, false, true, null, false, true);
				if(moreMsg != null){
					LogManager.errToLog(moreMsg);
				}
				throw new HCSecurityException(PropertiesManager.ILLEGAL_CLASS);
			}
			if(callerClass != null && className.equals(callerClass)){
				isFromCallerClass = true;
			}
		}
		
		if(callerClass != null && isFromCallerClass == false){
			if(PropertiesManager.isSimu()){
				LogManager.errToLog("Class [" + callerClass + "] should be in stack trace in ClassLoader[" + checkLoader.toString() + "], but NOT.");
			}
			if(moreMsg != null){
				LogManager.errToLog(moreMsg);
			}
			throw new HCSecurityException(PropertiesManager.ILLEGAL_CLASS);
		}
	}

	/**
	 * 将 BASE64 编码的字符串 s 进行解码
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
		return isMacOSX()?"÷":"";
	}

	public static String buildUUID() {
		return UUID.randomUUID().toString();
	}

	public static void sendToClipboard(final String text) {
		final Clipboard clipbd = getClipboard();
		final StringSelection clipString = new StringSelection(text);
		clipbd.setContents(clipString, clipString);
	}

	public static Clipboard getClipboard() {
		return Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	public static String getTxtFromClipboard(){
		final Clipboard clipbd = getClipboard();
		final Transferable clipT = clipbd.getContents(null);
		String d = null;
		if (clipT != null) {
			// 检查内容是否是文本类型
			if (clipT.isDataFlavorSupported(DataFlavor.stringFlavor)){
				try {
					d = (String) clipT.getTransferData(DataFlavor.stringFlavor);
				} catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		}
		if(d == null){
			return "";
		}else{
			return d.trim();
		}
	}

	/**
	 * 
	 * @param host
	 * @param timeout the time, in milliseconds, before the call aborts
	 * @return
	 */
	public static NetworkInterface searchReachableNetworkInterface(final String host, final int timeout) {
		try{
	    	final InetAddress ia = InetAddress.getByName(host);
	    	final Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
	    	while(e.hasMoreElements()){
	    		final NetworkInterface nextElement = e.nextElement();
				if(ia.isReachable(nextElement, 100, timeout)){
					return nextElement;
				}
	    	}
		}catch (final Throwable e) {
		}
		return null;
	}

	public static boolean canCreateWiFiAccountOnPlatform(final WiFiDeviceManager platManager) {
		return platManager != null && platManager.hasWiFiModule() && platManager.canCreateWiFiAccount();
	}

	/**
	 * 没有找到返回null
	 * @param domainOrIP
	 * @return
	 */
	public static InetAddress searchReachableInetAddress(final String domainOrIP){
		final int timeout = 2000;
		NetworkInterface availableNI = searchReachableNetworkInterface(domainOrIP, timeout);
		
		int total = 0;
		while(availableNI == null && total < 5000){
			try{
				total += 500;
				Thread.sleep(500);
			}catch (final Exception e) {
			}
	
			availableNI = searchReachableNetworkInterface(domainOrIP, timeout);
		}
		
		if(availableNI != null){
			InetAddress out = HttpUtil.filerInetAddress(availableNI, false);
			if(out == null){
				out = HttpUtil.filerInetAddress(availableNI, true);
			}
			return out;
		}else{
			LogManager.errToLog("[" + domainOrIP + "] is unreachable via ICMP ECHO REQUEST!");
		}
		
		return null;
	}

	public static String getProductName() {
		return (String) get(UILang.PRODUCT_NAME);
	}

	public static void doCopyShortcutForMac() {
		if(isMacOSX()){//JRE7缺省是正确的，但是不同Skin会导致被覆盖
			final UIDefaults defaultsUI = UIManager.getDefaults();
			for (final Enumeration keys = defaultsUI.keys(); keys
					.hasMoreElements();) {
				final Object key = keys.nextElement();
				
				if(key instanceof String){
					final String keyStr = (String)key;
					if(keyStr.endsWith(".focusInputMap")){
						final InputMap im = (InputMap) defaultsUI.get(keyStr);//"TextField.focusInputMap"
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
						im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
					}
				}
			}
		}
	}

	public static BufferedImage standardMenuIconForAllPlatform(BufferedImage bi, final int toSize, final boolean roundWithSize) {
		final int cornDegree = 30;
	
		if(bi.getWidth() != toSize || bi.getHeight() != toSize){
			bi = resizeImage(bi, toSize, toSize);
			if(roundWithSize){
				bi = ImageSrc.makeRoundedCorner(bi, cornDegree);
			}
		}
		if(roundWithSize == false){
			bi = ImageSrc.makeRoundedCorner(bi, cornDegree);
		}
		return bi;
	}

	public static String getElementIDFromTarget(final String target) {
		try{
			return target.substring(target.indexOf(HCURL.HTTP_SPLITTER) + HCURL.HTTP_SPLITTER.length());
		}catch (final Throwable e) {
			throw new MissingFormatArgumentException("invalid target : " + target);
		}
	}

	public static boolean isDisableUIForTest() {
		return PropertiesManager.isSimu();
	}

	public static Class loadClass(final String className, final boolean printWhenNoFound) {
		try{
			return Class.forName(className);
		}catch (final Throwable e) {
			try{
				return ClassLoader.getSystemClassLoader().loadClass(className);
			}catch (final Throwable ex) {
				try {
					return ResourceUtil.class.getClassLoader().loadClass(className);
				} catch (final Throwable e1) {
					if(printWhenNoFound){
						e1.printStackTrace();//不建议用ExceptionReporter
					}
				}
			}
		}
		return null;
	}
}
