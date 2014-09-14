package hc.server.relay;

import hc.core.IConstant;
import hc.server.nio.ActionRead;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class RelayActionRead extends ActionRead{
	@Override
	public void action(SelectionKey key) {
		RelayManager.relay(key);
	}
	
	final int max_session_num = IConstant.getInstance().getInt(IConstant.RelayMax);
	
	@Override
	public boolean isTop(SocketChannel channel) {
		if( RelayManager.isShutdowning || RelayManager.disableRelay || 
				(RelayManager.size >= max_session_num)){
			return true;
		}
		return false;
	}
}
