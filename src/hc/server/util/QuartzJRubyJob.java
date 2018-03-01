package hc.server.util;

import hc.server.ui.ProjectContext;
import third.quartz.JobDataMap;
import third.quartz.JobExecutionException;

public class QuartzJRubyJob extends QuartzJob {
	@Override
	public void executeByMap(final JobDataMap map) throws JobExecutionException {
		ProjectContext.getProjectContext().eval(getJobScripts(map));
	}

}
