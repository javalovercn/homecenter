package hc.server.util.ai;

public class RobotData extends AnalysableData {
	public long functionID;
	public Object parameter;
	public Object out;

	@Override
	public final boolean isSameWithPre(final AnalysableData pre) {
		return false;
	}
}
