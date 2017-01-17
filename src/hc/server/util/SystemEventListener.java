package hc.server.util;

import hc.server.ui.ProjectContext;

/**
 * 
 * @see ProjectContext#addSystemEventListener(SystemEventListener)
 */
public abstract class SystemEventListener {
	/**
	 * the procedure of {@link #onEvent(String)} is in session level if triggered by session events (for example EVENT_SYS_MOBILE_LOGIN), even if it is added in project level.
	 * <BR><BR>
	 * if there is a long time task in it, it will NOT block the server thread.<br><BR>
	 * to get the interval time between shutdown and startup in milliseconds, see {@link ProjectContext#getIntervalMSForRestart()}.<br>
	 * if the HAR project need more time to shutdown, please set a bigger value in option pane.<br>
	 * <BR><STRONG>Note :</STRONG> it will raise unknown error when the <STRONG>startup</STRONG> process start new connection, but the same device is NOT shutdown yet.
	 * @param eventName one of the following, but not all, so <code>else</code> is potential bug.
	 * <br>
	 * {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}, {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}, 
	 * {@link ProjectContext#EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND}, {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}, 
	 * {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}.
	 */
	public abstract void onEvent(final String eventName);
	
}
