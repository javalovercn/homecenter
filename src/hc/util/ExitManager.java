package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.GlobalEventCenterDriver;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.L;
import hc.core.RootServerConnector;
import hc.core.SessionManager;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ThreadPool;
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
import hc.server.util.HCLimitSecurityManager;
import hc.server.util.StarterParameter;
import third.hsqldb.Database;
import third.hsqldb.DatabaseManager;

public class ExitManager {
	public static void startExitSystem(){
		ResourceUtil.checkHCStackTrace();

		J2SESessionManager.notifyReadyShutdown();
		
		//直接采用主线程，会导致退出提示信息会延时显示，效果较差
		ProcessingWindowManager.showCenterMessage((String)ResourceUtil.get(9067));
		
		if(ResourceUtil.isAndroidServerPlatform()){
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					try{
						Thread.sleep(2000);
					}catch (final Exception e) {
					}
					PlatformManager.getService().doExtBiz(PlatformService.BIZ_GO_HOME, null);
				}
			});
		}
		
		SingleJFrame.disposeAll();

		LogManager.log("Start ExitManager");
//		SessionManager.notifyShutdown();
//		ServerUIUtil.stop();
		final GlobalEventCenterDriver gecd = GlobalEventCenterDriver.getGECD();
		gecd.notifyShutdown();
		
		ExitManager.startForceExitThread();

		L.V = L.WShop ? false : LogManager.log("GECD waitForAllDone...");
		gecd.waitForAllDone();
		L.V = L.WShop ? false : LogManager.log("GECD waitForAllDone OK!");
		ServerUIUtil.promptAndStop(false, null);//会stopAllSession
		//注意：AI manager永久使用连接，所以只能此处进行关闭
		DatabaseManager.closeDatabases(Database.CLOSEMODE_NORMAL);
		LogManager.log("closed all HSQLDB databases.");

		RMSLastAccessTimeManager.checkIdleAndRemove();
		RMSLastAccessTimeManager.save();

    	TrayMenuUtil.removeTray(App.getThreadPoolToken());

//		HttpUtil.notifyStopServer(false, null);
		//以上逻辑不能置于notifyShutdown中，因为这些方法有可能被其它外部事件，如手机下线，中继下线触发。
		
		J2SESessionManager.stopAllSession(true, false, false);//注意：notifyRelineon是false
		
		exit();	
		LogManager.exit();

		ThreadPool.shutdown();
    	SessionManager.shutdown();

	}
	
	private static void exit(){
		try{
			//清除等待客户上线的记录
			if(IConstant.serverSide){
				try{
					final String out = RootServerConnector.delLineInfo(TokenManager.getToken(), false);//只在此唯一使用delLineInfo，isMobileLineIn停用
					if(out != null && out.length() == 0){
						TokenManager.clearUPnPPort();
					}
				}catch (final Exception e) {
					//无网络会出现本情形
				}
			}
			//EventBack=>ConditionWatch=>HCTimer，所以要提前关闭，否则部分对象为null，比如responsor
			//所以提前到此
			HCTimer.shutDown();
			//不能立即ThreadPool.shutdown();
			
			PlatformManager.getService().stopCaptureIfEnable();
			
			final CoreSession[] coreSSS = SessionManager.getAllSocketSessions();
			for (int i = 0; i < coreSSS.length; i++) {
				final CoreSession coreSS = coreSSS[i];
				
				ScreenServer.emptyScreen((J2SESession)coreSS);
				coreSS.closeHC();
			}
			
			try{
				if(StarterParameter.getDirectServer() != null){
					StarterParameter.getDirectServer().shutdown();
				}
				if(StarterParameter.getNIORelay() != null){
					StarterParameter.getNIORelay().shutdown();
				}
			}catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}

//			ContextManager.exit();
			
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		
		ProcessingWindowManager.disposeProcessingWindow();
		
		HCLimitSecurityManager.getHCEventQueue().shutdown();
		//由于与starter包协同，所以不能执行该命令，因为starter可能正在下载中
//		System.exit(0);
	}

	private static boolean isStartedForceExitThread = false;
	
	private static void startForceExitThread() {
		if(isStartedForceExitThread){
			return;
		}
		
		isStartedForceExitThread = true;
		
		final Thread foreceExit = new Thread(){
			final long curr = System.currentTimeMillis();
			
			@Override
			public void run(){
				LogManager.log("ready " + (App.EXIT_MAX_DELAY_MS/1000) + "-seconds to force exit...");
				while(true){
					if(System.currentTimeMillis() - curr > App.EXIT_MAX_DELAY_MS){
						LogManager.log("force to exit.");
						LogManager.exit();
						
						if(LogManager.INI_DEBUG_ON){
							System.out.println("-------------------------------------------------------------------------------------------");
							System.out.println("------------------------before force exit, print all thread stack----------------------");
							System.out.println("-------------------------------------------------------------------------------------------");
							ClassUtil.printThreadStack(null);
							System.out.println("-------------------------------------------------------------------------------------------");
						}
						
						PlatformManager.getService().exitSystem();
					}else{
						try{
							Thread.sleep(1000);
						}catch (final Exception e) {
						}
					}
				}
			}
		};
		foreceExit.setDaemon(true);
		foreceExit.start();
	}
}
