package hc.server.ui.design;

import hc.server.msb.Converter;
import hc.server.msb.DeviceCompatibleDescription;

public class ConverterAndExt {
	public final Converter converter;
	public DeviceCompatibleDescription upDeviceCompatibleDescriptionCache;
	public DeviceCompatibleDescription downDeviceCompatibleDescriptionCache;
	
	public ConverterAndExt(final Converter converter) {
		this.converter = converter;
	}
}
