package hc.core.util;

/**
 * watchTrigger检查超时是否发生，如果超时，则执行doBiz，而非定时执行。
 *
 */
public abstract class TimeWatcher {
	final int trigMS;
	long lastTime;

	public TimeWatcher(final int trigMS){
		this.trigMS = trigMS;
		lastTime = System.currentTimeMillis();
	}
	
	public void watchTrigger(){
		final long now = System.currentTimeMillis();
		if(now - lastTime > trigMS){
			doBiz();
		}
		lastTime = now;
	}
	
	public abstract void doBiz();
}
