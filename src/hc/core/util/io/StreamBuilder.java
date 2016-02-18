package hc.core.util.io;

import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.MsgBuilder;
import hc.core.util.ByteUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

public class StreamBuilder {
	public static final Object LOCK = new Object();
	
	private static int currentStreamID = 1;
	
	final static Hashtable inputStreamTable = new Hashtable(10);
	final static Hashtable outputStreamTable = new Hashtable(10);
	
	public static void notifyExceptionForReleaseStreamResources(final IOException exp){
		startCycle(exp, inputStreamTable);
		startCycle(exp, outputStreamTable);
	}
	
	static void startCycle(final IOException exp, final Hashtable hashtable) {
		synchronized (hashtable) {
			if(hashtable.size() > 0){
				final Enumeration e = hashtable.keys();
				while(e.hasMoreElements()){
					final IHCStream stream = (IHCStream)hashtable.get(e.nextElement());
					stream.notifyExceptionAndCycle(exp);
				}
				
				hashtable.clear();
			}
		}
	}
	
	private final static int getStreamID(final Hashtable streamTable){
		int out = currentStreamID++;
		if(currentStreamID == Integer.MAX_VALUE){
			currentStreamID = 1;
		}
		while(streamTable.containsKey(new Integer(out))){
			out = currentStreamID++;
			if(currentStreamID == Integer.MAX_VALUE){
				currentStreamID = 1;
			}
		}
		return out;
	}
	
	public static InputStream buildInputStream(final String className, final byte[] parameter, final int offset, final int len, final boolean isInitial){
		final int streamID;
		final InputStream is;
		
		final Hashtable streamTable = inputStreamTable;
		synchronized(streamTable){
			streamID = getStreamID(streamTable);
			is = new HCInputStream(streamID);
			streamTable.put(new Integer(streamID), is);
		}
		if(isInitial){
			manageRemoteStream(false, streamID, className, parameter, offset, len);
		}
		return is;
	}
	
	public static OutputStream buildOutputStream(final String className, final byte[] parameter, final int offset, final int len, final boolean isInitial){
		final int streamID;
		final OutputStream is;
		
		final Hashtable streamTable = outputStreamTable;
		synchronized(streamTable){
			streamID = getStreamID(streamTable);
			is = new HCOutputStream(streamID);
			streamTable.put(new Integer(streamID), is);
		}
		if(isInitial){
			manageRemoteStream(true, streamID, className, parameter, offset, len);
		}
		return is;
	}
	
	public static final byte[] EMPTY_BS = new byte[0];
	
	public static void manageRemoteStream(final boolean isInputStream, final int streamID, final String className, final byte[] parameter, final int offset, final int len){
		final byte[] classNameBS = ByteUtil.getBytes(className, IConstant.UTF_8);
		final int classNameLen = classNameBS.length;
		final int sendLen = 1 + STREAM_ID_LEN + 1 + classNameLen + 2 + len;
		final byte[] sendBS = ByteUtil.byteArrayCacher.getFree(sendLen);
		
		int offsetIdx = 0;
		sendBS[offsetIdx] = (byte)(isInputStream?1:0);
		offsetIdx += 1;
		ByteUtil.integerToFourBytes(streamID, sendBS, offsetIdx);
		offsetIdx += STREAM_ID_LEN;
		ByteUtil.integerToOneByte(classNameLen, sendBS, offsetIdx);
		offsetIdx += 1;
		System.arraycopy(classNameBS, 0, sendBS, offsetIdx, classNameLen);
		offsetIdx += classNameLen;
		ByteUtil.integerToTwoBytes(len, sendBS, offsetIdx);
		offsetIdx += 2;
		System.arraycopy(parameter, offset, sendBS, offsetIdx, len);
		
		ContextManager.getContextInstance().sendWrap(MsgBuilder.E_STREAM_MANAGE, sendBS, 0, sendLen);
		ByteUtil.byteArrayCacher.cycle(sendBS);
	}
	
	public static final String TAG_CLOSE_STREAM = "_closeStream";
	
	public static final int STREAM_ID_LEN = 4;

	public static Object closeStream(final boolean isInputStream, final int streamid) {
		final Hashtable streamTable = isInputStream?inputStreamTable:outputStreamTable;
		
		synchronized (streamTable) {
			return streamTable.remove(new Integer(streamid));
		}
	}

	public static void notifyCloseRemoteStream(final boolean isInputStream, final int streamID) {
		manageRemoteStream(isInputStream, streamID, TAG_CLOSE_STREAM, EMPTY_BS, 0, 0);
	}
	
}
