package hc.core;

public class UDPMerger {
	private static int getReceiveSplitMaxSize() {
		final int max = Integer.parseInt(RootConfig.getInstance()
				.getProperty(RootConfig.p_Receive_Split_Max_Size));
		return IConstant.serverSide ? max / 4 : max;
	}

	/**
	 * 最大分包缓存数要考虑以下几个因素： 1.由于采用服务器小块分传技术，会降低对一次大包的要求。
	 * 2.服务器采用图片间相差发现，从而只传送变化的数据块，以降低了分包的大小。
	 * 3.重传技术和缓存丢弃技术会导致缓存的包会存在较长的时间，从而增加了缓存空间的要求。 4.不同硬件环境可提供的硬件区别较大。
	 * 5.降低传送频度，可以减少缓存空间要求。
	 */
	private static final int SPLIT_MAX_SIZE = getReceiveSplitMaxSize();
	private static final HCUDPSubPacketEvent[] SPLIT_HCEVENT = new HCUDPSubPacketEvent[SPLIT_MAX_SIZE];
	private static final HCMessage[] SPLIT_MSG = new HCMessage[SPLIT_MAX_SIZE];
	private static final long[] STORE_TIME = new long[SPLIT_MAX_SIZE];
	private static final long THROW_TIME = Integer.parseInt(RootConfig
			.getInstance().getProperty(RootConfig.p_Receive_Split_Throw_MS));
	private static int STORE_SPLIT_NUM = 0;

	private static final Object LOCK = new Object();
	private static final HCUDPSubPacketEvent[] TMP_SPLIT_EVENTS = new HCUDPSubPacketEvent[SPLIT_MAX_SIZE];
	private static final int[] TMP_SPLIT_EVENTS_INDEX = new int[SPLIT_MAX_SIZE];
	private static final HCMessageBuffer msgCacher = HCMessageBuffer
			.getInstance();

	/**
	 * 检查是否已完整齐备，如果完备，则进行合并，并返回合并后的HCEvent。
	 */
	public static HCUDPSubPacketEvent tryFindCompletSplit(
			HCUDPSubPacketEvent eventNew) {
		synchronized (LOCK) {
			if (STORE_SPLIT_NUM == SPLIT_MAX_SIZE) {
				return null;
			}
			HCMessage msgNew = msgCacher.getFree();
			msgNew.setFastByte(eventNew.data_bs);
			// System.out.println("Enter blob msgID:" + SPLIT_MSG.getMsgID() +
			// ", no:" + SPLIT_MSG.getSplitNO() + ", store Num:" +
			// STORE_SPLIT_NUM);
			int splitBlockNum = msgNew.SplitNum;
			if (splitBlockNum <= (STORE_SPLIT_NUM + 1)) {
				// 数量上满足，但是有可能分属多个MsgID
				int currGroupID = msgNew.BlobGroupID;
				for (int i = 0; i < splitBlockNum; i++) {
					TMP_SPLIT_EVENTS[i] = null;
					TMP_SPLIT_EVENTS_INDEX[i] = -1;
				}
				int splitNO = msgNew.SplitNO;
				// System.out.println("Try full msgID:" + SPLIT_MSG.getMsgID() +
				// ", no:" + SPLIT_MSG.getSplitNO());
				TMP_SPLIT_EVENTS[splitNO] = eventNew;
				// TMP_SPLIT_MSG[splitNO] =
				// msgNew;//由于MSG和Event具有相同索引，故不建TMP_SPLIT_MSG

				// 不入缓存的，表示新参数传入，且不在SPLIT_EVENTS之列，释放时以示区别
				// TMP_SPLIT_EVENTS_INDEX[splitNO] = -1;

				int findPacketNum = 1;
				for (int i = 0, findEvent = 0; findEvent < STORE_SPLIT_NUM
						&& i < SPLIT_MAX_SIZE; i++) {
					HCUDPSubPacketEvent event = SPLIT_HCEVENT[i];
					if (event == null) {
						continue;
					} else {
						findEvent++;
						HCMessage message = SPLIT_MSG[i];
						if (message.BlobGroupID == currGroupID) {
							splitNO = message.SplitNO;
							TMP_SPLIT_EVENTS[splitNO] = event;
							TMP_SPLIT_EVENTS_INDEX[splitNO] = i;// 入缓存的。以备合并成功后，清除之用
							if ((++findPacketNum) == splitBlockNum) {
								// 全部找集
								// System.out.println("Full splitBlockNum:" +
								// splitBlockNum + ", MsgID:" + currMsgID);
								// 取第一个
								HCUDPSubPacketEvent eventTotal = TMP_SPLIT_EVENTS[0];
								byte[] splitBS = eventTotal.data_bs;
								final int splitPackageMaxDataSize = HCMessage
										.getMsgLen(splitBS);

								int totalSize = splitBlockNum
										* splitPackageMaxDataSize
										+ MsgBuilder.INDEX_UDP_MSG_DATA;
								eventTotal.tryUseBlobBS(totalSize);
								byte[] totalbs = eventTotal.data_bs;
								int totalInIndex = splitPackageMaxDataSize
										+ MsgBuilder.INDEX_UDP_MSG_DATA;
								System.arraycopy(splitBS, 0, totalbs, 0,
										totalInIndex);

								// 取出从第二个开始的后继
								int splitBlockNumMinusOne = splitBlockNum - 1;
								for (int j = 1; j < splitBlockNum; j++) {
									event = TMP_SPLIT_EVENTS[j];
									splitBS = event.data_bs;
									int copyLen;
									if (j < splitBlockNumMinusOne) {
										copyLen = splitPackageMaxDataSize;
									} else {
										copyLen = HCMessage.getMsgLen(splitBS);
									}
									try {
										System.arraycopy(splitBS,
												MsgBuilder.INDEX_UDP_MSG_DATA,
												totalbs, totalInIndex, copyLen);
									} catch (Exception e) {
										System.err.println("Desc:"
												+ totalInIndex + ", copyLen:"
												+ copyLen);
										return null;
									}
									totalInIndex += copyLen;
								}

								// 更新合并后的真实长度
								HCMessage.setMsgLen(totalbs, totalInIndex
										- MsgBuilder.INDEX_UDP_MSG_DATA);

								// 合并拼接完成，清空已使用的
								STORE_SPLIT_NUM -= splitBlockNumMinusOne;

								// 不回收，但要参与清空逻辑
								// SPLIT_HCEVENT[TMP_SPLIT_EVENTS_INDEX[0]] =
								// null;

								for (int k = 0; k < splitBlockNum; k++) {
									// 注意：必须K=1开始，因为第一个不能被回收
									int t_index = TMP_SPLIT_EVENTS_INDEX[k];
									// System.out.println("Cmplit:" + t_index);
									if (t_index != -1) {
										HCUDPSubPacketEvent event_split = SPLIT_HCEVENT[t_index];
										if (eventTotal == event_split) {
											// 需要返回的，不进行回收
											SPLIT_HCEVENT[t_index] = null;

										} else {
											packetCacher.cycle(
													event_split.datagram);
											eventCacher.cycle(event_split);

											SPLIT_HCEVENT[t_index] = null;
										}
										msgCacher.cycle(SPLIT_MSG[t_index]);
										// SPLIT_MSG[t_index] = null;//不需要置null
									} else {
										// 新传入，且不入SPLIT_HCEVENT数组的
										HCUDPSubPacketEvent event_total_tmp = TMP_SPLIT_EVENTS[k];
										if (eventTotal == event_total_tmp) {
											// 新入，且最后一个到达，且分包号为0号，即第一个分包
										} else {
											packetCacher.cycle(
													event_total_tmp.datagram);
											eventCacher.cycle(event_total_tmp);
										}

										// 传入，且不入SPLIT_MSG，故要回收
										msgCacher.cycle(msgNew);
									}
								}

								// for (int j = 0; j < SPLIT_MAX_SIZE; j++) {
								// if(SPLIT_HCEVENT[j] != null){
								// SPLIT_MSG.setByte(SPLIT_HCEVENT[j].getByteArr());
								// System.out.println("Unused split hcevent
								// msgID:" + SPLIT_MSG.getMsgID() + ", no:" +
								// SPLIT_MSG.getSplitNO() );
								// }
								// }
								return eventTotal;
							} else {
								continue;
							}
						} else {
							continue;
						}
					}
				}
			}
			// 不够数
			for (int i = 0; i < SPLIT_MAX_SIZE; i++) {
				if (SPLIT_HCEVENT[i] == null) {
					// 入缓存，以备完整时之用
					// System.out.println("Push Temp Store blob msgID:" +
					// SPLIT_MSG.getMsgID() + ", no:" + SPLIT_MSG.getSplitNO() +
					// ", store Num:" + STORE_SPLIT_NUM);
					SPLIT_HCEVENT[i] = eventNew;
					SPLIT_MSG[i] = msgNew;
					STORE_TIME[i] = System.currentTimeMillis();
					STORE_SPLIT_NUM++;
					return null;
				}
			}
		}
		return null;
	}

	// /**
	// * 注意与EventCenter中下cycle的区别：少调用了一个方法
	// * @param event
	// */
	// public static void cycle(HCEvent event) {
	// Object datagram = event.getDatagram();
	// //不允许为空的情形出现，
	// //关闭原如Line_off
	// packetCacher.cycle(datagram);
	// eventCacher.cycle(event);
	// }

	private static void releaseUnusedSplit() {
		final long now = System.currentTimeMillis() - THROW_TIME;

		synchronized (LOCK) {
			if (STORE_SPLIT_NUM > 0) {
				for (int i = 0; i < SPLIT_MAX_SIZE; i++) {
					HCUDPSubPacketEvent event = SPLIT_HCEVENT[i];
					if (event != null && now > STORE_TIME[i]) {
						packetCacher.cycle(event.datagram);
						eventCacher.cycle(event);
						msgCacher.cycle(SPLIT_MSG[i]);

						// LogManager.log("Unused Split");

						SPLIT_HCEVENT[i] = null;
						SPLIT_MSG[i] = null;
						STORE_SPLIT_NUM--;
					}
				}
			}
		}
	}

	private static final HCUDPSubPacketCacher eventCacher = HCUDPSubPacketCacher
			.getInstance();
	private static final DatagramPacketCacher packetCacher = DatagramPacketCacher
			.getInstance();

	public static HCTimer HTReleaseUnusedSplit = new HCTimer(
			"ReleaseUnusedSplit", 1000, true) {
		public final void doBiz() {
			releaseUnusedSplit();
		}
	};
}
