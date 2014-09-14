package hc.core.util;

public class EventBackCacher {
	private static EventBackCacher instance;
	
	public final static EventBackCacher getInstance(){
		if(instance != null){
		}else{
			instance = new EventBackCacher();
		}
		return instance;
	}
	
	final private Stack free = new Stack();
	
	private int freeSize = 0;
	
	public EventBackCacher(){
	}
	
	public final EventBack getFree(){
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