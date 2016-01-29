package hc.core;

import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.RecycleThread;
import hc.core.util.ThreadPool;

public class ContextManager {
	private static IContext instanceContext;
	public static short cmStatus;
	
	public static IContext getContextInstance(){
		return instanceContext;
	}
	
    public static void exit(){
    	if(instanceContext != null){
	    	setStatus(STATUS_EXIT);
    	}
    }
    
    public static void setContextInstance(final IContext ib){
    	if(instanceContext != null && IConstant.serverSide){//手机端初次连接失败后，再次连接会重新调用本方法
    		if(L.isInWorkshop){
    			LogManager.err("Error : ContextInstance is setted");
    		}
    		return;
    	}
    	instanceContext = ib;
    	setStatus(STATUS_NEED_NAT);
    	if(isNotifyShutdown){
    		notifyShutdown();
    	}
    }
    static boolean isNotifyShutdown = false;
    public static void notifyShutdown(){
    	//用户请求关闭时，可能存在其它消息任务。消息任务须优先被执行。故增加缺省线程优先级来处理关闭逻辑
    	ContextManager.getThreadPool().run(new Runnable() {
    		public void run(){
    	    	if(instanceContext != null){
    	    		instanceContext.notifyShutdown();
    	    	}else{
    	    		isNotifyShutdown = true;
    	    	}
    		}
    	});
    }

    
    public static void displayMessage(final String caption, final String text, final int type, final int timeOut){
    	if(instanceContext != null){
    		instanceContext.displayMessage(caption, text, type, null, 0);
    	}
    }
		
	public static void start() {
		instanceContext.run();	
	}

	public static void shutDown(){
		if(instanceContext != null){
			instanceContext.shutDown();	
		}
	}
	
	public static ReceiveServer getReceiveServer() {
		return instanceContext.getReceiveServer();
	}
	
	public static boolean isServerStatus(){
		final int[] servermode = {STATUS_SERVER_SELF};
		final int m = cmStatus;
		for (int i = 0; i < servermode.length; i++) {
			if(m == servermode[i]){
				return true;
			}
		}
		return false;
	}
	
	public static boolean isClientStatus(){
		final int mode = cmStatus;
		if(mode == STATUS_CLIENT_SELF){
			return true;
		}
		return false;
	}
	
	public static boolean isNotWorkingStatus(){
		final int[] notWorking = {STATUS_READY_TO_LINE_ON, STATUS_EXIT, STATUS_NEED_NAT, STATUS_LINEOFF};
		final int m = cmStatus;
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
	public static final short STATUS_EXIT = 7;

	public static IStatusListen statusListen;
	
	public static final short MODE_CONNECTION_NONE = 0;
	public static final short MODE_CONNECTION_HOME_WIRELESS = 1;
	public static final short MODE_CONNECTION_PUBLIC_DIRECT = 2;
	public static final short MODE_CONNECTION_PUBLIC_UPNP_DIRECT = 3;
	public static final short MODE_CONNECTION_RELAY = 4;
	
	private static short modeStatus = MODE_CONNECTION_NONE;
	
	public static void setConnectionModeStatus(final short modeStat){
		modeStatus = modeStat;
	}
	
	public static short getConnectionModeStatus(){
		return modeStatus;
	}
	
	public static void setStatus(final short mode){
		if(mode != ContextManager.STATUS_EXIT && cmStatus == ContextManager.STATUS_EXIT){
			L.V = L.O ? false : LogManager.log("forbid change status from [" + cmStatus + "] to [" + mode + "]");
			return;
		}
		
		hc.core.L.V=hc.core.L.O?false:LogManager.log("Change Status, From [" + cmStatus + "] to [" + mode + "]");
		if(statusListen != null){
			statusListen.notify(cmStatus, mode);
		}
		
		if(mode == ContextManager.STATUS_LINEOFF){
			modeStatus = MODE_CONNECTION_NONE;
		}
		
		if(cmStatus == mode && mode == ContextManager.STATUS_EXIT){
			forceExit();
		}
		
		cmStatus = mode;
		
		if(mode == STATUS_EXIT){
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
		if(threadPool == null){
			threadPool = new ThreadPool(null){
				protected Thread buildThread(final RecycleThread rt) {
					return new Thread(rt);
				}
				
				protected void checkAccessPool(final Object token){
				}
			};
		}
		return threadPool;
	}

	public static final boolean isMobileLogin() {
		return cmStatus == STATUS_SERVER_SELF;
	}
}
