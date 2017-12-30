package hc.server.rms;

import hc.App;
import hc.core.HCTimer;
import hc.core.cache.CacheManager;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.util.PropertiesManager;
import hc.util.PropertiesMap;

import java.util.Iterator;
import java.util.Vector;

public class RMSLastAccessTimeManager {
	final static PropertiesMap map = new PropertiesMap(PropertiesManager.p_PROJ_CACHE_LAST_ACCESS_TIME);
	final static ThreadGroup token = App.getThreadPoolToken();
	
	public static void doNothing(){
		//init token
	}
	
	/**
	 * 有可能在ExceptionReporter.print中调用，故要入pool
	 */
	public static final void save(){
		synchronized (map) {//不能使用threadpool进行异步
			map.save();
		}
	}
	
	private static String KEY_SPLITTER = StringUtil.SPLIT_LEVEL_3_DOLLAR;
	
	private static final String buildKey(final String projectID, final String uuid){
		return projectID + KEY_SPLITTER + uuid;
	}
	
	private static final String[] splitKey(final String key){
		return StringUtil.splitToArray(key, KEY_SPLITTER);
	}
	
	/**
	 * 存储方式为[projectID|currentMS]
	 * @param projectID
	 * @param uuid
	 */
	public static final void notifyAccess(final String projectID, final String uuid){
		final String lastMS = String.valueOf(System.currentTimeMillis());
		final String key = buildKey(projectID, uuid);

		synchronized (map) {
			map.put(key, lastMS);
		}
	}
	
	public static final void checkIdleAndRemove(){
		CCoreUtil.checkAccess();
		
		final long currMS = System.currentTimeMillis();
		
		final Vector<String> removedKeys = new Vector<String>();
		
		synchronized (map) {
			final Iterator it = map.keySet().iterator();
			while(it.hasNext()){
				final String buildKey = (String)it.next();
				final long lastAccessMS = Long.valueOf(map.get(buildKey));
				
//				if((currMS - lastAccessMS) > 1000 * 60 * 60 * 8){//超过8小时
				if(((currMS - lastAccessMS) / HCTimer.ONE_DAY) > 90){//超过90天
					final String[] projIDUUID = splitKey(buildKey);
					final String projectID = projIDUUID[0];
					final String uuid = projIDUUID[1];
					
//					System.out.println("[" + projectID + "/" + uuid + "] last access : " + new Date(lastAccessMS).toLocaleString());
					LogManager.log("clear cache [" + projectID + "/" + uuid + "] for long idle.");
					
					removedKeys.add(buildKey);
					CacheManager.removeUIDFrom(projectID, uuid);
				}
			}
			
			final int removeSize = removedKeys.size();
			for (int i = removeSize - 1; i >= 0; i--) {
				map.remove(removedKeys.elementAt(i));
			}
		}
	}
	
}
