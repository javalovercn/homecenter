package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.EventCenterDriver;
import hc.core.HCTimer;
import hc.core.L;
import hc.core.RootServerConnector;
import hc.core.SessionManager;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ThreadPool;
import hc.server.JRubyInstaller;
import hc.server.PlatformManager;
import hc.server.PlatformService;
import hc.server.ProcessingWindowManager;
import hc.server.ScreenServer;
import hc.server.SingleJFrame;
import hc.server.TrayMenuUtil;
import hc.server.rms.RMSLastAccessTimeManager;
import hc.server.ui.J2SESessionManager;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.LinkMenuManager;
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.SafeDataManager;
import hc.server.util.StarterParameter;
import third.hsqldb.Database;
import third.hsqldb.DatabaseManager;

public class ExitManager {
	private static boolean isStartingExitSystem;

	public static boolean isStartingExitSystem() {
		return isStartingExitSystem;
	}

	public static void startExitSystem() {
		ResourceUtil.checkHCStackTrace();

		synchronized (ExitManager.class) {
			if (isStartingExitSystem) {
				return;
			}
			isStartingExitSystem = true;
		}

		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				try {
					final String out = RootServerConnector.delLineInfo(TokenManager.getToken());// 只在此唯一使用delLineInfo，isMobileLineIn停用
					if (out != null && out.length() == 0) {
						TokenManager.clearUPnPPort();
					}
				} catch (final Exception e) {
					// 无网络会出现本情形
				}
			}
		});

		// 不考虑Android服务器关闭又进入，因为原界面仍含提示信息。

		JRubyInstaller.shutdown();// 下载中，不能safebackup，导致部分用户设置，比如开机启动丢失。

		// 直接采用主线程，会导致退出提示信息会延时显示，效果较差
		ProcessingWindowManager.showCenterMessage(ResourceUtil.get(9067));

		SafeDataManager.notifyShutdown();

		if (ResourceUtil.isAndroidServerPlatform()) {
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(2000);
					} catch (final Exception e) {
					}
					PlatformManager.getService().doExtBiz(PlatformService.BIZ_GO_HOME, null);
				}
			});
		}

		LinkMenuManager.closeLinkPanel();
		SingleJFrame.disposeAll();

		LogManager.log("Start ExitManager");
		// SessionManager.notifyShutdown();
		// ServerUIUtil.stop();

		ExitManager.startForceExitThread();

		ServerUIUtil.promptAndStop(false, null);// 会stopAllSession

		L.V = L.WShop ? false : LogManager.log("GECD waitForAllDone...");
		EventCenterDriver.waitForAllDriverDone();
		L.V = L.WShop ? false : LogManager.log("GECD waitForAllDone OK!");

		// 注意：AI manager永久使用连接，所以只能此处进行关闭
		try {
			DatabaseManager.closeDatabases(Database.CLOSEMODE_NORMAL);
		} catch (final Throwable e) {
			e.printStackTrace();
			// third.hsqldb.HsqlException: file input/output error
			// java.io.FileNotFoundException:
			// /Users/homecenter/Documents/eclipse_workspace/homecenter/test_run/user_data/testFull.Mlet/_HC/DB/test/test.script.new
			// (No such file or directory)
			// /Users/homecenter/Documents/eclipse_workspace/homecenter/test_run/user_data/testFull.Mlet/_HC/DB/test/test.script.new
			// at third.hsqldb.error.Error.error(Error.java:139)
			// at
			// third.hsqldb.scriptio.ScriptWriterEncode.openFile(ScriptWriterEncode.java:116)
			// at
			// third.hsqldb.scriptio.ScriptWriterBase.<init>(ScriptWriterBase.java:174)
			// at
			// third.hsqldb.scriptio.ScriptWriterText.<init>(ScriptWriterText.java:126)
			// at
			// third.hsqldb.scriptio.ScriptWriterEncode.<init>(ScriptWriterEncode.java:82)
			// at third.hsqldb.persist.Log.writeScript(Log.java:725)
			// at third.hsqldb.persist.Log.close(Log.java:203)
			// at third.hsqldb.persist.Logger.close(Logger.java:720)
			// at third.hsqldb.Database.close(Database.java:642)
			// at
			// third.hsqldb.DatabaseManager.closeDatabases(DatabaseManager.java:121)
		}
		LogManager.log("closed all HSQLDB databases.");

		RMSLastAccessTimeManager.checkIdleAndRemove();
		RMSLastAccessTimeManager.save();

		SafeDataManager.setPowerOffOK();

		TrayMenuUtil.removeTray(App.getThreadPoolToken());

		// HttpUtil.notifyStopServer(false, null);
		// 以上逻辑不能置于notifyShutdown中，因为这些方法有可能被其它外部事件，如手机下线，中继下线触发。

		J2SESessionManager.stopAllSession(true, false);// 注意：notifyRelineon是false

		exit();
		LogManager.exit();

		ExceptionReporter.shutdown();
		ThreadPool.shutdown();
		SessionManager.shutdown();
		if (L.isInWorkshop) {
			System.out.println("done [startExitSystem]!");
		}
	}

	private static void exit() {
		try {
			// EventBack=>ConditionWatch=>HCTimer，所以要提前关闭，否则部分对象为null，比如responsor
			// 所以提前到此
			HCTimer.shutDown();
			// 不能立即ThreadPool.shutdown();

			PlatformManager.getService().stopCaptureIfEnable();

			final CoreSession[] coreSSS = SessionManager.getAllSocketSessions();
			for (int i = 0; i < coreSSS.length; i++) {
				final CoreSession coreSS = coreSSS[i];

				ScreenServer.emptyScreen((J2SESession) coreSS);
				coreSS.closeHC();
			}

			try {
				if (StarterParameter.getDirectServer() != null) {
					StarterParameter.getDirectServer().shutdown();
				}
				if (StarterParameter.getNIORelay() != null) {
					StarterParameter.getNIORelay().shutdown();
				}
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}

			// ContextManager.exit();

		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}

		ProcessingWindowManager.disposeProcessingWindow();

		HCLimitSecurityManager.getHCEventQueue().shutdown();
		// 由于与starter包协同，所以不能执行该命令，因为starter可能正在下载中
		// System.exit(0);
	}

	private static boolean isStartedForceExitThread = false;

	private static void startForceExitThread() {
		if (isStartedForceExitThread) {
			return;
		}

		isStartedForceExitThread = true;

		final Thread foreceExit = new Thread() {
			final long curr = System.currentTimeMillis();

			@Override
			public void run() {
				LogManager.log("ready " + (App.EXIT_MAX_DELAY_MS / 1000) + "-seconds to force exit...");
				try {
					Thread.sleep(App.EXIT_MAX_DELAY_MS);
				} catch (final Exception e) {
				}

				try {
					LogManager.log("force to exit.");
					LogManager.exit();

					if (LogManager.INI_DEBUG_ON) {
						System.out.println("-------------------------------------------------------------------------------------------");
						System.out.println("------------------------before force exit, print all thread stack----------------------");
						System.out.println("-------------------------------------------------------------------------------------------");
						ClassUtil.printThreadStack(null);
						System.out.println("-------------------------------------------------------------------------------------------");
					}
				} catch (final Throwable e) {
				}
				PlatformManager.getService().exitSystem();
			}
		};
		foreceExit.setDaemon(true);
		foreceExit.start();
	}
}
