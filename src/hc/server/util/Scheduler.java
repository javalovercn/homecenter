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

import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.util.ExceptionReporter;
import hc.server.data.StoreDirManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.util.ai.AIPersistentManager;
import hc.server.util.scheduler.AnnualJobCalendar;
import hc.server.util.scheduler.CronExcludeJobCalendar;
import hc.server.util.scheduler.DailyJobCalendar;
import hc.server.util.scheduler.HolidayJobCalendar;
import hc.server.util.scheduler.JobCalendar;
import hc.server.util.scheduler.MonthlyJobCalendar;
import hc.server.util.scheduler.WeeklyJobCalendar;
import hc.util.ThreadConfig;

import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import third.hsqldb.cmdline.SqlFile;
import third.quartz.CronExpression;
import third.quartz.CronScheduleBuilder;
import third.quartz.CronTrigger;
import third.quartz.JobBuilder;
import third.quartz.JobDataMap;
import third.quartz.JobDetail;
import third.quartz.JobKey;
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
 * this class is a wrapper to Quartz Scheduler, the follow document is copied/modified from Scheduler.
 * 
 * <p>
 * A <code>Scheduler</code> maintains a registry of <code>Job</code>s
 * and <code>Trigger</code>s. Once registered, the <code>Scheduler</code>
 * is responsible for executing <code>Job</code> s when their associated
 * <code>Trigger</code> s fire (when their scheduled time arrives).
 * </p>
 * 
 * <p>
 * After a <code>Scheduler</code>
 * has been created, it is in "stand-by" mode, and must invoke its 
 * <code>start()</code> before it will fire any <code>Job</code>s.
 * </p>
 * 
 * <p>
 * <code>Trigger</code> s can then be defined to fire individual <code>Job</code>
 * instances based on given schedules.
 * </p>
 * 
 * <p>
 * the stored job can also be 'manually' triggered through the {@link #triggerJob(String)}.
 * </p>
 * 
 * @author James House
 * @author Sharada Jambula
 */
public final class Scheduler {
	private final ProjectContext projectContext;
	private final third.quartz.Scheduler sched;
	private static final String defaultJobGroup = null;
	private final String domainName;
	
	/**
	 * DONT invoke this method to get an instance, please use {@link ProjectContext#getScheduler(String)}.
	 * @param projectContext
	 * @param domainName
	 * @param isAllInRAM
	 */
	@Deprecated
	public Scheduler(final ProjectContext projectContext, final String domainName, final boolean isAllInRAM){
		this.domainName = domainName;
		this.projectContext = projectContext;
		final String projectID = projectContext.getProjectID();
		
		final String key = projectID + "_" + domainName;
		
		sched = getScheduler(key, domainName, isAllInRAM);
	}

	private final void buildProvider(final String key, final ProjectContext projectContext,
			final String domainName) {
		try{
			DBConnectionManager.getInstance().shutdown(key);
		}catch (final Exception e) {
		}
		
		DBConnectionManager.getInstance().addConnectionProvider(key, new ConnectionProvider() {
			@Override
			public final void shutdown() throws SQLException {
			}
			
			@Override
			public final void initialize() throws SQLException {
			}
			
			final String user = "project_user";
			final String password = ServerUIAPIAgent.getDBPassword(projectContext);
			boolean isInited = false;
			String absPath;
			
			@Override
			public final Connection getConnection() throws SQLException {
				Connection connection = null;
				
				if(isInited == false){
					synchronized (this) {
						if(isInited == false){
							final File cronDir = buildCronDir();
							final File dDir = new File(cronDir, domainName);
							final File dbName = new File(dDir, domainName);//ByteUtil.toHex(StringUtil.getBytes(
							final boolean isNewDB = (dDir.isDirectory() == false);
							absPath = dbName.getAbsolutePath();
							connection = buildNewConn();
							
							if(isNewDB){
								setLastCompactMS(projectContext, domainName);
								try{
									final SqlFile sqlFile = new SqlFile(new InputStreamReader(HSQLDBDelegate.class.getResourceAsStream("tables_hsqldb.sql"),
							                IConstant.UTF_8),
							                "init", System.out, IConstant.UTF_8, false,
							                null);
									sqlFile.setConnection(connection);
									sqlFile.execute();
								}catch (final Exception e) {
//									ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
									ExceptionReporter.printStackTrace(e);
								}
							}
							
							connection = compackDB(connection, domainName);
							
							isInited = true;//要置于最后，以保证absPath != null
						}
					}
				}
				
				if(connection == null){
					connection = buildNewConn();
				}
				
				return connection;
			}

			private final Connection buildNewConn() throws SQLException {
				return AIPersistentManager.getConnection(absPath, user, password);
			}
			
			private final long getLastCompactMS(final ProjectContext ctx, final String cronDomain){
				final String prop = ServerUIAPIAgent.PROJ_CRON_DB_COMPACT_MS + cronDomain;
				final String value = ServerUIAPIAgent.getSuperProp(ctx, prop);
				if(value == null){
					return 0;
				}else{
					try{
						return Long.parseLong(value);
					}catch (final Exception e) {
					}
				}
				return 0;
			}
			
			private final void setLastCompactMS(final ProjectContext ctx, final String cronDomain){
				final String prop = ServerUIAPIAgent.PROJ_CRON_DB_COMPACT_MS + cronDomain;
				ServerUIAPIAgent.setSuperProp(ctx, prop, String.valueOf(System.currentTimeMillis()));
				ctx.saveProperties();
			}
			
			final long half_year = HCTimer.ONE_YEAR / 2;
			
			private final Connection compackDB(Connection connection, final String domainName) throws SQLException {
				if(System.currentTimeMillis() - getLastCompactMS(projectContext, domainName) > half_year){//低频率能增强数据库安全
					try{
						final Statement state = connection.createStatement();
						state.execute(AIPersistentManager.SHUTDOWN_COMPACT);
						state.close();
					}catch (final Exception e) {
						ExceptionReporter.printStackTrace(e);
					}
					setLastCompactMS(projectContext, domainName);
					connection = buildNewConn();
				}
				return connection;
			}
		});
	}
	
	private final File buildCronDir() {
		return projectContext.getPrivateFile(
				StoreDirManager.HC_SYS_FOR_USER_PRIVATE_DIR + StoreDirManager.CRON_SUB_DIR_FOR_USER_PRIVATE_DIR);
	}
	
	private final third.quartz.Scheduler getScheduler(final String key, final String domainName, final boolean isRam){
		try{
			final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
			third.quartz.Scheduler scheduler;
			scheduler = factory.getScheduler(key);
			if(scheduler == null){
		        JobStore store;
		        
		        if(isRam){
		        	store = new RAMJobStore();
		        }else{
		    		buildProvider(key, projectContext, domainName);

		        	final JobStoreTX storeTx = new JobStoreTX();
		        	storeTx.setDriverDelegateClass(HSQLDBDelegate.class.getName());
		        	storeTx.setDataSource(key);
		        	storeTx.setTablePrefix("QRTZ_");
		        	storeTx.setInstanceId(key);
		        	
		        	store = storeTx;
		        }
		        
				final QuartzThreadPool threadPool = new QuartzThreadPool(projectContext);
		        threadPool.initialize();
		        factory.createScheduler(key, key, threadPool, store);
		        scheduler = factory.getScheduler(key);
			}
	        return scheduler;
//        	this.scheduler.getListenerManager().addTriggerListener(new TriggerListener());
//       	this.scheduler.start();
		}catch (final Throwable e) {
//    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
		
		return null;
	}
	
    /**
     * Starts the <code>Scheduler</code>'s threads that fire Triggers.
     * When a scheduler is first created it is in "stand-by" mode, and will not
     * fire triggers.  The scheduler can also be put into stand-by mode by
     * calling the <code>standby()</code> method. 
     * 
     * <p>
     * The misfire/recovery process will be started, if it is the initial call
     * to this method on this scheduler instance.
     * </p>
     * if fail to start, invoke {@link #getThrownException()} to get thrown exception.
     * @see #startDelayed(int)
     * @see #standby()
     * @see #shutdown()
     */
    public final void start() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.start();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }

    /**
     * Calls {@link #start()} after the indicated number of seconds.
     * (This call does not block). This can be useful within applications that
     * have initializers that create the scheduler immediately, before the
     * resources needed by the executing jobs have been fully initialized.
     * <BR><BR>
     * if fail to start, invoke {@link #getThrownException()} to get thrown exception.
     * @see #start() 
     * @see #standby()
     * @see #shutdown()
     */
    public final void startDelayed(final int seconds) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.startDelayed(seconds);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }

    /**
     * Temporarily halts the <code>Scheduler</code>'s firing of Triggers.
     * 
     * <p>
     * When <code>start()</code> is called (to bring the scheduler out of 
     * stand-by mode), trigger misfire instructions will NOT be applied
     * during the execution of the <code>start()</code> method - any misfires 
     * will be detected immediately afterward (by the <code>JobStore</code>'s 
     * normal process).
     * </p>
     * 
     * <p>
     * The scheduler is not destroyed, and can be re-started at any time.
     * </p>
     * if fail to standby, invoke {@link #getThrownException()} to get thrown exception.
     * @see #start()
     * @see #pauseAll()
     */
    public final void standby() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
	    	sched.standby();
	    }catch (final Throwable e) {
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
     * @return false means not started or exception thrown. 
     * <BR>invoke {@link #getThrownException()} to get thrown exception.
     * @see #start()
     * @see #isShutdown()
     * @see #isStandby()
     */     
    public final boolean isStarted() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.isStarted();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }
    
    /**
     * Reports whether the <code>Scheduler</code> is in stand-by mode.
     * @return false means not standby or exception thrown,
     * <BR>invoke {@link #getThrownException()} to get thrown exception.
     * @see #standby()
     * @see #start()
     */
    public final boolean isStandby() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.isInStandbyMode();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }

    /**
     * Halts the <code>Scheduler</code>'s firing of <code>Triggers</code>,
     * and cleans up all resources associated with the Scheduler. Equivalent to
     * <code>shutdown(false)</code>.
     * 
     * <p>
     * The scheduler cannot be re-started.
     * </p>
     * if fail to shutdown, invoke {@link #getThrownException()} to get thrown exception.
     * @see #shutdown(boolean)
     */
    public final void shutdown() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
	        sched.shutdown();
	        ServerUIAPIAgent.removeScheduler(projectContext, domainName);
	    }catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
    }

    /**
     * Halts the <code>Scheduler</code>'s firing of <code>Triggers</code>,
     * and cleans up all resources associated with the Scheduler.
     * 
     * <p>
     * The scheduler cannot be re-started.
     * </p>
     * if fail to shutdown, invoke {@link #getThrownException()} to get thrown exception.
     * @param waitForJobsToComplete
     *          if <code>true</code> the scheduler will not allow this method
     *          to return until all currently executing jobs have completed.
     * @see #shutdown()
     */
    public final void shutdown(final boolean waitForJobsToComplete) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
	        sched.shutdown(waitForJobsToComplete);
	        ServerUIAPIAgent.removeScheduler(projectContext, domainName);
	    }catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
    }

    /**
     * Reports whether the <code>Scheduler</code> has been shutdown.
     * @return false means is shutdown or exception thrown, 
     * <BR>invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean isShutdown() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
	        return sched.isShutdown();
	    }catch (final Throwable e) {
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
			ExceptionReporter.printStackTrace(e);
		}
    	return false;
    }

//    public final List<JobExecutionContext> getCurrentlyExecutingJobs() throws Exception {
//        return sched.getCurrentlyExecutingJobs();
//    }
    
    /**
     * Clears (deletes!) all scheduling data - all Jobs, Triggers
     * Calendars.
     * <BR><BR>
     * if fail to clear, invoke {@link #getThrownException()} to get thrown exception.
     */
    public final void clear() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.clear();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }
    
    /**
     * Add (if exists then replace) the given <code>Job</code> to the scheduler. The <code>Job</code> will be silent until
     * it is scheduled with a <code>Trigger</code> or <code>Scheduler.triggerJob()</code>
     * is called for it.
     * <BR><BR>
     * the added job is durable, see {@link #deleteTrigger(String)} for more.
     * <BR><BR>
     * if fail to add job, invoke {@link #getThrownException()} to get thrown exception.
     * @see #addJob(String, String, boolean, boolean, boolean)
     * @param jobKey
     * @param jrubyScripts
     */
    public final void addJob(final String jobKey, final String jrubyScripts) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.addJob(buildJobDetail(jobKey, jrubyScripts), true);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }
    
    /**
     * get the names of all registered jobs.
     * @return null means exception thrown,<BR>if no job returns a list with zero size.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     * @return
     */
    public final List<String> getJobKeys(){
    	final ArrayList<String> out = new ArrayList<String>(10);
        try {
        	ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			final Set<JobKey> jobKeys = sched.getJobKeys(GroupMatcher.jobGroupEquals(JobKey.DEFAULT_GROUP));
			final Iterator<JobKey>it = jobKeys.iterator();
			while(it.hasNext()){
				out.add(it.next().getName());
			}
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    		return null;
    	}
        return out;
    }
    
    /**
     * get the names of all registered triggers.
     * @return null means exception thrown,<BR>if no trigger returns a list with zero size.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final List<String> getTriggerKeys(){
    	final ArrayList<String> out = new ArrayList<String>(10);
    	try {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			final Set<TriggerKey> triggerKeys = sched.getTriggerKeys(GroupMatcher.triggerGroupEquals(TriggerKey.DEFAULT_GROUP));
			final Iterator<TriggerKey>it = triggerKeys.iterator();
			while(it.hasNext()){
				out.add(it.next().getName());
			}
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    		return null;
    	}
    	return out;
    }

    /**
     * Add (if exists then replace) the given <code>Job</code> to the scheduler. The <code>Job</code> will be silent until
     * it is scheduled with a <code>Trigger</code> or <code>Scheduler.triggerJob()</code>
     * is called for it.
     * <BR><BR>
     * the added job is durable, see {@link #deleteTrigger(String)} for more.
     * <p>
     * With the <code>storeNonDurableWhileAwaitingScheduling</code> parameter
     * set to <code>true</code>, a non-durable job can be stored.  Once it is
     * scheduled, it will resume normal non-durable behavior (i.e. be deleted
     * once there are no remaining associated triggers).
     * </p>
     * if fail to add job, invoke {@link #getThrownException()} to get thrown exception.
     */
    public final void addJob(final String jobKey, final String jrubyScripts, final boolean storeNonDurableWhileAwaitingScheduling) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.addJob(buildJobDetail(jobKey, jrubyScripts), true, storeNonDurableWhileAwaitingScheduling);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }
    
    /**
     * Add (if exists then replace) the given <code>Job</code> to the scheduler.
     * <BR><BR>
     * if fail to add job, invoke {@link #getThrownException()} to get thrown exception.
     * @param jobKey
     * @param jrubyScripts
     * @param isDurable false means the job will also be deleted if there is no other trigger refers to it.<BR>see {@link #deleteTrigger(String)}.
     * @param jobShouldRecover Instructs the Scheduler whether or not the Job should be re-executed if a 'recovery' or 'fail-over' situation is encountered. If not explicitly set, the default value is false.
     * @param storeNonDurableWhileAwaitingScheduling
     */
    public final void addJob(final String jobKey, final String jrubyScripts, final boolean isDurable, final boolean jobShouldRecover,
    		final boolean storeNonDurableWhileAwaitingScheduling) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
	        final JobDetail jobDetail = buildJobDetail(jobKey, jrubyScripts, isDurable, jobShouldRecover);
			sched.addJob(jobDetail, true, storeNonDurableWhileAwaitingScheduling);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }
    
    private final JobDetail buildJobDetail(final String jobKey, final String jobScripts){
    	return buildJobDetail(jobKey, jobScripts, true, false);
    }
    
    private final JobDetail buildJobDetail(final String jobKey, final String jobScripts, final boolean isDurable, final boolean jobShouldRecover){
    	final JobDataMap map = new JobDataMap();
    	QuartzJRubyJob.setJobScripts(map, jobScripts);
    	
    	final JobBuilder builder = JobBuilder.newJob(QuartzJRubyJob.class);
    	builder.withIdentity(jobKey, defaultJobGroup);
    	builder.setJobData(map);
    	builder.storeDurably(isDurable);
    	builder.requestRecovery(jobShouldRecover);
    	
    	return builder.build();
    }
    
    /**
     * Delete the identified <code>Job</code>s and any
     * associated <code>Trigger</code>s.
     * 
     * <p>Note that while this bulk operation is likely more efficient than
     * invoking <code>deleteJob(JobKey jobKey)</code> several
     * times, it may have the adverse affect of holding data locks for a
     * single long duration of time (rather than lots of small durations
     * of time).</p>
     *  
     * @return false means one/more were not deleted or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean deleteJobs(final List<String> jobKeys) {
    	final int size = jobKeys.size();
    	
    	final ArrayList<JobKey> arrayJobKeys = new ArrayList<JobKey>(size);
    	final Iterator<String> it = jobKeys.iterator();
    	while(it.hasNext()){
    		arrayJobKeys.add(buildJobKey(it.next()));
    	}
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.deleteJobs(arrayJobKeys);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }
    
    /**
     * it is equals with {@link #deleteJobs(List)}
     * @param jobKeys
     * @return
     */
    public final boolean removeJobs(final List<String> jobKeys) {
    	return deleteJobs(jobKeys);
    }
    
    private final JobKey buildJobKey(final String jobKey){
    	return new JobKey(jobKey, defaultJobGroup);
    }

//    public final void scheduleJobs(final Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, final boolean replace) throws Exception {
//        sched.scheduleJobs(triggersAndJobs, replace);
//    }

//    public final void scheduleJob(final JobDetail jobDetail, final Set<? extends Trigger> triggersForJob, final boolean replace) throws Exception {
//        sched.scheduleJob(jobDetail,  triggersForJob, replace);
//    }
    
    /**
     * Remove all of the indicated triggers from the scheduler.
     * 
     * <p>If the related job does not have any other triggers, and the job is
     * not durable, then the job will also be deleted.</p>
     * 
     * <p>Note that while this bulk operation is likely more efficient than
     * invoking <code>unscheduleJob(TriggerKey triggerKey)</code> several
     * times, it may have the adverse affect of holding data locks for a
     * single long duration of time (rather than lots of small durations
     * of time).</p> 
     * @return false means one/more not found or exception thrown,<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean deleteTriggers(final List<String> triggerKeys) {
    	final int size = triggerKeys.size();
    	
    	final ArrayList<TriggerKey> arrayTrigKeys = new ArrayList<TriggerKey>(size);
    	final Iterator<String> it = triggerKeys.iterator();
    	while(it.hasNext()){
    		arrayTrigKeys.add(buildTriggerKey(it.next()));
    	}
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.unscheduleJobs(arrayTrigKeys);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }
    
    /**
     * it is equals with {@link #deleteTriggers(List)}.
     * @param triggerKeys
     * @return
     */
    public final boolean removeTriggers(final List<String> triggerKeys) {
    	return deleteTriggers(triggerKeys);
    }
    
    private final TriggerKey buildTriggerKey(final String triggerKey){
    	return TriggerKey.triggerKey(triggerKey, defaultJobGroup);
    }
    
    /**
     * Delete the identified <code>Job</code> and any
     * associated <code>Trigger</code>s.
     * 
     * @return false means not found or exception thrown,<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean deleteJob(final String jobKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.deleteJob(buildJobKey(jobKey));
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }
    
    /**
     * it is equals with {@link #deleteJob(String)}.
     * @param jobKey
     * @return
     */
    public final boolean removeJob(final String jobKey) {
    	return deleteJob(jobKey);
    }

    /**
     * Remove the indicated trigger from the scheduler.
     * 
     * <p>If the related job does not have any other triggers, and the job is
     * not durable, then the job will also be deleted.</p>
     * @return false means not found or exception thrown,<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean deleteTrigger(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.unscheduleJob(buildTriggerKey(triggerKey));
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }
    
    /**
     * it is equals with {@link #deleteTrigger(String)}.
     * @param triggerKey
     * @return
     */
    public final boolean removeTrigger(final String triggerKey) {
    	return deleteTrigger(triggerKey);
    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    public final Date rescheduleJob(final String triggerKey,
//            final Trigger newTrigger) throws Exception {
//        return sched.rescheduleJob(triggerKey, newTrigger);
//    }

    /**
     * Trigger the identified <code>Job</code>
     * (execute it now).<BR><BR>
     * if fail to trigger job, invoke {@link #getThrownException()} to get thrown exception.
     * @param jobKey
     */
    public final void triggerJob(final String jobKey) {
    	final JobKey jKey = buildJobKey(jobKey);
		final JobDataMap extMap = null;
		try{
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			sched.triggerJob(jKey, extMap);
		}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    public final void triggerJob(final JobKey jobKey, final JobDataMap data)
//        throws Exception {
//        sched.triggerJob(jobKey, data);
//    }

    /**
     * Pause the <code>Trigger</code> with the given key.
     * <BR><BR>
     * if fail to pause trigger, invoke {@link #getThrownException()} to get thrown exception.
     * @see #resumeTrigger(String)
     */
    public final void pauseTrigger(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.pauseTrigger(buildTriggerKey(triggerKey));
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    public final void pauseTriggers(final GroupMatcher<TriggerKey> matcher) throws Exception {
//        sched.pauseTriggers(matcher);
//    }

    /**
     * Pause the <code>Job</code> with the given
     * key - by pausing all of its current <code>Trigger</code>s.
     * <BR><BR>
     * if fail to pause job, invoke {@link #getThrownException()} to get thrown exception.
     * @see #resumeJob(String)
     */
    public final void pauseJob(final String jobKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.pauseJob(buildJobKey(jobKey));
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }

//    /** 
//     * @see third.quartz.Scheduler#getPausedTriggerGroups()
//     */
//    public final Set<String> getPausedTriggerGroups() throws Exception {
//        return sched.getPausedTriggerGroups();
//    }
    
//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    public final void pauseJobs(final GroupMatcher<JobKey> matcher) throws Exception {
//        sched.pauseJobs(matcher);
//    }

    /**
     * Resume (un-pause) the trigger with the given
     * key.
     * 
     * <p>
     * If the <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     * if fail to resume trigger, invoke {@link #getThrownException()} to get thrown exception.
     * @see #pauseTrigger(String)
     */
    public final void resumeTrigger(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.resumeTrigger(buildTriggerKey(triggerKey));
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    public final void resumeTriggers(final GroupMatcher<TriggerKey> matcher) throws Exception {
//        sched.resumeTriggers(matcher);
//    }

    /**
     * Resume (un-pause) the <code>Job</code> with
     * the given key.
     * 
     * <p>
     * If any of the <code>Job</code>'s<code>Trigger</code> s missed one
     * or more fire-times, then the <code>Trigger</code>'s misfire
     * instruction will be applied.
     * </p>
     * if fail to resume job, invoke {@link #getThrownException()} to get thrown exception.
     * @see #pauseJob(String)
     */
    public final void resumeJob(final String jobKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.resumeJob(buildJobKey(jobKey));
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }

//    /**
//     * <p>
//     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
//     * </p>
//     */
//    public final void resumeJobs(final GroupMatcher<JobKey> matcher) throws Exception {
//        sched.resumeJobs(matcher);
//    }

    /**
     * Pause all triggers, however, after using this method <code>resumeAll()</code> 
     * must be called to clear the scheduler's state of 'remembering' that all 
     * new triggers will be paused as they are added. 
     * 
     * <p>
     * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
     * instructions WILL be applied.
     * </p>
     * if fail to pause all, invoke {@link #getThrownException()} to get thrown exception.
     * @see #resumeAll()
     * @see #standby()
     */
    public final void pauseAll() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.pauseAll();
    	}catch (final Throwable e) {
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
     * if fail to resume all, invoke {@link #getThrownException()} to get thrown exception.
     * @see #pauseAll()
     */
    public final void resumeAll() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.resumeAll();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }

//    public final List<String> getJobGroupNames() throws Exception {
//        return sched.getJobGroupNames();
//    }

//    public final List<? extends Trigger> getTriggersOfJob(final JobKey jobKey)
//        throws Exception {
//        return sched.getTriggersOfJob(jobKey);
//    }

//    public final Set<JobKey> getJobKeys(final GroupMatcher<JobKey> matcher) throws Exception {
//        return sched.getJobKeys(matcher);
//    }

//    public final List<String> getTriggerGroupNames() throws Exception {
//        return sched.getTriggerGroupNames();
//    }

//    public final Set<TriggerKey> getTriggerKeys(final GroupMatcher<TriggerKey> matcher) throws Exception {
//        return sched.getTriggerKeys(matcher);
//    }

    /**
     * get the scripts of the <code>jobKey</code>.
     * @param jobKey
     * @return null means exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final String getJobScripts(final String jobKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
	        final JobDataMap map = sched.getJobDetail(buildJobKey(jobKey)).getJobDataMap();
	        return QuartzJRubyJob.getJobScripts(map);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	
    	return null;
    }

//    public final Trigger getTrigger(final TriggerKey triggerKey)
//        throws Exception {
//        return sched.getTrigger(triggerKey);
//    }

//    public final TriggerState getTriggerState(final TriggerKey triggerKey)
//        throws Exception {
//        return sched.getTriggerState(triggerKey);
//    }
    
	private final Trigger getTrigger(final String triggerKey) throws Exception {
		final Trigger trigger = sched.getTrigger(buildTriggerKey(triggerKey));
        if (trigger == null) {
            throw new IllegalArgumentException("No such trigger: " + triggerKey);
        }
        return trigger;
	}
	
	/**
	 * true means the the trigger with <code>triggerKey</code> is exists.
	 * @param triggerKey
	 * @return false means not exists or exception thrown,<BR>
	 * invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean isExistsTrigger(final String triggerKey){
		return hasTrigger(triggerKey);
	}
	
	/**
	 * it is equals with {@link #isExistsTrigger(String)}.
	 * @param triggerKey
	 * @return false means not exists or exception thrown,<BR>
	 * invoke {@link #getThrownException()} to get thrown exception.
	 */
	public final boolean hasTrigger(final String triggerKey){
		try{
			ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
			return sched.checkExists(buildTriggerKey(triggerKey));
		}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
		return false;
	}
    
	/**
	 * 
	 * @param triggerKey
	 * @return null means exception thrown.<BR>
	 * invoke {@link #getThrownException()} to get thrown exception.
	 */
    public final String getTriggerJobKey(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).getJobKey().getName();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }
    
    /**
     * Get the name of the <code>{@link JobCalendar}</code> associated with this
     * Trigger.
     * 
     * @return <code>null</code> if there is no associated JobCalendar or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final String getTriggerCalendarName(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).getCalendarName();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }

    /**
     * Used by the <code>{@link Scheduler}</code> to determine whether or not
     * it is possible for this <code>Trigger</code> to fire again.
     * 
     * <p>
     * If the returned value is <code>false</code> then the <code>Scheduler</code>
     * may remove the <code>Trigger</code> from Store</code>.
     * </p>
     * @return false means not fire again or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean isTriggerMayFireAgain(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).mayFireAgain();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }

    /**
     * Get the time at which the <code>Trigger</code> should occur.
     * @return null means exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final Date getTriggerStartTime(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).getStartTime();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }

    /**
     * Get the time at which the <code>Trigger</code> should quit repeating -
     * regardless of any remaining repeats (based on the trigger's particular 
     * repeat settings). 
     * @return null means the never end or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     * @see #getTriggerFinalFireTime(String)
     */
    public final Date getTriggerEndTime(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).getEndTime();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }

    /**
     * Returns the next time at which the <code>Trigger</code> is scheduled to fire. If
     * the trigger will not fire again, <code>null</code> will be returned.  Note that
     * the time returned can possibly be in the past, if the time that was computed
     * for the trigger to next fire has already arrived, but the scheduler has not yet
     * been able to fire the trigger (which would likely be due to lack of resources
     * e.g. threads).
     *
     * <p>The value returned is not guaranteed to be valid until after the <code>Trigger</code>
     * has been added to the scheduler.
     * @return null means not fire again or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     * </p>
     */
    public final Date getTriggerNextFireTime(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).getNextFireTime();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }

    /**
     * Returns the previous time at which the <code>Trigger</code> fired.
     * If the trigger has not yet fired, <code>null</code> will be returned.
     * @return null means not yet fired or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final Date getTriggerPreviousFireTime(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).getPreviousFireTime();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }

    /**
     * Returns the next time at which the <code>Trigger</code> will fire,
     * after the given time. If the trigger will not fire after the given time,
     * <code>null</code> will be returned.
     * @return null means not fire after given time or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final Date getTriggerFireTimeAfter(final String triggerKey, final Date afterTime) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).getFireTimeAfter(afterTime);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }

    /**
     * Returns the last time at which the <code>Trigger</code> will fire, if
     * the Trigger will repeat indefinitely, null will be returned.
     * 
     * <p>
     * Note that the return time *may* be in the past.
     * </p>
     * @return null means repeat indefinitely or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final Date getTriggerFinalFireTime(final String triggerKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return getTrigger(triggerKey).getFinalFireTime();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }
    
//    public final void resetTriggerFromErrorState(final TriggerKey triggerKey)
//            throws Exception {
//        sched.resetTriggerFromErrorState(triggerKey);
//    }

    /**
     * Add (if exists then replace) the given <code>JobCalendar</code> to the Scheduler.
     * <BR><BR>
     * if fail to add calendar or calendar is exists but replace is false, invoke {@link #getThrownException()} to get thrown exception.
     * @param calName
     * @param calendar instance of {@link JobCalendar}, for example {@link AnnualJobCalendar}, {@link CronExcludeJobCalendar}, {@link DailyJobCalendar}, {@link HolidayJobCalendar}, {@link MonthlyJobCalendar} and {@link WeeklyJobCalendar}.
     */
    public final void addCalendar(final String calName, final JobCalendar calendar) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		sched.addCalendar(calName, calendar, true, true);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }
    
//    /**
//     * Add (register) the given cron JobCalendar to the Scheduler.
//     * <BR><BR>
//     * for more about cron expression, please click <a href="http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html</a>
//     * <BR><BR>
//     * for complex calendar, see {@link #addCalendar(String, JobCalendar, boolean, boolean)}.
//     * @param calName
//     * @param cronExpression
//     * @param baseCalendar null means no base calendar.
//     * @throws Exception
//     */
//    public final void addCronCalendar(final String calName, final String cronExpression, final JobCalendar baseCalendar) throws Exception {
//    		final JobCalendar calendar = new CronExcludeJobCalendar(baseCalendar, cronExpression);
//            sched.addCalendar(calName, calendar, true, true);
//    }
    
    /**
     * Indicates whether the specified cron expression can be parsed into a 
     * valid cron expression.
     * <BR><BR>
     * for more about Cron Expression, please click <a href="http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html</a>
     * @param cronExpression the cron expression
     * @return a boolean indicating whether the given expression is a valid cron
     *         expression
     */
    public final boolean isValidCronExpression(final String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }
    
    /**
     * add and schedule a cron trigger.<BR><BR>
     * if fail to add cron trigger, invoke {@link #getThrownException()} to get thrown exception.<BR><BR>
     * <STRONG>Note :</STRONG><BR>
     * if trigger is upon a mis-fire situation, then be fired once now.
     * @param triggerKey
     * @param cronExpression for more about Cron Expression, please click <a href="http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html</a>
     * @param calendarName
     * @param jobKey
     * @see #addCronTrigger(String, String, TimeZone, String, Date, Date, int, String, String)
     */
    public final void addCronTrigger(final String triggerKey, final String cronExpression, final String calendarName, final String jobKey) {
    	addCronTrigger(triggerKey,
    			cronExpression, null, calendarName,
    			new Date(), null, MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, jobKey, null);
	}
    
    /**
     * Instructs the <code>{@link Scheduler}</code> that upon a mis-fire
     * situation, the cron trigger wants to have it's
     * next-fire-time updated to the next time in the schedule after the
     * current time (taking into account any associated <code>{@link JobCalendar}</code>,
     * but it does not want to be fired now.
     * @see #MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
     */
    public static final int MISFIRE_INSTRUCTION_DO_NOTHING = CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING;
    
    /**
     * <p>
     * Instructs the <code>Scheduler</code> that upon a mis-fire
     * situation, the cron trigger wants to be fired now
     * by <code>Scheduler</code>.
     * </p>
     * @see #MISFIRE_INSTRUCTION_DO_NOTHING
     */
    public static final int MISFIRE_INSTRUCTION_FIRE_ONCE_NOW = CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW;
    
    /**
     * add and schedule a cron trigger.<BR><BR>
     * if fail to add and schedule cron trigger, invoke {@link #getThrownException()} to get thrown exception.
     * @param triggerKey
     * @param cronExpression for more about Cron Expression, please click <a href="http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html</a>
     * @param cronTimeZone null means default.
     * @param calendarName the name of the Calendar that should be applied to this trigger.
     * @param startTime null means start now.
     * @param endTime null means never end.
     * @param misfireOption one of {@link #MISFIRE_INSTRUCTION_DO_NOTHING}, {@link #MISFIRE_INSTRUCTION_FIRE_ONCE_NOW} or others in future.
     * @param jobKey if null then do nothing.
     * @param description
     */
    public final void addCronTrigger(final String triggerKey, 
    		final String cronExpression, final TimeZone cronTimeZone, final String calendarName, 
    		final Date startTime, final Date endTime, final int misfireOption, 
    		final String jobKey, final String description) {
    	if(jobKey == null){
    		return;
    	}
    	
    	final CronScheduleBuilder cronScheBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
    	cronScheBuilder.inTimeZone(cronTimeZone);
        try{
        	ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
        	
        	if(misfireOption == MISFIRE_INSTRUCTION_FIRE_ONCE_NOW){
        		cronScheBuilder.withMisfireHandlingInstructionFireAndProceed();
        	}else if(misfireOption == MISFIRE_INSTRUCTION_DO_NOTHING){
        		cronScheBuilder.withMisfireHandlingInstructionDoNothing();
        	}else{
        		throw new IllegalArgumentException("illegal misfire option : " + misfireOption);
        	}
        	
            final Trigger trigger = TriggerBuilder.newTrigger().
            		withSchedule(cronScheBuilder).
            		withIdentity(triggerKey, defaultJobGroup).
            		modifiedByCalendar(calendarName).
            		withDescription(description).
            		startAt(startTime==null?new Date():startTime).
            		endAt(endTime).//可以为null
            		forJob(buildJobKey(jobKey)).
            		build();

            sched.scheduleJob(trigger);
        }catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    }

    /**
     * Delete the identified <code>JobCalendar</code> from the Scheduler.
     * 
     * @return false means one or more triggers reference the calendar and exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean deleteCalendar(final String calName) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.deleteCalendar(calName);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }
    
    /**
     * it is equals with {@link #deleteCalendar(String)}.
     * @param calName
     * @return false means one or more triggers reference the calendar and exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean removeCalendar(final String calName) {
    	return deleteCalendar(calName);
    }

    /**
     * Get the <code>{@link JobCalendar}</code> instance with the given name.
     * @return null means not found or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final JobCalendar getCalendar(final String calName) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.getCalendar(calName);
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }

    /**
     * Get the names of all registered <code>{@link JobCalendar}s</code>.
     * @return null means exception thrown.<BR>if no calendar, returns a list with zero size.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final List<String> getCalendarNames() {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.getCalendarNames();
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return null;
    }

    /**
     * Determine whether a Job with the given identifier already 
     * exists within the scheduler.
     * 
     * @param jobKey the identifier to check for
     * @return false means not exists or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean isExistsJob(final String jobKey) {
    	try{
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, null);
    		return sched.checkExists(buildJobKey(jobKey));
    	}catch (final Throwable e) {
    		ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION, e);
    		ExceptionReporter.printStackTrace(e);
    	}
    	return false;
    }
    
    /**
     * it is equals with {@link #isExistsJob(String)}.
     * @param jobKey
     * @return false means not exists or exception thrown.<BR>
     * invoke {@link #getThrownException()} to get thrown exception.
     */
    public final boolean hasJob(final String jobKey) {
    	return isExistsJob(jobKey);
    }
    
    /**
     * it returns null, if there is no exception from the last invoked.
     * @return exception is thrown in current thread from the last invoked.
     */
    public final Throwable getThrownException(){
    	return (Throwable)ThreadConfig.getValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION);
    }
    
//    /**
//     * @see third.quartz.Scheduler#setJobFactory(third.quartz.spi.JobFactory)
//     */
//    public final void setJobFactory(final JobFactory factory) throws Exception {
//        sched.setJobFactory(factory);
//    }

//    public final ListenerManager getListenerManager() throws Exception {
//        return sched.getListenerManager();
//    }

//    /**
//     * Request the interruption, within this Scheduler instance, of all 
//     * currently executing instances of the identified <code>Job</code>, which 
//     * must be an implementor of the <code>InterruptableJob</code> interface.
//     * 
//     * <p>
//     * If more than one instance of the identified job is currently executing,
//     * the <code>InterruptableJob#interrupt()</code> method will be called on
//     * each instance.  However, there is a limitation that in the case that  
//     * <code>interrupt()</code> on one instances throws an exception, all 
//     * remaining  instances (that have not yet been interrupted) will not have 
//     * their <code>interrupt()</code> method called.
//     * </p>
//     * 
//     * <p>
//     * This method is not cluster aware.  That is, it will only interrupt 
//     * instances of the identified InterruptableJob currently executing in this 
//     * Scheduler instance, not across the entire cluster.
//     * </p>
//     * 
//     * @return true if at least one instance of the identified job was found
//     * and interrupted.
//     * @throws Exception if the job does not implement
//     * <code>InterruptableJob</code>, or there is an exception while 
//     * interrupting the job.
//     */
//    public final boolean interruptJob(final String jobKey) throws Exception {
//        return sched.interrupt(buildJobKey(jobKey));
//    }

//    public final boolean interrupt(final String fireInstanceId) throws UnableToInterruptJobException {
//        return sched.interrupt(fireInstanceId);
//    }

}
