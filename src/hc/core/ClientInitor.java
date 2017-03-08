package hc.core;

import hc.core.data.DataNatReqConn;
import hc.core.data.DataPNG;
import hc.core.sip.IPAndPort;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.HCURLUtil;
import hc.core.util.LogManager;
import hc.core.util.io.HCInputStream;
import hc.core.util.io.IHCStream;
import hc.core.util.io.StreamBuilder;

import java.util.Hashtable;

public class ClientInitor {
	public ClientInitor(final IContext ic, final EventCenter eventCenter){
//		new HCTimer("", 1000 * 60, true) {
//			public final void doBiz() {
//				//无论客户端，服务器
//				Runtime.getRuntime().gc();
//			}
//		};
		
		//无论客户端，服务器都设置
		eventCenter.addListener(ic.rootTagListener);
		
		eventCenter.addListener(new IEventHCListener() {
			
			public byte getEventTag() {
				return MsgBuilder.E_ACK_XOR_PACKAGE_ID;
			}
			
			public boolean action(byte[] bs, CoreSession coreSS) {
				final long ackPakcageID = ByteUtil.eightBytesToLong(bs, MsgBuilder.INDEX_MSG_DATA);
				if(coreSS.hcConnection.ackXorPackage(ackPakcageID)){
					L.V = L.WShop ? false : LogManager.log("successful ack XorPackageID : " + ackPakcageID);
				}else{
					LogManager.errToLog("error ack package!");
					SIPManager.notifyLineOff(coreSS, false, false);
				}
				return true;
			}
		});
		
		eventCenter.addListener(new IEventHCListener() {
			
			public final byte getEventTag() {
				return MsgBuilder.E_STREAM_MANAGE;
			}
			
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
//				final int sendLen = 1 + STREAM_ID_LEN + 1 + classNameLen + 2 + len;
				int offsetidx = MsgBuilder.INDEX_MSG_DATA;
				final boolean isInputStream = (bs[offsetidx]==1);
				offsetidx += 1;
				final int streamID = (int)ByteUtil.fourBytesToLong(bs, offsetidx);
				offsetidx += StreamBuilder.STREAM_ID_LEN;
				final int classNameLen = ByteUtil.oneByteToInteger(bs, offsetidx);
				offsetidx += 1;
				final String className = ByteUtil.buildString(bs, offsetidx, classNameLen, IConstant.UTF_8);
				if(className.equals(StreamBuilder.TAG_CLOSE_STREAM)){
					final Object stream = coreSS.streamBuilder.closeStream(isInputStream, streamID);
					if(stream != null && stream instanceof IHCStream){
						((IHCStream)stream).notifyClose();
					}
					return true;
				}
				
				offsetidx += classNameLen;
				final int paraBSLen = ByteUtil.twoBytesToInteger(bs, offsetidx);
				offsetidx +=2;
				final byte[] paraBS = ByteUtil.byteArrayCacher.getFree(paraBSLen);
				System.arraycopy(bs, offsetidx, paraBS, 0, paraBSLen);
				
				coreSS.context.notifyStreamReceiverBuilder(isInputStream, className, streamID, paraBS, 0, paraBSLen);
				ByteUtil.byteArrayCacher.cycle(paraBS);
				return true;
			}
		});
		
		eventCenter.addListener(new IEventHCListener(){
//			long lastReceive = 0;
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				try{
					//要先行关闭，因为有可能会导致新生成的连接被关闭(连接发出端或中继端)
					coreSS.hcConnection.sipContext.closeSocket(coreSS.hcConnection.sipContext.getSocket());
				}catch (final Exception e) {
					
				}

				//循环替补端口
//				int backPort = ((Integer)ContextManager.getContextInstance().doExtBiz(
//						IContext.BIZ_GET_BACK_PORT, null)).intValue();
				
				final DataNatReqConn nat = new DataNatReqConn();
				nat.setBytes(bs);
				
				LogManager.log("The curr relay server will shutdown");
				final String remoteIP = nat.getRemoteIP();
				final int remotePort = nat.getRemotePort();
				LogManager.log("Change relay to [" + remoteIP + ":" + remotePort + "]");
				final byte[] tokenBS = (byte[])coreSS.context.doExtBiz(IContext.BIZ_GET_TOKEN, null);
				
				//原为backPort
				final Object send = SIPManager.sendRegister(coreSS.hcConnection, new IPAndPort(remoteIP, remotePort),
						MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_FIRST, SIPManager.REG_WAITING_MS, tokenBS);
				if(send != null){
					//稍等
					final IWatcher watcher = new IWatcher() {
						long start = System.currentTimeMillis();
						
						//先让client挂上，因为server有可能粘包发生。所以服务器的时间要更大为3000，而客户端为0。
						long waitTime = (IConstant.serverSide?2000:0);
						
						public boolean watch() {
							if((System.currentTimeMillis() - start) >= waitTime){
//								//加时，以等待
								try{
									coreSS.hcConnection.sipContext.deploySocket(coreSS.hcConnection, para);
								}catch (final Exception e) {
									LogManager.log("Fail relay to[watch]." + e.getMessage());
									SIPManager.notifyLineOff(coreSS, false, false);
								}
								return true;
							}
							return false;
						}
						
						Object para;
						public void setPara(final Object p) {
							para = p;
						}
						public void cancel() {
						}
						public boolean isCancelable() {
							return false;
						}
					};
					
					watcher.setPara(send);
					coreSS.eventCenterDriver.addWatcher(watcher);
					
					if(IConstant.serverSide){
						//更新RelayIP
						final String[] paras = {remoteIP, String.valueOf(remotePort)};
						coreSS.context.doExtBiz(IContext.BIZ_CHANGE_RELAY, paras);
					}
				}
				return true;
			}

			public final byte getEventTag() {
				return MsgBuilder.E_TAG_MOVE_TO_NEW_RELAY;
			}});
		
		eventCenter.addListener(new IEventHCListener() {
			public final byte getEventTag() {
				return MsgBuilder.E_TAG_UN_FORWARD_DATA;
			}
			
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				LogManager.log("Un forward data from relay");
				if(IConstant.serverSide){
					if(coreSS.hcConnection.isBuildedUPDChannel
							&& coreSS.hcConnection.isDoneUDPChannelCheck){
						LogManager.log("UDP mode, continue.");
					}else{
						SIPManager.notifyLineOff(coreSS, false, false);
					}
				}
				return true;
			}
		});
		
		eventCenter.addListener(new IEventHCListener() {
			public final byte getEventTag() {
				return MsgBuilder.E_TAG_ONLY_SUB_TAG_MSG;
			}
			
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final byte subTag = bs[MsgBuilder.INDEX_CTRL_SUB_TAG];
//				LogManager.log("Sub msg sub tag:" + subTag);
				if(subTag == MsgBuilder.DATA_SUB_TAG_MSG_MTU_1472){
//					LogManager.log(
//						"UDP 1472 Reached mobile, change mtu:" + ContextManager.getContextInstance().udpSender.real_len_upd_data 
//						+ " to " + MsgBuilder.UDP_MTU_DATA_MAX_SIZE);
					LogManager.log("Find best MTU for UDP");
					coreSS.hcConnection.udpSender.real_len_upd_data = MsgBuilder.UDP_MTU_DATA_MAX_SIZE;
					return true;
				}else if(subTag == MsgBuilder.DATA_SUB_TAG_MSG_UDP_CHECK_ALIVE){
					if(IConstant.serverSide == false){
						if(coreSS.udpAliveMobiDetectTimer == null){
							coreSS.udpAliveMobiDetectTimer = new HCTimer("", 20000, false) {
								public final void doBiz() {
									if(coreSS.context.cmStatus == ContextManager.STATUS_EXIT){
										setEnable(false);
									}else{
										if(coreSS.hcConnection.getUDPController().tryRebuildUDPChannel(coreSS) == false){
											LogManager.log("Fail on UDP-check-alive, notify connect error!");
											SIPManager.notifyLineOff(coreSS, false, false);
											setEnable(false);
										}
									}
								}
							};
						}
						
						//如果是mobi环境
						coreSS.hcConnection.udpSender.
						sendUDP(MsgBuilder.E_TAG_ONLY_SUB_TAG_MSG, MsgBuilder.DATA_SUB_TAG_MSG_UDP_CHECK_ALIVE,
								bs, 0, 0, 0, false);
						coreSS.udpAliveMobiDetectTimer.setEnable(true);
						coreSS.udpAliveMobiDetectTimer.resetTimerCount();
						
//						LogManager.log("Send back udp line watch at RootTagEventHCListener");
					}else{
						//服务器收到mobi回应
						coreSS.context.rootTagListener.setServerReceiveMS(System.currentTimeMillis());
//						LogManager.log("Receive udp line watch at RootTagEventHCListener");
					}
					return true;
				}
				
				return false;
			}
		});

		eventCenter.addListener(new IEventHCListener() {
			public final byte getEventTag() {
				return MsgBuilder.E_STREAM_DATA;
			}
			
			public final boolean action(final byte[] bs, final CoreSession coreSS) {
				final Hashtable inputStreamTable = coreSS.streamBuilder.inputStreamTable;
				
				final int len = HCMessage.getMsgLen(bs);
				final int streamID = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_MSG_DATA);
				Object is;
				final int dataLen = len - StreamBuilder.STREAM_ID_LEN;
				
				synchronized (coreSS.streamBuilder.LOCK) {
					is = inputStreamTable.get(new Integer(streamID));
				}
				if(is != null){
					((HCInputStream)is).appendStream(bs, MsgBuilder.INDEX_MSG_DATA + StreamBuilder.STREAM_ID_LEN, dataLen);
					return true;
				}
				
				return true;
			}
		});
		
		if(IConstant.serverSide == false){
			//客户端
			
			eventCenter.addListener(new IEventHCListener(){
				public final boolean action(final byte[] bs, final CoreSession coreSS) {
					final String cmd = HCMessage.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
//					LogManager.log("======>Receive:" + cmd);
					HCURLUtil.process(coreSS, cmd, coreSS.urlAction);
					return true;
				}

				public final byte getEventTag() {
					return MsgBuilder.E_GOTO_URL;
				}});
			
			eventCenter.addListener(new IEventHCListener() {
				public final byte getEventTag() {
					return MsgBuilder.E_SOUND;
				}
				DataPNG blob = null;
				byte[] soundBS;
				public final boolean action(final byte[] bs, final CoreSession coreSS) {
					if(blob == null){
						blob = new DataPNG();
					}
					
					blob.bs = bs;
					
					final int pngDatalength = blob.getPNGDataLen();
					if(soundBS == null || soundBS.length < pngDatalength){
						soundBS = new byte[pngDatalength];
					}
					
					blob.copyPNGDataOut(pngDatalength, soundBS, 0);

					coreSS.context.doExtBiz(IContext.BIZ_PLAYSOUND, soundBS);
					return true;
				}
			});
		
			eventCenter.addListener(new IEventHCListener() {
				
				public final byte getEventTag() {
					return MsgBuilder.E_TAG_MTU_1472;
				}
				
				public final boolean action(final byte[] bs, final CoreSession coreSS) {
					if(UDPPacketResender.checkUDPBlockData(bs, MsgBuilder.UDP_MTU_DATA_MAX_SIZE)){
//						LogManager.log("Receive Succ MTU 1472");
						coreSS.context.send(
							null, MsgBuilder.E_TAG_ONLY_SUB_TAG_MSG, MsgBuilder.DATA_SUB_TAG_MSG_MTU_1472);
					}else{
//						LogManager.log("Receive Fail MTU 1472");
					}
					return true;
				}
			});

		}//仅添加到客户端
	}
}
