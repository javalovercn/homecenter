package hc.core.util;

/**
 * watchTrigger检查超时是否发生，如果超时，则执行doBiz，而非定时执行。 不占线程，调用watchTrigger进行检查和触发。
 */
public abstract class TimeWatcher {
	final int trigMS;
	long lastTime;

	public TimeWatcher(final int trigMS) {
		this.trigMS = trigMS;
		lastTime = System.currentTimeMillis();
	}

	public final void watchTrigger() {
		final long now = System.currentTimeMillis();
		if (now - lastTime > trigMS) {
			doBiz();
		}
		lastTime = now;
	}

	public abstract void doBiz();
}
