package hc.server.rms;

import hc.core.cache.RecordWriter;
import hc.core.cache.RecordWriterBuilder;
import hc.server.data.StoreDirManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class J2SERecordWriterBuilder extends RecordWriterBuilder {
	@Override
	public RecordWriter openRecordStore(final String rmsName, final boolean createIfNecessary) throws Exception {
		return new J2SERMSRecordWriter(getFileForRMS(getTableRealName(rmsName)), rmsName);
	}

	public RandomAccessFile getFileForRMS(final String rmsName)
			throws FileNotFoundException {
		return new RandomAccessFile(getRMSFile(rmsName), "rw");
	}

	public static final File getRMSFile(final String rmsName) {
		return new File(StoreDirManager.RMS_DIR, rmsName);
	}

	@Override
	public void deleteRecordStore(final String rmsName) throws Exception {
		final String tableRealName = getTableRealName(rmsName);
		super.deleteRecordStore(tableRealName);
		
		final File rmsFile = getRMSFile(tableRealName);
		if(rmsFile.delete() == false){
			rmsFile.deleteOnExit();
		}
	}

}
