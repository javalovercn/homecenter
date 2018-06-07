package hc.server.localnet;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import hc.core.ContextManager;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.util.HttpUtil;

public class ReceiveDeployServer {
	public static final boolean KEEP_RUNNING = true;
	public static final String ACCEPT_VERSION = "7.88";
	public static final String PARA_SPLITER = StringUtil.SPLIT_LEVEL_1_AT;
	
	final static int port = 56152;

	static ServerSocket server = null;
	
	/**
	 * 有可能返回null
	 * @return
	 */
	public static String getServerIP() {
		final ServerSocket snap = server;
		if(snap == null) {
			return null;
		}else {
			return snap.getInetAddress().getHostAddress();
		}
	}

	public static void startServer() {
		try {
			final InetAddress ia = HttpUtil.getLocal();

			if (ia == null || ia instanceof Inet6Address) {
				LogManager.log("[Deploy] can't create receive-deploy service on IPv6 interface.");
				return;
			}

			final String ip = ia.getHostAddress();
			if (HttpUtil.isLocalNetworkIP(ip) == false) {
				LogManager.log("[Deploy] can't create receive-deploy service on NON local network.");
				return;
			}

			final String gateIP = ip.substring(0, ip.lastIndexOf('.') + 1) + "1";

			final int backlog = 10;
			if (server != null) {
				server.close();
			}
			server = new ServerSocket(port, backlog, ia);

			final Thread t = new Thread("ReceiveDeployServer") {
				@Override
				public void run() {
					while (server != null) {
						Socket socket = null;
						try {
							socket = server.accept();
							LogManager.log("[Deploy] new deploy is coming.");

							final SocketAddress sa = socket.getRemoteSocketAddress();
							if (sa instanceof InetSocketAddress) {
								final InetSocketAddress isa = (InetSocketAddress) sa;
								final String remoteIP = isa.getAddress().getHostAddress();
								if (remoteIP.equals(gateIP)) {
									LogManager.errToLog("[Deploy] drap data from gateway : " + gateIP);
									socket.close();
									continue;
								}
							}
							final Deploylet deploylet = new Deploylet(new DeploySocket(socket));
							socket = null;
							ContextManager.getThreadPool().run(new Runnable() {
								@Override
								public void run() {
									deploylet.processOneClient();
								}
							});
						} catch (final Throwable e) {
							e.printStackTrace();
						}
					}
					LogManager.errToLog("[Deploy] shutdown deploy service : " + ip);
				}
			};
			t.setDaemon(true);
			t.start();

			LogManager.log("[Deploy] successful start receive-deploy service from local network at [" + ip + "/" + port + "].");
		} catch (final Throwable e) {
			e.printStackTrace();
		}
	}

	public static void stopServer() {
		CCoreUtil.checkAccess();

		if (server != null) {
			final ServerSocket snap = server;
			server = null;
			try {
				snap.close();
			} catch (final Throwable e) {
				e.printStackTrace();
			}
			LogManager.log("[Deploy] stop receive-deploy service, which receives deployment from local network.");
		}
	}
}
