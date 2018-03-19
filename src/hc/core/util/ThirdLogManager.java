package hc.core.util;

public class ThirdLogManager {

	public static boolean log(String msg) {
		return LogManager.log(msg);
	}

	public static boolean debug(String msg) {
		return log(msg);
	}

	public static boolean debug(String msg, final Throwable e) {
		if (e != null) {
			e.printStackTrace();
		}
		return log(msg);
	}

	public static boolean debug(String msg, final String str1,
			final String str2) {
		return log(msg);
	}

	public static boolean info(String msg, Throwable e) {
		if (e != null) {
			e.printStackTrace();
		}
		return log(msg);
	}

	public static boolean info(String msg, String str1) {
		return log(msg);
	}

	public static boolean warn(String msg, Throwable e) {
		if (e != null) {
			e.printStackTrace();
		}
		return warning(msg);
	}

	public static boolean warn(String msg) {
		return warning(msg);
	}

	/**
	 * warning to log only.
	 * 
	 * @param msg
	 * @return
	 */
	public static boolean warning(String msg) {
		return LogManager.warning(msg);
	}

	/**
	 * 通知出错，要进行UI提示
	 * 
	 * @param msg
	 */
	public static boolean err(String msg) {
		LogManager.err(msg);
		return false;
	}

	public static boolean error(String msg, final Throwable e) {
		if (e != null) {
			e.printStackTrace();
		}
		return err(msg);
	}

	public static boolean error(String msg) {
		return err(msg);
	}

	public static boolean errForShop(String msg) {
		return LogManager.errForShop(msg);
	}

	public static void errToLog(String msg) {
		LogManager.errToLog(msg);
	}

	public static boolean info(String msg) {
		LogManager.info(msg);
		return false;
	}

}
