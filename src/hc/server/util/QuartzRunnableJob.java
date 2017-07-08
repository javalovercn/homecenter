package hc.server.util;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.util.scheduler.CronExcludeJobCalendar;
import hc.server.util.scheduler.JobCalendar;
import hc.util.StringBuilderCacher;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import third.quartz.Job;
import third.quartz.JobDataMap;
import third.quartz.JobDetail;
import third.quartz.JobExecutionContext;
import third.quartz.JobExecutionException;
import third.quartz.Trigger;
import third.quartz.impl.triggers.CronTriggerImpl;

public class QuartzRunnableJob implements Job {

	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final JobDetail jobDetail = context.getJobDetail();
		final JobDataMap map = jobDetail.getJobDataMap();
		printCronAndNextFireTime(context);
		L.V = L.WShop ? false : LogManager.log("begin QuartzRunnableJob...");
		getRunnable(map).run();
		L.V = L.WShop ? false : LogManager.log("end QuartzRunnableJob...");
	}
	
    static final Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	static final void printCronAndNextFireTime(final JobExecutionContext context){
		final Trigger trigger = context.getTrigger();
		if(trigger instanceof CronTriggerImpl){
			final CronTriggerImpl cronTriggerImpl = (CronTriggerImpl)trigger;
			final Date nextFireTime = cronTriggerImpl.getNextFireTime();
			if(nextFireTime != null){
				final StringBuilder sb = StringBuilderCacher.getFree();
				
				sb.append("[Quartz Job] ");
				
				sb.append(" trigger name : [");
				sb.append(trigger.getKey().getName());
				sb.append("], CronExpression : [");
				sb.append(cronTriggerImpl.getCronExpression());
				sb.append("], next fire time : ");
				sb.append(format.format(nextFireTime));
				
				final JobCalendar jobCal = context.getCalendar();
				if(jobCal instanceof CronExcludeJobCalendar){
					sb.append(CronExcludeJobCalendar.class.getSimpleName());
					sb.append(" CronExpression : [");
					final CronExcludeJobCalendar cronExclude = (CronExcludeJobCalendar)jobCal;
					sb.append(cronExclude.getCronExpression());
					sb.append("], nextExcludeTime : ");
					final long nextMS = cronExclude.getNextIncludedTime(System.currentTimeMillis());
					final Date date = new Date(nextMS);
				    sb.append(format.format(date));
				}

				final String msg = sb.toString();
				LogManager.log(msg);
				StringBuilderCacher.cycle(sb);
			}
		}
	}

	private static final String jobMapRunnable = "runnable";
	
	public static void setRunnable(final JobDataMap map, final Runnable runnable){
		map.put(jobMapRunnable, runnable);
	}
	
	public static Runnable getRunnable(final JobDataMap map){
		return (Runnable)map.get(jobMapRunnable);
	}
}
