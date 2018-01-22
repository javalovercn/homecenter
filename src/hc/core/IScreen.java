package hc.core;

public interface IScreen {

	public abstract boolean isSameScreenID(final byte[] bs, final int offset, final int len);

	/**
	 * 注意：最大screenID不能超过255字节，因为某些地方需传送ID，其长度是由一个byte承担。
	 * @param screenID
	 */
	public abstract void setScreenID(final String screenID);
	
	public abstract void setScreenCancelable(final boolean isCancelable);
	
	public abstract boolean isScreenCancelable();
}