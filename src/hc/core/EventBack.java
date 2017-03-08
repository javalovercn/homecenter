package hc.core;

import hc.core.util.ByteUtil;
import hc.core.util.CUtil;
import hc.core.util.EventBackCacher;
import hc.core.util.LogManager;

public class EventBack implements IWatcher{
	Object datagramPacket;
	CoreSession coreSocketSession;
	byte[] bs;
	int dataLen;
	HCUDPSubPacketEvent subPacket = null;
	private final static EventBackCacher ebCacher = EventBackCacher.getInstance();
	private static final DatagramPacketCacher packetCacher = DatagramPacketCacher.getInstance();
	private static final HCUDPSubPacketCacher eventCacher = HCUDPSubPacketCacher.getInstance();
	private final boolean isInWorkshop = L.isInWorkshop;
	final int IDX_HEADER1 = MsgBuilder.INDEX_UDP_HEADER;
	final int IDX_HEADER2 = MsgBuilder.INDEX_UDP_HEADER + 1;

	private final boolean serverSide = IConstant.serverSide;
	
	public boolean watch() {
		final byte ctrlTag;
		final EventCenter eventCenter = coreSocketSession.eventCenter;
        final HCConnection hcConnection = coreSocketSession.hcConnection;

        if(datagramPacket != null){
			//UDP数据包
			bs = hcConnection.sipContext.getDatagramBytes(datagramPacket);

	        if(bs[IDX_HEADER1] == hcConnection.udpHeader[0] && bs[IDX_HEADER2] == hcConnection.udpHeader[1]){
            }else{
				cancel();
				return true;
            }

			ctrlTag = bs[MsgBuilder.INDEX_CTRL_TAG];
			
			if(isInWorkshop){
				LogManager.log("EventBack Receive ctrlTag : " + ctrlTag + " in session [" + coreSocketSession.hashCode() + "].");
			}
			
			//内容服务Tag必须系统处于服务状态
			//注意：与TCP段同步
			if(serverSide && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN){
				if(coreSocketSession.context.cmStatus == ContextManager.STATUS_SERVER_SELF){
				}else{
					LogManager.log("Invalid statue tag received["+ctrlTag+"]");
					cancel();
					return true;
				}
			}
			
			if(ctrlTag == MsgBuilder.E_TAG_ACK){
				final int len0 = bs[MsgBuilder.INDEX_MSG_LEN_HIGH] & 0xFF;
		    	final int len1 = bs[MsgBuilder.INDEX_MSG_LEN_MID] & 0xFF;
		    	final int len2 = bs[MsgBuilder.INDEX_MSG_LEN_LOW] & 0xFF;

		    	hcConnection.sipContext.resender.ackAtSend(bs, MsgBuilder.INDEX_UDP_MSG_DATA, MsgBuilder.INDEX_UDP_MSG_DATA + ((len0 << 16) + (len1 << 8) + len2));
				cancel();
				return true;
        	}else{
        		hcConnection.sipContext.ackbatchTimer.ack(bs, MsgBuilder.INDEX_MSG_ID_HIGH);

				final int temp0 = bs[MsgBuilder.INDEX_MSG_ID_HIGH] & 0xFF;
				final int temp1 = bs[MsgBuilder.INDEX_MSG_ID_MID] & 0xFF;
				final int temp2 = bs[MsgBuilder.INDEX_MSG_ID_LOW] & 0xFF;
				final int msgID = ((temp0 << 16) + (temp1 << 8) + temp2);
//				LogManager.log("msgID(Send from other):" + msgID + " is Received.");
				
				if(hcConnection.sipContext.resender.ackAtReceive(msgID) != 0){
					//第二次或多次到达，并在第一次已签收，
					//回收，并不做后面逻辑
//					LogManager.log("ReAck msgID:" + msgID);
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
	    		CUtil.superXor(hcConnection, hcConnection.OneTimeCertKey, bs, MsgBuilder.INDEX_UDP_MSG_DATA, dataLen, null, false, true);
	    	}

			if(bs[MsgBuilder.INDEX_PACKET_SPLIT] == MsgBuilder.DATA_PACKET_SPLIT){
				subPacket = eventCacher.getFree();
				
				subPacket.setType(ctrlTag, datagramPacket, bs);
	
				subPacket = UDPMerger.tryFindCompletSplit(subPacket);
				if(subPacket != null){
					bs = subPacket.data_bs;
					final int merLen = HCMessage.getMsgLen(bs);
	
//					LogManager.log("comp split:[" + bs[MsgBuilder.INDEX_CTRL_TAG] + "], dataLen:" + merLen);
					
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
			
			if(isInWorkshop){
				LogManager.log("EventBack Receive ctrlTag : " + ctrlTag + " in session [" + coreSocketSession.hashCode() + "].");
			}
			
			if(serverSide && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN){
				if(coreSocketSession.context.cmStatus == ContextManager.STATUS_SERVER_SELF){
				}else{
					LogManager.log("Invalid statue tag received["+ctrlTag+"]");
					cancel();
					return true;
				}
			}

			if(ctrlTag == MsgBuilder.E_PACKAGE_SPLIT_TCP){
				//TCP合并包
				final int newPackageID = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_TCP_SPLIT_SUB_GROUP_ID);
				if(hcConnection.package_tcp_id != 0 && hcConnection.package_tcp_id != newPackageID){
					LogManager.errToLog("invalid TCP sub package id : " + newPackageID + ", expected id : " + hcConnection.package_tcp_id);
					resetForNextBigData(hcConnection);
//					cancel();
					return true;
				}
				if(hcConnection.package_tcp_id == 0){
					hcConnection.package_tcp_id = newPackageID;
					hcConnection.packaeg_tcp_num = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_TCP_SPLIT_SUB_GROUP_NUM);
					if(isInWorkshop){
						System.out.println("----[Big Msg]-----package tcp id : " + newPackageID + ", num : " + hcConnection.packaeg_tcp_num);
					}
					hcConnection.package_tcp_bs = new byte[MsgBuilder.MAX_LEN_TCP_PACKAGE_SPLIT * hcConnection.packaeg_tcp_num + MsgBuilder.TCP_PACKAGE_SPLIT_EXT_BUF_SIZE];
					hcConnection.packaeg_tcp_appended_num = 0;
					hcConnection.package_tcp_last_store_idx = MsgBuilder.INDEX_MSG_DATA;
					
					for (int i = 0; i < MsgBuilder.INDEX_MSG_DATA; i++) {
						hcConnection.package_tcp_bs[i] = bs[i];
					}
					
					hcConnection.package_tcp_bs[MsgBuilder.INDEX_CTRL_TAG] = bs[MsgBuilder.INDEX_TCP_SPLIT_TAG];
					hcConnection.package_tcp_bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = bs[MsgBuilder.INDEX_TCP_SPLIT_SUB_TAG];
				}
				
				final int eachLen = dataLen - MsgBuilder.LEN_TCP_PACKAGE_SPLIT_DATA_BLOCK_LEN;
				System.arraycopy(bs, MsgBuilder.TCP_SPLIT_STORE_IDX, hcConnection.package_tcp_bs, hcConnection.package_tcp_last_store_idx, eachLen);
				hcConnection.package_tcp_last_store_idx += eachLen;

				if(isInWorkshop){
					System.out.println("----[Big Msg]-----append data tcp id : " + newPackageID + ", num : " + (hcConnection.packaeg_tcp_appended_num + 1) + ", curr len : " + eachLen);
				}
				
				if(++hcConnection.packaeg_tcp_appended_num == hcConnection.packaeg_tcp_num){
					HCMessage.setBigMsgLen(hcConnection.package_tcp_bs, hcConnection.package_tcp_last_store_idx - MsgBuilder.INDEX_MSG_DATA);//还原数据块总长度
					final byte[] snap_bs = hcConnection.package_tcp_bs;
					resetForNextBigData(hcConnection);//先执行，以下下块逻辑可能产生异常
					eventCenter.action(snap_bs[MsgBuilder.INDEX_CTRL_TAG], snap_bs, eventCenter.nestAction);
				}
				
//				cancel();//释放当前块
				return true;
			}else{//普通tcp包，非大消息块
	//			LogManager.log("Receive ctrlTag : " + ctrlTag);
			}
		}
		
		eventCenter.action(ctrlTag, bs, eventCenter.nestAction);
		cancel();//释放当前块
		return true;
	}

	private final void resetForNextBigData(final HCConnection hcConnection) {
		hcConnection.package_tcp_id = 0;
		hcConnection.packaeg_tcp_appended_num = 0;
		hcConnection.package_tcp_bs = null;//释放合并后的块
	}
	
	/**
	 * 
	 * @param dp 如果传入null，则表示为TCP接收的数据，否则为UDP接收的数据，它们存在结构上的差别
	 * @param b
	 * @param len
	 */
	public void setBSAndDatalen(final CoreSession coreSocketSession, final Object dp, final byte[] b, final int len){
		this.coreSocketSession = coreSocketSession;
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
