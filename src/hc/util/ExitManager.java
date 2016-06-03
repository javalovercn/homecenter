package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.L;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.KeepaliveManager;
import hc.server.PlatformManager;
import hc.server.ProcessingWindowManager;
import hc.server.ScreenServer;
import hc.server.SingleJFrame;
import hc.server.rms.RMSLastAccessTimeManager;
import hc.server.util.HCLimitSecurityManager;

public class ExitManager {
	public static void startExitSystem(){
		CCoreUtil.checkAccess();

		//直接采用主线程，会导致退出提示信息会延时显示，效果较差
		ProcessingWindowManager.showCenterMessage((String)ResourceUtil.get(9067));
		
		SingleJFrame.disposeAll();

		L.V = L.O ? false : LogManager.log("Start ExitManager");
		
		RMSLastAccessTimeManager.checkIdleAndRemove();
		RMSLastAccessTimeManager.save();

		HttpUtil.notifyStopServer(false, null);
		//以上逻辑不能置于notifyShutdown中，因为这些方法有可能被其它外部事件，如手机下线，中继下线触发。
		
		try{
			RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_ServerReq_STR);
		}catch (final Exception e) {
			//Anroid环境下，有可能不连接服务器时，产生异常。需catch。
		}
		ExitManager.startForceExitThread();
		ContextManager.notifyShutdown();		
	}
	
	public static void exit(){
		CCoreUtil.checkAccess();
		
		startForceExitThread();
		
		try{
			//清除等待客户上线的记录
			if(IConstant.serverSide){
				try{
					final String out = RootServerConnector.delLineInfo(TokenManager.getToken(), false);
					if(out != null && out.length() == 0){
						TokenManager.clearUPnPPort();
					}
				}catch (final Exception e) {
					//无网络会出现本情形
				}
			}
			
			PlatformManager.getService().stopCapture();
			
			ScreenServer.emptyScreen();
			
			SIPManager.close();
			
			try{
				if(KeepaliveManager.getDirectServer() != null){
					KeepaliveManager.getDirectServer().shutdown();
				}
				if(KeepaliveManager.getNIORelay() != null){
					KeepaliveManager.getNIORelay().shutdown();
				}
			}catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
			HCTimer.shutDown();

//			ContextManager.exit();
			
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
		LogManager.exit();
		
		ProcessingWindowManager.disposeProcessingWindow();
		
		HCLimitSecurityManager.getHCSecurityManager().getHCEventQueue().shutdown();
		//由于与starter包协同，所以不能执行该命令，因为starter可能正在下载中
//		System.exit(0);
	}

	private static boolean isStartedForceExitThread = false;
	
	public static void startForceExitThread() {
		if(isStartedForceExitThread){
			return;
		}
		
		isStartedForceExitThread = true;
		
		final Thread foreceExit = new Thread(){
			final long curr = System.currentTimeMillis();
			
			@Override
			public void run(){
				L.V = L.O ? false : LogManager.log("ready " + (App.EXIT_MAX_DELAY_MS/1000) + "-seconds to force exit...");
				while(true){
					if(System.currentTimeMillis() - curr > App.EXIT_MAX_DELAY_MS){
						L.V = L.O ? false : LogManager.log("force to exit.");
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
