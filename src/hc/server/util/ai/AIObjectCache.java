package hc.server.util.ai;

import hc.util.ObjectCache;

public class AIObjectCache {
	private static final ObjectCache<FormData> formDataCache = new ObjectCache<FormData>(FormData.class);
	private static final ObjectCache<RobotData> robotDataCache = new ObjectCache<RobotData>(RobotData.class);
	private static final ObjectCache<RobotEventData> robotEventDataCache = new ObjectCache<RobotEventData>(RobotEventData.class);

	public static final FormData getFormData() {
		return formDataCache.getFree();
	}

	public static final void cycleFormData(final FormData data) {
		formDataCache.cycle(data);
	}

	public static final RobotData getRobotData() {
		return robotDataCache.getFree();
	}

	public static final void cycleRobotData(final RobotData data) {
		robotDataCache.cycle(data);
	}

	public static final RobotEventData getRobotEventData() {
		return robotEventDataCache.getFree();
	}

	public static final void cycleRobotEventData(final RobotEventData data) {
		robotEventDataCache.cycle(data);
	}
}
