package hc.server.relay;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface ExtReplayBiz {
	/**
	 * 根据情况，进行socket关闭。如果未处理，则返回false。
	 * @param socket
	 * @param bs
	 * @throws Exception
	 */
	public boolean doExt(SocketChannel socket, ByteBuffer buffer) throws Exception;
}
