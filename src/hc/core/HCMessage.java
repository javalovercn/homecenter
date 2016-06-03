package hc.core;

import hc.core.util.ByteUtil;
import hc.core.util.ExceptionReporter;

import java.io.UnsupportedEncodingException;

public class HCMessage {
	public String uuid;
	public int BlobGroupID, SplitNO, SplitNum;

	/**
	 * 数据包分块时，专用，以获得高性能
	 * @param bs
	 */
	public final void setFastByte(final byte[] bs){
		int temp0 = bs[MsgBuilder.INDEX_GROUP_ID_HIGH] & 0xFF;
		int temp1 = bs[MsgBuilder.INDEX_GROUP_ID_LOW] & 0xFF;
		BlobGroupID = ((temp0 << 8) + temp1);

		temp0 = bs[MsgBuilder.INDEX_PACKET_SPLIT_NO_HIGH] & 0xFF;
		temp1 = bs[MsgBuilder.INDEX_PACKET_SPLIT_NO_LOW] & 0xFF;
		SplitNO = ((temp0 << 8) + temp1);
		
		temp0 = bs[MsgBuilder.INDEX_PACKET_SPLIT_NUM_HIGH] & 0xFF;
		temp1 = bs[MsgBuilder.INDEX_PACKET_SPLIT_NUM_LOW] & 0xFF;
		SplitNum = ((temp0 << 8) + temp1);
	}
	
	private static final int MAX_GROUP_ID = 65535;
	
	private static int AUTO_GROUP_ID_GROW = 0;
	private static int AUTO_MSG_ID_GROW = 0;
	private static final Object LOCK = new Object();
	
	public static void reset(){
		AUTO_GROUP_ID_GROW = 0;
		AUTO_MSG_ID_GROW = 0;
	}
	
	public final static int getAndSetAutoMsgID(final byte[] bs){
		final int msgID = AUTO_MSG_ID_GROW++;

		bs[MsgBuilder.INDEX_MSG_ID_HIGH] = (byte) ((msgID >>> 16) & 0xFF);
		bs[MsgBuilder.INDEX_MSG_ID_MID] = (byte) ((msgID >>> 8) & 0xFF);
		bs[MsgBuilder.INDEX_MSG_ID_LOW] = (byte) (msgID & 0xFF);
		return msgID;
	}
	
	/**
	 * 如果传入0，则生成一个新序列号
	 * @param groupid
	 * @return
	 */
	public final static int setSplitPara(final byte[] bs, int groupid, final int splitNo, final int splitNum){
		bs[MsgBuilder.INDEX_PACKET_SPLIT_NO_HIGH] = (byte) ((splitNo >>> 8) & 0xFF);
		bs[MsgBuilder.INDEX_PACKET_SPLIT_NO_LOW] = (byte) (splitNo & 0xFF);
			
		bs[MsgBuilder.INDEX_PACKET_SPLIT_NUM_HIGH] = (byte) ((splitNum >>> 8) & 0xFF);
		bs[MsgBuilder.INDEX_PACKET_SPLIT_NUM_LOW] = (byte) (splitNum & 0xFF);
			
		if(groupid == 0){
			synchronized(LOCK){
				groupid = ++AUTO_GROUP_ID_GROW;
				if(AUTO_GROUP_ID_GROW == MAX_GROUP_ID){
					AUTO_GROUP_ID_GROW = 1;
				}
			}
		}
		bs[MsgBuilder.INDEX_GROUP_ID_HIGH] = (byte) ((groupid >>> 8) & 0xFF);
		bs[MsgBuilder.INDEX_GROUP_ID_LOW] = (byte) (groupid & 0xFF);
		return groupid;
	}
	
		
	/**
	 * byte长度，非String的长度
	 * @return
	 */
	public final static int getMsgLen(final byte[] bs){
		//注意:
		//此逻辑与BizServer和SendServer下解密逻辑块保持同步
		//为了获得高性能，采用代码冗余方式
		int temp0 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
		int temp1 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
		int temp2 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;
		return ((temp0 << 16) + (temp1 << 8) + temp2);
	}
	
//	public final int getLenAtByteLevel(byte[] bs){
//		return getMsgLen(bs) + MsgBuilder.INDEX_MSG_DATA;
//	}
	
	public final static void setMsgLen(final byte[] bs, final int len){
		bs[MsgBuilder.INDEX_MSG_LEN_HIGH] = (byte) ((len >>> 16) & 0xFF);
		bs[MsgBuilder.INDEX_MSG_LEN_MID] = (byte) ((len >>> 8) & 0xFF);		
		bs[MsgBuilder.INDEX_MSG_LEN_LOW] = (byte) (len & 0xFF);		
	}
	
	public final static int getBigMsgLen(final byte[] bs){
		final int temp0 = bs[MsgBuilder.INDEX_CTRL_SUB_TAG] & 0xFF;
		final int temp1 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
		final int temp2 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
		final int temp3 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;
		return ((temp0 << 24) + (temp1 << 16) + (temp2 << 8) + temp3);
	}

	public final static void setBigMsgLen(final byte[] bs, final int len){
		bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = (byte) ((len >>> 24) & 0xFF);
		bs[MsgBuilder.INDEX_MSG_LEN_HIGH] = (byte) ((len >>> 16) & 0xFF);
		bs[MsgBuilder.INDEX_MSG_LEN_MID] = (byte) ((len >>> 8) & 0xFF);		
		bs[MsgBuilder.INDEX_MSG_LEN_LOW] = (byte) (len & 0xFF);		
	}
	
	public final static void setMsgBody(byte[] bs, String body){
		byte[] bb;
		try {
			bb = body.getBytes(IConstant.UTF_8);
			int length = bb.length;
			setMsgLen(bs, length);
			
			int bs_index = MsgBuilder.INDEX_MSG_DATA;
			for (int i = 0; i < length; i++) {
				bs[bs_index++] = bb[i]; 
			}
		} catch (UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
	/**
	 * 将超长型body其中的一个片断放入发送体中
	 * @param bs 消息体BS
	 * @param storeIdx 存入bs的起始位置
	 * @param src
	 * @param offset
	 * @param len
	 */
	public final static void setMsgBody(final byte[] bs, final int storeIdx, final byte[] src, final int offset, final int len){
		setMsgLen(bs, len);
		System.arraycopy(src, offset, bs, storeIdx, len);
	}
	
	public final static void setMsgTcpSplitCtrlData(final byte[] bs, short storeIdx, final byte ctrlTag, final int group_id, final int package_no){
		bs[storeIdx] = ctrlTag;
//		bs[storeIdx++] = subTag;
		
		ByteUtil.integerToFourBytes(group_id, bs, storeIdx + 2);
		ByteUtil.integerToFourBytes(package_no, bs, storeIdx + 6);
	}
	
	public static final String getMsgBody(final byte[] bs, final int storeIdx){
		final int len = getMsgLen(bs);
		try {
			return new String(bs, storeIdx, len, IConstant.UTF_8);
		} catch (UnsupportedEncodingException e) {
			ExceptionReporter.printStackTrace(e);
		}
		return new String(bs, storeIdx, len);
	}	
}
