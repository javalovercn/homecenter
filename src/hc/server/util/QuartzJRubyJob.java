package hc.server.util;

import hc.server.ui.ProjectContext;
import third.quartz.Job;
import third.quartz.JobDataMap;
import third.quartz.JobDetail;
import third.quartz.JobExecutionContext;
import third.quartz.JobExecutionException;

public class QuartzJRubyJob implements Job {

	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		final JobDetail jobDetail = context.getJobDetail();
		final JobDataMap map = jobDetail.getJobDataMap();
		ProjectContext.getProjectContext().eval(getJobScripts(map));
	}
	
	private static final String jobMapScriptsKey = "scripts";
	
	public static void setJobScripts(final JobDataMap map, final String scripts){
		map.put(jobMapScriptsKey, scripts);
	}
	
	public static String getJobScripts(final JobDataMap map){
		return map.getString(jobMapScriptsKey);
	}
	
}
