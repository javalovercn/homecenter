package hc.server.msb;

import hc.core.util.StringUtil;

public class DeviceBindInfo {
	private static final String SPLIT = "/";

	public final String projID;
	public final String robotName;

	/**
	 * 注意：被Robot和deviceTree两类共用
	 * 
	 * @param projID
	 * @param robotName
	 */
	public DeviceBindInfo(final String projID, final String robotName) {
		this.projID = projID;
		this.robotName = robotName;
	}

	public String ref_dev_id = "";
	public String bind_id = "";

	public static String buildStandardBindID(final String projID, final String robotName,
			final String ref_devID) {
		return projID + SPLIT + robotName + SPLIT + ref_devID;
	}

	public static RobotReferBindInfo decodeReferIDFromBindID(final String bind_id) {
		final RobotReferBindInfo rrbi = new RobotReferBindInfo();
		final String[] out = StringUtil.splitToArray(bind_id, SPLIT);
		rrbi.proj_id = out[0];
		rrbi.robot_name = out[1];
		rrbi.refer_id = out[2];

		return rrbi;
	}
}
