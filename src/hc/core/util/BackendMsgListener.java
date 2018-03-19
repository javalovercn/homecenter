package hc.core.util;

public interface BackendMsgListener {
	public void notifyNewMsg();

	public void setNextMsgListener(BackendMsgListener next);

	public BackendMsgListener getNextMsgListener();
}
