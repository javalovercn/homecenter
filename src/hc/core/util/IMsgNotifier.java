package hc.core.util;

public interface IMsgNotifier {
	public void notifyNewMsg(String msg);

	/**
	 * 如果没有，则返回null
	 * 
	 * @return
	 */
	public String getNextMsg();
}
