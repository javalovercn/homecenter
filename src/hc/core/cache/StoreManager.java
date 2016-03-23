package hc.core.cache;

import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;

import java.util.Vector;

public class StoreManager {
	protected static final byte[] EMPTY_BS = "_eMpTy_Bs_Hc".getBytes();
	private static RecordWriterBuilder rwBuilder;
	
	public static void setRecrodWriterBuilder(RecordWriterBuilder rwb){
		rwBuilder = rwb;
	}
	
	public static RecordWriter openRecordStore(String dbname, boolean createIfNecessary) throws Exception{
		return rwBuilder.openRecordStore(dbname, createIfNecessary);
	}
	
	public static void removeRMS(String rmsName){
		try {
			rwBuilder.deleteRecordStore(rmsName);
		} catch (Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public static void storeData(String rsName, Vector vector){
		synchronized(vector){
			RecordWriter rs = null;
			try {
				final DataItem firstDataItem = (DataItem)vector.elementAt(0);
				final int totalProjNum = getRecordNum(firstDataItem.bs);
				rs = openRecordStore(rsName, true);
				final int newRecordNum = vector.size() - 1;
				final int endIdx = newRecordNum + 1;
				for (int idx = 1; idx < endIdx;idx++) {
					DataItem nextItem = (DataItem)vector.elementAt(idx);
					if((nextItem.status == DataItem.STATUS_TO_ADD || nextItem.status == DataItem.STATUS_TO_UPDATE)){
						if(nextItem.isEmpty || idx <= totalProjNum){
							rs.setRecord(idx + 1, nextItem.bs, 0, nextItem.bs.length);
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
					}else if(nextItem.status == DataItem.STATUS_TO_DEL){
						rs.setRecord(idx + 1, EMPTY_BS, 0, EMPTY_BS.length);
						nextItem.setEmpty();
					}
					nextItem.status = DataItem.STATUS_EXISTS;
				}
				
				if(totalProjNum != newRecordNum){
					setRecordNum(firstDataItem.bs, newRecordNum);
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
	
	private static final int RECORD_NUM_BS_LEN = 2;
	
	protected static boolean isEmptyBS(byte[] bs){
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
	public final static int getRecordNum(byte[] bs){
		return ByteUtil.twoBytesToInteger(bs, 0);
	}
	
	public final static void setRecordNum(byte[] bs, int newNum){
		ByteUtil.integerToTwoBytes(newNum, bs, 0);
	}
	
	public static int getRecordNumByStoreName(String rsName){
		try {
			RecordWriter rs = openRecordStore(rsName, true);
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
		}
		return 0;
	}
	
	/**
	 * 如果没有数据，返回长度为1(只含记录总数为0)；否则每条记录的byte[]存放到Vector中
	 * @param rsName
	 * @return DataItem
	 */
	public static Vector getDataList(String rsName){
		RecordWriter rs = null;
		Vector v = new Vector();
		try {
			int totalProjNum = 0;
			rs = openRecordStore(rsName, true);
			byte[] bs = null;
			try{
				bs = rs.getRecord(1);
			}catch (Throwable e) {
			}
			if(bs != null){
				totalProjNum = getRecordNum(bs);
			}else{
				bs = new byte[RECORD_NUM_BS_LEN];
				rs.addRecord(bs, 0, bs.length);
			}
			
			{
				DataItem item = new DataItem(bs, DataItem.STATUS_EXISTS);
				v.addElement(item);
			}
			
			final int endIdx = totalProjNum + 1;
			for (int idx = 1; idx < endIdx;idx++) {
				bs = rs.getRecord(idx + 1);
				DataItem item = new DataItem(bs, DataItem.STATUS_EXISTS);
				v.addElement(item);
			}
		} catch (Exception e) {
			ExceptionReporter.printStackTrace(e);
		} finally {
			if(rs != null){
				try{
					rs.closeRecordStore();
				}catch (Throwable e) {
				}
			}
		}
		return v;
	}
	
}
