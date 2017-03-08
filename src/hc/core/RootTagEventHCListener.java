package hc.core;

import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;

public class RootTagEventHCListener extends IEventHCListener{
	final HCConnection hcConnection;
	
	public RootTagEventHCListener(final HCConnection hcConnection){
		this.hcConnection = hcConnection;
	}
	
	public final byte getEventTag() {
		return MsgBuilder.E_TAG_ROOT;
	}
	
	public final long getServerReceiveMS(){
		return hcConnection.receiveMS;
	}
	
	public final void setServerReceiveMS(long ms){
		hcConnection.receiveMS = ms;
	}

	public final boolean action(final byte[] bs, final CoreSession coreSS) {
		final byte subTag = bs[MsgBuilder.INDEX_CTRL_SUB_TAG];
		if(L.isInWorkshop){
			LogManager.log("Root Event , sub tag:" + subTag);
		}
		
		final HCConnection hcConnection1 = coreSS.hcConnection;
		
		if(subTag == MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_RELAY){
			//服务器收到RootRelay回应
			hcConnection1.receiveMS = System.currentTimeMillis();
//			LogManager.log("Receive line watch at RootTagEventHCListener");
			
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_SERVERING){
			if(IConstant.serverSide == false){
				//如果是mobi环境
				coreSS.context.send(MsgBuilder.E_TAG_ROOT, bs, 0);
//				LogManager.log("Send back line watch at RootTagEventHCListener");
			}else{
				//服务器收到mobi回应
				hcConnection1.receiveMS = System.currentTimeMillis();
				if(L.isInWorkshop){
					LogManager.log("Receive line watch at RootTagEventHCListener");
				}
			}
			
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_OS_IN_LOCK){
			if(IConstant.serverSide == false){
				coreSS.context.doExtBiz(IContext.BIZ_NOTIFY_MOBI_IN_LOCK, null);
			}
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_SERVER_IN_DIRECT_MODE){
			if(IConstant.serverSide == false){
				coreSS.context.doExtBiz(IContext.BIZ_NOTIFY_SERVER_IN_DIRECT_MODE, null);
			}
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_SAME_ID_IS_USING){
			String msg = "Same ID is using, try another ID please!";
			LogManager.err(msg);
			coreSS.context.displayMessage("Error", msg, 
					IContext.ERROR, null, 0);
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_UDP_PORT_NOTIFY){
			int port = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
			hcConnection1.setUDPChannelPort(port);
			
			//初始化UDP Header
			hcConnection1.udpHeader[0] = bs[MsgBuilder.INDEX_MSG_DATA + 2];
			hcConnection1.udpHeader[1] = bs[MsgBuilder.INDEX_MSG_DATA + 3];
			
			if(hcConnection1.sipContext.buildUDPChannel(hcConnection1)){	
				LogManager.log("Build UDP Channel to remote port : " + port);
				hcConnection1.udpSender = hcConnection1.sipContext.resender;
				hcConnection1.isDoneUDPChannelCheck = false;
				hcConnection1.isBuildedUPDChannel = true;
				
				//供UDP中继器注册之用，由于UDP中继收到后，不签收，则E_TAG_ROOT_UDP_ADDR_REG会重发，
				//从而间接实现延时和检测通达性，但是重发时间较长，导致接收后于检测到达时间，
				UDPPacketResender.sendUDPREG(hcConnection1);
				
				try{
					//以免某端完全先于对端发送完可通达性包，从而无法实现验证可通达性
					Thread.sleep(ThreadPriorityManager.UI_DELAY_MOMENT);
				}catch (Exception e) {
					
				}
				UDPPacketResender.sendUDPREG(hcConnection1);
				
				if(IConstant.serverSide){
					UDPPacketResender.sendUDPBlockData(hcConnection1, MsgBuilder.E_TAG_MTU_1472, MsgBuilder.UDP_MTU_DATA_MAX_SIZE);
					
//					LogManager.log("Send MTU 1472 Test");
				}
			}
			return true;
		}
		return false;
	}

}
