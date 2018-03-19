package hc.server.util;

public class IDEUtil {
	IDEUtil() {
	}

	/**
	 * tell IDE to treat the return object as an instance of class <code>claz</code>.
	 * 
	 * @param obj
	 *            the object will be returned.
	 * @param claz
	 *            the class will be treated as.
	 * @return the <code>obj</code>.
	 * @since 7.52
	 */
	public static Object asClass(final Object obj, final Object claz) {
		return obj;
	}

	/**
	 * build Runnable instance. <BR>
	 * <BR>
	 * it is not required to define run method in body. <BR>
	 * it is the best way to write Runnable instance in JRuby scripts in IDE.
	 * 
	 * @param run
	 * @return
	 */
	public static Runnable buildRunnable(final Runnable run) {
		return run;
	}
}
