package hc.core.util;


public class ByteArrayCacher {
	private final int[] bytes_size;
	private final Stack[] free_bytes;
	private final int max;
	
	/**
	 * 
	 * @param initBlockSize 最小起步分配尺寸
	 * @param maxBlockSize 最大起步分配尺寸
	 * @param expand 从最小起步尺寸开始的，下级的增长倍数；如2，则尺寸翻倍
	 */
	public ByteArrayCacher(int initBlockSize, int maxBlockSize, int expand){
		bytes_size = new int[20];
		free_bytes = new Stack[20];
		
		int blockSize = initBlockSize;
		int stepMax = 0;
		for (; blockSize <= maxBlockSize; stepMax++) {
	        bytes_size[stepMax] = blockSize;
	        free_bytes[stepMax] = new Stack();
	        blockSize = blockSize * expand;
        }
		
		max = stepMax;
	}
	
	/**
	 * 超大型的特殊获得，不回收
	 */
	public final byte[] getFree(int minSize){
		int i = 0;
		for (; i < max; i++) {
	        if(minSize <= bytes_size[i]){
	        	break;
	        }
        }
		
		if(i == max){
			//超大型的特殊获得，不回收
			return new byte[minSize];
		}else{
			Stack free_sets = free_bytes[i];
	        byte[] out = null;
			synchronized (free_sets) {
				out = (byte[])free_sets.pop();
            }
			if(out != null){
				return out;
			}else{
//				hc.core.L.V=hc.core.L.O?false:LogManager.log("------MEM ALLOCATE [ByteArrayCacher]------");
				return new byte[bytes_size[i]];
			}
		}		
	}
	
	/**
	 * 超大型的自动释放，不回收
	 */
	public final void cycle(byte[] bytes){
		if(bytes == null){
			return;
		}
		
		final int size = bytes.length;

		int i = 0;
		for (; i < max; i++) {
	        if(size == bytes_size[i]){
	        	break;
	        }
        }
		
		if(i == max){
			//超大型的自动释放，不回收
			return ;
		}
		
		final Stack free_sets = free_bytes[i];
		synchronized (free_sets) {
			free_sets.push(bytes);
        }		
		
	}

}
