package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.RecycleThread;
import hc.core.util.ThreadPool;

public class ContextManager {
	public static void exit(final IContext ic){
		if(ic != null){
			ic.setStatus(ContextManager.STATUS_EXIT);
		}
	}

	public static void setContextInstance(final IContext ic){
//		if((ContextManager.instanceContext != null) && IConstant.serverSide){//手机端初次连接失败后，再次连接会重新调用本方法
//			if(L.isInWorkshop){
//				LogManager.err("Error : ContextInstance is setted");
//			}
//			return;
//		}
//		ContextManager.instanceContext = ib;
		ic.setStatus(ContextManager.STATUS_NEED_NAT);
		if(CoreSession.isNotifyShutdown()){
			ContextManager.notifyShutdown(ic);
		}
	}
	
	/**
	 * 
	 * @param ic 有可能为null
	 */
	public static void notifyShutdown(final IContext ic){
//		ExceptionReporter.shutdown();
	
		//用户请求关闭时，可能存在其它消息任务。消息任务须优先被执行。故增加缺省线程优先级来处理关闭逻辑
		ContextManager.getThreadPool().run(new Runnable() {
			public void run(){
				try{
					if(ic != null){
						ic.notifyShutdown();
					}else{
						if(L.isInWorkshop){
							LogManager.errToLog("notifyShutdown IContext is null");
						}
						CoreSession.setNotifyShutdown();
					}
				}catch (Exception e) {
					e.printStackTrace();//不产生通知到服务器上
				}
			}
		});
	}


//	public static void displayMessage(final String caption, final String text, final int type, final int timeOut){
//		final CoreSocketSession coreSS = SessionManager.getPreparedSocketSession();
//		if(coreSS != null && coreSS.context != null){
//			coreSS.context.displayMessage(caption, text, type, null, 0);
//		}
//	}

//	public static void start() {
//		ContextManager.instanceContext.run();
//	}

	public static void shutDown(final CoreSession coreSS){
		if(coreSS != null && coreSS.context != null){
			coreSS.context.shutDown(coreSS);
		}
	}

	public static boolean isServerStatus(final IContext ic){
		final int[] servermode = {ContextManager.STATUS_SERVER_SELF};
		final int m = ic.cmStatus;
		for (int i = 0; i < servermode.length; i++) {
			if(m == servermode[i]){
				return true;
			}
		}
		return false;
	}

	public static boolean isClientStatus(final IContext ic){
		if(ic != null && ic.cmStatus == ContextManager.STATUS_CLIENT_SELF){
			return true;
		}
		return false;
	}

	public static boolean isNotWorkingStatus(final IContext ic){
		final int[] notWorking = {ContextManager.STATUS_READY_TO_LINE_ON, ContextManager.STATUS_EXIT, ContextManager.STATUS_NEED_NAT, ContextManager.STATUS_LINEOFF};
		final int m = ic.cmStatus;
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
	
	public static final short MODE_CONNECTION_NONE = 0;
	public static final short MODE_CONNECTION_HOME_WIRELESS = 1;
	public static final short MODE_CONNECTION_PUBLIC_DIRECT = 2;
	public static final short MODE_CONNECTION_PUBLIC_UPNP_DIRECT = 3;
	public static final short MODE_CONNECTION_RELAY = 4;

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
		
		threadPool = tp;
		securityToken = t;
	}

	public static final Object getThreadPoolToken(){
		CCoreUtil.checkAccess();

		if(securityToken == null){
			throw new Error("Fail on initial ContextManager threadPoolToken.");
		}
		return securityToken;
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
	public static final boolean isMobileLogin(final IContext ic) {
		return ic.cmStatus == ContextManager.STATUS_SERVER_SELF;
	}
}
