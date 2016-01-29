package hc.server.rms;

import hc.core.cache.RecordWriter;
import hc.core.cache.RecordWriterBuilder;
import hc.server.data.StoreDirManager;

import java.io.File;
import java.io.RandomAccessFile;

public class J2SERecordWriterBuilder implements RecordWriterBuilder {
	@Override
	public RecordWriter openRecordStore(String dbname, boolean createIfNecessary) throws Exception {
		return new J2SERMSRecordWriter(new RandomAccessFile(getRMSFile(dbname), "rw"));
	}

	public final File getRMSFile(String dbname) {
		return new File(StoreDirManager.RMS_DIR, dbname);
	}

	@Override
	public void deleteRecordStore(String rmsName) throws Exception {
		final File rmsFile = getRMSFile(rmsName);
		if(rmsFile.delete() == false){
			rmsFile.deleteOnExit();
		}
	}

}
