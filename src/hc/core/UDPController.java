package hc.core;

import hc.core.util.ByteUtil;
import hc.core.util.LogManager;

public class UDPController {
	int tryBuildUDPtimers = 0;
	long lastCallMS = 0;
	
	public synchronized boolean tryRebuildUDPChannel(final CoreSession coreSS){
		if(tryBuildUDPtimers < 2){
			final long now = System.currentTimeMillis();
			if(tryBuildUDPtimers == 0){
				if(now - lastCallMS < 10000){
					LogManager.log("It seem error cycle build UDP , skip build process.");
					return false;
				}
			}else{
				if(now - lastCallMS > 100000){
					tryBuildUDPtimers = 0;
				}
			}
			
			if(coreSS.context.cmStatus == ContextManager.STATUS_EXIT){
				//暂未考虑服务器端发生UDP自动关闭时，及再仅手机断开后，重联引发的问题
				return false;
			}
			
			lastCallMS = now;
			tryBuildUDPtimers++;
			LogManager.log("Fail on UDP-check-alive, rebuild UDP connection. try " + tryBuildUDPtimers + " times.");

			coreSS.hcConnection.sipContext.tryRebuildUDPChannel(coreSS.hcConnection);
			return true;
		}
		tryBuildUDPtimers = 0;
		return false;
	}
	
	public final int UDP_RANDOM_HEADER_STARD_IDX = MsgBuilder.LEN_UDP_CONTROLLER_HEAD + 1;
	public final int UUID_STARD_IDX = UDP_RANDOM_HEADER_STARD_IDX + MsgBuilder.LEN_UDP_HEADER;
	
	public int getSetNullAddCmdLen(){
		return UUID_STARD_IDX + IConstant.uuidBS.length;
	}
	
	public void fillSetNullAddrCmdData(final HCConnection hcConnection, final byte[] bs){

		//填充UDP_TAG
		ByteUtil.integerToTwoBytes(MsgBuilder.E_UDP_CONTROLLER_SET_ADDR_NULL, bs, 0);

		//填充服务器或客户机标识
		bs[MsgBuilder.LEN_UDP_CONTROLLER_HEAD] = (byte)(IConstant.serverSide?1:0);
		
		//填充随机凭证号
		bs[UDP_RANDOM_HEADER_STARD_IDX] = hcConnection.udpHeader[0];
		bs[UDP_RANDOM_HEADER_STARD_IDX + 1] = hcConnection.udpHeader[1];
		
		//填充UUID数据
		for (int i = 0; i < IConstant.uuidBS.length; i++) {
			bs[i + UUID_STARD_IDX] = IConstant.uuidBS[i];
		}
	}
}
