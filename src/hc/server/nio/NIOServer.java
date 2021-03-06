package hc.server.nio;

import hc.core.CoreSession;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;

public class NIOServer {
	private static AcceptReadThread at;

	public static UDPPair buildUDPPortPair() {
		return at.buildUDPPortPair();
	}

	/**
	 * 注意：对于用户级中继器，没有实现udpSpeedPort的上传，而只实现了标准中继器的测速UDP端口。
	 * 
	 * @param coreSS
	 * @param ip
	 * @param localPort
	 * @param udpSpeedPort
	 * @param read
	 */
	public NIOServer(final CoreSession coreSS, final String ip, final int localPort, final int udpSpeedPort, final ActionRead read) {
		try {
			LogManager.log("try build relay server on IP : " + ip + ", port : " + localPort);
			at = new AcceptReadThread(coreSS, ip, localPort, udpSpeedPort, read);
		} catch (final Exception e) {
			LogManager.err("Unable NIO Server, IP:" + ip + ", Port:" + localPort);
			ExceptionReporter.printStackTrace(e);
		}
	}

	public boolean isOpen() {
		return at.isOpen();
	}

	// public void rebuildServerChannel(String ip){
	// try {
	// at.rebuildServerChannel(ip);
	// } catch (Exception e) {
	// ExceptionReporter.printStackTrace(e);
	// LogManager.log("rebuildServerChannel:" + e.getMessage());
	// }
	// }

	public void close() {
		try {
			at.close();
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
			LogManager.log("buildServerChannel:" + e.getMessage());
		}

	}

	public void shutdown() {
		// AcceptThread要置后
		at.shutDown();
	}

}
