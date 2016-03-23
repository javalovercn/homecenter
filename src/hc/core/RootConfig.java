package hc.core;

import hc.core.util.CCoreUtil;

public class RootConfig extends HCConfig{
	private static RootConfig rc;
	
	/**
	 * 注意：实例引用不能改变，但内容可能被reset刷新
	 * @return
	 */
	public static RootConfig getInstance(){
		CCoreUtil.checkAccess();
		
		if(rc == null){
			reset(false);//可能因网络故障，没有获得数据
		}
		return rc;
	}

	public static void reset(boolean isStillForData) {
		CCoreUtil.checkAccess();
		
		String msg = null;
		boolean lineOff;
		do{
			msg = RootServerConnector.getRootConfig();
			if(msg != null){
				if(rc == null){
					rc = new RootConfig(msg);
				}else{
					rc.refresh(msg);
					CCoreUtil.resetFactor();//不建议直接走CUtil，因为可能初始化问题
				}
			}
			
			lineOff = (isStillForData && msg == null);
			
			if(lineOff){
				try {
					Thread.sleep(10 * 1000);
				} catch (Throwable e) {
				}
			}
		}while(lineOff);
	}
	
	public static final short p_ShowDonate = 0;//停止使用
	public static final short p_RelayDirectBFSize = 1;
	public static final short p_DefaultUDPSize = 2;
	public static final short p_SendBufferSize = 3;
	public static final short p_ReceiveBufferSize = 4;
	public static final short p_Color_On_Relay = 5;
	public static final short p_MS_On_Relay = 6;
	public static final short p_MIDP_VER = 7;
	public static final short p_ClientServerSendBufferSize = 8;
	public static final short p_RelayTryNum = 9;//停用
	public static final short p_ScreenCapMinBlockSize = 10;
	public static final short p_RootDelNotAlive = 11;
	public static final short p_ShowDesingerToAll = 12;
	public static final short p_KeepAliveMS = 13;
	public static final short p_ShowDesinger = 14;
	public static final short p_RelayServerThreadPriority = 15;
	public static final short p_RootRelayServer = 16;
	public static final short p_RootRelayServerPort = 17;
	public static final short p_MIDP_START_TONE = 18;
	public static final short p_Discover_Stun_Server_Time_Out_MS = 19;
	public static final short p_MIDP_START_TONE_VOL_LEVEL = 20;
	public static final short p_Display_Install_Tip = 21;
	public static final short p_ServerSendBufferSize = 22;
	public static final short p_ServerReceiveBufferSize = 23;
	public static final short p_enableLineWatcher = 24;
	public static final short p_TrafficClass = 25;
	public static final short p_forceRelay = 26;
	public static final short p_udpSendSize = 27;
	public static final short p_udpReceiveSize = 28;
	public static final short p_Packet_Resend_MS = 29;
	public static final short p_Packet_Resend_MaxSize = 30;
	public static final short p_PacketResendMaxTimes = 31;
	public static final short p_PacketLossDeepMsgIDNum = 32;
	public static final short p_Packet_Resend_Expired_MS = 33;
	public static final short p_mobile_color_bit_install = 34;
	public static final short p_First_Reg_Tuto = 35;
	//deprecated
	public static final short p_Sample_Ver = 36;//stop online, J2SEContext.getSampleHarVersion();
	public static final short p_Lock_Warn_First_Login = 37;
	public static final short p_Receive_Split_Max_Size = 38;
	public static final short p_Receive_Split_Throw_MS = 39;
	public static final short p_Encrypt_Factor = 40;
	public static final short p_UpdateOneTimeMinMinutes = 41;
	public static final short p_blockExceptionReport = 42;
	public static final short p_isMovToGooglePlayStore = 43;

	public RootConfig(String msg) {
//		System.out.println(msg);
		super(msg);
	}

}
