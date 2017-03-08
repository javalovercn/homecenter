package hc.core;

import hc.core.util.Stack;

public class HCUDPSubPacketCacher {
	private final static HCUDPSubPacketCacher instance = new HCUDPSubPacketCacher();
	
	public static HCUDPSubPacketCacher getInstance(){
		return instance;
	}
	
	final private Stack free = new Stack();
	private short stackSize = 0;

	public HCUDPSubPacketCacher(){
	}
	
	public final HCUDPSubPacketEvent getFree(){
		synchronized (free) {
			if(stackSize == 0){
//				LogManager.log("------MEM ALLOCATE [HCEvent]------");
				return new HCUDPSubPacketEvent();
			}else{
				stackSize--;
				return (HCUDPSubPacketEvent)free.pop();
			}
        }
	}
	
	public final void cycle(HCUDPSubPacketEvent dp){
		synchronized (free) {
			free.push(dp);
			stackSize++;
        }		
	}
}
