package hc.core.cache;

public interface RecordWriterBuilder {
	public RecordWriter openRecordStore(String dbname, boolean createIfNecessary) throws Exception;
	
	public void deleteRecordStore(String rmsName) throws Exception;
}
