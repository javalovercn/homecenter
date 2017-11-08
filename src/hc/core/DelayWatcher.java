package hc.core;

import hc.core.util.ExceptionReporter;

public abstract class DelayWatcher implements IWatcher {
	private final long execMS;
	
	public DelayWatcher(final int msDelay){
		if(msDelay <= 0){
			new IllegalArgumentException("error parameter delaywatcher : " + msDelay);
		}
		execMS = System.currentTimeMillis() + msDelay;
	}
	
	public boolean watch() {
		if(checkWatchCond() == false){
			return true;
		}
		
		if(System.currentTimeMillis() > execMS){
			try{
				doBiz();
			}catch (Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
			return true;
		}
		return false;
	}
	
	public abstract void doBiz();

	public void setPara(Object p) {
	}
	
	/**
	 * true : 属于有效watcher，仍需继续
	 * @return
	 */
	public boolean checkWatchCond(){
		return true;
	}

	public void cancel() {
	}

	public boolean isCancelable() {
		return false;
	}
}
