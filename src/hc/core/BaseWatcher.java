package hc.core;

public abstract class BaseWatcher implements IWatcher {
	public void setPara(Object p) {
	}

	public void cancel() {
	}

	public boolean isCancelable() {
		return false;
	}
}
