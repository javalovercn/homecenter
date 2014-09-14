package hc.core;

import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;

public class RootTagEventHCListener extends IEventHCListener{
	public byte getEventTag() {
		return MsgBuilder.E_TAG_ROOT;
	}
	
	//不能初始为0，极端初次条件下可能认为，长时无接收。
	private long receiveMS = System.currentTimeMillis();
	
	public long getServerReceiveMS(){
		return receiveMS;
	}
	
	public void setServerReceiveMS(long ms){
		receiveMS = ms;
	}

	static{
		//发送UDP_ADDRESS_REG包，进行转发器的地址注册，供正常数据转发时所需之地址
		EventCenter.addListener(new IEventHCListener() {
			public byte getEventTag() {
				return MsgBuilder.E_TAG_ROOT_UDP_ADDR_REG;
			}
			
			public boolean action(byte[] bs) {
				//有可能收到，有可能收不到。不作任何处理。仅供UDP中继之用
//				L.V = L.O ? false : LogManager.log("Receive E_TAG_ROOT_UDP_ADDR_REG");
				final boolean isRight = UDPPacketResender.checkUDPBlockData(bs, MsgBuilder.UDP_MTU_DATA_MIN_SIZE);
				if(isRight && (ContextManager.getContextInstance().isDoneUDPChannelCheck == false)){
					L.V = L.O ? false : LogManager.log("Done UDP Channel Check by E_TAG_ROOT_UDP_ADDR_REG");
					
					ContextManager.getContextInstance().isDoneUDPChannelCheck = true;
				}
				return true;
			}
		});
		
	}
	
	public boolean action(final byte[] bs) {
		final byte subTag = bs[MsgBuilder.INDEX_CTRL_SUB_TAG];
//		L.V = L.O ? false : LogManager.log("Root Event , sub tag:" + subTag);
		if(subTag == MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_RELAY){
			//服务器收到RootRelay回应
			receiveMS = System.currentTimeMillis();
//			L.V = L.O ? false : LogManager.log("Receive line watch at RootTagEventHCListener");
			
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_LINE_WATCHER_ON_SERVERING){
			if(IConstant.serverSide == false){
				//如果是mobi环境
				ContextManager.getContextInstance().send(MsgBuilder.E_TAG_ROOT, bs, 0);
//				L.V = L.O ? false : LogManager.log("Send back line watch at RootTagEventHCListener");
			}else{
				//服务器收到mobi回应
				receiveMS = System.currentTimeMillis();
//				L.V = L.O ? false : LogManager.log("Receive line watch at RootTagEventHCListener");
			}
			
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_OS_IN_LOCK){
			if(IConstant.serverSide == false){
				ContextManager.getContextInstance().doExtBiz(IContext.BIZ_NOTIFY_MOBI_IN_LOCK, null);
			}
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_SERVER_IN_DIRECT_MODE){
			if(IConstant.serverSide == false){
				ContextManager.getContextInstance().doExtBiz(IContext.BIZ_NOTIFY_SERVER_IN_DIRECT_MODE, null);
			}
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_SAME_ID_IS_USING){
			String msg = "Same ID is using, try another ID please!";
			LogManager.err(msg);
			ContextManager.getContextInstance().displayMessage("Error", msg, 
					IContext.ERROR, null, 0);
			return true;
		}else if(subTag == MsgBuilder.DATA_ROOT_UDP_PORT_NOTIFY){
			int port = ByteUtil.twoBytesToInteger(bs, MsgBuilder.INDEX_MSG_DATA);
			SIPManager.setUDPChannelPort(port);
			
			//初始化UDP Header
			IContext.udpHeader[0] = bs[MsgBuilder.INDEX_MSG_DATA + 2];
			IContext.udpHeader[1] = bs[MsgBuilder.INDEX_MSG_DATA + 3];
			
			if(SIPManager.getSIPContext().buildUDPChannel()){	
				L.V = L.O ? false : LogManager.log("Build UDP Channel to remote port : " + port);
				ContextManager.getContextInstance().udpSender = SIPManager.getSIPContext().resender;
				ContextManager.getContextInstance().isDoneUDPChannelCheck = false;
				ContextManager.getContextInstance().isBuildedUPDChannel = true;
				
				//供UDP中继器注册之用，由于UDP中继收到后，不签收，则E_TAG_ROOT_UDP_ADDR_REG会重发，
				//从而间接实现延时和检测通达性，但是重发时间较长，导致接收后于检测到达时间，
				UDPPacketResender.sendUDPREG();
				
				try{
					//以免某端完全先于对端发送完可通达性包，从而无法实现验证可通达性
					Thread.sleep(500);
				}catch (Exception e) {
					
				}
				UDPPacketResender.sendUDPREG();
				
				if(IConstant.serverSide){
					UDPPacketResender.sendUDPBlockData(MsgBuilder.E_TAG_MTU_1472, MsgBuilder.UDP_MTU_DATA_MAX_SIZE);
					
//					L.V = L.O ? false : LogManager.log("Send MTU 1472 Test");
				}
			}
			return true;
		}
		return false;
	}

}
