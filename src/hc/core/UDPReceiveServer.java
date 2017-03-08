package hc.core;

import hc.core.sip.ISIPContext;
import hc.core.util.EventBackCacher;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;

import java.io.IOException;

public abstract class UDPReceiveServer extends Thread{
	final CoreSession coreSS;
	
	public UDPReceiveServer(final CoreSession coreSS){
		this.coreSS = coreSS;
		setPriority(ThreadPriorityManager.DATA_TRANS_PRIORITY);
        //J2ME不支持setName
		//thread.setName("Receive Server");
    }
	
	protected final Object LOCK = new Object();
	
	protected Object socket;

	public void run(){
		final DatagramPacketCacher cacher = DatagramPacketCacher.getInstance();  
		final EventBackCacher ebCacher = EventBackCacher.getInstance();
		
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

			final ISIPContext isip = coreSS.hcConnection.sipContext;
			final Object dp = cacher.getFree(isip);
        	isip.setDatagramLength(dp, MsgBuilder.UDP_BYTE_SIZE);
            try {
            	receiveUDP(dp);
            	
				final EventBack eb = ebCacher.getFreeEB();
				eb.setBSAndDatalen(coreSS, dp, null, 0);
				coreSS.eventCenterDriver.addWatcher(eb);
            }catch (Exception e) {
				cacher.cycle(dp);

				if(coreSS.hcConnection.sipContext.isNearDeployTime()){
					LogManager.log("UDPReceive Exception near deploy time, maybe closed the old socket.");
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
//		LogManager.log("Changed Receive Socket");
		coreSS.hcConnection.sipContext.enterDeployStatus();
		
		closeOldSocket();
		socket = udpServerSocket;
		synchronized (LOCK) {
			LOCK.notify();
		}
	}

}