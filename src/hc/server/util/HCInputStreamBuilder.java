package hc.server.util;

import hc.core.L;
import hc.core.util.LogManager;

import java.io.InputStream;

public class HCInputStreamBuilder {
	public static HCFileInputStream build(final InputStream is, final String fileExtension, final String fileName) {
		L.V = L.WShop ? false : LogManager.log("build stream (file), fileExtension : " + fileExtension + ", fileName : " + fileName);
		return new HCFileInputStream(is, fileExtension, fileName);
	}

	public static HCImageInputStream buildImageStream(final InputStream is, final float azimuth, final float pitch, final float roll,
			final String format, final double latitude, final double longitude, final double altitude) {
		L.V = L.WShop ? false
				: LogManager.log("build stream (image), " + "format : " + format + ", azimuth : " + azimuth + ", pitch : " + pitch
						+ ", roll : " + roll + ", latitude : " + latitude + ", longitude : " + longitude + ", altitude : " + altitude);
		return new HCImageInputStream(is, azimuth, pitch, roll, format, latitude, longitude, altitude);
	}

	public static HCAudioInputStream buildAudioStream(final InputStream is, final String format, final double latitude,
			final double longitude, final double altitude) {
		L.V = L.WShop ? false
				: LogManager.log("build stream (audio), format : " + format + ", latitude : " + latitude + ", longitude : " + longitude
						+ ", altitude : " + altitude);
		return new HCAudioInputStream(is, format, latitude, longitude, altitude);
	}
}
