package hc.core.util;

public class StringBufferCacher {
	private static final Stack free = new Stack(8);
	
	public static StringBuffer getFree(){
		synchronized (free) {
			if(free.size() == 0){
				return new StringBuffer(1024 * 200);
			}else{
				return (StringBuffer)free.pop();
			}
        }
	}
	
	public static void cycle(final StringBuffer sb){
		if(sb == null){
			return;
		}
		
		sb.setLength(0);
		
		synchronized (free) {
			free.push(sb);
        }		
	}
}
