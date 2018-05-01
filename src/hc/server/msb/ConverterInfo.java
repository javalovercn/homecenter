package hc.server.msb;

import java.io.Serializable;

public class ConverterInfo implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String proj_id;
	public String name;
	public String[] upCompItems;
	public String[] downCompItems;
	public String converterInfoDesc;
	public int hashCodeForConverter;

	@Override
	public String toString() {
		return (proj_id == null ? "" : proj_id) + "/" + (name == null ? "" : name);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ConverterInfo) {
			final ConverterInfo target = (ConverterInfo) obj;
			try {
				return target.proj_id.equals(proj_id) && target.name.equals(name);
			} catch (final Throwable e) {
			}
		}
		return false;
	}
}
