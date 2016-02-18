package hc.core.util;

import hc.core.EventBack;


public class EventBackCacher {
	private static final EventBackCacher instance = new EventBackCacher();
	
	public final static EventBackCacher getInstance(){
		return instance;
	}
	
	final private Stack free = new Stack();
	
	private int freeSize = 0;
	
	public EventBackCacher(){
	}
	
	public final EventBack getFreeEB(){//iOS环境下会与出现JavaUtilArrayList getFree
		synchronized (free) {
			if(freeSize == 0){
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("------MEM ALLOCATE [EventBack]------");
				return new EventBack();
			}else{
				freeSize--;
				return (EventBack)free.pop();
			}
        }
		
	}
	
	public final void cycle(EventBack dp){
		synchronized (free) {
			free.push(dp);
			freeSize++;
        }		
	}
}