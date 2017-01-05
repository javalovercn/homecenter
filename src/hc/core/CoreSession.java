package hc.core;

import hc.core.cache.CacheManager;
import hc.core.data.ServerConfig;
import hc.core.sip.IPAndPort;
import hc.core.sip.ISIPContext;
import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.core.util.IHCURLAction;
import hc.core.util.ThreadPriorityManager;
import hc.core.util.io.StreamBuilder;

public class CoreSession {
	private static boolean isNotifyShutdown = false;
	
	public static boolean isNotifyShutdown(){
		return isNotifyShutdown;
	}
	
	public static void setNotifyShutdown(){
		CCoreUtil.checkAccess();
		
		isNotifyShutdown = true;
	}
	
	public byte[] OneTimeCertKey;
	public final byte[] udpHeader = new byte[MsgBuilder.LEN_UDP_HEADER];
	public final StreamBuilder streamBuilder;
	public byte[] package_tcp_bs;
	public int package_tcp_id;
	public int package_tcp_last_store_idx = MsgBuilder.INDEX_MSG_DATA;
	public int packaeg_tcp_num;
	public int packaeg_tcp_appended_num;
	
	public EventCenter eventCenter;
	public ISIPContext sipContext;
	public IContext context;
	public HCTimer udpAliveMobiDetectTimer;

	public IPAndPort relayIpPort = new IPAndPort();
	public long lastLineOff = 0;
	public boolean isStartLineOffProcess;
	public final HCConditionWatcher eventCenterDriver = new HCConditionWatcher("EventCenterDriver", ThreadPriorityManager.LOWEST_PRIORITY);
	public boolean isInitialCloseReceiveForJ2ME = false;
	public byte[] mobileUidBSForCache;
	public final byte[] codeBSforMobileSave = new byte[CacheManager.CODE_LEN];
	public IHCURLAction urlAction;
	public int urlParaIdx = 1;
	public HCURL contextHCURL;
	public 	ServerConfig j2meServerConfig;

	public CoreSession(){
		streamBuilder = new StreamBuilder(this);
	}
	
	public AckBatchHCTimer ackbatchTimer;
	
	public final void setSIPContextAndResender(ISIPContext sipCtx, final UDPPacketResender resender){
		sipContext = sipCtx;
		ackbatchTimer = new AckBatchHCTimer("AckBatch", HCTimer.HC_INTERNAL_MS, false, resender);
	}
	
//	public final int DISCOVER_TIME_OUT_MS = Integer.parseInt(
//			RootConfig.getInstance().getProperty(
//					RootConfig.p_Discover_Stun_Server_Time_Out_MS));
	
	public final ReceiveServer getReceiveServer() {
		return rServer;
	}
	public ReceiveServer rServer;
	public UDPReceiveServer udpReceivServer;

	public final UDPReceiveServer getUDPReceiveServer() {
		return udpReceivServer;
	}

	public final void setReceiver(final ReceiveServer rs, final UDPReceiveServer udpRS){
		rServer = rs;
		udpReceivServer = udpRS;
	}
	private UDPController udpController;
	
	public final UDPController getUDPController(){
		synchronized (this) {
			if(udpController == null){
				udpController = new UDPController();
			}
			return udpController;
		}
	}
	
	public void release(){
		eventCenter = null;
		context = null;
		
		HCTimer.remove(sipContext.resender.resenderTimer);
		sipContext = null;
		
		streamBuilder.coreSS = null;//streamBuilder构造时，生成，所以不为null
		
		HCTimer.remove(ackbatchTimer);
		HCTimer.remove(udpAliveMobiDetectTimer);
	}
	
	public void setOneTimeCertKey(final byte[] bs){
//		L.V = L.O ? false : LogManager.log("successful set OneTimeCertKey : " + ByteUtil.toHex(bs));
		
		if(OneTimeCertKey == null){
			OneTimeCertKey = bs;
		}else{
			for (int i = 0; i < bs.length; i++) {
				OneTimeCertKey[i] = bs[i];
			}
		}
	}
	
}
