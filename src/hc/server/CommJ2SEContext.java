package hc.server;

import hc.core.EventCenter;
import hc.core.IContext;
import hc.server.ui.design.J2SESession;
import hc.util.ResourceUtil;

public abstract class CommJ2SEContext extends IContext {
	public CommJ2SEContext(final boolean isRootRelay, final J2SESession socketSession, final EventCenter eventCenter) {
		super(socketSession, eventCenter);

		if (isRootRelay) {// 仅供root级，用户级移至KeepaliveManager
			ResourceUtil.buildAliveRefresher(socketSession, isRootRelay);// 不考虑remove
			// HCTimer
		}
	}

	@Override
	public void run() {
	}
}