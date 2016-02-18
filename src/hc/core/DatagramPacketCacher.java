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
	ISIPContext isip;
	
	public DatagramPacketCacher(){
	}
	
	public final Object getFree(){
		synchronized (free) {
			if(freeSize == 0){
				if(isip == null){
					isip = SIPManager.getSIPContext();
				}
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("------MEM ALLOCATE [DatagramPacket]------");
				return isip.getDatagramPacket(null);
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