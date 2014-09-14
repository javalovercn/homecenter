package hc.core;

public interface IScreen {

	public abstract boolean isSameScreenID(final byte[] bs, final int offset,
			final int len);

	public abstract void setScreenID(final String screenID);

}