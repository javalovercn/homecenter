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
 */
package third.quartz.listeners;

import hc.core.util.ILog;

import third.quartz.JobDetail;
import third.quartz.JobKey;
import third.quartz.SchedulerException;
import third.quartz.SchedulerListener;
import third.quartz.Trigger;
import third.quartz.TriggerKey;

/**
 * A helpful abstract base class for implementors of 
 * <code>{@link third.quartz.SchedulerListener}</code>.
 * 
 * <p>
 * The methods in this class are empty so you only need to override the  
 * subset for the <code>{@link third.quartz.SchedulerListener}</code> events
 * you care about.
 * </p>
 * 
 * @see third.quartz.SchedulerListener
 */
public abstract class SchedulerListenerSupport implements SchedulerListener {

    public void jobAdded(JobDetail jobDetail) {
    }

    public void jobDeleted(JobKey jobKey) {
    }

    public void jobPaused(JobKey jobKey) {
    }

    public void jobResumed(JobKey jobKey) {
    }

    public void jobScheduled(Trigger trigger) {
    }

    public void jobsPaused(String jobGroup) {
    }

    public void jobsResumed(String jobGroup) {
    }

    public void jobUnscheduled(TriggerKey triggerKey) {
    }

    public void schedulerError(String msg, SchedulerException cause) {
    }

    public void schedulerInStandbyMode() {
    }

    public void schedulerShutdown() {
    }

    public void schedulerShuttingdown() {
    }

    public void schedulerStarted() {
    }

    public void schedulerStarting() {
    }

    public void triggerFinalized(Trigger trigger) {
    }

    public void triggerPaused(TriggerKey triggerKey) {
    }

    public void triggerResumed(TriggerKey triggerKey) {
    }

    public void triggersPaused(String triggerGroup) {
    }

    public void triggersResumed(String triggerGroup) {
    }
    
    public void schedulingDataCleared() {
    }

}
