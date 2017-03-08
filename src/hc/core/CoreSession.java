package hc.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import hc.core.cache.CacheManager;
import hc.core.data.ServerConfig;
import hc.core.sip.ISIPContext;
import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.core.util.io.StreamBuilder;

public abstract class CoreSession {
	private static boolean isNotifyShutdown = false;
	
	public static boolean isNotifyShutdown(){
		return isNotifyShutdown;
	}
	
	public static void setNotifyShutdown(){
		CCoreUtil.checkAccess();
		
		isNotifyShutdown = true;
	}
	
	public final DataInputStream swapSocket(final HCConnection keepConn1, final HCConnection dropConn2, final boolean isShutdownReceive){
		ISIPContext sip1 = keepConn1.sipContext;
		ISIPContext sip2 = dropConn2.sipContext;
		
		{
			final Object sock1 = sip1.getSocket();
			final Object sock2 = sip2.getSocket();
			
			final boolean isSwap = true;
			sip1.setSocket(sock2, isSwap);
			sip2.setSocket(sock1, isSwap);
			
			DataInputStream is1 = null;
			try{
				is1 = sip1.getInputStream(sock1);//有可能已关闭
			}catch (Exception e) {
				e.printStackTrace();
			}
			DataInputStream is2 = null;
			try{
				is2 = sip2.getInputStream(sock2);//有可能已关闭
			}catch (Exception e) {
				e.printStackTrace();
			}
			
			DataOutputStream os1 = null;
			try{
				os1 = sip1.getOutputStream(sock1);//有可能已关闭
			}catch (Exception e) {
				e.printStackTrace();
			}
			DataOutputStream os2 = null;
			try{
				os2 = sip2.getOutputStream(sock2);//有可能已关闭
			}catch (Exception e) {
				e.printStackTrace();
			}
			
			sip1.setInputOutputStream(is2, os2);
			sip2.setInputOutputStream(is1, os1);
		}
		
		final Object is1 = keepConn1.getReceiveServerInputStream();
		final Object is2 = dropConn2.getReceiveServerInputStream();
		keepConn1.setReceiveServerInputStream(is2, false, true);
		dropConn2.setReceiveServerInputStream(is1, true, false);
			
		{
			final Object os1 = keepConn1.getOutputStream();
			final Object os2 = dropConn2.getOutputStream();
			keepConn1.setOutputStream(os2);
			dropConn2.setOutputStream(os1);
		}
		
		L.V = L.WShop ? false : LogManager.log("[Change] done swap socket.");
		
		return (DataInputStream)is1;
	}
	
	public final StreamBuilder streamBuilder;
	
	public EventCenter eventCenter;
	public IContext context;
	public HCTimer udpAliveMobiDetectTimer;
	public HCConnection hcConnection = new HCConnection();

	public final HCConditionWatcher eventCenterDriver = new HCConditionWatcher("EventCenterDriver", ThreadPriorityManager.LOWEST_PRIORITY);
	public byte[] mobileUidBSForCache;
	public final byte[] codeBSforMobileSave = new byte[CacheManager.CODE_LEN];
	public IHCURLAction urlAction;
	public int urlParaIdx = 1;
	public HCURL contextHCURL;
	public 	ServerConfig j2meServerConfig;

	public CoreSession(){
		streamBuilder = new StreamBuilder(this);
	}
	
//	public final int DISCOVER_TIME_OUT_MS = Integer.parseInt(
//			RootConfig.getInstance().getProperty(
//					RootConfig.p_Discover_Stun_Server_Time_Out_MS));
	
	protected abstract void delayToSetNull();
	
	public void release(){
		hcConnection.release();
		delayToSetNull();
		
		HCTimer.remove(udpAliveMobiDetectTimer);
	}

	protected final void setNull() {
		eventCenter = null;
		context = null;
		hcConnection.sipContext = null;
		streamBuilder.coreSS = null;//streamBuilder构造时，生成，所以不为null
	}
	
	public void setOneTimeCertKey(final byte[] bs){
//		LogManager.log("successful set OneTimeCertKey : " + ByteUtil.toHex(bs));
		
		if(hcConnection.OneTimeCertKey == null){
			hcConnection.OneTimeCertKey = bs;
		}else{
			final int len = bs.length;
			final byte[] oneTimeBS = hcConnection.OneTimeCertKey;
			for (int i = 0; i < len; i++) {
				oneTimeBS[i] = bs[i];
			}
		}
	}
	
}
