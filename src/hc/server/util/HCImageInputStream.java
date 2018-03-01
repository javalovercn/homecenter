package hc.server.util;

import java.io.InputStream;

public final class HCImageInputStream extends HCFileInputStream {
	private final float azimuth, pitch, roll;
	private final String format;
	private final double latitude;
	private final double longitude;
	private final double altitude;

	HCImageInputStream(final InputStream is, final float azimuth, final float pitch,
			final float roll, final String format, final double latitude, final double longitude,
			final double altitude) {
		super(is, format, nullFileName);
		this.azimuth = azimuth;
		this.pitch = pitch;
		this.roll = roll;
		this.format = format;
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
	}

	public final String getImageFormat() {
		return format;
	}

	// /**
	// * rotation around the Z axis (0<=azimuth<360). 0 = North, 90 = East, 180
	// = South, 270 = West
	// * @return
	// */
	// public final int getImageAzimuthDegree(){
	// return azimuth;
	// }

	// /**
	// * rotation around X axis (-180<=pitch<=180), with positive values when
	// the z-axis moves toward the y-axis.
	// * @return
	// */
	// public final int getImagePitchDegree(){
	// return pitch;
	// }

	// /**
	// * rotation around Y axis (-90<=roll<=90), with positive values when the
	// z-axis moves toward the x-axis.
	// * @return
	// */
	// public final int getImageRollDegree(){
	// return roll;
	// }

	/**
	 * get the latitude of taking photograph.<BR>
	 * 
	 * @return if location is not required or no GPS signal, then return -1.0
	 * @see #isWithoutLocation()
	 * @see #getImageLongitude()
	 * @see #getImageAltitude()
	 */
	public final double getImageLatitude() {
		return latitude;
	}

	/**
	 * true means location is not required or no GPS signal.<BR>
	 * 
	 * @return
	 * @see #getImageLatitude()
	 * @see #getImageLongitude()
	 * @see #getImageAltitude()
	 */
	public final boolean isWithoutLocation() {
		return latitude == -1.0;
	}

	/**
	 * get the longitude of taking photograph.<BR>
	 * 
	 * @return if location is not required or no GPS signal, then return -1.0
	 * @see #isWithoutLocation()
	 * @see #getImageLatitude()
	 * @see #getImageAltitude()
	 */
	public final double getImageLongitude() {
		return longitude;
	}

	/**
	 * get the altitude of taking photograph.<BR>
	 * 
	 * @return if location is not required or no GPS signal, then return -1.0
	 * @see #isWithoutLocation()
	 * @see #getImageLongitude()
	 * @see #getImageLatitude()
	 */
	public final double getImageAltitude() {
		return altitude;
	}
}
