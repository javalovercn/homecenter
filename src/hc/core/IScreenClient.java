package hc.core;


public interface IScreenClient extends IPNGScreen {
	/**
	 * 客户端高于服务器的屏尺寸，故要缩小原画面尺寸，对于手机环境，本方法基本无效
	 * @param width
	 * @param height
	 */
	public void notifyRemoteSize(int width, int height);
//	public int getClientWidth();
//	public int getClientHeight();
	
	/**
	 * 显示远程服务器回传给客户端的复制文本信息
	 */
	public void sendBackTxtToClient(String txt);
	
	/**
	 * 小于0表示向左移成功，
	 */
	public void succMoveRight(int pixle);
	
	/**
	 * 小于表示向下移成功
	 * @param pixle
	 */
	public void succMoveUp(int pixle);
	
	public void enableMoveUp(boolean e);
	public void enableMoveDown(boolean e);
	public void enableMoveLeft(boolean e);
	public void enableMoveRight(boolean e);
	
	public void copyArea(int x, int y, int width, int height, int dx, int dy);
	public void setGrayForEmptyArea();
	public void fillRect(int x, int y, int width, int height);
	
	/**
	 * 不能再移动或无效移动
	 */
	public void noMoveWarn();

	public void refreshScreen(final int x, final int y, final int w, final int h);
}
