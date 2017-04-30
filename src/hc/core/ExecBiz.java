package hc.core;

import hc.core.util.ExceptionReporter;

public abstract class ExecBiz implements IWatcher {
	public boolean watch() {
		try{
			doBiz();
		}catch (Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return true;
	}
	
	public abstract void doBiz();

	public void setPara(Object p) {
	}

	public void cancel() {
	}

	public boolean isCancelable() {
		return false;
	}

}
