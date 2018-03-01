package hc.server;

import hc.core.UDPReceiveServer;
import hc.server.ui.design.J2SESession;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class J2SEUDPReceiveServer extends UDPReceiveServer {
	public J2SEUDPReceiveServer(final J2SESession socketSession) {
		super(socketSession);
	}

	@Override
	public void receiveUDP(final Object dp) throws IOException {
		((DatagramSocket) socket).receive((DatagramPacket) dp);
	}

	@Override
	public final void shutDown() {
		super.shutDown();

		try {
			((DatagramSocket) socket).close();
		} catch (final Exception e) {
		}
	}

	@Override
	public void closeOldSocket() {
		final DatagramSocket snapSocket = (DatagramSocket) socket;
		socket = null;

		try {
			snapSocket.close();
		} catch (final Exception e) {
		}
	}

}
