package hc.server.relay;

import hc.core.IConstant;
import hc.core.IContext;
import hc.server.nio.ActionRead;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class RelayActionRead extends ActionRead {
	final IContext ctx;

	public RelayActionRead(final IContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void action(final SelectionKey key) {
		RelayManager.relay(ctx, key);
	}

	final int max_session_num = IConstant.getInstance().getInt(IConstant.RelayMax);

	@Override
	public boolean isTop(final SocketChannel channel) {
		if (RelayManager.isShutdowning || RelayManager.disableRelay
				|| (RelayManager.size >= max_session_num)) {
			return true;
		}
		return false;
	}
}
