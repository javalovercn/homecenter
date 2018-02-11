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

package hc.server.util.calendar;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;


/**
 * <p>
 * This implementation of the JobCalendar stores a list of holidays (full days
 * that are excluded from scheduling).
 * </p>
 * 
 * <p>
 * The implementation DOES take the year into consideration, so if you want to
 * exclude July 4th for the next 10 years, you need to add 10 entries to the
 * exclude list.
 * </p>
 * 
 * @author Sharada Jambula
 * @author Juergen Donnerstag
 */
public class HolidayJobCalendar extends BaseJobCalendar implements JobCalendar,
        Serializable {
    static final long serialVersionUID = -7590908752291814693L;
    
    // A sorted set to store the holidays
    private TreeSet<Date> dates = new TreeSet<Date>();

    public HolidayJobCalendar() {
    }

    public HolidayJobCalendar(final JobCalendar baseCalendar) {
        super(baseCalendar);
    }

    public HolidayJobCalendar(final TimeZone timeZone) {
        super(timeZone);
    }

    public HolidayJobCalendar(final JobCalendar baseCalendar, final TimeZone timeZone) {
        super(baseCalendar, timeZone);
    }

    @Override
    public Object clone() {
        final HolidayJobCalendar clone = (HolidayJobCalendar) super.clone();
        clone.dates = new TreeSet<Date>(dates);
        return clone;
    }
    
    /**
     * <p>
     * Determine whether the given time (in milliseconds) is 'included' by the
     * JobCalendar.
     * </p>
     * 
     * <p>
     * Note that this JobCalendar is only has full-day precision.
     * </p>
     */
    @Override
    public boolean isTimeIncluded(final long timeStamp) {
        if (super.isTimeIncluded(timeStamp) == false) {
            return false;
        }

        final Date lookFor = getStartOfDayJavaCalendar(timeStamp).getTime();

        return !(dates.contains(lookFor));
    }

    /**
     * <p>
     * Determine the next time (in milliseconds) that is 'included' by the
     * JobCalendar after the given time.
     * </p>
     * 
     * <p>
     * Note that this JobCalendar is only has full-day precision.
     * </p>
     */
    @Override
    public long getNextIncludedTime(long timeStamp) {

        // Call base calendar implementation first
        final long baseTime = super.getNextIncludedTime(timeStamp);
        if ((baseTime > 0) && (baseTime > timeStamp)) {
            timeStamp = baseTime;
        }

        // Get timestamp for 00:00:00
        final java.util.Calendar day = getStartOfDayJavaCalendar(timeStamp);
        while (isTimeIncluded(day.getTime().getTime()) == false) {
            day.add(java.util.Calendar.DATE, 1);
        }

        return day.getTime().getTime();
    }

    /**
     * <p>
     * Add the given date (java.util.Date) to the list of excluded days. Only the month, day and
     * year of the returned dates are significant.
     * </p>
     */
    public void addExcludedDate(final Date excludedDate) {
        final Date date = getStartOfDayJavaCalendar(excludedDate.getTime()).getTime();
        /*
         * System.err.println( "HolidayJobCalendar.add(): date=" +
         * excludedDate.toLocaleString());
         */
        this.dates.add(date);
    }

    /**
     * Remove the given date (java.util.Date) from the list of excluded days.
     * @param dateToRemove
     */
    public void removeExcludedDate(final Date dateToRemove) {
        final Date date = getStartOfDayJavaCalendar(dateToRemove.getTime()).getTime();
        dates.remove(date);
    }

    /**
     * <p>
     * Returns a <code>SortedSet</code> of dates (java.util.Date) representing the excluded
     * days. Only the month, day and year of the returned dates are
     * significant.
     * </p>
     */
    public SortedSet<Date> getExcludedDates() {
        return Collections.unmodifiableSortedSet(dates);
    }
}
