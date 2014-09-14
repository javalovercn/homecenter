package hc.core;

import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;

public class UDPController {
	static int tryBuildUDPtimers = 0;
	static long lastCallMS = 0;
	
	public static synchronized boolean tryRebuildUDPChannel(){
		if(tryBuildUDPtimers < 2){
			final long now = System.currentTimeMillis();
			if(tryBuildUDPtimers == 0){
				if(now - lastCallMS < 10000){
					L.V = L.O ? false : LogManager.log("It seem error cycle build UDP , skip build process.");
					return false;
				}
			}else{
				if(now - lastCallMS > 100000){
					tryBuildUDPtimers = 0;
				}
			}
			
			if(ContextManager.cmStatus == ContextManager.STATUS_EXIT){
				//暂未考虑服务器端发生UDP自动关闭时，及再仅手机断开后，重联引发的问题
				return false;
			}
			
			lastCallMS = now;
			tryBuildUDPtimers++;
			L.V = L.O ? false : LogManager.log("Fail on UDP-check-alive, rebuild UDP connection. try " + tryBuildUDPtimers + " times.");

			SIPManager.getSIPContext().tryRebuildUDPChannel();
			return true;
		}
		tryBuildUDPtimers = 0;
		return false;
	}
	
	public static final int UDP_RANDOM_HEADER_STARD_IDX = MsgBuilder.LEN_UDP_CONTROLLER_HEAD + 1;
	public static final int UUID_STARD_IDX = UDP_RANDOM_HEADER_STARD_IDX + MsgBuilder.LEN_UDP_HEADER;
	
	public static int getSetNullAddCmdLen(){
		return UDPController.UUID_STARD_IDX + IConstant.uuidBS.length;
	}
	
	public static void fillSetNullAddrCmdData(final byte[] bs){

		//填充UDP_TAG
		ByteUtil.integerToTwoBytes(MsgBuilder.E_UDP_CONTROLLER_SET_ADDR_NULL, bs, 0);

		//填充服务器或客户机标识
		bs[MsgBuilder.LEN_UDP_CONTROLLER_HEAD] = (byte)(IConstant.serverSide?1:0);
		
		//填充随机凭证号
		bs[UDP_RANDOM_HEADER_STARD_IDX] = IContext.udpHeader[0];
		bs[UDP_RANDOM_HEADER_STARD_IDX + 1] = IContext.udpHeader[1];
		
		//填充UUID数据
		for (int i = 0; i < IConstant.uuidBS.length; i++) {
			bs[i + UUID_STARD_IDX] = IConstant.uuidBS[i];
		}
	}
}
