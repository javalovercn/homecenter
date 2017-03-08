package hc.core;

import hc.core.sip.ISIPContext;
import hc.core.sip.SIPManager;
import hc.core.util.Stack;

public class DatagramPacketCacher {
	private final static DatagramPacketCacher instance = new DatagramPacketCacher();
	
	public final static DatagramPacketCacher getInstance(){
		return instance;
	}
	
	final private Stack free = new Stack();
	
	private int freeSize = 0;
	
	public DatagramPacketCacher(){
	}
	
	public final Object getFree(final ISIPContext sipContext){
		synchronized (free) {
			if(freeSize == 0){
//				LogManager.log("------MEM ALLOCATE [DatagramPacket]------");
				return sipContext.getDatagramPacket(null);
			}else{
				freeSize--;
				return free.pop();
			}
//			System.out.println("DatagramCache [getFree] Free size:" + fastFreeSize + ", Vector size:" + free.size() + ", obj:" + packate);			
        }
		
	}
	
	public final void cycle(Object dp){		
		synchronized (free) {
			free.push(dp);
			freeSize++;
        }		
	}
}