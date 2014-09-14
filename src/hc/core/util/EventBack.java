package hc.core.util;

import hc.core.AckBatchHCTimer;
import hc.core.ContextManager;
import hc.core.DatagramPacketCacher;
import hc.core.EventCenter;
import hc.core.HCTimer;
import hc.core.HCUDPSubPacketCacher;
import hc.core.HCUDPSubPacketEvent;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.Message;
import hc.core.MsgBuilder;
import hc.core.ReceiveServer;
import hc.core.UDPMerger;
import hc.core.UDPPacketResender;
import hc.core.sip.ISIPContext;
import hc.core.sip.SIPManager;

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
			
//			L.V = L.O ? false : LogManager.log("Receive ctrlTag : " + ctrlTag);
			
			//内容服务Tag必须系统处于服务状态
			//注意：与TCP段同步
			if(IConstant.serverSide && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN){
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
	    		CUtil.superXor(bs, MsgBuilder.INDEX_UDP_MSG_DATA, dataLen, CUtil.OneTimeCertKey, false);
	    	}

			if(bs[MsgBuilder.INDEX_PACKET_SPLIT] == MsgBuilder.DATA_PACKET_SPLIT){
				subPacket = eventCacher.getFree();
				
				subPacket.setType(ctrlTag, datagramPacket, bs);
	
				subPacket = UDPMerger.tryFindCompletSplit(subPacket);
				if(subPacket != null){
					bs = subPacket.data_bs;
					final int merLen = Message.getMsgLen(bs);
	
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
			
//			L.V = L.O ? false : LogManager.log("Receive ctrlTag : " + ctrlTag);

			if(IConstant.serverSide && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN){
				if(ContextManager.cmStatus == ContextManager.STATUS_SERVER_SELF){
				}else{
					L.V = L.O ? false : LogManager.log("Invalid statue tag received["+ctrlTag+"]");
					cancel();
					return true;
				}
			}

			if(dataLen == 0 || ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN){
	    	}else{
	    		//解密
	    		CUtil.superXor(bs, MsgBuilder.INDEX_MSG_DATA, dataLen, CUtil.OneTimeCertKey, false);
	    	}
		}
		
		EventCenter.action(ctrlTag, bs);
		cancel();
		return true;
	}
	
	/**
	 * 
	 * @param dp 如果传入null，则表示为TCP接收的数据，否则为UDP接收的数据，它们存在结构上的差别
	 * @param b
	 * @param len
	 */
	public void setBSAndDatalen(Object dp, byte[] b, int len){
		this.datagramPacket = dp;
		this.bs = b;
		this.dataLen = len;
		
		subPacket = null;
	}

	public void setPara(Object p) {
		bs = (byte[])p;
	}

	public void cancel() {
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

	public boolean isNotCancelable() {
		return false;
	}

}
