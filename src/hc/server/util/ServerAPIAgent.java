package hc.server.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.util.ReturnableRunnable;

public class ServerAPIAgent {
	public static void init(){
	}
	
	final static Object threadToken = App.getThreadPoolToken();
	
	static final Object runAndWaitInSysThread(final ReturnableRunnable returnRun){
		return ContextManager.getThreadPool().runAndWait(returnRun, threadToken);
	}


	static final void runInSysThread(final Runnable run){
		ContextManager.getThreadPool().run(run, threadToken);
	}
}
