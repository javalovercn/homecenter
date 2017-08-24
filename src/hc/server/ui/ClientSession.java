package hc.server.ui;

import hc.core.util.LogManager;
import hc.server.ui.design.HCPermissionConstant;
import hc.server.ui.design.J2SESession;
import hc.util.ThreadConfig;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * a {@link ClientSession} instance will be created before {@link ProjectContext#EVENT_SYS_MOBILE_LOGIN}, and will be released by server after {@link ProjectContext#EVENT_SYS_MOBILE_LOGOUT}.
 * <BR><BR>
 * a {@link ProjectContext} instance will be created before {@link ProjectContext#EVENT_SYS_PROJ_STARTUP}, and will be released after {@link ProjectContext#EVENT_SYS_PROJ_SHUTDOWN}.
 * <BR><BR>
 * invoke {@link ProjectContext#getClientSession()} to get this instance for current session.
 * <BR><BR>it is thread safe.
 * @since 7.7
 */
public class ClientSession {
	private final Hashtable<String, Object> attribute_map = new Hashtable<String, Object>();
	final J2SESession j2seCoreSSMaybeNull;
	final boolean hasLocationOfMobile;
	Thread waitThread;
	boolean isLogout;
	
	ClientSession(final J2SESession j2seCoreSS, final boolean hasLocationOfMobile){
		this.j2seCoreSSMaybeNull = j2seCoreSS;//在Simu下传入null
		if(j2seCoreSSMaybeNull != null){
			j2seCoreSSMaybeNull.clientSession = this;
		}
		this.hasLocationOfMobile = hasLocationOfMobile;
	}
	
	/**
	 * returns the last known latitude.<BR><BR>
	 * for location of mobile, please enable permission [<STRONG>Location of mobile</STRONG>] of project.
	 * <BR><BR>
	 * if no permission (project or mobile) or low GPS signal, then return -1.0;
	 * <BR><BR>
	 * to set location updates frequency, see {@link ProjectContext#setLocationUpdates(long)}.
	 * @return
	 * @see #getLocationLongitude()
	 * @see ProjectContext#EVENT_SYS_MOBILE_LOCATION
	 * @see ProjectContext#addSystemEventListener(hc.server.util.SystemEventListener)
	 * @see #getLocationAltitude()
	 * @see #getLocationCourse()
	 * @see #getLocationSpeed()
	 */
	public final double getLocationLatitude(){//纬度
		if(hasLocationOfMobile == false){
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}
		
		if(j2seCoreSSMaybeNull == null){
			return SimuMobile.MOBILE_LATITUDE;
		}
		return j2seCoreSSMaybeNull.location.latitude;
	}
	
//	public final HCInputStream browseFile(final String type){
//		return null;
//	}
	
//	/**
//	 * scan QR code on client and wait for result.
//	 * <BR><BR>
//	 * <STRONG>Important</STRONG> : <BR>
//	 * it will block current thread for result.<BR><BR>
//	 * mobile client will not display alert/question/dialog when scanning code.
//	 * @return null means canceled by user or other exception.
//	 * @since 7.72
//	 */
//	public final String scanQRCode(){
//		if(j2seCoreSSMaybeNull == null){
//			return SimuMobile.MOBILE_QR_RESULT;
//		}
//		
//		final Thread threadSnap = Thread.currentThread();
//		waitThread = threadSnap;//注意：不考虑多线程并发调用
//		
//		ServerUIAPIAgent.runInSysThread(new Runnable() {
//			@Override
//			public void run() {
//				HCURLUtil.sendCmd(j2seCoreSSMaybeNull, HCURL.DATA_CMD_SendPara, HCURL.DATA_PARA_SCAN_QR_CODE, "");
//			}
//		});
//		
//		synchronized (threadSnap) {
//			if(isLogout){
//				return null;
//			}
//			try {
//				threadSnap.wait();
//			} catch (final InterruptedException e) {
//			}
//		}
//		
//		final String result = (String)ThreadConfig.getValue(ThreadConfig.QR_RESULT, true);
//		if(HCURL.CANCEL_HC_CMD.equals(result)){
//			LogManager.log("user cancel scan QR code.");
//			return null;
//		}
//		
//		return result;
//	}
	
	final void notifyClientSessionWaitObjectShutdown(){
		isLogout = true;
		
		notifyClientSessionWaitObject(waitThread);
	}

	private final void notifyClientSessionWaitObject(final Thread threadSnap) {
		if(threadSnap != null){
			synchronized (threadSnap) {
				threadSnap.notify();
			}
//			threadSnap = null;//不注释可能会导致notifyQRCode出现null情形
		}
	}
	
	final void notifyQRCode(final String result){
		final Thread threadSnap = waitThread;
		if(threadSnap != null){
			ThreadConfig.putValue(threadSnap, ThreadConfig.QR_RESULT, result);
			
			notifyClientSessionWaitObject(threadSnap);
		}else{
			LogManager.errToLog("Error on notifyQRCode");
		}
	}
	
	/**
	 * true means GPS; false means otherwise, maybe WiFi.
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final boolean isLocationGPS(){
		if(hasLocationOfMobile == false){
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return false;
		}
		
		if(j2seCoreSSMaybeNull == null){
			return SimuMobile.MOBILE_GPS;
		}
		return j2seCoreSSMaybeNull.location.isGPS;
	}
	
	/**
	 * true means fresh; false means otherwise, maybe last known GPS.
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final boolean isLocationFresh(){
		if(hasLocationOfMobile == false){
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return false;
		}
		
		if(j2seCoreSSMaybeNull == null){
			return SimuMobile.MOBILE_FRESH;
		}
		return j2seCoreSSMaybeNull.location.isFresh;
	}
	
	/**
	 * returns the last known altitude (unit : meter).
	 * <BR>
	 * if no permission (project or mobile) or low GPS signal, then return -1.0;
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final double getLocationAltitude(){//海拔：米
		if(hasLocationOfMobile == false){
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}
		
		if(j2seCoreSSMaybeNull == null){
			return SimuMobile.MOBILE_ALTITUDE;
		}
		return j2seCoreSSMaybeNull.location.altitude;
	}
	
	/**
	 * returns the last known course.<BR>
	 * -1.0 means unknown (some Android return 0.0).<BR><BR>
	 * 0 means north<BR>
	 * 90 means east<BR>
	 * 180 means south<BR>
	 * 270 means west
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final double getLocationCourse(){//航向：0表示北 90东 180南 270西
		if(hasLocationOfMobile == false){
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}
		
		if(j2seCoreSSMaybeNull == null){
			return SimuMobile.MOBILE_COURSE;
		}
		return j2seCoreSSMaybeNull.location.course;
	}
	
	/**
	 * returns the last known speed (unit : meter/second).<BR>
	 * -1.0 means unknown (some Android return 0.0).<BR><BR>
	 * @return
	 * @see #getLocationLongitude()
	 */
	public final double getLocationSpeed(){//设备移动速度：米/秒
		if(hasLocationOfMobile == false){
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}
		
		if(j2seCoreSSMaybeNull == null){
			return SimuMobile.MOBILE_SPEED;
		}
		return j2seCoreSSMaybeNull.location.speed;
	}
	
	/**
	 * returns the last known longitude.<BR><BR>
	 * for location of mobile, please enable permission [<STRONG>Location of mobile</STRONG>] of project.
	 * <BR><BR>
	 * if no permission (project or mobile) or low GPS signal, then return -1.0;
	 * <BR><BR>
	 * to set location updates frequency, see {@link ProjectContext#setLocationUpdates(long)}.
	 * @return
	 * @see #getLocationLatitude()
	 * @see ProjectContext#EVENT_SYS_MOBILE_LOCATION
	 * @see ProjectContext#addSystemEventListener(hc.server.util.SystemEventListener)
	 * @see #getLocationAltitude()
	 * @see #getLocationCourse()
	 * @see #getLocationSpeed()
	 * @see #isLocationGPS()
	 * @see #isLocationFresh()
	 */
	public final double getLocationLongitude(){//经度
		if(hasLocationOfMobile == false){
			LogManager.err(HCPermissionConstant.NO_PERMISSION_OF_LOCATION_OF_MOBILE);
			return J2SESession.NO_PERMISSION_LOC;
		}
		
		if(j2seCoreSSMaybeNull == null){
			return SimuMobile.MOBILE_LONGITUDE;
		}
		return j2seCoreSSMaybeNull.location.longitude;
	}
	
	/**
	 * returns the attribute with the given name, or null if there is no
	 * attribute by that name. <BR>
	 * It is thread safe.
	 * 
	 * @param name
	 * @return the attribute with the <code>name</code>.
	 * @since 7.7
	 */
	public final Object getAttribute(final String name) {
		return attribute_map.get(name);
	}
	
	/**
	 * returns the attribute with the given name, or <code>defaultValue</code>
	 * if there is no attribute by that name.
	 * 
	 * @param name
	 * @param defaultValue
	 *            the default value for name.
	 * @return <code>defaultValue</code> if this map contains no attribute for
	 *         the name
	 * @since 7.7
	 */
	public final Object getAttribute(final String name, final Object defaultValue) {
		final Object value = attribute_map.get(name);
		if (value == null) {
			return defaultValue;
		} else {
			return value;
		}
	}
	
	/**
	 * set an object to a given attribute name. <BR><BR>it is thread safe.
	 * <BR><BR>to set attribute for project (NOT for session), see {@link ProjectContext#setAttribute(String, Object)}.
	 * @param name
	 *            the name of attribute.
	 * @param obj
	 *            the value of the attribute.
	 * @return the previous object of the specified name, or null if it did not have one
	 * @since 7.7
	 */
	public final Object setAttribute(final String name, final Object obj) {
		return attribute_map.put(name, obj);
	}
	
	/**
	 * removes the attribute with the given name.
	 * 
	 * @param name the name that needs to be removed
	 * @return the attribute to which the name had been mapped, or null if the name did not have a mapping
	 * @since 7.7
	 */
	public final Object removeAttribute(final String name) {
		return attribute_map.remove(name);
	}
	
	/**
	 * It is equals with {@link #removeAttribute(String)}
	 * 
	 * @param name the name that needs to be removed
	 * @return the attribute to which the name had been mapped, or null if the name did not have a mapping
	 * @since 7.7
	 */
	public final Object clearAttribute(final String name) {
		return removeAttribute(name);
	}
	
	/**
	 * returns an enumeration containing the attribute names available.
	 * 
	 * @return the enumeration of all attribute names.
	 * @since 7.7
	 */
	public final Enumeration getAttributeNames() {
		final HashSet<String> set = new HashSet<String>();
		synchronized (attribute_map) {
			final Enumeration<String> en = attribute_map.keys();
			try{
				while (en.hasMoreElements()) {
					final String item = en.nextElement();
					set.add(item);
				}
			}catch (final NoSuchElementException e) {
			}
		}

		final Iterator<String> setit = set.iterator();
		return new Enumeration() {
			@Override
			public boolean hasMoreElements() {
				return setit.hasNext();
			}

			@Override
			public Object nextElement() {
				return setit.next();
			}
		};
	}
	
	/**
	 * returns the number of attribute names.
	 * @return
	 * @since 7.7
	 */
	public final int getAttributeSize() {
		return attribute_map.size();
	}
	
	/**
     * tests if the specified name is a key in this session.
     * @param   name possible key
     * @return 
     * @throws  NullPointerException  if the name is null.
     * @since 7.7
     */
    public final boolean containsAttributeName(final Object name) {
        return attribute_map.containsKey(name);
    }
    
    /**
     * returns true if one or more names maps to this object.
     * @param object
     * @return 
     * @throws NullPointerException  if the value is null
     * @since 7.7
     */
    public final boolean containsAttributeObject(final Object object) {
        return attribute_map.contains(object);
    }
    
    /**
     * check if these is no names to values.
     *
     * @return  true if empty.
     * @since 7.7
     */
    public final boolean isAttributeEmpty(){
    	return attribute_map.isEmpty();
    }
}
