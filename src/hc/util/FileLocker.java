package hc.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class FileLocker {
	private final RandomAccessFile raf;
	private java.nio.channels.FileLock lock;
	private final FileChannel channel;
	final File lockFile;
	public static final String READ_MODE = "r";
	public static final String READ_WRITE_MODE = "rw";
	
	public FileLocker(final File file, final String mode) throws FileNotFoundException{
		this.lockFile = file;
		raf = new RandomAccessFile(file, mode);
		channel = raf.getChannel();
	}
	
	public boolean lock() throws IOException{
		lock = channel.tryLock();		
		return lock != null;
	}
	
	public void release() throws IOException{
		lock.release();		
	}
	
	public void exit(){
		try{
			channel.close();
			raf.close();
		}catch (final Exception e) {
		}
	}
}
