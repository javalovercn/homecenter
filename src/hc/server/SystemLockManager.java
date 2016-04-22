package hc.server;

import hc.util.PropertiesManager;

public class SystemLockManager {

	public static int MAXTIMES;
	public static long LOCK_MS;

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
