package hc.server.util;

public class ByteArr {
	public byte[] bytes;
	public int len;
	
	public ByteArr(byte[] bytes, int len){
		this.bytes = bytes;
		this.len = len;
	}
	public byte[] getBytes() {
		return bytes;
	}
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}
	
	public int getLen() {
		return len;
	}
	
	public void setLen(int len) {
		this.len = len;
	}
	
	public boolean equals(Object obj){
		if(obj instanceof ByteArr){
			ByteArr c = (ByteArr)obj;
			if(c.len == this.len){
				for (int i = 0; i < bytes.length; i++) {
					if(c.bytes[i] != bytes[i]){
						return false;
					}
				}
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
		
	}
}
