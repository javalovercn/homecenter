package hc.server.util.ai;

import hc.core.ExecBiz;

public abstract class AIExecBiz extends ExecBiz {
	@Override
	public final boolean watch() {
		try {
			doBiz();
		} catch (final Throwable e) {
			e.printStackTrace();// 不report
		}
		return true;
	}
}
