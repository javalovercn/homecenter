package hc.server.util;

public class IDEUtil {
	
	/**
	 * tell IDE to treat the return object as an instance of class <code>claz</code>.
	 * @param object
	 * @param claz
	 * @return assigned by <code>obj</code>,
	 * @since 7.52
	 */
	public static Object asClass(final Object object, final Object claz){
		return object;
	}
	
	/**
	 * build Runnable instance.
	 * <BR><BR>
	 * it is not required to define run method in body. 
	 * <BR>
	 * it is the best way to write Runnable instance in JRuby scripts.
	 * @param run
	 * @return
	 */
	public static Runnable buildRunnable(final Runnable run){
		return run;
	}
}
