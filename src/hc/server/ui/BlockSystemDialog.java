package hc.server.ui;

import hc.server.ui.design.SystemDialog;

/**
 * it will block user thread when show on client
 */
public class BlockSystemDialog extends SystemDialog {
	private static final long serialVersionUID = 7903127637532524135L;
	final Object lock;

	public BlockSystemDialog(final Object lock) {
		this.lock = lock;
		setCancelableToNo();
	}

	public final void notifyBlockUserThread() {
		synchronized (lock) {
			lock.notifyAll();
		}
	}
}
