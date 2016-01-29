package hc.core;

import hc.core.util.LogManager;

public class AckBatchHCTimer extends HCTimer {
	final int msgIDLen = 3;
	final int MAX_LEN = 2048;
	final int MAX_STORE_IDX = MAX_LEN / msgIDLen * msgIDLen;
	final byte[] ackIDS = new byte[MAX_LEN];
	int storeIdx;
	final UDPPacketResender resender;
	final int batchByteLen = (MsgBuilder.UDP_INTERNET_MIN_MSS - 40) / msgIDLen * msgIDLen;
	
	public AckBatchHCTimer(String name, int ms, boolean enable, UDPPacketResender resend) {
		super(name, ms, enable);
		this.resender = resend;
	}

	public final void doBiz() {
		//由于本逻辑完全被EventBack包围，且共属于HCTimer，所以关闭锁，ack方法同本
//		synchronized (ackIDS) {
			while(storeIdx > batchByteLen){
				storeIdx -= batchByteLen;
//				L.V = L.O ? false : LogManager.log("Send bat len : " + batchByteLen);
				resender.sendUDP(MsgBuilder.E_TAG_ACK, MsgBuilder.NULL_CTRL_SUB_TAG, ackIDS, 
						storeIdx, batchByteLen, 0, true);
			}
			if(storeIdx != 0){
				resender.sendUDP(MsgBuilder.E_TAG_ACK, MsgBuilder.NULL_CTRL_SUB_TAG, ackIDS, 
						0, storeIdx, 0, true);
				storeIdx = 0;
			}
			setEnable(false);
//		}
	}
	
	public void ack(final byte[] bs, int startIdx){
//		synchronized(ackIDS){
			if(storeIdx == MAX_STORE_IDX){
				L.V = L.O ? false : LogManager.log("Warning AckBatchHCTimer oversize!");
				return;
			}
			
			for (final int endStoreIdx = storeIdx + msgIDLen; storeIdx < endStoreIdx; ) {
				ackIDS[storeIdx++] = bs[startIdx++];
			}
//		}
		setEnable(true);
	}

}
