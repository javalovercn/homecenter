package hc.core.cache;

import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootConfigListener;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;

import java.util.Hashtable;
import java.util.Vector;

public class CacheManager {
	private final static int MIN_SIZE = 20 * 1024;
	
	private static int fastSnapMinSize;
	
	static{
		RootConfig.addListener(new RootConfigListener() {
			public void onRefresh() {
				CacheManager.resetMinCacheSize();
			}
		});
	}
	
	public static void resetMinCacheSize(){
		fastSnapMinSize = 0;
	}
	
	/**
	 * 服务器端设定的最低Cache的数据包大小。
	 * @return
	 */
	public static int getMinCacheSize(){
		int out = fastSnapMinSize;
		if(out == 0){
			out = getMinCacheSizeFromRC();
			fastSnapMinSize = out;
		}
		return out;
	}
	
	private static int getMinCacheSizeFromRC(){
		final int newMin = RootConfig.getInstance().getIntProperty(RootConfig.p_CacheMinSize);
		if(newMin < MIN_SIZE){
			return MIN_SIZE;
		}else{
			return newMin;
		}
	}

	public static final String TABLE_NAME_SPLITTER = "_";

	public static final String buildUIDTableNamePrefix(final String uid){
		return uid + CacheManager.TABLE_NAME_SPLITTER;
	}
	
	public static final String ELE_URL_ID_MENU = CCoreUtil.SYS_PREFIX + "MENU";
	public static final byte[] ELE_URL_ID_MENU_BS = ELE_URL_ID_MENU.getBytes();
	
	public static final String ELE_PROJ_ID_HTML_PROJ = CCoreUtil.SYS_PREFIX + "HTMLPRO";
	public static final byte[] ELE_PROJ_ID_HTML_PROJ_BS = ELE_PROJ_ID_HTML_PROJ.getBytes();
	
	public static final String ELE_URL_ID_HTML_BODY = CCoreUtil.SYS_PREFIX + "HTML_BODY";//竖屏
	public static final byte[] ELE_URL_ID_HTML_BODY_BS = ELE_URL_ID_HTML_BODY.getBytes();
	
	private static final String _LIST = "_list";
	public static final int TYPE_CSS = 1;
	public static final int TYPE_JS = 2;
	
	public static final int CODE_LEN = 20;
	
	public static final String RS_PROJ_LIST = "cach_prj_list";
	private static final String RS_PRE_PROJ_UUID_LIST = "cach_uid_";
	private static final String RS_PRE_PROJ_URLID_LIST = "cach_url_";
	private static final String RS_PRE_URLID_FILECODE_LIST = "cach_fc_";
	private static final String RS_PRE_URLID_FILE_LIST = "cach_file_";
	
	private static final Hashtable cacheBuffer = new Hashtable();
	
	public static final synchronized void clearBuffer(){//加锁，以免其它线程可能正在操作时，发生清空。
		final int oldSize = cacheBuffer.size();
		cacheBuffer.clear();
		if(oldSize > 0){
			L.V = L.O ? false : LogManager.log("[cache] clear cache buffer (not cache data) .");
		}
	}
	
	/**
	 * 删除全部的cache数据。
	 */
	public static synchronized void emptyCache(){
		Vector vector = getCacheVector(RS_PROJ_LIST);
		final int size = vector.size();
		
		for (int i = 1; i < size; i++) {
			CacheDataItem item = (CacheDataItem)vector.elementAt(i);
			if(item.isEmpty){
				continue;
			}
			final String itemProjID = item.toStringValue();
			
			item.setDel();
			removeProjCacheAndRMS(itemProjID);
		}
		
		CacheStoreManager.removeRMS(RS_PROJ_LIST);
		
		clearBuffer();
		
		L.V = L.O ? false : LogManager.log("[cache] clear all cache data and buffer.");
	}
	
	/**
	 * 删除工程时，附带删除工程的cache文件
	 * @param projIDS 数组中间可能出现null
	 */
	public static synchronized void delProjects(String[] projIDS){
		Vector vector = getCacheVector(RS_PROJ_LIST);
		final int size = vector.size();
		
		for (int i = 1; i < size; i++) {
			CacheDataItem item = (CacheDataItem)vector.elementAt(i);
			if(item.isEmpty){
				continue;
			}
			final String itemProjID = item.toStringValue();
			
			for (int j = 0; j < projIDS.length; j++) {
				if(itemProjID.equals(projIDS[j])){//参数可为null
					item.setDel();
					removeProjCacheAndRMS(itemProjID);
					break;
				}
			}
		}
		
		CacheStoreManager.storeData(RS_PROJ_LIST, vector);
	}
	
	static byte[] mobile_uidBS;
	static final byte[] codeBSforMobileSave = new byte[CODE_LEN];
	
	public static synchronized void encodeStoreRespAtMobileSide(
			final byte[] screenIDBS, final int screenIDIdx, final int screentIDLen,
			final byte[] projIdBS, final int projIdIdx, final int projIdLen, 
			final byte[] scriptBS, final int scriptIndex, final int scriptLen, 
			final String projID,	final String urlID, final String uid) {
		if(isMeetCacheLength(scriptLen) == false){
			L.V = L.O ? false : LogManager.log("cancle cache for [" + projID + "/" + uid + "/" + urlID + "] for length lower than min length.");
			return;
		}
		
		if(mobile_uidBS == null){
			mobile_uidBS = ByteUtil.getBytes(uid, IConstant.UTF_8);
		}
		
		L.V = L.O ? false : LogManager.log("save a cache for [" + projID + "/" + uid + "/" + urlID + "]");
		
		ByteUtil.encodeFileXOR(scriptBS, scriptIndex, scriptLen, codeBSforMobileSave, 0, codeBSforMobileSave.length);

		CacheManager.storeCache(projID, uid, urlID, 
				projIdBS, projIdIdx, projIdLen, 
				mobile_uidBS, 0, mobile_uidBS.length,
				screenIDBS, screenIDIdx, screentIDLen, 
				codeBSforMobileSave, 0, codeBSforMobileSave.length, 
				scriptBS, scriptIndex, scriptLen, false);
		
		ContextManager.getContextInstance().sendWrap(MsgBuilder.E_RESP_CACHE_OK, codeBSforMobileSave, 0, codeBSforMobileSave.length);
		L.V = L.O ? false : LogManager.log("send cache responce. [" + ByteUtil.toHex(codeBSforMobileSave) + "]");
	}
	
	/**
	 * 仅限于客户端的功能。
	 * 通知cache系统，最新的工程名称列表，以供删除不被使用的工程cache数据。
	 * @param projIDS
	 * @return 返回删除的工程标识
	 */
	public static Vector notifyServerProj(String[] projIDS){
		Vector delProjVector = new Vector();
		
		Vector vector = getCacheVector(RS_PROJ_LIST);
		final int size = vector.size();
		
		boolean isChanged = false;
		for (int i = 1; i < size; i++) {
			CacheDataItem item = (CacheDataItem)vector.elementAt(i);
			if(item.isEmpty){
				continue;
			}
			
			final String itemProjID = item.toStringValue();
			
			if(itemProjID.equals(ELE_PROJ_ID_HTML_PROJ)){//虚拟工程
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
				item.setDel();
				
				removeProjCacheAndRMS(itemProjID);
				delProjVector.addElement(itemProjID);
				isChanged = true;
			}
		}
		
		if(isChanged){
			CacheStoreManager.storeData(RS_PROJ_LIST, vector);
		}
		
		return delProjVector;
	}
	
	public static String getRMSNameForUUID(String projID){
		return RS_PRE_PROJ_UUID_LIST + projID + _LIST;
	}
	
	public static String getRMSNameForURLID(String projID, String uuid){
		return RS_PRE_PROJ_URLID_LIST + projID + "_" + uuid + _LIST;//注意：由于ios的RMS命名规范是rmsName+recordNo，所以前缀名不能被包含。
	}
	
	public static String getRMSNameForFileCode(String projID, String uuid, String urlID){
		return RS_PRE_URLID_FILECODE_LIST + projID + "_" + uuid + "_" + urlID + _LIST;
	}
	
	public static String getRMSNameForFile(String projID, String uuid, String urlID){
		return RS_PRE_URLID_FILE_LIST + projID + "_" + uuid + "_" + urlID + _LIST;
	}
	
	/**
	 * 将指定工程库的全部信息，从cache系统中删除
	 * @param projID
	 */
	public static synchronized void removeProjCacheAndRMS(String projID){
		L.V = L.O ? false : LogManager.log("[cache] delete all cache data of project [" + projID + "].");
		removeAllUUIDFrom(projID);
		
		//注意：不能先删除，因为有可能存储故障，而导致问题，可以下次再次进行删除
		{
			//将projID从库记录中删除
			Vector projs = getCacheVector(RS_PROJ_LIST);
			final int size = projs.size();
			for (int i = 1; i < size; i++) {
				final CacheDataItem dataItem = (CacheDataItem)projs.elementAt(i);
				if(dataItem.isEmpty){
					continue;
				}
				String prjid = dataItem.toStringValue();
				if(prjid.equals(projID)){
					dataItem.setDel();
					CacheStoreManager.storeData(RS_PROJ_LIST, projs);
					
					break;
				}
			}
		}
		
		L.V = L.O ? false : LogManager.log("[cache] remove all cache for project : [" + projID + "].");
	}

	/**
	 * 服务器端某uid的cache故障，仅删除关联的。
	 * @param projID
	 * @param uid
	 */
	public static synchronized void removeUIDFrom(final String uid) {
		Vector projs = getCacheVector(RS_PROJ_LIST);
		final int sizeProj = projs.size();
		for (int i = 1; i < sizeProj; i++) {//遍历全部project
			final CacheDataItem pdi = (CacheDataItem)projs.elementAt(i);
			if(pdi.isEmpty){
				continue;
			}
			
			final String projID = pdi.toStringValue();

			removeUIDFrom(projID, uid);
		}
	}
	
	/**
	 * 获得指定工程下uid的有效记录数。
	 * @param projID
	 * @param uid
	 * @return
	 */
	public synchronized static int getRecordNum(final String projID, final String uid){
		final String rmsURLID = getRMSNameForURLID(projID, uid);
		Vector urlIDs = getCacheVector(rmsURLID);
		return urlIDs.size() - 1;
	}
	
	/**
	 * 删除uid在指定的projID下。
	 * @param projID
	 * @param uid
	 */
	public synchronized static boolean removeUIDFrom(final String projID, final String uid){
		String rmsUUID = getRMSNameForUUID(projID);
		Vector uuid = getCacheVector(rmsUUID);
		final int sizeUUID = uuid.size();
		for (int uuidIdx = 1; uuidIdx < sizeUUID; uuidIdx++) {
			final CacheDataItem dataItem = (CacheDataItem)uuid.elementAt(uuidIdx);
			if(dataItem.isEmpty){
				continue;
			}
			
			String uuidStr = dataItem.toStringValue();
			if(uid.equals(uuidStr)){
				final boolean isChanged = removeAllURL(projID, uuidStr);
				
				dataItem.setDel();
				CacheStoreManager.storeData(rmsUUID, uuid);
				return isChanged;
			}
		}
		return false;
	}
	
	/**
	 * 删除指定工程下全部uid的缓存
	 * @param projID
	 */
	private static void removeAllUUIDFrom(String projID) {
		String rmsUUID = getRMSNameForUUID(projID);
		Vector uuid = getCacheVector(rmsUUID);
		final int size = uuid.size();
		for (int uuidIdx = 1; uuidIdx < size; uuidIdx++) {
			final CacheDataItem dataItem = (CacheDataItem)uuid.elementAt(uuidIdx);
			if(dataItem.isEmpty){
				continue;
			}
			
			String uuidStr = dataItem.toStringValue();
			
			removeAllURL(projID, uuidStr);
		}
		
		delCacheAndRMS(rmsUUID);
	}

	private static boolean removeAllURL(String projID, String uuidStr) {
		boolean isChanged = false;
		
		final String rmsURLID = getRMSNameForURLID(projID, uuidStr);
		
		Vector urlIDs = getCacheVector(rmsURLID);
		final int size = urlIDs.size();
		for (int i = 1; i < size; i++) {
			final CacheDataItem dataItem = (CacheDataItem)urlIDs.elementAt(i);
			if(dataItem.isEmpty){
				continue;
			}
			
			isChanged = true;
			
			String urlID = dataItem.toStringValue();
			
			delCacheAndRMSForURL(projID, uuidStr, urlID);
		}
		
		delCacheAndRMS(rmsURLID);
		
		return isChanged;
	}

	private static void delCacheAndRMSForURL(String projID, String uuid, String urlID) {
		final String rmsNameForFile = getRMSNameForFile(projID, uuid, urlID);
		final String rmsNameForFileCode = getRMSNameForFileCode(projID, uuid, urlID);

		delCacheAndRMS(rmsNameForFile);
		delCacheAndRMS(rmsNameForFileCode);
	}

	private static void delCacheAndRMS(final String rmsName) {
		CacheStoreManager.removeRMS(rmsName);
		delCacheVector(rmsName);
	}
	
	public static Vector getCacheVector(String rmsName){
		Vector out = (Vector)cacheBuffer.get(rmsName);
		if(out == null){
			out = CacheStoreManager.getDataList(rmsName);
			cacheBuffer.put(rmsName, out);
		}
		return out;
	}
	
	private static void delCacheVector(String rmsName){
		cacheBuffer.remove(rmsName);
	}
	
//	public static Vector getCacheCoder(String projID, String uuid, String urlID){
//		return getCacheVector(getRMSNameForFileCode(projID, uuid, urlID));
//	}
	
	public static void setDelOnCacheCoderForStoreProblem(String projID, String uuid, Vector vectorCoder, String cacheXorCoder){
		final int size = vectorCoder.size();
		for (int i = size - 1; i >= 1; i--) {
			final CacheDataItem item = (CacheDataItem)vectorCoder.elementAt(i);
			if(item.isEmpty){
				continue;
			}
			
			if(item.toStringValue().equals(cacheXorCoder)){
				item.setDel();
				CacheStoreManager.storeData(getRMSNameForURLID(projID, uuid), vectorCoder);
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
	public static CacheDataItem getCacheFileItem(final String projID, String uuid, String urlID, 
			final byte[] codeBS, final int codeOffset, final int codeLen){
		final Vector vectorCode = getCacheVector(getRMSNameForFileCode(projID, uuid, urlID));
		final int matchCodeIdx = CacheDataItem.searchMatchVectorIdx(vectorCode, codeBS, codeOffset, codeLen);
		if(matchCodeIdx > 0){
			Vector vectorFile = getCacheVector(getRMSNameForFile(projID, uuid, urlID));
			if(vectorFile.size() <= matchCodeIdx){
				LogManager.errToLog("over size index[" + matchCodeIdx + "] for getCacheFileItem");
				return null;
			}
			final CacheDataItem elementAt = (CacheDataItem)vectorFile.elementAt(matchCodeIdx);
			if(elementAt.isEmpty){
				LogManager.errToLog("size index[" + matchCodeIdx + "] for getCacheFileItem is empty record.");
				return null;
			}else{
				return elementAt;
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
		CacheDataItem item = getCacheFileItem(projID, uuid, urlID, 
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
		CacheDataItem item = getCacheFileItem(projID, uuid, urlID, codeBS, codeOffset, codeLen);
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
			CacheDataItem.addRecrodIfNotExists(RS_PROJ_LIST, vectorProj, projIDBS, projIdOffset, projLen);
		}
		
		{
			final String rmsUUID = getRMSNameForUUID(projID);
			Vector vectorUUID = getCacheVector(rmsUUID);
			CacheDataItem.addRecrodIfNotExists(rmsUUID, vectorUUID, uuidBS, uuidOffset, uuidLen);
		}

		//检查urlID是否存在，
		{
			final String rmsNameForURLID = getRMSNameForURLID(projID, uuid);
			Vector vectorURLID = getCacheVector(rmsNameForURLID);
			CacheDataItem.addRecrodIfNotExists(rmsNameForURLID, vectorURLID, urlIDBS, urlIDOffset, urlIDLen);
		}

		int storeIdx = 0;
		final String rmsNameForFileCode = getRMSNameForFileCode(projID, uuid, urlID);
		Vector vectorCoder = getCacheVector(rmsNameForFileCode);

		final String rmsNameForFile = getRMSNameForFile(projID, uuid, urlID);
		Vector vectorFile = getCacheVector(rmsNameForFile);

		final int fileSize = vectorFile.size();
		if(fileSize == vectorCoder.size() + 1){//先存file，后存code
			//后一个条件是由于保存时，可能不同步，导致的差错
			if(fileSize >= 2){
				CacheDataItem item = (CacheDataItem)vectorFile.elementAt(fileSize - 1);
//				if(item.isEmpty == false){
					item.setDel();//清掉旧的最后一条记录数据
					CacheStoreManager.storeData(rmsNameForFile, vectorFile);
//				}
			}
		}
		
		storeIdx = CacheDataItem.searchMatchVectorIdx(vectorCoder, codeBS, codeOffset, codeLen);
		final boolean isMatch = (storeIdx > 0);
		if(isMatch == false){
			storeIdx = CacheDataItem.addRecrodAndSave(rmsNameForFile, vectorFile, valueBS, valueOffset, valuelen, valueIsCopyed, CacheDataItem.isEmptyFirst);
		}else{
			//有可能存在相同特征码，但内容不同，所以进行覆盖式更新
			CacheDataItem item = (CacheDataItem)vectorFile.elementAt(storeIdx);
			if(valueIsCopyed){
				item.setUpdate(valueBS);
			}else{
				byte[] copyValueBS = new byte[valuelen];
				System.arraycopy(valueBS, valueOffset, copyValueBS, 0, copyValueBS.length);
				item.setUpdate(copyValueBS);
			}
			
			CacheStoreManager.storeData(rmsNameForFile, vectorFile);
		}
		
		if(isMatch == false){
			if(storeIdx == vectorCoder.size()){//后一个条件是由于保存时，可能不同步，导致的差错
				CacheDataItem.addRecrodAndSave(rmsNameForFileCode, vectorCoder, codeBS, codeOffset, codeLen, false, false);//注意：此处isEmptyFirst必须为false
			}else{
				//重用empty record
				CacheDataItem item = (CacheDataItem)vectorCoder.elementAt(storeIdx);
				
				if(item.isEmpty == false){
					LogManager.errToLog("rmsNameForFileCode should be empty at idx : " + storeIdx + ", but it is used.");
				}
				
				final byte[] copyValueBS = new byte[codeLen];
				System.arraycopy(codeBS, codeOffset, copyValueBS, 0, copyValueBS.length);
				item.setUpdate(copyValueBS);
				
				CacheStoreManager.storeData(rmsNameForFileCode, vectorCoder);
			}
			
			L.V = L.O ? false : LogManager.log("[cache] successful store cache item for [" + projID + "/" + uuid + "/" + urlID + "/" + (storeIdx<0?fileSize:storeIdx) + "]");
		}
	}

	public static boolean isMeetCacheLength(final int data_len) {
		return data_len > getMinCacheSize();
	}

	/**
	 * 此逻辑应部署于服务器端
	 * @param projID
	 * @param matchUID
	 * @return
	 */
	public static boolean isOverflowProjectTotalCacheNum(final String projID, final String matchUID){
		int total = 0;
		
		final String rmsUUID = getRMSNameForUUID(projID);
		final Vector uuid = getCacheVector(rmsUUID);
		final int uuidSize = uuid.size();
		for (int uuidIdx = 1; uuidIdx < uuidSize; uuidIdx++) {
			final CacheDataItem dataItem = (CacheDataItem)uuid.elementAt(uuidIdx);
			if(dataItem.isEmpty){
				continue;
			}
			
			final String uuidStr = dataItem.toStringValue();
			
			if(uuidStr.equals(matchUID) == false){
				continue;
			}
			
			final Vector urlVector = getCacheVector(getRMSNameForURLID(projID, uuidStr));
			final int size = urlVector.size();
			for (int i = 1; i < size; i++) {
				final CacheDataItem itemUrlID = (CacheDataItem)urlVector.elementAt(i);
				if(itemUrlID.isEmpty){
					continue;
				}
				total += CacheStoreManager.getRecordNumByStoreName(getRMSNameForFileCode(projID, uuidStr, itemUrlID.toStringValue()));
			}
		}
		
		return total > (uuidSize - 1) * 20 * 3;
	}
	
	
	public static synchronized final void searchAndRemoveTooLongSleep(CacheTooLongChecker checker){
		final Vector vector = getCacheVector(RS_PROJ_LIST);
		final int size = vector.size();
		
		for (int i = 1; i < size; i++) {
			final CacheDataItem projItem = (CacheDataItem)vector.elementAt(i);
			if(projItem.isEmpty){
				continue;
			}
			
			final String projID = projItem.toStringValue();
			
			final Vector uuid = getCacheVector(getRMSNameForUUID(projID));
			final int uuidSize = uuid.size();
			for (int uuidIdx = 1; uuidIdx < uuidSize; uuidIdx++) {
				final CacheDataItem dataItem = (CacheDataItem)uuid.elementAt(uuidIdx);
				if(dataItem.isEmpty){
					continue;
				}
				
				final String uuidStr = dataItem.toStringValue();
				
				if(checker.isTooLongForSleep(projID, uuidStr)){
					removeUIDFrom(projID, uuidStr);
				}
			}
		}
		
	}
	
	/**
	 * 此逻辑应部署于服务器端
	 * @param matchUID
	 * @return
	 */
	public static synchronized Vector checkAndDelCacheOverflow(final String matchUID){
		final Vector delProjVector = new Vector();
		
		final Vector vector = getCacheVector(RS_PROJ_LIST);
		final int size = vector.size();
		
		for (int i = 1; i < size; i++) {
			final CacheDataItem item = (CacheDataItem)vector.elementAt(i);
			if(item.isEmpty){
				continue;
			}
			
			final String itemProjID = item.toStringValue();
			if(isOverflowProjectTotalCacheNum(itemProjID, matchUID)){//检查存储记录数是否过载
				L.V = L.O ? false : LogManager.log("cache [" + itemProjID + "/" + matchUID + "] is overflow (up threshold), clear it.");
				removeUIDFrom(itemProjID, matchUID);//仅删除关联，不能全删工程
				
				delProjVector.addElement(itemProjID);
			}else{
//				L.V = L.O ? false : LogManager.log("cache [" + itemProjID + "/" + matchUID + "] is not overflow (up threshold).");
			}
		}
		
		return delProjVector;
	}
}
