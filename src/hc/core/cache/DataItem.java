package hc.core.cache;

import java.util.Vector;
import hc.core.util.ByteUtil;

public class DataItem {
	//记录已存在，可能为空记录
	public static final int STATUS_EXISTS = 1;
	
	//将要增加到库中，更新到库后，将转为STATUS_EXISTS
	public static final int STATUS_TO_ADD = 2;
	
	//将从库中删除，删除后，将转为STATUS_EXISTS，并转为空记录
	public static final int STATUS_TO_DEL = 3;
	
	//将要更新到库中，更新到库后，将转为STATUS_EXISTS
	public static final int STATUS_TO_UPDATE = 4;
	
	public boolean isEmpty = false;
	public byte[] bs;
	public int status;
	public String stringValue;
	int emptyIdx;
	
	public final String toStringValue(){
		if(isEmpty){
			return "";
		}
		
		if(stringValue == null){
			stringValue = ByteUtil.toString(bs, 0, bs.length);
		}
		return stringValue;
	}
	
	public final void setEmpty(){
		bs = StoreManager.EMPTY_BS;
		isEmpty = true;
		stringValue = null;
	}
	
	public DataItem(byte[] bs, int status){
		this.bs = bs;
		if(StoreManager.isEmptyBS(bs)){
			isEmpty = true;
		}
		this.status = status;
	}
	
	public final boolean equalBS(final byte[] value, final int offset, final int len){
		if(len == bs.length){
			final int endIdx = offset + len;
			for (int i = offset, j = 0; i < endIdx; i++, j++) {
				if(bs[j] != value[i]){
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public final static int addRecrodAndSave(final String rmsName, final Vector vector, byte[] value, int offset, int len, 
			boolean valueIsCopyed, boolean isEmptyFirst){
		byte[] copybs;
		if(valueIsCopyed){
			copybs = value;
		}else{
			copybs = new byte[len];
			System.arraycopy(value, offset, copybs, 0, len);//必须是拷贝方式，因为数据源是EventCenter和压缩可复用对象
		}
		
		synchronized (vector) {
			DataItem item = getEmptyRecord(vector);
			if(isEmptyFirst && item != null){
				item.bs = copybs;
				item.status = STATUS_TO_UPDATE;
				item.stringValue = null;
				item.isEmpty = false;
				
				final int lastStoreIdx = item.emptyIdx;
				StoreManager.storeData(rmsName, vector);
				return lastStoreIdx;
			}else{
				item = new DataItem(copybs, STATUS_TO_ADD);
				vector.addElement(item);
				
				StoreManager.storeData(rmsName, vector);
				return -1;
			}
		}
	}
	
	/**
	 * 
	 * @param vector
	 * @param value
	 * @return > 0: exits vector index, < 0 : not exists and added
	 */
	public final static int addRecrodIfNotExists(final String rmsName, final Vector vector, final byte[] value, final int offset, final int len){
		synchronized (vector) {
			final int endIdx = vector.size();
			for (int i = endIdx - 1; i >= 1; i--) {//因为尾部可能存在存储不一致产生的空位。
				DataItem item = (DataItem)vector.elementAt(i);
				if(item.equalBS(value, offset, len)){
					return i;
				}
			}
			return addRecrodAndSave(rmsName, vector, value, offset, len, false, true);
		}
	}
	
	public static void setUpdate(final DataItem item){
		item.status = STATUS_TO_UPDATE;
		item.isEmpty = false;
	}
	
	public static void setDel(final DataItem item){
		item.status = STATUS_TO_DEL;
	}
	
	/**
	 * 如果没有空记录，则返回null
	 * @param vector
	 * @return
	 */
	private static DataItem getEmptyRecord(final Vector vector){
		final int endIdx = vector.size();
		for (int i = 1; i < endIdx; i++) {
			DataItem item = (DataItem)vector.elementAt(i);
			if(item.isEmpty){
				item.emptyIdx = i;
				return item;
			}
		}
		return null;
	}
}
