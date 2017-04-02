package hc.server.msb;

/**
 * The listener for receiving Robot events.
 * @see Robot#addRobotListener(RobotListener)
 * @see Robot#removeRobotListener(RobotListener)
 */
public abstract class RobotListener {
	public RobotListener(){
	}
	
	/**
	 * Invoked when an action occurs.
	 * @param event
	 * @see Robot#dispatchRobotEvent(RobotEvent)
	 * @see Robot#buildRobotEvent(String, Object, Object)
	 */
	public abstract void action(final RobotEvent event);
}
