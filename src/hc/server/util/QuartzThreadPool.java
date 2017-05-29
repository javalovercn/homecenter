package hc.server.util;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import third.quartz.SchedulerConfigException;
import third.quartz.spi.ThreadPool;

public class QuartzThreadPool implements ThreadPool{
	final ProjectContext ctx;
	int counter;
	final Object lock;
	
	public QuartzThreadPool(final ProjectContext ctx){
		this.ctx = ctx;
		lock = this;
	}
	
	@Override
	public boolean runInThread(final Runnable runnable) {
		synchronized (lock) {
			counter++;
		}
		if(L.isInWorkshop){
			LogManager.log("Quartz in [" + ctx.getProjectID() + "] is consuming thread, total : " + counter);
		}
		
		final Runnable printCounterRunnable = new Runnable() {
			@Override
			public void run() {
				try{
					runnable.run();
				}catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}finally{
					synchronized (lock) {
						if(--counter == 0){
							lock.notify();
						}
					}
					if(L.isInWorkshop){
						LogManager.log("Quartz in [" + ctx.getProjectID() + "] is consuming thread, total : " + counter);
					}
				}
			}
		};
		ServerUIAPIAgent.runInProjContext(ctx, printCounterRunnable);
		return true;
	}

	@Override
	public int blockForAvailableThreads() {
		return 1;
	}

	@Override
	public void initialize() throws SchedulerConfigException {
	}

	@Override
	public void shutdown(final boolean waitForJobsToComplete) {
		if(waitForJobsToComplete){
			synchronized (lock) {
				if(counter > 0){
					try {
						lock.wait();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public int getPoolSize() {
		return Integer.MAX_VALUE;
	}

	@Override
	public void setInstanceId(final String schedInstId) {
		LogManager.log("QuartzThreadPool setInstanceId : " + schedInstId);
	}

	@Override
	public void setInstanceName(final String schedName) {
		LogManager.log("QuartzThreadPool setInstanceName : " + schedName);
	}

}
