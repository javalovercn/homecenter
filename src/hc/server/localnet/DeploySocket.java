package hc.server.localnet;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import hc.core.IConstant;
import hc.core.util.ByteArrayCacher;
import hc.core.util.ByteUtil;

public class DeploySocket {
	public static final byte H_HELLO = 10;
	public static final byte H_REQ_AUTH_FROM_CLIENT = 20;
	public static final byte H_AUTH = 30;
	public static final byte H_MD5 = 40;
	public static final byte H_ERROR = 50;
	public static final byte H_TRANS_VER = 60;
	public static final byte H_TRANS = 65;
	public static final byte H_OK = 70;
	public static final byte H_BYE = 80;
	public static final byte H_IS_ACCEPT_TRANS = 90;
	public static final byte H_MSG = 100;
	public static final byte H_SEND_BIND_OBJS = 101;
	public static final byte H_SAVE_BIND_OBJS = 102;
	
	public static final int HEADER_LEN_OFFSET = 1;
	public static final int HEADER_LEN_SIZE = 4;
	public static final int HEADER_BS_LEN = HEADER_LEN_OFFSET + HEADER_LEN_SIZE;

	public static final int AUTH_LEN = 100;
	public static final int MAX_DATA_LEN = 1 * 1024 * 1024;
	public static final int READ_TIMEOUT_MS = 0;//由于可能rebind在发送端，所以时间不限。
	public static final int CONN_TIMEOUT_MS = 1000;// 太小会导致无法正常连接，192.168.1.1: icmp_seq=0 ttl=64 time=0.672 ms

	public static final String TEST_PASSWORD = "Hello world";

	static final byte[] ERR_NO_JRUBY = ByteUtil.getBytes("JRuby is installing and NOT ready!", IConstant.UTF_8);
	static final byte[] ERR_EJECT_CONN = ByteUtil.getBytes("refuse connect for too many error password.", IConstant.UTF_8);
	static final byte[] ERR_IS_BUSY = ByteUtil.getBytes("server is busy or can't deploy now, please try late or close locking window!",
			IConstant.UTF_8);
	static final byte[] ERR_VERIFY_MD5 = ByteUtil.getBytes("fail verify md5", IConstant.UTF_8);
	static final byte[] ERR_PASSWORD = ByteUtil.getBytes("password is error!", IConstant.UTF_8);

	static final byte[] VERSION_1_0 = ByteUtil.getBytes("1.0", IConstant.UTF_8);

	public final void sendError(final String msg) throws IOException {
		final byte[] bs = ByteUtil.getBytes(msg, IConstant.UTF_8);
		sendError(bs);
	}
	
	public final void sendError(final byte[] bs) throws IOException {
		sendHeader(H_ERROR, bs.length);
		sendData(bs, 0, bs.length, false, false, null);
		
		this.socket.close();
	}
	
	public final void sendMsg(final String msg) throws IOException {
		final byte[] bs = ByteUtil.getBytes(msg, IConstant.UTF_8);
		sendMsg(bs);
	}
	
	public final byte sayHelloProject(final String projectID) throws IOException{
		final byte[] projectBS = ByteUtil.getBytes(projectID, IConstant.UTF_8);
		final int projectBSLen = projectBS.length;

		sendHeader(DeploySocket.H_HELLO, projectBSLen);
		sendData(projectBS, 0, projectBSLen, false, false, null);

		final byte header = receive();//如果是deled工程，则可能为H_ERROR
		final int respLen = receiveDataLen();
		if(respLen > 0) {
			final byte[] dataBS = receiveData(respLen, null);
			if(dataBS != null) {
				cache.cycle(dataBS);
			}
		}
		return header;
	}
	
	public final void sendMsg(final byte[] bs) throws IOException {
		sendHeader(H_MSG, bs.length);
		sendData(bs, 0, bs.length, false, false, null);
	}

	final static ByteArrayCacher cache = ByteUtil.byteArrayCacher;

	final Socket socket;
	final DataInputStream dis;
	final OutputStream os;

	public final void close() {
		try {
			socket.close();
		} catch (final Throwable e) {
		}
	}

	public DeploySocket(final Socket socket) throws Exception {
		this.socket = socket;
		socket.setSoTimeout(DeploySocket.READ_TIMEOUT_MS);

		dis = new DataInputStream(socket.getInputStream());
		os = socket.getOutputStream();
	}

	public final void sendHeader(final byte header, final int len) throws IOException {
		final byte[] head = cache.getFree(HEADER_BS_LEN);
		head[0] = header;
		ByteUtil.integerToFourBytes(len, head, DeploySocket.HEADER_LEN_OFFSET);

		os.write(head, 0, HEADER_BS_LEN);
		os.flush();

		cache.cycle(head);
	}

	public final void sendData(final byte[] bs, final int offset, final int len, final boolean isEncrypt, final boolean isEncode,
			final byte[] pwd) throws IOException {
		if (isEncrypt) {
			superXor(bs, offset, len, pwd, isEncode);
		}

		os.write(bs, offset, len);
		os.flush();
	}

	/**
	 * 
	 * @param len
	 * @param passwords
	 *            null means NOT encrypt
	 * @return
	 * @throws IOException
	 */
	public final byte[] receiveData(final int len, final byte[] passwords) throws IOException {
		final byte[] bs = cache.getFree(len);
		dis.readFully(bs, 0, len);
		if (passwords != null) {
			superXor(bs, 0, len, passwords, false);
		}
		return bs;
	}

	/**
	 * 
	 * @param bs
	 * @param len
	 * @param passwords
	 *            null means NOT encrypt
	 * @return
	 * @throws IOException
	 */
	public final byte[] receiveData(final byte[] bs, final int len, final byte[] passwords) throws IOException {
		dis.readFully(bs, 0, len);
		if (passwords != null) {
			superXor(bs, 0, len, passwords, false);
		}
		return bs;
	}

	public final int receiveDataLen() throws IOException {
		final byte[] bs = cache.getFree(DeploySocket.HEADER_LEN_SIZE);
		dis.readFully(bs, 0, DeploySocket.HEADER_LEN_SIZE);
		final int out = (int) ByteUtil.fourBytesToLong(bs);
		cache.cycle(bs);
		return out;
	}

	public final byte receive() throws IOException {
		return dis.readByte();
	}

	private final void superXor(final byte[] src, final int offset, final int s_len, final byte[] keys, final boolean isEncode) {
		final int k_len = keys.length;
		if (k_len == 0) {// 密码为空
			return;
		}

		final int endIdx = offset + s_len;

		if (isEncode) {
			int modeKIdx = 0;
			for (int i = offset; i < endIdx; i++) {
				src[i] ^= keys[modeKIdx++];
				if (modeKIdx == k_len) {
					modeKIdx = 0;
				}
				src[i] ^= keys[modeKIdx++];
				if (modeKIdx == k_len) {
					modeKIdx = 0;
				}
				// System.out.println("modeKIdx : " + modeKIdx);
			}
		}
		final int factorBigOne = 4;
		final int factor_temp = (s_len <= 10) ? factorBigOne * 4 : ((s_len <= 20) ? factorBigOne * 2 : factorBigOne);
		int modeK = isEncode ? 0 : ((factor_temp * s_len - 1) % k_len);
		int t = isEncode ? 0 : (factor_temp - 1);
		for (; t >= 0 && t < factor_temp;) {
			int i = isEncode ? offset : (endIdx - 1);
			for (; i >= offset && i < endIdx;) {
				final byte maskKey = keys[(t + (modeK)) % k_len];
				final int maskKeyInt = maskKey & (0xFF);
				final int storeIdx = ((factor_temp + t + (t == 0 ? i : i << 1) + maskKeyInt + (factor_temp == 0 ? 0 : modeK)) % s_len)
						+ offset;
				// if(storeIdx < offset || storeIdx >= (offset + s_len)){
				// LogManager.logInTest("storeIdx : " + storeIdx + ", modeK:" +
				// modeK + ", k_len:" + k_len + ", maskKey:" + (maskKey & 0xFF)
				// + ", s_len:" + s_len);
				// }
				final boolean maskKModTwo = (modeK % 2) == 0;
				if (isEncode == false && maskKModTwo) {
					src[storeIdx] ^= maskKey;
				}
				if (maskKey % 2 == 0) {
					if (isEncode) {
						src[storeIdx] += maskKey / 2;
					} else {
						src[storeIdx] -= maskKey / 2;
					}
				} else {
					if (isEncode) {
						src[storeIdx] -= maskKey;
					} else {
						src[storeIdx] += maskKey;
					}
				}
				// System.out.println("storeIdx : " + storeIdx + ", modeK:" +
				// modeK + ", i : " + i + ", t : " + t + ", maskKey : " +
				// maskKey);
				if (isEncode && maskKModTwo) {
					src[storeIdx] ^= maskKey;
				}
				if (isEncode) {
					if (++modeK == k_len) {
						modeK = 0;
					}
					i++;
				} else {
					if (--modeK == -1) {
						modeK = k_len - 1;
					}
					i--;
				}
			}
			if (isEncode) {
				t++;
			} else {
				t--;
			}
		}

		if (isEncode == false) {
			int modeKIdx = (s_len * 2 - 1) % k_len;
			// System.out.println("modeKIdx : " + modeKIdx);
			for (int i = endIdx - 1; i >= offset; i--) {
				src[i] ^= keys[modeKIdx--];
				if (modeKIdx == -1) {
					modeKIdx = k_len - 1;
				}
				src[i] ^= keys[modeKIdx--];
				if (modeKIdx == -1) {
					modeKIdx = k_len - 1;
				}
			}
		}
	}

}
