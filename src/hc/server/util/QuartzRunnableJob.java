package hc.server.util;

import hc.core.L;
import hc.core.util.LogManager;
import third.quartz.Job;
import third.quartz.JobDataMap;
import third.quartz.JobDetail;
import third.quartz.JobExecutionContext;
import third.quartz.JobExecutionException;

public class QuartzRunnableJob implements Job {

	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final JobDetail jobDetail = context.getJobDetail();
		final JobDataMap map = jobDetail.getJobDataMap();
		L.V = L.WShop ? false : LogManager.log("begin QuartzRunnableJob...");
		getRunnable(map).run();
		L.V = L.WShop ? false : LogManager.log("end QuartzRunnableJob...");
	}

	private static final String jobMapRunnable = "runnable";
	
	public static void setRunnable(final JobDataMap map, final Runnable runnable){
		map.put(jobMapRunnable, runnable);
	}
	
	public static Runnable getRunnable(final JobDataMap map){
		return (Runnable)map.get(jobMapRunnable);
	}
}
