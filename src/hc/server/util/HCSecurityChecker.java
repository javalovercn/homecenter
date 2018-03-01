package hc.server.util;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.core.util.SecurityChecker;
import hc.server.HCSecurityException;

import java.util.ArrayList;

public class HCSecurityChecker implements SecurityChecker {
	final Thread eventDispatchThread = HCLimitSecurityManager.getEventDispatchThread();
	final HCEventQueue hcEventQueue = HCLimitSecurityManager.getHCEventQueue();
	final ThreadGroup threadPoolToken = App.getRootThreadGroup();
	final ArrayList<Thread> allowedThread = new ArrayList<Thread>(128);

	@Override
	public final void check(final Object token) {
		ThreadGroup g = null;
		Thread currentThread = null;
		boolean isEventDispatchThread = false;
		if (token != null && token instanceof ThreadGroup) {
			g = (ThreadGroup) token;
		} else {
			currentThread = Thread.currentThread();

			if (allowedThread.contains(currentThread)) {// 多线程下安全
				return;
			}

			isEventDispatchThread = (currentThread == eventDispatchThread);
			if (isEventDispatchThread) {
				final ContextSecurityConfig csc = hcEventQueue.currentConfig;
				if (csc != null) {
					g = csc.threadGroup;
				} else {
					g = threadPoolToken;
				}
			} else {
				g = currentThread.getThreadGroup();
			}
		}

		if (ContextSecurityManager.getConfig(g) != null) {
			throw new HCSecurityException(
					HCLimitSecurityManager.HC_FAIL_TO_ACCESS_HOME_CENTER_NON_PUBLIC_API);
		}

		if (token == null && isEventDispatchThread == false) {
			synchronized (this) {
				allowedThread.add(currentThread);
			}
		}
	}

	@Override
	public final void resetFastCheckThreads() {
		CCoreUtil.checkAccess();

		synchronized (this) {
			allowedThread.clear();
		}
	}
}
