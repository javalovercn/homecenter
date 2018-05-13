package hc.server.localnet;

import java.awt.BorderLayout;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import hc.App;
import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ByteUtil;
import hc.core.util.LogManager;
import hc.server.ui.design.BDNTree;
import hc.server.ui.design.ConverterTree;
import hc.server.ui.design.Designer;
import hc.server.ui.design.DevTree;
import hc.server.ui.design.DeviceBinderWizard;
import hc.server.ui.design.LocalnetDeviceBinderWizSource;
import hc.util.ResourceUtil;

public class DeploySender {

	private static DeploySocket buildDeploySocket(final String hostAddress) throws Exception {//注：此处抛出异常需要caller显示
		final Socket s = buildSocketTo(hostAddress);
		if(s == null) {
			throw new DeployError("fail to connect : " + hostAddress);
		}
		return new DeploySocket(s);
	}
	
	public final byte sayHelloProject(final String projectID) throws IOException{
		return socket.sayHelloProject(projectID);
	}

	public static boolean isAlive(final String ip, final Designer designer) {

		final Socket s = buildSocketTo(ip);

		if (s == null) {
			return false;
		}
		boolean isHelloOK = false;
		try {
			final DeploySocket socket = new DeploySocket(s);
			final byte header = socket.sayHelloProject(designer.getCurrProjID());
			isHelloOK = (header == DeploySocket.H_HELLO || header == DeploySocket.H_ERROR);
			socket.sendHeader(DeploySocket.H_BYE, 0);
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

		return isHelloOK;
	}

	public static Socket buildSocketTo(final String ip) {
		try {
			final Socket client = new Socket();
			// 设置connect timeout
			client.connect(new InetSocketAddress(ip, ReceiveDeployServer.port), DeploySocket.CONN_TIMEOUT_MS);
			return client;
		} catch (final Throwable e) {
			if (L.isInWorkshop) {
				LogManager.log("[Deploy] fail to connect : " + ip + ", exception : " + e.toString());
			}
		}
		return null;
	}

	private final DeploySocket socket;
	private byte[] passwordBS;

	public DeploySender(final String ip, final byte[] passBS) throws Exception {
		this(buildDeploySocket(ip), passBS);
	}

	public DeploySender(final DeploySocket socket, final byte[] passwordBS) {
		this.socket = socket;
		this.passwordBS = passwordBS;
	}

	public final void changePassword(final byte[] pwd) {
		this.passwordBS = pwd;
	}
	
	public final boolean isAcceptTran() throws Exception {
		socket.sendHeader(DeploySocket.H_IS_ACCEPT_TRANS, 0);
		
		final int header = socket.receive();
		final int headerLen = socket.receiveDataLen();
		if(header == DeploySocket.H_IS_ACCEPT_TRANS) {
			final byte[] authData = socket.receiveData(headerLen, null);
			final String result = ByteUtil.bytesToStr(authData, 0, headerLen);
			DeploySocket.cache.cycle(authData);
			return IConstant.TRUE.equals(result);
		}else {
			throw new Exception("low version");
		}
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
	 * @param bs
	 * @param off
	 * @param len
	 * @return true means successful.
	 * @throws IOException
	 *             可以抛出ERR_IS_BUSY
	 */
	public final boolean sendHARData(final byte[] bs, final int off, final int len, final String deployIPMaybeNull) throws IOException {
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

		while(true) {
			L.V = L.WShop ? false : LogManager.log("[Deploy] ready receive response after H_TRANS.");
			final byte done = socket.receive();//注：此处超时异常会被外层拦截，并提示用户deploy超时
			final int dataLen = socket.receiveDataLen();
			if(done == DeploySocket.H_MSG) {
				L.V = L.WShop ? false : LogManager.log("[Deploy] receive H_MSG after H_TRANS.");
				final byte[] msgBS = socket.receiveData(dataLen, null);
				final String msg = ByteUtil.buildString(msgBS, 0, dataLen, IConstant.UTF_8);
				DeploySocket.cache.cycle(msgBS);
				
				final JPanel panel = new JPanel(new BorderLayout());
				final JLabel lable = new JLabel(msg, App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING);
				panel.add(lable, BorderLayout.CENTER);
				final boolean isNewJFrame = true;
				App.showCenterPanelMain(panel, 0, 0, ResourceUtil.getInfoI18N(), false, null, null, null, null, Designer.getInstance(), false, isNewJFrame, null, false, false);
				
				continue;
			}else if (done == DeploySocket.H_OK) {
				L.V = L.WShop ? false : LogManager.log("[Deploy] receive OK and send bye.");
				socket.sendHeader(DeploySocket.H_BYE, 0);
				try {
					Thread.sleep(1000);
				}catch (final Exception e) {
				}
				return true;
			} else if (done == DeploySocket.H_ERROR) {
				L.V = L.WShop ? false : LogManager.log("[Deploy] receive H_ERROR after H_TRANS.");
				final byte[] errBS = socket.receiveData(dataLen, null);
				final String errStr = ByteUtil.buildString(errBS, 0, dataLen, IConstant.UTF_8);
				DeploySocket.cache.cycle(errBS);
				throw new DeployError(errStr);
			} else if(done == DeploySocket.H_SEND_BIND_OBJS){
				final byte[] objsBS = socket.receiveData(dataLen, null);
				final ByteArrayInputStream bais = new ByteArrayInputStream(objsBS);
				final ObjectInputStream ois = new ObjectInputStream(bais);

				try {
					final DevTree devTree = (DevTree)ois.readObject();
					final ConverterTree converterTree = (ConverterTree)ois.readObject();
					final BDNTree bdnTree = (BDNTree)ois.readObject();
					
					final LocalnetDeviceBinderWizSource localnetDevBindSource = new LocalnetDeviceBinderWizSource(devTree, converterTree, bdnTree, socket);
					DeviceBinderWizard.enterBindUI(localnetDevBindSource, Designer.getInstance(), deployIPMaybeNull);
				}catch (final Exception e) {
					throw new IOException("version are not same, try to upgrade both!");
				}
			} else {
				L.V = L.WShop ? false : LogManager.log("[Deploy] receive unknow head after H_TRANS.");
				throw new IOException("fail to trans data, header : " + done);
			}
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
