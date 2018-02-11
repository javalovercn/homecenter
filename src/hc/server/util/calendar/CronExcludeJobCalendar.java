package hc.server.util.calendar;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import third.quartz.CronExpression;

/**
 * This implementation of the JobCalendar excludes the set of times expressed by a
 * given cron expression. For example, you 
 * could use this calendar to exclude all but business hours (8AM - 5PM) every 
 * day using the expression &quot;* * 0-7,18-23 ? * *&quot;. 
 * <P>
 * It is important to remember that the cron expression here describes a set of
 * times to be <I>excluded</I> from firing. Whereas the cron expression in 
 * cron trigger describes a set of times that can
 * be <I>included</I> for firing. Thus, if a cron trigger has a 
 * given cron expression and is associated with a <CODE>CronExcludeJobCalendar</CODE> with
 * the <I>same</I> expression, the calendar will exclude all the times the 
 * trigger includes, and they will cancel each other out. 
 * 
 * @author Aaron Craven
 */
public class CronExcludeJobCalendar extends BaseJobCalendar {
    static final long serialVersionUID = -8172103999750856831L;

    CronExpression cronExpression;

    /**
     * Create a <CODE>CronExcludeJobCalendar</CODE> with the given cron expression and no
     * <CODE>baseCalendar</CODE>.
     *  
     * @param cronExpression for more about Cron Expression, please click <a href="http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html</a>
     */
    public CronExcludeJobCalendar(final String cronExpression) 
        throws ParseException {
        this(null, cronExpression, null);
    }

    /**
     * Create a <CODE>CronExcludeJobCalendar</CODE> with the given cron expression and 
     * <CODE>baseCalendar</CODE>. 
     * 
     * @param baseCalendar the base calendar for this calendar instance
     * @param cronExpression for more about Cron Expression, please click <a href="http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html</a>
     */
    public CronExcludeJobCalendar(final JobCalendar baseCalendar,
            final String cronExpression) throws ParseException {
        this(baseCalendar, cronExpression, null);
    }

    /**
     * Create a <CODE>CronExcludeJobCalendar</CODE> with the given cron exprssion, 
     * <CODE>baseCalendar</CODE>, and <code>TimeZone</code>. 
     * 
     * @param baseCalendar the base calendar for this calendar instance
     * @param cronExpression for more about Cron Expression, please click <a href="http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html">http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html</a>
     * @param timeZone
     *          Specifies for which time zone the <code>expression</code>
     *          should be interpreted, i.e. the expression 0 0 10 * * ?, is
     *          resolved to 10:00 am in this time zone.  If 
     *          <code>timeZone</code> is <code>null</code> then 
     *          <code>TimeZone.getDefault()</code> will be used.
     */
    public CronExcludeJobCalendar(final JobCalendar baseCalendar,
            final String cronExpression, final TimeZone timeZone) throws ParseException {
        super(baseCalendar);
        this.cronExpression = new CronExpression(cronExpression);
        this.cronExpression.setTimeZone(timeZone);
    }
    
    @Override
    public Object clone() {
        final CronExcludeJobCalendar clone = (CronExcludeJobCalendar) super.clone();
        clone.cronExpression = new CronExpression(cronExpression);
        return clone;
    }

    /**
     * Returns the time zone for which the <code>CronExpression</code> of
     * this <code>CronExcludeJobCalendar</code> will be resolved.
     * <p>
     */
    @Override
    public TimeZone getTimeZone() {
        return cronExpression.getTimeZone();
    }

    /**
     * Sets the time zone for which the <code>CronExpression</code> of this
     * <code>CronExcludeJobCalendar</code> will be resolved.  If <code>timeZone</code> 
     * is <code>null</code> then <code>TimeZone.getDefault()</code> will be 
     * used.
     * <p>
     */
    @Override
    public void setTimeZone(final TimeZone timeZone) {
        cronExpression.setTimeZone(timeZone);
    }
    
    /**
     * Determines whether the given time (in milliseconds) is 'included' by the
     * <CODE>BaseJobCalendar</CODE>
     * 
     * @param timeInMillis the date/time to test
     * @return a boolean indicating whether the specified time is 'included' by
     *         the <CODE>CronExcludeJobCalendar</CODE>
     */
    @Override
    public boolean isTimeIncluded(final long timeInMillis) {        
        if ((getBaseJobCalendar() != null) && 
                (getBaseJobCalendar().isTimeIncluded(timeInMillis) == false)) {
            return false;
        }
        
        return (!(cronExpression.isSatisfiedBy(new Date(timeInMillis))));
    }

    /**
     * Determines the next time included by the <CODE>CronExcludeJobCalendar</CODE>
     * after the specified time.
     * 
     * @param timeInMillis the initial date/time after which to find an 
     *                     included time
     * @return the time in milliseconds representing the next time included
     *         after the specified time.
     */
    @Override
    public long getNextIncludedTime(final long timeInMillis) {
        long nextIncludedTime = timeInMillis + 1; //plus on millisecond
        
        while (!isTimeIncluded(nextIncludedTime)) {

            //If the time is in a range excluded by this calendar, we can
            // move to the end of the excluded time range and continue testing
            // from there. Otherwise, if nextIncludedTime is excluded by the
            // baseCalendar, ask it the next time it includes and begin testing
            // from there. Failing this, add one millisecond and continue
            // testing.
            if (cronExpression.isSatisfiedBy(new Date(nextIncludedTime))) {
                nextIncludedTime = cronExpression.getNextInvalidTimeAfter(
                        new Date(nextIncludedTime)).getTime();
            } else if ((getBaseJobCalendar() != null) && 
                    (!getBaseJobCalendar().isTimeIncluded(nextIncludedTime))){
                nextIncludedTime = 
                    getBaseJobCalendar().getNextIncludedTime(nextIncludedTime);
            } else {
                nextIncludedTime++;
            }
        }
        
        return nextIncludedTime;
    }

    /**
     * Returns a string representing the properties of the 
     * <CODE>CronExcludeJobCalendar</CODE>
     * 
     * @return the properteis of the CronExcludeJobCalendar in a String format
     */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append("base calendar: [");
        if (getBaseJobCalendar() != null) {
            buffer.append(getBaseJobCalendar().toString());
        } else {
            buffer.append("null");
        }
        buffer.append("], excluded cron expression: '");
        buffer.append(cronExpression);
        buffer.append("'");
        return buffer.toString();
    }
    
    /**
     * Returns the object representation of the cron expression that defines the
     * dates and times this calendar excludes.
     * 
     * @return the cron expression
     */
    public String getCronExpression() {
    	if(cronExpression == null){
    		return null;
    	}
    	
        return cronExpression.getCronExpression();
    }
    
    /**
     * Sets the cron expression for the calendar to a new value
     * 
     * @param cronExpression the new string value to build a cron expression from
     * @throws Exception
     *         if the string expression cannot be parsed
     */
    public void setCronExpression(final String cronExpression) throws Exception {
        final CronExpression newExp = new CronExpression(cronExpression);
        
        this.cronExpression = newExp;
    }

}