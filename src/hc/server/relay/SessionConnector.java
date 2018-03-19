package hc.server.relay;

import hc.core.IConstant;
import hc.core.L;
import hc.core.MsgBuilder;
import hc.core.util.LinkedSet;
import hc.core.util.LogManager;
import hc.server.nio.ByteBufferCacher;
import hc.server.nio.UDPPair;
import hc.server.util.ByteArr;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Random;

public class SessionConnector {
	// 服务器端接入Channel
	public SocketChannel serverSide;
	public long firstServerRegMS;
	// 手机端端接入Channel
	public SocketChannel clientSide;
	public boolean isMatched = false;
	public boolean isDelTDN = false;
	public boolean isDirectOK = false;

	public SelectionKey serverKey, clientKey;
	public final LinkedSet writeToServerBackSet, writeToClientBackSet;
	private int sizeWriteToServerBackSet, sizeWriteToClientBackSet;
	public final ByteBufferCacher bbCache;
	public ByteArr uuidbs;
	public String token;
	public UDPPair udpPair;
	public final byte[] randomUDPHead = new byte[MsgBuilder.LEN_UDP_HEADER];

	public final void buildRandomUDPHeader(final byte[] bs, final int fillStartIdx) {
		final Random r = new Random(System.currentTimeMillis());

		for (int i = 0; i < randomUDPHead.length; i++) {
			randomUDPHead[i] = (byte) (r.nextInt() & 0xFF);
			bs[fillStartIdx + i] = randomUDPHead[i];
		}
	}

	// 投入使用，则状态为false；回收后，则状态为true
	public boolean isFreeStatus = true;

	public static boolean resetXXSideUDPAddressNull(final byte[] bs, final int offset, final int len, final boolean isServer,
			final byte udpRandomHead0, final byte udpRandomHead1) {
		final SessionConnector sc;

		synchronized (RelayManager.tdn) {
			sc = RelayManager.tdn[len].getNodeData(bs, offset, offset + len);
		}

		if (sc != null) {
			if (sc.randomUDPHead[0] == udpRandomHead0 && sc.randomUDPHead[1] == udpRandomHead1) {
				if (L.isLogInRelay) {
					LogManager.log("SetUDPAddrNull match the randomUDPHeader");
				}
				final UDPPair udpPair = sc.udpPair;
				if (udpPair == null) {
					return false;
				}
				if (udpPair.isServerPort == isServer) {
					udpPair.addr = null;
					return true;
				}
				if (udpPair.target != null && (udpPair.target.isServerPort == isServer)) {
					udpPair.target.addr = null;
					return true;
				}
			}
		}
		return false;
	}

	public SessionConnector(final ByteBufferCacher bbCache) {
		writeToServerBackSet = new LinkedSet();
		writeToClientBackSet = new LinkedSet();

		this.bbCache = bbCache;
	}

	/**
	 * 将一个数据体加挂到输出Channel的后备队列
	 * 
	 * @param writetarget
	 * @param bb
	 * @param serverOrClientReset
	 *            如果writetarget为空，则通过本参数来指明为输出到服务端或客户端
	 */
	public final void appendWriteSet(final SocketChannel writetarget, final ByteBuffer bb, final boolean serverOrClientReset) {
		if (serverSide == writetarget) {
			sizeWriteToServerBackSet++;
			writeToServerBackSet.addTail(bb);
		} else if (clientSide == writetarget) {
			sizeWriteToClientBackSet++;
			writeToClientBackSet.addTail(bb);
		} else if (writetarget == null) {
			// 因为ClientReset，可能使writetarget为null，而进行数据暂存
			if (serverOrClientReset) {
				sizeWriteToServerBackSet++;
				writeToServerBackSet.addTail(bb);
			} else {
				sizeWriteToClientBackSet++;
				writeToClientBackSet.addTail(bb);
			}
		}
	}

	public final void setRewriteSet(final SocketChannel writetarget, final ByteBuffer bb) {
		if (serverSide == writetarget) {
			sizeWriteToServerBackSet++;
			writeToServerBackSet.addToFirst(bb);
		} else if (clientSide == writetarget) {
			sizeWriteToClientBackSet++;
			writeToClientBackSet.addToFirst(bb);
		}
	}

	/**
	 * 
	 * @param writetarget
	 * @param serverOrClientReset
	 * @return 返回999999999，表示错误
	 */
	public final int getWriteSetSize(final SocketChannel writetarget, final boolean serverOrClientReset) {
		if (serverSide == writetarget) {
			return sizeWriteToServerBackSet;
		} else if (clientSide == writetarget) {
			return sizeWriteToClientBackSet;
		} else if (writetarget == null) {
			// 检查重连模式前，是否存在缓存数据
			if (serverOrClientReset) {
				return sizeWriteToServerBackSet;
			} else {
				return sizeWriteToClientBackSet;
			}
		}
		return 999999999;
	}

	/**
	 * 从输出Channel的取出一个数据体
	 * 
	 * @param writetarget
	 * @return
	 */
	public final ByteBuffer getWriteSet(final SocketChannel writetarget) {
		if (serverSide == writetarget) {
			if (sizeWriteToServerBackSet > 0) {
				sizeWriteToServerBackSet--;
				return (ByteBuffer) writeToServerBackSet.getFirst();
			}
		} else if (clientSide == writetarget) {
			if (sizeWriteToClientBackSet > 0) {
				sizeWriteToClientBackSet--;
				return (ByteBuffer) writeToClientBackSet.getFirst();
			}
		}

		return null;
	}

	public final String getUUIDString() {
		try {
			return new String(uuidbs.bytes, 0, uuidbs.len, IConstant.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			return new String(uuidbs.bytes, 0, uuidbs.len);
		}
	}

	/**
	 * 如果是ServerChannel返回true；如果是ClientChannel返回false。否则返回异常
	 * 
	 * @param channel
	 * @return
	 * @throws Exception
	 */
	public final boolean isServerChannel(final SocketChannel channel) {
		if (serverSide == channel) {
			return true;
		} else {// if(fromClient == channel){
			return false;
			// }else{
			// throw new Exception("No server or client channel");
		}
	}

	public final SocketChannel getTarget(final SocketChannel channel) {
		if (serverSide == channel) {
			return clientSide;
		} else if (clientSide == channel) {
			return serverSide;
		} else {
			return null;
		}
	}

	public final void setKey(final SocketChannel channel, final SelectionKey sk, final boolean isFromServer) {
		if (isFromServer) {
			if (serverSide != null) {
				if (L.isLogInRelay) {
					LogManager.log("override old Server channel:" + serverSide.hashCode());
				}
			}
			serverSide = channel;
			serverKey = sk;
		} else {
			if (clientSide != null) {
				if (L.isLogInRelay) {
					LogManager.log("override old Client channel:" + clientSide.hashCode());
				}
			}
			clientSide = channel;
			clientKey = sk;
		}
	}

	public final void reset() {
		clientSide = null;
		serverSide = null;
		clientKey = null;
		serverKey = null;
		uuidbs = null;
		token = null;

		isDelTDN = false;
		isDirectOK = false;
		isMatched = false;

		if (udpPair != null) {
			udpPair.reset();
			udpPair = null;
		}

		cyc(writeToServerBackSet);
		sizeWriteToServerBackSet = 0;

		cyc(writeToClientBackSet);
		sizeWriteToClientBackSet = 0;
	}

	private final void cyc(final LinkedSet linkedSet) {
		bbCache.cycleSet(linkedSet);
	}

}
