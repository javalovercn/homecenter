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

import third.quartz.Scheduler;
import third.quartz.SchedulerException;
import third.quartz.core.JobRunShell;
import third.quartz.core.JobRunShellFactory;
import third.quartz.spi.TriggerFiredBundle;

/**
 * <p>
 * Responsible for creating the instances of <code>{@link third.quartz.core.JobRunShell}</code>
 * to be used within the <class>{@link third.quartz.core.QuartzScheduler}
 * </code> instance.
 * </p>
 * 
 * @author James House
 */
public class StdJobRunShellFactory implements JobRunShellFactory {
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private Scheduler scheduler;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Initialize the factory, providing a handle to the <code>Scheduler</code>
     * that should be made available within the <code>JobRunShell</code> and
     * the <code>JobExecutionContext</code> s within it.
     * </p>
     */
    public void initialize(Scheduler sched) {
        this.scheduler = sched;
    }

    /**
     * <p>
     * Called by the <class>{@link third.quartz.core.QuartzSchedulerThread}
     * </code> to obtain instances of <code>
     * {@link third.quartz.core.JobRunShell}</code>.
     * </p>
     */
    public JobRunShell createJobRunShell(TriggerFiredBundle bndle) throws SchedulerException {
        return new JobRunShell(scheduler, bndle);
    }
}
