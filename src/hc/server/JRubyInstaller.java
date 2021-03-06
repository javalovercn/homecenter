package hc.server;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import hc.App;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.GlobalConditionWatcher;
import hc.core.IConstant;
import hc.core.IWatcher;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.SessionManager;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.design.Designer;
import hc.server.ui.design.J2SESession;
import hc.server.util.SafeDataManager;
import hc.server.util.ServerUtil;
import hc.util.CheckSum;
import hc.util.HttpUtil;
import hc.util.IBiz;
import hc.util.MultiThreadDownloader;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.SecurityDataProtector;

public class JRubyInstaller {
	public static final String JRUBY_VERSION = "1.7.27";
	public static final String JRUBY_ANDROID_VERSION = "1.7.27.1";
	public static final String JRUBY_VERSION_1_7_27 = "1.7.27";

	/**
	 * 如果jruby.jar升级，由于Android环境下依赖此，重要测试
	 */
	public static final String jrubyjarname = ResourceUtil.getLibNameForAllPlatforms("jruby");

	static MultiThreadDownloader mtd;

	private static String getInnverJRubyDownloadFile(final String outerVersion) {
		if (ResourceUtil.isAndroidServerPlatform()) {
			return getInnverAndroidJRubyDownloadFile(outerVersion);
		}

		final String[] versions = { JRUBY_VERSION };
		final String[] fileName = { "jruby-complete-1.7.27.jar" };

		for (int i = 0; i < versions.length; i++) {
			if (versions[i].equals(outerVersion)) {
				return fileName[i];
			}
		}

		return fileName[0];
	}

	/**
	 * null means not version.
	 * @param outerVersion
	 * @return
	 */
	private static String getInnverJRubyMD5(final String outerVersion) {
		if (ResourceUtil.isAndroidServerPlatform()) {
			return getInnverAndroidJRubyMD5(outerVersion);
		}

		final String[] versions = { JRUBY_VERSION };
		final String[] innerMD5 = { "dadb699ab987170f82d2ca7ee948f719" };

		for (int i = 0; i < versions.length; i++) {
			if (versions[i].equals(outerVersion)) {
				return innerMD5[i];
			}
		}

		return null;
	}

	private static String getInnverJRubySHA512(final String outerVersion) {
		if (ResourceUtil.isAndroidServerPlatform()) {
			return getInnverAndroidJRubySHA512(outerVersion);
		}

		final String[] versions = { JRUBY_VERSION };
		final String[] innerMD5 = {
				"2ef1a6503a9e245f47459bad3a36810514125bf5bb94593a615bd58f7869e961c71e3686f8181fd38f9c72d3f89a2c2e64a69e4a10bf6ded6fc9f206b39f9522" };

		for (int i = 0; i < versions.length; i++) {
			if (versions[i].equals(outerVersion)) {
				return innerMD5[i];
			}
		}

		return null;
	}

	private static String getInnverAndroidJRubyDownloadFile(final String outerVersion) {
		// RubotoCore version 1.0.5
		// 1. Updated to JRuby 1.7.19
		// 2. Updated to Ruboto 1.3.0
		// 3. Updated to ActiveRecord 4.1.10
		// 4. Updated to activerecord-jdbc-adapter 1.3.15
		// 5. Added thread_safe gem
		final String[] versions = { JRUBY_ANDROID_VERSION };
		final String[] fileName = { "jruby-complete-1.7.27.dex.jar" };

		for (int i = 0; i < versions.length; i++) {
			if (versions[i].equals(outerVersion)) {
				return fileName[i];
			}
		}

		return fileName[0];
	}

	private static String getInnverAndroidJRubyMD5(final String outerVersion) {
		//由于Android 7.0停用URLClassLoader的parent，故修改org.jruby.util.JRubyClassLoader，导致非原jar的dex，而产生md5和sha512变化
		final String[] versions = { JRUBY_ANDROID_VERSION };
		final String[] innerMD5 = { "16ac51a429fed12ae691b383b5f0b1c2" };

		for (int i = 0; i < versions.length; i++) {
			if (versions[i].equals(outerVersion)) {
				return innerMD5[i];
			}
		}

		return null;
	}

	private static String getInnverAndroidJRubySHA512(final String outerVersion) {
		final String[] versions = { JRUBY_ANDROID_VERSION };
		final String[] innerMD5 = {
				"ad9e055f0b9bbbab1c6670656d92b9b8624f9d84c0d5e03ea302f35fa1adbaa3093e24675d2df9c4f2508c128662b3dbc8b88d1f18e83de2f281be6c90b22a2d" };

		for (int i = 0; i < versions.length; i++) {
			if (versions[i].equals(outerVersion)) {
				return innerMD5[i];
			}
		}

		return null;
	}

	public static boolean isJRubyInstalled() {
		final String localVer = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);
		return localVer != null;
	}

	public static boolean checkNeedUpgradeJRuby() {
		if (isJRubyInstalled() == false) {
			return true;
		}

		final String localVer = PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer);
		final String romoteVer = getRomoteVer();
		final boolean isJRubyNeedUpgrade = StringUtil.higher(romoteVer, localVer);
		if (isJRubyNeedUpgrade) {
			LogManager.log("[JRubyInstaller] remote version : " + romoteVer + ", local version : " + localVer);
		}
		return isJRubyNeedUpgrade;
	}

	private static String getRomoteVer() {
		return RootConfig.getInstance()
				.getProperty(ResourceUtil.isAndroidServerPlatform() ? RootConfig.p_AndroidJRubyVer : RootConfig.p_JRubyVer);
	}

	public static void startInstall() {
		CCoreUtil.checkAccess();

		if (LinkProjectStatus.tryEnterStatus(null, LinkProjectStatus.MANAGER_JRUBY_INSTALL)) {
			LogManager.log("[JRubyInstaller] start download jruby engine.");
			new Thread() {
				@Override
				public void run() {
					callDownload(true);
				}
			}.start();
		} else {
			LogManager.errToLog("[JRubyInstaller] fail to enter status : MANAGER_JRUBY_INSTALL");
		}
	}

	static Object waitUpgradeLock;

	static boolean isCheckUpgradForEnginOnce = false;

	/**
	 * 只返回true
	 * 
	 * @return
	 */
	public static boolean checkNeedUpgradeJRubyAndWaitForEngine() {
		if (isCheckUpgradForEnginOnce) {// 由于可能长时间运行，所以只启动时，执行一次
			return true;
		}
		isCheckUpgradForEnginOnce = true;

		if (checkNeedUpgradeJRuby() == false) {
			return true;
		}

		synchronized (JRubyInstaller.class) {
			if (waitUpgradeLock == null) {
				waitUpgradeLock = new Object();
			}
		}

		synchronized (waitUpgradeLock) {
			try {
				waitUpgradeLock.wait();
			} catch (final InterruptedException e) {
			}
		}

		return true;
	}

	static void callDownload(final boolean isFirstDownload) {
		final boolean isJRubyInstalled = isJRubyInstalled();

		if (isFirstDownload) {
			LogManager.log("[JRubyInstaller] download/upgrade JRuby engine...");
		} else {
			LogManager.log("[JRubyInstaller] fail on download and retry download JRuby engine...");
		}

		if (isJRubyInstalled == false) {
			GlobalConditionWatcher.addWatcher(new IWatcher() {
				final long ms = System.currentTimeMillis();

				@Override
				public boolean watch() {
					final CoreSession coreSS = SessionManager.getPreparedSocketSession();// 注意：只需取一个即可，无需all
					if (System.currentTimeMillis() - ms > 5000
							|| (coreSS != null && coreSS.isContextStatus(ContextManager.STATUS_READY_TO_LINE_ON))) {
						TrayMenuUtil.displayMessage(ResourceUtil.getInfoI18N(), ResourceUtil.get(9066), // JRuby is downloading,
								// which is powerful
								// core component.
								IConstant.INFO, null, 0);
						return true;
					}
					return false;
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
		}

		final String _lastJrubyVer = getRomoteVer();

		final String md5 = getInnverJRubyMD5(_lastJrubyVer);
		final String sha512 = getInnverJRubySHA512(_lastJrubyVer);
		if(md5 == null || sha512 == null) {
			LogManager.err("can't download JRuby, please upgrade server first!");
			LinkProjectStatus.exitStatus();
			closeProgressWindow();
			return;
		}
		
		final CheckSum checkSum = new CheckSum(md5, sha512);

		String fromURL = RootServerConnector.HTTS_HC_44X + "/download/" + getInnverJRubyDownloadFile(_lastJrubyVer);
		if (PropertiesManager.isSimu()) {
			fromURL = HttpUtil.replaceSimuURL(fromURL, true);
		}
		LogManager.log("[JRubyInstaller] ready to download JRuby engine from : " + fromURL);
		final String storeFile = JRubyInstaller.jrubyjarname;
		final File rubyjar = new File(ResourceUtil.getBaseDir(), storeFile);
		final File rubyjarbak = new File(ResourceUtil.getBaseDir(), "bak" + storeFile);
		if (rubyjar.exists()) {
			rubyjar.renameTo(rubyjarbak);
		}

		final IBiz biz = new IBiz() {
			@Override
			public void start() {
				PropertiesManager.setValue(PropertiesManager.p_jrubyJarVer, _lastJrubyVer);
				PropertiesManager.saveFile();

				ServerUtil.clearJRubyClassLoader();

				if (waitUpgradeLock != null) {
					synchronized (waitUpgradeLock) {
						waitUpgradeLock.notifyAll();
					}
				}

				rubyjarbak.delete();

				try {
					LogManager.log("[JRubyInstaller] successful installed/upgrade JRuby to version : " + _lastJrubyVer);

					RootServerConnector.notifyLineOffType(J2SESession.NULL_J2SESESSION_FOR_PROJECT, "lof=jrubyOK");

					SecurityDataProtector.init();// Android环境下进行数据加密

					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							if (isJRubyInstalled == false) {// 初次安装
								TrayMenuUtil.displayMessage(ResourceUtil.getInfoI18N(), ResourceUtil.get(9108), IConstant.INFO, null, 0);// Successful
																																			// installed
																																			// JRuby
																																			// script
																																			// engine!

								TrayMenuUtil.disableMessageForWhile();
								App.showCongratulation();
								Designer.setupMyFirstAndApply();
							} else {
								// ServerUIUtil.restartResponsorServer(null,
								// null);
							}

							J2SESessionManager.startNewIdleSession();// 安卓服务器可能升级JRuby时，不能接入客户端
							SafeDataManager.startSafeBackupProcess(true, true);
						}
					});
				} finally {
					LinkProjectStatus.exitStatus();
					closeProgressWindow();
				}
			}

			@Override
			public void setMap(final HashMap map) {
			}
		};
		final IBiz failBiz = new IBiz() {
			@Override
			public void start() {
				if (isJRubyInstalled && isFirstDownload) {
					if (rubyjarbak.exists()) {
						rubyjarbak.renameTo(rubyjar);
					}

					if (waitUpgradeLock != null) {
						synchronized (waitUpgradeLock) {
							waitUpgradeLock.notifyAll();
						}
					}

					LinkProjectStatus.exitStatus();
					closeProgressWindow();
				} else {
					if (isJRubyInstalled == false) {
						redownload();
					}
				}
			}

			@Override
			public void setMap(final HashMap map) {
			}
		};
		if (isShutdown) {
			return;
		}
		mtd = new MultiThreadDownloader();
		if (isShutdown) {
			return;
		}
		refreshProgressWindow();

		mtd.download(StringUtil.split(fromURL, RootConfig.CFG_SPLIT), rubyjar, checkSum, biz, failBiz,
				ResourceUtil.isNonUIServer() ? false : true, false);//显示下载进度forUIServer
	}

	private static boolean isShutdown;

	public static void shutdown() {
		CCoreUtil.checkAccess();

		isShutdown = true;
		final MultiThreadDownloader snap = mtd;
		if (snap != null) {
			snap.shutdown();
			// SafeDataManager.startSafeBackupProcess(true, true);
		}
	}

	private static void redownload() {
		try {
			LogManager.log("[JRubyInstaller] fail to get download online lib information, wait for a moment...");
			Thread.sleep(5000);
			callDownload(false);
		} catch (final Exception e) {
		}
	}

	public static JProgressBar getFinishPercent() {
		while (mtd == null) {
			try {
				Thread.sleep(100);
			} catch (final Exception e) {
			}
		}
		return mtd.getFinishPercent();
	}

	public synchronized static void closeProgressWindow() {
		final Window snapWindow = progressWindow;
		if (snapWindow != null) {
			snapWindow.dispose();
			progressWindow = null;
		}
	}

	public synchronized static void refreshProgressWindow() {
		final Window snapWindow = progressWindow;
		if (snapWindow != null && snapWindow.isVisible()) {
			final Container c = snapWindow.getParent();
			snapWindow.dispose();
			progressWindow = null;

			showProgressWindow(((c != null && c instanceof JFrame) ? (JFrame) c : null));
		}
	}

	private static Window progressWindow;

	public synchronized static void showProgressWindow(final JFrame parent) {
		if (progressWindow == null || (progressWindow.isVisible() == false)) {
			final JLabel label = new JLabel("<html>" + ResourceUtil.get(9084) +
			// "<br>if we have finished, a notify window will display." +
					"</html>", App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING);
			final JPanel panel = new JPanel(new BorderLayout());
			panel.add(label, BorderLayout.CENTER);
			final JProgressBar finishPercent = getFinishPercent();
			if (finishPercent != null) {
				panel.add(finishPercent, BorderLayout.SOUTH);
			}

			final ActionListener listener = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					closeProgressWindow();
				}
			});

			final JButton closeButton = App.buildDefaultCloseButton();
			progressWindow = App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getInfoI18N(), false, closeButton, null, listener, listener, parent,
					true, true, null, false, false);
		} else {
			progressWindow.toFront();
		}
	}

}
