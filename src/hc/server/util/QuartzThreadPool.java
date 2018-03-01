package hc.server.util;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.server.ui.design.ProjResponser;
import third.quartz.SchedulerConfigException;
import third.quartz.spi.ThreadPool;

public class QuartzThreadPool implements ThreadPool {
	final ProjectContext ctx;
	final J2SESession j2seSession;
	final String domain;
	int counter;
	int shutingDownJob;
	final Object lock;
	final ProjResponser resp;

	public QuartzThreadPool(final ProjectContext ctx, final J2SESession j2seSession,
			final String domain) {
		this.ctx = ctx;
		this.j2seSession = j2seSession;
		this.domain = domain;
		this.resp = ServerUIAPIAgent.getProjResponserMaybeNull(ctx);
		lock = this;
	}

	@Override
	public boolean runInThread(final Runnable runnable) {
		synchronized (lock) {
			counter++;
		}
		if (L.isInWorkshop) {
			LogManager.log(
					"scheduler [" + domain + "] is consuming thread for job, total : " + counter);
		}

		final Runnable printCounterRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					runnable.run();
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				} finally {
					synchronized (lock) {
						if (--counter == shutingDownJob) {
							lock.notifyAll();
						}
					}
					if (L.isInWorkshop) {
						LogManager.log("scheduler [" + domain
								+ "] revert job thread, consuming total : " + counter);
					}
				}
			}
		};
		if (j2seSession == null) {
			ServerUIAPIAgent.runInProjContext(ctx, printCounterRunnable);
		} else {
			ServerUIAPIAgent.runInSessionThreadPool(j2seSession, resp, printCounterRunnable);
		}
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
		if (waitForJobsToComplete) {
			boolean isStackUseOneThread = false;

			// 检查stack上，是否占用一个
			final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
			for (int i = 0; i < ste.length; i++) {
				final StackTraceElement ele = ste[i];
				final String stackClassName = ele.getClassName();
				if (QuartzRunnableJob.class.getName().equals(stackClassName)
						|| QuartzJRubyJob.class.getName().equals(stackClassName)) {
					isStackUseOneThread = true;
					break;
				}
			}

			if (isStackUseOneThread) {
				synchronized (lock) {
					shutingDownJob++;
				}
			}

			while (true) {
				synchronized (lock) {
					if (counter > shutingDownJob) {
						if (L.isInWorkshop) {
							LogManager.log("scheduler [" + domain
									+ "] is shutdown and wait for job finish, consuming total : "
									+ counter + ", shutingdownJob : " + shutingDownJob);
						}
						try {
							lock.wait();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					} else {
						break;
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
