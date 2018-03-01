package hc.server.msb;

public class DataDeviceCapDesc {
	public final String desc, capList, ver;

	public DataDeviceCapDesc(final String desc, final String capList, final String ver) {
		this.desc = (desc == null ? "" : desc);
		this.capList = (capList == null ? "" : capList);
		this.ver = (ver == null ? "1.0" : ver);
	}
}
