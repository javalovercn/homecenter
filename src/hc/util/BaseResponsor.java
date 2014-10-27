package hc.util;

import hc.core.util.IHCURLAction;
import hc.server.ui.ProjectContext;

public abstract class BaseResponsor implements IBiz, IHCURLAction{

	public static final String[] EVENT_LIST = {
		ProjectContext.EVENT_SYS_PROJ_STARTUP,
		ProjectContext.EVENT_SYS_MOBILE_LOGIN, ProjectContext.EVENT_SYS_MOBILE_LOGOUT,
		ProjectContext.EVENT_SYS_PROJ_SHUTDOWN};

	public abstract void stop();
	
	/**
	 * @param contextName
	 */
	public abstract void enterContext(String contextName);
	
	public abstract Object onEvent(Object event);
	
	public abstract void addProjectContext(ProjectContext pc);
}
