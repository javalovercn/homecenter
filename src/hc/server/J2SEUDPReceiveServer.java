package hc.server;

import hc.core.UDPReceiveServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class J2SEUDPReceiveServer extends UDPReceiveServer{

	@Override
	public void receiveUDP(Object dp) throws IOException{
        ((DatagramSocket)socket).receive((DatagramPacket)dp);  
	}
	
	@Override
	public void shutDown() {
    	isShutdown = true;
    	try {
    		((DatagramSocket)socket).close();
		} catch (Exception e) {
		}
//    	thread.stop();
	}

	@Override
	public void closeOldSocket() {
		DatagramSocket snapSocket = (DatagramSocket)socket;
		socket = null;
		
		try{
			snapSocket.close();
		}catch (Exception e) {
		}
	}

}
