package hc.core;

import hc.core.util.ByteUtil;
import hc.core.util.LogManager;

public abstract class UDPPacketResender {
	protected final byte[] UDP_HEADER;
	protected final int IDX_HEADER_1 = MsgBuilder.INDEX_UDP_HEADER;
	protected final int IDX_HEADER_2 = MsgBuilder.INDEX_UDP_HEADER + 1;
	
	public abstract Object getUDPSocket();
	
	public abstract void setUDPTargetAddress(Object address, int port);
	
//	public static boolean isACKCtrlTag(byte ctrlTag){
//		if(ctrlTag >= 8 && ctrlTag <= 100){
//			return true;
//		}
//		return false;
//	}
	
	public UDPPacketResender(final CoreSession coreSocketSession) {
		UDP_HEADER = coreSocketSession.udpHeader;
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("Packet Resender Started");
		for (int i = 0; i < NEED_MAX_SIZE; i++) {
			packetMsgID[i] = -1;
		}
	}
	
	protected int real_len_upd_data = MsgBuilder.UDP_MTU_DATA_MIN_SIZE;
	
	public void setBestUDPLen(int bestUDPLen){
		real_len_upd_data = bestUDPLen;
	}
	
	public void reset(){
		real_len_upd_data = MsgBuilder.UDP_MTU_DATA_MIN_SIZE;
		
		ackReceiveMaxMsgID = 0;
		ackReceiveMinMsgID = -1;
		
		if(ackTop != null){
			Ack next = ackTop.next;
			while(next != null){
				Ack.cycle(ackTop);
				ackTop = next;
				next = ackTop.next;
			}
			Ack.cycle(ackTop);
			ackTop = null;
		}
		
		synchronized (needACK) {
			for (int i = 0, j = 0; j < size && i < NEED_MAX_SIZE; i++) {
				if(packetMsgID[i] != -1){
					j++;
					packetMsgID[i] = -1;
					cacher.cycle(needACK[i]);
					
					//清空以备充入时检查空位之用
					needACK[i] = null;
				}
			}	
			size = 0;
		}
	}
	
	public abstract void sendUDP(byte p_ctrlTag, byte subCtrlTag, byte[] jcip_bs, int offset, int len, int mtuLen, boolean isFlushNow);
	
	public abstract void setUDPSocket(Object sender);
	
	protected abstract void resend(Object packet);
	
	private final int NEED_MAX_SIZE = buidlResendMaxSize();
	
	private static int buidlResendMaxSize(){
		int size;
		try{
			size = Integer.parseInt(RootConfig.getInstance().
				getProperty(RootConfig.p_Packet_Resend_MaxSize));
		}catch (Throwable e) {
			//连接服务器失败
			size = 10240;
		}
		if(IConstant.serverSide){
			return size;
		}else{
			return size / 4;
		}
	}
	private final Object[] needACK = new Object[NEED_MAX_SIZE];
	private final int[] packetMsgID = new int[NEED_MAX_SIZE];
	private final long[] storeMS = new long[NEED_MAX_SIZE];
	private final long[] resendMSArr = new long[NEED_MAX_SIZE];
	private int size;
	private int ackReceiveMaxMsgID = 0, ackReceiveMinMsgID = -1;
	private Ack ackTop = null;
	
	public void printAckLink(){
		Ack t = ackTop;
		while(t != null){
			System.out.print("[" + t.waitStart + "-" + t.waitStart + "],");
			t = t.next;
		}
		System.out.println();
	}
	
	short storeIdx = 0;
	
	public final void needAckAtSend(final Object packet, final int msgID){
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("--- Need Ack : " + msgID);
		
		final long currentTimeMillis = System.currentTimeMillis();

		resenderTimer.setEnable(true);
		
		synchronized (needACK) {
			if((++size) == NEED_MAX_SIZE){
				LogManager.err("Oversize Resender, skip send");
				
				//以下线状态处理，因为过多堆积的包会导致中继等问题
//				SIPManager.notifyRelineon(true);
				
				size--;
				return;
			}
			
			//注意与下段代码同步，以获得高性能
			for (; storeIdx < NEED_MAX_SIZE; storeIdx++) {
				if(needACK[storeIdx] == null){
					needACK[storeIdx] = packet;
					packetMsgID[storeIdx] = msgID;
					storeMS[storeIdx] = currentTimeMillis;
					resendMSArr[storeIdx] = currentTimeMillis - resend_ms;
					return;
				}
			}
			
			//从起始0开始找空位
			storeIdx = 0;
			
			for (; storeIdx < NEED_MAX_SIZE; storeIdx++) {
				if(needACK[storeIdx] == null){
					needACK[storeIdx] = packet;
					packetMsgID[storeIdx] = msgID;
					storeMS[storeIdx] = currentTimeMillis;
					resendMSArr[storeIdx] = currentTimeMillis - resend_ms;
					return;
				}
			}
		}
	}
	
	
	public final void ackAtSend(final byte[] bs, short startIdx, final int endIdx){
//		L.V = L.O ? false : LogManager.log("receiv batch ack byte len : " + (endIdx - startIdx));
		while(startIdx < endIdx){
			final int temp0 = bs[startIdx++] & 0xFF;
			final int temp1 = bs[startIdx++] & 0xFF;
			final int temp2 = bs[startIdx++] & 0xFF;
			final int msgid = ((temp0 << 16) + (temp1 << 8) + temp2);

//			System.out.println("ack at send msgID:" + msgid);
			Object ack = null;
			synchronized (needACK) {
				int tryCount = size;
				for (int i = 0; (tryCount > 0) && (i < NEED_MAX_SIZE); i++) {
					final int tmp_msgID = packetMsgID[i];
					if(tmp_msgID != -1){
						if(tmp_msgID == msgid){
							packetMsgID[i] = -1;
							ack = needACK[i];
							
							//清空以备充入时检查空位之用
							needACK[i] = null;
							size--;
//							hc.core.L.V=hc.core.L.O?false:LogManager.log("ack " + msgid);
							break;
						}
						tryCount--;
					}
				}
			}
			if(ack != null){
				cacher.cycle(ack);
			}
			
		}
	}
	
	/**
	 * 返回0表示第一次确认，并由方法外进行确认消息发送；
	 * 返回1表示以前确认过，并由方法外进行确认消息发送，无需后续逻辑处理，并在方法外进行回收操作
	 * -1表示Oversize而没有签收，返回后，不发送确认消息，无后续逻辑，只回收
	 * @param msgid
	 * @return
	 */
	public final byte ackAtReceive(final int msgid){
//		if(msgid > 3){
//			return -1;
//		}
		
		//将情形多的概率提到最前
		if(ackReceiveMaxMsgID == msgid){
			if(ackTop == null){
				ackReceiveMinMsgID = ackReceiveMaxMsgID;
			}
			ackReceiveMaxMsgID++;
			return 0;
		}else if(ackReceiveMaxMsgID > msgid){
			if(ackReceiveMinMsgID >= msgid){
				return 1;
			}
			if(ackTop != null){
				Ack tmpTop = ackTop;
				Ack preTop = null;
				while(true){
					int llwaitStart = tmpTop.waitStart;
					if(llwaitStart > msgid){
						return 1;
					} else {
						int llwaitEnd = tmpTop.waitEnd;
						if(llwaitStart == msgid){
							if(llwaitEnd == llwaitStart){
								//消除本块
								Ack.cycle(tmpTop);
								tmpTop = tmpTop.next;
								
								if(preTop == null){
									ackTop = tmpTop;
									if(ackTop == null){
										ackReceiveMinMsgID = ackReceiveMaxMsgID - 1;
									}else{
//										msgID:33, ackReceiveMaxMsgID:48, ackReceiveMinMsgID:32
//										33-33, 35-35, 40-40, 42-42, 45-45, 
//										msgID:35, ackReceiveMaxMsgID:48, ackReceiveMinMsgID:32
										ackReceiveMinMsgID = tmpTop.waitStart - 1;
									}
								}else{
									preTop.next = tmpTop;
								}

							}else{
								tmpTop.waitStart++;
								if(tmpTop == ackTop){
									ackReceiveMinMsgID++;
								}
							}
							return 0;
						}else if(llwaitEnd > msgid){
							//分链
							Ack newAck = Ack.getFree();
							newAck.waitStart = msgid + 1;
							newAck.waitEnd = llwaitEnd;
							newAck.next = tmpTop.next;
							tmpTop.next = newAck;
							tmpTop.waitEnd = msgid - 1;
							return 0;
						}else if(llwaitEnd == msgid){
							tmpTop.waitEnd--;
							return 0;
						}else if(tmpTop.next == null){
							return 1;
						}else{
							//转next
							preTop = tmpTop;
							tmpTop = tmpTop.next;
						}
					}
				}
			}else{
				return 1;
			}
		}else{
			//}else if(ackReceiveMaxMsgID < msgid){
			Ack ackTmpl = Ack.getFree();
			ackTmpl.waitStart = ackReceiveMaxMsgID;
			ackTmpl.waitEnd = msgid - 1;
			ackTmpl.next = null;
			ackReceiveMaxMsgID = msgid + 1;
			
			if(ackTop == null){
				ackTop = ackTmpl;
			}else{
				int newMinMsgID = ackReceiveMaxMsgID - p_PacketLossDeepMsgIDNum;
				while(ackTop.waitEnd < newMinMsgID){
//					msgID:4746, ackReceiveMaxMsgID:4746, ackReceiveMinMsgID:1339
//					1340-1340, 3256-3256, 3280-3296,  
					Ack.cycle(ackTop);
					ackTop = ackTop.next;
					if(ackTop == null){
						ackTop = ackTmpl;
						ackReceiveMinMsgID = ackTop.waitStart - 1;
						return 0;
					}else{
						ackReceiveMinMsgID = ackTop.waitStart - 1;
					}
				}
				Ack ackTmp = ackTop;

				Ack next = ackTmp.next;
				while(next != null){
					ackTmp = next;
					next = ackTmp.next;
				}
				ackTmp.next = ackTmpl;
			}
			return 0;
		}
	}
	
	public static void sendUDPREG(final CoreSession coreSS) {
		sendUDPBlockData(coreSS, MsgBuilder.E_TAG_ROOT_UDP_ADDR_REG, MsgBuilder.UDP_MTU_DATA_MIN_SIZE);
	}

	public static boolean checkUDPBlockData(final byte[] bs, final int dataLen) {
		final int endIdx = dataLen + MsgBuilder.INDEX_MSG_DATA;
		for (int i = MsgBuilder.INDEX_MSG_DATA, j = 0; i < endIdx; i++) {
			if(bs[i] == ((byte)(j++))){
				
			}else{
//				L.V = L.O ? false : LogManager.log("Fail index : " + i);
				return false;
			}
		}
		return true;
	}

	public static void sendUDPBlockData(final CoreSession coreSS, final byte ctrlTag, final int dataLen) {
		final int mtuLen = dataLen + MsgBuilder.INDEX_UDP_MSG_DATA;
		final byte[] byte1472 = ByteUtil.byteArrayCacher.getFree(mtuLen);
		for (int i = 0, j = 0; i < dataLen; ) {
			byte1472[i++] = (byte)(j++);
		}
		coreSS.context.udpSender.sendUDP(ctrlTag, MsgBuilder.NULL_CTRL_SUB_TAG, byte1472, 0, dataLen, dataLen, true);
		ByteUtil.byteArrayCacher.cycle(byte1472);
	}

	private final int p_PacketLossDeepMsgIDNum = Integer.parseInt(RootConfig.getInstance().
			getProperty(RootConfig.p_PacketLossDeepMsgIDNum));
	private final int resend_ms = Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_Packet_Resend_MS));
	
	public final DatagramPacketCacher cacher = DatagramPacketCacher.getInstance();
	
	private int getPacketResendMaxTimes(){
		final int time = Integer.parseInt(
				RootConfig.getInstance().getProperty(RootConfig.p_PacketResendMaxTimes));
		return IConstant.serverSide?time:time/4;
	}
	
	public final HCTimer resenderTimer = new HCTimer("Resender", HCTimer.HC_INTERNAL_MS, false){
		final int packet_resend_expired_ms = Integer.parseInt(
				RootConfig.getInstance().getProperty(RootConfig.p_Packet_Resend_Expired_MS));
		
//		final int p_PacketResendMaxTimes = Integer.parseInt(RootConfig.getInstance().
//				getProperty(RootConfig.p_PacketResendMaxTimes));
		final int NUM_EACH_SEND = getPacketResendMaxTimes();
		int lastStartIdx = 0;
		public void doBiz(){
			
			final long currentTimeMillis = System.currentTimeMillis();
//			hc.core.L.V=hc.core.L.O?false:LogManager.log("Unack size:" + size);
			final long expMS = currentTimeMillis - packet_resend_expired_ms;
			final long lastMS = currentTimeMillis - resend_ms;
			int thisStartIdx = lastStartIdx;
			int count = 0, sendCount = 0;
			
			synchronized (needACK) {
				if(size > 0){
					for (; sendCount < NUM_EACH_SEND && count < size && thisStartIdx < NEED_MAX_SIZE; thisStartIdx++) {
						if(packetMsgID[thisStartIdx] != -1){
							if(expMS < storeMS[thisStartIdx]){
								count++;
								if((resendMSArr[thisStartIdx]) <= lastMS){
//									L.V = L.O ? false : LogManager.log("Resend msgid:" + packetMsgID[thisStartIdx] + ", idx:" + thisStartIdx);
									resend(needACK[thisStartIdx]);
									resendMSArr[thisStartIdx] = currentTimeMillis;
									sendCount++;
								}
							}else{
//								L.V = L.O ? false : LogManager.log("throw overtime msgid:" + packetMsgID[thisStartIdx]);
								packetMsgID[thisStartIdx] = -1;

								cacher.cycle(needACK[thisStartIdx]);

								//清空以备充入时检查空位之用
								needACK[thisStartIdx] = null;
								size--;
							}
						}
					}
					if(count < size && thisStartIdx < NEED_MAX_SIZE){
						lastStartIdx = thisStartIdx;
					}else{
						lastStartIdx = 0;
//						L.V = L.O ? false : LogManager.log("=======================To Zero UDP Send. size:" + size);
					}
				}else{
					setEnable(false);
//					L.V = L.O ? false : LogManager.log("===========No data, Disable UDPResender.");
				}
			}
		}
	};
}
