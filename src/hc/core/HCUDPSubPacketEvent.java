package hc.core;

import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;

public class HCUDPSubPacketEvent {
	public HCUDPSubPacketEvent() {
	}

	protected byte type;
	public Object datagram;
	public byte[] data_bs;
	
	public void setType(byte type, Object dg, byte[] data) {
		this.type = type;
		this.datagram = dg;
		this.data_bs = data;
	}

	//如果采用BLOB_BS，则将原存储，移到本处
	private byte[] normalOffBS;
	
	private final static ByteArrayCacher cacher = ByteUtil.byteArrayCacher;

	/**
	 * 不考虑重传等情形，只采用一个大块方案；
	 * 如果并发，则进行等待。
	 * @return
	 */
	public void tryUseBlobBS(int dataLen){
		normalOffBS = data_bs;
		data_bs = cacher.getFree(dataLen);
	}

	public void releaseUseBlobBs(){
		cacher.cycle(data_bs);
		data_bs = normalOffBS;
	}
}
