
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

package third.quartz.core;

import hc.server.util.scheduler.JobCalendar;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import third.quartz.JobDataMap;
import third.quartz.JobDetail;
import third.quartz.JobExecutionContext;
import third.quartz.JobKey;
import third.quartz.SchedulerContext;
import third.quartz.SchedulerException;
import third.quartz.Trigger;
import third.quartz.TriggerKey;
import third.quartz.UnableToInterruptJobException;
import third.quartz.Trigger.TriggerState;
import third.quartz.impl.matchers.GroupMatcher;
import third.quartz.spi.OperableTrigger;

/**
 * @author James House
 */
public interface RemotableQuartzScheduler {//注意：android环境抛出java.lang.NoClassDefFoundError: third.quartz.core.QuartzScheduler，因为不支持java.rmi.Remote，去掉extends Remote 

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    String getSchedulerName() throws Exception;

    String getSchedulerInstanceId() throws Exception;

    SchedulerContext getSchedulerContext() throws SchedulerException, Exception;

    void start() throws SchedulerException, Exception;

    void startDelayed(int seconds) throws SchedulerException, Exception;
    
    void standby() throws Exception;

    boolean isInStandbyMode() throws Exception;

    void shutdown() throws Exception;

    void shutdown(boolean waitForJobsToComplete) throws Exception;

    boolean isShutdown() throws Exception;

    Date runningSince() throws Exception;

    String getVersion() throws Exception;

    int numJobsExecuted() throws Exception;

    Class<?> getJobStoreClass() throws Exception;

    boolean supportsPersistence() throws Exception;

    boolean isClustered() throws Exception;

    Class<?> getThreadPoolClass() throws Exception;

    int getThreadPoolSize() throws Exception;

    void clear() throws SchedulerException, Exception;
    
    List<JobExecutionContext> getCurrentlyExecutingJobs() throws SchedulerException, Exception;

    Date scheduleJob(JobDetail jobDetail, Trigger trigger) throws SchedulerException, Exception;

    Date scheduleJob(Trigger trigger) throws SchedulerException, Exception;

    void addJob(JobDetail jobDetail, boolean replace) throws SchedulerException, Exception;

    void addJob(JobDetail jobDetail, boolean replace, boolean storeNonDurableWhileAwaitingScheduling) throws SchedulerException, Exception;

    boolean deleteJob(JobKey jobKey) throws SchedulerException, Exception;

    boolean unscheduleJob(TriggerKey triggerKey) throws SchedulerException, Exception;

    Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException, Exception;
        
    void triggerJob(JobKey jobKey, JobDataMap data) throws SchedulerException, Exception;

    void triggerJob(OperableTrigger trig) throws SchedulerException, Exception;
    
    void pauseTrigger(TriggerKey triggerKey) throws SchedulerException, Exception;

    void pauseTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException, Exception;

    void pauseJob(JobKey jobKey) throws SchedulerException, Exception;

    void pauseJobs(GroupMatcher<JobKey> matcher) throws SchedulerException, Exception;

    void resumeTrigger(TriggerKey triggerKey) throws SchedulerException, Exception;

    void resumeTriggers(GroupMatcher<TriggerKey> matcher) throws SchedulerException, Exception;

    Set<String> getPausedTriggerGroups() throws SchedulerException, Exception;
    
    void resumeJob(JobKey jobKey) throws SchedulerException, Exception;

    void resumeJobs(GroupMatcher<JobKey> matcher) throws SchedulerException, Exception;

    void pauseAll() throws SchedulerException, Exception;

    void resumeAll() throws SchedulerException, Exception;

    List<String> getJobGroupNames() throws SchedulerException, Exception;

    Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) throws SchedulerException, Exception;

    List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException, Exception;

    List<String> getTriggerGroupNames() throws SchedulerException, Exception;

    Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) throws SchedulerException, Exception;

    JobDetail getJobDetail(JobKey jobKey) throws SchedulerException, Exception;

    Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException, Exception;

    TriggerState getTriggerState(TriggerKey triggerKey) throws SchedulerException, Exception;

    void resetTriggerFromErrorState(TriggerKey triggerKey) throws SchedulerException, Exception;

    void addCalendar(String calName, JobCalendar calendar, boolean replace, boolean updateTriggers) throws SchedulerException, Exception;

    boolean deleteCalendar(String calName) throws SchedulerException, Exception;

    JobCalendar getCalendar(String calName) throws SchedulerException, Exception;

    List<String> getCalendarNames() throws SchedulerException, Exception;

    boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException,Exception;

    boolean interrupt(String fireInstanceId) throws UnableToInterruptJobException,Exception;
    
    boolean checkExists(JobKey jobKey) throws SchedulerException,Exception; 
   
    boolean checkExists(TriggerKey triggerKey) throws SchedulerException,Exception;
 
    public boolean deleteJobs(List<JobKey> jobKeys) throws SchedulerException,Exception;

    public void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> triggersAndJobs, boolean replace) throws SchedulerException,Exception;

    public void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> triggersForJob, boolean replace) throws SchedulerException,Exception;

    public boolean unscheduleJobs(List<TriggerKey> triggerKeys) throws SchedulerException,Exception;
    
}
