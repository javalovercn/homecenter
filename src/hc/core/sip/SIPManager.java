package hc.core.sip;

import hc.core.ContextManager;
import hc.core.EnumNAT;
import hc.core.EventCenter;
import hc.core.HCMessage;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.data.DataReg;
import hc.core.util.CCoreUtil;
import hc.core.util.CUtil;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

public class SIPManager {

	private static ISIPContext sipContext;
	public static void setSIPContext(final ISIPContext context){
		if(SIPManager.sipContext != null){
			if(L.isInWorkshop){
				LogManager.errToLog("fail to setSIPContext");
			}
			return;
		}
		SIPManager.sipContext = context;
	}

	public static ISIPContext getSIPContext(){
		return SIPManager.sipContext;
	}

	public static void resetAllConnection(){
		SIPManager.getSIPContext().closeDeploySocket();
		SIPManager.isOnRelay = false;
	}

	public static final int DISCOVER_TIME_OUT_MS = Integer.parseInt(
			RootConfig.getInstance().getProperty(
					RootConfig.p_Discover_Stun_Server_Time_Out_MS));

	private static boolean isOnRelay = false;

	/**
	 * 被ProjectContext访问，请勿checkAccess
	 * @return
	 */
	public static boolean isOnRelay(){
		return SIPManager.isOnRelay;
	}

	public static void setOnRelay(final boolean onRelay){
		CCoreUtil.checkAccess();

		//		L.V = L.O ? false : LogManager.log("On Relay : " + onRelay);
		SIPManager.isOnRelay = onRelay;
		//		ContextManager.getSendServer().notifyIsRelay(isOnRelay);
	}

	public static void send(final OutputStream os, final byte[] bs, final int off, final int len) throws IOException{
		HCMessage.setMsgLen(bs, len - MsgBuilder.MIN_LEN_MSG);

		synchronized (os) {
			os.write(bs, off, len);
			os.flush();
		}
	}

	//	public static void setOnStun(StunDesc desc){
	//		SIPManager.LocalNATIPAddress = HCURLUtil.convertIPv46(desc.getPublicIP());
	//		SIPManager.LocalNATPort = desc.getPublicPort();
	//		SIPManager.LocalNATType = desc.getNatType();
	//
	//		Object socket = desc.getSocket();
	//		try{
	//			sipContext.deploySocket(socket, getSIPContext().getInputStream(socket),
	//					getSIPContext().getOutputStream(socket));
	//		}catch (Exception e) {
	//			L.V = L.O ? false : LogManager.log("setOnStun Exception:" + e.getMessage());
	//			e.printStackTrace();
	//		}
	//	}

	/**
	 * 没有成功，返回null
	 * @param udpPort
	 * @return
	 */
	//	public static StunDesc launchSTUN(Object iaddress, int udpPort){
	//		StunDesc desc = null;
	//
	//		String rip = RootConfig.getInstance().
	//				getProperty(RootConfig.p_RootRelayServer);
	//		int rport = Integer.parseInt(RootConfig.getInstance().
	//				getProperty(RootConfig.p_RootRelayServerPort));
	//		LogManager.info("try connect Root Server.");// ["+STUN[idx]+"]");
	//		LogManager.info("  {" + rip + "}:" + rport);
	//		try {
	//			desc = sipContext.stun(iaddress, rip, rport, udpPort);
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//		if(desc != null){
	//    		LogManager.info("launched Root Server");
	//		}
	//		return desc;
	//	}

	public static boolean startConnectionInit() {
		ContextManager.setStatus(ContextManager.STATUS_READY_TO_LINE_ON);

		if(IConstant.serverSide == false){
			final int idx_localip = 0, idx_localport = 1, idx_nattype = 2, idx_relayip = 5, idx_relayport = 6;

			//优先检查服务器上线，Open Cone等模式，以获得高性能
			final Object obj = RootServerConnector.getServerIPAndPort(RootServerConnector.getHideToken());
			if((obj == null) || (obj instanceof Vector)){
				LogManager.info("server : off");
				//	              LogManager.info("please check as follow:");
				//	              LogManager.info("1. ID[" + IConstant.uuid + "] maybe wrong HomeCenter ID. It is free.");
				//	              LogManager.info("2. PC server maybe NOT running.");
				//	              LogManager.info("3. try 'Enable Transmit Certification' for server.");
				//	              LogManager.info("4. HomeCenter server maybe reconnecting, try later.");
				ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_LINEOFF, null);
				return false;
			}

			LogManager.info("server : online");
			final String[] out = (String[])obj;

			//优先尝试直接连接
			if(out[idx_localport].equals("0") == false){
				//家庭内网直联
				LogManager.info("try direct conn...");
				final IPAndPort ipport = new IPAndPort(out[idx_localip], Integer.parseInt(out[idx_localport]));
				final IPAndPort l_directIpPort = SIPManager.tryBuildConnOnDirect(ipport, "Direct Mode",
						Integer.parseInt(out[idx_nattype]), 3000);//内网直联最长时间改为一秒
				if(l_directIpPort != null){
					//EnumNAT.OPEN_INTERNET or Symmetric
					LogManager.info("direct mode : yes");

					ContextManager.setConnectionModeStatus(ContextManager.MODE_CONNECTION_HOME_WIRELESS);
					SIPManager.directIpPort = l_directIpPort;

					//家庭内网不需要调用本步
					//发送REG到服务器,触发服务器初始化进程
					//					ContextManager.getContextInstance().send(MsgBuilder.E_TAG_SERVER_RELAY_START);

					return true;
				}else{
					LogManager.info("direct mode : fail");
				}
			}

			if(out[idx_relayport].equals("0") == false){
				LogManager.info("try relay connect...");
				final IPAndPort ipport = new IPAndPort(out[idx_relayip], Integer.parseInt(out[idx_relayport]));
				final IPAndPort l_relayIpPort = SIPManager.tryBuildConnOnDirect(ipport,
						"Relay Mode", EnumNAT.FULL_AGENT_BY_OTHER, SIPManager.REG_WAITING_MS);
				if(l_relayIpPort != null){
					LogManager.info("relay mode : yes");

					//中继时，可能并非3G/4G，也许是WiFi，3G/4G由其它逻辑提示
					//					LogManager.info("3G/4G/...");//more time and unreliable
					//					LogManager.info("Busy network and congestion may cause error");

					ContextManager.setConnectionModeStatus(ContextManager.MODE_CONNECTION_RELAY);
					SIPManager.relayIpPort = l_relayIpPort;

					//发送REG到服务器,触发服务器初始化进程
					ContextManager.getContextInstance().send(MsgBuilder.E_TAG_SERVER_RELAY_START);

					return true;
				}else{
					LogManager.info("relay mode : fail");
				}
			}

			ContextManager.getContextInstance().doExtBiz(IContext.BIZ_MOBI_FAIL_CONN, null);

			//尝试无法连接
			return false;
		}

		final String[] out = (String[])ContextManager.getContextInstance().doExtBiz(IContext.BIZ_UPLOAD_LINE_ON, null);

		if(IConstant.serverSide && (out != null)){
			if(out[0].equals("false")){
				//服务器ID被占用或无法连接服务器
				return false;
			}
		}

		LogManager.info("uploaded conection info to root server, and waiting for mobile");

		//LogManager.info("req connection");

		return true;
	}

	public static IPAndPort relayIpPort = new IPAndPort();
	public static IPAndPort directIpPort = new IPAndPort();

	//获得远程中继的UDP控制器端口
	public static int getUDPControllerPort(){
		//注意与NIOServer生成时，保持一致
		return SIPManager.relayIpPort.port - 1;
	}

	public static void setUDPChannelPort(final int udpPort){
		SIPManager.relayIpPort.udpPort = udpPort;
	}

	public static IPAndPort reConnectAfterResetExcep(){
		L.V = L.O ? false : LogManager.log("reConnectAfterExcepReset to " + SIPManager.relayIpPort.ip + ":" + SIPManager.relayIpPort.port);
		return SIPManager.proccReg(SIPManager.relayIpPort, MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_RESET, SIPManager.REG_WAITING_MS);
	}

	/**
	 * 如果创建成功，则返回true
	 * @param ipport
	 * @param remoteNattype
	 * @return
	 */
	private static IPAndPort tryBuildConnOnDirect(final IPAndPort ipport,
			final String memo, final int nattype, final int waitMS) {
		//LogManager.info("connect " + memo + " Server");

		if(nattype == EnumNAT.FULL_AGENT_BY_OTHER){
			SIPManager.setOnRelay(true);
		}

		//客户端连接，使用随机端口
		return SIPManager.proccReg(ipport, MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_FIRST, waitMS);
	}

	public static final int REG_WAITING_MS = 7000;

	public static IPAndPort proccReg(final IPAndPort ipport, final byte firstOrReset, final int waitingMS) {
		final Object socket = SIPManager.sendRegister(ipport, firstOrReset, waitingMS);
		if(socket != null){
			try{
				SIPManager.getSIPContext().deploySocket(socket);

				return ipport;
			}catch (final Exception e) {
				e.printStackTrace();
				try {
					SIPManager.getSIPContext().closeSocket(socket);
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
			return null;
		}else{
			//	        LogManager.info("Fail connect " + ipport.ip + ":" + ipport.port);
			return null;
		}
	}

	public static void close(){
		try{
			SIPManager.getSIPContext().closeDeploySocket();
		}catch (final Exception e) {
			//可能出现nullPointerException
		}
		SIPManager.setOnRelay(false);
	}


	private static long lastLineOff = 0;
	static final byte[] line_off_bs = SIPManager.buildLineOff();

	private static byte[] buildLineOff(){
		final byte[] e = new byte[MsgBuilder.UDP_BYTE_SIZE];
		e[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_LINE_OFF_EXCEPTION;
		return e;
	}

	public static void notifyRelayChangeToLineOff(){
		//因为在切换的同时，可以会产生断线事件
		SIPManager.lastLineOff = System.currentTimeMillis();
	}

	public static void notifyRelineon(final boolean isClientRequest) {
		final long now = System.currentTimeMillis();
		if(isClientRequest == false){
			//如果是有效连接，被客户端主动请求，且时间间隔极短，有可能产生本错误，而不去重新连接
			if((now - SIPManager.lastLineOff) > 5000){//10有可能较长，改小5000
			}else{
				L.V = L.O ? false : LogManager.log("skip recall lineoff autolineoff");
				if(IConstant.serverSide){
					ContextManager.getContextInstance().doExtBiz(IContext.BIZ_START_WATCH_KEEPALIVE_FOR_RECALL_LINEOFF, null);
				}
				return;
			}
		}else{
			if((now - SIPManager.lastLineOff) > CCoreUtil.WAIT_MS_FOR_NEW_CONN){
			}else{
				L.V = L.O ? false : LogManager.log("skip recall lineoff clientReq");
				if(IConstant.serverSide){
					ContextManager.getContextInstance().doExtBiz(IContext.BIZ_START_WATCH_KEEPALIVE_FOR_RECALL_LINEOFF, null);
				}
				return;
			}
		}
		if(isClientRequest && IConstant.serverSide){
			try{
				//服务器端要先等待用户断线，以免有客户端产生断线消息
				Thread.sleep(ThreadPriorityManager.UI_DELAY_MOMENT);
			}catch (final Exception e) {
			}
		}
		SIPManager.startRelineonForce(isClientRequest);
	}

	public static void startRelineonForce(final boolean isClientRequest) {
		SIPManager.lastLineOff = System.currentTimeMillis();

		RootServerConnector.notifyLineOffType(RootServerConnector.LOFF_LineEx_STR);

		ContextManager.setStatus(ContextManager.STATUS_LINEOFF);
		CUtil.resetCheck();
		final String cr = String.valueOf(isClientRequest);
		HCMessage.setMsgBody(SIPManager.line_off_bs, cr);

		ContextManager.getContextInstance().reset();

		EventCenter.notifyLineOff(SIPManager.line_off_bs);
	}

	public static void notifyCertPwdPassAtClient() {
		ContextManager.setStatus(ContextManager.STATUS_CLIENT_SELF);

		//仅在手机端执行
		ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS, null);
		//		L.V = L.O ? false : LogManager.log("Send menu:///root");
		//		ContextManager.getContextInstance().send(MsgBuilder.E_CANVAS_MAIN, "menu:///root");
	}

	public static Object sendRegister(final IPAndPort ipport, final byte firstOrReset, final int waiteMS){
		final Object send = SIPManager.getSIPContext().buildSocket(
				0, ipport.ip, ipport.port);
		if(send == null){
			return null;
		}

		final DataReg reg = new DataReg();
		final byte[] bs = new byte[MsgBuilder.UDP_BYTE_SIZE];//DatagramPacketCacher.getInstance().getFree();

		reg.setBytes(bs);

		if(SIPManager.isOnRelay()){
			if(IConstant.serverSide){
				reg.setFromServer(MsgBuilder.DATA_IS_SERVER_TO_RELAY);
				final byte[] tokenBS = (byte[])ContextManager.getContextInstance().doExtBiz(IContext.BIZ_GET_TOKEN, null);
				reg.setTokenDataIn(tokenBS, 0, tokenBS.length);
			}else{
				reg.setFromServer(MsgBuilder.DATA_IS_CLIENT_TO_RELAY);
			}
		}
		bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_RELAY_REG;
		bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = firstOrReset;
		final byte[] uuidbs = IConstant.getUUIDBS();
		reg.setUUIDDataIn(uuidbs, 0, uuidbs.length);

		final int regLen = DataReg.LEN_DATA_REG + MsgBuilder.INDEX_MSG_DATA;
		final byte[] bsClone = new byte[regLen];
		System.arraycopy(bs, 0, bsClone, 0, bsClone.length);

		try{
			SIPManager.send(SIPManager.getSIPContext().getOutputStream(send),
					bs, 0, regLen);
		}catch (final Exception e) {
			e.printStackTrace();
			try {
				SIPManager.getSIPContext().closeSocket(send);
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
			return null;
		}
		final HCTimer closeTimer = new HCTimer("closeTimer", waiteMS, true) {
			public final void doBiz() {
				try {
					//					L.V = L.O ? false : LogManager.log("CloseTimer close Reg Socket");
					SIPManager.getSIPContext().closeSocket(send);
				} catch (final Exception e) {
					e.printStackTrace();
				}

				HCTimer.remove(this);
			}
		};
		try{
			final DataInputStream is = SIPManager.getSIPContext().getInputStream(send);
			is.readFully(bs, 0, regLen);
			//验证是正确的回应，以免连接到已存在在，但是偶巧的端口上
			for (int i = MsgBuilder.INDEX_MSG_DATA, endIdx = regLen; i < endIdx; i++) {
				if(bs[i] != bsClone[i]){
					L.V = L.O ? false : LogManager.log("Error back echo data");
					throw new Exception();
				}
			}
			HCTimer.remove(closeTimer);
			L.V = L.O ? false : LogManager.log("Receive Echo");
			return send;
		}catch (final Exception e) {
			e.printStackTrace();
			try {
				SIPManager.getSIPContext().closeSocket(send);
			} catch (final Exception e1) {
			}
		}
		return null;
	}

}
