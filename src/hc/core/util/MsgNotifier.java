package hc.core.util;

public class MsgNotifier implements IMsgNotifier {
	public String[] getAllMsgs() {
		synchronized (this) {
			String[] out = new String[size];
			for (int i = 0; i < size; i++) {
				out[i] = msgBuffer[i];
			}
			return out;
		}
	}

	public void empty() {
		synchronized (this) {
			for (int i = 0; i < size; i++) {
				msgBuffer[i] = null;
			}
			size = 0;
		}
	}

	public String getNextMsg() {
		String lineMsg = null;
		synchronized (this) {
			if (size > 0) {
				lineMsg = msgBuffer[0];

				for (int i = 0; i < (size - 1); i++) {
					msgBuffer[i] = msgBuffer[i + 1];
				}

				msgBuffer[--size] = null;
				return lineMsg;
			}
		}
		return null;
	}

	String[] msgBuffer = new String[20];
	int size = 0;
	private BackendMsgListener currListener;

	public void setCurrListener(BackendMsgListener next) {
		if (currListener != null) {
			next.setNextMsgListener(currListener);
		}
		currListener = next;
	}

	public void popCurrListener() {
		currListener = currListener.getNextMsgListener();
	}

	public void notifyNewMsg(String msg) {
		synchronized (this) {
			if (size < msgBuffer.length) {
				msgBuffer[size++] = msg;
			}
		}
		if (currListener != null) {
			currListener.notifyNewMsg();
		}
	}

	public static MsgNotifier getInstance() {
		return instance;
	}

	private static MsgNotifier instance = new MsgNotifier();

}
