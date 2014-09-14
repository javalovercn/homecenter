package hc.server.ui;


import java.util.HashMap;

public class ProjectContextManager {

	public static void setThreadObject(final ProjectContext obj){
		final long threadID = Thread.currentThread().getId();
		ProjectContextManager.threadMap.put(threadID, obj);
	}

	public static ProjectContext getThreadObject(){
		final long threadID = Thread.currentThread().getId();
		return (ProjectContext)ProjectContextManager.threadMap.get(threadID);
	}

	private final static HashMap<Long, ProjectContext> threadMap = new HashMap<Long, ProjectContext>();

}
