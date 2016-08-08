package hc.server.j2se;

import hc.core.util.ExceptionReporter;
import hc.core.util.IMsgNotifier;
import hc.util.ClassUtil;

public class CapHelper {
	public static final String CapManager_CLASS = "hc.video.CapManager";
	public static final String CapNotify_CLASS = "hc.video.CapNotify";

	public final static Object capNotify = buildCapNotifyInstance();
	
	static {
		try{
			CapHelper.addListener(CapHelper.capNotify);
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	public static final void addListener(final Object cn){
		try{
			final Class[] paraTypes = {IMsgNotifier.class};
			final Object[] para = {cn};
			ClassUtil.invoke(getCapManagerClass(), getCapManagerClass(), "addListener", paraTypes, para, true);
//		CapManager.addListener(capNotify);
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	public static final Object buildCapNotifyInstance(){
		try{
			final Class c = Class.forName(CapNotify_CLASS);
			return c.newInstance();
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}
	
	public static final Class getCapManagerClass(){
		try {
			return Class.forName(CapManager_CLASS);
		} catch (final ClassNotFoundException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}
}
