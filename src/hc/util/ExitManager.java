package hc.util;

import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.L;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.Stack;
import hc.server.AbstractDelayBiz;
import hc.server.DelayServer;
import hc.server.KeepaliveManager;
import hc.server.ScreenServer;

public class ExitManager {
	private static Stack delayBiz = null;
	
	public static void addDelayBiz(AbstractDelayBiz biz){
		if(delayBiz == null){
			delayBiz = new Stack();
		}
		delayBiz.push(biz);
	}
	
	public static void exit(){
		hc.core.L.V=hc.core.L.O?false:LogManager.log("Start ExitManager");
		
		startForceExitThread();
		
		
		try{
			//LoginDialog可能退出时，使用本逻辑
			DelayServer.getInstance().shutDown();
		}catch (Exception e) {
			//可能出现NullPointerException
		}
		
		try{
			//清除等待客户上线的记录
			if(IConstant.serverSide){
				try{
					String out = RootServerConnector.delLineInfo(TokenManager.getToken(), false);
					if(out != null && out.length() == 0){
						TokenManager.clearUPnPPort();
					}
				}catch (Exception e) {
					//无网络会出现本情形
					CCoreUtil.globalExit();
					//可能出现NullPointerException
				}
			}
			
			try{
				hc.server.ui.video.CapStream cs = hc.server.ui.video.CapStream.getInstance(true);
				if(cs != null){
					cs.stop();
				}
			}catch (Throwable e) {
				//有可能未安装，会导致本异常
			}
			PropertiesManager.saveFile();
			
			ScreenServer.emptyScreen();
			
			SIPManager.close();
			
			try{
				if(KeepaliveManager.dServer != null){
					KeepaliveManager.dServer.shutdown();
				}
				if(KeepaliveManager.nioRelay != null){
					KeepaliveManager.nioRelay.shutdown();
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
			HCTimer.shutDown();

			ContextManager.exit();
			
			if(delayBiz != null){
				int size = delayBiz.size();
				for (int i = 0; i < size; i++) {
					AbstractDelayBiz biz = (AbstractDelayBiz)delayBiz.elementAt(i);
					biz.doBiz();
				}
			}
		
		}catch (Exception e) {
			e.printStackTrace();
		}
		LogManager.exit();
		//由于与starter包协同，所以不能执行该命令，因为starter可能正在下载中
//		System.exit(0);
	}

	private static boolean isStartedForceExitThread = false;
	
	public static void startForceExitThread() {
		if(isStartedForceExitThread){
			return;
		}
		
		isStartedForceExitThread = true;
		
		new Thread(){
			long curr = System.currentTimeMillis();
			
			public void run(){
				while(true){
//					L.V = L.O ? false : LogManager.log("Start 10-Seconds ExitManager...");
					if(System.currentTimeMillis() - curr > 10 * 1000){
						L.V = L.O ? false : LogManager.log("force kill to exit.");
						ContextManager.forceExit();
					}else{
						try{
							Thread.sleep(1000);
						}catch (Exception e) {
							
						}
					}
				}
			}
		}.start();
	}
}
