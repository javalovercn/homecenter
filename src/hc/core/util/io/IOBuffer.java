package hc.core.util.io;

import java.io.IOException;
import java.io.InputStream;
import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;

public class IOBuffer {
	public byte[] buffer;
	public int storeIdx;
	public int readIdx;
	int bufferLen;
	private boolean isCycleable = true;
	public IOBufferFileStream stream;
	
	/**
	 * true means file is too big, it must file <-> file.
	 * @param size
	 * @return
	 */
	public static boolean isStreamSize(final int size){
		return size >= 1024 * 1024 * 10;
	}
	
	public IOBuffer(final int size){
		buffer = ByteUtil.byteArrayCacher.getFree(size);
		bufferLen = buffer.length;
	}
	
	public IOBuffer(final IOBufferFileStream stream){
		this.stream = stream;
		isCycleable = false;
	}
	
	public IOBuffer(final byte[] bs){
		buffer = bs;
		bufferLen = buffer.length;
		storeIdx = bufferLen;
		isCycleable = false;
	}
	
	public final void reset(){
		storeIdx = 0;
		readIdx = 0;
	}
	
	public final void recycle(){
		if(isCycleable == false){
			return;
		}
		
		if(buffer != null){
			ByteUtil.byteArrayCacher.cycle(buffer);
			storeIdx = 0;
			buffer = null;
			stream = null;
		}
	}
	
	public final boolean isDataStored(){
		return storeIdx > 0 || stream != null;
	}
	
	public final void load(final InputStream fis){
		final byte[] buf = ByteUtil.byteArrayCacher.getFree(1024);
		
		try{
	        int len;
	        while ((len = fis.read(buf)) > 0) {
	        	write(buf, 0, len);
	        }       
	        fis.close();
		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			ByteUtil.byteArrayCacher.cycle(buf);
		}
	}
	
	public final int write(final byte[] bs){
		return write(bs, 0, bs.length);
	}
	
	public final int write(final byte[] bs, final int offset, final int len){
		final int nextStoreIdx = storeIdx + len;
		if(nextStoreIdx >= bufferLen){
			final int newBufferLen = nextStoreIdx * 2;
			final byte[] newBuffer = ByteUtil.byteArrayCacher.getFree(newBufferLen);
			System.arraycopy(buffer, 0, newBuffer, 0, storeIdx);
			ByteUtil.byteArrayCacher.cycle(buffer);
			buffer = newBuffer;
			bufferLen = newBuffer.length;
		}
		
		System.arraycopy(bs, offset, buffer, storeIdx, len);
		storeIdx = nextStoreIdx;
		
		return nextStoreIdx;
	}
	
	public final int write(final int data){
		if(storeIdx == bufferLen){
			final int newBufferLen = bufferLen * 2;
			final byte[] newBuffer = ByteUtil.byteArrayCacher.getFree(newBufferLen);
			System.arraycopy(buffer, 0, newBuffer, 0, bufferLen);
			ByteUtil.byteArrayCacher.cycle(buffer);
			buffer = newBuffer;
			bufferLen = newBuffer.length;
		}
		buffer[storeIdx++] = (byte)data;
		
		return storeIdx;
	}
	
	public final int read(final byte[] bs, final int offset, final int len){
		final int leftUnread = storeIdx - readIdx;
		final int readLen = (leftUnread > len)?len:leftUnread;
		if(readLen > 0){
			System.arraycopy(buffer, readIdx, bs, offset, readLen);
			readIdx += readLen;
			return readLen;
		}else{
			return 0;
		}
	}
	
	/**
	 * null means fail to read.
	 * @param is
	 * @return
	 */
	public static IOBuffer readFromInputStream(InputStream is) {
	    try {
	        byte[] b = new byte[1024];  
	        int len = 0;
	        IOBuffer iobuffer = new IOBuffer(1024 * 10);
	        while ((len = is.read(b)) != -1) {  
	        	iobuffer.write(b, 0, len);  
	        }  
	        return iobuffer;  
	    } catch (Exception e) {  
	        ExceptionReporter.printStackTrace(e);  
	    } finally {  
	        try {  
	        	if(is != null){
	        		is.close();  
	        	}
	        } catch (IOException e) {  
	        }  
	    }  
	    return null;
	}
}
