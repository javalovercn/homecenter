package hc.core;

import hc.core.util.CUtil;
import hc.core.util.IHCURLAction;
import hc.core.util.LogManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class IContext {
	public final void init(ReceiveServer rs, UDPReceiveServer udpRS){
		rServer = rs;
		udpReceivServer = udpRS;
	}
	
	
	InputStream udpInputStream;
	OutputStream udpOutputStream;
	
	private boolean isExit = false;

	public final void shutDown(){
		if(isExit){
			return;
		}
		isExit = true;

		ContextManager.setStatus(ContextManager.STATUS_EXIT);
		
		if(CUtil.userEncryptor != null){
			try{
				CUtil.userEncryptor.notifyExit(!IConstant.serverSide);
			}catch (Throwable e) {
				
			}
		}
		
		if(rServer != null){
			rServer.shutDown();
		}
		
		if(udpReceivServer != null){
			udpReceivServer.shutDown();
		}
		
		exit();				
		
	}

	public final void startAllServers() {
		//注意：SendServer应最先启动，ReceiverServer应最后启动，与流执行的次序相反。
		rServer.start();
		udpReceivServer.start();
	}

	
	public static final int ERROR = 1;
	public static final int WARN = 2;
	public static final int INFO = 3;
	public static final int ALARM = 4;
	public static final int CONFIRMATION = 5;
	public static final int TIP = 6;
	
	public static final int OK = 1010;
	public static final int EXIT = 1011;
	public static final int SAVE = 1012;
	
	public static final short BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS = 1;
	public static final short BIZ_SERVER_AFTER_PWD_ERROR = 2;
	public static final short BIZ_SERVER_AFTER_CERTKEY_ERROR = 3;
	public static final short BIZ_SERVER_AFTER_SERVICE_IS_FULL = 4;
	public static final short BIZ_SERVER_AFTER_UNKNOW_STATUS = 5;
	public static final short BIZ_SERVER_AFTER_OLD_MOBI_VER_STATUS = 6;
	public static final short BIZ_AFTER_HOLE = 7;
	public static final short BIZ_LOAD_SERVER_CONFIG = 8;
	public static final short BIZ_UPLOAD_LINE_ON = 9;
	public static final short BIZ_NEW_NOTIFICATION = 10;
	public static final short BIZ_VERSION_MID_OR_PC = 11;
	public static final short BIZ_CHANGE_RELAY = 12;
	public static final short BIZ_PLAYSOUND = 13;
	public static final short BIZ_SET_TRAY_ENABLE = 14;
	public static final short BIZ_GET_TOKEN = 15;
	//服务器绑定的最低手机端版本要求
	public static final short BIZ_GET_REQ_MOBI_VER_FROM_PC = 16;
	public static final short BIZ_SERVER_LINEOFF = 17;
	public static final short BIZ_NOTIFY_MOBI_IN_LOCK = 18;
	public static final short BIZ_NOTIFY_SERVER_IN_DIRECT_MODE = 19;
	public static final short BIZ_FORBID_UPDATE_CERT = 20;
	public static final short BIZ_MOVING_SCREEN_TIP = 21;
	public static final short BIZ_MOBI_FAIL_CONN = 22;
	public static final short BIZ_I18N_KEY = 23;
	public static final short BIZ_CTRL_BTN_TXT = 24;
	
	public final ReceiveServer getReceiveServer() {
		return rServer;
	}
	private ReceiveServer rServer;
	private UDPReceiveServer udpReceivServer;

	public final UDPReceiveServer getUDPReceiveServer() {
		return udpReceivServer;
	}

	public abstract void exit();
	public abstract void notifyShutdown();
	public abstract IHCURLAction getHCURLAction();
	public abstract void run();
	public abstract void displayMessage(String caption, String text, int type, Object imageData, int timeOut);

	public abstract Object getSysImg();
	
	public abstract Object doExtBiz(short bizNo, Object newParam);

	public abstract boolean isSoundOnMode();
    	
	public abstract void interrupt(Thread thread);
	
	public abstract Object getProperty(Object propertyID);
	
	public final void send(final byte event_type, final String body) {
		try {
			final byte[] jcip_bs = body.getBytes(IConstant.UTF_8);
			sendWrap(event_type, jcip_bs, 0, jcip_bs.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private final Boolean LOCK = new Boolean(false);
	
	public boolean isUsingUDPAtMobile(){
		return (IConstant.serverSide == false) && isBuildedUPDChannel && isDoneUDPChannelCheck;
	}

	/**
	 * 复制数据块到发送块的载荷段
	 * @param event_type
	 * @param jcip_bs
	 * @param offset
	 * @param len
	 */
	public final void sendWrap(final byte ctrlTag, final byte[] jcip_bs, final int offset, final int len) {
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
				&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, MsgBuilder.NULL_CTRL_SUB_TAG, jcip_bs, offset, len, 0, false);
			return;
		}
		
		final int minSize = len + MsgBuilder.MIN_LEN_MSG;
		synchronized (LOCK) {
			if(blobBS.length < minSize){
				blobBS = new byte[minSize];
			}
			
			final byte[] bs = blobBS;
			
//			L.V = L.O ? false : LogManager.log("sendWrap blobBS.length:" + blobBS.length + ", jcip_bs.length:" + jcip_bs.length + ", offset:" + offset + ", len:" + len);
			Message.setMsgBody(bs, MsgBuilder.INDEX_MSG_DATA, jcip_bs, offset, len);
			bs[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;
			
			if(ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN || len == 0){
	    	}else{
	    		//加密
//	    		L.V = L.O ? false : LogManager.log("Xor len:" + len);
    			CUtil.superXor(bs, MsgBuilder.INDEX_MSG_DATA, len, CUtil.OneTimeCertKey, true);
	    	}

//    		hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], len:" + len);
			try{
				synchronized (outStream) {
					outStream.write(bs, 0, minSize);
					outStream.flush();
				}
			}catch (Exception e) {
//				e.printStackTrace();
//				SIPManager.notifyRelineon(false);
//				L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
			}
		}
	}
	
	public void reset(){
		getUDPReceiveServer().setUdpServerSocket(null);

		isDoneUDPChannelCheck = false;
		isBuildedUPDChannel = false;
	}
	
	public boolean isDoneUDPChannelCheck = false;
	public boolean isBuildedUPDChannel = false;
	public UDPPacketResender udpSender = null;
	public static final byte[] udpHeader = new byte[MsgBuilder.LEN_UDP_HEADER];
	
	public final void send(final byte ctrlTag, final byte[] bs, final int data_len) {
			if(isBuildedUPDChannel && isDoneUDPChannelCheck
					&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, bs[MsgBuilder.INDEX_CTRL_SUB_TAG], bs, MsgBuilder.INDEX_MSG_DATA, data_len, 0, false);
			return;
		}

		Message.setMsgLen(bs, data_len);
		
		bs[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;

		if(ctrlTag <= MsgBuilder.UN_XOR_MSG_TAG_MIN || data_len == 0){
    	}else{
    		//加密
//    		L.V = L.O ? false : LogManager.log("Xor len:" + data_len);
    		CUtil.superXor(bs, MsgBuilder.INDEX_MSG_DATA, data_len, CUtil.OneTimeCertKey, true);
    	}

//		hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], len:" + data_len);
		try{
			synchronized (outStream) {
				outStream.write(bs, 0, data_len + MsgBuilder.INDEX_MSG_DATA);
				outStream.flush();
			}
		} catch (IOException e) {
//			e.printStackTrace();
			L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
			//因为reset重连时，有可能outStream为空，所以，异常问题由ReceiveServer来处理，不在本处调用。
//			SIPManager.notifyRelineon(false);
		}
		
	}

	static final byte[] oneTagBS = new byte[MsgBuilder.MIN_LEN_MSG];
	static final byte[] zeroLenbs = new byte[MsgBuilder.MIN_LEN_MSG];

	static byte[] blobBS = new byte[40 * 1024];
	
	static{
		Message.setMsgLen(oneTagBS, 0);
	}
	
	public final void send(final byte ctrlTag){
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
				&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, MsgBuilder.NULL_CTRL_SUB_TAG, oneTagBS, MsgBuilder.MIN_LEN_MSG, 0, 0, false);
			return;
		}
	    
		synchronized (oneTagBS) {
			oneTagBS[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;

			try {
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], len:" + 0);
				synchronized (outStream) {
					outStream.write(oneTagBS, 0, MsgBuilder.MIN_LEN_MSG);
					outStream.flush();
				}
			} catch (IOException e) {
//				e.printStackTrace();
				L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
//				SIPManager.notifyRelineon(false);
			}
		}
	}

	public final void send(OutputStream os, final byte[] bs, final int idx, final int len){
		final byte ctrlTag = bs[MsgBuilder.INDEX_CTRL_TAG];
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
				&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, bs[MsgBuilder.INDEX_CTRL_SUB_TAG], bs, idx + MsgBuilder.INDEX_MSG_DATA, len, 0, false);
			return;
		}

		if(os == null){
			os = outStream;
		}
		try {
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], subTage:" + subTag);
			synchronized (os) {
				os.write(bs, idx, len + MsgBuilder.INDEX_MSG_DATA);
				os.flush();
			}
		} catch (IOException e) {
//				e.printStackTrace();
			L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
//				SIPManager.notifyRelineon(false);
		}
	}

	public final void send(OutputStream os, final byte ctrlTag, final byte subTag){
		if(isBuildedUPDChannel && isDoneUDPChannelCheck
				&& (ctrlTag != MsgBuilder.E_GOTO_URL && ctrlTag != MsgBuilder.E_INPUT_EVENT && ctrlTag > MsgBuilder.UN_XOR_MSG_TAG_MIN)){
			udpSender.sendUDP(ctrlTag, subTag, zeroLenbs, MsgBuilder.MIN_LEN_MSG, 0, 0, false);
			return;
		}

		synchronized (zeroLenbs) {
			zeroLenbs[MsgBuilder.INDEX_CTRL_TAG] = ctrlTag;
			zeroLenbs[MsgBuilder.INDEX_CTRL_SUB_TAG] = subTag;
		    
			if(os == null){
				os = outStream;
			}
			try {
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("Send [" + ctrlTag + "], subTage:" + subTag);
				synchronized (os) {
					os.write(zeroLenbs, 0, MsgBuilder.MIN_LEN_MSG);
					os.flush();
				}
			} catch (IOException e) {//不能拦截os为null的异常，因为KeepaliveManager.java保活需要此异常
//				e.printStackTrace();
				L.V = L.O ? false : LogManager.log("Exception:" + e.getMessage() + ", Lose package");
//				SIPManager.notifyRelineon(false);
			}
		}
	}

	public void setOutputStream(DataOutputStream os) {
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("Changed Send Socket");
		this.outStream = os;
	}

	private DataOutputStream outStream;
}
