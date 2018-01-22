package hc.core;

import java.util.Vector;

import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;

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
	
	private final static Vector listenerVector = new Vector(4);
	
	/**
	 * 添加RootConfig更新事件。不含初始化获得。
	 * @param listener
	 */
	public static void addListener(final RootConfigListener listener){
		CCoreUtil.checkAccess();
		
		synchronized (listenerVector) {
			listenerVector.addElement(listener);
		}
	}
	
	private static boolean isStartingUpdateRootCfgForFail;
	
	private static void startUpdateRootCfgWhenFail(){
		synchronized (listenerVector) {
			if(isStartingUpdateRootCfgForFail == false){
				Thread t = new Thread(){
					String msg;
					public void run(){
						do{
							try{
								Thread.sleep(HCTimer.ONE_MINUTE);
							}catch (Exception e) {
							}
							msg  = RootServerConnector.getRootConfig();
						}while(msg == null);
						reset(false);
						isStartingUpdateRootCfgForFail = false;
					}
				};
				RootBuilder.getInstance().setDaemonThread(t);
				t.start();
				isStartingUpdateRootCfgForFail = true;
			}
		}
	}
	
	public static boolean isFailToGetAliveRootCfg(){
		return isStartingUpdateRootCfgForFail;
	}

	public static void reset(boolean isStillForData) {
		CCoreUtil.checkAccess();
		
		String msg = null;
		boolean lineOff;
		do{
			msg = RootServerConnector.getRootConfig();
			
			if(msg != null){
				RootBuilder.getInstance().doBiz(RootBuilder.ROOT_SET_LAST_ROOT_CFG, msg);
			}else{
				final String lastRootCfg = (String)RootBuilder.getInstance().doBiz(RootBuilder.ROOT_GET_LAST_ROOT_CFG, null);
				if(lastRootCfg != null){
					RootServerConnector.stopRepairTipTimer();
					LogManager.errToLog("shift to OFF-LINE mode!!!");
					LogManager.errToLog("fail to get root config from net, use last root config!!!");
					msg = lastRootCfg;
					startUpdateRootCfgWhenFail();
				}
			}
			
			if(msg != null){
				if(rc == null){
					rc = new RootConfig(msg);
				}else{
					rc.refresh(msg);
					CCoreUtil.resetFactor();//不建议直接走CUtil，因为可能初始化问题
					
					synchronized (listenerVector) {
						final int size = listenerVector.size();
						for (int i = size - 1; i >= 0; i--) {
							RootConfigListener listener = (RootConfigListener)listenerVector.elementAt(i);
							listener.onRefresh();
						}
					}
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
	public static final short p_CacheMinSize = 44;
	public static final short p_isDisplayVIPMenu = 45;
	public static final short p_NovVIPClientFileMaxSizeK = 46;
	public static final short p_JRubyVer = 47;
	public static final short p_AndroidJRubyVer = 48;
	
	public RootConfig(String msg) {
//		System.out.println(msg);
		super(msg);
	}

}
