package hc.core;


import hc.core.util.Stack;

public class HCMessageBuffer {
	private final static HCMessageBuffer instance = new HCMessageBuffer();
	
	public final static HCMessageBuffer getInstance(){
		return instance;
	}
	
	final private Stack free = new Stack();
	private short stackSize = 0;

	public HCMessageBuffer(){
	}
	
	public final HCMessage getFree(){
		synchronized (free) {
			if(stackSize == 0){
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("------MEM ALLOCATE [Message]------");
				return new HCMessage();
			}else{
				stackSize--;
				return (HCMessage)free.pop();
			}
        }
	}
	
	public final void cycle(HCMessage dp){
		synchronized (free) {
			free.push(dp);
			stackSize++;
        }		
	}

}
