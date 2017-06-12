package hc.core.cache;

import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import java.util.Vector;

public abstract class RecordWriterBuilder {
	private static final String TABLE_PREFIX = "table" + CacheManager.TABLE_NAME_SPLITTER;
	protected final String tableNameForMapper = "SYS_TableMapper";
	protected final String end_tableNameForMapper = CacheManager.TABLE_NAME_SPLITTER + tableNameForMapper;
	private final String rsPrefixForMobile;
//	如果没有数据，返回长度为1(只含记录总数为0)；否则每条记录的byte[]存放到Vector中
	protected final Vector mapSnap;
	protected static boolean isMapName = true;
	
	public static void enableMapName(final boolean isMap){
		CCoreUtil.checkAccess();
		isMapName = isMap;
	}

	public RecordWriterBuilder(final String rsPrefixForMobile){
		this.rsPrefixForMobile = rsPrefixForMobile;
		
		mapSnap = CacheStoreManager.getDataList(buildMapStore(), tableNameForMapper);
		
//		//列印map信息
//		final int size = mapSnap.size();
//		for (int i = 1; i < size; i++) {
//			DataItem di = (DataItem)mapSnap.elementAt(i);
//			if(di.isEmpty){
//				LogManager.log("record number : " + i + " is empty.");
//			}else{
//				LogManager.log("record number : " + i + " map [" + di.toStringValue() + "]");
//			}
//		}
	}
	
	public final String getTableRealName(String rmsName){
		if(rsPrefixForMobile != null){
			rmsName = rsPrefixForMobile + rmsName;
		}
		
//		System.out.println("getTableRealName : " + rmsName);
		
		boolean isEndTableNameMapper = false;
		if(tableNameForMapper.equals(rmsName) //for J2SE的不转换特殊表
				|| (isEndTableNameMapper = rmsName.endsWith(end_tableNameForMapper))//for J2ME关联登录账号
				|| isMapName == false){
			if(isEndTableNameMapper){
				return tableNameForMapper;
			}else{
				return rmsName;
			}
		}
		
		int tableIdx = -1;
		synchronized(mapSnap){
			final int size = mapSnap.size();
			
			//找已存在的关联名
			for (int i = 1; i < size; i++) {
				CacheDataItem di = (CacheDataItem)mapSnap.elementAt(i);
				if(di.isEmpty == false && di.toStringValue().equals(rmsName)){
					tableIdx = i;
//					LogManager.log("successful find MapTableName for : " + rmsName + "[" + (TABLE_PREFIX + tableIdx) + "], on record  number : " + tableIdx);
					return TABLE_PREFIX + tableIdx;
				}
			}
			
			//找一个删除的空位来重用
			for (int i = 1; i < size; i++) {
				CacheDataItem di = (CacheDataItem)mapSnap.elementAt(i);
				if(di.isEmpty){
					di.setUpdate(StringUtil.getBytes(rmsName));
					CacheStoreManager.storeData(tableNameForMapper, mapSnap);
					
					tableIdx = i;
//					LogManager.log("successful reuse MapTableName for : " + rmsName + "[" + (TABLE_PREFIX + tableIdx) + "], on record  number : " + tableIdx);

					break;
				}
			}
			
			if(tableIdx == -1){
				CacheDataItem di = new CacheDataItem(StringUtil.getBytes(rmsName), CacheDataItem.STATUS_TO_ADD);
				mapSnap.addElement(di);
				CacheStoreManager.storeData(tableNameForMapper, mapSnap);
				
				tableIdx = size;

//				LogManager.log("successful add MapTableName for : " + rmsName + "[" + (TABLE_PREFIX + tableIdx) + "], left record  number : " + tableIdx);
			}
		}
		
		return TABLE_PREFIX + tableIdx;
	}
	
	private final RecordWriter buildMapStore() {
		try {
			return openRecordStore(tableNameForMapper, true);
		} catch (Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return null;
	}
	
	public abstract RecordWriter openRecordStore(String rmsName, boolean createIfNecessary) throws Exception;
	
	public abstract void deleterCacheDir();
	
	public void deleteRecordStore(final String rmsName) throws Exception{
		boolean isEndTableNameMapper = false;
		if(tableNameForMapper.equals(rmsName) //for J2SE的不转换特殊表
				|| (isEndTableNameMapper = rmsName.endsWith(end_tableNameForMapper))//for J2ME关联登录账号
				|| isMapName == false){
			return;
		}
		
		try{
//			LogManager.log("try delete MapTableName in deleteRecordStore for : [" + rmsName + "]");
			final String idx = rmsName.substring(TABLE_PREFIX.length());
			final int int_idx = Integer.parseInt(idx);
			synchronized(mapSnap){
				final int size = mapSnap.size();
				if(int_idx < size){
					CacheDataItem di = (CacheDataItem)mapSnap.elementAt(int_idx);
					di.setDel();
					CacheStoreManager.storeData(tableNameForMapper, mapSnap);
					
//					LogManager.log("successful delete MapTableName for : " + rmsName + ", left record  number : " + (size - 1));
					return;
				}
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
