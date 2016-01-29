package hc.core.cache;

public interface RecordWriter {
	public void setRecord(int recordId, byte[] data, int offset, int len) throws Exception;
	
	public int addRecord(byte[] data, int offset, int len) throws Exception;
	
	public void closeRecordStore() throws Exception;
	
	public byte[] getRecord(int recordId) throws Exception;
	
	public void deleteRecord(int recordId) throws Exception;
}
