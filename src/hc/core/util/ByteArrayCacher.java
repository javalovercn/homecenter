package hc.core.util;


public class ByteArrayCacher {
	private int[] BYTES_SIZE;
	private Stack[] FREE_BYTES;
	private int MAX = 0;
	
	/**
	 * 
	 * @param initBlockSize 最小起步分配尺寸
	 * @param maxBlockSize 最大起步分配尺寸
	 * @param expand 从最小起步尺寸开始的，下级的增长倍数；如2，则尺寸翻倍
	 */
	public ByteArrayCacher(int initBlockSize, int maxBlockSize, int expand){
		BYTES_SIZE = new int[20];
		FREE_BYTES = new Stack[20];
		
		int blockSize = initBlockSize;
		for (; blockSize <= maxBlockSize; MAX++) {
	        BYTES_SIZE[MAX] = blockSize;
	        FREE_BYTES[MAX] = new Stack();
	        blockSize = blockSize * expand;
        }
	}
	
	public byte[] getFree(int minSize){
		int i = 0;
		for (; i < MAX; i++) {
	        if(minSize <= BYTES_SIZE[i]){
	        	break;
	        }
        }
		
		if(i == MAX){
//			u.L.V = u.L.O ? false : Util.error("不合法的内存请求尺寸");
			return new byte[minSize];
		}else{
			Stack free_sets = FREE_BYTES[i];
	        byte[] out = null;
			synchronized (free_sets) {
				if(free_sets.isEmpty()){
					
				}else{
					out = (byte[])free_sets.pop();
				}
            }
			if(out != null){
				return out;
			}
		}		
		
//		hc.core.L.V=hc.core.L.O?false:LogManager.log("------MEM ALLOCATE [ByteArrayCacher]------");
		return new byte[BYTES_SIZE[i]];
	}
	
	public void cycle(byte[] bytes){
		if(bytes == null){
			return;
		}
		
		int size = bytes.length;

		int i = 0;
		for (; i < MAX; i++) {
	        if(size == BYTES_SIZE[i]){
	        	break;
	        }
        }
		
		if(i == MAX){
			return ;
		}
		
		Stack free_sets = FREE_BYTES[i];
		synchronized (free_sets) {
			free_sets.push(bytes);
        }		
		
	}

}
