package hc.core.util;

public class LogManager {
//	public static void logInTest(String msg){
//		System.out.println(msg);
//	}
	
	private static ILog log = null;
	
	public static void setLog(ILog ilog){
		CCoreUtil.checkAccess();
		log = ilog;
	}
	
	public static ILog getLogger(){
		return log;
	}
	
	public static void flush(){
		if(log != null){
			log.flush();
		}
	}
	
	public static synchronized void exit(){
		if(log != null){
			log.exit();
			log = null;
		}
	}
	
	public static boolean log(String msg){
		if(log != null){
			log.log(msg);
		}else{
			System.out.println(msg);
		}
		return false;
	}
	
	public static void debug(String msg){
		log(msg);
	}
	
	public static void debug(String msg, final Throwable e){
		log(msg);
	}
	
	public static void debug(String msg, final String str1, final String str2){
		log(msg);
	}
	
	public static void info(String msg, Throwable exception){
		log(msg);
	}
	
	public static void info(String msg, String str1){
		log(msg);
	}
	
	public static void warn(String msg, Throwable exception){
		warning(msg);
	}
	
	public static void warn(String msg){
		warning(msg);
	}
	
	/**
	 * warning to log only.
	 * @param msg
	 * @return
	 */
	public static boolean warning(String msg){
		if(log != null){
			log.warning(msg);
		}else{
			System.out.println(ILog.WARNING + msg);
		}
		return false;
	}
	
	/**
	 * 通知出错，要进行UI提示
	 * @param msg
	 */
	public static void err(String msg){
		if(log != null){
			log.errWithTip(msg);
		}else{
			System.err.println(msg);
		}
	}
	
	public static void error(String msg, final Throwable e){
		err(msg);
	}
	
	public static void error(String msg){
		err(msg);
	}
	
	public static boolean errForShop(String msg){
		if(log != null){
			log.err(msg);
		}else{
			System.err.println(msg);
		}
		return false;
	}
	
	public static void errToLog(String msg){
		if(log != null){
			log.err(msg);
		}else{
			System.err.println(msg);
		}
	}
	
	public static void info(String msg){
		if(log != null){
			log.info(msg);
			log.log(msg);
		}else{
			System.err.println(msg);
		}
	}

	public static boolean INI_DEBUG_ON = false;
}
