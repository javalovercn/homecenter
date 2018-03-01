package hc.server.nio;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class ActionRead {
	public abstract void action(SelectionKey key);

	public abstract boolean isTop(SocketChannel channel);
}
