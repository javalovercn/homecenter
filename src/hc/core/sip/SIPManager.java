package hc.core.sip;

import hc.core.ContextManager;
import hc.core.CoreSession;
import hc.core.EnumNAT;
import hc.core.HCConnection;
import hc.core.HCMessage;
import hc.core.HCTimer;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.data.DataReg;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.RootBuilder;
import hc.core.util.StringUtil;
import hc.core.util.UIUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SIPManager {

	public static void resetAllConnection(final HCConnection hcConnection) {
		final ISIPContext sipCtx = hcConnection.sipContext;
		sipCtx.closeDeploySocket(hcConnection);
		sipCtx.isOnRelay = false;
	}

	/**
	 * 被ProjectContext访问，请勿checkAccess
	 * 
	 * @return
	 */
	public static boolean isOnRelay(final HCConnection hcConnection) {
		return hcConnection.sipContext.isOnRelay;
	}

	public static void send(final OutputStream os, final byte[] bs,
			final int off, final int len) throws IOException {
		HCMessage.setMsgLen(bs, len - MsgBuilder.MIN_LEN_MSG);

		synchronized (os) {
			os.write(bs, off, len);
			os.flush();
		}
	}

	// public static void setOnStun(StunDesc desc){
	// SIPManager.LocalNATIPAddress =
	// HCURLUtil.convertIPv46(desc.getPublicIP());
	// SIPManager.LocalNATPort = desc.getPublicPort();
	// SIPManager.LocalNATType = desc.getNatType();
	//
	// Object socket = desc.getSocket();
	// try{
	// sipContext.deploySocket(socket, getSIPContext().getInputStream(socket),
	// getSIPContext().getOutputStream(socket));
	// }catch (Exception e) {
	// LogManager.log("setOnStun Exception:" + e.getMessage());
	// ExceptionReporter.printStackTrace(e);
	// }
	// }

	/**
	 * 没有成功，返回null
	 * 
	 * @param udpPort
	 * @return
	 */
	// public static StunDesc launchSTUN(Object iaddress, int udpPort){
	// StunDesc desc = null;
	//
	// String rip = RootConfig.getInstance().
	// getProperty(RootConfig.p_RootRelayServer);
	// int rport = Integer.parseInt(RootConfig.getInstance().
	// getProperty(RootConfig.p_RootRelayServerPort));
	// LogManager.info("try connect Root Server.");// ["+STUN[idx]+"]");
	// LogManager.info(" {" + rip + "}:" + rport);
	// try {
	// desc = sipContext.stun(iaddress, rip, rport, udpPort);
	// } catch (Exception e) {
	// ExceptionReporter.printStackTrace(e);
	// }
	// if(desc != null){
	// LogManager.info("launched Root Server");
	// }
	// return desc;
	// }

	private static final int CONN_OK_MODE_CONNECTION_RELAY = 2;
	private static final int CONN_OK_MODE_CONNECTION_HOME_WIRELESS = 1;
	private static final int CONN_ERR_BIZ_SERVER_LINEOFF = -1;
	private static final int CONN_ERR_BIZ_SERVER_ACCOUNT_BUSY = -2;
	private static final int CONN_ERR_MOBI_FAIL_CONN = -3;

	public static boolean startConnectionInit(final CoreSession coreSS,
			final HCConnection hcConnection) {
		coreSS.context.setStatus(ContextManager.STATUS_READY_TO_LINE_ON);

		if (IConstant.serverSide == false) {
			final byte[] tokenBS = (byte[]) coreSS.context
					.doExtBiz(IContext.BIZ_GET_TOKEN, null);
			final int out = startConnectionInitForClient(hcConnection,
					MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_FIRST, tokenBS);

			if (out < 0) {
				if (out == CONN_ERR_BIZ_SERVER_LINEOFF) {
					// 由于连接画面已提示，故关闭
					// coreSS.context.doExtBiz(IContext.BIZ_SERVER_LINEOFF,
					// null);
					coreSS.context.doExtBiz(IContext.BIZ_CLIENT_RELOGIN, null);
				} else if (out == CONN_ERR_BIZ_SERVER_ACCOUNT_BUSY) {
					coreSS.context.doExtBiz(IContext.BIZ_SERVER_ACCOUNT_BUSY,
							null);
				} else if (out == CONN_ERR_MOBI_FAIL_CONN) {
					coreSS.context.doExtBiz(IContext.BIZ_MOBI_FAIL_CONN, null);
				}
				return false;
			} else {
				// 注意：如果以下增加逻辑，请同步J2MERootBuilder.ROOT_BUILD_NEW_CONNECTION
				if (out == CONN_OK_MODE_CONNECTION_HOME_WIRELESS) {
					coreSS.context.setConnectionModeStatus(
							ContextManager.MODE_CONNECTION_HOME_WIRELESS);
				} else if (out == CONN_OK_MODE_CONNECTION_RELAY) {
					coreSS.context.setConnectionModeStatus(
							ContextManager.MODE_CONNECTION_RELAY);
					// 发送REG到服务器,触发服务器初始化进程
					coreSS.context.send(MsgBuilder.E_TAG_SERVER_RELAY_START);
				}
				return true;
			}
		}

		final String[] out = (String[]) coreSS.context
				.doExtBiz(IContext.BIZ_UPLOAD_LINE_ON, null);

		if (IConstant.serverSide && (out != null)) {
			if (IConstant.FALSE.equals(out[0])) {
				// 服务器ID被占用或无法连接服务器
				return false;
			}
		}

		LogManager.info(
				"uploaded conection info to root server, and waiting for mobile");

		// LogManager.info("req connection");

		return true;
	}

	public static int startConnectionInitForClient(
			final HCConnection hcConnection, final byte subTag,
			final byte[] tokenBS) {
		final int idx_localip = 0, idx_localport = 1, idx_nattype = 2,
				idx_relayip = 5, idx_relayport = 6;

		// 优先检查服务器上线，Open Cone等模式，以获得高性能
		Object obj;
		int count = 0;

		final String tryConn = StringUtil.replace(
				UIUtil.getUIString("m100", "try connect : [{uuid}]"), "{uuid}",
				IConstant.getUUID());
		LogManager.info(tryConn);

		boolean isShowServerBusy = false;

		if (RootConfig.isFailToGetAliveRootCfg()) {
			final String ipAndPort = (String) RootBuilder.getInstance()
					.doBiz(RootBuilder.ROOT_QUERY_DIRECT_SERVER_IP, null);
			if (ipAndPort != null) {
				try {
					infoServerOnline();

					final String[] ipport = StringUtil.splitToArray(ipAndPort,
							StringUtil.SPLIT_LEVEL_2_JING);
					final String ip = ipport[0];
					final String port = ipport[1];
					L.V = L.WShop ? false
							: LogManager.log(
									"query direct server by multicast, ip : "
											+ ip + ", port : " + port);

					final int timeOut = 5000;
					final IPAndPort l_directIpPort = SIPManager
							.tryBuildConnOnDirect(hcConnection, subTag,
									new IPAndPort(ip, Integer.parseInt(port)),
									"Direct Mode", EnumNAT.OPEN_INTERNET,
									timeOut, tokenBS);// 内网直联最长时间改为一秒
					if (l_directIpPort != null) {
						infoDirectModeYes();

						return CONN_OK_MODE_CONNECTION_HOME_WIRELESS;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}

		final int maxTryCount = 3;// 14/2=7, 7 > call.php/TIME_TO_SEC/3 * 2
		do {
			L.V = L.WShop ? false : LogManager.log("try get server ip/port...");
			obj = RootServerConnector
					.getServerIPAndPortV2(RootServerConnector.getHideToken());
			if (obj == null) {
				L.V = L.WShop ? false
						: LogManager.log("done to get server ip/port : null");
				// LogManager.info("please check as follow:");
				// LogManager.info("1. ID[" + IConstant.uuid + "] maybe wrong
				// HomeCenter ID. It is free.");
				// LogManager.info("2. PC server maybe NOT running.");
				// LogManager.info("3. try 'Enable Transmit Certification' for
				// server.");
				// LogManager.info("4. HomeCenter server maybe reconnecting, try
				// later.");
				L.V = L.WShop ? false
						: LogManager.log("no server connection for : ["
								+ IConstant.getUUID()
								+ "] now, wait and try later.");
				if (isShowServerBusy == false && count < maxTryCount / 2) {
				} else if (isShowServerBusy) {
				} else {
					LogManager.info(
							UIUtil.getUIString("m101", "server : off/hide"));

					LogManager.info(UIUtil.getUIString("m123",
							"if server is lineon, please choose one or both:"));
					String msg = UIUtil.getUIString("m124",
							"1. [{showUp}] on server,");
					msg = StringUtil.replace(msg, "{showUp}",
							UIUtil.getUIString("9180", "Show up"));
					LogManager.info(msg);// 9180=Show up
					msg = UIUtil.getUIString("m125", "2. [{etc}] on server,");
					msg = StringUtil.replace(msg, "{etc}", UIUtil.getUIString(
							"1020", "Enable Transmit Certification"));
					LogManager.info(msg);

					LogManager.info(UIUtil.getUIString("m115",
							"press screen to exit!"));// press screen to exit!

					return CONN_ERR_BIZ_SERVER_LINEOFF;
				}

				L.V = L.WShop ? false : LogManager.log("try sleep 1000ms...");
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
				}
			} else if (isShowServerBusy || obj instanceof String
					&& RootServerConnector.MULTI_CLIENT_BUSY.equals(obj)) {
				L.V = L.WShop ? false
						: LogManager.log(
								"done to get server ip/port : MULTI_CLIENT_BUSY");
				if (isShowServerBusy == false) {
					isShowServerBusy = true;
					LogManager
							.info(UIUtil.getUIString("m102", "server : busy"));
					LogManager.info(UIUtil.getUIString("m103",
							"do our best to connect..."));
				}

				L.V = L.WShop ? false : LogManager.log("try sleep 1000ms...");
				try {
					Thread.sleep(1000);// 与call.php/busy/3 seconds
				} catch (Exception e) {
				}
			} else {
				break;
			}

			count++;
		} while (count < maxTryCount);

		if (obj instanceof String
				&& RootServerConnector.MULTI_CLIENT_BUSY.equals(obj)) {
			L.V = L.WShop ? false
					: LogManager.log(
							"fail to connect : CONN_ERR_BIZ_SERVER_ACCOUNT_BUSY");
			return CONN_ERR_BIZ_SERVER_ACCOUNT_BUSY;
		}

		// try{
		// LogManager.info("server : busy");
		// ContextManager.getContextInstance().doExtBiz(IContext.BIZ_SERVER_BUSY,
		// null);
		// return false;
		// }catch (Throwable e) {
		// }

		final String[] out = (String[]) obj;

		// 优先尝试直接连接
		if (out[idx_localport].equals("0") == false) {
			// 家庭内网直联
			LogManager
					.info(UIUtil.getUIString("m105", "try direct connect..."));
			IPAndPort l_directIpPort = null;
			try {
				int timeOut = 5000;
				final String ip = out[idx_localip];
				if (ip.startsWith("192.168.")) {
					final String[] ipv4 = StringUtil.splitToArray(ip, ".");
					if (ipv4 != null && ipv4.length == 4) {
						timeOut -= 1000;// 内网，减一秒，原减两秒太长
					}
				}
				final IPAndPort ipport = new IPAndPort(ip,
						Integer.parseInt(out[idx_localport]));
				l_directIpPort = SIPManager.tryBuildConnOnDirect(hcConnection,
						subTag, ipport, "Direct Mode",
						Integer.parseInt(out[idx_nattype]), timeOut, tokenBS);// 内网直联最长时间改为一秒
			} catch (Throwable e) {
			}
			if (l_directIpPort != null) {
				// EnumNAT.OPEN_INTERNET or Symmetric
				infoDirectModeYes();

				// 家庭内网不需要调用本步
				// 发送REG到服务器,触发服务器初始化进程
				// ContextManager.getContextInstance().send(MsgBuilder.E_TAG_SERVER_RELAY_START);

				return CONN_OK_MODE_CONNECTION_HOME_WIRELESS;
			} else {
				LogManager
						.info(UIUtil.getUIString("m107", "direct mode : fail"));
			}
		}

		if (out[idx_relayport].equals("0") == false) {
			LogManager.info(UIUtil.getUIString("m108", "try relay connect..."));
			final int maxConnTryCount = 3;
			int tryCount = 0;
			while (tryCount < maxConnTryCount) {
				final IPAndPort ipport = new IPAndPort(out[idx_relayip],
						Integer.parseInt(out[idx_relayport]));
				final IPAndPort l_relayIpPort = SIPManager.tryBuildConnOnDirect(
						hcConnection, subTag, ipport, "Relay Mode",
						EnumNAT.FULL_AGENT_BY_OTHER,
						SIPManager.REG_WAITING_MS + 1000, tokenBS);// Android机器出现relay
																	// fail情形，再试成功，故增加1秒
				if (l_relayIpPort != null) {
					LogManager.info(
							UIUtil.getUIString("m109", "relay mode : yes"));

					// 中继时，可能并非3G/4G，也许是WiFi，3G/4G由其它逻辑提示
					// LogManager.info("3G/4G/...");//more time and unreliable
					// LogManager.info("Busy network and congestion may cause
					// error");
					hcConnection.relayIpPort = l_relayIpPort;
					return CONN_OK_MODE_CONNECTION_RELAY;
				} else {
					tryCount++;
				}
			}
			LogManager.info(UIUtil.getUIString("m110", "relay mode : fail"));
		}

		if (RootConfig.isFailToGetAliveRootCfg()) {
			RootServerConnector.errorRepareNetwork();
		}

		// 尝试无法连接
		return CONN_ERR_MOBI_FAIL_CONN;
	}

	private static void infoDirectModeYes() {
		LogManager.info(UIUtil.getUIString("m106", "direct mode : yes"));
	}

	public static void infoServerOnline() {
		LogManager.info(UIUtil.getUIString("m104", "server : online"));
	}

	// public static IPAndPort reConnectAfterResetExcep(final HCConnection
	// hcConnection, final byte[] tokenBS){
	// LogManager.log("reConnectAfterExcepReset to " +
	// hcConnection.relayIpPort.ip + ":" + hcConnection.relayIpPort.port);
	// try{
	// return SIPManager.proccReg(hcConnection, hcConnection.relayIpPort,
	// MsgBuilder.DATA_E_TAG_RELAY_REG_SUB_RESET,
	// SIPManager.REG_WAITING_MS, tokenBS);
	// }catch (Throwable e) {
	// //主要拦截hc.core.sip.SIPManager.send，java.net.SocketException
	// e.printStackTrace();//不作ExceptionReport处理。因为较为频繁。
	// }
	// return null;
	// }

	/**
	 * 如果创建成功，则返回true
	 * 
	 * @param ipport
	 * @param remoteNattype
	 * @return
	 */
	private static IPAndPort tryBuildConnOnDirect(
			final HCConnection hcConnection, final byte subTag,
			final IPAndPort ipport, final String memo, final int nattype,
			final int waitMS, final byte[] tokenBS) {
		// LogManager.info("connect " + memo + " Server");

		if (nattype == EnumNAT.FULL_AGENT_BY_OTHER) {
			hcConnection.setOnRelay(true);
		}

		// 客户端连接，使用随机端口
		return SIPManager.proccReg(hcConnection, ipport, subTag, waitMS,
				tokenBS);
	}

	public static final int REG_WAITING_MS = 7000;

	public static IPAndPort proccReg(final HCConnection hcConnection,
			final IPAndPort ipport, final byte firstOrReset,
			final int waitingMS, final byte[] tokenBS) {
		final Object socket = SIPManager.sendRegister(hcConnection, ipport,
				firstOrReset, waitingMS, tokenBS);
		if (socket != null) {
			try {
				hcConnection.sipContext.deploySocket(hcConnection, socket);

				return ipport;
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
				try {
					hcConnection.sipContext.closeSocket(socket);
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
			return null;
		} else {
			// LogManager.info("Fail connect " + ipport.ip + ":" + ipport.port);
			return null;
		}
	}

	public static void notifyCertPwdPassAtClient(final CoreSession coreSS) {
		coreSS.context.setStatus(ContextManager.STATUS_CLIENT_SELF);

		// 仅在手机端执行
		coreSS.context.doExtBiz(IContext.BIZ_SERVER_AFTER_CERTKEY_AND_PWD_PASS,
				null);
		// LogManager.log("Send menu:///root");
		// ContextManager.getContextInstance().send(MsgBuilder.E_CANVAS_MAIN,
		// "menu:///root");
	}

	public static Object sendRegister(final HCConnection hcConnection,
			final IPAndPort ipport, final byte firstOrReset, final int waiteMS,
			final byte[] tokenBS) {
		final Object send = hcConnection.sipContext.buildSocket(0, ipport.ip,
				ipport.port);
		if (send == null) {
			return null;
		}

		final DataReg reg = new DataReg();
		final byte[] bs = new byte[MsgBuilder.UDP_BYTE_SIZE];// DatagramPacketCacher.getInstance().getFree();

		reg.setBytes(bs);

		if (SIPManager.isOnRelay(hcConnection)) {
			if (IConstant.serverSide) {
				reg.setFromServer(MsgBuilder.DATA_IS_SERVER_TO_RELAY);
				reg.setTokenDataIn(tokenBS, 0, tokenBS.length);
			} else {
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

		try {
			SIPManager.send(hcConnection.sipContext.getOutputStream(send), bs,
					0, regLen);
		} catch (final Throwable e) {
			// ExceptionReporter.printStackTrace(e);
			try {
				hcConnection.sipContext.closeSocket(send);
			} catch (final Exception e1) {
			}
			return null;
		}
		final HCTimer closeTimer = new HCTimer("closeTimer", waiteMS, true) {
			public final void doBiz() {
				synchronized (this) {
					if (isEnable) {
						try {
							LogManager.log("timeout on SIP start connect!");
							hcConnection.sipContext.closeSocket(send);
						} catch (final Exception e) {
							ExceptionReporter.printStackTrace(e);
						}
					}
				}
				HCTimer.remove(this);
			}
		};
		try {
			final DataInputStream is = hcConnection.sipContext
					.getInputStream(send);
			final int headLen = MsgBuilder.INDEX_MSG_DATA;
			is.readFully(bs, 0, headLen);

			if (bs[MsgBuilder.INDEX_CTRL_SUB_TAG] == MsgBuilder.DATA_ROOT_SAME_ID_IS_USING
					&& bs[MsgBuilder.INDEX_CTRL_TAG] == MsgBuilder.E_TAG_ROOT) {
				if (IConstant.serverSide) {
					hcConnection.iContext
							.doExtBiz(IContext.BIZ_SHOW_ONCE_SAME_ID, null);
				}
				throw new Exception();
			}

			if (regLen > headLen) {
				is.readFully(bs, headLen, regLen - headLen);
			}

			// 验证是正确的回应，以免连接到已存在在，但是偶巧的端口上
			for (int i = MsgBuilder.INDEX_MSG_DATA, endIdx = regLen; i < endIdx; i++) {
				if (bs[i] != bsClone[i]) {
					LogManager.log("Error back echo data");
					throw new Exception();
				}
			}
			LogManager.log(UIUtil.getUIString("m111", "receive echo"));
			return send;
		} catch (final Throwable e) {
			LogManager.errToLog("fail to receive echo!");
			// e.printStackTrace();//注意：在服务器的keepalive中，下线时，有可能关闭时，输出此异常，故关闭。不能进行ExceptionReporter
			// ExceptionReporter.printStackTrace(e);
			try {
				hcConnection.sipContext.closeSocket(send);
			} catch (final Throwable e1) {
			}
		} finally {
			synchronized (closeTimer) {
				closeTimer.setEnable(false);
			}
			HCTimer.remove(closeTimer);
		}
		return null;
	}

}
