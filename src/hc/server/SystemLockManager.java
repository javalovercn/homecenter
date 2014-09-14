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
		
		int lock_minutes = getConfigLockMinutes();
		SystemLockManager.LOCK_MS = lock_minutes * 60 * 1000;
	}

	public static int getConfigLockMinutes() {
		return (PropertiesManager.getValue(PropertiesManager.PWD_ERR_LOCK_MINUTES)==null)
				?3
				:Integer.parseInt(PropertiesManager.getValue(PropertiesManager.PWD_ERR_LOCK_MINUTES));
	}

	public static int getConfigErrTry() {
		return (PropertiesManager.getValue(PropertiesManager.PWD_ERR_TRY)==null)
				?10
				:Integer.parseInt(PropertiesManager.getValue(PropertiesManager.PWD_ERR_TRY));
	}


}
