package hc.core.util;

public class XorPackage {
	public byte[] bs;
	public int len;
	public long packageID;
	
	final static private Stack free = new Stack();
	private static int freeSize = 0;
	
	public static final XorPackage getFree(){
		synchronized (free) {
			if(freeSize == 0){
//				LogManager.log("------MEM ALLOCATE [HCURL]------");
				return new XorPackage();
			}else{
				freeSize--;
				return (XorPackage)free.pop();
			}
        }
		
	}
	
	public static final void cycle(XorPackage dp){
		synchronized (free) {
			free.push(dp);
			freeSize++;
        }		
	}
}
