package hc.server.util;

import hc.core.util.CCoreUtil;
import hc.util.ResourceUtil;

public class ContextSecurityManager {
	private static int length;
	private static ThreadGroup[] group = new ThreadGroup[16];// 增加一个tempLimitPool
	private static ContextSecurityConfig[] config = new ContextSecurityConfig[group.length];

	public static synchronized final void putContextSecurityConfig(final ThreadGroup threadGroup, final ContextSecurityConfig csc) {
		CCoreUtil.checkAccess();

		for (int i = 0; i < length; i++) {
			if (group[i] == threadGroup) {
				config[i] = csc;
				return;
			}
		}

		if (length == group.length - 1) {
			group = (ThreadGroup[]) ResourceUtil.moveToDoubleArraySize(group);
			config = (ContextSecurityConfig[]) ResourceUtil.moveToDoubleArraySize(config);
		}

		group[length] = threadGroup;
		config[length++] = csc;
	}

	public static final ContextSecurityConfig getConfig(final ThreadGroup startGroup) {
		for (int i = 0; i < length; i++) {
			// 即使Android环境下，putContextSecurityConfig正在扩容时，调用本方法，也不会产生异常
			if (group[i].parentOf(startGroup)) {
				return config[i];
			}
		}
		return null;
	}

}
