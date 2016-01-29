package hc.core.util.io;

import hc.core.util.ByteUtil;

public class IOBuffer {
	byte[] buffer;
	int storeIdx;
	int bufferLen;
	
	public IOBuffer(final int size){
		buffer = ByteUtil.byteArrayCacher.getFree(size);
		bufferLen = buffer.length;
	}
	
	public final void release(){
		if(buffer != null){
			ByteUtil.byteArrayCacher.cycle(buffer);
			buffer = null;
		}
	}
	
	public final int writeIn(final byte[] bs, final int offset, final int len){
		final int nextStoreIdx = storeIdx + len;
		if(nextStoreIdx >= bufferLen){
			final int newBufferLen = nextStoreIdx * 2;
			final byte[] newBuffer = ByteUtil.byteArrayCacher.getFree(newBufferLen);
			System.arraycopy(buffer, 0, newBuffer, 0, bufferLen);
			ByteUtil.byteArrayCacher.cycle(buffer);
			buffer = newBuffer;
			bufferLen = newBufferLen;
		}
		
		System.arraycopy(bs, offset, buffer, storeIdx, len);
		storeIdx = nextStoreIdx;
		
		return nextStoreIdx;
	}
	
	public final int writeIn(final int data){
		if(storeIdx == bufferLen){
			final int newBufferLen = bufferLen * 2;
			final byte[] newBuffer = ByteUtil.byteArrayCacher.getFree(newBufferLen);
			System.arraycopy(buffer, 0, newBuffer, 0, bufferLen);
			ByteUtil.byteArrayCacher.cycle(buffer);
			buffer = newBuffer;
			bufferLen = newBufferLen;
		}
		buffer[storeIdx++] = (byte)data;
		
		return storeIdx;
	}
	
	public final int readOut(final byte[] bs, final int offset, final int len){
		final int readLen = (storeIdx > len)?len:storeIdx;
		if(readLen > 0){
			System.arraycopy(buffer, 0, bs, offset, readLen);
			final int leftUnread = storeIdx - readLen;
			if(leftUnread > 0){
				System.arraycopy(buffer, readLen, buffer, 0, leftUnread);
			}
			storeIdx = leftUnread;
		}
		return readLen;
	}
}
