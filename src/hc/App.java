package hc;

import hc.core.ConditionWatcher;
import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.cache.CacheManager;
import hc.core.cache.CacheStoreManager;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.IEncrypter;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.core.util.RootBuilder;
import hc.core.util.Stack;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPool;
import hc.core.util.ThreadPriorityManager;
import hc.j2se.HCAjaxX509TrustManager;
import hc.res.ImageSrc;
import hc.server.AppThreadPool;
import hc.server.ConfigPane;
import hc.server.DebugThreadPool;
import hc.server.DisposeListener;
import hc.server.HCActionListener;
import hc.server.J2SEConstant;
import hc.server.J2SEContext;
import hc.server.PlatformManager;
import hc.server.ProcessingWindowManager;
import hc.server.ServerInitor;
import hc.server.StarterManager;
import hc.server.ThirdlibManager;
import hc.server.data.StoreDirManager;
import hc.server.msb.MSBException;
import hc.server.msb.WiFiHelper;
import hc.server.rms.J2SERecordWriterBuilder;
import hc.server.rms.RMSLastAccessTimeManager;
import hc.server.ui.ClientDesc;
import hc.server.ui.ClosableWindow;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.SingleMessageNotify;
import hc.server.util.ContextSecurityManager;
import hc.server.util.ExceptionViewer;
import hc.server.util.HCInitor;
import hc.server.util.HCJDialog;
import hc.server.util.HCJFrame;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.HCSecurityChecker;
import hc.server.util.IDArrayGroup;
import hc.server.util.J2SERootBuilder;
import hc.server.util.VerifyEmailManager;
import hc.util.ClassUtil;
import hc.util.HttpUtil;
import hc.util.IBiz;
import hc.util.LogServerSide;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.SecurityDataProtector;
import hc.util.TokenManager;
import hc.util.UILang;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;

public class App {//注意：本类名被工程HCAndroidServer的ServerMainActivity反射引用，请勿改名
	public static final int EXIT_MAX_DELAY_MS = 15 * 1000;//10秒不够

	public static final String TAG_INI_DEBUG_ON = "debugOn";
	public static final String TAG_INI_DEBUG_THREAD_POOL_ON = "debugThreadPoolOn";
	public static final String TAG_SERVER_MODE = "serverOn";
	public static final String TAG_SERVER_OFF_MODE = "serverOff";
	public static final String TAG_STARTER_VER = "starterVer";
	
	private static ThreadGroup threadPoolToken;
	public static boolean DEBUG_THREAD_POOL_ON = false;
	
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final char WINDOW_PATH_SEPARATOR = '\\';

	static FileLock lock;

	public static final String SYS_ERROR_ICON = "OptionPane.errorIcon";
	public static final String SYS_INFO_ICON = "OptionPane.informationIcon";
	public static final String SYS_QUES_ICON = "OptionPane.questionIcon";
	public static final String SYS_WARN_ICON = "OptionPane.warningIcon";

	public static Icon getSysIcon(final String key) {
		return UIManager.getIcon(key);
	}
	
	private static Float jreVer;
	public static float getJREVer() {
		if(jreVer == null){
			if(ResourceUtil.isAndroidServerPlatform()){
				try{
					jreVer = Float.parseFloat(System.getProperty(CCoreUtil.SYS_ANDROID_SERVER_JAVA_VERSION));
					return jreVer.floatValue();
				}catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
			
			final String ver = System.getProperty("java.version");
			final Pattern pattern = Pattern.compile("^(\\d\\.\\d)");
			final Matcher matcher = pattern.matcher(ver);
			if (matcher.find()) {
				jreVer = Float.parseFloat(matcher.group(1));
			} else {
				try{
					final Integer verint = Integer.parseInt(ver);
					jreVer = verint.floatValue();
				}catch (final Throwable e) {
				}
				jreVer = 1.0F;
			}
		}
		
		return jreVer.floatValue();
	}

	public static void main(final String args[]) {
		new Thread(getRootThreadGroup(), "hc_main"){
			@Override
			public void run(){
				try{
					execMain(args);
				}catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		}.start();
	}
	
	public final static ThreadGroup getThreadPoolToken(){
		return (ThreadGroup)ContextManager.getThreadPoolToken();
	}
	
	public final static ThreadGroup getRootThreadGroup(){
		CCoreUtil.checkAccess();
		
		ThreadGroup group = Thread.currentThread().getThreadGroup();
		
		while(group.getParent() != null){
			group = group.getParent();
		}
		
		return group;
	}
	
	private static void execMain(final String args[]) {
		if (ResourceUtil.isJ2SELimitFunction() && getJREVer() < 1.6) {
			App.showMessageDialog(null, "JRE 1.6 or above!", "Error",
					JOptionPane.ERROR_MESSAGE);
			CCoreUtil.globalExit();
		}
		
		boolean isSimu = false;

		//forece init, because MouseEvent.static {NativeLibLoader.loadLibraries()};
		try{
			new MouseEvent(new JLabel(""), 0, 0, 0, 0, 0, 0, false, 0);
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		
		// 创建锁文件
		try {
			// Get a file channel for the file
			final File file = new File(ResourceUtil.getBaseDir(), "lockme.hc");
			final FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
			
			final long startMS = System.currentTimeMillis();
			
			do{
				try {
					// lock = channel.lock();
					lock = channel.tryLock();
				} catch (final Exception e) {
					// File is already locked in this thread or virtual machine
				}
				if(lock == null){
					try{
						Thread.sleep(200);
					}catch (final Exception e) {
					}
				}
			}while(lock == null && (System.currentTimeMillis() - startMS) <= EXIT_MAX_DELAY_MS);
			
			if (lock == null) {
				App.showMessageDialog(null,
						"HomeCenter is already runing!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			// Release the lock
			// lock.release();
			// Close the file
			// channel.close();
		} catch (final Exception e) {
		}

		IConstant.setServerSide(true);//必须最先被执行

		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				final String arg = args[i];
				if (arg == null) {
					break;
				}
				if (arg.equals(TAG_INI_DEBUG_ON)) {
					System.err.println("main method arguments [debugOn] is deprecated, you may need option/developer/Logger." +
							"\nyou should remove main method arguments : 'debugOn serverOn verify'");
					LogManager.INI_DEBUG_ON = true;
					System.out.println(TAG_INI_DEBUG_ON);
				} else if (arg.equals(TAG_SERVER_MODE)) {
				} else if (arg.equals(TAG_SERVER_OFF_MODE)) {
				}else if(arg.startsWith(TAG_STARTER_VER)){//starter传入其自身版本
					try{
						final String[] values = arg.split("=");
						StarterManager.setCurrStarterVer(values[1]);
					}catch (final Throwable e) {
					}
				}else if(arg.equals(TAG_INI_DEBUG_THREAD_POOL_ON)){
					DEBUG_THREAD_POOL_ON = true;
				}else if(arg.equals("isSimu")){
					isSimu = true;
				}
			}
		}
		IConstant.propertiesFileName = "hc_config.properties";
		if(ResourceUtil.isLoggerOn() == false){
			LogManager.INI_DEBUG_ON = true;
		}
		SecurityDataProtector.init();

		if(isSimu){
			//只做强制isSimu，不做isSimu为false的情形，因为原配置可能为true
			PropertiesManager.setValue(PropertiesManager.p_IsSimu, IConstant.TRUE);//注意：须在下行isSimu之前
		}
		
		if (PropertiesManager.isSimu()) {//注意：须在上行setValue(PropertiesManager.p_IsSimu, IConstant.TRUE);之后
			L.setInWorkshop(true);
			L.V = L.O ? false : LogManager.log("isSimu : true");
		}
		
		//依赖isInWorkshop
		PropertiesManager.emptyDelDir();
		
		//初始化
		HCLimitSecurityManager.getTempLimitThreadPool();
		
		CCoreUtil.setSecurityChecker(new HCSecurityChecker());
		
		final ThreadPool threadPool = DEBUG_THREAD_POOL_ON?new DebugThreadPool():new AppThreadPool();
		threadPool.setName("mainThreadPool");
		threadPoolToken = getRootThreadGroup();
		ContextManager.setThreadPool(threadPool, threadPoolToken);

		RootBuilder.setInstance(new J2SERootBuilder(threadPoolToken));
		
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				//HCLimitSecurityManager may block write file in user-level thread.
				ImageIO.setUseCache(false);
				try {
					//start Image Fetcher daemon thread, because HCLimitSecurityManager may block create thread in user-level thread.
					final ImageIcon ico = new ImageIcon(ResourceUtil.getResource("hc/res/hc_32.png"));
					ico.getIconHeight();
				} catch (final Throwable e) {
				}
			}
		});
		
		{
			HCTimer.doNothing();//trig init process
		}
		
		if(RootConfig.getInstance() == null){
			SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_ERROR_CONNECTION, 
					(String)ResourceUtil.get(1000)
					+ "<BR><BR>" + (new Date().toLocaleString()), (String)ResourceUtil.get(IContext.ERROR),
					60000 * 5, App.getSysIcon(App.SYS_ERROR_ICON));
			RootConfig.reset(true);
		}
		
		//------------------------------------------------------------------------
		//取得RootConfig之后
		//------------------------------------------------------------------------
		
		//依赖RootConfig，故置于上行之后
		ExceptionReporter.setHarHelper(HCLimitSecurityManager.getHCSecurityManager());//系统线程进行初始化，防止用户线程来初始化
		if(PropertiesManager.isTrue(PropertiesManager.p_isReportException)){
			ExceptionReporter.start(false);//false:由于log系统尚未完成初始化
		}
		{
			final UncaughtExceptionHandler oldhandler = Thread.getDefaultUncaughtExceptionHandler();
			
			final UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
				@Override
				public final void uncaughtException(final Thread t, final Throwable e) {
	//					L.V = L.O ? false : LogManager.log("******************uncaughtException*****************=>" + e.getMessage());
						ExceptionReporter.printStackTraceFromThreadPool(e);
//						if(oldhandler != null){//for Android存在重复printStackTrace
//							oldhandler.uncaughtException(t, e);//for Android异常退出
//						}
				}
			};
			
			Thread.setDefaultUncaughtExceptionHandler(handler);
		}
		
		final String hcVersion = StarterManager.getHCVersion();

		final String ver7_7 = "7.7";
		final boolean isLower7_7;
		{//专门处理低7.4的版本逻辑
			final String clearRMSCacheVersion = PropertiesManager.getValue(PropertiesManager.p_clearRMSCacheVersion, "1.0");
			if(isLower7_7 = StringUtil.lower(clearRMSCacheVersion, ver7_7)){
				ResourceUtil.clearDir(StoreDirManager.RMS_DIR);
				
				PropertiesManager.setValue(PropertiesManager.p_clearRMSCacheVersion, ver7_7);
				PropertiesManager.saveFile();
			}
		}
		
		if(isLower7_7){//不再处理
		}else if(StringUtil.higher(hcVersion, ver7_7)){//处理其它版本需要清空RMSCache的逻辑
		}
		
		CacheStoreManager.setRecrodWriterBuilder(new J2SERecordWriterBuilder());
		CacheManager.clearBuffer();
		RMSLastAccessTimeManager.doNothing();
		
		MSBException.init();
		ExceptionViewer.init();
		ExceptionViewer.notifyPopup(PropertiesManager.isTrue(PropertiesManager.p_isEnableMSBExceptionDialog));
		
		HCAjaxX509TrustManager.initSSLSocketFactory();
		
		StarterManager.startUpgradeStarter();
		{
			final String initVersion = PropertiesManager.getValue(PropertiesManager.p_InitVersion);
			if(initVersion == null){
				PropertiesManager.setValue(PropertiesManager.p_InitVersion, hcVersion);
			}
		}
		
		ThirdlibManager.loadThirdLibs();

		// 选择Skin
		final String selectedSkin = ConfigPane.getSystemSkin();

		if(selectedSkin != null && selectedSkin.length() > 0){
			applyLookFeel(selectedSkin, ConfigPane.getDefaultSkin());
		}
		
		PropertiesManager.remove(PropertiesManager.p_IAgree);
		final String agreeVersion = PropertiesManager.getValue(PropertiesManager.p_AgreeVersion, "1.0");
		if (StringUtil.higher(getLastAgreeVersion(), agreeVersion)) {
			final IBiz biz = new IBiz() {
				@Override
				public void start() {
					PropertiesManager.setValue(PropertiesManager.p_AgreeVersion, getLastAgreeVersion());
					PropertiesManager.saveFile();
					
					initServer();
				}

				@Override
				public void setMap(final HashMap map) {
				}
			};
			final IBiz cancelBiz = new IBiz() {
				@Override
				public void start() {
					PlatformManager.getService().exitSystem();
				}

				@Override
				public void setMap(final HashMap map) {
				}
			};
			// 同意使用许可
			showAgreeLicense("HomeCenter : License Agreement",
					true, null, biz, cancelBiz, true);
			return;
		}

		//因为initServer被其它引用，如果有有置逻辑，请并入initServer
		initServer();
		
		doExtTestAfterStartup();
	}

	private static String getLastAgreeVersion(){
		//注意：此处是AgreeVersion，而非StarterManager.getHCVersion()。如果License没有变动，即使版本变动也无需改动。
		return "7.0";
	}
	
	private static void doExtTestAfterStartup(){
//		SysEventTester.doNothing();
//		ContextManager.getThreadPool().run(new Runnable() {
//			@Override
//			public void run() {
//				try{
//					Thread.sleep(2000);
//				}catch (final Exception e) {
//				}
//			}
//		});
	}
	
	public static void setDisposeListener(final Window window, final DisposeListener listener){
		if(window instanceof HCJFrame){
			final HCJFrame hcjFrame = (HCJFrame)window;
			final DisposeListener oldListener = hcjFrame.getDisposeListener();
			if(oldListener != null){
				hcjFrame.setDisposeListener(new DisposeListener() {
					@Override
					public void dispose() {
						oldListener.dispose();
						listener.dispose();
					}
				});
			}else{
				hcjFrame.setDisposeListener(listener);
			}
		}else if(window instanceof HCJDialog){
			final HCJDialog hcjdialog = (HCJDialog)window;
			final DisposeListener oldListener = hcjdialog.getDisposeListener();
			if(oldListener != null){
				hcjdialog.setDisposeListener(new DisposeListener() {
					@Override
					public void dispose() {
						oldListener.dispose();
						listener.dispose();
					}
				});
			}else{
				hcjdialog.setDisposeListener(listener);
			}
		}
	}

	public static void setNoTransCert() {
		PropertiesManager.setValue(PropertiesManager.p_NewCertIsNotTransed, IConstant.TRUE);
		PropertiesManager.saveFile();
	}

	public static void generateCert() {
		CCoreUtil.checkAccess();
		
		byte[] newCertKey = CUtil.getCertKey();

		if(newCertKey == null){
			newCertKey = new byte[CCoreUtil.CERT_KEY_LEN];
			CUtil.setCertKey(newCertKey);
		}

		CCoreUtil.generateRandomKey(ResourceUtil.getStartMS(), newCertKey, 0, CCoreUtil.CERT_KEY_LEN);
		if(IConstant.getInstance() != null){
			IConstant.getInstance().setObject(IConstant.CertKey, newCertKey);
		}else{
			PropertiesManager.updateCertKey(newCertKey);
		}
	}
	
	private static void initServer() {
		HCLimitSecurityManager.switchHCSecurityManager(true);
		
		HCInitor.init();

		if(IConstant.isRegister()){
			setServerLog();
		}else{
			//第一次，未建立帐号前
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					try{
						while(IConstant.isRegister() == false){
							Thread.sleep(20);
						}
					}catch (final Exception e) {
					}
					setServerLog();
				}
			}, threadPoolToken);
		}
		IConstant.setInstance(new J2SEConstant());
		
		if(PropertiesManager.getValue(PropertiesManager.p_CertKey) == null){
			L.V = L.O ? false : LogManager.log("create new certification for new install.");
			ResourceUtil.generateCertForNullOrError();
		}

		if(PropertiesManager.getValue(PropertiesManager.p_RMSServerUID) == null){
			PropertiesManager.setValue(PropertiesManager.p_RMSServerUID, StringUtil.genUID(ResourceUtil.getStartMS()));
			PropertiesManager.saveFile();
		}
		
		ServerInitor.doNothing();

		// JRE 6 install
		// http://www.oracle.com/technetwork/java/javase/downloads/jre-6u27-download-440425.html

//		if (PropertiesManager.isSimu()) {
//			RootConfig.getInstance().setProperty(RootConfig.p_RootRelayServer,
//					RootServerConnector.SIMU_ROOT_IP);
//			RootConfig.getInstance().setProperty(
//					RootConfig.p_RootRelayServerPort, 
//					String.valueOf(RootServerConnector.SIMU_ROOT_PORT));
//		}
		// ContextManager.setSimulate(errorStunEnv != null &&
		// errorStunEnv.equals("true"));

		final String valueDefaultFontSize = PropertiesManager.getValue(PropertiesManager.C_SYSTEM_DEFAULT_FONT_SIZE, IConstant.TRUE);
		if(IConstant.TRUE.equals(valueDefaultFontSize)){
		}else{
			// 初始化字体
			final GraphicsEnvironment environment = GraphicsEnvironment
					.getLocalGraphicsEnvironment();// GraphicsEnvironment是一个抽象类，不能实例化，只能用其中的静态方法获取一个实例
			final String[] fontNames = environment.getAvailableFontFamilyNames();// 获取系统字体

			final JMenu l = new JMenu();
			final String defaultFontName = l.getFont().getName();//"Dialog";
			final String fontName = PropertiesManager.getValue(PropertiesManager.C_FONT_NAME, defaultFontName);
			final String defaultFontSize = String.valueOf(l.getFont().getSize());//16;
			final String fontSize = PropertiesManager.getValue(PropertiesManager.C_FONT_SIZE, defaultFontSize);
			for (int i = 0; i < fontNames.length; i++) {
				if (fontNames[i].equals(fontName)) {
					try{
						initGlobalFontSetting(new Font(fontName, Font.PLAIN,
							Integer.parseInt(fontSize)));
					}catch (final Throwable e) {
						//由于某些皮肤包导致本错误
						LogManager.err("initGlobalFontSetting error : " + e.toString());
						ExceptionReporter.printStackTrace(e);
						
						L.V = L.O ? false : LogManager.log("(set default font and LookAndFeel)");
						applyLookFeel(ConfigPane.getDefaultSkin(), "");
					}
					break;
				}
			}
		}
		// App.invokeLater(new Runnable() {
		// public void run() {
		// try{
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel());
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel());
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel());//V
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel());
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel());//V
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel());//VVV
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel());VV
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel());//VVV
		// // UIManager.setLookAndFeel(new
		// org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel());//V
		// }catch (Exception e) {
		//
		// }
		// new App();
		// }
		// });

		// boolean succ = UDPChannel.buildChannel();
		// if (succ == false) {
		// App.showMessageDialog(null,
		// (String) ResourceUtil.get(1000), (String) ResourceUtil
		// .get(UILang.ERROR), JOptionPane.ERROR_MESSAGE);
		// ExitManager.exit();
		// }

		boolean needNewApp = true;
		PropertiesManager.remove(PropertiesManager.p_AutoStart);//废弃本项
		final String password = PropertiesManager.getValue(PropertiesManager.p_password);
		final String uuid = PropertiesManager.getValue(PropertiesManager.p_uuid);
		if (password != null && uuid != null) {
			needNewApp = false;

			if (IConstant.checkUUID(uuid) == false) {
				App.showMessageDialog(null, "Invalid UUID",
						"Error", JOptionPane.ERROR_MESSAGE);
				CCoreUtil.globalExit();
			}
			startAfterInfo();
		}
		if (needNewApp) {
			startRegDialog();
			
//			ActionListener getUUIDAL = new HCActionListener(new Runnable() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					startRegDialog();
//				}
//			};
//
//			JButton jbOK = null;
//			jbOK = new JButton((String) ResourceUtil.get(IContext.OK));
//
//			JPanel panel = new JPanel();
//			panel.add(new JLabel("Welcome to HomeCenter World!"));
//			showCenterPanel(panel, 300, 120, "HomeCenter", false, jbOK,
//					getUUIDAL, null, null, false, false);
		}
	}

	private static void setServerLog() {
		LogManager.setLog(new LogServerSide());
		L.enable(true);
	}

	public static void startAfterInfo() {
		final String uuid = PropertiesManager.getValue(PropertiesManager.p_uuid);

		if (IConstant.checkUUID(uuid) == false) {
			App.showMessageDialog(null, "Invalid ID", "Error",
					JOptionPane.ERROR_MESSAGE);
			CCoreUtil.globalExit();
		}

		IConstant.setUUID(uuid);
		final String pwd = PropertiesManager.getPasswordAsInput();
		IConstant.setPassword(pwd);
		IConstant.setServerSide(true);

		final ILog ilog = LogManager.getLogger();
		if (ilog != null && ilog instanceof LogServerSide) {
			((LogServerSide) ilog).buildOutStream();
		}

		try {
			new J2SEContext();
			
			WiFiHelper.startAPIfExists();//依赖context

			ContextManager.start();
			
			if(ResourceUtil.isJ2SELimitFunction()){
				//最低JRE要求7
				final String msgMinJre7 = IDArrayGroup.MSG_MIN_JRE_7;
				if(getJREVer() < 1.7 && IDArrayGroup.check(msgMinJre7) == false){
					String msg = (String)ResourceUtil.get(9191);
					msg = StringUtil.replace(msg, "{curr}", "6");
					msg = StringUtil.replace(msg, "{to}", "7");
					final JPanel panel = buildMessagePanel("<html>" + msg + "</html>", getSysIcon(SYS_WARN_ICON));
					final ActionListener okListener = new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							IDArrayGroup.checkAndAdd(msgMinJre7);
						}
					};
					showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.WARN), false, null, null, okListener, null, null, false, true, null, false, true);
				}
			}
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
			App
					.showMessageDialog(
							null,
							"Error connect to server! please try again after few minutes.",
							ResourceUtil.getErrorI18N(),
							JOptionPane.ERROR_MESSAGE);

		}
	}

	public static void showAgreeLicense(final String title, final boolean isBCLLicense, final String license_url, 
			final IBiz biz, final IBiz cancelBiz, final boolean logoHC) {
		final JDialog dialog = new HCJDialog();
		dialog.setModal(true);
		dialog.setTitle(title);
		dialog.setIconImage(App.SYS_LOGO);

		final Container main = dialog.getContentPane();

		final JPanel c = new JPanel();
		c.setLayout(new BorderLayout(5, 5));

		addBorderGap(main, c);

		final String iagree = (String)ResourceUtil.get(9115);
		
		try {
			String accept = (String)ResourceUtil.get(9114);
			accept = StringUtil.replace(accept, "{iagree}", iagree);
			final JLabel label = new JLabel(accept,
					(logoHC ? new ImageIcon(ImageIO.read(ResourceUtil
							.getResource("hc/res/hc_32.png"))) : null),
					SwingConstants.LEADING);
			c.add(label, "North");
		} catch (final IOException e) {
			ExceptionReporter.printStackTrace(e);
		}

		final JTextArea area = new JTextArea(30, 30);
		try {
			if(isBCLLicense){
				try{
					final URL bclurl = ResourceUtil.getBCLURL();
					final BufferedReader in = new BufferedReader(new InputStreamReader(bclurl.openStream()));
					area.read(in, null);
				}catch (final Throwable e) {
				}
			}else{
				final URL url = new URL(HttpUtil.replaceSimuURL(license_url, PropertiesManager.isSimu()));
				final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				area.read(in, null);
			}
			
			area.setEditable(false);
			area.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(final MouseEvent mouseEvent) {
					area.setCursor(new Cursor(Cursor.TEXT_CURSOR)); // 鼠标进入Text区后变为文本输入指针
				}

				@Override
				public void mouseExited(final MouseEvent mouseEvent) {
					area.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)); // 鼠标离开Text区后恢复默认形态
				}
			});
			area.getCaret().addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(final ChangeEvent e) {
					area.getCaret().setVisible(true); // 使Text区的文本光标显示
				}
			});
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					final String[] options = { "O K" };
					App.showOptionDialog(null,
							"Cant connect server, please try late!", "HomeCenter",
							JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
							null, options, options[0]);
					dialog.dispose();
					PlatformManager.getService().exitSystem();
				}
			});
			return;
		}
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		final java.awt.event.ActionListener exitActionListener = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
				if(cancelBiz != null){
					cancelBiz.start();
				}
			}
		}, threadPoolToken);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				dialog.dispose();
			}
		});
		dialog.getRootPane().registerKeyboardAction(exitActionListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		final JCheckBox check = new JCheckBox(iagree);
		check.requestFocusInWindow();
		final JButton ok = buildDefaultOKButton();

		check.addActionListener(new HCActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				ok.setEnabled(check.isSelected());
			}
		});

		ok.setEnabled(false);
		ok.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
				if (biz != null) {
					biz.start();
				}
			}
		}, threadPoolToken));
		final JButton cancel = buildDefaultCancelButton();
		cancel.addActionListener(exitActionListener);

		final JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new GridLayout(1, 2, 5, 5));
		btnPanel.add(cancel);
		btnPanel.add(ok);

		final JPanel botton = new JPanel();
		botton.setLayout(new BorderLayout(5, 5));
		botton.add(check, "Center");
		botton.add(btnPanel, "East");

		final JScrollPane jsp = new JScrollPane(area);
		c.add(jsp, "Center");
		c.add(botton, "South");

		dialog.setSize(800, 700);//Android服务器调大
		dialog.setResizable(false);
		showCenter(dialog);
	}

	public static void addBorderGap(final Container main, final JPanel c) {
		main.setLayout(new GridBagLayout());
		main.add(c, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
						10, 10, 10, 10), 0, 0));
	}

	public static BufferedImage SYS_LOGO;
	static {
		try {
			App.SYS_LOGO = ImageIO.read(App.class.getClassLoader().getResource(
					"hc/res/hc_16.png"));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static synchronized void initGlobalFontSetting(final Font fnt) {
		if(defaultFontSetting == null || defaultFontSetting.keySet().size() == 0){
			defaultFontSetting = new HashMap<Object, Object>();
			saveDefaultGlobalFontSetting();
		}
		
		final FontUIResource fontRes = new FontUIResource(fnt);
		for (final Enumeration keys = UIManager.getDefaults().keys(); keys
				.hasMoreElements();) {
			final Object key = keys.nextElement();
			final Object value = UIManager.get(key);
			if (value instanceof FontUIResource)
				UIManager.put(key, fontRes);
		}
	}
	
	private static HashMap<Object, Object> defaultFontSetting = null;
	
	private static void saveDefaultGlobalFontSetting() {
		for (final Enumeration keys = UIManager.getDefaults().keys(); keys
				.hasMoreElements();) {
			final Object key = keys.nextElement();
			final Object value = UIManager.get(key);
			if (value instanceof FontUIResource)
				defaultFontSetting.put(key, value);
		}
	}
	

	public static void restoreDefaultGlobalFontSetting() {
		if(defaultFontSetting == null || defaultFontSetting.keySet().size() == 0){
			return;
		}
		
		for (final Enumeration keys = UIManager.getDefaults().keys(); keys
				.hasMoreElements();) {
			final Object key = keys.nextElement();
			final Object value = UIManager.get(key);
			if (value instanceof FontUIResource)
				UIManager.put(key, defaultFontSetting.get(key));
		}
	}

	public static void showCenter(final Component frame) {
//		App.invokeLater(new Runnable() {
//			@Override
//			public void run() {
				ProcessingWindowManager.disposeProcessingWindow();
				
				ProcessingWindowManager.showCenter(frame);
//			}
//		});
	}

	/**
	 * 
	 * @param panel
	 * @param width
	 *            为0，则为自适应高宽
	 * @param height
	 *            为0，则为自适应高宽
	 * @param title
	 * @param isAddCancle
	 *            如果为true，则添加一个Cancle
	 * @param jbOK
	 *            如果为null，则创建缺少的图标按钮
	 * @param cancelButText
	 * @param listener
	 * @param cancelListener
	 * @param frame
	 * @param model
	 * @param relativeToComponent
	 * @param isResizable
	 * @param delay 延时显示，如果当前有正在显示的CenterPanel，则当前对话关闭后，才后加载显示
	 */
	public static Window showCenterPanel(final JPanel panel, final int width, final int height,
			final String title, final boolean isAddCancle, final JButton jbOK,
			final String cancelButText, final ActionListener listener,
			final ActionListener cancelListener, final JFrame frame, final boolean model, final boolean isNewJFrame, 
			final Component relativeToComponent, final boolean isResizable, final boolean delay) {
		return showCenterOKDisposeDelayMode(panel, width, height, title, isAddCancle, jbOK,
				cancelButText, listener, cancelListener, true, frame, model, isNewJFrame, relativeToComponent, isResizable, delay);
	}
	
	/**
	 * 内含关闭窗口后的Delay(包括Ok,Cancel)的处理逻辑<BR>
	 * 严禁Delay处理逻辑散布到此方法以外。
	 * @param panel
	 * @param width
	 * @param height
	 * @param title
	 * @param isAddCancle
	 * @param jbOK
	 * @param cancelButText
	 * @param listener
	 * @param cancelListener
	 * @param isOkDispose
	 * @param parentframe
	 * @param model
	 * @param isNewJFrame
	 * @param relativeToObj
	 * @param isResizable
	 * @param isDelayMode
	 * @return
	 */
	public static Window showCenterOKDisposeDelayMode(final JPanel panel, final int width,
			final int height, final String title, final boolean isAddCancle, JButton jbOK,
			final String cancelButText, final ActionListener listener,
			final ActionListener cancelListener, final boolean isOkDispose, final JFrame parentframe,
			final boolean model, final boolean isNewJFrame, final Component relativeToObj, final boolean isResizable, final boolean isDelayMode) {
		final JButton jbCancle = new JButton(((cancelButText == null)?(String) ResourceUtil.get(IContext.CANCEL):cancelButText),
				new ImageIcon(ImageSrc.CANCEL_ICON));
		final UIActionListener cancelAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok,
					final JButton cancel) {
				window.dispose();//注意：要先关闭，因为Listener逻辑可能会打开新窗口

				if (cancelListener != null) {
					try{
						cancelListener.actionPerformed(null);
					}catch (final Throwable e) {
						ExceptionReporter.printStackTrace(e);
					}
				}

				if(isDelayMode){
					loadDelayWindow(window);
				}
			}
		};
		if (jbOK == null) {
			final JButton okButton = new JButton(
					(String) ResourceUtil.get(IContext.OK), new ImageIcon(ImageSrc.OK_ICON));
			jbOK = okButton;
		}
		final UIActionListener jbOKAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok,
					final JButton cancel) {
				if (isOkDispose) {//有可能OK时，仅执行逻辑
					window.dispose();//注意：要先关闭，因为Listener逻辑可能会打开新窗口
				}
				if (listener != null) {
					try {
						listener.actionPerformed(null);//有可能上一个窗口会导致下一个死锁
					} catch (final Throwable e) {
						ExceptionReporter.printStackTrace(e);
					}
				}
				if (isOkDispose && isDelayMode) {
					loadDelayWindow(window);
				}
			}
		};

		return showCenterDelayMode(panel, width, height, title, isAddCancle, jbOK,
				jbCancle, jbOKAction, cancelAction, parentframe, model, isNewJFrame, relativeToObj, isResizable, isDelayMode);
	}

	/**
	 * 不带button的Window
	 * @param panel
	 * @param width
	 * @param height
	 * @param dialog
	 * @param relativeTo
	 * @param isResizable
	 * @return
	 */
	public static Window showCenterPanelWindowWithoutButton(final JPanel panel, final int width,
			final int height, final Window dialog, final Component relativeTo, final boolean isResizable) {
		try{
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				dialog.setIconImage(App.SYS_LOGO);
		
				final ActionListener quitAction = new HCActionListener(new Runnable() {
					@Override
					public void run() {
						dialog.dispose();
					}
				}, threadPoolToken);
				if(dialog instanceof ClosableWindow){
					((ClosableWindow)dialog).setCloseAction(quitAction);
				}
				JRootPane rootPane = null;
				if (dialog instanceof JFrame) {
					rootPane = ((JFrame) dialog).getRootPane();
				} else if (dialog instanceof JDialog){
					rootPane = ((JDialog) dialog).getRootPane();
				}
				if(rootPane != null){
					rootPane.registerKeyboardAction(quitAction,
						KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
						JComponent.WHEN_IN_FOCUSED_WINDOW);
				}
		
				{
					Container c = null;
					if (dialog instanceof JFrame) {
						c = ((JFrame) dialog).getContentPane();
					} else if (dialog instanceof JDialog){
						c = ((JDialog) dialog).getContentPane();
					}
					if(c != null){
						c.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
					}
					{
						final JPanel insetPanel = new JPanel(new GridBagLayout());
						final GridBagConstraints gc = new GridBagConstraints();
						gc.insets = new Insets(ClientDesc.hgap, ClientDesc.vgap, ClientDesc.hgap, ClientDesc.vgap);
						gc.fill =GridBagConstraints.BOTH;
						gc.anchor = GridBagConstraints.CENTER;
						gc.weightx = 1.0;
						gc.weighty = 1.0;
						insetPanel.add(panel, gc);
						if(c != null){
							c.add(insetPanel, BorderLayout.CENTER);
						}
					}
				}
				
				if (dialog instanceof JFrame) {
					((JFrame) dialog)
							.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				} else if (dialog instanceof JDialog){
					((JDialog) dialog)
							.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				}
				dialog.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(final WindowEvent e) {
						quitAction.actionPerformed(null);
					}
				});
		
				if (width == 0 || height == 0) {
					dialog.pack();
					final Dimension dimesion = dialog.getSize();
					if(dimesion.width < 300){//设置最小宽度，因为可能太小，标题不好看
						if(L.isInWorkshop){
							L.V = L.O ? false : LogManager.log("set mininum dialog width to 300.");
						}
						dialog.setSize(300, dimesion.height);
					}
				} else {
					dialog.setSize(width, height);
				}
				
				if(isResizable == false){
					if (dialog instanceof JFrame) {
						((JFrame) dialog).setResizable(false);
					} else if(dialog instanceof JDialog){
						((JDialog) dialog).setResizable(false);
					}
				}
				
				if(relativeTo != null){
					dialog.setLocationRelativeTo(relativeTo);
					dialog.applyComponentOrientation(ComponentOrientation
							.getOrientation(UILang.getUsedLocale()));
				}else{
					dialog.applyComponentOrientation(ComponentOrientation
							.getOrientation(UILang.getUsedLocale()));
					final int width_d = dialog.getWidth(), height_d = dialog.getHeight();
					final int w = (Toolkit.getDefaultToolkit().getScreenSize().width - width_d) / 2;
					final int h = (Toolkit.getDefaultToolkit().getScreenSize().height - height_d) / 2;
					dialog.setLocation(w, h);
				}
		
				setVisibleCenterWindow(dialog, false);
			}
		});
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return dialog;
	}
	
	public static Window showCenter(final JPanel panel, final int width,
			final int height, final boolean isAddCancle, final JButton jbCancle, final JButton jbOK,
			final boolean isSwapOKCancel, final UIActionListener cancelAction,
			final UIActionListener jbOKAction, final Window dialog, final Component relativeTo, final boolean isResizable) {//不能带isRelayMode参数，因为按钮可能无关逻辑
		return showCenterDelayMode(panel, width, height, isAddCancle, jbCancle, jbOK, isSwapOKCancel, cancelAction, jbOKAction, dialog, relativeTo, isResizable, false);
	}
	
	private static Window showCenterDelayMode(final JPanel panel, final int width,
			final int height, final boolean isAddCancle, final JButton jbCancle, final JButton jbOK,
			final boolean isSwapOKCancel, final UIActionListener cancelAction,
			final UIActionListener jbOKAction, final Window dialog, final Component relativeTo, final boolean isResizable, 
			final boolean isDelayMode) {
		try{
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				dialog.setIconImage(App.SYS_LOGO);
		
				jbOK.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						jbOKAction.actionPerformed(dialog, jbOK, jbCancle);
					}
				}, threadPoolToken));
				final ActionListener quitAction = new HCActionListener(new Runnable() {
					@Override
					public void run() {
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run(){
								dialog.dispose();
								if (cancelAction != null) {
									cancelAction.actionPerformed(dialog, jbOK, jbCancle);
								}
								//注意：由于cancleAction内在delayCenterWindow逻辑，所以无需loadDelayWindow
							}
						});
					}
				}, threadPoolToken);
				if(dialog instanceof ClosableWindow){
					((ClosableWindow)dialog).setCloseAction(quitAction);
				}
				JRootPane rootPane = null;
				if (dialog instanceof JFrame) {
					rootPane = ((JFrame) dialog).getRootPane();
				} else if (dialog instanceof JDialog){
					rootPane = ((JDialog) dialog).getRootPane();
				}
				if(rootPane != null){
					rootPane.registerKeyboardAction(quitAction,
						KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
						JComponent.WHEN_IN_FOCUSED_WINDOW);
				}
				final JPanel bottomPanel = new JPanel();
				bottomPanel.setLayout(new GridBagLayout());
		
				final Insets insets = new Insets(5, 5, 5, 5);
				
				final Border panelBorder = panel.getBorder();
				final boolean isPanelTitledBorded = (panelBorder != null && panelBorder instanceof TitledBorder);
				if(isPanelTitledBorded == false){	
					final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
					bottomPanel.add(separator, new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1.0,
						0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						insets, 0, 0));
				}
				if (isAddCancle) {
					final JPanel subPanel = new JPanel();
					subPanel.setLayout(new GridLayout(1, 2, 5, 5));
					jbCancle.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run(){
							cancelAction.actionPerformed(dialog, jbOK, jbCancle);
						}
					}, threadPoolToken));
					if(isSwapOKCancel){
						subPanel.add(jbOK);
						subPanel.add(jbCancle);
					}else{
						subPanel.add(jbCancle);
						subPanel.add(jbOK);
					}
					bottomPanel.add(subPanel, new GridBagConstraints(0, isPanelTitledBorded?0:1, 1, 1, 1.0,
							0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
							insets, 0, 0));
				} else {
					bottomPanel.add(jbOK, new GridBagConstraints(0, isPanelTitledBorded?0:1, 1, 1, 1.0, 0.0,
							GridBagConstraints.LINE_END, GridBagConstraints.NONE, insets,
							0, 0));
				}
		
				{
					Container c = null;
					if (dialog instanceof JFrame) {
						c = ((JFrame) dialog).getContentPane();
					} else if (dialog instanceof JDialog){
						c = ((JDialog) dialog).getContentPane();
					}
					if(c != null){
						c.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
					}
					{
						final JPanel insetPanel = new JPanel(new GridBagLayout());
						final GridBagConstraints gc = new GridBagConstraints();
						gc.insets = new Insets(ClientDesc.hgap, ClientDesc.vgap, ClientDesc.hgap, ClientDesc.vgap);
						gc.fill =GridBagConstraints.BOTH;
						gc.anchor = GridBagConstraints.CENTER;
						gc.weightx = 1.0;
						gc.weighty = 1.0;
						insetPanel.add(panel, gc);
						if(c != null){
							c.add(insetPanel, BorderLayout.CENTER);
						}
					}
					if(c != null){
						c.add(bottomPanel, BorderLayout.SOUTH);
					}
				}
				
				if (dialog instanceof JFrame) {
					((JFrame) dialog)
							.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				} else if (dialog instanceof JDialog){
					((JDialog) dialog)
							.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				}
				dialog.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(final WindowEvent e) {
						quitAction.actionPerformed(null);
					}
				});
				jbOK.setFocusable(true);
				if(rootPane != null){
					rootPane.setDefaultButton(jbOK);
				}
				jbOK.requestFocus();
		
				if (width == 0 || height == 0) {
					dialog.pack();
					final Dimension dimesion = dialog.getSize();
					if(dimesion.width < 300){//设置最小宽度，因为可能太小，标题不好看
						if(L.isInWorkshop){
							L.V = L.O ? false : LogManager.log("set mininum dialog width to 300.");
						}
						dialog.setSize(300, dimesion.height);
					}
				} else {
					dialog.setSize(width, height);
				}
				
				if(isResizable == false){
					if (dialog instanceof JFrame) {
						((JFrame) dialog).setResizable(false);
					} else if(dialog instanceof JDialog){
						((JDialog) dialog).setResizable(false);
					}
				}
				
				if(relativeTo != null){
					dialog.setLocationRelativeTo(relativeTo);
					dialog.applyComponentOrientation(ComponentOrientation
							.getOrientation(UILang.getUsedLocale()));
				}else{
					dialog.applyComponentOrientation(ComponentOrientation
							.getOrientation(UILang.getUsedLocale()));
					final int width_d = dialog.getWidth(), height_d = dialog.getHeight();
					final int w = (Toolkit.getDefaultToolkit().getScreenSize().width - width_d) / 2;
					final int h = (Toolkit.getDefaultToolkit().getScreenSize().height - height_d) / 2;
					dialog.setLocation(w, h);
				}
		
				setVisibleCenterWindow(dialog, isDelayMode);
			}
		});
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return dialog;
	}

	private static Window currCenterWindow = null;
	private static final Stack delayCenterWindow = new Stack();
	
	private static void setVisibleCenterWindow(final Window dialog, final boolean isDelayMode){
		synchronized (delayCenterWindow) {
			if(currCenterWindow != null && isDelayMode){
				delayCenterWindow.push(dialog);
				if(PropertiesManager.isSimu()){
					L.V = L.O ? false : LogManager.log("push window to delay stack!!!");
				}
				return;
			}else{
				if(isDelayMode){
					currCenterWindow = dialog;		
				}
			}
		}
		
		ProcessingWindowManager.disposeProcessingWindow();
		
		dialog.setVisible(true);
		dialog.toFront();
//			App.invokeLater(new Runnable() {
//				@Override
//				public void run() {
//					dialog.setVisible(true);
//					if(dialog instanceof JFrame){
//						if(dialog instanceof Window){
//							((Window)dialog).toFront();
//						}
//					}
//				}
//			});
	}
	
	public static final void invokeLaterUI(final Runnable run){
		if(EventQueue.isDispatchThread()){
			run.run();
		}else{
			SwingUtilities.invokeLater(run);
		}
	}
	
	public static final void invokeAndWaitUI(final Runnable run){
		if(EventQueue.isDispatchThread()){
			run.run();
		}else{
			try{
				SwingUtilities.invokeAndWait(run);
			}catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
	}
	
	private static void loadDelayWindow(final Window lastCloseWindow){
		synchronized (delayCenterWindow) {
			if(currCenterWindow == lastCloseWindow){
				currCenterWindow = null;
			}

			if(delayCenterWindow.size() > 0){
				final Window dialog = (Window)delayCenterWindow.pop();
				setVisibleCenterWindow(dialog, true);
			}
		}
	}

	public static Window showCenter(final JPanel panel, final int width,
			final int height, final String title, final boolean isAddCancle, final JButton jbOK,
			final JButton jbCancle, final UIActionListener jbOKAction,
			final UIActionListener cancelAction, final JFrame frame, final boolean model,
			final boolean isNewJFrame, final Component relativeTo, final boolean isResizable) {
		return showCenterDelayMode(panel, width, height, title, isAddCancle, jbOK, jbCancle, jbOKAction, cancelAction, frame, model, isNewJFrame, relativeTo, isResizable, false);
	}
	
	private static Window showCenterDelayMode(final JPanel panel, final int width,
			final int height, final String title, final boolean isAddCancle, final JButton jbOK,
			final JButton jbCancle, final UIActionListener jbOKAction,
			final UIActionListener cancelAction, final JFrame frame, final boolean model,
			final boolean isNewJFrame, final Component relativeTo, final boolean isResizable, final boolean isDelayMode) {
		final Window dialog;

		dialog = App.buildCloseableWindow(isNewJFrame, frame, title, model);
		return showCenterDelayMode(panel, width, height, isAddCancle, jbCancle, jbOK,
				false, cancelAction, jbOKAction, dialog, relativeTo, isResizable, isDelayMode);
	}

	public static void showCenterPanel(final JPanel panel, final int width, final int height,
			final String title) {
		showCenterPanel(panel, width, height, title, false, null, null, null,
				null, null, false, false, null, false, false);
	}

	
	/**
	 * 重要，请勿在Event线程中调用，
	 */
	public static String showInputDialog(final Component parentComponent, final Object message, final Object initialSelectionValue) {
		return (String)showInputDialog(parentComponent, message,
				getOptionPaneTitle("OptionPane.inputDialogTitle", parentComponent), JOptionPane.QUESTION_MESSAGE, null, null,
				initialSelectionValue);
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 */
	public static String showInputDialog(final Component parentComponent,
	        final Object message, final String title, final int messageType) {
	        return (String)showInputDialog(parentComponent, message, title,
	                                       messageType, null, null, null);
    }
	
	/**
	 * 重要，请勿在Event线程中调用，
	 */
    public static Object showInputDialog(final Component parentComponent,
            final Object message, final String title, final int messageType, final Icon icon,
            final Object[] selectionValues, final Object initialSelectionValue){
    	CCoreUtil.checkAccess();
    	ProcessingWindowManager.disposeProcessingWindow();
    	
    	return ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				return JOptionPane.showInputDialog(parentComponent, message, title, messageType, 
		    			icon, selectionValues, initialSelectionValue);
			}
		});
    }

    public static final String[] showHARInputDialog(final String title, final String[] fieldName, final String[] fieldDesc){
		if(fieldName == null || searchNull(fieldName)){
			throw new Error("fieldName is null or null member");
		}
		if(fieldDesc == null || searchNull(fieldDesc)){
			throw new Error("fieldDesc is null or null member");
		}
		if(fieldDesc.length != fieldDesc.length){
			throw new Error("the length of fieldName must equals to the length of fieldDesc");
		}
		final String[] value = new String[fieldName.length];
		for (int i = 0; i < value.length; i++) {
			value[i] = "";
		}
		final JTextField[] fields = new JTextField[value.length];
		for (int i = 0; i < value.length; i++) {
			fields[i] = new JTextField("");
			fields[i].setColumns(18);
		}
		
		final JPanel tablePanel = new JPanel(new GridLayout(value.length, 2));
		tablePanel.setBorder(new TitledBorder("Input Items :"));
		{
			for (int i = 0; i < value.length; i++) {
				tablePanel.add(new JLabel(fieldName[i]));
				tablePanel.add(fields[i]);
			}
		}
		
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(tablePanel, BorderLayout.CENTER);
		final StringBuilder sb = new StringBuilder();
		{
			sb.append("<html>");
			for (int i = 0; i < value.length; i++) {
				if(i != 0){
					sb.append("<br>");
				}
				sb.append("<STRONG>");
				sb.append(fieldName[i]);
				sb.append("</STRONG>");
				sb.append(" : ");
				sb.append(fieldDesc[i]);
			}
			sb.append("</html>");
		}
		{
			final JPanel descPanel = new JPanel(new BorderLayout());
			descPanel.setBorder(new TitledBorder("Description :"));
			descPanel.add(new JLabel(sb.toString()), BorderLayout.CENTER);
			
			panel.add(descPanel, BorderLayout.SOUTH);
		}
		
		ProcessingWindowManager.pause();
		
		ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				final ActionListener listener = new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						for (int i = 0; i < value.length; i++) {
							value[i] = fields[i].getText();
						}
					}
				};
				App.showCenterPanel(panel, 0, 0, title, false, null, null, listener, null, null, true, false, null, false, false);
				return null;
			}
		}, threadPoolToken);
		
		ProcessingWindowManager.resume();
		
		return value;
	}
    
    public static JPanel buildMessagePanel(final String message, final Icon icon){
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel(message, icon, SwingConstants.LEADING), BorderLayout.CENTER);
		return panel;
    }
    
	private static final boolean searchNull(final String[] array){
		for (int i = 0; i < array.length; i++) {
			if(array[i] == null){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * @param parentComponent
	 * @param message
	 * @param title
	 * @return
	 */
	public static int showOptionDialog(final Component parentComponent, final Object message, final String title) {
		final Object[] options = {ResourceUtil.get(1032),
                ResourceUtil.get(1033),
                ResourceUtil.get(1018)};
		return App.showOptionDialog(parentComponent,
				message,
				title,
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			options,
			options[0]);
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * @param parentComponent
	 * @param message
	 * @param title
	 * @param optionType
	 * @return
	 */
	public static int showConfirmDialog(final Component parentComponent,
	        final Object message, final String title, final int optionType){
		return showConfirmDialog(parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * @param parentComponent
	 * @param message
	 * @param title
	 * @param optionType
	 * @param messageType
	 * @return
	 */
	public static int showConfirmDialog(final Component parentComponent,
	        final Object message, final String title, final int optionType, final int messageType){
		return showConfirmDialog(parentComponent, message, title, optionType, messageType, null);
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * @param parentComponent
	 * @param message
	 * @param title
	 * @param optionType
	 * @param messageType
	 * @param icon
	 * @return
	 */
	public static int showConfirmDialog(final Component parentComponent,
	        final Object message, final String title, final int optionType,
	        final int messageType, final Icon icon){
		CCoreUtil.checkAccess();

		ProcessingWindowManager.disposeProcessingWindow();
		
		return (Integer)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				return JOptionPane.showConfirmDialog(parentComponent, message, title, optionType, messageType, icon);
			}
		});
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * @param parentComponent
	 * @param message
	 * @param title
	 * @param optionType
	 * @param messageType
	 * @param icon
	 * @param options
	 * @param initialValue
	 * @return
	 */
	public static int showOptionDialog(final Component parentComponent,
	        final Object message, final String title, final int optionType, final int messageType,
	        final Icon icon, final Object[] options, final Object initialValue){
		
		ProcessingWindowManager.disposeProcessingWindow();
		
		return (Integer)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() {
				return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue);
			}
		});
	}
	
	private static String getOptionPaneTitle(final String uiTitle, final Component parentComponent) {
		final Class[] paraTypes = {Object.class, Component.class};
		final Object[] paras = {uiTitle, parentComponent};
		
		String title = null;
		try{
			title = (String)ClassUtil.invoke(UIManager.class, UIManager.class, "getString", paraTypes, paras, true);
		}catch (final Throwable e) {
		}
		if(title == null){
			title = (String)ResourceUtil.get(9210);
		}
		return title;
	}
	
	public static void showMessageDialog(final Component parentComponent, final Object message){
		showMessageDialog(parentComponent, message, 
				getOptionPaneTitle("OptionPane.messageDialogTitle", parentComponent), JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showMessageDialog(final Component parentComponent,
	        final Object message, final String title, final int messageType){
		showMessageDialog(parentComponent, message, title, messageType, null);
	}
	
	/**
	 * 
	 * @param parentComponent
	 * @param message 支持html
	 * @param title
	 * @param messageType
	 * @param icon
	 */
	public static void showMessageDialog(final Component parentComponent,
	        final Object message, final String title, final int messageType, final Icon icon){
		CCoreUtil.checkAccess();
		ProcessingWindowManager.disposeProcessingWindow();
		
//		ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
//			@Override
//			public Object run() {
//				JOptionPane.showMessageDialog(parentComponent, message, title, messageType, icon);
//				return null;
//			}
//		});
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(parentComponent, message, title, messageType, icon);
			}
		});
	}
	
	private static String appendProjectIDForTitle(String title) {
		if(title == null){//Android Server下可能为null
			title = "message";
		}
		return title + " in project [" + ContextSecurityManager.getConfig(Thread.currentThread().getThreadGroup()).projID + "]";
	}
	
	public static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
	public static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
	public static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
	public static final int QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE;
	public static final int PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;
	
	public static void showHARMessageDialog(final String message){
		final String title = getOptionPaneTitle("OptionPane.messageDialogTitle", null);
		showHARMessageDialog(message, appendProjectIDForTitle(title), 
				INFORMATION_MESSAGE);
	}

	public static void showHARMessageDialog(final String message,
	        final String title, final int messageType){
		showHARMessageDialog(message, title, messageType, null);
	}
	
	public static void showHARMessageDialog(final String message,
	        final String title, final int messageType, final Icon icon){
		synchronized (displayHARMsg) {
			if(displayHARMsg.contains(message)){
				return;
			}
			displayHARMsg.add(message);
		}
		
		ContextManager.getThreadPool().run(new Runnable() {//由于处于用户线程，所以加此
			@Override
			public void run() {
				ProcessingWindowManager.disposeProcessingWindow();
				
				final JPanel panel = new JPanel(new BorderLayout());
				final JLabel lable = new JLabel(message.toString(), icon, SwingConstants.LEADING);
				panel.add(lable, BorderLayout.CENTER);
				
				final ActionListener listener = new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						synchronized (displayHARMsg) {
							displayHARMsg.remove(message);
						}
					}
				};
				
				App.showCenterPanel(panel, 0, 0, title, false, null, null, listener, listener, null, false, true, null, false, true);//isDelay
			}
		}, threadPoolToken);
		
		try{
			Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);//以保证上行的Runnable显示成功！
		}catch (final Exception e) {
		}
	}
	
	private static final HashSet<Object> displayHARMsg = new HashSet<Object>(8);
	
	public static final String getPasswordLocalStoreTip(){
		return (String)ResourceUtil.get(9075);
	}
	
	public static void showInputPWDDialog(final String uuid, final String pwd1,
			final String pwd2, final boolean isToLogin) {
		final String passwdStr = (String) ResourceUtil.get(9030);

		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		final Insets insets = new Insets(5, 5, 5, 5);

		panel.setBorder(new TitledBorder(""));

		final JLabel jluuid = new JLabel();
		jluuid.setIcon(new ImageIcon(ImageSrc.ACCOUNT_ICON));
		final JPanel uuidPanelflow = new JPanel();
		uuidPanelflow.setLayout(new FlowLayout());
		uuidPanelflow.add(jluuid);
		uuidPanelflow.add(new JLabel(VerifyEmailManager.getEmailI18N()));
		uuidPanelflow.add(new JLabel(":"));
		panel.add(uuidPanelflow, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));

		final int columns = 20;
		final JTextField jtfuuid = new JTextField(uuid, columns);
		jtfuuid.setEditable(isToLogin);
		jtfuuid.setForeground(Color.BLUE);
		jtfuuid.setHorizontalAlignment(SwingConstants.RIGHT);
//		JPanel uuidPanel = new JPanel();
//		uuidPanel.setLayout(new FlowLayout());
//		uuidPanel.add(jtfuuid);
//		uuidPanel.add(new JLabel(ImageSrc.DONE_ICON)));//UUID OK图标
//		if (isToLogin && false) {
//			JButton payButton = new JButton("I am VIP");
//			payButton.addActionListener(new HCActionListener(new Runnable() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					showUnlock();
//				}
//			});
//			uuidPanel.add(payButton);
//		}
		panel.add(jtfuuid, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));


		final JLabel jlPassword = new JLabel(passwdStr);
		jlPassword.setIcon(new ImageIcon(ImageSrc.PASSWORD_ICON));
		final String inputPwdTip = "<html>input new password for new account.<BR>" + getPasswordLocalStoreTip() + "</html>";
		if(isToLogin){
			jlPassword.setToolTipText(inputPwdTip);
		}
		
		final JPasswordField passwd1, passwd2;
		passwd1 = new JPasswordField(pwd1, columns);
		passwd1.setEchoChar('*');
		passwd1.enableInputMethods(true);
		passwd1.setHorizontalAlignment(SwingUtilities.RIGHT);
		passwd2 = new JPasswordField(pwd2, columns);
		passwd2.setEchoChar('*');
		passwd2.enableInputMethods(true);
		passwd2.setHorizontalAlignment(SwingUtilities.RIGHT);

		final JPanel pwJpanel = new JPanel();
		pwJpanel.setLayout(new FlowLayout());
		pwJpanel.add(jlPassword);
		pwJpanel.add(new JLabel(":"));
		panel.add(pwJpanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));
		Component subItem = passwd1;
		panel.add(subItem, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));

		final JPanel doublepw = new JPanel();
		doublepw.setLayout(new FlowLayout());
		final JLabel jlPassword2 = new JLabel(passwdStr);
		jlPassword2.setIcon(new ImageIcon(ImageSrc.PASSWORD_ICON));
		if(isToLogin){
			jlPassword2.setToolTipText(inputPwdTip);
		}
		doublepw.add(jlPassword2);

		// 两次密码图标
		// jlPassword2 = new JLabel();
		// jlPassword2.setIcon(ImageSrc.PASSWORD_ICON));
		// doublepw.add(jlPassword2);

		doublepw.add(new JLabel(":"));

		panel.add(doublepw, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.NONE, insets,
				0, 0));
		
		subItem = passwd2;
		panel.add(subItem, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
				0, 0));
//		if (isToLogin) {
//			// 增加安全说明文字
//			String url = "http://homecenter.mobi/msg/know.php?lang="
//					+ System.getProperties().getProperty("user.language");
//			String content = HttpUtil.getAjax(url);
//
//			if (content.length() > 0) {
//				JPanel know = new JPanel();
//				know.setLayout(new BorderLayout());
//				know.setBorder(new TitledBorder("Do you know?"));// <STRONG></STRONG><BR>
//				JLabel desc = new JLabel(content);
//				know.add(desc, BorderLayout.CENTER);
//				panel.add(know, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
//						GridBagConstraints.LINE_START, GridBagConstraints.NONE,
//						insets, 0, 0));
//			}
//		}

		// JPanel main = new JPanel();
		// main.setLayout(new GridBagLayout());
		// main.add(panel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
		// GridBagConstraints.CENTER, GridBagConstraints.BOTH,
		// new Insets(10, 10, 10, 10), 0, 0));
		//
		if(isToLogin == false){
			BufferedImage img = null;
			try{
				img = ImageIO.read(ResourceUtil.getResource("hc/res/tip_16.png"));
			}catch (final Exception e) {
			}
			final JLabel passwordTip = new JLabel(getPasswordLocalStoreTip(), 
					new ImageIcon(img), SwingConstants.LEADING);

//			JPanel subPanel = new JPanel(new BorderLayout());
//			subPanel.add(forgetPwd, BorderLayout.CENTER);
//			subPanel.setBorder(new TitledBorder(""));
			panel.add(passwordTip, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
					GridBagConstraints.LINE_START, GridBagConstraints.BOTH, insets,
					0, 0));
		}
		final JButton jbCancle = new JButton((String) ResourceUtil.get(1018),
				new ImageIcon(ImageSrc.CANCEL_ICON));
		final UIActionListener cancelAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok,
					final JButton cancel) {
				window.dispose();
				if (isToLogin) {
					PlatformManager.getService().exitSystem();
				}
			}
		};
		final JButton jbOK = new JButton(
					(String) ResourceUtil.get(IContext.OK), new ImageIcon(ImageSrc.OK_ICON));
		final UIActionListener jbOKAction = new UIActionListener() {
			@Override
			public void actionPerformed(final Window window, final JButton ok,
					final JButton cancel) {
				try {
					final String email = jtfuuid.getText();
					if(ResourceUtil.checkEmailID(email, window) == false){
						return;
					}
					
					if ((passwd1.getText().getBytes(IConstant.UTF_8).length >= App.MIN_PWD_LEN)
							&& passwd2.getText().equals(passwd1.getText())) {
						window.dispose();
						PropertiesManager.setValue(PropertiesManager.p_uuid, email);//注意：强制更新，因为密码丢失时，需要调用此逻辑重写
						IConstant.setUUID(email);
						App.storePWD(passwd2.getText());
						if (isToLogin) {
							startAfterInfo();
						}
					} else {
						App.showMessageDialog(window,
								StringUtil.replace((String)ResourceUtil.get(9077), "{min}", "" + App.MIN_PWD_LEN),
								(String)ResourceUtil.get(9076), JOptionPane.ERROR_MESSAGE);
					}
				} catch (final UnsupportedEncodingException e) {
					ExceptionReporter.printStackTrace(e);
				}
			}
		};

		showCenter(panel, 0, 0, passwdStr, true, jbOK, jbCancle,
				jbOKAction, cancelAction, null, false, false, null, false);
		if(isToLogin){
			jtfuuid.requestFocus();
		}else{
			passwd1.requestFocus();
		}
	}

	public static void showImageURLWindow(final String title, final String url) {
		try {
			final ImageIcon image = new ImageIcon(new URL(url));
			if (image == null || image.getIconWidth() <= 0) {
				// 没有取到QR图片
				return;
			}
			final JPanel jp = new JPanel() {
				@Override
				public void paint(final Graphics g) {
					g.drawImage(image.getImage(), 68, 48, this);
				}
			};

			final JFrame frame = new HCJFrame();
			frame.setTitle(title);
			frame.setIconImage(SYS_LOGO);
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(jp, BorderLayout.CENTER);
			final JButton close = new JButton((String) ResourceUtil.get(1010));
			close.addActionListener(new HCActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					frame.dispose();
				}
			});
			frame.getContentPane().add(close, BorderLayout.SOUTH);
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setResizable(false);
			frame.setSize(300, 300);
			frame.setVisible(true);
		} catch (final MalformedURLException e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static final int MIN_PWD_LEN = 6;

	public static void storePWD(final String pwd) {
		CCoreUtil.checkAccess();
		
		PropertiesManager.setPasswordAsInput(pwd);
		
		IConstant.setPassword(pwd);
		
		reloadEncrypt(false, null);
		
//		if (IConstant.serverSide) {
//			final String checkAutoStart = IConstant.TRUE;
//			PropertiesManager.setValue(PropertiesManager.p_AutoStart,
//					checkAutoStart);
//		}

		PropertiesManager.saveFile();
	}

	public static void reloadEncrypt(final boolean forceRestartHAR, final Runnable runAfterStop) {
		CCoreUtil.checkAccess();
		
		final boolean isNeedRestart = forceRestartHAR || ServerUIUtil.isServing();
		if(isNeedRestart){
			final boolean isQuery = forceRestartHAR?false:true;
			ServerUIUtil.promptAndStop(isQuery, null);

			if(runAfterStop != null){
				runAfterStop.run();
			}
		}
		
		final IEncrypter en = CUtil.getUserEncryptor();
		if(en != null){
			L.V = L.O ? false : LogManager.log("ID or password is changed, call user encryptor.notifyExit methoad.");
			en.notifyExit(IConstant.serverSide);
			L.V = L.O ? false : LogManager.log("reload user encryptor.");
		}
		CUtil.loadEncryptor();
		if(isNeedRestart){
			ServerUIUtil.restartResponsorServerDelayMode(null, null);
		}
	}

	public static void applyLookFeel(String newSkin, final String errorSkin) {
		if(newSkin == null){
			return;
		}
		try {
			if (newSkin.startsWith(ConfigPane.SYS_LOOKFEEL)) {
				newSkin = newSkin.substring(ConfigPane.SYS_LOOKFEEL.length());
				for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
					if (newSkin.equals(info.getName())) {
						UIManager.setLookAndFeel(info.getClassName());
						return;
					}
				}
			} else {
				try{
					UIManager.setLookAndFeel(newSkin);
					return;
				}catch (final Throwable e) {
					final Throwable ex = e;
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							App.showConfirmDialog(null, ex.getClass().getName() + " : \n" + ex.getMessage(), 
									"Apply UI LookAndFeel Error", JOptionPane.CLOSED_OPTION, 
									JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
						}
					}, getRootThreadGroup());
				}
			}
		} catch (final Throwable e) {
			LogManager.err("Loading look and feel error : " + newSkin);
			ExceptionReporter.printStackTrace(e);
		}
		
		PropertiesManager.setValue(PropertiesManager.C_SKIN, errorSkin);
		PropertiesManager.saveFile();
		
	}

	public static void showTuto() {
		try {
			final String[] urls = new String[7];
			for (int i = 0; i < urls.length;) {
				urls[i++] = "step" + i + ".png";
			}
			final HCNexter next = new HCNexter(urls);
			final JPanel imgPanel = new JPanel();
			final JLabel imgLabel = new JLabel();
			imgPanel.add(imgLabel);
			JButton jbOK = null;
			final String nextStr = (String) ResourceUtil.get(1029);
			try {
				final JButton okButton = new JButton(nextStr, new ImageIcon(
						ImageIO.read(ResourceUtil
								.getResource("hc/res/next_22.png"))));
				jbOK = okButton;
			} catch (final IOException e1) {
			}
			final Icon nextIco = jbOK.getIcon();
			final JButton nextBtn = jbOK;
			JButton jbpre = null;
			try {
				jbpre = new JButton((String) ResourceUtil.get(1030),
						new ImageIcon(ImageIO.read(ResourceUtil
								.getResource("hc/res/prev_22.png"))));
				jbpre.setEnabled(false);
			} catch (final IOException e1) {
			}
			imgLabel.setIcon(new ImageIcon(next.imgs[0]));
			final UIActionListener nextal = new UIActionListener() {
				@Override
				public void actionPerformed(final Window window, final JButton ok,
						final JButton cancel) {
					if (next.idx == 0) {
						cancel.setEnabled(true);
					}
					if (next.idx < (next.imgUrl.length - 1)) {
						final BufferedImage image = next.imgs[++next.idx];
						if (image == null) {
							next.idx--;
							return;
						}
						imgLabel.setIcon(new ImageIcon(image));
						if (next.idx == (next.imgUrl.length - 1)) {
							ok.setText((String) ResourceUtil.get(IContext.OK));
							ok.setIcon(new ImageIcon(ImageSrc.OK_ICON));
						}
					} else {
						window.dispose();
						
						showLockWarning();
					}
				}
			};
			final UIActionListener preal = new UIActionListener() {
				@Override
				public void actionPerformed(final Window window, final JButton ok,
						final JButton cancel) {
					if (next.idx == next.imgUrl.length - 1) {
						nextBtn.setText(nextStr);
						nextBtn.setIcon(nextIco);
					}

					if (next.idx == 0) {
						cancel.setEnabled(false);
					} else if (next.idx < next.imgUrl.length) {
						imgLabel.setIcon(new ImageIcon(next.imgs[--next.idx]));
					}
				}
			};
			showCenter(imgPanel, 0, 0,
					(String) ResourceUtil.get(9029), true, jbOK, jbpre, nextal,
					preal, null, false, false, null, false);
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static void showVIP() {
		final JDialog showDonate = new HCJDialog();
		showDonate.setTitle("VIP Register");
		showDonate.setIconImage(App.SYS_LOGO);
		final java.awt.event.ActionListener exitActionListener = new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				showDonate.dispose();
			}
		};
		showDonate.getRootPane().registerKeyboardAction(exitActionListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		showDonate.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		showDonate.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				showDonate.dispose();
			}
		});

		final JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2, 2, 10, 10));
		// panel.setBorder(new TitledBorder((String)ResourceUtil.get(9010)));
		final JLabel label_ID = new JLabel(VerifyEmailManager.getEmailI18N());
		label_ID.setHorizontalAlignment(SwingConstants.LEFT);
		label_ID.setIcon(new ImageIcon(ImageSrc.ACCOUNT_ICON));
		panel.add(label_ID);
		final JTextField donateID = new JTextField("");
		donateID.setColumns(15);
//		if(ResourceUtil.validEmail(IConstant.getUUID())){
			donateID.setText(IConstant.getUUID());//使用旧0帐号或邮箱
//		}
		panel.add(donateID);
		final JLabel label_key = new JLabel("Token");
		label_key.setIcon(new ImageIcon(ResourceUtil
				.getResource("hc/res/vip_22.png")));
		label_key.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(label_key);
		final JTextField donateKey = new JTextField("");
		panel.add(donateKey);

		final JButton jbOK = new JButton("OK", new ImageIcon(ImageSrc.OK_ICON));
		jbOK.setText((String) ResourceUtil.get(IContext.OK));
		showDonate.getRootPane().setDefaultButton(jbOK);
		final JButton jbExit = new JButton("", new ImageIcon(ImageSrc.CANCEL_ICON));
		jbExit.setText((String) ResourceUtil.get(1018));

		jbOK.addActionListener(new HCActionListener(new Runnable() {
            @Override
			public void run() {
				final String emailID = donateID.getText().trim();
				if(ResourceUtil.validEmail(emailID) == false){
					App.showMessageDialog(showDonate,
							(String)ResourceUtil.get(9073),
							(String) ResourceUtil.get(IContext.ERROR),
							JOptionPane.ERROR_MESSAGE);
					donateID.selectAll();
					return;
				}

				final String donateToken = donateKey.getText().trim();

				final String result = RootServerConnector.bindDonateKey(emailID, donateToken);
				if (result == null) {
					App.showMessageDialog(null,
							"Error connect to server! try again.",
							ResourceUtil.getErrorI18N(),
							JOptionPane.ERROR_MESSAGE);
					return;
				} else if (result.equals(RootServerConnector.ROOT_AJAX_OK)) {
					showDonate.dispose();

					TokenManager.changeTokenFromUI(emailID, donateToken);
					
					final JPanel jpanel = new JPanel();
					jpanel.add(new JLabel(
							"<html>success active VIP token. Login ID is [<STRONG>" + emailID + "</STRONG>]</html>",
							new ImageIcon(ImageSrc.OK_ICON),
							SwingConstants.LEFT));
					App.showCenterPanel(jpanel, 0, 0, "Success");

					return;
				} else {
					final JPanel jpanel = new JPanel();
					jpanel.add(new JLabel(
							"<html><STRONG>Invalid Token</STRONG><BR><BR>Please check the token again, "
									+ "<BR>or email help@homecenter.mobi</html>",
							App.getSysIcon(App.SYS_ERROR_ICON),
							SwingConstants.LEFT));
					App.showCenterPanel(jpanel, 0, 0, "Unknow Status");
				}
			}
		}, threadPoolToken));
		jbExit.addActionListener(exitActionListener);
		final JPanel allPanel = new JPanel();
		allPanel.setLayout(new BorderLayout());
		panel.setBorder(new TitledBorder(""));
		allPanel.add(panel, BorderLayout.CENTER);
		final JPanel jPanel3 = new JPanel();
		allPanel.add(jPanel3, BorderLayout.SOUTH);
		jPanel3.add(jbOK, null);
		JButton toVIP = null;
		try {
			toVIP = new JButton("buy token to be VIP",
					new ImageIcon(ImageIO.read(ResourceUtil
							.getResource("hc/res/vip_22.png"))));
			toVIP.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					String targetURL;
					try {
						targetURL = HttpUtil.buildLangURL("pc/vip.htm", null);
						HttpUtil.browseLangURL(targetURL);
					} catch (final Exception e1) {
					}
				}
			}, threadPoolToken));
			jPanel3.add(toVIP, null);
		} catch (final IOException e1) {
		}
		jPanel3.add(jbExit, null);
		showDonate.add(allPanel);
		showDonate.pack();

		showCenter(showDonate);

	}

	private static void startRegDialog() {
//		String uuid = IConstant.getInstance().getAjaxForSimu(
//				"http://homecenter.mobi/ajax/call.php?f=uuid", false);

//		if (uuid == null) {
//			App.showMessageDialog(null,
//					(String) ResourceUtil.get(1000),
//					(String) ResourceUtil.get(IContext.ERROR),
//					JOptionPane.ERROR_MESSAGE);
//			ExitManager.exit();
//		} else {
//			uuid = UUIDUtil.addCheckCode("0" + uuid);
//
//			// http://localhost:8080/images/qr.php?txt=helloWorld%E4%B8%AD%E5%9B%BD123456780
//			// 返回一个PNG图片
//			App.showImageURLWindow(uuid,
//					"http://homecenter.mobi/images/qr.php?txt="
//							+ uuid);
//
//			PropertiesManager.setValue(PropertiesManager.p_uuid,
//					uuid);

			final String pwd1 = "", pwd2 = "";

			showInputPWDDialog("", pwd1, pwd2, true);
			// storePWD("123456");
			//
			// PropertiesManager.setValue(PropertiesManager.p_IsInitPWD,
			// IConstant.TRUE);
			// PropertiesManager.saveFile();
			//
			// startAfterInfo();

			if (RootConfig.getInstance().isTrue(
					RootConfig.p_First_Reg_Tuto)) {
				ConditionWatcher.addWatcher(new IWatcher() {
					@Override
					public boolean watch() {
						if (ContextManager.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON) {
							// 服务器上线，启动引导运行手机端
							final JPanel totuQuest = new JPanel();
							totuQuest.add(new JLabel(
									((String) ResourceUtil
											.get(9036)) + "?",
									App.getSysIcon(App.SYS_QUES_ICON),
									SwingConstants.LEFT));
							final JButton jbOK =new JButton(
										(String) ResourceUtil
												.get(IContext.OK),
												new ImageIcon(ImageSrc.OK_ICON));
							App.showCenterOKDisposeDelayMode(
									totuQuest,
									300,
									120,
									((String) ResourceUtil
											.get(9029)) + "?",
									true,
									jbOK,
									null, new HCActionListener(new Runnable() {
										@Override
										public void run() {
											showTuto();
										}
									}, threadPoolToken), new HCActionListener(new Runnable() {
										@Override
										public void run() {
											showLockWarning();
										}
									}, threadPoolToken), true, null,
									false, false, null, false, false);
							return true;
						} else {
							return false;
						}
					}

					@Override
					public void setPara(final Object p) {
					}

					@Override
					public boolean isCancelable() {
						return false;
					}

					@Override
					public void cancel() {
					}
				});
			}else{
				ConditionWatcher.addWatcher(new IWatcher() {
					@Override
					public boolean watch() {
						if (ContextManager.cmStatus == ContextManager.STATUS_READY_TO_LINE_ON) {
							showLockWarning();
							return true;
						}else{
							return false;
						}
					}

					@Override
					public void setPara(final Object p) {
					}

					@Override
					public void cancel() {
					}

					@Override
					public boolean isCancelable() {
						return false;
					}
				});
			}
//		end if uuid
//		}
	}
	
	private static void showLockWarning(){
		if(IDArrayGroup.checkAndAdd(IDArrayGroup.MSG_ID_LOCK_SCREEN) == false){
			return;
		}
		
		final JPanel lockPanel = new JPanel(new BorderLayout(0, 20));
		//final String targetURL = "https://forums.oracle.com/thread/1279871";
		Icon cong_icon = null;
		try {
			cong_icon = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/art_cong.png")));
		} catch (final IOException e1) {
		}
		final int panelWidth = 500;
		final JPanel congPanel = new JPanel(new BorderLayout());
		final JLabel congLabel = new JLabel
				("<html><body style=\"width:" + (panelWidth - cong_icon.getIconWidth()) + "\">"
						+ StringUtil.replace((String)ResourceUtil.get(9065), "{uuid}", IConstant.getUUID()) + "<BR>" +
						(String)ResourceUtil.get(9089) +
						"</body></html>");
		congPanel.add(congLabel, BorderLayout.CENTER);
		congPanel.add(new JLabel(cong_icon), BorderLayout.WEST);
		
		final boolean isLockWarn = RootConfig.getInstance().isTrue(RootConfig.p_Lock_Warn_First_Login);

		if(isLockWarn){
			congPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		}
		
		//L.V = L.O ? false : LogManager.log("os.version : " + System.getProperty("os.name"));
		if(isLockWarn && 
				(ResourceUtil.isWindowsXP() || ResourceUtil.isWindows2003()
				|| ResourceUtil.isWindows2008() || ResourceUtil.isWindowsVista())){
			lockPanel.add(congPanel, BorderLayout.NORTH);

			final JLabel lockLabel = new JLabel
					("<html><body style=\"width:" + panelWidth + "\">" +
							"<STRONG>Important : </STRONG><BR><BR>In " + System.getProperty("os.name") + " (<STRONG>not</STRONG> Windows 7 64bit or other), when mobile is accessing desktop, " +
							"press Win + L to lock screen(or screen save is triggered), mobile phone will display full black. " +
	//						"<BR>for more, click <a href=''>" + targetURL + "</a>" + 
							"<BR><BR>Windows 7 (Fedora 19) works well in lock mode." +
							"<BR><BR>Strongly recommended to install Windows 7." +
							"</body></html>",
					SwingConstants.LEFT);
			lockLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			lockPanel.add(lockLabel, BorderLayout.CENTER);
	//			lockLabel.addMouseListener(new MouseAdapter(){
	//			 public void mouseClicked(MouseEvent e) {
	//				 HttpUtil.browseLangURL(targetURL);
	//			 }
	//		});
		}else{
			lockPanel.add(congPanel, BorderLayout.CENTER);
		}
		
		if(PropertiesManager.getValue(PropertiesManager.p_isReportException) == null){
			PropertiesManager.setValue(PropertiesManager.p_isReportException, IConstant.TRUE);
			PropertiesManager.saveFile();
		}
		
		lockPanel.add(buildReportExceptionCheckBox(true), BorderLayout.SOUTH);
		
		App.showCenterOKDisposeDelayMode(
				lockPanel,
				0, 0,
				((String) ResourceUtil
						.get(IContext.INFO)),
				false,
				null,
				null, null, null, true, null,
					false, false, null, false, false);
	}
	
	
	public static void printThreadStackForProjectContext(final String name) {
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				ClassUtil.printThreadStack(name);
				System.gc();
			}
		}, threadPoolToken);
	}
	
	public static Window buildCloseableWindow(final boolean newFrame, final JFrame owner,
			final String title, final boolean model) {
		if (newFrame) {
			 class CloseableFrame extends HCJFrame implements ClosableWindow {
				 public CloseableFrame(final String title){
					 super(title);
				 }
				 ActionListener al;
				@Override
				public void notifyClose() {
					al.actionPerformed(null);
				}
	
				@Override
				public void setCloseAction(final ActionListener al) {
					this.al = al;
				}
		    }
			return new CloseableFrame(title) ;
		} else {
			class CloseableDialog extends HCJDialog implements ClosableWindow {
				 public CloseableDialog(final Frame owner, final String title, final boolean modal){
					 super(owner, title, modal);
				 }
				 ActionListener al;
				@Override
				public void notifyClose() {
					al.actionPerformed(null);
				}
	
				@Override
				public void setCloseAction(final ActionListener al) {
					this.al = al;
				}
			}
			return new CloseableDialog(owner, title, model);
		}
	}

	public static JButton buildDefaultCancelButton() {
		return new JButton((String) ResourceUtil.get(IContext.CANCEL), new ImageIcon(ImageSrc.CANCEL_ICON));
	}

	public static JButton buildDefaultOKButton() {
		return new JButton((String) ResourceUtil.get(IContext.OK), new ImageIcon(ImageSrc.OK_ICON));
	}

	public static JButton buildDefaultCloseButton() {
		return new JButton(
				(String) ResourceUtil.get(9093), new ImageIcon(ImageSrc.CANCEL_ICON));
	}

	public static final JCheckBox buildReportExceptionCheckBox(final boolean withActionListener) {
		final JCheckBox enableReportException = new JCheckBox((String)ResourceUtil.get(9168));
		enableReportException.setToolTipText((String)ResourceUtil.get(9169));
		
		final String isOldReportException = PropertiesManager.getValue(PropertiesManager.p_isReportException, IConstant.FALSE);
		enableReportException.setSelected(isOldReportException.equals(IConstant.TRUE));
		
		if(withActionListener){
			enableReportException.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					final boolean isSelected = enableReportException.isSelected();
					PropertiesManager.setValue(PropertiesManager.p_isReportException, isSelected?IConstant.TRUE:IConstant.FALSE);
					if(isSelected){
						ExceptionReporter.start();
					}else{
						ExceptionReporter.stop();
					}
					PropertiesManager.saveFile();
				}
			}, threadPoolToken));
		}
		return enableReportException;
	}

	static {
		if(IConstant.isHCServer() == false){
			throw new Error("Cant invoke App method in Relay/Monitor Server!");
		}
	}
}