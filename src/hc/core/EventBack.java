package hc.core;

import hc.core.sip.ISIPContext;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CUtil;
import hc.core.util.EventBackCacher;
import hc.core.util.LogManager;

public class EventBack implements IWatcher{
	Object datagramPacket;
	byte[] bs;
	int dataLen;
	HCUDPSubPacketEvent subPacket = null;
	private final static EventBackCacher ebCacher = EventBackCacher.getInstance();
	private static final DatagramPacketCacher packetCacher = DatagramPacketCacher.getInstance();
	private static final HCUDPSubPacketCacher eventCacher = HCUDPSubPacketCacher.getInstance();

	final static UDPPacketResender resender = SIPManager.getSIPContext().resender;

	final static ISIPContext isip = SIPManager.getSIPContext();
	final static byte[] udpHeader = IContext.udpHeader;
	final static int IDX_HEADER1 = MsgBuilder.INDEX_UDP_HEADER;
	final static int IDX_HEADER2 = MsgBuilder.INDEX_UDP_HEADER + 1;

	final static AckBatchHCTimer ackbatchTimer = new AckBatchHCTimer("", HCTimer.HC_INTERNAL_MS, false, resender);

	private static byte[] package_tcp_bs;
	private static int package_tcp_id;
	private static int package_tcp_last_store_idx = MsgBuilder.INDEX_MSG_DATA;
	private static int packaeg_tcp_num;
	private static int packaeg_tcp_appended_num;
	private final boolean serverSide = IConstant.serverSide;
	
	public boolean watch() {
		final byte ctrlTag;
		if(datagramPacket != null){
			//UDP数据包
	        bs = isip.getDatagramBytes(datagramPacket);

	        if(bs[IDX_HEADER1] == udpHeader[0] && bs[IDX_HEADER2] == udpHeader[1]){
            }else{
				cancel();
				return true;
            }

			ctrlTag = bs[MsgBuilder.INDEX_CTRL_TAG];
			
//			L.V = L.O ? false : LogManager.log("EventBack Receive ctrlTag : " + ctrlTag);
			
			//内容服务Tag必须系统处于服务状态
			//注意：与TCP段同步
			if(serverSide && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN){
				if(ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF){
				}else{
					L.V = L.O ? false : LogManager.log("Invalid statue tag received["+ctrlTag+"]");
					cancel();
					return true;
				}
			}
			
			if(ctrlTag == MsgBuilder.E_TAG_ACK){
				final int len0 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
		    	final int len1 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
		    	final int len2 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;

				resender.ackAtSend(bs, MsgBuilder.INDEX_UDP_MSG_DATA, MsgBuilder.INDEX_UDP_MSG_DATA + ((len0 << 16) + (len1 << 8) + len2));
				cancel();
				return true;
        	}else{
        		ackbatchTimer.ack(bs, MsgBuilder.INDEX_MSG_ID_HIGH);

				final int temp0 = bs[MsgBuilder.INDEX_MSG_ID_HIGH] & 0xFF;
				final int temp1 = bs[MsgBuilder.INDEX_MSG_ID_MID] & 0xFF;
				final int temp2 = bs[MsgBuilder.INDEX_MSG_ID_LOW] & 0xFF;
				final int msgID = ((temp0 << 16) + (temp1 << 8) + temp2);
//				L.V = L.O ? false : LogManager.log("msgID(Send from other):" + msgID + " is Received.");
				
				if(resender.ackAtReceive(msgID) != 0){
					//第二次或多次到达，并在第一次已签收，
					//回收，并不做后面逻辑
//					L.V = L.O ? false : LogManager.log("ReAck msgID:" + msgID);
					cancel();
					return true;
				}else{
			    	final int len0 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
			    	final int len1 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
			    	final int len2 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;
			    	dataLen = ((len0 << 16) + (len1 << 8) + len2);
				}
        	}

			if(dataLen == 0 || ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN){
	    	}else{
	    		//解密
	    		CUtil.superXor(bs, MsgBuilder.INDEX_UDP_MSG_DATA, dataLen, null, false, true);
	    	}

			if(bs[MsgBuilder.INDEX_PACKET_SPLIT] == MsgBuilder.DATA_PACKET_SPLIT){
				subPacket = eventCacher.getFree();
				
				subPacket.setType(ctrlTag, datagramPacket, bs);
	
				subPacket = UDPMerger.tryFindCompletSplit(subPacket);
				if(subPacket != null){
					bs = subPacket.data_bs;
					final int merLen = HCMessage.getMsgLen(bs);
	
//					L.V = L.O ? false : LogManager.log("comp split:[" + bs[MsgBuilder.INDEX_CTRL_TAG] + "], dataLen:" + merLen);
					
					System.arraycopy(bs, MsgBuilder.INDEX_UDP_MSG_DATA, bs, MsgBuilder.INDEX_MSG_DATA, merLen);
				}else{
					//不作回收，因为提交到tryFindCompletSplit级回收
					//cancel();千万不能调用本方法，因为数据块已记入UDPMerger，由它回收或组状后再回收
					return true;
				}
			}else{
				System.arraycopy(bs, MsgBuilder.INDEX_UDP_MSG_DATA, bs, MsgBuilder.INDEX_MSG_DATA, dataLen);
			}	
		}else{
			//TCP数据包
			ctrlTag = bs[MsgBuilder.INDEX_CTRL_TAG];
			
//			L.V = L.O ? false : LogManager.log("EventBack Receive ctrlTag : " + ctrlTag);

			if(serverSide && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN){
				if(ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF){
				}else{
					L.V = L.O ? false : LogManager.log("Invalid statue tag received["+ctrlTag+"]");
					cancel();
					return true;
				}
			}

			if(ctrlTag == MsgBuilder.E_PACKAGE_SPLIT_TCP){
				//TCP合并包
				final int newPackageID = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_TCP_SPLIT_SUB_GROUP_ID);
				if(package_tcp_id != 0 && package_tcp_id != newPackageID){
					LogManager.errToLog("invalid TCP sub package id : " + newPackageID + ", expected id : " + package_tcp_id);
					resetForNextBigData();
//					cancel();
					return true;
				}
				if(package_tcp_id == 0){
					package_tcp_id = newPackageID;
					packaeg_tcp_num = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_TCP_SPLIT_SUB_GROUP_NUM);
//					System.out.println("----[Big Msg]-----package tcp id : " + newPackageID + ", num : " + packaeg_tcp_num);
					package_tcp_bs = new byte[MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT * packaeg_tcp_num + MsgBuilder.TCP_PACKAGE_SPLIT_EXT_BUF_SIZE];
					packaeg_tcp_appended_num = 0;
					package_tcp_last_store_idx = MsgBuilder.INDEX_MSG_DATA;
					
					for (int i = 0; i < MsgBuilder.INDEX_MSG_DATA; i++) {
						package_tcp_bs[i] = bs[i];
					}
					
					package_tcp_bs[MsgBuilder.INDEX_CTRL_TAG] = bs[MsgBuilder.INDEX_TCP_SPLIT_TAG];
					package_tcp_bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = bs[MsgBuilder.INDEX_TCP_SPLIT_SUB_TAG];
				}
				
				final int eachLen = dataLen - MsgBuilder.LEN_TCP_PACKAGE_SPLIT_DATA_BLOCK_LEN;
				CUtil.superXor(bs, MsgBuilder.TCP_SPLIT_STORE_IDX, eachLen, null, false, true);
				System.arraycopy(bs, MsgBuilder.TCP_SPLIT_STORE_IDX, package_tcp_bs, package_tcp_last_store_idx, eachLen);
				package_tcp_last_store_idx += eachLen;

//				System.out.println("----[Big Msg]-----append data tcp id : " + newPackageID + ", num : " + (packaeg_tcp_appended_num + 1) + ", curr len : " + eachLen);
				
				if(++packaeg_tcp_appended_num == packaeg_tcp_num){
					HCMessage.setBigMsgLen(package_tcp_bs, package_tcp_last_store_idx - MsgBuilder.INDEX_MSG_DATA);//还原数据块总长度
					final byte[] snap_bs = package_tcp_bs;
					resetForNextBigData();//先执行，以下下块逻辑可能产生异常
					EventCenter.action(snap_bs[MsgBuilder.INDEX_CTRL_TAG], snap_bs, EventCenter.nestAction);
				}
				
//				cancel();//释放当前块
				return true;
			}else{//普通tcp包，非大消息块
	//			L.V = L.O ? false : LogManager.log("Receive ctrlTag : " + ctrlTag);
				if(dataLen == 0 || ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN){
		    	}else{
		    		//解密
		    		CUtil.superXor(bs, MsgBuilder.INDEX_MSG_DATA, dataLen, null, false, true);
		    	}
			}
		}
		
		EventCenter.action(ctrlTag, bs, EventCenter.nestAction);
		cancel();
		return true;
	}

	private final void resetForNextBigData() {
		package_tcp_id = 0;
		packaeg_tcp_appended_num = 0;
		package_tcp_bs = null;//释放合并后的块
	}
	
	/**
	 * 
	 * @param dp 如果传入null，则表示为TCP接收的数据，否则为UDP接收的数据，它们存在结构上的差别
	 * @param b
	 * @param len
	 */
	public void setBSAndDatalen(final Object dp, final byte[] b, final int len){
		this.datagramPacket = dp;
		this.bs = b;
		this.dataLen = len;
		
		subPacket = null;
	}

	public void setPara(final Object p) {
		bs = (byte[])p;
	}

	public final void cancel() {
		if(datagramPacket != null){
			if(subPacket != null){
				//此步回收必先于cycle(subPacket)，因为后者可能被重新投入使用而产生脏的subPacket.datagram
				packetCacher.cycle(subPacket.datagram);

				subPacket.releaseUseBlobBs();
				eventCacher.cycle(subPacket);
				subPacket = null;

			}else{
				packetCacher.cycle(datagramPacket);
			}
			datagramPacket = null;
		}else{
			ReceiveServer.recBytesCacher.cycle(bs);
		}
		ebCacher.cycle(this);
	}

	public boolean isCancelable() {
		return true;//专为EventBack之用
	}

}
