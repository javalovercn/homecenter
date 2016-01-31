package hc.core.util.io;

import hc.core.ContextManager;
import hc.core.FastSender;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;

import java.io.IOException;
import java.io.OutputStream;

public class HCOutputStream extends OutputStream implements IHCStream{
	IOException exception;
	final int streamID;
	final FastSender fastSender = ContextManager.getContextInstance().getFastSender();
	final int MAX_BLOCK_LEN = MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT;
	final static ByteArrayCacher cacher = ByteUtil.byteArrayCacher;
	boolean isclosed;

	public HCOutputStream(final int streamID) {
		this.streamID = streamID;
		
		synchronized (StreamBuilder.LOCK) {
			StreamBuilder.outputStreamTable.put(new Integer(streamID), this);
		}
	}
	
	public final void notifyExceptionAndCycle(final IOException exp){
		synchronized (this) {
			exception = exp;
		}
	}
	
	public final void notifyClose(){
		synchronized (this) {
			isclosed = true;
		}
	}
	
	byte[] oneByte;
	
	public void write(final int arg0) throws IOException {
		synchronized (this) {
			if(oneByte == null){
				oneByte = new byte[1];
			}
			oneByte[0] = (byte)arg0;
			write(oneByte, 0, 1);
		}
	}

	public void write(final byte[] b) throws IOException{
		write_0(b, 0, b.length);
	}

	final void write_0(final byte[] b, int offset, final int len) throws IOException{
		synchronized (this) {
			if(exception != null){
				throw exception;
			}
			if(isclosed){
				throw new IOException("IO is closed");
			}
			
			final int endIdx = offset + len;
			while(offset < endIdx){
				int sendBlockLen = endIdx - offset;
				sendBlockLen = (sendBlockLen > MAX_BLOCK_LEN)?MAX_BLOCK_LEN:sendBlockLen;
				
				{
					final int flushLen = sendBlockLen + StreamBuilder.STREAM_ID_LEN;
					final byte[] flushBS = cacher.getFree(flushLen);
					ByteUtil.integerToFourBytes(streamID, flushBS, 0);
					System.arraycopy(b, offset, flushBS, StreamBuilder.STREAM_ID_LEN, sendBlockLen);
					fastSender.sendWrapAction(MsgBuilder.E_STREAM_DATA, flushBS, 0, flushLen);
				}
				
				offset += sendBlockLen;
			}
		}
	}

	public void write(final byte[] b, final int off, final int len) throws IOException{
		write_0(b, off, len);
	}
	
	public void flush() throws IOException{
	}
	
	public void close() throws IOException{
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("close HCOutputStream : " + streamID);
		}
		
		synchronized (this) {
			if(exception != null){
				throw exception;
			}
			
			if(isclosed){
				return;
			}
			
			flush();

			isclosed = true;

			StreamBuilder.closeStream(false, streamID);
			StreamBuilder.notifyCloseRemoteStream(true, streamID);
		}
	}
}