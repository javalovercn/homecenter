package hc.server.msb;

import java.io.Serializable;

public class RealDeviceInfo implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	public String proj_id;
	public String dev_name;
	public String dev_id;
	public String[] compatibleItem;
	public DataDeviceCapDesc deviceCapDesc;

	@Override
	public String toString() {
		return (proj_id == null ? "" : proj_id) + "/" + (dev_name == null ? "" : dev_name) + "/" + (dev_id == null ? "" : dev_id);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof RealDeviceInfo) {
			final RealDeviceInfo targetRDI = (RealDeviceInfo) obj;
			try {
				return targetRDI.proj_id.equals(proj_id) && targetRDI.dev_name.equals(dev_name) && targetRDI.dev_id.equals(dev_id);
			} catch (final Throwable e) {
			}
		}
		return false;
	}
}
