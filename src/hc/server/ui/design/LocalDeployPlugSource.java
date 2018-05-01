package hc.server.ui.design;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import hc.core.L;
import hc.core.util.LogManager;
import hc.server.localnet.DeploySocket;

public class LocalDeployPlugSource extends PlugSource {
	final DeploySocket socket;
	
	public LocalDeployPlugSource(final DeploySocket socket) {
		this.socket = socket;
	}

	@Override
	public void sendMessage(final String msg) {
		try {
			socket.sendMsg(msg);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendError(final String error) {
		try {
			socket.sendError(error);
		}catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendWarn(final String warn) {
		sendMessage(warn);
	}

	@Override
	public J2SESession getJ2SESession() {
		return null;
	}

	@Override
	public void sendInitCheckActivingMessaeg() {
	}

	@Override
	public void enterMobileBindInSysThread(final MobiUIResponsor mobiResp, final BindRobotSource bindSource) {
		final DesktopDeviceBinderWizSource desktop = new DesktopDeviceBinderWizSource(bindSource, mobiResp);
		try {
			final DevTree devTree = desktop.buildDevTree();
			final ConverterTree converterTree = desktop.buildConverterTree();
			final BDNTree bdnTree = desktop.buildTree();
			
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(devTree);
			oos.writeObject(converterTree);
			oos.writeObject(bdnTree);
			oos.flush();
			final byte[] bs = bos.toByteArray();
			oos.close();
			bos.close();
			
			final int objsLen = bs.length;
			socket.sendHeader(DeploySocket.H_SEND_BIND_OBJS, objsLen);
			socket.sendData(bs, 0, objsLen, false, false, null);
			L.V = L.WShop ? false : LogManager.log("[Deploy] send H_SEND_BIND_OBJS and data len : " + objsLen);
			
			final byte backByte = socket.receive();
			final int headerLen = socket.receiveDataLen();
			if(headerLen == 0) {
				//取消
				DesktopDeviceBinderWizSource.cancelImpl();
				LogManager.log("[Deploy] cancel rebind on send side.");
			}else if(backByte == DeploySocket.H_SAVE_BIND_OBJS){
				L.V = L.WShop ? false : LogManager.log("[Deploy] receive H_SAVE_BIND_OBJS.");
				final byte[] bdnTreeBS = socket.receiveData(headerLen, null);
				final ByteArrayInputStream bais = new ByteArrayInputStream(bdnTreeBS);
				final ObjectInputStream ois = new ObjectInputStream(bais);
				final BDNTree back = (BDNTree)ois.readObject();
				DesktopDeviceBinderWizSource.saveImpl(back, mobiResp);
				ois.close();
				bais.close();
				LogManager.log("[Deploy] done and save bind on receiver side after rebind on send side.");
			}
		}catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
