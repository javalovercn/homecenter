package hc.server.localnet;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.util.ResourceUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class DeploySender {

	private static DeploySocket buildDeploySocket(final String hostAddress,
			final boolean showErrInfo) throws Exception {
		final Socket s = buildSocketTo(hostAddress, showErrInfo);
		return new DeploySocket(s);
	}

	public static boolean isAlive(final String ip, final String projectID) {

		final Socket s = buildSocketTo(ip, false);

		if (s == null) {
			return false;
		}

		try {
			final DeploySocket socket = new DeploySocket(s);
			final byte[] projectBS = ByteUtil.getBytes(projectID, IConstant.UTF_8);
			final int projectBSLen = projectBS.length;

			socket.sendHeader(DeploySocket.H_HELLO, projectBSLen);
			socket.sendData(projectBS, 0, projectBSLen, false, false, null);

			final boolean isHello = (socket.receive() == DeploySocket.H_HELLO);
			final int respLen = socket.receiveDataLen();
			boolean hasAliveSameProjID = isHello;
			if (isHello) {
				if (respLen > 0) {
					hasAliveSameProjID = true;
				} else {
					hasAliveSameProjID = false;
				}
				socket.sendHeader(DeploySocket.H_BYE, 0);
			}
			return hasAliveSameProjID;
		} catch (final Throwable e) {
			// if(L.isInWorkshop){
			// LogManager.errToLog("check ip alive exception : " +
			// e.toString());
			// }
		} finally {
			try {
				s.close();
			} catch (final Exception ex) {
			}
		}

		return false;
	}

	private static Socket buildSocketTo(final String ip, final boolean showErrInfo) {
		try {
			final Socket client = new Socket();
			// 设置connect timeout
			client.connect(new InetSocketAddress(ip, ReceiveDeployServer.port),
					DeploySocket.CONN_TIMEOUT_MS);
			return client;
		} catch (final Throwable e) {
			if (showErrInfo) {
				if (L.isInWorkshop) {
					LogManager.errToLog(
							"[Deploy] fail to connect : " + ip + ", exception : " + e.toString());
				}
			}
		}
		return null;
	}

	private final DeploySocket socket;
	private byte[] passwordBS;

	public DeploySender(final String ip, final byte[] passBS, final boolean showErrInfo)
			throws Exception {
		this(buildDeploySocket(ip, showErrInfo), passBS);
	}

	public DeploySender(final DeploySocket socket, final byte[] passwordBS) {
		this.socket = socket;
		this.passwordBS = passwordBS;
	}

	public final void changePassword(final byte[] pwd) {
		this.passwordBS = pwd;
	}

	public final boolean auth() throws IOException {
		socket.sendHeader(DeploySocket.H_REQ_AUTH_FROM_CLIENT, 0);

		final int header = socket.receive();
		final int headerLen = socket.receiveDataLen();

		if (header == DeploySocket.H_AUTH) {
			if (headerLen > DeploySocket.MAX_DATA_LEN) {
				throw new IOException("Err max data len");
			}

			final int authLen = DeploySocket.AUTH_LEN;

			if (headerLen != authLen) {
				throw new IOException("should be AUTH_LEN, but " + headerLen);
			}

			{
				final byte[] authData = socket.receiveData(authLen, passwordBS);
				flipRandom(authData, authLen);

				socket.sendHeader(DeploySocket.H_AUTH, authLen);
				socket.sendData(authData, 0, authLen, true, true, passwordBS);
				DeploySocket.cache.cycle(authData);
			}

			final byte nextHeader = socket.receive();
			final int dataLen = socket.receiveDataLen();

			if (nextHeader == DeploySocket.H_ERROR) {
				final byte[] errBS = socket.receiveData(dataLen, null);
				DeploySocket.cache.cycle(errBS);
				return false;
			} else if (nextHeader != DeploySocket.H_OK) {
				throw new IOException("should be H_OK");
			}

			return true;
		} else {
			throw new IOException("should receive H_AUTH!");
		}
	}

	/**
	 * 
	 * @param bs
	 * @param off
	 * @param len
	 * @return true means successful.
	 * @throws IOException
	 *             可以抛出ERR_IS_BUSY
	 */
	public final boolean sendData(final byte[] bs, final int off, final int len)
			throws IOException {
		final String md5 = ResourceUtil.getMD5(bs, off, len);
		final byte[] md5bs = ByteUtil.getBytes(md5, IConstant.UTF_8);

		socket.sendHeader(DeploySocket.H_MD5, md5bs.length);
		socket.sendData(md5bs, 0, md5bs.length, true, true, passwordBS);

		{
			final int transVersionLen = DeploySocket.VERSION_1_0.length;
			socket.sendHeader(DeploySocket.H_TRANS_VER, transVersionLen);
			socket.sendData(DeploySocket.VERSION_1_0, 0, transVersionLen, false, false, null);
		}

		socket.sendHeader(DeploySocket.H_TRANS, len);
		socket.sendData(bs, off, len, true, true, passwordBS);

		final byte done = socket.receive();
		final int dataLen = socket.receiveDataLen();
		if (done == DeploySocket.H_OK) {
			socket.sendHeader(DeploySocket.H_BYE, 0);
			return true;
		} else if (done == DeploySocket.H_ERROR) {
			final byte[] errBS = socket.receiveData(dataLen, null);
			throw new DeployError(errBS);
		} else {
			throw new IOException("fail to trans data!");
		}
	}

	public static void flipRandom(final byte[] authData, final int randomLen) {
		for (int i = 0; i < randomLen; i++) {
			if (i % 2 == 0) {
				authData[i] += 1;
			} else {
				authData[i] -= 1;
			}
		}
	}

	public final void close() {
		socket.close();
	}
}
