package hc.core.util;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.LogManager;

public class ExceptionReporter {
	private static boolean status = false;
	private static boolean isShutDown = false;
	private final static LinkedSet cache = new LinkedSet();
	private final static Object lock = new Object();
	public static final String THROW_FROM = "Throw From : ";
	
	private static final Thread backThread = new Thread(){
		public void run(){
			final RootBuilder rootBuilder = RootBuilder.getInstance();

			ExceptionJSON json = null;
			
			while(true){
				synchronized (cache) {
					if(isShutDown){
						break;
					}
					json = (ExceptionJSON)cache.getFirst();
					if(json == null){
						try {
							cache.wait();
						} catch (Throwable e) {
						}
						continue;
					}
				}
				rootBuilder.reportException(json);
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

	private static ExceptionJSONBuilder builder;
	private static ExceptionChecker checker;
	private static boolean SERVER_SIDE;

	public static final void start(final boolean withLog){
		CCoreUtil.checkAccess();
		
		synchronized (lock) {
			if(status){
				return;
			}
			if(isStartThread == false){
				
				//初始化
				builder = RootBuilder.getInstance().getExceptionJSONBuilder();
				checker = ExceptionChecker.getInstance();
				SERVER_SIDE = IConstant.serverSide;
				
				RootBuilder.getInstance().setDaemonThread(backThread);
				backThread.start();
				isStartThread = true;
			}
			status = true;
		}
		
		if(withLog){
			printReporterStatusToLog();
		}
	}

	public static void printReporterStatusToLog() {
		CCoreUtil.checkAccess();
		
		if(status){
			LogManager.log("started ExceptionReporter.");
		}else{
			LogManager.log("stopped ExceptionReporter.");
		}
	}
	
	public static final void printStackTrace(final Throwable throwable){
		printStackTrace(throwable, null, null, INVOKE_NORMAL);
	}
	
	private static HarHelper harHelper;
	
	public static final int INVOKE_NORMAL = 0;
	public static final int INVOKE_HAR = 1;
	public static final int INVOKE_THREADPOOL = 2;
	
	public static final void printStackTraceFromThreadPool(final Throwable throwable){
		printStackTrace(throwable, null, null, INVOKE_THREADPOOL);
	}
	
	public static final void printStackTraceFromHAR(final Throwable throwable, final String script, final String errMessage){
		printStackTrace(throwable, script, errMessage, INVOKE_HAR);
	}
	
	public static boolean isCauseByLineOffSession(final Throwable throwable) {
		Throwable t = throwable;
		while(t != null) {
			if(t instanceof SessionLineOffError) {
				return true;
			}
			t = (Throwable)RootBuilder.getInstance().doBiz(RootBuilder.ROOT_GET_CAUSE_ERROR, t);
		}
		return false;
	}
	
	/**
	 * 
	 * @param throwable
	 * @param script
	 * @param errMessage
	 * @param invokeFrom one of {@link #INVOKE_NORMAL}, {@link #INVOKE_HAR}, {@link #INVOKE_THREADPOOL}
	 */
	public static final void printStackTrace(final Throwable throwable, final String script, final String errMessage, final int invokeFrom){
		if(isCauseByLineOffSession(throwable)){
			return;
		}
		
		if(L.isInWorkshop){
			RootBuilder.getInstance().doBiz(RootBuilder.ROOT_PRINT_STACK_WITH_FULL_CAUSE, throwable);//printStackTrace or cause by without more
		}else{
			throwable.printStackTrace();
		}
		
		if(status == false){
			return;
		}
		
		boolean isEmail = false;
		String exceptionURLOrEmail = null;
		
		if(SERVER_SIDE == false){
			
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
			exceptionURLOrEmail = harHelper.getExceptionReportURL();//开发环境下可能得不到url
			if(exceptionURLOrEmail != null){
				if(HarHelper.NO_REPORT_URL_IN_HAR == exceptionURLOrEmail){
					return;
				}
				
				final int atIdx = exceptionURLOrEmail.indexOf('@');
				final int maoHaoIdx = exceptionURLOrEmail.indexOf(':');
				isEmail = exceptionURLOrEmail!=null && atIdx > 0 && maoHaoIdx < 0;
			}
		}
		
//		if(isInvokedFromHAR){//排除不发送的情形
//			if(SERVER_SIDE && receiveHarException == false && exceptionURL == null){
//				return;
//			}
//		}
		
		if(isEmail || ((invokeFrom == INVOKE_NORMAL) || (invokeFrom == INVOKE_THREADPOOL && exceptionURLOrEmail == null)	)){
			//满足向服务器的条件。前者接收邮件；后者接收服务自身的
			final ExceptionJSON json = builder.buildJSON(harHelper, checker, throwable, ExceptionJSON.HC_EXCEPTION_URL, script, errMessage);
			
			if(json == null){
				return;
			}

			if(exceptionURLOrEmail != null){//标记，HC不接收
				json.setReceiveExceptionForHC(false);
			}
			
			if(isEmail){
				json.setAttToEmail(exceptionURLOrEmail);
			}
			
			synchronized (cache) {
				cache.addTail(json);
				cache.notify();
			}
		}
		
		if(exceptionURLOrEmail != null && isEmail == false){
			//满足向Har provider的条件
			final ExceptionJSON json = builder.buildJSON(harHelper, checker, throwable, exceptionURLOrEmail, script, errMessage);
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
		
		printReporterStatusToLog();
	}
}
