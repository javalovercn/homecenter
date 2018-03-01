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
import java.util.Date;
import java.util.TimeZone;

/**
 * <p>
 * This implementation of the JobCalendar may be used (you don't have to) as a
 * base class for more sophisticated one's. It merely implements the base
 * functionality required by each JobCalendar.
 * </p>
 *
 * <p>
 * Regarded as base functionality is the treatment of base calendars. Base
 * calendar allow you to chain (stack) as much calendars as you may need. For
 * example to exclude weekends you may use WeeklyJobCalendar. In order to
 * exclude holidays as well you may define a WeeklyJobCalendar instance to be
 * the base calendar for HolidayJobCalendar instance.
 * </p>
 *
 * @see hc.server.util.calendar.JobCalendar
 *
 * @author Juergen Donnerstag
 * @author James House
 */
public class BaseJobCalendar implements JobCalendar, Serializable, Cloneable {

	static final long serialVersionUID = 3106623404629760239L;

	// <p>A optional base calendar.</p>
	private JobCalendar baseCalendar;

	private String description;

	private TimeZone timeZone;

	public BaseJobCalendar() {
	}

	public BaseJobCalendar(final JobCalendar baseCalendar) {
		setBaseJobCalendar(baseCalendar);
	}

	/**
	 * @param timeZone
	 *            The time zone to use for this JobCalendar, <code>null</code>
	 *            if <code>{@link TimeZone#getDefault()}</code> should be used
	 */
	public BaseJobCalendar(final TimeZone timeZone) {
		setTimeZone(timeZone);
	}

	/**
	 * @param timeZone
	 *            The time zone to use for this JobCalendar, <code>null</code>
	 *            if <code>{@link TimeZone#getDefault()}</code> should be used
	 */
	public BaseJobCalendar(final JobCalendar baseCalendar, final TimeZone timeZone) {
		setBaseJobCalendar(baseCalendar);
		setTimeZone(timeZone);
	}

	@Override
	public Object clone() {
		try {
			final BaseJobCalendar clone = (BaseJobCalendar) super.clone();
			if (getBaseJobCalendar() != null) {
				clone.baseCalendar = (JobCalendar) getBaseJobCalendar().clone();
			}
			if (getTimeZone() != null)
				clone.timeZone = (TimeZone) getTimeZone().clone();
			return clone;
		} catch (final CloneNotSupportedException ex) {
			throw new IncompatibleClassChangeError("Not Cloneable.");
		}
	}

	/**
	 * <p>
	 * Set a new base calendar or remove the existing one
	 * </p>
	 */
	@Override
	public void setBaseJobCalendar(final JobCalendar baseCalendar) {
		this.baseCalendar = baseCalendar;
	}

	/**
	 * <p>
	 * Get the base calendar. Will be null, if not set.
	 * </p>
	 */
	@Override
	public JobCalendar getBaseJobCalendar() {
		return this.baseCalendar;
	}

	/**
	 * <p>
	 * Return the description given to the <code>JobCalendar</code> instance by
	 * its creator (if any).
	 * </p>
	 *
	 * @return null if no description was set.
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * <p>
	 * Set a description for the <code>JobCalendar</code> instance - may be
	 * useful for remembering/displaying the purpose of the calendar, though the
	 * description has no meaning to Quartz.
	 * </p>
	 */
	@Override
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * Returns the time zone for which this <code>JobCalendar</code> will be
	 * resolved.
	 *
	 * @return This JobCalendar's timezone, <code>null</code> if JobCalendar
	 *         should use the <code>{@link TimeZone#getDefault()}</code>
	 */
	public TimeZone getTimeZone() {
		return timeZone;
	}

	/**
	 * Sets the time zone for which this <code>JobCalendar</code> will be
	 * resolved.
	 *
	 * @param timeZone
	 *            The time zone to use for this JobCalendar, <code>null</code>
	 *            if <code>{@link TimeZone#getDefault()}</code> should be used
	 */
	public void setTimeZone(final TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * <p>
	 * Check if date/time represented by timeStamp is included. If included
	 * return true. The implementation of BaseJobCalendar simply calls the base
	 * calendars isTimeIncluded() method if base calendar is set.
	 * </p>
	 *
	 * @see hc.server.util.calendar.JobCalendar#isTimeIncluded(long)
	 */
	@Override
	public boolean isTimeIncluded(final long timeStamp) {

		if (timeStamp <= 0) {
			throw new IllegalArgumentException("timeStamp must be greater 0");
		}

		if (baseCalendar != null) {
			if (baseCalendar.isTimeIncluded(timeStamp) == false) {
				return false;
			}
		}

		return true;
	}

	/**
	 * <p>
	 * Determine the next time (in milliseconds) that is 'included' by the
	 * JobCalendar after the given time. Return the original value if timeStamp
	 * is included. Return 0 if all days are excluded.
	 * </p>
	 *
	 * @see hc.server.util.calendar.JobCalendar#getNextIncludedTime(long)
	 */
	@Override
	public long getNextIncludedTime(final long timeStamp) {

		if (timeStamp <= 0) {
			throw new IllegalArgumentException("timeStamp must be greater 0");
		}

		if (baseCalendar != null) {
			return baseCalendar.getNextIncludedTime(timeStamp);
		}

		return timeStamp;
	}

	/**
	 * Build a <code>{@link java.util.Calendar}</code> for the given timeStamp.
	 * The new JobCalendar will use the <code>BaseJobCalendar</code> time zone
	 * if it is not <code>null</code>.
	 */
	protected java.util.Calendar createJavaCalendar(final long timeStamp) {
		final java.util.Calendar calendar = createJavaCalendar();
		calendar.setTime(new Date(timeStamp));
		return calendar;
	}

	/**
	 * Build a <code>{@link java.util.Calendar}</code> with the current time.
	 * The new JobCalendar will use the <code>BaseJobCalendar</code> time zone
	 * if it is not <code>null</code>.
	 */
	protected java.util.Calendar createJavaCalendar() {
		return (getTimeZone() == null) ? java.util.Calendar.getInstance()
				: java.util.Calendar.getInstance(getTimeZone());
	}

	/**
	 * Returns the start of the given day as a
	 * <code>{@link java.util.Calendar}</code>. This calculation will take the
	 * <code>BaseJobCalendar</code> time zone into account if it is not
	 * <code>null</code>.
	 *
	 * @param timeInMillis
	 *            A time containing the desired date for the start-of-day time
	 * @return A <code>{@link java.util.Calendar}</code> set to the start of the
	 *         given day.
	 */
	protected java.util.Calendar getStartOfDayJavaCalendar(final long timeInMillis) {
		final java.util.Calendar startOfDay = createJavaCalendar(timeInMillis);
		startOfDay.set(java.util.Calendar.HOUR_OF_DAY, 0);
		startOfDay.set(java.util.Calendar.MINUTE, 0);
		startOfDay.set(java.util.Calendar.SECOND, 0);
		startOfDay.set(java.util.Calendar.MILLISECOND, 0);
		return startOfDay;
	}

	/**
	 * Returns the end of the given day <code>{@link java.util.Calendar}</code>.
	 * This calculation will take the <code>BaseJobCalendar</code> time zone
	 * into account if it is not <code>null</code>.
	 *
	 * @param timeInMillis
	 *            a time containing the desired date for the end-of-day time.
	 * @return A <code>{@link java.util.Calendar}</code> set to the end of the
	 *         given day.
	 */
	protected java.util.Calendar getEndOfDayJavaCalendar(final long timeInMillis) {
		final java.util.Calendar endOfDay = createJavaCalendar(timeInMillis);
		endOfDay.set(java.util.Calendar.HOUR_OF_DAY, 23);
		endOfDay.set(java.util.Calendar.MINUTE, 59);
		endOfDay.set(java.util.Calendar.SECOND, 59);
		endOfDay.set(java.util.Calendar.MILLISECOND, 999);
		return endOfDay;
	}
}
