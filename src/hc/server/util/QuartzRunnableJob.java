package hc.server.util;

import third.quartz.JobDataMap;
import third.quartz.JobExecutionException;

public class QuartzRunnableJob extends QuartzJob {
	@Override
	public void executeByMap(final JobDataMap map) throws JobExecutionException {
		getRunnable(map).run();
	}
}
