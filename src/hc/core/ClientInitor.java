package hc.core;

import hc.core.data.DataNatReqConn;
import hc.core.data.DataPNG;
import hc.core.sip.IPAndPort;
import hc.core.sip.SIPManager;
import hc.core.util.HCURLUtil;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class ClientInitor {
	public static void doNothing(){
		
	}
	public static final RootTagEventHCListener rootTagListener = new RootTagEventHCListener(); 
	static{
		new HCTimer("", 1000 * 60, true) {
			public void doBiz() {
				//无论客户端，服务器
				System.gc();
			}
		};
		
		//无论客户端，服务器都设置
		EventCenter.addListener(rootTagListener);
		
		EventCenter.addListener(new IEventHCListener(){
//			long lastReceive = 0;
			public boolean action(final byte[] bs) {
				SIPManager.notifyRelayChangeToLineOff();
				
				try{
					//要先行关闭，因为有可能会导致新生成的连接被关闭(连接发出端或中继端)
					SIPManager.getSIPContext().closeSocket(SIPManager.getSIPContext().getSocket());
				}catch (Exception e) {
					
				}

				//循环替补端口
//				int backPort = ((Integer)ContextManager.getContextInstance().doExtBiz(
//						IContext.BIZ_GET_BACK_PORT, null)).intValue();
				
				DataNatReqConn nat = new DataNatReqConn();
				nat.setBytes(bs);
				
				hc.core.L.V=hc.core.L.O?false:LogManager.log("The curr relay server will shutdown");
				String remoteIP = nat.getRemoteIP();
				int remotePort = nat.getRemotePort();
				hc.core.L.V=hc.core.L.O?false:LogManager.log("Change relay to [" + remoteIP + ":" + remotePort + "]");
				
				//原为backPort
				Object send = SIPManager.sendRegister(new IPAndPort(remoteIP, remotePort), MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_FIRST, SIPManager.REG_WAITING_MS);
				if(send != null){
					//稍等
					IWatcher watcher = new IWatcher() {
						long start = System.currentTimeMillis();
						
						//先让client挂上，因为server有可能粘包发生。所以服务器的时间要更大为3000，而客户端为0。
						long waitTime = (IConstant.serverSide?2000:0);
						
						public boolean watch() {
							if((System.currentTimeMillis() - start) >= waitTime){
//								//加时，以等待
								try{
									DataInputStream is = SIPManager.getSIPContext().getInputStream(para);
									DataOutputStream os = SIPManager.getSIPContext().getOutputStream(para);
									
									SIPManager.getSIPContext().deploySocket(para, is, os);
								}catch (Exception e) {
									L.V = L.O ? false : LogManager.log("Fail relay to[watch]." + e.getMessage());
									SIPManager.notifyRelineon(false);
								}
								return true;
							}
							return false;
						}
						
						Object para;
						public void setPara(Object p) {
							para = p;
						}
						public void cancel() {
						}
						public boolean isNotCancelable() {
							return false;
						}
					};
					
					watcher.setPara(send);
					ConditionWatcher.addWatcher(watcher);
					
					if(IConstant.serverSide){
						//更新RelayIP
						String[] paras = {remoteIP, String.valueOf(remotePort)};
						ContextManager.getContextInstance().doExtBiz(IContext.BIZ_CHANGE_RELAY, paras);
					}
				}
				return true;
			}

			public byte getEventTag() {
				return MsgBuilder.E_TAG_MOVE_TO_NEW_RELAY;
			}});
		
		EventCenter.addListener(new IEventHCListener() {
			public byte getEventTag() {
				return MsgBuilder.E_TAG_UN_FORWARD_DATA;
			}
			
			public boolean action(byte[] bs) {
				L.V = L.O ? false : LogManager.log("Un forward data from relay");
				if(IConstant.serverSide){
					if(ContextManager.getContextInstance().isBuildedUPDChannel
							&& ContextManager.getContextInstance().isDoneUDPChannelCheck){
						L.V = L.O ? false : LogManager.log("UDP mode, continue.");
					}else{
						SIPManager.notifyRelineon(false);
					}
				}
				return true;
			}
		});
		
		EventCenter.addListener(new IEventHCListener() {
			public byte getEventTag() {
				return MsgBuilder.E_TAG_ONLY_SUB_TAG_MSG;
			}
			
			HCTimer udpAliveMobiDetectTimer;
			
			public boolean action(byte[] bs) {
				final byte subTag = bs[MsgBuilder.INDEX_CTRL_SUB_TAG];
//				L.V = L.O ? false : LogManager.log("Sub msg sub tag:" + subTag);
				if(subTag == MsgBuilder.DATA_SUB_TAG_MSG_MTU_1472){
//					L.V = L.O ? false : LogManager.log(
//						"UDP 1472 Reached mobile, change mtu:" + ContextManager.getContextInstance().udpSender.real_len_upd_data 
//						+ " to " + MsgBuilder.UDP_MTU_DATA_MAX_SIZE);
					L.V = L.O ? false : LogManager.log("Find best MTU for UDP");
					ContextManager.getContextInstance().udpSender.real_len_upd_data = MsgBuilder.UDP_MTU_DATA_MAX_SIZE;
					return true;
				}else if(subTag == MsgBuilder.DATA_SUB_TAG_MSG_UDP_CHECK_ALIVE){
					if(IConstant.serverSide == false){
						if(udpAliveMobiDetectTimer == null){
							udpAliveMobiDetectTimer = new HCTimer("", 20000, false) {
								public void doBiz() {
									if(ContextManager.cmStatus == ContextManager.STATUS_EXIT){
										setEnable(false);
									}else{
										if(UDPController.tryRebuildUDPChannel() == false){
											L.V = L.O ? false : LogManager.log("Fail on UDP-check-alive, notify connect error!");
											SIPManager.notifyRelineon(false);
											setEnable(false);
										}
									}
								}
							};
						}
						
						//如果是mobi环境
						ContextManager.getContextInstance().udpSender.
						sendUDP(MsgBuilder.E_TAG_ONLY_SUB_TAG_MSG, MsgBuilder.DATA_SUB_TAG_MSG_UDP_CHECK_ALIVE,
								bs, 0, 0, 0, false);
						udpAliveMobiDetectTimer.setEnable(true);
						udpAliveMobiDetectTimer.resetTimerCount();
						
//						L.V = L.O ? false : LogManager.log("Send back udp line watch at RootTagEventHCListener");
					}else{
						//服务器收到mobi回应
						rootTagListener.setServerReceiveMS(System.currentTimeMillis());
//						L.V = L.O ? false : LogManager.log("Receive udp line watch at RootTagEventHCListener");
					}
					return true;
				}
				
				return false;
			}
		});


		
		if(IConstant.serverSide == false){
			//客户端
			
			EventCenter.addListener(new IEventHCListener(){
				IHCURLAction urlAction;
				public boolean action(final byte[] bs) {
					String cmd = Message.getMsgBody(bs, MsgBuilder.INDEX_MSG_DATA);
//					L.V = L.O ? false : LogManager.log("Receive:" + cmd);
					if(urlAction == null){
						urlAction = ContextManager.getContextInstance().getHCURLAction();
					}
					HCURLUtil.process(cmd, urlAction);
					return true;
				}

				public byte getEventTag() {
					return MsgBuilder.E_GOTO_URL;
				}});
			
			EventCenter.addListener(new IEventHCListener() {
				public byte getEventTag() {
					return MsgBuilder.E_SOUND;
				}
				DataPNG blob = null;
				byte[] soundBS;
				public boolean action(final byte[] bs) {
					if(blob == null){
						blob = new DataPNG();
					}
					
					blob.bs = bs;
					
					int pngDatalength = blob.getPNGDataLen();
					if(soundBS == null || soundBS.length < pngDatalength){
						soundBS = new byte[pngDatalength];
					}
					
					blob.copyPNGDataOut(pngDatalength, soundBS, 0);

					ContextManager.getContextInstance().doExtBiz(IContext.BIZ_PLAYSOUND, soundBS);
					return true;
				}
			});
		
			EventCenter.addListener(new IEventHCListener() {
				
				public byte getEventTag() {
					return MsgBuilder.E_TAG_MTU_1472;
				}
				
				public boolean action(byte[] bs) {
					if(UDPPacketResender.checkUDPBlockData(bs, MsgBuilder.UDP_MTU_DATA_MAX_SIZE)){
//						L.V = L.O ? false : LogManager.log("Receive Succ MTU 1472");
						ContextManager.getContextInstance().send(
							null, MsgBuilder.E_TAG_ONLY_SUB_TAG_MSG, MsgBuilder.DATA_SUB_TAG_MSG_MTU_1472);
					}else{
//						L.V = L.O ? false : LogManager.log("Receive Fail MTU 1472");
					}
					return true;
				}
			});

		}
	}
}
