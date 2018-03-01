package hc.server.j2se;

import hc.core.util.ExceptionReporter;
import hc.core.util.IMsgNotifier;
import hc.util.ClassUtil;
import hc.util.ResourceUtil;

public class CapHelper {
	public static final String CapManager_CLASS = "hc.video.CapManager";
	public static final String CapNotify_CLASS = "hc.video.CapNotify";

	public final static Object capNotify = buildCapNotifyInstance();

	static {
		try {
			CapHelper.addListener(CapHelper.capNotify);
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static final void addListener(final Object cn) {
		try {
			final Class[] paraTypes = { IMsgNotifier.class };
			final Object[] para = { cn };
			ClassUtil.invoke(getCapManagerClass(), getCapManagerClass(), "addListener", paraTypes,
					para, true);
			// CapManager.addListener(capNotify);
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static final Object buildCapNotifyInstance() {
		final Class c = ResourceUtil.loadClass(CapNotify_CLASS, true);// Class.forName
		if (c != null) {
			try {
				return c.newInstance();
			} catch (final Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static final Class getCapManagerClass() {
		return ResourceUtil.loadClass(CapManager_CLASS, true);// Class.forName
	}
}
