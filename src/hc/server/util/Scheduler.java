/* 
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package hc.server.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.ReturnableRunnable;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.ui.design.J2SESession;
import hc.server.util.calendar.AnnualJobCalendar;
import hc.server.util.calendar.CronExcludeJobCalendar;
import hc.server.util.calendar.DailyJobCalendar;
import hc.server.util.calendar.HolidayJobCalendar;
import hc.server.util.calendar.JobCalendar;
import hc.server.util.calendar.MonthlyJobCalendar;
import hc.server.util.calendar.WeeklyJobCalendar;
import hc.util.ThreadConfig;
import third.quartz.CronExpression;
import third.quartz.CronScheduleBuilder;
import third.quartz.CronTrigger;
import third.quartz.JobBuilder;
import third.quartz.JobDataMap;
import third.quartz.JobDetail;
import third.quartz.JobKey;
import third.quartz.SchedulerException;
import third.quartz.Trigger;
import third.quartz.TriggerBuilder;
import third.quartz.TriggerKey;
import third.quartz.impl.DirectSchedulerFactory;
import third.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import third.quartz.impl.jdbcjobstore.JobStoreTX;
import third.quartz.impl.matchers.GroupMatcher;
import third.quartz.simpl.RAMJobStore;
import third.quartz.spi.JobStore;
import third.quartz.utils.ConnectionProvider;
import third.quartz.utils.DBConnectionManager;

/**
 * <p>
 * A <code>Scheduler</code> maintains a registry of <code>Job</code>s and
 * <code>Trigger</code>s. Once registered, the <code>Scheduler</code> is
 * responsible for executing <code>Job</code> s when their associated
 * <code>Trigger</code> s fire (when their scheduled time arrives).
 * </p>
 * 
 * <p>
 * After a <code>Scheduler</code> has been created, it is in "stand-by" mode,
 * and must invoke its <code>start()</code> before it will fire any
 * <code>Job</code>s.
 * </p>
 * 
 * <p>
 * <code>Trigger</code> s can then be defined to fire individual
 * <code>Job</code> instances based on given schedules.
 * </p>
 * 
 * <p>
 * the stored job can also be 'manually' triggered through the
 * {@link #triggerJob(String)}.
 * </p>
 * 
 * @author James House
 * @author Sharada Jambula
 */
public final class Scheduler {
	private final ProjectContext projectContext;
	private final third.quartz.Scheduler sched;
	final boolean isAllInRAM;
	private static final String defaultJobGroup = null;
	private final String domainName;
	final J2SESession j2seSession;
	final boolean isSessionLevel;

	/**
	 * DONT invoke this method to get an instance, please use
	 * {@link ProjectContext#getScheduler(String)}.
	 * 
	 * @param projectContext
	 * @param domainName
	 * @param isAllInRAM
	 * @param j2seSession
	 */
	@Deprecated
	public Scheduler(final ProjectContext projectContext, final String domainName,
			final boolean isAllInRAM, final J2SESession j2seSession) {
		this.domainName = domainName;
		this.projectContext = projectContext;
		this.j2seSession = j2seSession;
		this.isAllInRAM = isAllInRAM;
		isSessionLevel = j2seSession != null;

		final String projectID = projectContext.getProjectID();

		final String key = projectID + "_" + domainName;

		if (isSessionLevel) {
			sched = getScheduler(key, domainName, isAllInRAM, j2seSession);
			j2seSession.addScheduler(projectID, domainName);
		} else {
			// 有可能当前是session，所以需runAndWaitInProjContext
			sched = (third.quartz.Scheduler) ServerUIAPIAgent.runAndWaitInProjContext(
					projectContext, new ReturnableRunnable() {
						@Override
						public Object run() throws Throwable {
							return getScheduler(key, domainName, isAllInRAM, j2seSession);
						}
					});
		}
	}

	private final void buildProvider(final String key, final ProjectContext projectContext,
			final String domainName) {
		try {
			DBConnectionManager.getInstance().shutdown(key);
		} catch (final Exception e) {
		}

		final ConnectionProvider provider = new HSQLDBConnProvider(key, projectContext, domainName);
		DBConnectionManager.getInstance().addConnectionProvider(key, provider);
	}

	private final third.quartz.Scheduler getScheduler(final String key, final String domainName,
			final boolean isRam, final J2SESession j2seSession) {
		try {
			final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
			JobStore store;

			if (isRam) {
				store = new RAMJobStore();
			} else {
				ServerAPIAgent.runAndWaitInSysThread(new ReturnableRunnable() {
					@Override
					public Object run() throws Throwable {
						buildProvider(key, projectContext, domainName);
						return null;
					}
				});

				final JobStoreTX storeTx = new JobStoreTX();
				storeTx.setDriverDelegateClass(HSQLDBDelegate.class.getName());
				storeTx.setDataSource(key);
				storeTx.setTablePrefix("QRTZ_");
				storeTx.setInstanceId(key);

				store = storeTx;
			}

			final QuartzThreadPool threadPool = new QuartzThreadPool(projectContext, j2seSession,
					domainName);
			threadPool.initialize();
			return factory.createScheduler(key, key, threadPool, store);
			// this.scheduler.getListenerManager().addTriggerListener(new
			// TriggerListener());
			// this.scheduler.start();
		} catch (final Throwable e) {
			// ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION,
			// e);
			ExceptionReporter.printStackTrace(e);
		}

		return null;
	}

	/**
	 * Starts the <code>Scheduler</code>'s threads that fire Triggers. When a
	 * scheduler is first created it is in "stand-by" mode, and will not fire
	 * triggers. The scheduler can also be put into stand-by mode by calling the
	 * <code>standby()</code> method.
	 * 
	 * <p>
	 * The misfire/recovery process will be started, if it is the initial call
	 * to this method on this scheduler instance.
	 * </p>
	 * if fail to start, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #startDelayed(int)
	 * @see #standby()
	 * @see #shutdown()
	 */
	public final void start() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.start();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Calls {@link #start()} after the indicated number of seconds. (This call
	 * does not block). This can be useful within applications that have
	 * initializers that create the scheduler immediately, before the resources
	 * needed by the executing jobs have been fully initialized. <BR>
	 * <BR>
	 * if fail to start, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #start()
	 * @see #standby()
	 * @see #shutdown()
	 */
	public final void startDelayed(final int seconds) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.startDelayed(seconds);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Temporarily halts the <code>Scheduler</code>'s firing of Triggers.
	 * 
	 * <p>
	 * When <code>start()</code> is called (to bring the scheduler out of
	 * stand-by mode), trigger misfire instructions will NOT be applied during
	 * the execution of the <code>start()</code> method - any misfires will be
	 * detected immediately afterward (by the <code>JobStore</code>'s normal
	 * process).
	 * </p>
	 * 
	 * <p>
	 * The scheduler is not destroyed, and can be re-started at any time.
	 * </p>
	 * if fail to standby, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #start()
	 * @see #pauseAll()
	 */
	public final void standby() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.standby();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Whether the scheduler has been started.
	 * 
	 * <p>
	 * Note: This only reflects whether <code>{@link #start()}</code> has ever
	 * been called on this scheduler, so it will return <code>true</code> even
	 * if the <code>Scheduler</code> is currently in standby mode or has been
	 * since shutdown.
	 * </p>
	 * 
	 * @return false means not started or exception thrown. <BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 * @see #start()
	 * @see #isShutdown()
	 * @see #isStandby()
	 */
	public final boolean isStarted() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.isStarted();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * Reports whether the <code>Scheduler</code> is in stand-by mode.
	 * 
	 * @return false means not standby or exception thrown, <BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 * @see #standby()
	 * @see #start()
	 */
	public final boolean isStandby() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.isInStandbyMode();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * Halts the <code>Scheduler</code>'s firing of <code>Triggers</code>, and
	 * cleans up all resources associated with the Scheduler. Equivalent to
	 * <code>shutdown(false)</code>.
	 * 
	 * <p>
	 * The scheduler cannot be re-started.
	 * </p>
	 * if fail to shutdown, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #shutdown(boolean)
	 */
	public final void shutdown() {
		shutdown(false);
	}

	/**
	 * Halts the <code>Scheduler</code>'s firing of <code>Triggers</code>, and
	 * releases all resources associated with the scheduler.
	 * <p>
	 * The scheduler cannot be re-started.
	 * </p>
	 * Invoking this method with <code>true</code> is allowed in job, scheduler
	 * will check stack of current thread and minus self from running jobs. <BR>
	 * <BR>
	 * if fail to shutdown, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @param waitForJobsToComplete
	 *            if <code>true</code> the scheduler will not allow this method
	 *            to return until all currently executing jobs have completed.
	 * @see #shutdown()
	 */
	public final void shutdown(final boolean waitForJobsToComplete) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			ServerUIAPIAgent.removeScheduler(projectContext, domainName);// 优先，防止被其它线程再次执行
			sched.shutdown(waitForJobsToComplete);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Reports whether the <code>Scheduler</code> has been shutdown.
	 * 
	 * @return false means is shutdown or exception thrown, <BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean isShutdown() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.isShutdown();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	// public final List<JobExecutionContext> getCurrentlyExecutingJobs() throws
	// Exception {
	// return sched.getCurrentlyExecutingJobs();
	// }

	/**
	 * Clears (deletes!) all scheduling data - all Jobs, Triggers Calendars. <BR>
	 * <BR>
	 * if fail to clear, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 */
	public final void clear() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			LogManager.log("scheduler [" + domainName + "] clear all job, calendar and trigger.");
			sched.clear();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Add (if exists then replace) the given <code>Job</code> to the scheduler.
	 * The <code>Job</code> will be silent until it is scheduled with a
	 * <code>Trigger</code> or <code>Scheduler.triggerJob()</code> is called. <BR>
	 * <BR>
	 * a job can be scheduled by multiple triggers. <BR>
	 * <BR>
	 * the added job is durable, see {@link #deleteTrigger(String)} for more. <BR>
	 * <BR>
	 * if fail to add job, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #addJob(String, String, boolean, boolean, boolean)
	 * @param jobKey
	 * @param jrubyScripts
	 *            the max length is 1GB.
	 */
	public final void addJob(final String jobKey, final String jrubyScripts) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.addJob(buildJobDetail(jobKey, jrubyScripts), true);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * add (if exists then replace) a <code>Runnable</code> instance as a job. <BR>
	 * <BR>
	 * <STRONG>Note :</STRONG><BR>
	 * 1. this method is valid only in AllInRAM scheduler, because the instance
	 * will be lost after shutdown project. <BR>
	 * <BR>
	 * if fail to add job, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @param jobKey
	 * @param runnable
	 *            to build instance in IDE, see
	 *            {@link IDEUtil#buildRunnable(Runnable)}.
	 */
	public final void addJob(final String jobKey, final Runnable runnable) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			if (isAllInRAM == false) {
				throw new SchedulerException(
						"addJob(String jobKey, Runnable runnable) must be invoked in AllInRAM scheduler.");
			}
			// if(isSessionLevel == false){
			// //if current scheduler is project level, but runs in session
			// level thread, then fail.
			// if(ProjectContext.isCurrentThreadInSessionLevel()){
			// throw new
			// SchedulerException("addJob(String jobKey, Runnable runnable) can't run in session level thread when it is project level scheduler.");
			// }
			// }
			sched.addJob(buildJobDetail(jobKey, runnable, isDurable, jobShouldRecover), true);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * get the names of all registered jobs.
	 * 
	 * @return null means exception thrown,<BR>
	 *         if no job returns a list with zero size.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 * @return
	 */
	public final List<String> getJobKeys() {
		final ArrayList<String> out = new ArrayList<String>(10);
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			final Set<JobKey> jobKeys = sched.getJobKeys(GroupMatcher
					.jobGroupEquals(JobKey.DEFAULT_GROUP));
			final Iterator<JobKey> it = jobKeys.iterator();
			while (it.hasNext()) {
				out.add(it.next().getName());
			}
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
			return null;
		}
		return out;
	}

	/**
	 * get the names of all registered triggers.
	 * 
	 * @return null means exception thrown,<BR>
	 *         if no trigger returns a list with zero size.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final List<String> getTriggerKeys() {
		final ArrayList<String> out = new ArrayList<String>(10);
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			final Set<TriggerKey> triggerKeys = sched.getTriggerKeys(GroupMatcher
					.triggerGroupEquals(TriggerKey.DEFAULT_GROUP));
			final Iterator<TriggerKey> it = triggerKeys.iterator();
			while (it.hasNext()) {
				out.add(it.next().getName());
			}
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
			return null;
		}
		return out;
	}

	/**
	 * Add (if exists then replace) the given <code>Job</code> to the scheduler.
	 * The <code>Job</code> will be silent until it is scheduled with a
	 * <code>Trigger</code> or <code>Scheduler.triggerJob()</code> is called for
	 * it. <BR>
	 * <BR>
	 * the added job is durable, see {@link #deleteTrigger(String)} for more.
	 * <p>
	 * With the <code>storeNonDurableWhileAwaitingScheduling</code> parameter
	 * set to <code>true</code>, a non-durable job can be stored. Once it is
	 * scheduled, it will resume normal non-durable behavior (i.e. be deleted
	 * once there are no remaining associated triggers).
	 * </p>
	 * if fail to add job, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @param jobKey
	 * @param jrubyScripts
	 *            the max length is 1GB.
	 * @param storeNonDurableWhileAwaitingScheduling
	 */
	public final void addJob(final String jobKey, final String jrubyScripts,
			final boolean storeNonDurableWhileAwaitingScheduling) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.addJob(buildJobDetail(jobKey, jrubyScripts), true,
					storeNonDurableWhileAwaitingScheduling);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Add (if exists then replace) the given <code>Job</code> to the scheduler. <BR>
	 * <BR>
	 * if fail to add job, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @param jobKey
	 * @param jrubyScripts
	 *            the max length is 1GB.
	 * @param isDurable
	 *            false means the job will also be deleted if there is no other
	 *            trigger refers to it.<BR>
	 *            see {@link #deleteTrigger(String)}.
	 * @param jobShouldRecover
	 *            Instructs the Scheduler whether or not the Job should be
	 *            re-executed if a 'recovery' or 'fail-over' situation is
	 *            encountered. If not explicitly set, the default value is
	 *            false.
	 * @param storeNonDurableWhileAwaitingScheduling
	 *            With the <code>storeNonDurableWhileAwaitingScheduling</code>
	 *            parameter set to <code>true</code>, a non-durable job can be
	 *            stored. Once it is scheduled, it will resume normal
	 *            non-durable behavior (i.e. be deleted once there are no
	 *            remaining associated triggers).
	 */
	public final void addJob(final String jobKey, final String jrubyScripts,
			final boolean isDurable, final boolean jobShouldRecover,
			final boolean storeNonDurableWhileAwaitingScheduling) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			final JobDetail jobDetail = buildJobDetail(jobKey, jrubyScripts, isDurable,
					jobShouldRecover);
			sched.addJob(jobDetail, true, storeNonDurableWhileAwaitingScheduling);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	private static final boolean isDurable = true;
	private static final boolean jobShouldRecover = false;

	private final JobDetail buildJobDetail(final String jobKey, final String jobScripts) {
		return buildJobDetail(jobKey, jobScripts, isDurable, jobShouldRecover);
	}

	private final JobDetail buildJobDetail(final String jobKey, final String jobScripts,
			final boolean isDurable, final boolean jobShouldRecover) {
		final JobDataMap map = new JobDataMap();
		QuartzJRubyJob.setJobScripts(map, jobScripts, projectContext.getProjectID(), domainName,
				jobKey);

		final JobBuilder builder = JobBuilder.newJob(QuartzJRubyJob.class);
		return setBuilderJob(builder, map, jobKey, isDurable, jobShouldRecover);
	}

	private final JobDetail buildJobDetail(final String jobKey, final Runnable run,
			final boolean isDurable, final boolean jobShouldRecover) {
		final JobDataMap map = new JobDataMap();
		QuartzRunnableJob.setRunnable(map, run, projectContext.getProjectID(), domainName, jobKey);

		final JobBuilder builder = JobBuilder.newJob(QuartzRunnableJob.class);
		return setBuilderJob(builder, map, jobKey, isDurable, jobShouldRecover);
	}

	private final JobDetail setBuilderJob(final JobBuilder builder, final JobDataMap map,
			final String jobKey, final boolean isDurable, final boolean jobShouldRecover) {
		builder.withIdentity(jobKey, defaultJobGroup);
		builder.setJobData(map);
		builder.storeDurably(isDurable);
		builder.requestRecovery(jobShouldRecover);

		return builder.build();
	}

	/**
	 * Delete the identified <code>Job</code>s and any associated
	 * <code>Trigger</code>s.
	 * 
	 * <p>
	 * Note that while this bulk operation is likely more efficient than
	 * invoking <code>deleteJob(JobKey jobKey)</code> several times, it may have
	 * the adverse affect of holding data locks for a single long duration of
	 * time (rather than lots of small durations of time).
	 * </p>
	 * 
	 * @return false means one/more were not deleted or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean deleteJobs(final List<String> jobKeys) {
		final int size = jobKeys.size();

		final ArrayList<JobKey> arrayJobKeys = new ArrayList<JobKey>(size);
		final Iterator<String> it = jobKeys.iterator();
		while (it.hasNext()) {
			arrayJobKeys.add(buildJobKey(it.next()));
		}
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.deleteJobs(arrayJobKeys);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * it is equals with {@link #deleteJobs(List)}
	 * 
	 * @param jobKeys
	 * @return
	 */
	public final boolean removeJobs(final List<String> jobKeys) {
		return deleteJobs(jobKeys);
	}

	private final JobKey buildJobKey(final String jobKey) {
		return new JobKey(jobKey, defaultJobGroup);
	}

	// public final void scheduleJobs(final Map<JobDetail, Set<? extends
	// Trigger>> triggersAndJobs, final boolean replace) throws Exception {
	// sched.scheduleJobs(triggersAndJobs, replace);
	// }

	// public final void scheduleJob(final JobDetail jobDetail, final Set<?
	// extends Trigger> triggersForJob, final boolean replace) throws Exception
	// {
	// sched.scheduleJob(jobDetail, triggersForJob, replace);
	// }

	/**
	 * Remove all of the indicated triggers from the scheduler.
	 * 
	 * <p>
	 * If the related job does not have any other triggers, and the job is not
	 * durable, then the job will also be deleted.
	 * </p>
	 * 
	 * <p>
	 * Note that while this bulk operation is likely more efficient than
	 * invoking <code>unscheduleJob(TriggerKey triggerKey)</code> several times,
	 * it may have the adverse affect of holding data locks for a single long
	 * duration of time (rather than lots of small durations of time).
	 * </p>
	 * 
	 * @return false means one/more not found or exception thrown,<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean deleteTriggers(final List<String> triggerKeys) {
		final int size = triggerKeys.size();

		final ArrayList<TriggerKey> arrayTrigKeys = new ArrayList<TriggerKey>(size);
		final Iterator<String> it = triggerKeys.iterator();
		while (it.hasNext()) {
			arrayTrigKeys.add(buildTriggerKey(it.next()));
		}
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.unscheduleJobs(arrayTrigKeys);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * it is equals with {@link #deleteTriggers(List)}.
	 * 
	 * @param triggerKeys
	 * @return
	 */
	public final boolean removeTriggers(final List<String> triggerKeys) {
		return deleteTriggers(triggerKeys);
	}

	private final TriggerKey buildTriggerKey(final String triggerKey) {
		return TriggerKey.triggerKey(triggerKey, defaultJobGroup);
	}

	/**
	 * Delete the identified <code>Job</code> and any associated
	 * <code>Trigger</code>s.
	 * 
	 * @return false means not found or exception thrown,<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean deleteJob(final String jobKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.deleteJob(buildJobKey(jobKey));
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * it is equals with {@link #deleteJob(String)}.
	 * 
	 * @param jobKey
	 * @return
	 */
	public final boolean removeJob(final String jobKey) {
		return deleteJob(jobKey);
	}

	/**
	 * Remove the indicated trigger from the scheduler.
	 * 
	 * <p>
	 * If the related job does not have any other triggers, and the job is not
	 * durable, then the job will also be deleted.
	 * </p>
	 * 
	 * @return false means not found or exception thrown,<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean deleteTrigger(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.unscheduleJob(buildTriggerKey(triggerKey));
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * it is equals with {@link #deleteTrigger(String)}.
	 * 
	 * @param triggerKey
	 * @return
	 */
	public final boolean removeTrigger(final String triggerKey) {
		return deleteTrigger(triggerKey);
	}

	// /**
	// * <p>
	// * Calls the equivalent method on the 'proxied'
	// <code>QuartzScheduler</code>.
	// * </p>
	// */
	// public final Date rescheduleJob(final String triggerKey,
	// final Trigger newTrigger) throws Exception {
	// return sched.rescheduleJob(triggerKey, newTrigger);
	// }

	/**
	 * Trigger the identified <code>Job</code> (execute it now).<BR>
	 * <BR>
	 * if fail to trigger job, invoke {@link #getThrownException()} to get
	 * thrown exception.
	 * 
	 * @param jobKey
	 */
	public final void triggerJob(final String jobKey) {
		final JobKey jKey = buildJobKey(jobKey);
		final JobDataMap extMap = null;
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.triggerJob(jKey, extMap);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	// /**
	// * <p>
	// * Calls the equivalent method on the 'proxied'
	// <code>QuartzScheduler</code>.
	// * </p>
	// */
	// public final void triggerJob(final JobKey jobKey, final JobDataMap data)
	// throws Exception {
	// sched.triggerJob(jobKey, data);
	// }

	/**
	 * Pause the <code>Trigger</code> with the given key. <BR>
	 * <BR>
	 * if fail to pause trigger, invoke {@link #getThrownException()} to get
	 * thrown exception.
	 * 
	 * @see #resumeTrigger(String)
	 */
	public final void pauseTrigger(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.pauseTrigger(buildTriggerKey(triggerKey));
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	// /**
	// * <p>
	// * Calls the equivalent method on the 'proxied'
	// <code>QuartzScheduler</code>.
	// * </p>
	// */
	// public final void pauseTriggers(final GroupMatcher<TriggerKey> matcher)
	// throws Exception {
	// sched.pauseTriggers(matcher);
	// }

	/**
	 * Pause the <code>Job</code> with the given key - by pausing all of its
	 * current <code>Trigger</code>s. <BR>
	 * <BR>
	 * if fail to pause job, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #resumeJob(String)
	 */
	public final void pauseJob(final String jobKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.pauseJob(buildJobKey(jobKey));
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	// /**
	// * @see third.quartz.Scheduler#getPausedTriggerGroups()
	// */
	// public final Set<String> getPausedTriggerGroups() throws Exception {
	// return sched.getPausedTriggerGroups();
	// }

	// /**
	// * <p>
	// * Calls the equivalent method on the 'proxied'
	// <code>QuartzScheduler</code>.
	// * </p>
	// */
	// public final void pauseJobs(final GroupMatcher<JobKey> matcher) throws
	// Exception {
	// sched.pauseJobs(matcher);
	// }

	/**
	 * Resume (un-pause) the trigger with the given key.
	 * 
	 * <p>
	 * If the <code>Trigger</code> missed one or more fire-times, then the
	 * <code>Trigger</code>'s misfire instruction will be applied.
	 * </p>
	 * if fail to resume trigger, invoke {@link #getThrownException()} to get
	 * thrown exception.
	 * 
	 * @see #pauseTrigger(String)
	 */
	public final void resumeTrigger(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.resumeTrigger(buildTriggerKey(triggerKey));
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	// /**
	// * <p>
	// * Calls the equivalent method on the 'proxied'
	// <code>QuartzScheduler</code>.
	// * </p>
	// */
	// public final void resumeTriggers(final GroupMatcher<TriggerKey> matcher)
	// throws Exception {
	// sched.resumeTriggers(matcher);
	// }

	/**
	 * Resume (un-pause) the <code>Job</code> with the given key.
	 * 
	 * <p>
	 * If any of the <code>Job</code>'s<code>Trigger</code> s missed one or more
	 * fire-times, then the <code>Trigger</code>'s misfire instruction will be
	 * applied.
	 * </p>
	 * if fail to resume job, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #pauseJob(String)
	 */
	public final void resumeJob(final String jobKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.resumeJob(buildJobKey(jobKey));
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	// /**
	// * <p>
	// * Calls the equivalent method on the 'proxied'
	// <code>QuartzScheduler</code>.
	// * </p>
	// */
	// public final void resumeJobs(final GroupMatcher<JobKey> matcher) throws
	// Exception {
	// sched.resumeJobs(matcher);
	// }

	/**
	 * Pause all triggers, however, after using this method
	 * <code>resumeAll()</code> must be called to clear the scheduler's state of
	 * 'remembering' that all new triggers will be paused as they are added.
	 * 
	 * <p>
	 * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
	 * instructions WILL be applied.
	 * </p>
	 * if fail to pause all, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #resumeAll()
	 * @see #standby()
	 */
	public final void pauseAll() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.pauseAll();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Resume (un-pause) all triggers - similar to calling
	 * <code>resumeTriggerGroup(group)</code> on every group.
	 * 
	 * <p>
	 * If any <code>Trigger</code> missed one or more fire-times, then the
	 * <code>Trigger</code>'s misfire instruction will be applied.
	 * </p>
	 * if fail to resume all, invoke {@link #getThrownException()} to get thrown
	 * exception.
	 * 
	 * @see #pauseAll()
	 */
	public final void resumeAll() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.resumeAll();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	// public final List<String> getJobGroupNames() throws Exception {
	// return sched.getJobGroupNames();
	// }

	// public final List<? extends Trigger> getTriggersOfJob(final JobKey
	// jobKey)
	// throws Exception {
	// return sched.getTriggersOfJob(jobKey);
	// }

	// public final Set<JobKey> getJobKeys(final GroupMatcher<JobKey> matcher)
	// throws Exception {
	// return sched.getJobKeys(matcher);
	// }

	// public final List<String> getTriggerGroupNames() throws Exception {
	// return sched.getTriggerGroupNames();
	// }

	// public final Set<TriggerKey> getTriggerKeys(final
	// GroupMatcher<TriggerKey> matcher) throws Exception {
	// return sched.getTriggerKeys(matcher);
	// }

	/**
	 * get the scripts of the <code>jobKey</code>.
	 * 
	 * @param jobKey
	 * @return null means exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final String getJobScripts(final String jobKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			final JobDataMap map = sched.getJobDetail(buildJobKey(jobKey)).getJobDataMap();
			return QuartzJRubyJob.getJobScripts(map);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}

		return null;
	}

	// public final Trigger getTrigger(final TriggerKey triggerKey)
	// throws Exception {
	// return sched.getTrigger(triggerKey);
	// }

	// public final TriggerState getTriggerState(final TriggerKey triggerKey)
	// throws Exception {
	// return sched.getTriggerState(triggerKey);
	// }

	private final Trigger getTrigger(final String triggerKey) throws Exception {
		final Trigger trigger = sched.getTrigger(buildTriggerKey(triggerKey));
		if (trigger == null) {
			throw new IllegalArgumentException("No such trigger: " + triggerKey);
		}
		return trigger;
	}

	/**
	 * true means the the trigger with <code>triggerKey</code> is exists.
	 * 
	 * @param triggerKey
	 * @return false means not exists or exception thrown,<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean isExistsTrigger(final String triggerKey) {
		return hasTrigger(triggerKey);
	}

	/**
	 * it is equals with {@link #isExistsTrigger(String)}.
	 * 
	 * @param triggerKey
	 * @return false means not exists or exception thrown,<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean hasTrigger(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.checkExists(buildTriggerKey(triggerKey));
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * 
	 * @param triggerKey
	 * @return null means exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final String getTriggerJobKey(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).getJobKey().getName();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Get the name of the <code>{@link JobCalendar}</code> associated with this
	 * Trigger.
	 * 
	 * @return <code>null</code> if there is no associated JobCalendar or
	 *         exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final String getTriggerCalendarName(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).getCalendarName();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Used by the <code>{@link Scheduler}</code> to determine whether or not it
	 * is possible for this <code>Trigger</code> to fire again.
	 * 
	 * <p>
	 * If the returned value is <code>false</code> then the
	 * <code>Scheduler</code> may remove the <code>Trigger</code> from Store.
	 * </p>
	 * 
	 * @return false means not fire again or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean isTriggerMayFireAgain(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).mayFireAgain();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * Get the time at which the <code>Trigger</code> should occur.
	 * 
	 * @return null means exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final Date getTriggerStartTime(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).getStartTime();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Get the time at which the <code>Trigger</code> should quit repeating -
	 * regardless of any remaining repeats (based on the trigger's particular
	 * repeat settings).
	 * 
	 * @return null means the never end or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 * @see #getTriggerFinalFireTime(String)
	 */
	public final Date getTriggerEndTime(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).getEndTime();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Returns the next time at which the <code>Trigger</code> is scheduled to
	 * fire. If the trigger will not fire again, <code>null</code> will be
	 * returned. Note that the time returned can possibly be in the past, if the
	 * time that was computed for the trigger to next fire has already arrived,
	 * but the scheduler has not yet been able to fire the trigger (which would
	 * likely be due to lack of resources e.g. threads).
	 * 
	 * <p>
	 * The value returned is not guaranteed to be valid until after the
	 * <code>Trigger</code> has been added to the scheduler.
	 * 
	 * @return null means not fire again or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 *         </p>
	 */
	public final Date getTriggerNextFireTime(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).getNextFireTime();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Returns the previous time at which the <code>Trigger</code> fired. If the
	 * trigger has not yet fired, <code>null</code> will be returned.
	 * 
	 * @return null means not yet fired or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final Date getTriggerPreviousFireTime(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).getPreviousFireTime();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Returns the next time at which the <code>Trigger</code> will fire, after
	 * the given time. If the trigger will not fire after the given time,
	 * <code>null</code> will be returned.
	 * 
	 * @return null means not fire after given time or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final Date getTriggerFireTimeAfter(final String triggerKey, final Date afterTime) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).getFireTimeAfter(afterTime);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Returns the last time at which the <code>Trigger</code> will fire, if the
	 * Trigger will repeat indefinitely, null will be returned.
	 * 
	 * <p>
	 * Note that the return time *may* be in the past.
	 * </p>
	 * 
	 * @return null means repeat indefinitely or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final Date getTriggerFinalFireTime(final String triggerKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return getTrigger(triggerKey).getFinalFireTime();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	// public final void resetTriggerFromErrorState(final TriggerKey triggerKey)
	// throws Exception {
	// sched.resetTriggerFromErrorState(triggerKey);
	// }

	/**
	 * Add (if exists then replace) the given <code>JobCalendar</code> to the
	 * Scheduler. <BR>
	 * <BR>
	 * if fail to add calendar or calendar is exists but replace is false,
	 * invoke {@link #getThrownException()} to get thrown exception.
	 * 
	 * @param calName
	 * @param calendar
	 *            instance of {@link JobCalendar}, for example
	 *            {@link AnnualJobCalendar}, {@link CronExcludeJobCalendar},
	 *            {@link DailyJobCalendar}, {@link HolidayJobCalendar},
	 *            {@link MonthlyJobCalendar} and {@link WeeklyJobCalendar}.
	 */
	public final void addCalendar(final String calName, final JobCalendar calendar) {
		addCalendarImpl(calName, calendar, true);
	}

	private final boolean addCalendarImpl(final String calName, final JobCalendar calendar,
			final boolean isCheckCalHC) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			if (calName == null) {
				throw new IllegalAccessException("Calendar name is null");
			}
			if (isCheckCalHC) {
				if (calName.startsWith(SYS_RESERVED)) {
					throw new IllegalArgumentException("Calendar name can't be begin with '"
							+ SYS_RESERVED + "'.");
				}
			}
			sched.addCalendar(calName, calendar, true, true);
			return true;
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	// /**
	// * Add (register) the given cron JobCalendar to the Scheduler.
	// * <BR><BR>
	// * for more about cron expression, please click <a
	// href="http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html</a>
	// * <BR><BR>
	// * for complex calendar, see {@link #addCalendar(String, JobCalendar,
	// boolean, boolean)}.
	// * @param calName
	// * @param cronExpression
	// * @param baseCalendar null means no base calendar.
	// * @throws Exception
	// */
	// public final void addCronCalendar(final String calName, final String
	// cronExpression, final JobCalendar baseCalendar) throws Exception {
	// final JobCalendar calendar = new CronExcludeJobCalendar(baseCalendar,
	// cronExpression);
	// sched.addCalendar(calName, calendar, true, true);
	// }

	/**
	 * <P>
	 * Cron expressions are comprised of 6 required fields and one optional
	 * field separated by white space. The fields respectively are described as
	 * follows:
	 * 
	 * <table cellpadding="3" cellspacing="1" border='1'>
	 * <tr>
	 * <th align="left">Field Name</th>
	 * <th align="left">Allowed Values</th>
	 * <th align="left">Allowed Special Characters</th>
	 * </tr>
	 * <tr>
	 * <td align="left"><code>Seconds</code></td>
	 * <td align="left"><code>0-59</code></td>
	 * <td align="left"><code>, - * /</code></td>
	 * </tr>
	 * <tr>
	 * <td align="left"><code>Minutes</code></td>
	 * <td align="left"><code>0-59</code></td>
	 * <td align="left"><code>, - * /</code></td>
	 * </tr>
	 * <tr>
	 * <td align="left"><code>Hours</code></td>
	 * <td align="left"><code>0-23</code></td>
	 * <td align="left"><code>, - * /</code></td>
	 * </tr>
	 * <tr>
	 * <td align="left"><code>Day-of-month</code></td>
	 * <td align="left"><code>1-31</code></td>
	 * <td align="left"><code>, - * ? / L W</code></td>
	 * </tr>
	 * <tr>
	 * <td align="left"><code>Month</code></td>
	 * <td align="left"><code>0-11 or JAN-DEC</code></td>
	 * <td align="left"><code>, - * /</code></td>
	 * </tr>
	 * <tr>
	 * <td align="left"><code>Day-of-Week</code></td>
	 * <td align="left"><code>1-7 or SUN-SAT</code></td>
	 * <td align="left"><code>, - * ? / L #</code></td>
	 * </tr>
	 * <tr>
	 * <td align="left"><code>Year (Optional)</code></td>
	 * <td align="left"><code>empty, 1970-2199</code></td>
	 * <td align="left"><code>, - * /</code></td>
	 * </tr>
	 * </table>
	 * <P>
	 * The '*' character is used to specify all values. For example,
	 * &quot;*&quot; in the minute field means &quot;every minute&quot;.
	 * <P>
	 * The '?' character is allowed for the day-of-month and day-of-week fields.
	 * It is used to specify 'no specific value'. This is useful when you need
	 * to specify something in one of the two fields, but not the other.
	 * <P>
	 * The '-' character is used to specify ranges For example &quot;10-12&quot;
	 * in the hour field means &quot;the hours 10, 11 and 12&quot;.
	 * <P>
	 * The ',' character is used to specify additional values. For example
	 * &quot;MON,WED,FRI&quot; in the day-of-week field means &quot;the days
	 * Monday, Wednesday, and Friday&quot;.
	 * <P>
	 * The '/' character is used to specify increments. For example
	 * &quot;0/15&quot; in the seconds field means &quot;the seconds 0, 15, 30,
	 * and 45&quot;. And &quot;5/15&quot; in the seconds field means &quot;the
	 * seconds 5, 20, 35, and 50&quot;. Specifying '*' before the '/' is
	 * equivalent to specifying 0 is the value to start with. Essentially, for
	 * each field in the expression, there is a set of numbers that can be
	 * turned on or off. For seconds and minutes, the numbers range from 0 to
	 * 59. For hours 0 to 23, for days of the month 0 to 31, and for months 0 to
	 * 11 (JAN to DEC). The &quot;/&quot; character simply helps you turn on
	 * every &quot;nth&quot; value in the given set. Thus &quot;7/6&quot; in the
	 * month field only turns on month &quot;7&quot;, it does NOT mean every 6th
	 * month, please note that subtlety.
	 * <P>
	 * The 'L' character is allowed for the day-of-month and day-of-week fields.
	 * This character is short-hand for &quot;last&quot;, but it has different
	 * meaning in each of the two fields. For example, the value &quot;L&quot;
	 * in the day-of-month field means &quot;the last day of the month&quot; -
	 * day 31 for January, day 28 for February on non-leap years. If used in the
	 * day-of-week field by itself, it simply means &quot;7&quot; or
	 * &quot;SAT&quot;. But if used in the day-of-week field after another
	 * value, it means &quot;the last xxx day of the month&quot; - for example
	 * &quot;6L&quot; means &quot;the last friday of the month&quot;. You can
	 * also specify an offset from the last day of the month, such as "L-3"
	 * which would mean the third-to-last day of the calendar month. <i>When
	 * using the 'L' option, it is important not to specify lists, or ranges of
	 * values, as you'll get confusing/unexpected results.</i>
	 * <P>
	 * The 'W' character is allowed for the day-of-month field. This character
	 * is used to specify the weekday (Monday-Friday) nearest the given day. As
	 * an example, if you were to specify &quot;15W&quot; as the value for the
	 * day-of-month field, the meaning is: &quot;the nearest weekday to the 15th
	 * of the month&quot;. So if the 15th is a Saturday, the trigger will fire
	 * on Friday the 14th. If the 15th is a Sunday, the trigger will fire on
	 * Monday the 16th. If the 15th is a Tuesday, then it will fire on Tuesday
	 * the 15th. However if you specify &quot;1W&quot; as the value for
	 * day-of-month, and the 1st is a Saturday, the trigger will fire on Monday
	 * the 3rd, as it will not 'jump' over the boundary of a month's days. The
	 * 'W' character can only be specified when the day-of-month is a single
	 * day, not a range or list of days.
	 * <P>
	 * The 'L' and 'W' characters can also be combined for the day-of-month
	 * expression to yield 'LW', which translates to &quot;last weekday of the
	 * month&quot;.
	 * <P>
	 * The '#' character is allowed for the day-of-week field. This character is
	 * used to specify &quot;the nth&quot; XXX day of the month. For example,
	 * the value of &quot;6#3&quot; in the day-of-week field means the third
	 * Friday of the month (day 6 = Friday and &quot;#3&quot; = the 3rd one in
	 * the month). Other examples: &quot;2#1&quot; = the first Monday of the
	 * month and &quot;4#5&quot; = the fifth Wednesday of the month. Note that
	 * if you specify &quot;#5&quot; and there is not 5 of the given day-of-week
	 * in the month, then no firing will occur that month. If the '#' character
	 * is used, there can only be one expression in the day-of-week field
	 * (&quot;3#1,6#3&quot; is not valid, since there are two expressions).
	 * <P>
	 * <!--The 'C' character is allowed for the day-of-month and day-of-week
	 * fields. This character is short-hand for "calendar". This means values
	 * are calculated against the associated calendar, if any. If no calendar is
	 * associated, then it is equivalent to having an all-inclusive calendar. A
	 * value of "5C" in the day-of-month field means "the first day included by
	 * the calendar on or after the 5th". A value of "1C" in the day-of-week
	 * field means
	 * "the first day included by the calendar on or after Sunday".-->
	 * <P>
	 * The legal characters and the names of months and days of the week are not
	 * case sensitive.
	 * 
	 * <p>
	 * <b>NOTES:</b>
	 * <ul>
	 * <li>Support for specifying both a day-of-week and a day-of-month value is
	 * not complete (you'll need to use the '?' character in one of these
	 * fields).</li>
	 * <li>Overflowing ranges is supported - that is, having a larger number on
	 * the left hand side than the right. You might do 22-2 to catch 10 o'clock
	 * at night until 2 o'clock in the morning, or you might have NOV-FEB. It is
	 * very important to note that overuse of overflowing ranges creates ranges
	 * that don't make sense and no effort has been made to determine which
	 * interpretation CronExpression chooses. An example would be
	 * "0 0 14-6 ? * FRI-MON".</li>
	 * <li>The legal characters and the names of months and days of the week are
	 * not case sensitive. <tt>MON</tt> is the same as <tt>mon</tt>.</li>
	 * </ul>
	 * </p>
	 * 
	 * <STRONG>Examples</STRONG>
	 * 
	 * <p>
	 * Here are some full examples:
	 * </p>
	 * <table cellpadding="3" cellspacing="1" border='1'>
	 * <tbody>
	 * <tr>
	 * <td width="200">**Expression**</td>
	 * <td>**Meaning**</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 0 12 * * ?</tt></td>
	 * 
	 * <td>Fire at 12pm (noon) every day</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 ? * *</tt></td>
	 * <td>Fire at 10:15am every day</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 * * ?</tt></td>
	 * 
	 * <td>Fire at 10:15am every day</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 * * ? *</tt></td>
	 * <td>Fire at 10:15am every day</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 * * ? 2005</tt></td>
	 * 
	 * <td>Fire at 10:15am every day during the year 2005</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 * 14 * * ?</tt></td>
	 * <td>Fire every minute starting at 2pm and ending at 2:59pm, every day</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 0/5 14 * * ?</tt></td>
	 * 
	 * <td>Fire every 5 minutes starting at 2pm and ending at 2:55pm, every day</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 0/5 14,18 * * ?</tt></td>
	 * <td>Fire every 5 minutes starting at 2pm and ending at 2:55pm, AND fire
	 * every 5 minutes starting at 6pm and ending at 6:55pm, every day</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 0-5 14 * * ?</tt></td>
	 * 
	 * <td>Fire every minute starting at 2pm and ending at 2:05pm, every day</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 10,44 14 ? 3 WED</tt></td>
	 * <td>Fire at 2:10pm and at 2:44pm every Wednesday in the month of March.</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 ? * MON-FRI</tt></td>
	 * 
	 * <td>Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday
	 * </td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 15 * ?</tt></td>
	 * <td>Fire at 10:15am on the 15th day of every month</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 L * ?</tt></td>
	 * 
	 * <td>Fire at 10:15am on the last day of every month</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 L-2 * ?</tt></td>
	 * 
	 * <td>Fire at 10:15am on the 2nd-to-last last day of every month</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 ? * 6L</tt></td>
	 * <td>Fire at 10:15am on the last Friday of every month</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 ? * 6L</tt></td>
	 * 
	 * <td>Fire at 10:15am on the last Friday of every month</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 ? * 6L 2002-2005</tt></td>
	 * <td>Fire at 10:15am on every last friday of every month during the years
	 * 2002, 2003, 2004 and 2005</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 15 10 ? * 6#3</tt></td>
	 * 
	 * <td>Fire at 10:15am on the third Friday of every month</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 0 12 1/5 * ?</tt></td>
	 * <td>Fire at 12pm (noon) every 5 days every month, starting on the first
	 * day of the month.</td>
	 * </tr>
	 * <tr>
	 * <td><tt>0 11 11 11 11 ?</tt></td>
	 * 
	 * <td>Fire every November 11th at 11:11am.</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 * <BR>
	 * Pay attention to the effects of '?' and '*' in the day-of-week and
	 * day-of-month fields! <BR>
	 * <BR>
	 * <STRONG>author</STRONG> Sharada Jambula, James House<BR>
	 * <STRONG>author</STRONG> Contributions from Mads Henderson<BR>
	 * <STRONG>author</STRONG> Refactoring from CronTrigger to CronExpression by
	 * Aaron Craven <BR>
	 * <BR>
	 * 
	 * @param cronExpression
	 *            the cron expression
	 * @return a boolean indicating whether the given expression is a valid cron
	 *         expression
	 */
	public final boolean isValidCronExpression(final String cronExpression) {// ***注意***：如果CronExpression升级，请同步更新Doc
		return CronExpression.isValidExpression(cronExpression);
	}

	/**
	 * add (if exists then replace) and schedule a cron trigger.<BR>
	 * if fail to add cron trigger, invoke {@link #getThrownException()} to get
	 * thrown exception.<BR>
	 * <BR>
	 * if trigger is upon a mis-fire situation, then be fired once now. <BR>
	 * <BR>
	 * to execute job now, see {@link #triggerJob(String)}.
	 * 
	 * @param triggerKey
	 * @param cronExpression
	 *            for more about Cron Expression, click
	 *            {@link #isValidCronExpression(String)}.
	 * @param calendarName
	 *            the name of the calendar that should be applied to this
	 *            trigger, null means no day in calendar is excluded, <BR>
	 *            The next trigger day must be met <code>cronExpression</code>
	 *            and <code>calendarName</code>.
	 * @param jobKey
	 *            if null then do nothing.
	 * @see #addCronTrigger(String, String, TimeZone, String, Date, Date, int,
	 *      String, String)
	 */
	public final void addCronTrigger(final String triggerKey, final String cronExpression,
			final String calendarName, final String jobKey) {
		addCronTrigger(triggerKey, cronExpression, null, calendarName, new Date(), null,
				MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, jobKey, null);
	}

	/**
	 * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
	 * situation, the cron trigger wants to have it's next-fire-time updated to
	 * the next time in the schedule after the current time (taking into account
	 * any associated <code>{@link JobCalendar}</code>, but it does not want to
	 * be fired now.
	 * 
	 * @see #MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
	 */
	public static final int MISFIRE_INSTRUCTION_DO_NOTHING = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;

	/**
	 * <p>
	 * Instructs the <code>Scheduler</code> that upon a mis-fire situation, the
	 * cron trigger wants to be fired now by <code>Scheduler</code>.
	 * </p>
	 * 
	 * @see #MISFIRE_INSTRUCTION_DO_NOTHING
	 */
	public static final int MISFIRE_INSTRUCTION_FIRE_ONCE_NOW = CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;

	private static final String SYS_RESERVED = CCoreUtil.SYS_RESERVED_KEYS_START;
	private static final String ALL_DAYS_CALENDAR = SYS_RESERVED + "AllDays";

	private static final MonthlyJobCalendar allDays = new MonthlyJobCalendar();

	/**
	 * add (if exists then replace) and schedule a cron trigger.<BR>
	 * if fail to add and schedule cron trigger, invoke
	 * {@link #getThrownException()} to get thrown exception. <BR>
	 * <BR>
	 * to execute job now, see {@link #triggerJob(String)}.
	 * 
	 * @param triggerKey
	 * @param cronExpression
	 *            for more about Cron Expression, click
	 *            {@link #isValidCronExpression(String)}.
	 * @param cronTimeZone
	 *            null means default.
	 * @param calendarName
	 *            the name of the calendar that should be applied to this
	 *            trigger, null means no day in calendar is excluded, <BR>
	 *            The next trigger day must be met <code>cronExpression</code>
	 *            and <code>calendarName</code>.
	 * @param startTime
	 *            null means start now.
	 * @param endTime
	 *            null means never end.
	 * @param misfireOption
	 *            one of {@link #MISFIRE_INSTRUCTION_DO_NOTHING},
	 *            {@link #MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} or others in
	 *            future.
	 * @param jobKey
	 *            if null then do nothing.
	 * @param description
	 */
	public final void addCronTrigger(final String triggerKey, final String cronExpression,
			final TimeZone cronTimeZone, String calendarName, final Date startTime,
			final Date endTime, final int misfireOption, final String jobKey,
			final String description) {
		if (jobKey == null) {
			LogManager.err("fail to addCronTrigger for jobyKey is null.");
			return;
		}

		if (calendarName == null) {
			calendarName = ALL_DAYS_CALENDAR;
			if (addCalendarImpl(calendarName, allDays, false) == false) {
				return;
			}
			// calendarName = ALL_DAYS_CALENDAR + ResourceUtil.buildUUID();
			// addCalendar(calendarName, new MonthlyJobCalendar());
		}

		final CronScheduleBuilder cronScheBuilder = CronScheduleBuilder
				.cronSchedule(cronExpression);
		cronScheBuilder.inTimeZone(cronTimeZone);
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);

			if (misfireOption == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
				cronScheBuilder.withMisfireHandlingInstructionFireAndProceed();
			} else if (misfireOption == MISFIRE_INSTRUCTION_DO_NOTHING) {
				cronScheBuilder.withMisfireHandlingInstructionDoNothing();
			} else {
				throw new IllegalArgumentException("illegal misfire option : " + misfireOption);
			}

			final Trigger trigger = TriggerBuilder.newTrigger().withSchedule(cronScheBuilder)
					.withIdentity(triggerKey, defaultJobGroup).modifiedByCalendar(calendarName)
					.withDescription(description)
					.startAt(startTime == null ? new Date() : startTime).endAt(endTime).// 可以为null
					forJob(buildJobKey(jobKey)).build();

			sched.scheduleJob(trigger);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Delete the identified <code>JobCalendar</code> from the Scheduler.
	 * 
	 * @return false means one or more triggers reference the calendar and
	 *         exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean deleteCalendar(final String calName) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			if (calName == null) {
				throw new IllegalAccessException("Calendar name is null");
			} else if (calName.startsWith(SYS_RESERVED)) {
				throw new IllegalArgumentException("Calendar name can't be begin with '"
						+ SYS_RESERVED + "'.");
			}
			return sched.deleteCalendar(calName);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * it is equals with {@link #deleteCalendar(String)}.
	 * 
	 * @param calName
	 * @return false means one or more triggers reference the calendar and
	 *         exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean removeCalendar(final String calName) {
		return deleteCalendar(calName);
	}

	/**
	 * Get the <code>{@link JobCalendar}</code> instance with the given name.
	 * 
	 * @return null means not found or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final JobCalendar getCalendar(final String calName) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.getCalendar(calName);
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Get the names of all registered <code>{@link JobCalendar}s</code>.
	 * 
	 * @return null means exception thrown.<BR>
	 *         if no calendar, returns a list with zero size.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final List<String> getCalendarNames() {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.getCalendarNames();
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Determine whether a Job with the given identifier already exists within
	 * the scheduler.
	 * 
	 * @param jobKey
	 *            the identifier to check for
	 * @return false means not exists or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean isExistsJob(final String jobKey) {
		try {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.checkExists(buildJobKey(jobKey));
		} catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}

	/**
	 * it is equals with {@link #isExistsJob(String)}.
	 * 
	 * @param jobKey
	 * @return false means not exists or exception thrown.<BR>
	 *         invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean hasJob(final String jobKey) {
		return isExistsJob(jobKey);
	}

	/**
	 * it returns null, if there is no exception from the last invoked.
	 * 
	 * @return exception is thrown in current thread from the last invoked.
	 */
	public final Throwable getThrownException() {
		return (Throwable) ThreadConfig.getValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION);
	}

	// /**
	// * @see third.quartz.Scheduler#setJobFactory(third.quartz.spi.JobFactory)
	// */
	// public final void setJobFactory(final JobFactory factory) throws
	// Exception {
	// sched.setJobFactory(factory);
	// }

	// public final ListenerManager getListenerManager() throws Exception {
	// return sched.getListenerManager();
	// }

	// /**
	// * Request the interruption, within this Scheduler instance, of all
	// * currently executing instances of the identified <code>Job</code>, which
	// * must be an implementor of the <code>InterruptableJob</code> interface.
	// *
	// * <p>
	// * If more than one instance of the identified job is currently executing,
	// * the <code>InterruptableJob#interrupt()</code> method will be called on
	// * each instance. However, there is a limitation that in the case that
	// * <code>interrupt()</code> on one instances throws an exception, all
	// * remaining instances (that have not yet been interrupted) will not have
	// * their <code>interrupt()</code> method called.
	// * </p>
	// *
	// * <p>
	// * This method is not cluster aware. That is, it will only interrupt
	// * instances of the identified InterruptableJob currently executing in
	// this
	// * Scheduler instance, not across the entire cluster.
	// * </p>
	// *
	// * @return true if at least one instance of the identified job was found
	// * and interrupted.
	// * @throws Exception if the job does not implement
	// * <code>InterruptableJob</code>, or there is an exception while
	// * interrupting the job.
	// */
	// public final boolean interruptJob(final String jobKey) throws Exception {
	// return sched.interrupt(buildJobKey(jobKey));
	// }

	// public final boolean interrupt(final String fireInstanceId) throws
	// UnableToInterruptJobException {
	// return sched.interrupt(fireInstanceId);
	// }

}
