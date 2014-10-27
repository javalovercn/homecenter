package hc.server.util;

import hc.server.ui.ProjectContext;

/**
 * 
 * @see ProjectContext#addSystemEventListener(SystemEventListener)
 */
public abstract class SystemEventListener {
	/**
	 * 
	 * @param eventName these following events are not all, new event may be added in the future, so <code>else</code> is potential bug.<br>{@link ProjectContext#EVENT_SYS_PROJ_STARTUP}, {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}, 
	 * {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}, {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN} or other events.
	 */
	public abstract void onEvent(final String eventName);
}
