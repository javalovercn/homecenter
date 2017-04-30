package hc.server.msb;

import hc.server.ui.ProjectContext;
import hc.server.util.ai.AIObjectCache;
import hc.server.util.ai.AIPersistentManager;
import hc.server.util.ai.AnalysableData;
import hc.server.util.ai.RobotData;
import hc.util.ResourceUtil;

public class RobotWrapper extends Robot {
	final Robot robot;
	
	public RobotWrapper(final Robot robot){
		this.robot = robot;
	}

	@Override
	public final Object operate(final long functionID, final Object parameter) {
		final Object out = robot.operate(functionID, parameter);

		if(AIPersistentManager.isEnableAnalyseFlow && AIPersistentManager.isEnableHCAI() 
				&& ResourceUtil.isAnalysableParameter(parameter)
				&& ResourceUtil.isAnalysableParameter(out)){
			final RobotData anaData = AIObjectCache.getRobotData();
			anaData.functionID = functionID;
			anaData.parameter = parameter;
			anaData.out = out;
			
			anaData.snap(robot.project_id, robot.getProjectContext().getClientLocale(), AnalysableData.DIRECT_IN);
			AIPersistentManager.processRobotData(anaData);
		}
		
		return out;
	}

	@Override
	public final String[] declareReferenceDeviceID() {
		return robot.declareReferenceDeviceID();
	}

	@Override
	public final DeviceCompatibleDescription getDeviceCompatibleDescription(final String referenceDeviceID) {
		return robot.getDeviceCompatibleDescription(referenceDeviceID);
	}

	@Override
	public final void response(final Message msg) {
		robot.response(msg);
	}

	@Override
	public final void startup() {
		robot.startup();
	}

	@Override
	public final void shutdown() {
		robot.shutdown();
	}

	@Override
	final protected Message getFreeMessage(final String ref_dev_id) {
		return robot.getFreeMessage(ref_dev_id);
	}
	
	@Override
	public final ProjectContext getProjectContext(){
		return robot.getProjectContext();
	}
	
	@Override
	protected final void dispatch(final Message msg, final boolean isInitiative){
		robot.dispatch(msg, isInitiative);
	}
	
	@Override
	final protected Message waitFor(final Message msg, final long timeout){
		return robot.waitFor(msg, timeout);
	}
	
	@Override
	public final String getIoTDesc(){
		return robot.getIoTDesc();
	}
	
	@Override
	public final void addRobotListener(final RobotListener listener){
		robot.addRobotListener(listener);
	}

	@Override
	public final boolean removeRobotListener(final RobotListener listener){
		return robot.removeRobotListener(listener);
	}

	@Override
	final protected RobotEvent buildRobotEvent(final String propertyName, final Object oldValue, final Object newValue){
		return robot.buildRobotEvent(propertyName, oldValue, newValue);
	}
	
	@Override
	final protected void dispatchRobotEvent(final RobotEvent event) {
		robot.dispatchRobotEvent(event);
	}
	
}
