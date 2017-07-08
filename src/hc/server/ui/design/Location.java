package hc.server.ui.design;

public class Location {
	public double latitude = J2SESession.NO_PERMISSION_LOC;
	public double longitude = J2SESession.NO_PERMISSION_LOC;
	public double altitude = J2SESession.NO_PERMISSION_LOC;//海拔
	public double course = J2SESession.NO_PERMISSION_LOC;//航向
	public double speed = J2SESession.NO_PERMISSION_LOC;//行走速度
	public boolean isGPS;
	public boolean isFresh;
	
	public Location(){
	}
	
	public Location(final double latitude, final double longitude,
			final double altitude, final double course, final double speed, final boolean isGPS,
			final boolean isFresh){
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.course = course;
		this.speed = speed;
		this.isGPS = isGPS;
		this.isFresh = isFresh;
	}
}
