package hc.server.msb;

import java.io.Serializable;

public class DataDeviceCapDesc implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public final String desc, capList, ver;

	public DataDeviceCapDesc(final String desc, final String capList, final String ver) {
		this.desc = (desc == null ? "" : desc);
		this.capList = (capList == null ? "" : capList);
		this.ver = (ver == null ? "1.0" : ver);
	}
}
