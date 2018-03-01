package hc.server;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.core.util.RecycleThread;
import hc.core.util.ThreadPool;
import hc.core.util.ThreadPriorityManager;
import hc.util.ClassUtil;

public class AppThreadPool extends ThreadPool {
	public AppThreadPool() {
		super(App.getRootThreadGroup(), true);
	}

	@Override
	public Thread buildThread(final RecycleThread rt) {
		final Thread t = new Thread((ThreadGroup) threadGroup, rt);
		t.setPriority(ThreadPriorityManager.SERVER_THREADPOOL_PRIORITY);
		return t;
	}

	@Override
	protected void checkAccessPool(final Object token) {
		CCoreUtil.checkAccess(token);
	}

	@Override
	public void printStack() {
		ClassUtil.printCurrentThreadStack("--------------nest stack--------------");
	}
}