package hc.server.msb;

import hc.core.util.Stack;

final class RobotEventPool {
	public static final RobotEventPool instance = new RobotEventPool();
	
	private final Stack vector = new Stack(32);
	
	public final RobotEvent getFreeRobotEvent(){
		Object out;
		synchronized (vector) {
			out = vector.pop();
		}
		
		if(out == null){
			out = new RobotEvent();
		}

		return (RobotEvent)out;
	}
	
	public final void recycle(final RobotEvent event){
		event.clear();
		
		synchronized (vector) {
			vector.push(event);
		}
	}
	
}