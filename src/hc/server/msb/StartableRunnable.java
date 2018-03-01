package hc.server.msb;

import hc.core.util.ExceptionReporter;

public abstract class StartableRunnable implements Runnable {
	boolean isStarted = false;

	public void start() {
		synchronized (this) {
			if (isStarted == false) {
				isStarted = true;
				notify();
			}
		}
	}

	@Override
	public void run() {
		synchronized (this) {
			if (isStarted == false) {
				try {
					wait();
				} catch (final Exception e) {
				}
			}
		}

		try {
			runAfterStart();
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	public abstract void runAfterStart();

}
