package hc.core.util;

import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.RootConfig;

public class ExceptionReporter {
	private static boolean status = false;
	private static boolean isShutDown = false;
	private final static LinkedSet cache = new LinkedSet();
	private final static Object lock = new Object();
	
	private static Thread backThread = new Thread(){
		public void run(){
			IContext contextInstance = null;
			while(contextInstance == null && isShutDown == false){
				try{
					Thread.sleep(1000);
				}catch (Throwable e) {
				}
				contextInstance = ContextManager.getContextInstance();
			}

			ExceptionJSON json = null;
			
			while(isShutDown == false){
				synchronized (cache) {
					json = (ExceptionJSON)cache.getFirst();
					if(json == null){
						try {
							cache.wait();
						} catch (Throwable e) {
						}
						continue;
					}
				}
				contextInstance.doExtBiz(IContext.BIZ_REPORT_EXCEPTION, json);
			}
		}
	};
	
	private static boolean isStartThread = false;
	
	public static final void start(){
		start(true);
	}
	
	public static final void setHarHelper(final HarHelper helper){
		CCoreUtil.checkAccess();
		
		harHelper = helper;
	}

	public static final void start(final boolean withLog){
		CCoreUtil.checkAccess();
		
		synchronized (lock) {
			if(status){
				return;
			}
			if(isStartThread == false){
				backThread.start();
				isStartThread = true;
			}
			status = true;
		}
		
		if(withLog){
			appendToLog();
		}
	}

	public static void appendToLog() {
		CCoreUtil.checkAccess();
		
		if(status){
			L.V = L.O ? false : LogManager.log("started ExceptionReporter.");
		}else{
			L.V = L.O ? false : LogManager.log("stopped ExceptionReporter.");
		}
	}
	
	private static final ExceptionJSONBuilder builder = RootBuilder.getInstance().getExceptionJSONBuilder();
	private static final ExceptionChecker checker = ExceptionChecker.getInstance();
	
	public static final void printStackTrace(final Throwable throwable){
		printStackTrace(throwable, null, null, INVOKE_NORMAL);
	}
	
	private static final boolean SERVER_SIDE = IConstant.serverSide;
	private static final RootConfig rootConfig = RootConfig.getInstance();
	
	private static HarHelper harHelper;
	
	public static final int INVOKE_NORMAL = 0;
	public static final int INVOKE_HAR = 1;
	public static final int INVOKE_THREADPOOL = 2;
	
	public static final void printStackTraceFromThreadPool(final Throwable throwable){
		printStackTrace(throwable, null, null, INVOKE_THREADPOOL);
	}
	
	public static final void printStackTraceFromRunException(final Throwable throwable, final String script, final String errMessage){
		printStackTrace(throwable, script, errMessage, INVOKE_HAR);
	}
	
	public static final void printStackTrace(final Throwable throwable, final String script, final String errMessage, final int invokeFrom){
		throwable.printStackTrace();
		
		if(status == false){
			return;
		}
		
		boolean isEmail = false;
		String exceptionURL = null;
		
		final boolean blockExcetionReport = rootConfig.isTrue(RootConfig.p_blockExceptionReport);//不接收服务器或手机端的异常报告

		if(SERVER_SIDE == false){
			if(blockExcetionReport){
				return;
			}
			
			if(builder == null){//J2ME或没有
				return;
			}
			
			//手机端
			final ExceptionJSON json = builder.buildJSON(harHelper, checker, throwable, ExceptionJSON.HC_EXCEPTION_URL, script, errMessage);
			if(json == null){
				return;
			}
		
			synchronized (cache) {
				cache.addTail(json);
				cache.notify();
			}
			
			return;
		}else{
			exceptionURL = harHelper.getExceptionReportURL();//开发环境下可能得不到url
			if(exceptionURL != null){
				final int atIdx = exceptionURL.indexOf('@');
				final int maoHaoIdx = exceptionURL.indexOf(':');
				isEmail = exceptionURL!=null && atIdx > 0 && maoHaoIdx < 0;
			}
		}
		
//		if(isInvokedFromHAR){//排除不发送的情形
//			if(SERVER_SIDE && receiveHarException == false && exceptionURL == null){
//				return;
//			}
//		}
		
		if(isEmail || (blockExcetionReport == false 
								&& (
										(invokeFrom == INVOKE_NORMAL) 
										|| (invokeFrom == INVOKE_THREADPOOL && exceptionURL == null)
										))){
			//满足向服务器的条件。前者接收邮件；后者接收服务自身的
			final ExceptionJSON json = builder.buildJSON(harHelper, checker, throwable, ExceptionJSON.HC_EXCEPTION_URL, script, errMessage);
			
			if(json == null){
				return;
			}

			if(blockExcetionReport || (invokeFrom == INVOKE_THREADPOOL && exceptionURL != null)
				){//标记，HC不接收
				json.setReceiveExceptionForHC(false);
			}
			
			if(isEmail){
				json.setAttToEmail(exceptionURL);
			}
			
			synchronized (cache) {
				cache.addTail(json);
				cache.notify();
			}
		}
		
		if(exceptionURL != null && isEmail == false){
			//满足向Har provider的条件
			final ExceptionJSON json = builder.buildJSON(harHelper, checker, throwable, exceptionURL, script, errMessage);
			if(json == null){
				return;
			}
			
			synchronized (cache) {
				cache.addTail(json);
				cache.notify();
			}
		}
	}
	
	public static final void shutdown(){
		CCoreUtil.checkAccess();
		
		isShutDown = true;
		synchronized (cache) {
			cache.notify();
		}
	}
	
	public static final void stop(){
		CCoreUtil.checkAccess();
		
		synchronized (lock) {
			if(status == false){
				return;
			}
			status = false;
		}
		
		appendToLog();
	}
}
