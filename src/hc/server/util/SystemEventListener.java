package hc.server.util;

import hc.server.ui.ProjectContext;

/**
 * 
 * @see ProjectContext#addSystemEventListener(SystemEventListener)
 */
public abstract class SystemEventListener {
	/**
	 * if there is a long time task in it, it will NOT block the main thread (HomeCenter server thread) to shutdown/restartUp.<br>
	 * to get the interval time between shutdown and startup in milliseconds, see {@link ProjectContext#getIntervalMSForRestart()}.<br>
	 * <br>if the HAR project need more time to shutdown, please set a bigger value in option pane.<br>
	 * if you want reduce the time between shutdown and startup, please set a smaller value. <BR>
	 * <BR><STRONG>Note :</STRONG> it will raise unknown error when the <STRONG>startup</STRONG> process start new connection, but the same device is NOT shutdown yet.
	 * @param eventName these following events are not all, new event may be added in the future, so <code>else</code> is potential bug.
	 * <br>
	 * <ul><li>{@link ProjectContext#EVENT_SYS_PROJ_STARTUP}, </li><li>{@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}, </li>
	 * <li>{@link ProjectContext#EVENT_SYS_MOBILE_BACKGROUND_OR_FOREGROUND}, </li><li>{@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}, </li><li>{@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}, </li><li> or other events.</li></ul>
	 */
	public abstract void onEvent(final String eventName);
	
}
