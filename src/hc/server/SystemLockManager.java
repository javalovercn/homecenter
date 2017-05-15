package hc.server;

import hc.App;
import hc.core.CoreSession;
import hc.core.IContext;
import hc.core.MsgBuilder;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.server.ui.SingleMessageNotify;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

public class SystemLockManager {
	private static int MAXTIMES;
	private static long LOCK_MS;
	
	private static short pwdErrTry;
	private static long lastErrMS;

	public static void resetErrInfo() {
		ResourceUtil.checkHCStackTrace();
		
		pwdErrTry = 0;
		lastErrMS = 0;
	}
	
	/**
	 * true means close session.
	 * @param coreSS
	 * @return
	 */
	public static boolean addOneConnBuildTry(final CoreSession coreSS){
		CCoreUtil.checkAccess();
		synchronized (SystemLockManager.class) {
			++pwdErrTry;
		}
		LogManager.errToLog("stop evil connection");
		setLastErrMS();
		return checkErrorCount(coreSS);
	}
	
	/**
	 * true means close session.
	 * @return
	 */
	public static boolean checkErrorCount(final CoreSession coreSS){
		if(pwdErrTry < MAXTIMES){
		}else{
			if(System.currentTimeMillis() - SystemLockManager.getLastErrMS() < LOCK_MS){
				SingleMessageNotify.showOnce(SingleMessageNotify.TYPE_LOCK_CERT, 
						"system is locking now!!!<BR><BR>error password or certification more than "+MAXTIMES+" times.", 
						"Lock System Now!!", 1000 * 60 * 1, App.getSysIcon(App.SYS_WARN_ICON));
				LogManager.errToLog("error password or certification more than "+MAXTIMES+" times.");
				coreSS.context.send(MsgBuilder.E_AFTER_CERT_STATUS, String.valueOf(IContext.BIZ_SERVER_AFTER_UNKNOW_STATUS));
				J2SEContext.sleepAfterError();
				coreSS.notifyLineOff(true, false);
				
				return true;
			}else{
				resetErrInfo();
			}
		}
		return false;
	}
	
	public static void addOnePwdErrTry(){
		CCoreUtil.checkAccess();
		final short count;
		synchronized (SystemLockManager.class) {
			count = ++pwdErrTry;
		}
		LogManager.log("Error Pwd OR Certifcation try "+ count +" time(s)");
		setLastErrMS();
	}
	
	public static long getLastErrMS(){
		return lastErrMS;
	}
	
	private static void setLastErrMS(){
		lastErrMS = System.currentTimeMillis();
	}

	static {
		updateMaxLock();
	}
	
	public static void updateMaxLock(){
		MAXTIMES = getConfigErrTry();
		
		final int lock_minutes = getConfigLockMinutes();
		LOCK_MS = lock_minutes * 60 * 1000;
	}

	public static int getConfigLockMinutes() {
		final String lockMin = PropertiesManager.getValue(PropertiesManager.PWD_ERR_LOCK_MINUTES);
		
		return (lockMin==null)
				?DefaultManager.LOCK_MINUTES
				:Integer.parseInt(lockMin);
	}

	public static int getConfigErrTry() {
		return (PropertiesManager.getValue(PropertiesManager.PWD_ERR_TRY)==null)
				?DefaultManager.ERR_TRY_TIMES
				:Integer.parseInt(PropertiesManager.getValue(PropertiesManager.PWD_ERR_TRY));
	}


}
