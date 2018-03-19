package hc.server.util;

import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.RootConfig;
import hc.core.RootServerConnector;
import hc.core.data.DataNatReqConn;
import hc.core.sip.IPAndPort;
import hc.core.sip.SIPManager;
import hc.core.util.ByteUtil;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.server.DirectServer;
import hc.server.nio.NIOServer;
import hc.util.ConnectionManager;
import hc.util.HttpUtil;
import hc.util.NetInterAddresses;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;
import hc.util.TokenManager;
import hc.util.UPnPUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class StarterParameter {

	public static void startBeforeSession() {
		CCoreUtil.checkAccess();

		// 执行重连前置逻辑
		ConnectionManager.startBeforeConnectBiz();
	}

	public static NIOServer nioRelay;
	public static DirectServer dServer;

	public final static NIOServer getNIORelay() {
		CCoreUtil.checkAccess();

		return nioRelay;
	}

	public final static DirectServer getDirectServer() {
		CCoreUtil.checkAccess();

		return dServer;
	}

	public static boolean buildNIOs(final InetAddress ia, final String networkName, final boolean buildNIO) {
		// try {
		// ia = InetAddress.getByName("0.0.0.0");
		// } catch (UnknownHostException e) {
		// ExceptionReporter.printStackTrace(e);
		// }
		if (nioRelay != null && nioRelay.isOpen()) {
			return true;
		}

		if (buildNIO) {
			LogManager.errToLog("stop relay server now!");
			// nioRelay = new NIOServer(null, ia.getHostAddress(), 0, 0, new
			// RelayActionRead(ctx));
		}

		if (dServer != null) {
			dServer.shutdown();
			dServer = null;
			try {
				Thread.sleep(200);
			} catch (final Exception e) {
			}
		}
		dServer = new DirectServer(ia, networkName);
		dServer.buildServer();
		dServer.start();
		return true;
	}

	public static boolean reconnect() {
		CCoreUtil.checkAccess();

		// removeUPnPMapping方法内使用变量，所以hcgd，
		// 使用情形：有可能网络中断后，又正常。
		UPnPUtil.hcgd = null;

		final String rootIP = RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServer);
		final int rootPort = Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServerPort));

		// 暂停stun IP
		// LogManager.log("try connect homecenter.mobi [" + rootIP + ":" +
		// rootPort + "] for stun IP");
		// publicShowIP = getStunIP(rootIP, rootPort);

		// SIPManager.resetAllConnection();
		String networkInterfacename = PropertiesManager.getValue(PropertiesManager.p_selectedNetwork, HttpUtil.AUTO_DETECT_NETWORK);
		InetAddress ia = null;
		if (networkInterfacename.equals(HttpUtil.AUTO_DETECT_NETWORK)) {
			final String host = (PropertiesManager.isSimu() == false) ? RootServerConnector.HOST_HOMECENTER_MOBI
					: RootServerConnector.IP_192_168_1_102;
			ia = ResourceUtil.searchReachableInetAddress(host);
		} else {
			ia = HttpUtil.getInetAddressByDeviceName(networkInterfacename);
			if (ia == null) {
				LogManager.log("fail build direct server");
				LogManager.log("  on network interface [" + networkInterfacename + "]");
				networkInterfacename = HttpUtil.AUTO_DETECT_NETWORK;
			}
		}
		if (ia == null) {
			ia = HttpUtil.getLocal();
		}
		if (PropertiesManager.isTrue(PropertiesManager.p_ForceRelay) || RootConfig.getInstance().isTrue(RootConfig.p_forceRelay)) {
			if (!PropertiesManager.isTrue(PropertiesManager.p_DisableHomeWireless)) {
				buildNIOs(ia, networkInterfacename, false);
			} else {
				LogManager.log("Disable home direct server.");
			}
		} else {
			if ((!PropertiesManager.isTrue(PropertiesManager.p_DisableDirect))
					&& StarterParameter.checkDirectPublic(ia, StarterParameter.publicShowIP, false)) {
				// localUPnPIP = publicShowIP; homeWirelessIpPort.ip =
				// publicShowIP
				StarterParameter.relayServerUPnPIP = StarterParameter.publicShowIP;

				final boolean b = buildNIOs(ia, networkInterfacename, true);

				StarterParameter.relayServerUPnPPort = StarterParameter.relayServerLocalPort;

				return b;
			}
			if (UPnPUtil.isSupportUPnP(ia)) {
				// 检查是否直接Public

				if (StarterParameter.checkDirectPublic(ia, StarterParameter.publicShowIP, true)) {
					StarterParameter.usingUPnP = true;

					final boolean b = buildNIOs(ia, networkInterfacename, true);

					// startUPnP(ia, 0 0原为localPort成员变量
					final String[] ups = UPnPUtil.startUPnP(ia, 0, StarterParameter.getUPnPPortFromP(PropertiesManager.p_DirectUPnPExtPort),
							TokenManager.getToken());
					StarterParameter.homeWirelessIpPort.port = Integer.parseInt(ups[1]);
					StarterParameter.homeWirelessIpPort.ip = ups[0];

					final String[] relayUPnP = UPnPUtil.startUPnP(ia, StarterParameter.relayServerLocalPort,
							StarterParameter.getUPnPPortFromP(PropertiesManager.p_RelayServerUPnPExtPort), TokenManager.getRelayToken());
					StarterParameter.relayServerUPnPPort = Integer.parseInt(relayUPnP[1]);
					StarterParameter.relayServerUPnPIP = relayUPnP[0];

					PropertiesManager.setValue(PropertiesManager.p_DirectUPnPExtPort, ups[1]);
					PropertiesManager.setValue(PropertiesManager.p_RelayServerUPnPExtPort, relayUPnP[1]);
					PropertiesManager.saveFile();

					return b;
				} else {
					StarterParameter.usingUPnP = false;
				}
			}
		}
		return false;
	}

	static final byte[] helloHC = "Hello from HomeCenter".getBytes();

	static MulticastSocket server;

	static boolean checkMulticastServerAlive() {
		if (checkMulticastServerAlive(RootServerConnector.MULTICAST_IPV4)) {
			return true;
		}
		if (checkMulticastServerAlive(RootServerConnector.MULTICAST_IPV6)) {
			return true;
		}
		return false;
	}

	static boolean checkMulticastServerAlive(final String ip) {
		MulticastSocket client = null;
		byte[] receiveBS = null;
		try {
			final InetAddress targetAddr = InetAddress.getByName(ip);
			client = new MulticastSocket();
			client.setBroadcast(true);
			client.setSoTimeout(4000);

			final byte[] msg = RootServerConnector.getCheckAliveCmdBS();
			final DatagramPacket sendPack = new DatagramPacket(msg, msg.length, targetAddr, RootServerConnector.MULTICAST_PORT);
			client.send(sendPack);
			receiveBS = ByteUtil.byteArrayCacher.getFree(1024);
			sendPack.setData(receiveBS, 0, 1024);
			client.receive(sendPack);

			return ByteUtil.isSame(helloHC, 0, helloHC.length, receiveBS, 0, sendPack.getLength());
		} catch (final Throwable e) {
		} finally {
			if (receiveBS != null) {
				ByteUtil.byteArrayCacher.cycle(receiveBS);
			}
			try {
				client.close();
			} catch (final Throwable e) {
			}
		}
		return false;
	}

	public static void startMulticastServer() {
		CCoreUtil.checkAccess();

		// if (checkMulticastServerAlive()) {
		// LogManager.log("[multicast] multicast server is alive, skip start new
		// multicast server.");
		// return;
		// }

		final Thread t = new Thread() {
			final int dataLen = RootServerConnector.MULTICAST_DATAGRAM_LEN;

			@Override
			public void run() {
				if (L.isInWorkshop) {
					HttpUtil.printAllNetworkInterface();
				}
				server = buildMulticastSocket(RootServerConnector.MULTICAST_IPV4);
				if (server == null) {
					server = buildMulticastSocket(RootServerConnector.MULTICAST_IPV6);
				}
				if (server == null) {
					LogManager.errToLog("[multicast] fail to create mutlicast socket!");
					return;
				}

				final byte[] bs = new byte[dataLen];
				while (true) {
					final DatagramPacket recvPack = new DatagramPacket(bs, dataLen);
					try {
						server.receive(recvPack);
					} catch (final Exception ex) {
						ex.printStackTrace();
						continue;
					}

					final int len = recvPack.getLength();
					L.V = L.WShop ? false
							: LogManager.log("[multicast] receice query from : " + recvPack.getAddress().toString() + ", port : "
									+ recvPack.getPort());
					if (RootServerConnector.isQueryDirectServerIPCmd(bs, 0, len)) {// query:user@email.com
						final byte[] queryBS = RootServerConnector.getQueryDirectServerCmdBS();
						final byte[] uuidBS = IConstant.getUUIDBS();
						if (ByteUtil.isSame(bs, queryBS.length, len - queryBS.length, uuidBS, 0, uuidBS.length)) {
							L.V = L.WShop ? false : LogManager.log("[multicast] query direct server IP/Port.");
							sendDirectServerIP(bs, recvPack);
						}
					} else if (RootServerConnector.isCheckAliveCmd(bs, 0, len)) {
						sendHelloFromServer(bs, recvPack);
					}
				}
			}

			private final MulticastSocket buildMulticastSocket(final String ip) {
				try {
					final InetAddress inetRemoteAddr = InetAddress.getByName(ip);
					final MulticastSocket server = new MulticastSocket(RootServerConnector.MULTICAST_PORT);
					final NetInterAddresses preferNI = null;// HttpUtil.getPreferNetworkInterface()
					final MulticastSocket out = tryJoinGroup(server, inetRemoteAddr, preferNI);
					if (out != null) {
						return out;
					}
				} catch (final Throwable e) {
				}
				return null;
			}

			final MulticastSocket tryJoinGroup(final MulticastSocket server, final InetAddress addr, final NetInterAddresses ni) {
				try {
					if (ni != null) {
						server.setNetworkInterface(ni.ni);
						server.setInterface(ni.getWLANInetAddress());
						final int port = RootServerConnector.MULTICAST_PORT;
						server.joinGroup(addr);
						LogManager.log("[multicast] success join group address : " + addr.toString() + ", port : " + port
								+ ", from networkInterface : " + ni.ni.toString());
						return server;
					} else {
						final Vector<NetInterAddresses> list = HttpUtil.getWLANNetworkInterfaces();
						final int size = list.size();
						for (int i = 0; i < size; i++) {
							final MulticastSocket out = tryJoinGroup(server, addr, list.elementAt(i));
							if (out != null) {
								return out;
							}
						}
					}
				} catch (final Throwable e) {
				}
				return null;
			}

			final void sendHelloFromServer(final byte[] bs, final DatagramPacket recvPack) {
				System.arraycopy(helloHC, 0, bs, 0, helloHC.length);
				recvPack.setLength(helloHC.length);

				try {
					server.send(recvPack);
				} catch (final Throwable ex) {
					ex.printStackTrace();
				}
			}

			final void sendDirectServerIP(final byte[] bs, final DatagramPacket recvPack) {
				final StringBuilder sb = StringBuilderCacher.getFree();
				sb.append(homeWirelessIpPort.ip);
				sb.append(StringUtil.SPLIT_LEVEL_2_JING);
				sb.append(homeWirelessIpPort.port);
				final String sendInfo = sb.toString();
				StringBuilderCacher.cycle(sb);
				final byte[] sendBS = StringUtil.getBytes(sendInfo);
				System.arraycopy(sendBS, 0, bs, 0, sendBS.length);
				recvPack.setLength(sendBS.length);

				try {
					server.send(recvPack);
					L.V = L.WShop ? false
							: LogManager.log("[multicast] successful send direct server ip/port to " + recvPack.getAddress().toString()
									+ ", port : " + recvPack.getPort());
				} catch (final Throwable ex) {
					ex.printStackTrace();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	public static boolean usingUPnP = false;

	public static void removeUPnPMapping() {
		if (usingUPnP) {
			UPnPUtil.removeUPnPMapping(StarterParameter.homeWirelessIpPort.port);
			UPnPUtil.removeUPnPMapping(StarterParameter.relayServerUPnPPort);
		}
	}

	public static void setHomeWirelessIPPort(final String ip, final int port) {
		CCoreUtil.checkAccess();
		homeWirelessIpPort.ip = ip;
		homeWirelessIpPort.port = port;
	}

	public static IPAndPort getHomeWirelessIPPort() {
		CCoreUtil.checkAccess();
		return homeWirelessIpPort;
	}

	public static String getHomeWirelessIP() {
		CCoreUtil.checkAccess();
		return homeWirelessIpPort.ip;
	}

	public static int getHomeWirelessPort() {
		CCoreUtil.checkAccess();
		return homeWirelessIpPort.port;
	}

	final static IPAndPort homeWirelessIpPort = new IPAndPort();
	public static int relayServerLocalPort;
	public static int relayServerUPnPPort;
	// 各型IP必须初始化为空串，以提供上传时不出现问题
	public static String relayServerUPnPIP = "";
	public static String relayUPnPToken;
	public static String publicShowIP = "";

	public static boolean checkDirectPublic(final InetAddress iaddress, String ip, final boolean useUPnP) {
		String[] ups = null;
		ServerSocket upnplisten = null;
		try {

			upnplisten = new ServerSocket(0);
			int testDPPort = upnplisten.getLocalPort();

			upnplisten.setSoTimeout(5000);

			if (useUPnP) {
				ups = UPnPUtil.startUPnP(iaddress, testDPPort, 100, "checkDP");

				testDPPort = Integer.parseInt(ups[1]);
				ip = ups[0];
			}

			final byte[] bs = new byte[MsgBuilder.UDP_BYTE_SIZE];// DatagramPacketCacher.getInstance().getFree();
			if (StarterParameter.pingRoot(ip, testDPPort, bs) == false) {
				return false;
			}

			final Socket publicIn = upnplisten.accept();
			LogManager.log("Finding public (UPnP) ip/port");

			publicIn.close();

			return true;
		} catch (final Exception e) {
		} finally {
			if (ups != null) {
				UPnPUtil.removeUPnPMapping(Integer.parseInt(ups[1]));
			}
			if (upnplisten != null) {
				try {
					upnplisten.close();
				} catch (final IOException e1) {
				}
			}
		}

		return false;
	}

	private static boolean pingRoot(final String echoIP, final int udpPort, final byte[] bs) {
		final DataNatReqConn nrn = new DataNatReqConn();
		nrn.setBytes(bs);
		nrn.setRemotePort(udpPort);
		nrn.setRemoteIP(echoIP);

		bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_ROOT;
		bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = MsgBuilder.DATA_ROOT_UPNP_TEST;
		// bs[MsgBuilder.INDEX_PACKET_SPLIT] = MsgBuilder.DATA_PACKET_NOT_SPLIT;

		final Socket socket = buildSocket(0, RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServer),
				Integer.parseInt(RootConfig.getInstance().getProperty(RootConfig.p_RootRelayServerPort)));
		if (socket == null) {
			// 增加null判断，以减少大量后续的错误
			return false;
		}

		OutputStream os = null;
		try {
			os = socket.getOutputStream();

			SIPManager.send(os, bs, 0, nrn.getLength() + MsgBuilder.INDEX_MSG_DATA);

			os.close();
			socket.close();

			return true;
		} catch (final Exception e) {
			if (L.isInWorkshop) {
				ExceptionReporter.printStackTrace(e);
			}
			// 因为已经混淆，所以此处无必要。
			// ExceptionReporter.printStackTrace(e);
			return false;
		}
	}

	public static int getUPnPPortFromP(final String extPort) {
		final String directUPnPExtPort = PropertiesManager.getValue(extPort);
		if (directUPnPExtPort == null) {
			return 0;
		} else {
			return Integer.parseInt(directUPnPExtPort);
		}
	}

	private static Socket buildSocket(final int localPort, final String targetServer, final int targetPort) {
		try {
			final InetAddress localAddress = ResourceUtil.searchReachableInetAddress(targetServer);
			final Socket socket = new Socket(InetAddress.getByName(targetServer), targetPort, localAddress, localPort);
			socket.setSoTimeout(6000);
			return socket;
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean checkServerOnline(final String remoteIP, final int remotePort, final byte[] uuid) {
		byte[] bs = null;
		Socket socket = null;
		try {
			socket = buildSocket(0, remoteIP, remotePort);
			if (socket == null) {
				return false;
			}
			bs = ByteUtil.byteArrayCacher.getFree(1024);

			bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_ROOT;
			bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = MsgBuilder.DATA_ROOT_SAME_ID_CHECK_ALIVE;

			final int dataLen = uuid.length;
			System.arraycopy(uuid, 0, bs, MsgBuilder.INDEX_MSG_DATA, dataLen);
			final OutputStream os = socket.getOutputStream();
			final int len = MsgBuilder.INDEX_MSG_DATA + dataLen;

			SIPManager.send(os, bs, 0, len);

			final InputStream is = socket.getInputStream();
			is.read(bs);

			socket.close();

			return true;
		} catch (final Exception e) {
			if (L.isInWorkshop) {
				ExceptionReporter.printStackTrace(e);
			}
		} finally {
			ByteUtil.byteArrayCacher.cycle(bs);
		}
		return false;
	}

	public static String getStunIP(final String remoteIP, final int remotePort) {
		byte[] bs = null;
		Socket socket = null;
		try {
			socket = buildSocket(0, remoteIP, remotePort);
			if (socket == null) {
				return "";
			}
			bs = new byte[MsgBuilder.UDP_BYTE_SIZE];// DatagramPacketCacher.getInstance().getFree();

			final DataNatReqConn nrn = new DataNatReqConn();
			nrn.setBytes(bs);

			nrn.setRemotePort(0);

			bs[MsgBuilder.INDEX_CTRL_TAG] = MsgBuilder.E_TAG_ROOT;
			bs[MsgBuilder.INDEX_CTRL_SUB_TAG] = MsgBuilder.DATA_ROOT_UPNP_TEST;
			// bs[MsgBuilder.INDEX_PACKET_SPLIT] =
			// MsgBuilder.DATA_PACKET_NOT_SPLIT;

			final OutputStream os = socket.getOutputStream();
			final int len = MsgBuilder.INDEX_MSG_DATA + nrn.getLength();

			SIPManager.send(os, bs, 0, len);

			final InputStream is = socket.getInputStream();
			is.read(bs);

			final String natIP = nrn.getRemoteIP();

			socket.close();

			return natIP;
		} catch (final Exception e) {
			if (L.isInWorkshop) {
				ExceptionReporter.printStackTrace(e);
			}
		}
		return "";
	}
}