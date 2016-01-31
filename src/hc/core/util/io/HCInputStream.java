package hc.core.util.io;

import hc.core.EventCenter;
import hc.core.HCMessage;
import hc.core.IEventHCListener;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

public class HCInputStream extends InputStream implements IHCStream{
	IOException exception;
	final int streamID;
	byte[] unread;
	long storeEndIdx;
	int offreadidx;
	boolean isEof;
	
	private final static ByteArrayCacher cacher = ByteUtil.byteArrayCacher;
	
	public HCInputStream(final int streamID) {
		this.streamID = streamID;
		
		synchronized (StreamBuilder.LOCK) {
			StreamBuilder.inputStreamTable.put(new Integer(streamID), this);
		}
	}
	
	static{
		EventCenter.addListener(new IEventHCListener() {
			final Hashtable inputStreamTable = StreamBuilder.inputStreamTable;
			
			public final byte getEventTag() {
				return MsgBuilder.E_STREAM_DATA;
			}
			
			public final boolean action(final byte[] bs) {
				final int len = HCMessage.getMsgLen(bs);
				final int streamID = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_MSG_DATA);
				Object is;
				final int dataLen = len - StreamBuilder.STREAM_ID_LEN;
				
				synchronized (StreamBuilder.LOCK) {
					is = inputStreamTable.get(new Integer(streamID));
				}
				if(is != null){
					((HCInputStream)is).appendStream(bs, MsgBuilder.INDEX_MSG_DATA + StreamBuilder.STREAM_ID_LEN, dataLen);
					return true;
				}
				
				return true;
			}
		});
	}
	
	public final void notifyClose(){
		synchronized (this) {
			this.isEof = true;
			//recycleInLock();//可能还有未用完的数据，所以不能调用
			notify();
			return;
		}
	}
	
	public final void appendStream(final byte[] bs, final int offset, final int len){
		if(len == 0){
		}else{
			synchronized (this) {
				if(isclosed || exception != null){
					return;
				}
				if(unread == null){
					unread = cacher.getFree(len);
					System.arraycopy(bs, offset, unread, 0, len);
					storeEndIdx = len;
					offreadidx = 0;
				}else{
					final long nextStoreEndIdx = storeEndIdx + len;
					if(nextStoreEndIdx > unread.length){
						final byte[] nextunread = cacher.getFree((int)nextStoreEndIdx);
						final int leftunreadlen = (int)(storeEndIdx - offreadidx);
						System.arraycopy(unread, offreadidx, nextunread, 0, leftunreadlen);
						System.arraycopy(bs, offset, nextunread, leftunreadlen, len);
						cacher.cycle(unread);
						unread = nextunread;
						storeEndIdx = leftunreadlen + len;
						offreadidx = 0;
					}else{
						System.arraycopy(bs, offset, unread, (int)storeEndIdx, len);
						storeEndIdx += len;
					}
				}
				this.notify();
			}
		}
	}
	
	public final void notifyExceptionAndCycle(final IOException exp){
		synchronized (this) {
			exception = exp;
			notify();
		}
	}

	private final void recycleInLock() {
		if(unread != null){
			cacher.cycle(unread);
			unread = null;
			storeEndIdx = 0;
			offreadidx = 0;
		}
	}
	
	/**
	 * Reads the next byte of data from the input stream. The value byte is returned as an int in the range 0 to 255. 
	 * This method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
	 */
	public int read() throws IOException {
		if(unread != null){
			synchronized (this) {
				final int out = unread[offreadidx++] & 0xFF;//0~255
				if(offreadidx == storeEndIdx){
					cacher.cycle(unread);
					unread = null;
					storeEndIdx = 0;
					offreadidx = 0;
				}
				return out;
			}
		}
		if(isEof){
			return -1;
		}
		synchronized (this) {
			while(true){
				if(isclosed){
					return -1;
				}
				
				if(exception != null){
					throw exception;
				}
				
				if(unread == null){
					try {
						this.wait();
					} catch (final InterruptedException e) {
						e.printStackTrace();
					}
				}
				return read();
			}
		}
	}

	public int read(final byte[] b) throws IOException {
		return read_0(b, 0, b.length);
	}
	
	/**
	 *  or -1 if there is no more data because the end of the stream has been reached.
	 * @param b
	 * @param off
	 * @param len
	 * @return  -1 if there is no more data because the end of the stream has been reached.
	 * @throws IOException
	 */
	private final int read_0(final byte[] b, final int off, final int len) throws IOException {
		synchronized (this) {
			if(unread != null){
				final long calUnreadLen = storeEndIdx - offreadidx;
				
				final int returnLen = (int)((calUnreadLen > len)?len:calUnreadLen);
				System.arraycopy(unread, offreadidx, b, off, returnLen);
				if(returnLen == calUnreadLen){
					cacher.cycle(unread);
					unread = null;
					storeEndIdx = 0;
					offreadidx = 0;
				}else{
					final long unreadLeft = calUnreadLen - returnLen;
					System.arraycopy(unread, offreadidx + returnLen, unread, 0, (int)unreadLeft);
					storeEndIdx = unreadLeft;
					offreadidx = 0;
				}
				return returnLen;
			}else{
				if(isEof){
					return -1;
				}

				if(isclosed){
					return -1;
				}
				
				if(exception != null){
					throw exception;
				}
			}
			return 0;
		}
	}

	public int read(final byte[] b, final int off, final int len) throws IOException {
		return read_0(b, off, len);
	}

	/**
	 * return the actual number of bytes skipped.
	 */
	public long skip(final long n) throws IOException {
		synchronized (this) {
			if(isclosed){
				throw new IOException("IO is closed");
			}
			if(exception != null){
				throw exception;
			}
			
			final long calUnreadLen = storeEndIdx - offreadidx;
			if(n > calUnreadLen){
				final long skip = calUnreadLen;
				cacher.cycle(unread);
				unread = null;
				storeEndIdx = 0;
				offreadidx = 0;
				return skip;
			}else{
				final long skip = n;
				final long left = calUnreadLen - skip;
				System.arraycopy(unread, (int)skip + offreadidx, unread, 0, (int)left);
				storeEndIdx = left;
				offreadidx = 0;
				return skip;
			}
		}
	}

	/**
	 * Returns the number of bytes that can be read
	 */
	public int available() throws IOException {
		synchronized (this) {
			if(isclosed){
				throw new IOException("IO is closed");
			}
			if(exception != null){
				throw exception;
			}
			return (int)storeEndIdx;
		}
	}

	boolean isclosed;
	
	public void close() throws IOException {
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("close HCInputStream : " + streamID);
		}
		synchronized (this) {
			if(exception != null){
				throw exception;
			}
			
			if(isclosed){
				return;
			}
			
			isclosed = true;
			
			recycleInLock();
			
			StreamBuilder.closeStream(true, streamID);
			StreamBuilder.notifyCloseRemoteStream(false, streamID);
		}
	}

	public void mark(final int readlimit) {
	}

	public void reset() throws IOException {
		synchronized (this) {
			if(isclosed){
				throw new IOException("IO is closed");
			}
			if(exception != null){
				throw exception;
			}
		}
	}

	public boolean markSupported() {
		return false;
	}
}