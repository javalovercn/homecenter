package hc.server.relay;

import hc.core.IWatcher;
import hc.core.L;
import hc.core.util.LogManager;

public abstract class RelayShutdownWatch implements IWatcher {
	@Override
	public void setPara(final Object p) {
	}

	long start = 0;

	@Override
	public boolean watch() {
		if (start == 0) {
			start = System.currentTimeMillis();
		}

		if (System.currentTimeMillis() - start > 30000) {
			// 发现可替Relay，但是没有回应
			RelayManager.notifyClientsLineOff();

			if (L.isLogInRelay) {
				LogManager.log("Stop relay task");
			}
			extShutdown();
			return true;
		} else {
			// 等待在途包全部转完，含重发
			try {
				Thread.sleep(200);
			} catch (final Exception e) {

			}
			return false;
		}
	}

	@Override
	public void cancel() {
	}

	@Override
	public boolean isCancelable() {
		return false;
	}

	public abstract void extShutdown();
}
