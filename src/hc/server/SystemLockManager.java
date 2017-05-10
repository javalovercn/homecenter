package hc.server;

import hc.core.util.CCoreUtil;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

public class SystemLockManager {

	public static int MAXTIMES;
	public static long LOCK_MS;
	
	private static short pwdErrTry;
	private static long lastErrMS;

	public static void resetErrInfo() {
		ResourceUtil.checkHCStackTrace();
		
		pwdErrTry = 0;
		lastErrMS = 0;
	}
	
	public static short getPwdErrTry() {
		return pwdErrTry;
	}
	
	public static short addOnePwdErrTry(){
		CCoreUtil.checkAccess();
		return ++pwdErrTry;
	}
	
	public static long getLastErrMS(){
		return lastErrMS;
	}
	
	public static void setLastErrMS(){
		CCoreUtil.checkAccess();
		lastErrMS = System.currentTimeMillis();
	}

	static {
		updateMaxLock();
	}
	
	public static void updateMaxLock(){
		SystemLockManager.MAXTIMES = getConfigErrTry();
		
		final int lock_minutes = getConfigLockMinutes();
		SystemLockManager.LOCK_MS = lock_minutes * 60 * 1000;
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
