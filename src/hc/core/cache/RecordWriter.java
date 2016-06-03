package hc.core.cache;

public interface RecordWriter {
	public void setRecord(int recordId, byte[] data, int offset, int len) throws Exception;
	
	public String getLogicTableName();
	
	/**
	 * 注意：起始记录号为1，不是0，这是RMS的规范
	 * @param data
	 * @param offset
	 * @param len
	 * @return
	 * @throws Exception
	 */
	public int addRecord(byte[] data, int offset, int len) throws Exception;
	
	public void closeRecordStore() throws Exception;
	
	/**
	 * 注意：起始记录号为1，不是0，这是RMS的规范
	 * @param recordId
	 * @return
	 * @throws Exception
	 */
	public byte[] getRecord(int recordId) throws Exception;
	
	/**
	 * 以下是J2ME的RMS规范
	 * The record is deleted from the record store. The recordId for this record is NOT reused.
	 * @param recordId
	 * @throws Exception
	 */
	public void deleteRecord(int recordId) throws Exception;
}
