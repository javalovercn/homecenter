package hc.core.util;

public abstract class RepeatManager {
	long lastMS;
	int occurCounter;

	public final void reset() {
		lastMS = 0;
		occurCounter = 0;
	}

	public final long getLastMS() {
		return lastMS;
	}

	public final int getCount() {
		return occurCounter;
	}

	/**
	 * true means processed and need reset.
	 * 
	 * @return
	 */
	public abstract boolean repeatAction();

	public final void occur() {
		if (lastMS == 0) {
			lastMS = System.currentTimeMillis();
		}
		occurCounter++;
		if (repeatAction()) {
			reset();
		}
	}
}
