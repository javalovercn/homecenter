package hc.core.cache;

import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;

import java.util.Vector;

public class CacheStoreManager {
	protected static final byte[] EMPTY_BS = "_eMpTy_Bs_Hc".getBytes();
	private static RecordWriterBuilder rwBuilder;
	
	public static void setRecrodWriterBuilder(final RecordWriterBuilder rwb){
		CCoreUtil.checkAccess();
		rwBuilder = rwb;
	}
	
	
	final static void deleteCacheDir(){
		rwBuilder.deleterCacheDir();
	}
	
	public static RecordWriter openRecordStore(final String dbname, final boolean createIfNecessary) throws Exception{
		return rwBuilder.openRecordStore(dbname, createIfNecessary);
	}
	
	public static void removeRMS(final String rmsName){
		try {
			rwBuilder.deleteRecordStore(rmsName);
		} catch (Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	/**
	 * Vector的0位是总记录数。存储到RMS中时，它对应的RecordNo为1，其它数据依次为2, 3, 4...
	 * @param rsName
	 * @param vector
	 */
	public static void storeData(final String rsName, final Vector vector){
		synchronized(vector){
			RecordWriter rs = null;
			try {
				final CacheDataItem firstDataItem = (CacheDataItem)vector.elementAt(0);
				final int oldRecordNum = getRecordNum(firstDataItem.bs);
				rs = openRecordStore(rsName, true);
				final int vectorSize = vector.size();
				final int newRecordNum = vectorSize - 1;
				for (int logicRecordNo = 1; logicRecordNo < vectorSize; logicRecordNo++) {
					CacheDataItem nextItem = (CacheDataItem)vector.elementAt(logicRecordNo);
					if((nextItem.status == CacheDataItem.STATUS_TO_ADD || nextItem.status == CacheDataItem.STATUS_TO_UPDATE)){
						if(nextItem.isEmpty || logicRecordNo <= oldRecordNum){
							rs.setRecord(logicRecordNo + 1, nextItem.bs, 0, nextItem.bs.length);//首记录RecordNo=1为记录数，所以实际存储从第2开始
						}else{
							rs.addRecord(nextItem.bs, 0, nextItem.bs.length);
						}
						
						if(isEmptyBS(nextItem.bs)){
							//有效记录转为空记录
							nextItem.setEmpty();
						}else{
							//转为有效记录
							nextItem.isEmpty = false;
						}
						
						nextItem.stringValue = null;
					}else if(nextItem.status == CacheDataItem.STATUS_TO_DEL){
						rs.setRecord(logicRecordNo + 1, EMPTY_BS, 0, EMPTY_BS.length);//注意：没有物理删除，只是将数据特殊处理。
						nextItem.setEmpty();
					}
					nextItem.status = CacheDataItem.STATUS_EXISTS;
				}
				
				if(oldRecordNum != newRecordNum){
					setRecordNumToBS(firstDataItem.bs, newRecordNum);
					rs.setRecord(1, firstDataItem.bs, 0, firstDataItem.bs.length);//总记录数只能在存储时，进行更新。
				}
			} catch (Exception e) {
				ExceptionReporter.printStackTrace(e);
			} finally {
				if(rs != null){
					try{
						rs.closeRecordStore();
					}catch (Throwable e) {
						ExceptionReporter.printStackTrace(e);
					}
				}
			}
		}
	}
	
	private static final int RECORD_NUM_BS_LEN = 4;
	
	protected static boolean isEmptyBS(final byte[] bs){
		if(bs.length == EMPTY_BS.length){
			for (int i = 0; i < bs.length; i++) {
				if(bs[i] != EMPTY_BS[i]){
					return false;
				}
			}
			return true;
		}
	
		return false;
	}
	
	/**
	 * 含空记录数量
	 * @param bs
	 * @return
	 */
	private final static int getRecordNum(final byte[] bs){
		if(bs.length == 2){
			return ByteUtil.twoBytesToInteger(bs, 0);//早期采用两位编码
		}else{
			return (int)ByteUtil.fourBytesToLong(bs, 0);
		}
	}
	
	private final static void setRecordNumToBS(final byte[] bs, final int newNum){
		if(bs.length == 2){
			ByteUtil.integerToTwoBytes(newNum, bs, 0);
		}else{
			ByteUtil.integerToFourBytes(newNum, bs, 0);
		}
	}
	
	public static int getRecordNumByStoreName(final String rsName){
		RecordWriter rs = null;
		try {
			rs = openRecordStore(rsName, true);
			byte[] bs = null;
			try{
				bs = rs.getRecord(1);
			}catch (Throwable e) {
			}
			if(bs != null){
				return getRecordNum(bs);
			}
		} catch (Exception e) {
			ExceptionReporter.printStackTrace(e);
		} finally{
			if(rs != null){
				try{
					rs.closeRecordStore();
				}catch (Throwable e) {
				}
			}
		}
		return 0;
	}
	
	/**
	 * 如果没有数据，返回长度为1(只含记录总数为0)；否则每条记录的byte[]存放到Vector中
	 * @param rsName
	 * @return DataItem
	 */
	public static Vector getDataList(final String rsName){
		try {
			final RecordWriter writer = openRecordStore(rsName, true);
			return getDataList(writer, writer.getLogicTableName());
		} catch (Exception e) {
			ExceptionReporter.printStackTrace(e);
			return new Vector();
		}
	}
	
	/**
	 * 如果没有数据，返回长度为1(只含记录总数为0)；否则每条记录的byte[]存放到Vector中
	 * @param rsName
	 * @return DataItem
	 */
	public static Vector getDataList(final RecordWriter rs, final String logicTableName){
		Vector v = new Vector();
		boolean isException = false;
		
		try {
			int totalProjNum = 0;
			byte[] bs = null;
			try{
				bs = rs.getRecord(1);
			}catch (Throwable e) {
			}
			if(bs != null){
				totalProjNum = getRecordNum(bs);
			}else{
				bs = new byte[RECORD_NUM_BS_LEN];
				rs.addRecord(bs, 0, bs.length);//生成初始结构。第一条recordNO=1内容为总记录数
			}
			
			{
				CacheDataItem item = new CacheDataItem(bs, CacheDataItem.STATUS_EXISTS);//第一条recordNo=1，为总记录数
				v.addElement(item);
			}
			
			final int endIdx = totalProjNum + 1;
			for (int idx = 1; idx < endIdx;idx++) {
				bs = rs.getRecord(idx + 1);
				CacheDataItem item = new CacheDataItem(bs, CacheDataItem.STATUS_EXISTS);
//				if(item.isEmpty){
//					LogManager.log(logicTableName + " recordID [" + idx + "] is empty!");
//				}else{
//					LogManager.log(logicTableName + " recordID [" + idx + "] len : " + item.bs.length);
//				}
				v.addElement(item);
			}
			
//			LogManager.log(logicTableName + " total record num : " + totalProjNum);
		} catch (Exception e) {
			isException = true;
			ExceptionReporter.printStackTrace(e);
		} finally {
			if(rs != null){
				try{
					rs.closeRecordStore();
				}catch (Throwable e) {
				}
			}
			if(isException){
				deleteCacheDir();
				
				final int size = v.size();
				for (int i = size - 1; i > 0; i--) {
					v.removeElementAt(i);
				}
			}
		}
		return v;
	}
	
}
