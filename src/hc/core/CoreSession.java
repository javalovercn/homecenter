package hc.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import hc.core.cache.CacheManager;
import hc.core.data.ServerConfig;
import hc.core.sip.ISIPContext;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.HCURL;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;
import hc.core.util.ThreadPriorityManager;
import hc.core.util.io.HCInputStream;
import hc.core.util.io.IHCStream;
import hc.core.util.io.StreamBuilder;

public abstract class CoreSession {
	private static boolean isNotifyShutdown = false;
	
	public static boolean isNotifyShutdown(){
		return isNotifyShutdown;
	}
	
	/**
	 * C/S处理通讯交互状态
	 * @return
	 */
	public abstract boolean isExchangeStatus();
	
	public static void setNotifyShutdown(){
		CCoreUtil.checkAccess();
		
		isNotifyShutdown = true;
	}
	
	public final void notifyContinue(){
		hcConnection.resendUnReachablePackage();
		hcConnection.connectionRebuilder.notifyContinue();
	}
	
	public final void dispatchStream(final byte[] bs){
		final Hashtable inputStreamTable = streamBuilder.inputStreamTable;
		
		final int len = HCMessage.getMsgLen(bs);
		final int streamID = (int)ByteUtil.fourBytesToLong(bs, MsgBuilder.INDEX_MSG_DATA);
		Object is;
		final int dataLen = len - StreamBuilder.STREAM_ID_LEN;
		
		synchronized (streamBuilder.lock) {
			is = inputStreamTable.get(new Integer(streamID));
		}
		if(is != null){
			((HCInputStream)is).appendStream(bs, MsgBuilder.INDEX_MSG_DATA + StreamBuilder.STREAM_ID_LEN, dataLen);
		}
	}
	
	public final void manageStream(final byte[] bs) {
//		final int sendLen = 1 + STREAM_ID_LEN + 1 + classNameLen + 2 + len;
		int offsetidx = MsgBuilder.INDEX_MSG_DATA;
		final boolean isInputStream = (bs[offsetidx]==1);
		offsetidx += 1;
		final int streamID = (int)ByteUtil.fourBytesToLong(bs, offsetidx);
		offsetidx += StreamBuilder.STREAM_ID_LEN;
		final int classNameLen = ByteUtil.oneByteToInteger(bs, offsetidx);
		offsetidx += 1;
		final String className = ByteUtil.buildString(bs, offsetidx, classNameLen, IConstant.UTF_8);
		if(className.equals(StreamBuilder.TAG_CLOSE_STREAM)){
			final Object stream = streamBuilder.closeStream(isInputStream, streamID);
			if(stream != null && stream instanceof IHCStream){
				((IHCStream)stream).notifyClose();
			}
			return;
		}
		
		offsetidx += classNameLen;
		final int paraBSLen = ByteUtil.twoBytesToInteger(bs, offsetidx);
		offsetidx +=2;
		final byte[] paraBS = ByteUtil.byteArrayCacher.getFree(paraBSLen);
		System.arraycopy(bs, offsetidx, paraBS, 0, paraBSLen);
		
		context.notifyStreamReceiverBuilder(isInputStream, className, streamID, paraBS, 0, paraBSLen);
		ByteUtil.byteArrayCacher.cycle(paraBS);
		return;
	}
	
	public final void resetCheck(){
		hcConnection.resetCheck();
	}
	
	public final void resetNearDeployTime(){
		hcConnection.sipContext.resetNearDeployTime();
	}
	
	public final void resetResender(){
		hcConnection.sipContext.resender.reset();
	}
	
	public final void notifyLineOff(final boolean isClientRequest, final boolean isForce) {
//		if(isForce == false){
//			final long now = System.currentTimeMillis();
//			if(isClientRequest == false){
//				//如果是有效连接，被客户端主动请求，且时间间隔极短，有可能产生本错误，而不去重新连接
//				if((now - coreSS.lastLineOff) > 5000){//10有可能较长，改小5000
//				}else{
//					LogManager.log("skip recall lineoff autolineoff");
//					if(IConstant.serverSide){
//						coreSS.context.doExtBiz(IContext.BIZ_START_WATCH_KEEPALIVE_FOR_RECALL_LINEOFF, null);
//					}
//					return;
//				}
//			}else{
//				if((now - coreSS.lastLineOff) > CCoreUtil.WAIT_MS_FOR_NEW_CONN){
//				}else{
//					LogManager.log("skip recall lineoff clientReq");
//					if(IConstant.serverSide){
//						coreSS.context.doExtBiz(IContext.BIZ_START_WATCH_KEEPALIVE_FOR_RECALL_LINEOFF, null);
//					}
//					return;
//				}
//			}
//			if(isClientRequest && IConstant.serverSide){
//				try{
//					//服务器端要先等待用户断线，以免有客户端产生断线消息
//					Thread.sleep(ThreadPriorityManager.UI_DELAY_MOMENT);
//				}catch (final Exception e) {
//				}
//			}
//		}//end isForce
		
		synchronized (this) {
			if(hcConnection.isStartLineOffProcess){
				return;
			}
			hcConnection.isStartLineOffProcess = true;
		}
		if(L.isInWorkshop){
			new Exception("[workshop] printStack").printStackTrace();
		}
		hcConnection.rServer.shutDown();
		RootBuilder.getInstance().doBiz(RootBuilder.ROOT_RELEASE_EXT_J2SE, this);
		startLineOffForce(isClientRequest);
	}
	
	public synchronized final void startLineOffForce(final boolean isClientRequest) {
		RootServerConnector.notifyLineOffType(this, RootServerConnector.LOFF_LineEx_STR);

		context.setStatus(ContextManager.STATUS_LINEOFF);
		hcConnection.resetCheck();

		hcConnection.reset();
		
//		final String cr = String.valueOf(isClientRequest);
//		final byte[] line_off_bs = buildLineOff();
//		HCMessage.setMsgBody(line_off_bs, cr);
//		eventCenter.notifyLineOff(line_off_bs);
		context.onEventLineOff(isClientRequest);
	}
	
	private final byte[] buildLineOff(){
		final byte[] e = new byte[MsgBuilder.UDP_BYTE_SIZE];
		e[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_LINE_OFF_EXCEPTION;
		return e;
	}
	
	public final boolean isOnRelay(){
		return SIPManager.isOnRelay(hcConnection);
	}
	
	public final Object buildSocket(int localPort, String targetServer, int targetPort){
		return hcConnection.sipContext.buildSocket(localPort, targetServer, targetPort);
	}
	
	public final DataOutputStream getOutputStream(Object socket) throws IOException{
		return hcConnection.sipContext.getOutputStream(socket);
	}
	
	public final void closeSocket(Object socket) throws IOException{
		hcConnection.sipContext.closeSocket(socket);
	}
	
	public final Object getSocket(){
		return hcConnection.sipContext.getSocket();
	}
	
	public final DataInputStream swapSocket(final HCConnection keepConn1, final HCConnection dropConn2, final boolean isShutdownReceive){
		
		ISIPContext sip1 = keepConn1.sipContext;
		ISIPContext sip2 = dropConn2.sipContext;
		
		{
			final boolean isOnRelay1 = SIPManager.isOnRelay(keepConn1);
			final boolean isOnRelay2 = SIPManager.isOnRelay(dropConn2);
			
			keepConn1.setOnRelay(isOnRelay2);
			dropConn2.setOnRelay(isOnRelay1);
		}
		
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
	
	public EventCenter eventCenter;//构造EventCenter时，反向更新到此
	public IContext context;
	public HCTimer udpAliveMobiDetectTimer;
	protected final HCConnection hcConnection = new HCConnection();

	//禁止多会话共用，会导致全局锁
	public final EventCenterDriver eventCenterDriver = new EventCenterDriver();
	
	public byte[] mobileUidBSForCache;
	public final byte[] codeBSforMobileSave = new byte[CacheManager.CACHE_CODE_LEN];
	public IHCURLAction urlAction;
	public int urlParaIdx = 1;
	public HCURL contextHCURL;
	public 	ServerConfig j2meServerConfig;

	public CoreSession(){
		streamBuilder = new StreamBuilder(this);
	}
	
	public abstract void synXorPackageID(final byte[] bs);
	
	public final HCConnection getHCConnection(){
		RootBuilder.getInstance().doBiz(RootBuilder.ROOT_BIZ_CHECK_STACK_TRACE, null);
		return hcConnection;
	}
	
	public final void deploySocket(Object socket) throws Exception{
		hcConnection.sipContext.deploySocket(hcConnection, socket);
	}
	
	public final void setOnRelay(final boolean isRelay){
		hcConnection.setOnRelay(isRelay);
	}
	
	public final void closeHC(){
		try{
			hcConnection.sipContext.closeDeploySocket(hcConnection);
		}catch (final Exception e) {
			//可能出现nullPointerException
		}
		setOnRelay(false);
	}
	
	public final UDPController getUDPController(){
		return hcConnection.getUDPController();
	}
	
//	public final int DISCOVER_TIME_OUT_MS = Integer.parseInt(
//			RootConfig.getInstance().getProperty(
//					RootConfig.p_Discover_Stun_Server_Time_Out_MS));
	
	protected abstract void delayToSetNull();
	
	public void release(){
		L.V = L.WShop ? false : LogManager.log("release CoreSession...");
		hcConnection.release();
		delayToSetNull();
		
		HCTimer.remove(udpAliveMobiDetectTimer);
		eventCenterDriver.notifyShutdown();
		L.V = L.WShop ? false : LogManager.log("done release CoreSession.");
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
