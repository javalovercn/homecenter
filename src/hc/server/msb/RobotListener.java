package hc.server.msb;

/**
 * @see Robot#addRobotListener(RobotListener)
 * @see Robot#removeRobotListener(RobotListener)
 */
public abstract class RobotListener {
	public RobotListener(){
	}
	
	/**
	 * 
	 * @param event
	 * @see Robot#dispatchRobotEvent(RobotEvent)
	 * @see Robot#buildRobotEvent(String, Object, Object)
	 */
	public abstract void action(final RobotEvent event);
}
