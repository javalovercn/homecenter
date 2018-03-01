package hc.server.util.ai;

import hc.server.msb.RobotWrapper;

public class RobotEventData extends AnalysableData {
	public RobotWrapper robotWrapper;
	public Object oldValue;
	public Object newValue;

	@Override
	public final boolean isSameWithPre(final AnalysableData pre) {
		return false;
	}
}
