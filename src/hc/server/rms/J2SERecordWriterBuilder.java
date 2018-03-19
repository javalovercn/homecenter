package hc.server.rms;

import hc.core.ContextManager;
import hc.core.RootServerConnector;
import hc.core.cache.CacheStoreManager;
import hc.core.cache.RecordWriter;
import hc.core.cache.RecordWriterBuilder;
import hc.core.util.LogManager;
import hc.server.data.StoreDirManager;
import hc.util.ResourceUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class J2SERecordWriterBuilder extends RecordWriterBuilder {
	public J2SERecordWriterBuilder() {
		super(null);
	}

	@Override
	public RecordWriter openRecordStore(final String rmsName, final boolean createIfNecessary) throws Exception {
		return new J2SERMSRecordWriter(getFileForRMS(getTableRealName(rmsName)), rmsName);
	}

	public RandomAccessFile getFileForRMS(final String rmsName) throws FileNotFoundException {
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
		if (rmsFile.delete() == false) {
			LogManager.errToLog("fail to deleteRecordStore : " + rmsFile.getAbsolutePath());
			// rmsFile.deleteOnExit();//注：不能使用PropertiesManager.addDeleteDir，因为可能被使用
		}
	}

	@Override
	public void deleterCacheDir() {
		ResourceUtil.clearDir(StoreDirManager.RMS_DIR);
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				RootServerConnector.notifyHttpError(RootServerConnector.LOFF_ERR_EMPTY_CACHE);
			}
		});
		CacheStoreManager.setRecrodWriterBuilder(new J2SERecordWriterBuilder());
	}

}
