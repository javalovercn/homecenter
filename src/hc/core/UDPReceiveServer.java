package hc.core;

import hc.core.sip.ISIPContext;
import hc.core.sip.SIPManager;
import hc.core.util.EventBackCacher;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;

import java.io.IOException;

public abstract class UDPReceiveServer extends Thread{
	public UDPReceiveServer(){
		setPriority(ThreadPriorityManager.DATA_TRANS_PRIORITY);
        //J2ME不支持setName
		//thread.setName("Receive Server");
    }
	
	protected final Object LOCK = new Object();
	
	protected Object socket;

	public void run(){
		final DatagramPacketCacher cacher = DatagramPacketCacher.getInstance();  
		final EventBackCacher ebCacher = EventBackCacher.getInstance();
		final ISIPContext isip = SIPManager.getSIPContext();
		
    	while (!isShutdown) {
			if(socket == null){
	    		synchronized (LOCK) {
	    			if(socket == null){
	    				try {
							LOCK.wait();
						} catch (InterruptedException e) {
						}
						continue;
	    			}
				}
			}

			final Object dp = cacher.getFree();
        	isip.setDatagramLength(dp, MsgBuilder.UDP_BYTE_SIZE);
            try {
            	receiveUDP(dp);
            	
				final EventBack eb = ebCacher.getFreeEB();
				eb.setBSAndDatalen(dp, null, 0);
				ConditionWatcher.addWatcher(eb);
            }catch (Exception e) {
				cacher.cycle(dp);

				if(SIPManager.getSIPContext().isNearDeployTime()){
					L.V = L.O ? false : LogManager.log("UDPReceive Exception near deploy time, maybe closed the old socket.");
            		continue;
            	}

				socket = null;
//				if(e.getMessage().toLowerCase().equals("socket closed")){
//					if(!isShutdown){
//						SIPManager.notifyRelineon(false);
//					}
//				}else{
//	            	ExceptionReporter.printStackTrace(e);					
//				}
			}  
        }//while
	}

	protected boolean isShutdown = false;
	
	public abstract void receiveUDP(Object dp) throws IOException;
	
	public void shutDown(){
		isShutdown = true;
		synchronized (LOCK) {
			LOCK.notify();
		}
	}
	
	public abstract void closeOldSocket();

	public void setUdpServerSocket(Object udpServerSocket) {
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("Changed Receive Socket");
		SIPManager.getSIPContext().enterDeployStatus();
		
		closeOldSocket();
		socket = udpServerSocket;
		synchronized (LOCK) {
			LOCK.notify();
		}
	}

}