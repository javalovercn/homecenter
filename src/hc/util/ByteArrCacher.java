package hc.util;

import hc.core.util.Stack;
import hc.server.util.ByteArr;

public class ByteArrCacher {
	private final Stack free;
	private final int byte_size;

	public ByteArrCacher(int byte_size){
        free = new Stack();
        this.byte_size = byte_size;
	}
	
	public ByteArr getFree(){
		synchronized (free) {
			if(free.size() == 0){
				byte[] buffer=new byte[byte_size];
				return new ByteArr(buffer, byte_size);
			}else{
				return (ByteArr)free.pop();
			}
        }
	}
	
	public void cycle(ByteArr dp){
		synchronized (free) {
			free.push(dp);
        }		
	}
}
