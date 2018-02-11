package hc.server.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.HCTimer;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;

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
	
	public static final VoiceCommand buildVoiceCommand(final String text, final J2SESession j2seCoreSS, final ProjResponser pr){
		return new VoiceCommand(text, j2seCoreSS, pr);
	}
	
	static final HCTimer addQuartzLongTimeWarn(final String projID, final String schedulerName, final String jobName){
		return (HCTimer)ContextManager.getThreadPool().runAndWait(new ReturnableRunnable() {
			@Override
			public Object run() throws Throwable {
				return new HCTimer("", HCTimer.ONE_SECOND * 30, true) {
					@Override
					public void doBiz() {
						LogManager.warn("long time scheduler job, it maybe block shutdown process of project, please put long task in ProjectContext.run(Runnable) or ProjectContext.runInProjectLevel(Runnable). Job details : [" + projID + "/" + schedulerName + "/" + jobName + "].");
					}
				};
			}
		}, threadToken);
	}
	
}
