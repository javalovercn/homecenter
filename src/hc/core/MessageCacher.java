package hc.core;


import hc.core.util.Stack;

public class MessageCacher {
	private final static MessageCacher instance = new MessageCacher();
	
	public final static MessageCacher getInstance(){
		return instance;
	}
	
	final private Stack free = new Stack();
	private short stackSize = 0;

	public MessageCacher(){
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
