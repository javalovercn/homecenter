package hc.server.util;

import hc.core.HCTimer;
import hc.core.L;
import hc.core.util.LogManager;
import hc.server.util.calendar.CronExcludeJobCalendar;
import hc.server.util.calendar.JobCalendar;
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

public abstract class QuartzJob implements Job {
	public abstract void executeByMap(final JobDataMap map) throws JobExecutionException;

	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final JobDetail jobDetail = context.getJobDetail();
		final JobDataMap map = jobDetail.getJobDataMap();
		printCronAndNextFireTime(context);

		final String projectID = map.getString(jobMapProjectID);
		final String schedulerID = map.getString(jobMapSchedulerID);
		final String jobKey = map.getString(jobMapJobName);

		L.V = L.WShop ? false : LogManager.log("begin QuartzJob [" + projectID + "/" + schedulerID + "/" + jobKey + "]");// projectID可以为null
		final HCTimer warn = ServerAPIAgent.addQuartzLongTimeWarn(projectID, schedulerID, jobKey);
		try {
			executeByMap(map);
		} catch (final Throwable ex) {
			throw new JobExecutionException(ex);
		} finally {
			warn.remove();
			L.V = L.WShop ? false : LogManager.log("end QuartzJob [" + projectID + "/" + schedulerID + "/" + jobKey + "].");
		}
	}

	static final Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	static final void printCronAndNextFireTime(final JobExecutionContext context) {
		final Trigger trigger = context.getTrigger();
		if (trigger instanceof CronTriggerImpl) {
			final CronTriggerImpl cronTriggerImpl = (CronTriggerImpl) trigger;
			final Date nextFireTime = cronTriggerImpl.getNextFireTime();
			if (nextFireTime != null) {
				final StringBuilder sb = StringBuilderCacher.getFree();

				sb.append("[Quartz Job] ");

				sb.append(" trigger name : [");
				sb.append(trigger.getKey().getName());
				sb.append("], CronExpression : [");
				sb.append(cronTriggerImpl.getCronExpression());
				sb.append("], next fire time : ");
				sb.append(format.format(nextFireTime));

				final JobCalendar jobCal = context.getCalendar();
				if (jobCal instanceof CronExcludeJobCalendar) {
					sb.append(CronExcludeJobCalendar.class.getSimpleName());
					sb.append(" CronExpression : [");
					final CronExcludeJobCalendar cronExclude = (CronExcludeJobCalendar) jobCal;
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

	private static final String jobMapScriptsKey = "scripts";
	private static final String jobMapRunnable = "runnable";
	private static final String jobMapProjectID = "projectID";
	private static final String jobMapSchedulerID = "schedulerID";
	private static final String jobMapJobName = "jobName";

	public static void setRunnable(final JobDataMap map, final Runnable runnable, final String projID, final String sche,
			final String job) {
		map.put(jobMapRunnable, runnable);
		setJobDetail(map, projID, sche, job);
	}

	static void setJobDetail(final JobDataMap map, final String projID, final String sche, final String job) {
		map.put(jobMapProjectID, projID);
		map.put(jobMapSchedulerID, sche);
		map.put(jobMapJobName, job);
	}

	static Runnable getRunnable(final JobDataMap map) {
		return (Runnable) map.get(jobMapRunnable);
	}

	public static void setJobScripts(final JobDataMap map, final String scripts, final String projID, final String sche, final String job) {
		map.put(jobMapScriptsKey, scripts);
		setJobDetail(map, projID, sche, job);
	}

	public static String getJobScripts(final JobDataMap map) {
		return map.getString(jobMapScriptsKey);
	}
}
