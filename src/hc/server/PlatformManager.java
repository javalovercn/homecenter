package hc.server;

import hc.core.util.ExceptionReporter;
import hc.util.ResourceUtil;

public class PlatformManager {
	final static PlatformService service = buildService();

	private static PlatformService buildService() {
		final String serviceClassName;
		if (ResourceUtil.isAndroidServerPlatform()) {
			serviceClassName = "hc.android.AndroidPlatformService";
		} else {
			serviceClassName = "hc.server.j2se.J2SEPlatformService";// 该类名又被引用HCLimitSecurityManager checkPermission
		}

		try {
			final Class c = PlatformManager.class.getClassLoader().loadClass(serviceClassName);
			return (PlatformService) c.newInstance();
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	public static PlatformService getService() {
		// 注意：
		// 由于被ResourceUtil.resizeImage所限，不能进行checkAccess
		// 如果涉及安全检查，请分散到方法内
		return service;
	}
}
