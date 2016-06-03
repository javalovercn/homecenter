package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.RecycleThread;
import hc.core.util.ThreadPool;

public class ContextManager {
	private static IContext instanceContext;
	public static short cmStatus;

	public static IContext getContextInstance(){
		return ContextManager.instanceContext;
	}

	public static void exit(){
		if(ContextManager.instanceContext != null){
			ContextManager.setStatus(ContextManager.STATUS_EXIT);
		}
	}

	public static void setContextInstance(final IContext ib){
		if((ContextManager.instanceContext != null) && IConstant.serverSide){//手机端初次连接失败后，再次连接会重新调用本方法
			if(L.isInWorkshop){
				LogManager.err("Error : ContextInstance is setted");
			}
			return;
		}
		ContextManager.instanceContext = ib;
		ContextManager.setStatus(ContextManager.STATUS_NEED_NAT);
		if(ContextManager.isNotifyShutdown){
			ContextManager.notifyShutdown();
		}
	}
	static boolean isNotifyShutdown = false;
	public static void notifyShutdown(){
		ExceptionReporter.shutdown();
		
		//用户请求关闭时，可能存在其它消息任务。消息任务须优先被执行。故增加缺省线程优先级来处理关闭逻辑
		ContextManager.getThreadPool().run(new Runnable() {
			public void run(){
				if(ContextManager.instanceContext != null){
					ContextManager.instanceContext.notifyShutdown();
				}else{
					ContextManager.isNotifyShutdown = true;
				}
			}
		});
	}


	public static void displayMessage(final String caption, final String text, final int type, final int timeOut){
		if(ContextManager.instanceContext != null){
			ContextManager.instanceContext.displayMessage(caption, text, type, null, 0);
		}
	}

	public static void start() {
		ContextManager.instanceContext.run();
	}

	public static void shutDown(){
		if(ContextManager.instanceContext != null){
			ContextManager.instanceContext.shutDown();
		}
	}

	public static ReceiveServer getReceiveServer() {
		return ContextManager.instanceContext.getReceiveServer();
	}

	public static boolean isServerStatus(){
		final int[] servermode = {ContextManager.STATUS_SERVER_SELF};
		final int m = ContextManager.cmStatus;
		for (int i = 0; i < servermode.length; i++) {
			if(m == servermode[i]){
				return true;
			}
		}
		return false;
	}

	public static boolean isClientStatus(){
		final int mode = ContextManager.cmStatus;
		if(mode == ContextManager.STATUS_CLIENT_SELF){
			return true;
		}
		return false;
	}

	public static boolean isNotWorkingStatus(){
		final int[] notWorking = {ContextManager.STATUS_READY_TO_LINE_ON, ContextManager.STATUS_EXIT, ContextManager.STATUS_NEED_NAT, ContextManager.STATUS_LINEOFF};
		final int m = ContextManager.cmStatus;
		for (int i = 0; i < notWorking.length; i++) {
			if(m == notWorking[i]){
				return true;
			}
		}
		return false;
	}

	public static final short STATUS_LINEOFF = 0;
	public static final short STATUS_NEED_NAT = 1;//尚未NAT
	public static final short STATUS_READY_TO_LINE_ON = 2;//空闲状态，暂无客户服务
	public static final short STATUS_READY_MTU = 3;//打通，但是尚未进行MTU
	//	public static final short STATUS_READY_FOR_CLIENT = 4;//MTU后，但是尚未验证客户
	public static final short STATUS_SERVER_SELF = 5;
	public static final short STATUS_CLIENT_SELF = 6;
	public static final short STATUS_READY_EXIT = 7;
	public static final short STATUS_EXIT = 8;
	
	public static IStatusListen statusListen;

	public static final short MODE_CONNECTION_NONE = 0;
	public static final short MODE_CONNECTION_HOME_WIRELESS = 1;
	public static final short MODE_CONNECTION_PUBLIC_DIRECT = 2;
	public static final short MODE_CONNECTION_PUBLIC_UPNP_DIRECT = 3;
	public static final short MODE_CONNECTION_RELAY = 4;

	private static short modeStatus = ContextManager.MODE_CONNECTION_NONE;

	public static void setConnectionModeStatus(final short modeStat){
		ContextManager.modeStatus = modeStat;
	}

	public static short getConnectionModeStatus(){
		return ContextManager.modeStatus;
	}

	public static void setStatus(final short mode){
		if((mode != ContextManager.STATUS_EXIT) && (ContextManager.cmStatus == ContextManager.STATUS_EXIT)){
			L.V = L.O ? false : LogManager.log("forbid change status from [" + ContextManager.cmStatus + "] to [" + mode + "]");
			return;
		}

		hc.core.L.V=hc.core.L.O?false:LogManager.log("Change Status, From [" + ContextManager.cmStatus + "] to [" + mode + "]");
		if(ContextManager.statusListen != null){
			ContextManager.statusListen.notify(ContextManager.cmStatus, mode);
		}

		if(mode == ContextManager.STATUS_LINEOFF){
			ContextManager.modeStatus = ContextManager.MODE_CONNECTION_NONE;
		}

		if((ContextManager.cmStatus == mode) && (mode == ContextManager.STATUS_EXIT)){
			ContextManager.getThreadPool().run(new Runnable() {
				public void run() {
					try{
						Thread.sleep(5000);
					}catch (Throwable e) {
					}
					ContextManager.forceExit();
				}
			});
		}

		ContextManager.cmStatus = mode;

		if(mode == ContextManager.STATUS_EXIT){
			//没有置null的必要，而且在退出shutdown逻辑中，该值仍被使用，
			//参见J2MESendServer循环中break
			//			instanceContext = null;

			return;
		}

		if(mode == ContextManager.STATUS_READY_MTU){
			if(IConstant.serverSide){
				try{
					//服务器稍等，提供客户初始化时间
					Thread.sleep(200);
				}catch (final Exception e) {
				}
			}

			//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Do biz after Hole");

			//激活KeepAlive hctimer
			ContextManager.getContextInstance().doExtBiz(IContext.BIZ_AFTER_HOLE, null);

			//			if(IConstant.serverSide){
			//			}else{
			//			}
		}

		//		if(mode == ContextManager.STATUS_SERVER_SELF){
		//			ContextManager.getContextInstance().doExtBiz(IContext.BIZ_IS_ON_SERVICE);
		//		}

		//		if(mode == ContextManager.STATUS_READY_TO_LINE_ON){
		//			hc.core.L.V=hc.core.L.O?false:LogManager.log("NO biz for status READY_TO_LINE_ON");
		//		}

		//		if(ContextManager.isClientStatus()){
		//		}

		//		if(ContextManager.isClientStatus() && ContextManager.isNotWorkingStatus() == false){
		//			ScreenClientManager.init();
		//		}
	}

	public static void forceExit() {
		LogManager.exit();
		CCoreUtil.globalExit();
	}

	public static byte[] cloneDatagram(final byte[] randomBS){
		final byte[] event = new byte[MsgBuilder.UDP_BYTE_SIZE];

		System.arraycopy(randomBS, 0, event, 0, (event.length > randomBS.length)?randomBS.length:event.length);
		return event;
	}

	private static ThreadPool threadPool;
	private static Object securityToken;

	public static final void setThreadPool(final ThreadPool tp, final Object t){
		CCoreUtil.checkAccess();
		
		ContextManager.threadPool = tp;
		ContextManager.securityToken = t;
	}

	public static final Object getThreadPoolToken(){
		CCoreUtil.checkAccess();

		if(ContextManager.securityToken == null){
			throw new Error("Fail on initial ContextManager threadPoolToken.");
		}
		return ContextManager.securityToken;
	}

	public static final ThreadPool getThreadPool(){
		if(ContextManager.threadPool == null){
			ContextManager.threadPool = new ThreadPool(null){
				protected Thread buildThread(final RecycleThread rt) {
					return new Thread(rt);
				}

				protected void checkAccessPool(final Object token){
				}
			};
		}
		return ContextManager.threadPool;
	}

	/**
	 * 被ProjectContext调用，请勿checkAccess
	 * @return
	 */
	public static final boolean isMobileLogin() {
		return ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF;
	}
}
