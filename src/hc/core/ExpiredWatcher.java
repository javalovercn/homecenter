package hc.core;

public abstract class ExpiredWatcher implements IWatcher {
	private final long expiredMS;

	public ExpiredWatcher(final int maxMS) {
		this.expiredMS = System.currentTimeMillis() + maxMS;
	}

	/**
	 * true : 已处理完毕; false : 需要进行下一次watch
	 * 
	 * @return
	 */
	public abstract boolean doBiz();

	public abstract void notifyExpired();

	public boolean watch() {
		if (expiredMS < System.currentTimeMillis()) {
			notifyExpired();
			return true;
		}

		if (doBiz()) {
			return true;
		}

		return false;
	}

	public void setPara(Object p) {
	}

	public void cancel() {
	}

	public boolean isCancelable() {
		return false;
	}

}
