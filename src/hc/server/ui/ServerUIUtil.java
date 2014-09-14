package hc.server.ui;

import hc.core.ContextManager;
import hc.core.MsgBuilder;
import hc.core.RootServerConnector;
import hc.core.sip.SIPManager;
import hc.core.util.LogManager;
import hc.server.AbstractDelayBiz;
import hc.server.DelayServer;
import hc.util.BaseResponsor;
import hc.util.HttpUtil;
import hc.util.PropertiesManager;

import javax.swing.JFrame;

public class ServerUIUtil {
	public static final Boolean LOCK = new Boolean(true);

	public static boolean useMainCanvas = PropertiesManager.isTrue(PropertiesManager.p_IsMobiMenu);
	private static BaseResponsor responsor;
	
	public static BaseResponsor getResponsor(){
		//加锁，以确保获得必须在启动占锁之后
		synchronized (LOCK) {
			return responsor;
		}
	}
	
	private static boolean isStared = false;
	
	public static BaseResponsor restartResponsorServer(){
		synchronized (LOCK) {
			stop();
			
			if(useMainCanvas){
				//MobiUIResponsor
				String className = RootServerConnector.unObfuscate("chs.reev.riud.segi.noMibIUeRpsnoosr");
				try {
					responsor = (BaseResponsor)Class.forName(className).newInstance();
				} catch (Throwable e) {//不能用Exception，因为有可能init error
					LogManager.err("load class:"+className);
					responsor = new DefaultUIResponsor();
				}
			}else{
				responsor = new DefaultUIResponsor();
			}
			try{
				responsor.start();
			}catch (Throwable e) {
				e.printStackTrace();
			}
			
			isStared = true;
			
			return responsor;
		}
	}

	public static void stop() {
		if(responsor != null && isStared){
			try{
				responsor.stop();
			}catch (Throwable e) {
				e.printStackTrace();
			}
			
			isStared = false;
		}
	}

	public static boolean response(String out) {
		ContextManager.getContextInstance().send(MsgBuilder.E_CANVAS_MAIN, out);
		return true;
	}

	public static boolean promptAndStop(final boolean isQuery, final JFrame parent){
		boolean isPrompt = isServing();
		if(isPrompt){
			HttpUtil.notifyStopServer(isQuery, parent);
			
			RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_ServerReq_STR);
			SIPManager.notifyRelineon(false);
		}

		ServerUIUtil.stop();		
		
		return isPrompt;
	}

	public static boolean isServing() {
		return ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF;
	}

	public static void restartResponsorServerDelayMode() {
		DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null) {
			@Override
			public void doBiz() {
				restartResponsorServer();
			}
		});
	}

}
