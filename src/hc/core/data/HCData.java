package hc.core.data;

public abstract class HCData {
	public byte[] bs;
	
	public void setBytes(byte[] bs){
		this.bs = bs;
	}
	
	/**
	 * 传送数据内容长度，而非物理包的长度
	 * @return
	 */
	public abstract int getLength();
}
