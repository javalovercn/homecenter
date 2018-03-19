package hc.core;

public interface IPNGScreen extends IScreen {

	/**
	 * 
	 * @param x
	 *            远程绝对坐标
	 * @param y
	 *            远程绝对坐标
	 * @param w
	 * @param h
	 * @param bs
	 *            存储PNG数据的数组
	 * @param offset
	 * @param len
	 */
	public abstract void drawImage(int x, int y, int w, int h, byte[] bs,
			int offset, int len);

	public abstract void refreshScreen(final boolean isRefreshNow);
}