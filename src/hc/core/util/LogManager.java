package hc.core.util;

import java.util.Calendar;
import java.util.Vector;

public class LogManager {
	// public static void logInTest(String msg){
	// System.out.println(msg);
	// }
	static final Vector beforeInitLog = new Vector(10);

	public static void addBeforeInitLog(final LogMessage msg) {
		beforeInitLog.addElement(msg);
		System.out.println(msg.msg);
	}

	private static ILog log = null;

	public static void setLog(ILog ilog) {
		CCoreUtil.checkAccess();
		log = ilog;

		final int size = beforeInitLog.size();
		for (int i = 0; i < size; i++) {
			final LogMessage lm = (LogMessage) beforeInitLog.elementAt(i);
			if (lm.isError) {
				errToLog(lm.msg);
			} else {
				log(lm.msg);
			}
		}

		beforeInitLog.removeAllElements();
	}

	public static ILog getLogger() {
		return log;
	}

	public static void flush() {
		if (log != null) {
			log.flush();
		}
	}

	public static synchronized void exit() {
		if (log != null) {
			log.exit();
			log = null;
		}
	}

	public static boolean log(String msg) {
		if (log != null) {
			log.log(msg);
		} else {
			System.out.println(addTime(msg));
		}
		return false;
	}

	public static String addTime(String msg) {
		return addTime(msg, false);
	}
	
	public static String addTime(String msg, final boolean addNewLine) {
		final StringBuffer sb = StringBufferCacher.getFree();

		Calendar calendar = Calendar.getInstance();

		sb.append(calendar.get(Calendar.YEAR));
		sb.append("-");
		final int month = calendar.get(Calendar.MONTH) + 1;
		if (month < 10) {
			sb.append('0');
		}
		sb.append(month);
		sb.append("-");
		final int day = calendar.get(Calendar.DAY_OF_MONTH);
		if (day < 10) {
			sb.append('0');
		}
		sb.append(day);
		sb.append(" ");
		final int hour = calendar.get(Calendar.HOUR_OF_DAY);
		if (hour < 10) {
			sb.append('0');
		}
		sb.append(hour);
		sb.append(":");
		final int minute = calendar.get(Calendar.MINUTE);
		if (minute < 10) {
			sb.append('0');
		}
		sb.append(minute);
		sb.append(":");
		final int second = calendar.get(Calendar.SECOND);
		if (second < 10) {
			sb.append('0');
		}
		sb.append(second);
		sb.append(".");
		final int ms = calendar.get(Calendar.MILLISECOND);
		if (ms < 10) {
			sb.append('0');
			sb.append('0');
		} else if (ms < 100) {
			sb.append('0');
		}
		sb.append(ms);
		sb.append(" ");

		sb.append(msg);
		
		if(addNewLine) {
			sb.append('\n');
		}

		final String pMsg = sb.toString();
		StringBufferCacher.cycle(sb);

		return pMsg;
	}

	public static void debug(String msg) {
		log(msg);
	}

	public static void debug(String msg, final Throwable e) {
		log(msg);
	}

	public static void debug(String msg, final String str1, final String str2) {
		log(msg);
	}

	public static void info(String msg, Throwable exception) {
		log(msg);
	}

	public static void info(String msg, String str1) {
		log(msg);
	}

	public static void warn(String msg, Throwable exception) {
		warning(msg);
	}

	public static void warn(String msg) {
		warning(msg);
	}

	/**
	 * warning to log only.
	 * 
	 * @param msg
	 * @return
	 */
	public static boolean warning(String msg) {
		if (log != null) {
			log.warning(msg);
		} else {
			System.out.println(addTime(ILog.WARNING + msg));
		}
		return false;
	}

	/**
	 * 通知出错，要进行UI提示，支持用户级线程。
	 * 
	 * @param msg
	 */
	public static void err(String msg) {
		if (log != null) {
			log.errWithTip(msg);
		} else {
			System.err.println(addTime(msg));
		}
	}

	public static void error(String msg, final Throwable e) {
		err(msg);
	}

	public static void error(String msg) {
		err(msg);
	}

	public static boolean errForShop(String msg) {
		if (log != null) {
			log.err(msg);
		} else {
			System.err.println(addTime(msg));
		}
		return false;
	}

	public static void errToLog(String msg) {
		if (log != null) {
			log.err(msg);
		} else {
			System.err.println(addTime(msg));
		}
	}

	public static void info(String msg) {
		if (log != null) {
			log.info(msg);
			log.log(msg);
		} else {
			System.err.println(addTime(msg));
		}
	}

	public static boolean INI_DEBUG_ON = false;
}
