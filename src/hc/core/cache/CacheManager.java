package hc.core.cache;

import hc.core.L;
import hc.core.util.LogManager;

import java.util.Hashtable;
import java.util.Vector;

public class CacheManager {
	private static final String _LIST = "_list";
	public static final int TYPE_CSS = 1;
	public static final int TYPE_JS = 2;
	
	public static final int CODE_LEN = 20;
	
	private static final String RS_PROJ_LIST = "cach_prj_list";
	private static final String RS_PRE_PROJ_UUID_LIST = "cach_uid_";
	private static final String RS_PRE_PROJ_URLID_LIST = "cach_url_";
	private static final String RS_PRE_URLID_FILECODE_LIST = "cach_fc_";
	private static final String RS_PRE_URLID_FILE_LIST = "cach_file_";
	
	private static final Hashtable cacheBuffer = new Hashtable();
	
	public static final void clearBuffer(){
		synchronized(cacheBuffer){
			cacheBuffer.clear();
		}
		L.V = L.O ? false : LogManager.log("[cache] clear buffer, not cache.");
	}
	
	/**
	 * 删除工程时，附带删除工程的cache文件
	 * @param projIDS 数组中间可能出现null
	 */
	public static void delProjects(String[] projIDS){
		Vector vector = getCacheVector(RS_PROJ_LIST);
		final int endIdx = vector.size();
		
		for (int i = 1; i < endIdx; i++) {
			DataItem item = (DataItem)vector.elementAt(i);
			final String itemProjID = item.toStringValue();
			
			for (int j = 0; j < projIDS.length; j++) {
				if(itemProjID.equals(projIDS[j])){//参数可为null
					DataItem.setDel(item);
					removeProjCacheAndRMS(itemProjID);
					break;
				}
			}
		}
		
		StoreManager.storeData(RS_PROJ_LIST, vector);
	}
	
	public static boolean isOverloadProjectTotalCacheNum(String projID){
		int total = 0;
		
		String rmsUUID = getRMSNameForUUID(projID);
		Vector uuid = getCacheVector(rmsUUID);
		final int endUUIDIdx = uuid.size();
		for (int uuidIdx = 1; uuidIdx < endUUIDIdx; uuidIdx++) {
			String uuidStr = ((DataItem)uuid.elementAt(uuidIdx)).toStringValue();
			
			Vector urlVector = getCacheVector(getRMSNameForURLID(projID, uuidStr));
			final int endIdx = urlVector.size();
			for (int i = 1; i < endIdx; i++) {
				DataItem itemUrlID = (DataItem)urlVector.elementAt(i);
				total += StoreManager.getRecordNumByStoreName(getRMSNameForFileCode(projID, uuidStr, itemUrlID.toStringValue()));
			}
		}
		
		return total > (endUUIDIdx - 1) * 20 * 3;
	}
	
	public static Vector checkAndDelCacheOverload(){
		Vector delProjVector = new Vector();
		
		Vector vector = getCacheVector(RS_PROJ_LIST);
		final int endIdx = vector.size();
		
		boolean isChanged = false;
		for (int i = 1; i < endIdx; i++) {
			DataItem item = (DataItem)vector.elementAt(i);
			final String itemProjID = item.toStringValue();
			if(itemProjID == null || itemProjID.length() == 0){//为删除记录状态
				continue;
			}
			if(isOverloadProjectTotalCacheNum(itemProjID)){//检查存储记录数是否过载
				DataItem.setDel(item);
				
				removeProjCacheAndRMS(itemProjID);
				delProjVector.addElement(itemProjID);
				isChanged = true;
			}
		}
		
		if(isChanged){
			StoreManager.storeData(RS_PROJ_LIST, vector);
		}
		
		return delProjVector;
	}
	
	/**
	 * 通知cache系统，最新的工程名称列表，以供删除不被使用的工程cache数据。
	 * @param projIDS
	 * @return 返回删除的工程标识
	 */
	public static Vector notifyServerProj(String[] projIDS){
		Vector delProjVector = new Vector();
		
		Vector vector = getCacheVector(RS_PROJ_LIST);
		final int endIdx = vector.size();
		
		boolean isChanged = false;
		for (int i = 1; i < endIdx; i++) {
			DataItem item = (DataItem)vector.elementAt(i);
			final String itemProjID = item.toStringValue();
			if(itemProjID == null || itemProjID.length() == 0){//为删除记录状态
				continue;
			}
			boolean matchID = false;
			for (int j = 0; j < projIDS.length; j++) {
				if(itemProjID.equals(projIDS[j])){
					matchID = true;
					break;
				}
			}
			//过载检查不在手机端做，因为有可能数据不同步。
			if(matchID == false){//注意：本过程只限于手机端，因为服务器端在工程deactive时，会删除缓存。
				DataItem.setDel(item);
				
				removeProjCacheAndRMS(itemProjID);
				delProjVector.addElement(itemProjID);
				isChanged = true;
			}
		}
		
		if(isChanged){
			StoreManager.storeData(RS_PROJ_LIST, vector);
		}
		
		return delProjVector;
	}
	
	private static String getRMSNameForUUID(String projID){
		return RS_PRE_PROJ_UUID_LIST + projID + _LIST;
	}
	
	private static String getRMSNameForURLID(String projID, String uuid){
		return RS_PRE_PROJ_URLID_LIST + projID + "_" + uuid + _LIST;//注意：由于ios的RMS命名规范是rmsName+recordNo，所以前缀名不能被包含。
	}
	
	private static String getRMSNameForFileCode(String projID, String uuid, String urlID){
		return RS_PRE_URLID_FILECODE_LIST + projID + "_" + uuid + "_" + urlID + _LIST;
	}
	
	private static String getRMSNameForFile(String projID, String uuid, String urlID){
		return RS_PRE_URLID_FILE_LIST + projID + "_" + uuid + "_" + urlID + _LIST;
	}
	
	/**
	 * 将指定工程库的全部信息，从cache系统中删除
	 * @param projID
	 */
	private static void removeProjCacheAndRMS(String projID){
		L.V = L.O ? false : LogManager.log("[cache] delete all cache of project [" + projID + "].");
		{
			String rmsUUID = getRMSNameForUUID(projID);
			Vector uuid = getCacheVector(rmsUUID);
			final int endUUIDIdx = uuid.size();
			for (int uuidIdx = 1; uuidIdx < endUUIDIdx; uuidIdx++) {
				String uuidStr = ((DataItem)uuid.elementAt(uuidIdx)).toStringValue();
				
				final String rmsURLID = getRMSNameForURLID(projID, uuidStr);
				
				Vector urlIDs = getCacheVector(rmsURLID);
				final int endIdx = urlIDs.size();
				for (int i = 1; i < endIdx; i++) {
					String urlID = ((DataItem)urlIDs.elementAt(i)).toStringValue();
					
					delCacheAndRMSForURL(projID, uuidStr, urlID);
				}
				
				delCacheAndRMS(rmsURLID);
			}
			
			delCacheAndRMS(rmsUUID);
		}
		
		//注意：不能先删除，因为有可能存储故障，而导致问题，可以下次再次进行删除
		{
			//将projID从库记录中删除
			Vector projs = getCacheVector(RS_PROJ_LIST);
			final int endIdx = projs.size();
			for (int i = 1; i < endIdx; i++) {
				final DataItem dataItem = (DataItem)projs.elementAt(i);
				String prjid = dataItem.toStringValue();
				if(prjid.equals(projID)){
					DataItem.setDel(dataItem);
					StoreManager.storeData(RS_PROJ_LIST, projs);
					
					break;
				}
			}
		}
		
		L.V = L.O ? false : LogManager.log("[cache] remove all cache for project : [" + projID + "].");
	}

	private static void delCacheAndRMSForURL(String projID, String uuid, String urlID) {
		final String rmsNameForFile = getRMSNameForFile(projID, uuid, urlID);
		final String rmsNameForFileCode = getRMSNameForFileCode(projID, uuid, urlID);

		delCacheAndRMS(rmsNameForFile);
		delCacheAndRMS(rmsNameForFileCode);
	}

	private static void delCacheAndRMS(final String rmsNameForFileCode) {
		StoreManager.removeRMS(rmsNameForFileCode);
		delCacheVector(rmsNameForFileCode);
	}
	
	private static Vector getCacheVector(String rmsName){
		Vector out = (Vector)cacheBuffer.get(rmsName);
		if(out == null){
			synchronized(cacheBuffer){
				out = StoreManager.getDataList(rmsName);
				cacheBuffer.put(rmsName, out);
			}
		}
		return out;
	}
	
	private static void delCacheVector(String rmsName){
		synchronized(cacheBuffer){
			cacheBuffer.remove(rmsName);
		}
	}
	
	public static Vector getCacheCoder(String projID, String uuid, String urlID){
		return getCacheVector(getRMSNameForFileCode(projID, uuid, urlID));
	}
	
	public static void setDelOnCacheCoderForStoreProblem(String projID, String uuid, Vector vectorCoder, String cacheXorCoder){
		final int endIdx = vectorCoder.size();
		for (int i = endIdx - 1; i >= 1; i--) {
			final DataItem item = (DataItem)vectorCoder.elementAt(i);
			if(item.toStringValue().equals(cacheXorCoder)){
				DataItem.setDel(item);
				StoreManager.storeData(getRMSNameForURLID(projID, uuid), vectorCoder);
				return;
			}
		}
	}
	
	/**
	 * @param projID
	 * @param uuid
	 * @param urlID
	 * @param codeBS
	 * @param codeOffset
	 * @param codeLen
	 * @return
	 */
	private static DataItem getCacheFileItem(final String projID, String uuid, String urlID, 
			final byte[] codeBS, final int codeOffset, final int codeLen){
		Vector vector = getCacheCoder(projID, uuid, urlID);
		final int endIdx = vector.size();
		for (int i = 1; i < endIdx; i++) {
			final DataItem item = (DataItem)vector.elementAt(i);
			if(item.isEmpty == false && item.equalBS(codeBS, codeOffset, codeLen)){
				Vector vectorFile = getCacheVector(getRMSNameForFile(projID, uuid, urlID));
				return (DataItem)vectorFile.elementAt(i);
			}
		}
		
//		L.V = L.O ? false : LogManager.log("[cache] no file cache for [" + projID + "/" + uuid + "/" + urlID + "]");
		return null;
	}
	
	/**
	 * 获得指定特征码的数位串
	 * @return
	 */
	public static byte[] getCacheFileBS(final String projID, final String uuid, final String urlID, 
			final byte[] codeBS, final int codeOffset, final int codeLen){
		DataItem item = getCacheFileItem(projID, uuid, urlID, 
				codeBS, codeOffset, codeLen);
		if(item != null){
			return item.bs;
		}else{
			return null;
		}
	}
	
	/**
	 * 获得指定特征码的数据字符串。
	 */
	public static String getCacheFileString(final String projID, final String uuid, final String urlID, 
			final byte[] codeBS, final int codeOffset, final int codeLen){
		DataItem item = getCacheFileItem(projID, uuid, urlID, codeBS, codeOffset, codeLen);
		if(item != null){
			return item.toStringValue();
		}else{
			return null;
		}
	}
	
	/**
	 * 存储一条完整的记录cache中
	 */
	public static synchronized void storeCache(final String projID, final String uuid, final String urlID, 
			final byte[] projIDBS, final int projIdOffset, final int projLen, 
			final byte[] uuidBS, final int uuidOffset, final int uuidLen, 
			final byte[] urlIDBS, final int urlIDOffset, final int urlIDLen, 
			final byte[] codeBS, final int codeOffset, final int codeLen, 
			final byte[] valueBS, final int valueOffset, final int valuelen, boolean valueIsCopyed){
		
//		L.V = L.O ? false : LogManager.log("projID : " + projID + ", uuid : " + uuid + ", urlID : " + urlID);
		
		//检查是否存在projID，如果没有，则更新到库中
		{
			Vector vectorProj = getCacheVector(RS_PROJ_LIST);
			DataItem.addRecrodIfNotExists(RS_PROJ_LIST, vectorProj, projIDBS, projIdOffset, projLen);
		}
		
		{
			final String rmsUUID = getRMSNameForUUID(projID);
			Vector vectorUUID = getCacheVector(rmsUUID);
			DataItem.addRecrodIfNotExists(rmsUUID, vectorUUID, uuidBS, uuidOffset, uuidLen);
		}

		//检查urlID是否存在，
		{
			final String rmsNameForURLID = getRMSNameForURLID(projID, uuid);
			Vector vectorURLID = getCacheVector(rmsNameForURLID);
			DataItem.addRecrodIfNotExists(rmsNameForURLID, vectorURLID, urlIDBS, urlIDOffset, urlIDLen);
		}

		int storeIdx = 0;
		final String rmsNameForFileCode = getRMSNameForFileCode(projID, uuid, urlID);
		Vector vectorCoder = getCacheVector(rmsNameForFileCode);

		final String rmsNameForFile = getRMSNameForFile(projID, uuid, urlID);
		Vector vectorFile = getCacheVector(rmsNameForFile);

		final int codeSize = vectorCoder.size();
		if(codeSize == vectorFile.size() + 1){
			//后一个条件是由于保存时，可能不同步，导致的差错
			if(codeSize >= 2){
				DataItem item = (DataItem)vectorCoder.elementAt(codeSize - 1);
				DataItem.setDel(item);//清掉旧的最后一条记录数据
				StoreManager.storeData(rmsNameForFileCode, vectorCoder);
			}
		}
		
		{
			//有可能存在相同特征码，但内容不同，所以进行覆盖式更新
			storeIdx = DataItem.addRecrodIfNotExists(rmsNameForFileCode, vectorCoder, codeBS, codeOffset, codeLen);
		}
		
		{
			if(storeIdx < 0 || storeIdx == vectorFile.size()){//后一个条件是由于保存时，可能不同步，导致的差错
				DataItem.addRecrodAndSave(rmsNameForFile, vectorFile, valueBS, valueOffset, valuelen, valueIsCopyed, false);
			}else{
				DataItem item = (DataItem)vectorFile.elementAt(storeIdx);
				if(valueIsCopyed){
					item.bs = valueBS;
				}else{
					byte[] copyValueBS = new byte[valuelen];
					System.arraycopy(valueBS, valueOffset, copyValueBS, 0, valuelen);
					item.bs = copyValueBS;
				}
				DataItem.setUpdate(item);
				StoreManager.storeData(rmsNameForFile, vectorFile);
			}
			
			L.V = L.O ? false : LogManager.log("[cache] store cache item for [" + projID + "/" + uuid + "/" + urlID + "/" + (storeIdx<0?codeSize:storeIdx) + "]");
		}
	}
}
