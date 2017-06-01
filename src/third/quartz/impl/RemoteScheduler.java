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

package third.quartz.impl;

import hc.server.util.scheduler.JobCalendar;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import third.quartz.JobDataMap;
import third.quartz.JobDetail;
import third.quartz.JobExecutionContext;
import third.quartz.JobKey;
import third.quartz.ListenerManager;
import third.quartz.Scheduler;
import third.quartz.SchedulerContext;
import third.quartz.SchedulerException;
import third.quartz.SchedulerMetaData;
import third.quartz.Trigger;
import third.quartz.TriggerKey;
import third.quartz.UnableToInterruptJobException;
import third.quartz.Trigger.TriggerState;
import third.quartz.core.RemotableQuartzScheduler;
import third.quartz.impl.matchers.GroupMatcher;
import third.quartz.spi.JobFactory;

/**
 * <p>
 * An implementation of the <code>Scheduler</code> interface that remotely
 * proxies all method calls to the equivalent call on a given <code>QuartzScheduler</code>
 * instance, via RMI.
 * </p>
 * 
 * @see third.quartz.Scheduler
 * @see third.quartz.core.QuartzScheduler
 * 
 * @author James House
 */
public class RemoteScheduler implements Scheduler {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private RemotableQuartzScheduler rsched;

    private String schedId;

    private String rmiHost;

    private int rmiPort;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Construct a <code>RemoteScheduler</code> instance to proxy the given
     * <code>RemoteableQuartzScheduler</code> instance, and with the given
     * <code>SchedulingContext</code>.
     * </p>
     */
    public RemoteScheduler(String schedId, String host, int port) {
        this.schedId = schedId;
        this.rmiHost = host;
        this.rmiPort = port;
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    protected RemotableQuartzScheduler getRemoteScheduler()
        throws SchedulerException {
        if (rsched != null) {
            return rsched;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(rmiHost, rmiPort);

            rsched = (RemotableQuartzScheduler) registry.lookup(schedId);

        } catch (Exception e) {
            SchedulerException initException = new SchedulerException(
                    "Could not get handle to remote scheduler: "
                            + e.getMessage(), e);
            throw initException;
        }

        return rsched;
    }

    protected SchedulerException invalidateHandleCreateException(String msg,
            Exception cause) {
        rsched = null;
        SchedulerException ex = new SchedulerException(msg, cause);
        return ex;
    }

    /**
     * <p>
     * Returns the name of the <code>Scheduler</code>.
     * </p>
     */
    public String getSchedulerName() throws SchedulerException {
        try {
            return getRemoteScheduler().getSchedulerName();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Returns the instance Id of the <code>Scheduler</code>.
     * </p>
     */
    public String getSchedulerInstanceId() throws SchedulerException {
        try {
            return getRemoteScheduler().getSchedulerInstanceId();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    public SchedulerMetaData getMetaData() throws SchedulerException {
        try {
            RemotableQuartzScheduler sched = getRemoteScheduler();
            return new SchedulerMetaData(getSchedulerName(),
                    getSchedulerInstanceId(), getClass(), true, isStarted(), 
                    isInStandbyMode(), isShutdown(), sched.runningSince(), 
                    sched.numJobsExecuted(), sched.getJobStoreClass(), 
                    sched.supportsPersistence(), sched.isClustered(), sched.getThreadPoolClass(), 
                    sched.getThreadPoolSize(), sched.getVersion());

        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }

    }

    /**
     * <p>
     * Returns the <code>SchedulerContext</code> of the <code>Scheduler</code>.
     * </p>
     */
    public SchedulerContext getContext() throws SchedulerException {
        try {
            return getRemoteScheduler().getSchedulerContext();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Schedululer State Management Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void start() throws SchedulerException {
        try {
            getRemoteScheduler().start();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void startDelayed(int seconds) throws SchedulerException {
        try {
            getRemoteScheduler().startDelayed(seconds);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void standby() throws SchedulerException {
        try {
            getRemoteScheduler().standby();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * Whether the scheduler has been started.  
     * 
     * <p>
     * Note: This only reflects whether <code>{@link #start()}</code> has ever
     * been called on this Scheduler, so it will return <code>true</code> even 
     * if the <code>Scheduler</code> is currently in standby mode or has been 
     * since shutdown.
     * </p>
     * 
     * @see #start()
     * @see #isShutdown()
     * @see #isInStandbyMode()
     */    
    public boolean isStarted() throws SchedulerException {
        try {
            return (getRemoteScheduler().runningSince() != null);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }   
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean isInStandbyMode() throws SchedulerException {
        try {
            return getRemoteScheduler().isInStandbyMode();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void shutdown() throws SchedulerException {
        try {
            String schedulerName = getSchedulerName();
            
            getRemoteScheduler().shutdown();
            
//            yyh
//            SchedulerRepository.getInstance().remove(schedulerName);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void shutdown(boolean waitForJobsToComplete)
        throws SchedulerException {
        try {
            String schedulerName = getSchedulerName();
            
            getRemoteScheduler().shutdown(waitForJobsToComplete);

//            yyh
//            SchedulerRepository.getInstance().remove(schedulerName);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean isShutdown() throws SchedulerException {
        try {
            return getRemoteScheduler().isShutdown();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException {
        try {
            return getRemoteScheduler().getCurrentlyExecutingJobs();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Scheduling-related Methods
    ///
    ///////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public Date scheduleJob(JobDetail jobDetail, Trigger trigger)
        throws SchedulerException {
        try {
            return getRemoteScheduler().scheduleJob(jobDetail,
                    trigger);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public Date scheduleJob(Trigger trigger) throws SchedulerException {
        try {
            return getRemoteScheduler().scheduleJob(trigger);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void addJob(JobDetail jobDetail, boolean replace)
        throws SchedulerException {
        try {
            getRemoteScheduler().addJob(jobDetail, replace);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    public void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling)
            throws SchedulerException {
        try {
            getRemoteScheduler().addJob(jobDetail, replace, storeNonDurableWhileAwaitingScheduling);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException {
        try {
            return getRemoteScheduler().deleteJobs(jobKeys);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws SchedulerException {
            try {
                getRemoteScheduler().scheduleJobs(triggersAndJobs, replace);
            } catch (Exception re) {
                throw invalidateHandleCreateException(
                        "Error communicating with remote scheduler.", re);
            }
    }
    
    public void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) throws SchedulerException {
        try {
            getRemoteScheduler().scheduleJob(jobDetail, triggersForJob, replace);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    public boolean unscheduleJobs(List<TriggerKey> triggerKeys)
            throws SchedulerException {
        try {
            return getRemoteScheduler().unscheduleJobs(triggerKeys);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean deleteJob(JobKey jobKey)
        throws SchedulerException {
        try {
            return getRemoteScheduler()
                    .deleteJob(jobKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean unscheduleJob(TriggerKey triggerKey)
        throws SchedulerException {
        try {
            return getRemoteScheduler().unscheduleJob(triggerKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public Date rescheduleJob(TriggerKey triggerKey,
            Trigger newTrigger) throws SchedulerException {
        try {
            return getRemoteScheduler().rescheduleJob(triggerKey,
                    newTrigger);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }
    
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void triggerJob(JobKey jobKey)
        throws SchedulerException {
        triggerJob(jobKey, null);
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void triggerJob(JobKey jobKey, JobDataMap data)
        throws SchedulerException {
        try {
            getRemoteScheduler().triggerJob(jobKey, data);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void pauseTrigger(TriggerKey triggerKey)
        throws SchedulerException {
        try {
            getRemoteScheduler()
                    .pauseTrigger(triggerKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        try {
            getRemoteScheduler().pauseTriggers(matcher);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void pauseJob(JobKey jobKey)
        throws SchedulerException {
        try {
            getRemoteScheduler().pauseJob(jobKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        try {
            getRemoteScheduler().pauseJobs(matcher);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void resumeTrigger(TriggerKey triggerKey)
        throws SchedulerException {
        try {
            getRemoteScheduler().resumeTrigger(triggerKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        try {
            getRemoteScheduler().resumeTriggers(matcher);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void resumeJob(JobKey jobKey)
        throws SchedulerException {
        try {
            getRemoteScheduler().resumeJob(jobKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException {
        try {
            getRemoteScheduler().resumeJobs(matcher);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void pauseAll() throws SchedulerException {
        try {
            getRemoteScheduler().pauseAll();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void resumeAll() throws SchedulerException {
        try {
            getRemoteScheduler().resumeAll();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public List<String> getJobGroupNames() throws SchedulerException {
        try {
            return getRemoteScheduler().getJobGroupNames();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException {
        try {
            return getRemoteScheduler().getJobKeys(matcher);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public List<? extends Trigger> getTriggersOfJob(JobKey jobKey)
        throws SchedulerException {
        try {
            return getRemoteScheduler().getTriggersOfJob(jobKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public List<String> getTriggerGroupNames() throws SchedulerException {
        try {
            return getRemoteScheduler().getTriggerGroupNames();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException {
        try {
            return getRemoteScheduler().getTriggerKeys(matcher);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public JobDetail getJobDetail(JobKey jobKey)
        throws SchedulerException {
        try {
            return getRemoteScheduler().getJobDetail(jobKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        try {
            return getRemoteScheduler().checkExists(jobKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }
   
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean checkExists(TriggerKey triggerKey) throws SchedulerException {
        try {
            return getRemoteScheduler().checkExists(triggerKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }
  
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void clear() throws SchedulerException {
        try {
            getRemoteScheduler().clear();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }
    
    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public Trigger getTrigger(TriggerKey triggerKey)
        throws SchedulerException {
        try {
            return getRemoteScheduler().getTrigger(triggerKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public TriggerState getTriggerState(TriggerKey triggerKey)
        throws SchedulerException {
        try {
            return getRemoteScheduler().getTriggerState(triggerKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void resetTriggerFromErrorState(TriggerKey triggerKey)
            throws SchedulerException {
        try {
            getRemoteScheduler().resetTriggerFromErrorState(triggerKey);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }




    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public void addCalendar(String calName, JobCalendar calendar, boolean replace, boolean updateTriggers)
        throws SchedulerException {
        try {
            getRemoteScheduler().addCalendar(calName, calendar,
                    replace, updateTriggers);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public boolean deleteCalendar(String calName) throws SchedulerException {
        try {
            return getRemoteScheduler().deleteCalendar(calName);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public JobCalendar getCalendar(String calName) throws SchedulerException {
        try {
            return getRemoteScheduler().getCalendar(calName);
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /**
     * <p>
     * Calls the equivalent method on the 'proxied' <code>QuartzScheduler</code>.
     * </p>
     */
    public List<String> getCalendarNames() throws SchedulerException {
        try {
            return getRemoteScheduler().getCalendarNames();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }

    /** 
     * @see third.quartz.Scheduler#getPausedTriggerGroups()
     */
    public Set<String> getPausedTriggerGroups() throws SchedulerException {
        try {
            return getRemoteScheduler().getPausedTriggerGroups();
        } catch (Exception re) {
            throw invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    ///
    /// Other Methods
    ///
    ///////////////////////////////////////////////////////////////////////////


    public ListenerManager getListenerManager() throws SchedulerException {
        throw new SchedulerException(
            "Operation not supported for remote schedulers.");
    }

    /**
     * @see third.quartz.Scheduler#interrupt(JobKey)
     */
    public boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException  {
        try {
            return getRemoteScheduler().interrupt(jobKey);
        } catch (SchedulerException se) {
            throw new UnableToInterruptJobException(se);
        } catch (Exception re) {
            throw new UnableToInterruptJobException(invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re));
        }
    }

    public boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException {
        try {
            return getRemoteScheduler().interrupt(fireInstanceId);
        } catch (SchedulerException se) {
            throw new UnableToInterruptJobException(se);
        } catch (Exception re) {
            throw new UnableToInterruptJobException(invalidateHandleCreateException(
                    "Error communicating with remote scheduler.", re));
        }
    }

    /**
     * @see third.quartz.Scheduler#setJobFactory(third.quartz.spi.JobFactory)
     */
    public void setJobFactory(JobFactory factory) throws SchedulerException {
        throw new SchedulerException(
                "Operation not supported for remote schedulers.");
    }

}
