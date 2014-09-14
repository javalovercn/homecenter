package hc.core.util;


public class HCURLCacher {
	private static HCURLCacher instance;
	
	public final static HCURLCacher getInstance(){
		if(instance != null){
		}else{
			instance = new HCURLCacher();
		}
		return instance;
	}
	
	final private Stack free = new Stack();
	
	private int freeSize = 0;
	
	public HCURLCacher(){
	}
	
	public final HCURL getFree(){
		synchronized (free) {
			if(freeSize == 0){
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("------MEM ALLOCATE [HCURL]------");
				return new HCURL();
			}else{
				freeSize--;
				return (HCURL)free.pop();
			}
        }
		
	}
	
	public final void cycle(HCURL dp){
		dp.removeAllParaValues();
		synchronized (free) {
			free.push(dp);
			freeSize++;
        }		
	}
}