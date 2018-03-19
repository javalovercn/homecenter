package hc.core;

public abstract class ExpiredRunnable implements Runnable {
	private final long expiredMS;

	public ExpiredRunnable(final int maxMS) {
		this.expiredMS = System.currentTimeMillis() + maxMS;
	}

	/**
	 * true : 已处理完毕; false : 需要进行下一次watch
	 * 
	 * @return
	 */
	public abstract boolean doBiz();

	public abstract void notifyExpired();

	public void run() {
		try {
			while (true) {
				Thread.sleep(HCTimer.HC_INTERNAL_MS);

				if (expiredMS < System.currentTimeMillis()) {
					notifyExpired();
					return;
				}

				if (doBiz()) {
					return;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
