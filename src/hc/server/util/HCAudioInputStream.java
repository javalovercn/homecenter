package hc.server.util;

import java.io.InputStream;

public final class HCAudioInputStream extends HCFileInputStream {
	private final String format;
	private final double latitude;
	private final double longitude;
	private final double altitude;

	HCAudioInputStream(final InputStream is, final String format, final double latitude, final double longitude, final double altitude) {
		super(is, format.toLowerCase(), nullFileName);
		this.format = format;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}

	public final String getAudioFormat() {
		return format;
	}

	/**
	 * get the latitude of recording audio.<BR>
	 * 
	 * @return if location is not required or no GPS signal, then return -1.0
	 * @see #isWithoutLocation()
	 * @see #getAudioLongitude()
	 * @see #getAudioAltitude()
	 */
	public final double getAudioLatitude() {
		return latitude;
	}

	/**
	 * true means location is not required or no GPS signal.<BR>
	 * 
	 * @return
	 * @see #getAudioLatitude()
	 * @see #getAudioLongitude()
	 * @see #getAudioAltitude()
	 */
	public final boolean isWithoutLocation() {
		return latitude == -1.0;
	}

	/**
	 * get the longitude of recording audio.<BR>
	 * 
	 * @return if location is not required or no GPS signal, then return -1.0
	 * @see #isWithoutLocation()
	 * @see #getAudioAltitude()
	 * @see #getAudioLatitude()
	 */
	public final double getAudioLongitude() {
		return longitude;
	}

	/**
	 * get the altitude of recording audio.<BR>
	 * 
	 * @return if location is not required or no GPS signal, then return -1.0
	 * @see #isWithoutLocation()
	 * @see #getAudioLongitude()
	 * @see #getAudioLatitude()
	 */
	public final double getAudioAltitude() {
		return altitude;
	}
}
